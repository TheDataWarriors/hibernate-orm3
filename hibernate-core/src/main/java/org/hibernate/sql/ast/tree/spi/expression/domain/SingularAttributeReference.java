/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.results.spi.Selectable;

/**
 * @author Steve Ebersole
 */
public class SingularAttributeReference implements NavigableReference {
	private final NavigableContainerReference containerReference;
	private final SingularPersistentAttribute referencedAttribute;
	private final NavigablePath navigablePath;


	public SingularAttributeReference(
			NavigableContainerReference containerReference,
			SingularPersistentAttribute referencedAttribute,
			NavigablePath navigablePath) {
		this.containerReference = containerReference;
		this.referencedAttribute = referencedAttribute;
		this.navigablePath = navigablePath;
	}

	public SingularPersistentAttribute<?,?> getReferencedAttribute() {
		return referencedAttribute;
	}

	@Override
	public ExpressableType getType() {
		return referencedAttribute;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitSingularAttributeReference( this );
	}

	@Override
	public Selectable getSelectable() {
		return getNavigable();
	}

	@Override
	public Navigable getNavigable() {
		return getReferencedAttribute();
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public NavigableContainerReference getNavigableContainerReference() {
		return containerReference;
	}
}
