/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import org.hibernate.engine.spi.RowSelection;


/**
 * A {@link LimitHandler} for Transact SQL and similar
 * databases which support the syntax {@code SELECT TOP n}.
 *
 * @author Brett Meyer
 */
public class TopLimitHandler extends AbstractLimitHandler {

	public static TopLimitHandler INSTANCE = new TopLimitHandler();

	@Override
	public String processSql(String sql, RowSelection selection) {
		if ( !hasMaxRows( selection) ) {
			return sql;
		}
		String top = supportsVariableLimit()
				? " top ? "
				: " top " + getMaxOrLimit(selection) + " ";
		return insertAfterDistinct( top, sql );
	}

	@Override
	public final boolean supportsLimit() {
		return true;
	}

	@Override
	public final boolean supportsLimitOffset() {
		return false;
	}

	@Override
	public boolean supportsVariableLimit() {
		return true;
	}

	@Override
	public final boolean bindLimitParametersFirst() {
		return true;
	}

}
