/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * Oracle array_set function.
 */
public class OracleArraySetFunction extends ArraySetUnnestFunction {

	public OracleArraySetFunction() {
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker) {
		JdbcMappingContainer expressionType = null;
		for ( SqlAstNode sqlAstArgument : sqlAstArguments ) {
			expressionType = ( (Expression) sqlAstArgument ).getExpressionType();
			if ( expressionType != null ) {
				break;
			}
		}

		final String arrayTypeName = ArrayTypeHelper.getArrayTypeName( expressionType, walker );
		sqlAppender.append( arrayTypeName );
		sqlAppender.append( "_set(" );
		sqlAstArguments.get( 0 ).accept( walker );
		sqlAppender.append( ',' );
		sqlAstArguments.get( 1 ).accept( walker );
		sqlAppender.append( ',' );
		sqlAstArguments.get( 2 ).accept( walker );
		sqlAppender.append( ')' );
	}
}
