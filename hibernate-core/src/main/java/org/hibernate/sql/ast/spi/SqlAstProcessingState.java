/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

/**
 * Generalized access to state information relative to the "current process" of
 * creating a SQL AST.
 *
 * @author Steve Ebersole
 */
public interface SqlAstProcessingState {
	SqlAstProcessingState getParentState();

	SqlExpressionResolver getSqlExpressionResolver();

	SqlAstCreationState getSqlAstCreationState();

	default boolean isTopLevel() {//todo: naming
		return getParentState() == null;
	}
}
