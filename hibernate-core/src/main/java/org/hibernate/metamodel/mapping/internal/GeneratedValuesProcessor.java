/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.InDatabaseGenerator;
import org.hibernate.loader.ast.internal.LoaderSelectBuilder;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.GeneratedValueResolver;
import org.hibernate.metamodel.mapping.InDatabaseGeneratedValueResolver;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.generator.Generator;

import static org.hibernate.sql.results.spi.ListResultsConsumer.UniqueSemantic.FILTER;

/**
 * Responsible for retrieving {@linkplain InDatabaseGenerator
 * database-generated} attribute values after an {@code insert} statement is executed.
 * <p>
 * Note that this class has responsibility for regular attributes of the entity. The
 * primary key / id attribute is handled separately, being the responsibility of an
 * instance of {@link org.hibernate.id.insert.InsertGeneratedIdentifierDelegate}.
 *
 * @see InDatabaseGenerator
 *
 * @author Steve Ebersole
 */
@Incubating
public class GeneratedValuesProcessor {
	private final SelectStatement selectStatement;
	private final List<GeneratedValueDescriptor> valueDescriptors = new ArrayList<>();
	private final List<JdbcParameter> jdbcParameters = new ArrayList<>();

	private final EntityMappingType entityDescriptor;
	private final SessionFactoryImplementor sessionFactory;

	public GeneratedValuesProcessor(
			EntityMappingType entityDescriptor,
			EventType timing,
			SessionFactoryImplementor sessionFactory) {
		this.entityDescriptor = entityDescriptor;
		this.sessionFactory = sessionFactory;

		final List<AttributeMapping> generatedValuesToSelect = getGeneratedAttributes( entityDescriptor, timing );
		if ( generatedValuesToSelect.isEmpty() ) {
			selectStatement = null;
		}
		else {
			selectStatement = LoaderSelectBuilder.createSelect(
					entityDescriptor,
					generatedValuesToSelect,
					entityDescriptor.getIdentifierMapping(),
					null,
					1,
					LoadQueryInfluencers.NONE,
					LockOptions.READ,
					jdbcParameters::add,
					sessionFactory
			);
		}
	}

	/**
	 * Find attributes generated by a {@link InDatabaseGenerator},
	 * populate the list of {@link GeneratedValueDescriptor}s by side effect, and
	 * return a list of {@link AttributeMapping}s.
	 */
	private List<AttributeMapping> getGeneratedAttributes(EntityMappingType entityDescriptor, EventType timing) {
		// todo (6.0): For now, we rely on the entity metamodel as composite attributes report
		//             GenerationTiming.NEVER even if they have attributes that would need generation
		final Generator[] generators = entityDescriptor.getEntityPersister().getEntityMetamodel().getGenerators();
		final List<AttributeMapping> generatedValuesToSelect = new ArrayList<>();
		entityDescriptor.visitAttributeMappings( mapping -> {
			final Generator generator = generators[ mapping.getStateArrayPosition() ];
			if ( generator != null
					&& generator.generatedByDatabase()
					&& generator.generatesSometimes() ) {
				// this attribute is generated for the timing we are processing...
				valueDescriptors.add( new GeneratedValueDescriptor(
						new InDatabaseGeneratedValueResolver( timing, generatedValuesToSelect.size() ),
						mapping
				) );
				generatedValuesToSelect.add( mapping );
			}
		} );
		return generatedValuesToSelect;
	}

	/**
	 * Obtain the generated values, and populate the snapshot and the fields of the entity instance.
	 */
	public void processGeneratedValues(Object entity, Object id, Object[] state, SharedSessionContractImplementor session) {
		if ( selectStatement != null ) {
			final List<Object[]> results = executeSelect( id, session );
			assert results.size() == 1;
			setEntityAttributes( entity, state, session, results.get(0) );
		}
	}

	private List<Object[]> executeSelect(Object id, SharedSessionContractImplementor session) {
		final JdbcParameterBindings jdbcParamBindings = getJdbcParameterBindings( id, session );
		final JdbcOperationQuerySelect jdbcSelect =
				sessionFactory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
						.buildSelectTranslator( sessionFactory, selectStatement )
						.translate( jdbcParamBindings, QueryOptions.NONE );
		return session.getFactory().getJdbcServices().getJdbcSelectExecutor()
				.list( jdbcSelect, jdbcParamBindings, createExecutionContext( session ), (row) -> row, FILTER );
	}

	private JdbcParameterBindings getJdbcParameterBindings(Object id, SharedSessionContractImplementor session) {
		final JdbcParameterBindings jdbcParamBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		int offset = jdbcParamBindings.registerParametersForEachJdbcValue(
				id,
				Clause.WHERE,
				entityDescriptor.getIdentifierMapping(),
				jdbcParameters,
				session
		);
		assert offset == jdbcParameters.size();
		return jdbcParamBindings;
	}

	private void setEntityAttributes(
			Object entity,
			Object[] state,
			SharedSessionContractImplementor session,
			Object[] selectionResults) {
		for ( int i = 0; i < valueDescriptors.size(); i++ ) {
			final GeneratedValueDescriptor descriptor = valueDescriptors.get( i );
			final Object generatedValue =
					descriptor.resolver.resolveGeneratedValue( selectionResults, entity, session, state[i] );
			state[ descriptor.attribute.getStateArrayPosition() ] = generatedValue;
			descriptor.attribute.getAttributeMetadataAccess()
					.resolveAttributeMetadata( entityDescriptor )
					.getPropertyAccess()
					.getSetter()
					.set( entity, generatedValue );
		}
	}

	private static ExecutionContext createExecutionContext(SharedSessionContractImplementor session) {
		return new ExecutionContext() {
			@Override
			public SharedSessionContractImplementor getSession() {
				return session;
			}

			@Override
			public QueryOptions getQueryOptions() {
				return QueryOptions.NONE;
			}

			@Override
			public String getQueryIdentifier(String sql) {
				return sql;
			}

			@Override
			public QueryParameterBindings getQueryParameterBindings() {
				return QueryParameterBindings.NO_PARAM_BINDINGS;
			}

			@Override
			public Callback getCallback() {
				throw new UnsupportedMappingException("Follow-on locking not supported yet");
			}
		};
	}

	private static class GeneratedValueDescriptor {
		public final GeneratedValueResolver resolver;
		public final AttributeMapping attribute;

		public GeneratedValueDescriptor(GeneratedValueResolver resolver, AttributeMapping attribute) {
			this.resolver = resolver;
			this.attribute = attribute;
		}
	}
}
