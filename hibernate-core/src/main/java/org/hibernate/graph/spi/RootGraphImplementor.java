/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.graph.spi;

import org.hibernate.graph.RootGraph;
import org.hibernate.metamodel.model.domain.spi.EntityTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeImplementor;

/**
 * Integration version of the RootGraph contract
 *
 * @author Steve Ebersole
 */
public interface RootGraphImplementor<J> extends RootGraph<J>, GraphImplementor<J> {
	boolean appliesTo(EntityTypeImplementor<? super J> entityType);

	@Override
	@SuppressWarnings("unchecked")
	default boolean appliesTo(ManagedTypeImplementor<? super J> managedType) {
		assert managedType instanceof EntityTypeImplementor;
		return appliesTo( (EntityTypeImplementor) managedType );
	}

	@Override
	RootGraphImplementor<J> makeRootGraph(String name, boolean mutable);

	@Override
	SubGraphImplementor<J> makeSubGraph(boolean mutable);

	/**
	 * Make an immutable copy of this entity graph, using the given name.
	 *
	 * @param name The name to apply to the immutable copy
	 *
	 * @return The immutable copy
	 */
	default RootGraphImplementor<J> makeImmutableCopy(String name) {
		return makeRootGraph( name, false );
	}
}
