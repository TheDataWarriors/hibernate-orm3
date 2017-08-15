/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.ConversionException;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.results.spi.SqlSelection;

/**
 * Represents a selection that is "re-used" in certain parts of the query
 * other than the select-clause (mainly important for order-by, group-by and
 * having).  Allows usage of the selection position within the select-clause
 * in that other part of the query rather than the full expression
 *
 * @author Steve Ebersole
 */
public class SqlSelectionExpression implements Expression {
	private final SqlSelection theSelection;
	private final Expression theExpression;

	public SqlSelectionExpression(
			SqlSelection theSelection,
			Expression theExpression) {
		this.theSelection = theSelection;
		this.theExpression = theExpression;
	}

	public SqlSelection getSelection() {
		return theSelection;
	}

	public Expression getExpression() {
		return theExpression;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitSqlSelectionExpression( this );
	}

	@Override
	public ExpressableType getType() {
		return theExpression.getType();
	}

	@Override
	public QueryResult createQueryResult(
			Expression expression,
			String resultVariable,
			SqlExpressionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		throw new ConversionException(
				"Unexpected attempt to create QueryResult from specialized SqlSelectionExpression"
		);
	}

}
