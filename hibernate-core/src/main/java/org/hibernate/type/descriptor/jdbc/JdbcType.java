/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.Incubating;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.query.sqm.CastType;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StringBuilderSqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.type.SqlTypes.*;

/**
 * Descriptor for the SQL/JDBC side of a value mapping. A {@code JdbcType} is
 * always coupled with a {@link JavaType} to describe the typing aspects of an
 * attribute mapping from Java to JDBC.
 * <p>
 * An instance of this type need not correspond directly to a SQL column type on
 * a particular database. Rather, a {@code JdbcType} defines how values are read
 * from and written to JDBC. Therefore, implementations of this interface map more
 * directly to the JDBC type codes defined by {@link Types} and {@link SqlTypes}.
 * <p>
 * Every {@code JdbcType} has a {@link ValueBinder} and a {@link ValueExtractor}
 * which, respectively, do the hard work of writing values to parameters of a
 * JDBC {@link java.sql.PreparedStatement}, and reading values from the columns
 * of a JDBC {@link java.sql.ResultSet}.
 * <p>
 * The {@linkplain #getJdbcTypeCode() JDBC type code} ultimately determines, in
 * collaboration with the {@linkplain org.hibernate.dialect.Dialect SQL dialect},
 * the SQL column type generated by Hibernate's schema export tool.
 * <p>
 * A JDBC type may be selected when mapping an entity attribute using the
 * {@link org.hibernate.annotations.JdbcType} annotation, or, indirectly, using
 * the {@link org.hibernate.annotations.JdbcTypeCode} annotation.
 * <p>
 * Custom implementations should be registered with the
 * {@link org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry} at startup.
 * The built-in implementations are registered automatically.
 *
 * @author Steve Ebersole
 */
public interface JdbcType extends Serializable {
	/**
	 * A "friendly" name for use in logging
	 */
	default String getFriendlyName() {
		return Integer.toString( getDefaultSqlTypeCode() );
	}

	/**
	 * The {@linkplain SqlTypes JDBC type code} used when interacting with JDBC APIs.
	 * <p>
	 * For example, it's used when calling {@link java.sql.PreparedStatement#setNull(int, int)}.
	 *
	 * @return a JDBC type code
	 */
	int getJdbcTypeCode();

	/**
	 * A {@linkplain SqlTypes JDBC type code} that identifies the SQL column type.
	 * <p>
	 * This value might be different from {@link #getDdlTypeCode()} if the actual type
	 * e.g. JSON is emulated through a type like CLOB.
	 *
	 * @return a JDBC type code
	 */
	default int getDefaultSqlTypeCode() {
		return getJdbcTypeCode();
	}

	/**
	 * A {@linkplain SqlTypes JDBC type code} that identifies the SQL column type to
	 * be used for schema generation.
	 * <p>
	 * This value is passed to {@link DdlTypeRegistry#getTypeName(int, Size, Type)}
	 * to obtain the SQL column type.
	 *
	 * @return a JDBC type code
	 * @since 6.2
	 */
	default int getDdlTypeCode() {
		return getDefaultSqlTypeCode();
	}

	default <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(
			Integer precision,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		// match legacy behavior
		return typeConfiguration.getJavaTypeRegistry().getDescriptor(
				JdbcTypeJavaClassMappings.INSTANCE.determineJavaClassForJdbcTypeCode( getDefaultSqlTypeCode() )
		);
	}

