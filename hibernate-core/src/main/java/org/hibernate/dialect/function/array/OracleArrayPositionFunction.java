/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

public class OracleArrayPositionFunction extends AbstractArrayPositionFunction {

	public OracleArrayPositionFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final String arrayTypeName = DdlTypeHelper.getTypeName(
				arrayExpression.getExpressionType(),
				walker.getSessionFactory().getTypeConfiguration()
		);
		sqlAppender.appendSql( arrayTypeName );
		sqlAppender.append( "_position(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ',' );
		sqlAstArguments.get( 1 ).accept( walker );
		if ( sqlAstArguments.size() > 2 ) {
			sqlAppender.append( ',' );
			sqlAstArguments.get( 2 ).accept( walker );
		}
		sqlAppender.append( ")" );
	}
}
