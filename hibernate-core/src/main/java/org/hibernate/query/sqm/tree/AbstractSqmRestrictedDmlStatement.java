/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree;

import java.util.Map;
import java.util.Set;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaCriteriaBase;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;

/**
 * @author Christian Beikov
 */
public abstract class AbstractSqmRestrictedDmlStatement<T> extends AbstractSqmDmlStatement<T>
		implements JpaCriteriaBase {

	private SqmWhereClause whereClause;

	public AbstractSqmRestrictedDmlStatement(SqmQuerySource querySource, NodeBuilder nodeBuilder) {
		super( querySource, nodeBuilder );
	}

	public AbstractSqmRestrictedDmlStatement(SqmRoot<T> target, SqmQuerySource querySource, NodeBuilder nodeBuilder) {
		super( target, querySource, nodeBuilder );
	}

	public AbstractSqmRestrictedDmlStatement(
			NodeBuilder builder,
			SqmQuerySource querySource,
			Set<SqmParameter<?>> parameters,
			Map<String, SqmCteStatement<?>> cteStatements,
			boolean withRecursiveCte,
			SqmRoot<T> target) {
		super( builder, querySource, parameters, cteStatements, withRecursiveCte, target );
	}

	protected SqmWhereClause copyWhereClause(SqmCopyContext context) {
		if ( getWhereClause() == null ) {
			return null;
		}
		else {
			final SqmWhereClause whereClause = new SqmWhereClause( nodeBuilder() );
			whereClause.setPredicate( getWhereClause().getPredicate().copy( context ) );
			return whereClause;
		}
	}

	public Root<T> from(Class<T> entityClass) {
		final EntityDomainType<T> entity = nodeBuilder().getDomainModel().entity( entityClass );
		SqmRoot<T> root = new SqmRoot<>( entity, null, false, nodeBuilder() );
		setTarget( root );
		return root;
	}

	public Root<T> from(EntityType<T> entity) {
		SqmRoot<T> root = new SqmRoot<>( (EntityDomainType<T>) entity, null, false, nodeBuilder() );
		setTarget( root );
		return root;
	}

	public Root<T> getRoot() {
		return getTarget();
	}

	public SqmWhereClause getWhereClause() {
		return whereClause;
	}

	public void applyPredicate(SqmPredicate predicate) {
		if ( predicate == null ) {
			return;
		}

		initAndGetWhereClause().applyPredicate( predicate );
	}

	public void setWhereClause(SqmWhereClause whereClause) {
		this.whereClause = whereClause;
	}

	@Override
	public JpaPredicate getRestriction() {
		return whereClause == null ? null : whereClause.getPredicate();
	}

	protected void setWhere(Expression<Boolean> restriction) {
		// Replaces the current predicate if one is present
		initAndGetWhereClause().setPredicate( (SqmPredicate) restriction );
	}

	protected SqmWhereClause initAndGetWhereClause() {
		if ( whereClause == null ) {
			whereClause = new SqmWhereClause( nodeBuilder() );
		}
		return whereClause;
	}

	protected void setWhere(Predicate... restrictions) {
		final SqmWhereClause whereClause = initAndGetWhereClause();
		// Clear the current predicate if one is present
		whereClause.setPredicate(null);
		for ( Predicate restriction : restrictions ) {
			whereClause.applyPredicate( (SqmPredicate) restriction );
		}
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		if ( whereClause != null && whereClause.getPredicate() != null ) {
			sb.append( " where " );
			whereClause.getPredicate().appendHqlString( sb );
		}
	}
}
