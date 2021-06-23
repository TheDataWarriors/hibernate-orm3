/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;
import java.util.Map;

import javax.persistence.TemporalType;

import org.hibernate.LockOptions;
import org.hibernate.dialect.function.QuantifiedLeastGreatestEmulation;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.TopLimitHandler;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.sql.ForUpdateFragment;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.Sybase11JoinFragment;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.TimestampTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.TinyIntTypeDescriptor;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;

/**
 * Dialect for Sybase Adaptive Server Enterprise for
 * Sybase 11.9.2 and above.
 */
public class SybaseASEDialect extends SybaseDialect {

	private final SizeStrategy sizeStrategy;

	public SybaseASEDialect() {
		this( 1100, false );
	}

	public SybaseASEDialect(DialectResolutionInfo info) {
		this(
				info.getDatabaseMajorVersion() * 100 + info.getDatabaseMinorVersion() * 10,
				info.getDriverName() != null && info.getDriverName().contains( "jTDS" )
		);
	}

	public SybaseASEDialect(int version, boolean jtdsDriver) {
		super( version, jtdsDriver );

		//On Sybase ASE, the 'bit' type cannot be null,
		//and cannot have indexes (while we don't use
		//tinyint to store signed bytes, we can use it
		//to store boolean values)
		registerColumnType( Types.BOOLEAN, "tinyint" );


		if ( getVersion() >= 1200 ) {
			//date / date were introduced in version 12
			registerColumnType( Types.DATE, "date" );
			registerColumnType( Types.TIME, "time" );
			if ( getVersion() >= 1500 ) {
				//bigint was added in version 15
				registerColumnType( Types.BIGINT, "bigint" );

				if ( getVersion() >= 1550 && !jtdsDriver ) {
					//According to Wikipedia bigdatetime and bigtime were added in 15.5
					//But with jTDS we can't use them as the driver can't handle the types
					registerColumnType( Types.DATE, "bigdatetime" );
					registerColumnType( Types.DATE, 3, "datetime" );
					registerColumnType( Types.TIME, "bigtime" );
					registerColumnType( Types.TIME, 3, "datetime" );
					registerColumnType( Types.TIMESTAMP, "bigdatetime" );
					registerColumnType( Types.TIMESTAMP, 3, "datetime" );
					registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "bigdatetime" );
					registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, 3, "datetime" );
				}
			}
		}

		// the maximum length of a VARCHAR or VARBINARY
		// depends on the page size, and also on the
		// version of Sybase ASE, and can be quite small,
		// so use 'image' and 'text' as the "long" types
		registerColumnType( Types.LONGVARBINARY, "image" );
		registerColumnType( Types.LONGVARCHAR, "text" );

		registerSybaseKeywords();
		sizeStrategy = new SizeStrategyImpl() {
			@Override
			public Size resolveSize(
					JdbcTypeDescriptor jdbcType,
					JavaTypeDescriptor javaType,
					Integer precision,
					Integer scale,
					Long length) {
				final int jdbcTypeCode = jdbcType.getJdbcType();
				switch ( jdbcTypeCode ) {
					case Types.FLOAT:
						// Sybase ASE allows FLOAT with a precision up to 48
						if ( precision != null ) {
							return Size.precision( Math.min( Math.max( precision, 1 ), 48 ) );
						}
				}
				return super.resolveSize( jdbcType, javaType, precision, scale, length );
			}
		};
	}

	@Override
	public int getDoublePrecision() {
		return 48;
	}

	@Override
	public SizeStrategy getSizeStrategy() {
		return sizeStrategy;
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new SybaseASESqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	/**
	 * The Sybase ASE {@code BIT} type does not allow
	 * null values, so we don't use it.
	 *
	 * @return false
	 */
	@Override
	public boolean supportsBitType() {
		return false;
	}

	@Override
	protected JdbcTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
		switch ( sqlCode ) {
			case Types.BOOLEAN:
				return TinyIntTypeDescriptor.INSTANCE;
			case Types.TIMESTAMP_WITH_TIMEZONE:
				// At least the jTDS driver does not support this type code
				if ( jtdsDriver ) {
					return TimestampTypeDescriptor.INSTANCE;
				}
			default:
				return super.getSqlTypeDescriptorOverride( sqlCode );
		}
	}

	@Override
	public String currentDate() {
		return "current_date()";
	}

	@Override
	public String currentTime() {
		return "current_time()";
	}

	@Override
	public String currentTimestamp() {
		return "current_bigdatetime()";
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType) {
		//TODO!!
		switch ( unit ) {
			case NANOSECOND:
			case NATIVE:
				// If the driver or database do not support bigdatetime and bigtime types,
				// we try to operate on milliseconds instead
				if ( getVersion() < 1550 || jtdsDriver ) {
					return "dateadd(millisecond, ?2/1000000, ?3)";
				}
				else {
					return "dateadd(mcs, ?2/1000, ?3)";
				}
			default:
				return "dateadd(?1, ?2, ?3)";
		}
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		//TODO!!
		switch ( unit ) {
			case NANOSECOND:
			case NATIVE:
				return "(datediff(mcs, ?2, ?3)*1000)";
			default:
				return "datediff(?1, ?2, ?3)";
		}
	}

	private void registerSybaseKeywords() {
		registerKeyword( "add" );
		registerKeyword( "all" );
		registerKeyword( "alter" );
		registerKeyword( "and" );
		registerKeyword( "any" );
		registerKeyword( "arith_overflow" );
		registerKeyword( "as" );
		registerKeyword( "asc" );
		registerKeyword( "at" );
		registerKeyword( "authorization" );
		registerKeyword( "avg" );
		registerKeyword( "begin" );
		registerKeyword( "between" );
		registerKeyword( "break" );
		registerKeyword( "browse" );
		registerKeyword( "bulk" );
		registerKeyword( "by" );
		registerKeyword( "cascade" );
		registerKeyword( "case" );
		registerKeyword( "char_convert" );
		registerKeyword( "check" );
		registerKeyword( "checkpoint" );
		registerKeyword( "close" );
		registerKeyword( "clustered" );
		registerKeyword( "coalesce" );
		registerKeyword( "commit" );
		registerKeyword( "compute" );
		registerKeyword( "confirm" );
		registerKeyword( "connect" );
		registerKeyword( "constraint" );
		registerKeyword( "continue" );
		registerKeyword( "controlrow" );
		registerKeyword( "convert" );
		registerKeyword( "count" );
		registerKeyword( "count_big" );
		registerKeyword( "create" );
		registerKeyword( "current" );
		registerKeyword( "cursor" );
		registerKeyword( "database" );
		registerKeyword( "dbcc" );
		registerKeyword( "deallocate" );
		registerKeyword( "declare" );
		registerKeyword( "decrypt" );
		registerKeyword( "default" );
		registerKeyword( "delete" );
		registerKeyword( "desc" );
		registerKeyword( "determnistic" );
		registerKeyword( "disk" );
		registerKeyword( "distinct" );
		registerKeyword( "drop" );
		registerKeyword( "dummy" );
		registerKeyword( "dump" );
		registerKeyword( "else" );
		registerKeyword( "encrypt" );
		registerKeyword( "end" );
		registerKeyword( "endtran" );
		registerKeyword( "errlvl" );
		registerKeyword( "errordata" );
		registerKeyword( "errorexit" );
		registerKeyword( "escape" );
		registerKeyword( "except" );
		registerKeyword( "exclusive" );
		registerKeyword( "exec" );
		registerKeyword( "execute" );
		registerKeyword( "exist" );
		registerKeyword( "exit" );
		registerKeyword( "exp_row_size" );
		registerKeyword( "external" );
		registerKeyword( "fetch" );
		registerKeyword( "fillfactor" );
		registerKeyword( "for" );
		registerKeyword( "foreign" );
		registerKeyword( "from" );
		registerKeyword( "goto" );
		registerKeyword( "grant" );
		registerKeyword( "group" );
		registerKeyword( "having" );
		registerKeyword( "holdlock" );
		registerKeyword( "identity" );
		registerKeyword( "identity_gap" );
		registerKeyword( "identity_start" );
		registerKeyword( "if" );
		registerKeyword( "in" );
		registerKeyword( "index" );
		registerKeyword( "inout" );
		registerKeyword( "insensitive" );
		registerKeyword( "insert" );
		registerKeyword( "install" );
		registerKeyword( "intersect" );
		registerKeyword( "into" );
		registerKeyword( "is" );
		registerKeyword( "isolation" );
		registerKeyword( "jar" );
		registerKeyword( "join" );
		registerKeyword( "key" );
		registerKeyword( "kill" );
		registerKeyword( "level" );
		registerKeyword( "like" );
		registerKeyword( "lineno" );
		registerKeyword( "load" );
		registerKeyword( "lock" );
		registerKeyword( "materialized" );
		registerKeyword( "max" );
		registerKeyword( "max_rows_per_page" );
		registerKeyword( "min" );
		registerKeyword( "mirror" );
		registerKeyword( "mirrorexit" );
		registerKeyword( "modify" );
		registerKeyword( "national" );
		registerKeyword( "new" );
		registerKeyword( "noholdlock" );
		registerKeyword( "nonclustered" );
		registerKeyword( "nonscrollable" );
		registerKeyword( "non_sensitive" );
		registerKeyword( "not" );
		registerKeyword( "null" );
		registerKeyword( "nullif" );
		registerKeyword( "numeric_truncation" );
		registerKeyword( "of" );
		registerKeyword( "off" );
		registerKeyword( "offsets" );
		registerKeyword( "on" );
		registerKeyword( "once" );
		registerKeyword( "online" );
		registerKeyword( "only" );
		registerKeyword( "open" );
		registerKeyword( "option" );
		registerKeyword( "or" );
		registerKeyword( "order" );
		registerKeyword( "out" );
		registerKeyword( "output" );
		registerKeyword( "over" );
		registerKeyword( "artition" );
		registerKeyword( "perm" );
		registerKeyword( "permanent" );
		registerKeyword( "plan" );
		registerKeyword( "prepare" );
		registerKeyword( "primary" );
		registerKeyword( "print" );
		registerKeyword( "privileges" );
		registerKeyword( "proc" );
		registerKeyword( "procedure" );
		registerKeyword( "processexit" );
		registerKeyword( "proxy_table" );
		registerKeyword( "public" );
		registerKeyword( "quiesce" );
		registerKeyword( "raiserror" );
		registerKeyword( "read" );
		registerKeyword( "readpast" );
		registerKeyword( "readtext" );
		registerKeyword( "reconfigure" );
		registerKeyword( "references" );
		registerKeyword( "remove" );
		registerKeyword( "reorg" );
		registerKeyword( "replace" );
		registerKeyword( "replication" );
		registerKeyword( "reservepagegap" );
		registerKeyword( "return" );
		registerKeyword( "returns" );
		registerKeyword( "revoke" );
		registerKeyword( "role" );
		registerKeyword( "rollback" );
		registerKeyword( "rowcount" );
		registerKeyword( "rows" );
		registerKeyword( "rule" );
		registerKeyword( "save" );
		registerKeyword( "schema" );
		registerKeyword( "scroll" );
		registerKeyword( "scrollable" );
		registerKeyword( "select" );
		registerKeyword( "semi_sensitive" );
		registerKeyword( "set" );
		registerKeyword( "setuser" );
		registerKeyword( "shared" );
		registerKeyword( "shutdown" );
		registerKeyword( "some" );
		registerKeyword( "statistics" );
		registerKeyword( "stringsize" );
		registerKeyword( "stripe" );
		registerKeyword( "sum" );
		registerKeyword( "syb_identity" );
		registerKeyword( "syb_restree" );
		registerKeyword( "syb_terminate" );
		registerKeyword( "top" );
		registerKeyword( "table" );
		registerKeyword( "temp" );
		registerKeyword( "temporary" );
		registerKeyword( "textsize" );
		registerKeyword( "to" );
		registerKeyword( "tracefile" );
		registerKeyword( "tran" );
		registerKeyword( "transaction" );
		registerKeyword( "trigger" );
		registerKeyword( "truncate" );
		registerKeyword( "tsequal" );
		registerKeyword( "union" );
		registerKeyword( "unique" );
		registerKeyword( "unpartition" );
		registerKeyword( "update" );
		registerKeyword( "use" );
		registerKeyword( "user" );
		registerKeyword( "user_option" );
		registerKeyword( "using" );
		registerKeyword( "values" );
		registerKeyword( "varying" );
		registerKeyword( "view" );
		registerKeyword( "waitfor" );
		registerKeyword( "when" );
		registerKeyword( "where" );
		registerKeyword( "while" );
		registerKeyword( "with" );
		registerKeyword( "work" );
		registerKeyword( "writetext" );
		registerKeyword( "xmlextract" );
		registerKeyword( "xmlparse" );
		registerKeyword( "xmltest" );
		registerKeyword( "xmlvalidate" );
	}

// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


	@Override
	@SuppressWarnings("deprecation")
	public JoinFragment createOuterJoinFragment() {
		return getVersion() < 1400
				? new Sybase11JoinFragment()
				: super.createOuterJoinFragment();
	}

	@Override
	public boolean supportsCascadeDelete() {
		return false;
	}

	@Override
	public int getMaxAliasLength() {
		return 30;
	}

	/**
	 * By default, Sybase string comparisons are case-insensitive.
	 * <p/>
	 * If the DB is configured to be case-sensitive, then this return
	 * value will be incorrect.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public boolean areStringComparisonsCaseInsensitive() {
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public String getCurrentTimestampSQLFunctionName() {
		return "getdate()";
	}

	@Override
	public String getCrossJoinSeparator() {
		return ", ";
	}

	@Override
	public boolean supportsValuesListForInsert() {
		return false;
	}

	@Override
	public boolean supportsLockTimeouts() {
		return false;
	}

	@Override
	public boolean supportsOrderByInSubquery() {
		return false;
	}

	@Override
	public boolean supportsUnionInSubquery() {
		// At least not according to HHH-3637
		return false;
	}

	@Override
	public boolean supportsPartitionBy() {
		return false;
	}

	@Override
	public String getTableTypeString() {
		//HHH-7298 I don't know if this would break something or cause some side affects
		//but it is required to use 'select for update'
		return getVersion() < 1570 ? super.getTableTypeString() : " lock datarows";
	}

	@Override
	public boolean supportsExpectedLobUsagePattern() {
		// Earlier Sybase did not support LOB locators at all
		return getVersion() >= 1570;
	}

	@Override
	public boolean supportsLobValueChangePropagation() {
		return false;
	}

	@Override
	public boolean forUpdateOfColumns() {
		return getVersion() >= 1570;
	}

	@Override
	public RowLockStrategy getWriteRowLockStrategy() {
		return getVersion() >= 1570 ? RowLockStrategy.COLUMN : RowLockStrategy.TABLE;
	}

	@Override
	public String getForUpdateString() {
		return getVersion() < 1570 ? "" : " for update";
	}

	@Override
	public String getForUpdateString(String aliases) {
		return getVersion() < 1570
				? ""
				: getForUpdateString() + " of " + aliases;
	}

	@Override
	public String appendLockHint(LockOptions mode, String tableName) {
		//TODO: is this really necessary??!
		return getVersion() < 1570 ? super.appendLockHint( mode, tableName ) : tableName;
	}

	@Override
	@SuppressWarnings("deprecation")
	public String applyLocksToSql(String sql, LockOptions aliasedLockOptions, Map<String, String[]> keyColumnNames) {
		//TODO: is this really correct?
		return getVersion() < 1570
				? super.applyLocksToSql( sql, aliasedLockOptions, keyColumnNames )
				: sql + new ForUpdateFragment( this, aliasedLockOptions, keyColumnNames ).toFragmentString();
	}

	@Override
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	/**
	 * Constraint-name extractor for Sybase ASE constraint violation exceptions.
	 * Orginally contributed by Denny Bartelt.
	 */
	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				final int errorCode = JdbcExceptionHelper.extractErrorCode( sqle );
				switch ( JdbcExceptionHelper.extractSqlState( sqle ) ) {
					// UNIQUE VIOLATION
					case "S1000":
						if (2601 == errorCode) {
							return extractUsingTemplate( "with unique index '", "'", sqle.getMessage() );
						}
						break;
					case "23000":
						if (546 == errorCode) {
							// Foreign key violation
							return extractUsingTemplate( "constraint name = '", "'", sqle.getMessage() );
						}
						break;
//					// FOREIGN KEY VIOLATION
//					case 23503:
//						return extractUsingTemplate( "violates foreign key constraint \"","\"", sqle.getMessage() );
//					// NOT NULL VIOLATION
//					case 23502:
//						return extractUsingTemplate( "null value in column \"","\" violates not-null constraint", sqle.getMessage() );
//					// TODO: RESTRICT VIOLATION
//					case 23001:
//						return null;
					// ALL OTHER
					default:
						return null;
				}
				return null;
			} );

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		if ( getVersion() < 1570 ) {
			return null;
		}

		return (sqlException, message, sql) -> {
			final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );
			final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );
			switch ( sqlState ) {
				case "JZ0TO":
				case "JZ006":
					throw new LockTimeoutException( message, sqlException, sql );
				case "S1000":
					switch ( errorCode ) {
						case 515:
							// Attempt to insert NULL value into column; column does not allow nulls.
						case 2601:
							// Unique constraint violation
							final String constraintName = getViolatedConstraintNameExtractor().extractConstraintName( sqlException );
							return new ConstraintViolationException( message, sqlException, sql, constraintName );
					}
					break;
				case "ZZZZZ":
					if (515 == errorCode) {
						// Attempt to insert NULL value into column; column does not allow nulls.
						final String constraintName = getViolatedConstraintNameExtractor().extractConstraintName( sqlException );
						return new ConstraintViolationException( message, sqlException, sql, constraintName );
					}
					break;
				case "23000":
					if (546 == errorCode) {
						// Foreign key violation
						final String constraintName = getViolatedConstraintNameExtractor().extractConstraintName( sqlException );
						return new ConstraintViolationException( message, sqlException, sql, constraintName );
					}
					break;
			}
			return null;
		};
	}

	@Override
	public LimitHandler getLimitHandler() {
		if ( getVersion() < 1250 ) {
			//support for SELECT TOP was introduced in Sybase ASE 12.5.3
			return super.getLimitHandler();
		}
		return new TopLimitHandler(false);
	}
}