	/**
	 * Obtain a {@linkplain JdbcLiteralFormatter formatter} object capable of rendering
	 * values of the given {@linkplain JavaType Java type} as SQL literals of the type
	 * represented by this object.
	 */
	// todo (6.0) : move to {@link org.hibernate.metamodel.mapping.JdbcMapping}?
	default <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return (appender, value, dialect, wrapperOptions) -> appender.appendSql( value.toString() );
	}

	/**
	 * Obtain a {@linkplain ValueBinder binder} object capable of binding values of the
	 * given {@linkplain JavaType Java type} to parameters of a JDBC
	 * {@link java.sql.PreparedStatement}.
	 *
	 * @param javaType The descriptor describing the types of Java values to be bound
	 *
	 * @return The appropriate binder.
	 */
	<X> ValueBinder<X> getBinder(JavaType<X> javaType);

	/**
	 * Obtain an {@linkplain ValueExtractor extractor} object capable of extracting
	 * values of the given {@linkplain JavaType Java type} from a JDBC
	 * {@link java.sql.ResultSet}.
	 *
	 * @param javaType The descriptor describing the types of Java values to be extracted
	 *
	 * @return The appropriate extractor
	 */
	<X> ValueExtractor<X> getExtractor(JavaType<X> javaType);

	/**
	 * The Java type class that is preferred by the binder or null.
	 */
	@Incubating
	default Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return null;
	}

	/**
	 * The check constraint that should be added to the column
	 * definition in generated DDL.
	 *
	 * @param columnName the name of the column
	 * @param javaType   the {@link JavaType} of the mapped column
	 * @param converter  the converter, if any, or null
	 * @param dialect    the SQL {@link Dialect}
	 * @return a check constraint condition or null
	 * @since 6.2
	 */
	default String getCheckCondition(String columnName, JavaType<?> javaType, BasicValueConverter<?, ?> converter, Dialect dialect) {
		return null;
	}

	/**
	 * Wraps the top level selection expression to be able to read values with this JdbcType's ValueExtractor.
	 * @since 6.2
	 */
	@Incubating
	default Expression wrapTopLevelSelectionExpression(Expression expression) {
		return expression;
	}

	/**
	 * Wraps the write expression to be able to write values with this JdbcType's ValueBinder.
	 * @since 6.2
	 */
	@Incubating
	default String wrapWriteExpression(String writeExpression, Dialect dialect) {
		final StringBuilder sb = new StringBuilder( writeExpression.length() );
		appendWriteExpression( writeExpression, new StringBuilderSqlAppender( sb ), dialect );
		return sb.toString();
	}

	/**
	 * Append the write expression wrapped in a way to be able to write values with this JdbcType's ValueBinder.
	 * @since 6.2
	 */
	@Incubating
	default void appendWriteExpression(String writeExpression, SqlAppender appender, Dialect dialect) {
		appender.append( writeExpression );
	}

	default boolean isInteger() {
		int typeCode = getDdlTypeCode();
		return isIntegral(typeCode)
			|| typeCode == BIT; //HIGHLY DUBIOUS!
	}

	default boolean isFloat() {
		return isFloatOrRealOrDouble( getDdlTypeCode() );
	}

	default boolean isDecimal() {
		return isNumericOrDecimal( getDdlTypeCode() );
	}

	default boolean isNumber() {
		return isNumericType( getDdlTypeCode() );
	}

	default boolean isBinary() {
		return isBinaryType( getDdlTypeCode() );
	}

	default boolean isString() {
		return isCharacterOrClobType( getDdlTypeCode() );
	}

	default boolean isStringLike() {
		int ddlTypeCode = getDdlTypeCode();
		return isCharacterOrClobType( ddlTypeCode )
			|| isEnumType( ddlTypeCode );
	}

	default boolean isTemporal() {
		return isTemporalType( getDdlTypeCode() );
	}

	default boolean isLob() {
		return isLob( getDdlTypeCode() );
	}

	static boolean isLob(int jdbcTypeCode) {
		switch ( jdbcTypeCode ) {
			case BLOB:
			case CLOB:
			case NCLOB: {
				return true;
			}
		}
		return false;
	}

	default boolean isLobOrLong() {
		return isLobOrLong( getDdlTypeCode() );
	}

	static boolean isLobOrLong(int jdbcTypeCode) {
		switch ( jdbcTypeCode ) {
			case BLOB:
			case CLOB:
			case NCLOB:
			case LONG32VARBINARY:
			case LONG32VARCHAR:
			case LONG32NVARCHAR: {
				return true;
			}
		}
		return false;
	}

	default boolean isNationalized() {
		return isNationalized( getDdlTypeCode() );
	}

	static boolean isNationalized(int jdbcTypeCode) {
		switch ( jdbcTypeCode ) {
			case NCHAR:
			case NVARCHAR:
			case LONGNVARCHAR:
			case LONG32NVARCHAR:
			case NCLOB: {
				return true;
			}
		}
		return false;
	}

	default boolean isInterval() {
		return isIntervalType( getDdlTypeCode() );
	}

	default boolean isDuration() {
		final int ddlTypeCode = getDefaultSqlTypeCode();
		return isDurationType( ddlTypeCode )
				|| isIntervalType( ddlTypeCode );
	}

	default CastType getCastType() {
		return getCastType( getDdlTypeCode() );
	}

	static CastType getCastType(int typeCode) {
		switch ( typeCode ) {
			case INTEGER:
			case TINYINT:
			case SMALLINT:
				return CastType.INTEGER;
			case BIGINT:
				return CastType.LONG;
			case FLOAT:
			case REAL:
				return CastType.FLOAT;
			case DOUBLE:
				return CastType.DOUBLE;
			case CHAR:
			case NCHAR:
			case VARCHAR:
			case NVARCHAR:
			case LONGVARCHAR:
			case LONGNVARCHAR:
				return CastType.STRING;
			case CLOB:
				return CastType.CLOB;
			case BOOLEAN:
				return CastType.BOOLEAN;
			case DECIMAL:
			case NUMERIC:
				return CastType.FIXED;
			case DATE:
				return CastType.DATE;
			case TIME:
			case TIME_UTC:
			case TIME_WITH_TIMEZONE:
				return CastType.TIME;
			case TIMESTAMP:
				return CastType.TIMESTAMP;
			case TIMESTAMP_WITH_TIMEZONE:
				return CastType.OFFSET_TIMESTAMP;
			case NULL:
				return CastType.NULL;
			default:
				return CastType.OTHER;
		}
	}

	/**
	 * Register the {@code OUT} parameter on the {@link CallableStatement} with the given name for this {@linkplain JdbcType}.
	 * @since 6.2
	 */
	default void registerOutParameter(CallableStatement callableStatement, String name) throws SQLException {
		callableStatement.registerOutParameter( name, getJdbcTypeCode() );
	}

	/**
	 * Register the {@code OUT} parameter on the {@link CallableStatement} with the given index for this {@linkplain JdbcType}.
	 * @since 6.2
	 */
	default void registerOutParameter(CallableStatement callableStatement, int index) throws SQLException {
		callableStatement.registerOutParameter( index, getJdbcTypeCode() );
	}

	@Incubating
	default void addAuxiliaryDatabaseObjects(
			JavaType<?> javaType,
			Size columnSize,
			Database database,
			TypeConfiguration typeConfiguration) {
	}

	@Incubating
	default String getExtraCreateTableInfo(JavaType<?> javaType, String columnName, String tableName, Database database) {
		return "";
	}
}
