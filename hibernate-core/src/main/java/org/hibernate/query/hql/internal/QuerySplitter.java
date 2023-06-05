/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import java.util.Set;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.internal.SimpleSqmCopyContext;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.SqmPolymorphicRootDescriptor;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmQueryGroup;
import org.hibernate.query.sqm.tree.select.SqmQueryPart;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

/**
 * Handles splitting queries containing unmapped polymorphic references.
 *
 * @author Steve Ebersole
 * @author Marco Belladelli
 */
public class QuerySplitter {

	public static <R> SqmSelectStatement<R>[] split(
			SqmSelectStatement<R> statement,
			SessionFactoryImplementor sessionFactory) {
		// We only allow unmapped polymorphism in a very restricted way.  Specifically,
		// the unmapped polymorphic reference can only be a root and can be the only
		// root.  Use that restriction to locate the unmapped polymorphic reference
		final SqmRoot<?> unmappedPolymorphicReference = findUnmappedPolymorphicReference( statement.getQueryPart() );

		if ( unmappedPolymorphicReference == null ) {
			return new SqmSelectStatement[] { statement };
		}

		final SqmPolymorphicRootDescriptor<?> unmappedPolymorphicDescriptor = (SqmPolymorphicRootDescriptor<?>) unmappedPolymorphicReference.getReferencedPathSource();
		final Set<EntityDomainType<?>> implementors = unmappedPolymorphicDescriptor.getImplementors();
		final SqmSelectStatement<R>[] expanded = new SqmSelectStatement[ implementors.size() ];

		int i = 0;
		for ( EntityDomainType<?> mappedDescriptor : implementors ) {
			expanded[i++] = copyStatement( statement, unmappedPolymorphicReference, mappedDescriptor );
		}

		return expanded;
	}

	private static SqmRoot<?> findUnmappedPolymorphicReference(SqmQueryPart<?> queryPart) {
		if ( queryPart instanceof SqmQuerySpec<?> ) {
			return ( (SqmQuerySpec<?>) queryPart ).getRoots()
					.stream()
					.filter( sqmRoot -> sqmRoot.getReferencedPathSource() instanceof SqmPolymorphicRootDescriptor )
					.findFirst()
					.orElse( null );
		}
		else {
			final SqmQueryGroup<?> queryGroup = (SqmQueryGroup<?>) queryPart;
			final SqmRoot<?> root = findUnmappedPolymorphicReference( queryGroup.getQueryParts().get( 0 ) );
			if ( root != null ) {
				throw new UnsupportedOperationException( "Polymorphic query group is unsupported" );
			}
			return null;
		}
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	private static <S extends SqmStatement<?>> S copyStatement(
			S statement,
			SqmRoot unmappedPolymorphicReference,
			EntityDomainType<?> mappedDescriptor) {
		// Copy the statement replacing the root's unmapped polymorphic reference with
		// the concrete mapped descriptor entity domain type
		final SqmCopyContext context = new QuerySplitterCopyContext();
		final SqmRoot<?> path = context.registerCopy(
				unmappedPolymorphicReference,
				new SqmRoot<>(
						mappedDescriptor,
						unmappedPolymorphicReference.getExplicitAlias(),
						unmappedPolymorphicReference.isAllowJoins(),
						unmappedPolymorphicReference.nodeBuilder()
				)
		);
		unmappedPolymorphicReference.copyTo( path, context );
		return (S) statement.copy( context );
	}

	public static <R> SqmDeleteStatement<R>[] split(
			SqmDeleteStatement<R> statement,
			SessionFactoryImplementor sessionFactory) {
		// We only allow unmapped polymorphism in a very restricted way.  Specifically,
		// the unmapped polymorphic reference can only be a root and can be the only
		// root.  Use that restriction to locate the unmapped polymorphic reference
		final SqmRoot<?> unmappedPolymorphicReference = findUnmappedPolymorphicReference( statement );

		if ( unmappedPolymorphicReference == null ) {
			return new SqmDeleteStatement[] { statement };
		}

		final SqmPolymorphicRootDescriptor<?> unmappedPolymorphicDescriptor = (SqmPolymorphicRootDescriptor<?>) unmappedPolymorphicReference.getReferencedPathSource();
		final Set<EntityDomainType<?>> implementors = unmappedPolymorphicDescriptor.getImplementors();
		final SqmDeleteStatement<R>[] expanded = new SqmDeleteStatement[ implementors.size() ];

		int i = 0;
		for ( EntityDomainType<?> mappedDescriptor : implementors ) {
			expanded[i++] = copyStatement( statement, unmappedPolymorphicReference, mappedDescriptor );
		}

		return expanded;
	}

	private static SqmRoot<?> findUnmappedPolymorphicReference(SqmDeleteOrUpdateStatement<?> queryPart) {
		if ( queryPart.getTarget().getReferencedPathSource() instanceof SqmPolymorphicRootDescriptor<?> ) {
			return queryPart.getTarget();
		}
		return null;
	}

	private static class QuerySplitterCopyContext extends SimpleSqmCopyContext {
		@Override
		public <T> T getCopy(T original) {
			if ( original instanceof SqmParameter<?> ) {
				// We never want to copy parameters when splitting polymorphic queries, as that
				// would cause losing their binding information as that's stored by instance,
				// so we always return the original object instead
				return original;
			}
			return super.getCopy( original );
		}
	}
}
