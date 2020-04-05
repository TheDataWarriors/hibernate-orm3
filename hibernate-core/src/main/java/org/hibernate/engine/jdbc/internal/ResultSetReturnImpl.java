/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.ResultSetReturn;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.resource.jdbc.spi.StatementExecutionListener;

/**
 * Standard implementation of the ResultSetReturn contract
 *
 * @author Brett Meyer
 */
public class ResultSetReturnImpl implements ResultSetReturn {
	private final JdbcCoordinator jdbcCoordinator;

	private final Dialect dialect;
	private final SqlStatementLogger sqlStatementLogger;
	private final SqlExceptionHelper sqlExceptionHelper;
	private final StatementExecutionListener statementExecutionListener;


	/**
	 * Constructs a ResultSetReturnImpl
	 *
	 * @param jdbcCoordinator The JdbcCoordinator
	 */
	public ResultSetReturnImpl(JdbcCoordinator jdbcCoordinator, JdbcServices jdbcServices, StatementExecutionListener statementExecutionListener) {
		this.jdbcCoordinator = jdbcCoordinator;
		this.dialect = jdbcServices.getDialect();
		this.sqlStatementLogger = jdbcServices.getSqlStatementLogger();
		this.sqlExceptionHelper = jdbcServices.getSqlExceptionHelper();
		this.statementExecutionListener = statementExecutionListener;
	}

	@Override
	public ResultSet extract(PreparedStatement statement) {
		// IMPL NOTE : SQL logged by caller
		long executeStartNanos = 0;
		if ( isLogSlowQuery() ) {
			executeStartNanos = System.nanoTime();
		}
		try {
			final ResultSet rs;
			try {
				jdbcExecuteStatementStart();
				rs = statement.executeQuery();
			}
			finally {
				jdbcExecuteStatementEnd();
				logSlowQuery( statement, executeStartNanos );
			}
			postExtract( rs, statement );
			return rs;
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not extract ResultSet" );
		}
	}

	private void jdbcExecuteStatementEnd() {
		jdbcCoordinator.getJdbcSessionOwner().getJdbcSessionContext().getObserver().jdbcExecuteStatementEnd();
	}

	private void jdbcExecuteStatementStart() {
		jdbcCoordinator.getJdbcSessionOwner().getJdbcSessionContext().getObserver().jdbcExecuteStatementStart();
	}

	@Override
	public ResultSet extract(CallableStatement callableStatement) {
		// IMPL NOTE : SQL logged by caller
		long executeStartNanos = 0;
		if ( isLogSlowQuery() ) {
			executeStartNanos = System.nanoTime();
		}
		try {
			final ResultSet rs;
			try {
				jdbcExecuteStatementStart();
				rs = dialect.getResultSet( callableStatement );
			}
			finally {
				jdbcExecuteStatementEnd();
				logSlowQuery( callableStatement, executeStartNanos );
			}
			postExtract( rs, callableStatement );
			return rs;
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not extract ResultSet" );
		}
	}

	@Override
	public ResultSet extract(Statement statement, String sql) {
		sqlStatementLogger.logStatement( sql );
		long executeStartNanos = 0;
		if ( isLogSlowQuery() ) {
			executeStartNanos = System.nanoTime();
		}
		try {
			final ResultSet rs;
			try {
				jdbcExecuteStatementStart();
				rs = statement.executeQuery( sql );
			}
			finally {
				jdbcExecuteStatementEnd();
				logSlowQuery( sql, executeStartNanos );
			}
			postExtract( rs, statement );
			return rs;
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not extract ResultSet" );
		}
	}

	@Override
	public ResultSet execute(PreparedStatement statement) {
		// sql logged by StatementPreparerImpl
		long executeStartNanos = 0;
		if ( isLogSlowQuery() ) {
			executeStartNanos = System.nanoTime();
		}
		try {
			final ResultSet rs;
			try {
				jdbcExecuteStatementStart();
				if ( !statement.execute() ) {
					while ( !statement.getMoreResults() && statement.getUpdateCount() != -1 ) {
						// do nothing until we hit the resultset
					}
				}
				rs = statement.getResultSet();
			}
			finally {
				jdbcExecuteStatementEnd();
				logSlowQuery( statement, executeStartNanos );
			}
			postExtract( rs, statement );
			return rs;
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not execute statement" );
		}
	}

	@Override
	public ResultSet execute(Statement statement, String sql) {
		sqlStatementLogger.logStatement( sql );
		long executeStartNanos = 0;
		if ( isLogSlowQuery() ) {
			executeStartNanos = System.nanoTime();
		}
		try {
			final ResultSet rs;
			try {
				jdbcExecuteStatementStart();
				if ( !statement.execute( sql ) ) {
					while ( !statement.getMoreResults() && statement.getUpdateCount() != -1 ) {
						// do nothing until we hit the resultset
					}
				}
				rs = statement.getResultSet();
			}
			finally {
				jdbcExecuteStatementEnd();
				logSlowQuery( statement, executeStartNanos );
			}
			postExtract( rs, statement );
			return rs;
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not execute statement" );
		}
	}

	@Override
	public int executeUpdate(PreparedStatement statement) {
		long executeStartNanos = 0;
		if ( isLogSlowQuery() ) {
			executeStartNanos = System.nanoTime();
		}
		try {
			jdbcExecuteStatementStart();
			return statement.executeUpdate();
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not execute statement" );
		}
		finally {
			jdbcExecuteStatementEnd();
			logSlowQuery( statement, executeStartNanos );
		}
	}

	@Override
	public int executeUpdate(Statement statement, String sql) {
		sqlStatementLogger.logStatement( sql );
		long executeStartNanos = 0;
		if ( isLogSlowQuery() ) {
			executeStartNanos = System.nanoTime();
		}
		try {
			jdbcExecuteStatementStart();
			return statement.executeUpdate( sql );
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not execute statement" );
		}
		finally {
			jdbcExecuteStatementEnd();
			logSlowQuery( statement, executeStartNanos );
		}
	}

	private void postExtract(ResultSet rs, Statement st) {
		if ( rs != null ) {
			jdbcCoordinator.getResourceRegistry().register( rs, st );
		}
	}

	private boolean isLogSlowQuery() {
		return this.sqlStatementLogger.getLogSlowQuery() > 0 || statementExecutionListener != null;
	}

	private void logSlowQuery(Statement statement, long executeStartNanos) {
		sqlStatementLogger.logSlowQuery( statement, executeStartNanos );
		if ( statementExecutionListener != null ) {
			statementExecutionListener.statementExecuted( statement.toString(), executeStartNanos );
		}
	}
	
	private void logSlowQuery(String statement, long executeStartNanos) {
		sqlStatementLogger.logSlowQuery( statement, executeStartNanos );
		if ( statementExecutionListener != null ) {
			statementExecutionListener.statementExecuted( statement, executeStartNanos );
		}
	}

}
