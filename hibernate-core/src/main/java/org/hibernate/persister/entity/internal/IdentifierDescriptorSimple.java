/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.entity.internal;

import java.util.List;

import org.hibernate.mapping.Property;
import org.hibernate.persister.common.internal.PersisterHelper;
import org.hibernate.persister.common.spi.AbstractSingularPersistentAttribute;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.common.spi.NavigableVisitationStrategy;
import org.hibernate.persister.common.spi.SingularPersistentAttribute;
import org.hibernate.persister.entity.spi.EntityHierarchy;
import org.hibernate.persister.entity.spi.IdentifiableTypeImplementor;
import org.hibernate.persister.entity.spi.IdentifierDescriptor;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.ast.produce.result.spi.Fetch;
import org.hibernate.sql.ast.produce.result.spi.FetchParent;
import org.hibernate.sql.ast.produce.result.spi.Return;
import org.hibernate.sql.ast.produce.result.spi.ReturnResolutionContext;
import org.hibernate.sql.ast.consume.spi.SqlSelectAstToJdbcSelectConverter;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReferenceExpression;
import org.hibernate.sql.ast.tree.spi.expression.domain.SingularAttributeReferenceExpression;
import org.hibernate.sql.ast.tree.spi.from.ColumnReference;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.select.Selectable;
import org.hibernate.sql.ast.tree.spi.select.SelectableBasicTypeImpl;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class IdentifierDescriptorSimple<O,J>
		extends AbstractSingularPersistentAttribute<O,J,BasicType<J>>
		implements IdentifierDescriptor<O,J>, SingularPersistentAttribute<O,J> {
	private final EntityHierarchy hierarchy;
	private final List<Column> columns;

	public IdentifierDescriptorSimple(
			EntityHierarchy hierarchy,
			IdentifiableTypeImplementor declarer,
			Property property,
			BasicType<J> ormType,
			List<Column> columns,
			PersisterCreationContext creationContext) {
		super(
				hierarchy.getRootEntityPersister(),
				property.getName(),
				PersisterHelper.resolvePropertyAccess( declarer, property, creationContext ),
				ormType,
				Disposition.ID,
				false
		);
		this.hierarchy = hierarchy;
		this.columns = columns;
	}

	@Override
	public BasicType getIdType() {
		return (BasicType) getOrmType();
	}

	@Override
	public boolean hasSingleIdAttribute() {
		return true;
	}

	@Override
	public List<Column> getColumns() {
		return columns;
	}

	@Override
	public SingularPersistentAttribute<O,J> getIdAttribute() {
		return this;
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.BASIC;
	}

	@Override
	public String asLoggableText() {
		return "IdentifierSimple(" + getSource().asLoggableText() + ")";
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.BASIC;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitSimpleIdentifier( hierarchy, getIdAttribute() );
	}

	@Override
	public Return generateReturn(
			ReturnResolutionContext returnResolutionContext,
			TableGroup tableGroup) {
		return new SelectableImpl( this, returnResolutionContext, tableGroup ).toQueryReturn( returnResolutionContext, null );
	}

	@Override
	public Fetch generateFetch(
			ReturnResolutionContext returnResolutionContext,
			TableGroup tableGroup,
			FetchParent fetchParent) {
		throw new UnsupportedOperationException();
	}




	private static class SelectableImpl implements Selectable, NavigableReferenceExpression {
		private final SingularAttributeReferenceExpression expressionDelegate;
		private final SelectableBasicTypeImpl selectableDelegate;
		private final NavigablePath navigablePath;

		public SelectableImpl(
				IdentifierDescriptorSimple idDescriptor,
				ReturnResolutionContext returnResolutionContext,
				TableGroup tableGroup) {
			this.navigablePath = returnResolutionContext.currentNavigablePath().append( idDescriptor.getNavigableName() );

			this.expressionDelegate = new SingularAttributeReferenceExpression(
					tableGroup,
					idDescriptor,
					navigablePath
			);
			this.selectableDelegate = new SelectableBasicTypeImpl(
					this,
					getColumnReferences().get( 0 ),
					getType()
			);
		}

		@Override
		public BasicType getType() {
			return (BasicType) expressionDelegate.getType();
		}

		@Override
		public Selectable getSelectable() {
			return this;
		}

		@Override
		public void accept(SqlSelectAstToJdbcSelectConverter walker) {
			// todo (6.0) : do we need a separate "visitEntityIdentifier" method(s)?

			walker.visitSingularAttributeReference( expressionDelegate );
		}

		@Override
		public Expression getSelectedExpression() {
			return expressionDelegate;
		}

		@Override
		public Return toQueryReturn(ReturnResolutionContext returnResolutionContext, String resultVariable) {
			return selectableDelegate.toQueryReturn( returnResolutionContext, resultVariable );
		}

		@Override
		public Navigable getNavigable() {
			return expressionDelegate.getNavigable();
		}

		@Override
		public NavigablePath getNavigablePath() {
			return expressionDelegate.getNavigablePath();
		}

		@Override
		public List<ColumnReference> getColumnReferences() {
			return expressionDelegate.getColumnReferences();
		}
	}
}
