/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.produce.function;

import org.hibernate.HibernateException;
import org.hibernate.QueryException;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmCollation;
import org.hibernate.query.sqm.tree.expression.SqmDurationUnit;
import org.hibernate.query.sqm.tree.expression.SqmExtractUnit;
import org.hibernate.query.sqm.tree.expression.SqmTrimSpecification;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.spi.AbstractSqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JdbcTypeRecommendationException;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

import java.lang.reflect.Type;
import java.sql.Types;
import java.util.List;

import static org.hibernate.type.SqlTypes.BIT;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.hasDatePart;
import static org.hibernate.type.SqlTypes.hasTimePart;
import static org.hibernate.type.SqlTypes.isCharacterOrClobType;
import static org.hibernate.type.SqlTypes.isCharacterType;
import static org.hibernate.type.SqlTypes.isIntegral;
import static org.hibernate.type.SqlTypes.isNumericType;
import static org.hibernate.type.SqlTypes.isTemporalType;


/**
 * Typechecks the arguments of HQL functions based on the assigned JDBC types.
 *
 * @apiNote Originally, the main purpose for doing this was that we wanted to be
 *          able to check named queries at startup or build time, and we wanted
 *          to be able to check all queries in the IDE. But since Hibernate 6
 *          it's of more general importance.
 *
 * @implNote Access to the {@link MappingMetamodel} is very problematic here,
 *           since we are sometimes called in a context where we have not built
 *           a {@link org.hibernate.internal.SessionFactoryImpl}, and therefore
 *           we have no persisters.
 *
 * @author Gavin King
 */
public class ArgumentTypesValidator implements ArgumentsValidator {
	// a JDBC type code of an enum when we don't know if it's mapped STRING or ORDINAL
	// this number has to be distinct from every code in SqlTypes!
	private static final int ENUM_UNKNOWN_JDBC_TYPE = -101977;

	final ArgumentsValidator delegate;
	private final FunctionParameterType[] types;

	public ArgumentTypesValidator(ArgumentsValidator delegate, FunctionParameterType... types) {
		this.types = types;
		if ( delegate == null ) {
			delegate = StandardArgumentsValidators.exactly(types.length);
		}
		this.delegate = delegate;
	}

	/**
	 * We do an initial validation phase with just the SQM tree, even though we don't
	 * have all typing information available here (in particular, we don't have the
	 * final JDBC type codes for things with converters) because this is the phase
	 * that is run at startup for named queries, and can be done in an IDE.
	 */
	@Override
	public void validate(
			List<? extends SqmTypedNode<?>> arguments,
			String functionName,
			TypeConfiguration typeConfiguration) {
		delegate.validate( arguments, functionName, typeConfiguration);
		int count = 0;
		for (SqmTypedNode<?> argument : arguments) {
			JdbcTypeIndicators indicators = typeConfiguration.getCurrentBaseSqlTypeIndicators();
			SqmExpressible<?> nodeType = argument.getNodeType();
			FunctionParameterType type = count < types.length ? types[count++] : types[types.length - 1];
			if ( nodeType != null ) {
				JavaType<?> javaType = nodeType.getExpressibleJavaType();
				if (javaType != null) {
					try {
						checkType(
								count, functionName, type,
								getJdbcType( typeConfiguration, argument, indicators, javaType ),
								javaType.getJavaTypeClass()
						);
					}
					catch (JdbcTypeRecommendationException e) {
						// it's a converter or something like that, and we will check it later
					}
				}
				switch (type) {
					case TEMPORAL_UNIT:
						if ( !(argument instanceof SqmExtractUnit) && !(argument instanceof SqmDurationUnit) ) {
							throwError(type, Object.class, functionName, count);
						}
						break;
					// the following are not really necessary for the functions we have today
					// since collations and trim specifications have special parser rules,
					// but at the very least this is an assertion that we don't get given
					// something crazy by the parser
					case TRIM_SPEC:
						if ( !(argument instanceof SqmTrimSpecification) ) {
							throwError(type, Object.class, functionName, count);
						}
						break;
					case COLLATION:
						if ( !(argument instanceof SqmCollation) ) {
							throwError(type, Object.class, functionName, count);
						}
						break;
				}
			}
			else {
				//TODO: appropriate error?
			}
		}
	}

	private int getJdbcType(
			TypeConfiguration typeConfiguration,
			SqmTypedNode<?> argument,
			JdbcTypeIndicators indicators,
			JavaType<?> javaType) {
		// For enum types, we must try to resolve the JdbcMapping of a possible path
		// to be sure we use the correct JdbcType for the validation
		final JdbcType jdbcType;
		if ( javaType.getJavaTypeClass().isEnum() ) {
			// we can't tell from the enum class whether it is mapped
			// as STRING or ORDINAL, we have to look at the entity
			// attribute it belongs to when it occurs as a path expression
			final JdbcMapping mapping = getJdbcMappingForEnum( argument, typeConfiguration );
			if ( mapping == null ) {
				jdbcType = javaType.getRecommendedJdbcType( indicators );
			}
			else {
				// indicates that we don't know if its STRING or ORDINAL
				return ENUM_UNKNOWN_JDBC_TYPE;
			}
		}
		else {
			jdbcType = javaType.getRecommendedJdbcType( indicators );
		}
		return jdbcType.getDefaultSqlTypeCode();
	}

	private JdbcMapping getJdbcMappingForEnum(SqmTypedNode<?> argument, TypeConfiguration typeConfiguration) {
		if ( argument instanceof SqmPath<?> ) {
			final SqmPath<?> path = (SqmPath<?>) argument;
			final ModelPartContainer modelPartContainer = getModelPartContainer( path.getLhs(), typeConfiguration );
			final ModelPart part = modelPartContainer.findSubPart( path.getReferencedPathSource().getPathName(), null );
			return part.getJdbcMappings().get( 0 );
		}
		return null;
	}

