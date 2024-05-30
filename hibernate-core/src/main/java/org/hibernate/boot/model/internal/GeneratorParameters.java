/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.Internal;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.LegacyNamingStrategy;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.SingleNamingStrategy;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.util.Map;
import java.util.Properties;

/**
 * Responsible for setting up the parameters which are passed to
 * {@link Configurable#configure(Type, Properties, ServiceRegistry)}
 * when a {@link Configurable} generator is instantiated.
 *
 * @since 7.0
 *
 * @author Gavin King
 */
public class GeneratorParameters {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( GeneratorBinder.class );

	/**
	 * Collect the parameters which should be passed to
	 * {@link Configurable#configure(Type, Properties, ServiceRegistry)}.
	 */
	public static Properties collectParameters(
			SimpleValue identifierValue,
			Dialect dialect,
			RootClass rootClass,
			Map<String, Object> configuration) {
		final ConfigurationService configService =
				identifierValue.getMetadata().getMetadataBuildingOptions().getServiceRegistry()
						.requireService( ConfigurationService.class );

		final Properties params = new Properties();

		// default initial value and allocation size per-JPA defaults
		params.setProperty( OptimizableGenerator.INITIAL_PARAM,
				String.valueOf( OptimizableGenerator.DEFAULT_INITIAL_VALUE ) );

		params.setProperty( OptimizableGenerator.INCREMENT_PARAM,
				String.valueOf( defaultIncrement( configService ) ) );
		//init the table here instead of earlier, so that we can get a quoted table name
		//TODO: would it be better to simply pass the qualified table name, instead of
		//	  splitting it up into schema/catalog/table names
		final String tableName = identifierValue.getTable().getQuotedName( dialect );
		params.setProperty( PersistentIdentifierGenerator.TABLE, tableName );

		//pass the column name (a generated id almost always has a single column)
		final Column column = (Column) identifierValue.getSelectables().get(0);
		final String columnName = column.getQuotedName( dialect );
		params.setProperty( PersistentIdentifierGenerator.PK, columnName );

		//pass the entity-name, if not a collection-id
		if ( rootClass != null ) {
			params.setProperty( IdentifierGenerator.ENTITY_NAME, rootClass.getEntityName() );
			params.setProperty( IdentifierGenerator.JPA_ENTITY_NAME, rootClass.getJpaEntityName() );
			// The table name is not really a good default for subselect entities,
			// so use the JPA entity name which is short
			params.setProperty( OptimizableGenerator.IMPLICIT_NAME_BASE,
					identifierValue.getTable().isSubselect()
							? rootClass.getJpaEntityName()
							: identifierValue.getTable().getName() );

			params.setProperty( PersistentIdentifierGenerator.TABLES,
					identityTablesString( dialect, rootClass ) );
		}
		else {
			params.setProperty( PersistentIdentifierGenerator.TABLES, tableName );
			params.setProperty( OptimizableGenerator.IMPLICIT_NAME_BASE, tableName );
		}

		params.put( IdentifierGenerator.CONTRIBUTOR_NAME,
				identifierValue.getBuildingContext().getCurrentContributorName() );

		final Map<String, Object> settings = configService.getSettings();
		if ( settings.containsKey( AvailableSettings.PREFERRED_POOLED_OPTIMIZER ) ) {
			params.put( AvailableSettings.PREFERRED_POOLED_OPTIMIZER,
					settings.get( AvailableSettings.PREFERRED_POOLED_OPTIMIZER ) );
		}

		params.putAll( configuration );

		return params;
	}

	private static String identityTablesString(Dialect dialect, RootClass rootClass) {
		final StringBuilder tables = new StringBuilder();
		for ( Table table : rootClass.getIdentityTables() ) {
			tables.append( table.getQuotedName( dialect ) );
			if ( !tables.isEmpty() ) {
				tables.append( ", " );
			}
		}
		return tables.toString();
	}

	private static int defaultIncrement(ConfigurationService configService) {
		final String idNamingStrategy =
				configService.getSetting( AvailableSettings.ID_DB_STRUCTURE_NAMING_STRATEGY,
						StandardConverters.STRING, null );
		if ( LegacyNamingStrategy.STRATEGY_NAME.equals( idNamingStrategy )
				|| LegacyNamingStrategy.class.getName().equals( idNamingStrategy )
				|| SingleNamingStrategy.STRATEGY_NAME.equals( idNamingStrategy )
				|| SingleNamingStrategy.class.getName().equals( idNamingStrategy ) ) {
			return 1;
		}
		else {
			return OptimizableGenerator.DEFAULT_INCREMENT_SIZE;
		}
	}


