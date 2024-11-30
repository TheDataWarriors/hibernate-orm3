/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import java.sql.PreparedStatement;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Helper for creating JDBC statements.
 *
 * @author Steve Ebersole
 */
public class StatementCreatorHelper {
	public static PreparedStatement prepareQueryStatement(
			String sql,
			SharedSessionContractImplementor session) {
		return session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sql );
	}
}