	/**
	 * @implNote Accesses the given {@link MappingMetamodel}, which is problematic.
	 */
	private ModelPartContainer getModelPartContainer(SqmPath<?> path, TypeConfiguration typeConfiguration) {
		final SqmPath<?> lhs = path.getLhs();
		if ( lhs == null ) {
			assert path instanceof SqmFrom<?, ?>;
			final EntityDomainType<?> entityDomainType = (EntityDomainType<?>) path.getNodeType().getSqmPathType();
			final MappingMetamodelImplementor mappingMetamodel;
			try {
				mappingMetamodel = typeConfiguration.getSessionFactory().getMappingMetamodel();
			}
			catch (HibernateException he) {
				// we don't have a SessionFactory, or a MappingMetamodel
				return null;
			}
			return mappingMetamodel.getEntityDescriptor( entityDomainType.getHibernateEntityName() );
		}
		else {
			final ModelPartContainer modelPartContainer = getModelPartContainer( lhs, typeConfiguration );
			return (ModelPartContainer) modelPartContainer.findSubPart( path.getReferencedPathSource().getPathName(), null );
		}
	}

	/**
	 * This is the final validation phase with the fully-typed SQL nodes. Note that these
	 * checks are much less useful, occurring "too late", right before we execute the
	 * query and get an error from the database. However, they help in the sense of (a)
	 * resulting in more consistent/understandable error messages, and (b) protecting the
	 * user from writing queries that depend on generally-unportable implicit type
	 * conversions happening at the database level. (Implicit type conversions between
	 * numeric types are portable, and are not prohibited here.)
	 */
	@Override
	public void validateSqlTypes(List<? extends SqlAstNode> arguments, String functionName) {
		int count = 0;
		for (SqlAstNode argument : arguments) {
			if (argument instanceof Expression) {
				JdbcMappingContainer expressionType = ((Expression) argument).getExpressionType();
				if (expressionType != null) {
					ParameterDetector detector = new ParameterDetector();
					argument.accept(detector);
					if (detector.detected) {
						count += expressionType.getJdbcTypeCount();
					}
					else {
						count = validateArgument(count, expressionType, functionName);
					}
				}
			}
		}
	}

	private int validateArgument(int count, JdbcMappingContainer expressionType, String functionName) {
		List<JdbcMapping> mappings = expressionType.getJdbcMappings();
		for (JdbcMapping mapping : mappings) {
			FunctionParameterType type = count < types.length ? types[count++] : types[types.length - 1];
			if (type != null) {
				checkType(
						count, functionName, type,
						mapping.getJdbcType().getDefaultSqlTypeCode(),
						mapping.getJavaTypeDescriptor().getJavaType()
				);
			}
		}
		return count;
	}

	private void checkType(int count, String functionName, FunctionParameterType type, int code, Type javaType) {
		switch (type) {
			case COMPARABLE:
				if ( !isCharacterType(code) && !isTemporalType(code) && !isNumericType(code) && code != UUID ) {
					if ( javaType == java.util.UUID.class && ( code == Types.BINARY || isCharacterType( code ) ) ) {
						// We also consider UUID to be comparable when it's a character or binary type
						return;
					}
					throwError(type, javaType, functionName, count);
				}
				break;
			case STRING:
				if ( !isCharacterType(code) && code != ENUM_UNKNOWN_JDBC_TYPE ) {
					throwError(type, javaType, functionName, count);
				}
				break;
			case STRING_OR_CLOB:
				if ( !isCharacterOrClobType(code) ) {
					throwError(type, javaType, functionName, count);
				}
				break;
			case NUMERIC:
				if ( !isNumericType(code) ) {
					throwError(type, javaType, functionName, count);
				}
				break;
			case INTEGER:
				if ( !isIntegral(code) && code != ENUM_UNKNOWN_JDBC_TYPE ) {
					throwError(type, javaType, functionName, count);
				}
				break;
			case BOOLEAN:
				// ugh, need to be careful here, need to accept all the
				// JDBC type codes that a Dialect might use for BOOLEAN
				if ( code != BOOLEAN && code != BIT && code != TINYINT && code != SMALLINT ) {
					throwError(type, javaType, functionName, count);
				}
				break;
			case TEMPORAL:
				if ( !isTemporalType(code) ) {
					throwError(type, javaType, functionName, count);
				}
				break;
			case DATE:
				if ( !hasDatePart(code) ) {
					throwError(type, javaType, functionName, count);
				}
				break;
			case TIME:
				if ( !hasTimePart(code) ) {
					throwError(type, javaType, functionName, count);
				}
				break;
		}
	}

	private void throwError(FunctionParameterType type, Type javaType, String functionName, int count) {
		throw new QueryException(
				String.format(
						"Parameter %d of function %s() has type %s, but argument is of type %s",
						count,
						functionName,
						type,
						javaType.getTypeName()
				)
		);
	}

	private static class ParameterDetector extends AbstractSqlAstWalker {
		private boolean detected;
		@Override
		public void visitParameter(JdbcParameter jdbcParameter) {
			detected = true;
		}
	}

	@Override
	public String getSignature() {
		String sig = delegate.getSignature();
		for (int i=0; i<types.length; i++) {
			String argName = types.length == 1 ? "arg" : "arg" + i;
			sig = sig.replace(argName, types[i] + " " + argName);
		}
		return sig;
	}
}