	@Internal
	public static void interpretTableGenerator(
			TableGenerator tableGeneratorAnnotation,
			IdentifierGeneratorDefinition.Builder definitionBuilder) {
		definitionBuilder.setName( tableGeneratorAnnotation.name() );
		definitionBuilder.setStrategy( org.hibernate.id.enhanced.TableGenerator.class.getName() );
		definitionBuilder.addParam( org.hibernate.id.enhanced.TableGenerator.CONFIG_PREFER_SEGMENT_PER_ENTITY, "true" );

		final String catalog = tableGeneratorAnnotation.catalog();
		if ( StringHelper.isNotEmpty( catalog ) ) {
			definitionBuilder.addParam( PersistentIdentifierGenerator.CATALOG, catalog );
		}

		final String schema = tableGeneratorAnnotation.schema();
		if ( StringHelper.isNotEmpty( schema ) ) {
			definitionBuilder.addParam( PersistentIdentifierGenerator.SCHEMA, schema );
		}

		final String table = tableGeneratorAnnotation.table();
		if ( StringHelper.isNotEmpty( table ) ) {
			definitionBuilder.addParam( org.hibernate.id.enhanced.TableGenerator.TABLE_PARAM, table );
		}

		final String pkColumnName = tableGeneratorAnnotation.pkColumnName();
		if ( StringHelper.isNotEmpty( pkColumnName ) ) {
			definitionBuilder.addParam(
					org.hibernate.id.enhanced.TableGenerator.SEGMENT_COLUMN_PARAM,
					pkColumnName
			);
		}

		final String pkColumnValue = tableGeneratorAnnotation.pkColumnValue();
		if ( StringHelper.isNotEmpty( pkColumnValue ) ) {
			definitionBuilder.addParam(
					org.hibernate.id.enhanced.TableGenerator.SEGMENT_VALUE_PARAM,
					pkColumnValue
			);
		}

		final String valueColumnName = tableGeneratorAnnotation.valueColumnName();
		if ( StringHelper.isNotEmpty( valueColumnName ) ) {
			definitionBuilder.addParam(
					org.hibernate.id.enhanced.TableGenerator.VALUE_COLUMN_PARAM,
					valueColumnName
			);
		}

		final String options = tableGeneratorAnnotation.options();
		if ( StringHelper.isNotEmpty( options ) ) {
			definitionBuilder.addParam(
					PersistentIdentifierGenerator.OPTIONS,
					options
			);
		}

		definitionBuilder.addParam(
				org.hibernate.id.enhanced.TableGenerator.INCREMENT_PARAM,
				String.valueOf( tableGeneratorAnnotation.allocationSize() )
		);

		// See comment on HHH-4884 wrt initialValue.  Basically initialValue is really the stated value + 1
		definitionBuilder.addParam(
				org.hibernate.id.enhanced.TableGenerator.INITIAL_PARAM,
				String.valueOf( tableGeneratorAnnotation.initialValue() + 1 )
		);

		// TODO : implement unique-constraint support
		final UniqueConstraint[] uniqueConstraints = tableGeneratorAnnotation.uniqueConstraints();
		if ( CollectionHelper.isNotEmpty( uniqueConstraints ) ) {
			LOG.ignoringTableGeneratorConstraints( tableGeneratorAnnotation.name() );
		}
	}

	@Internal
	public static void interpretSequenceGenerator(
			SequenceGenerator sequenceGeneratorAnnotation,
			IdentifierGeneratorDefinition.Builder definitionBuilder) {
		definitionBuilder.setName( sequenceGeneratorAnnotation.name() );
		definitionBuilder.setStrategy( SequenceStyleGenerator.class.getName() );

		final String catalog = sequenceGeneratorAnnotation.catalog();
		if ( StringHelper.isNotEmpty( catalog ) ) {
			definitionBuilder.addParam( PersistentIdentifierGenerator.CATALOG, catalog );
		}

		final String schema = sequenceGeneratorAnnotation.schema();
		if ( StringHelper.isNotEmpty( schema ) ) {
			definitionBuilder.addParam( PersistentIdentifierGenerator.SCHEMA, schema );
		}

		final String sequenceName = sequenceGeneratorAnnotation.sequenceName();
		if ( StringHelper.isNotEmpty( sequenceName ) ) {
			definitionBuilder.addParam( SequenceStyleGenerator.SEQUENCE_PARAM, sequenceName );
		}

		definitionBuilder.addParam(
				SequenceStyleGenerator.INCREMENT_PARAM,
				String.valueOf( sequenceGeneratorAnnotation.allocationSize() )
		);
		definitionBuilder.addParam(
				SequenceStyleGenerator.INITIAL_PARAM,
				String.valueOf( sequenceGeneratorAnnotation.initialValue() )
		);

		final String options = sequenceGeneratorAnnotation.options();
		if ( StringHelper.isNotEmpty( options ) ) {
			definitionBuilder.addParam( PersistentIdentifierGenerator.OPTIONS, options );
		}
	}
}