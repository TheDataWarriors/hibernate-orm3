/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Index;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitCollectionTableNameSource;
import org.hibernate.boot.model.naming.ImplicitJoinTableNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.NamingStrategyHelper;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AnnotatedJoinColumn;
import org.hibernate.cfg.AnnotatedJoinColumns;
import org.hibernate.cfg.IndexOrUniqueKeySecondPass;
import org.hibernate.cfg.JPAIndexHolder;
import org.hibernate.cfg.ObjectNameSource;
import org.hibernate.cfg.PropertyHolder;
import org.hibernate.cfg.UniqueConstraintHolder;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SortableValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;

import org.jboss.logging.Logger;

import static org.hibernate.cfg.BinderHelper.isEmptyOrNullAnnotationValue;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.isQuoted;
import static org.hibernate.internal.util.StringHelper.unquote;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * Table related operations
 *
 * @author Emmanuel Bernard
 */
public class TableBinder {
	//TODO move it to a getter/setter strategy
	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, TableBinder.class.getName() );

	MetadataBuildingContext buildingContext;

	private String schema;
	private String catalog;
	private String name;
	private boolean isAbstract;
	private List<UniqueConstraintHolder> uniqueConstraints;
	//	private List<String[]> uniqueConstraints;
	String constraints;
	private String ownerEntityTable;
	private String associatedEntityTable;
	private String propertyName;
	private String ownerClassName;
	private String ownerEntity;
	private String ownerJpaEntity;
	private String associatedClassName;
	private String associatedEntity;
	private String associatedJpaEntity;
	private boolean isJPA2ElementCollection;
	private List<JPAIndexHolder> jpaIndexHolders;

	public void setBuildingContext(MetadataBuildingContext buildingContext) {
		this.buildingContext = buildingContext;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setAbstract(boolean anAbstract) {
		isAbstract = anAbstract;
	}

	public void setUniqueConstraints(UniqueConstraint[] uniqueConstraints) {
		this.uniqueConstraints = TableBinder.buildUniqueConstraintHolders( uniqueConstraints );
	}

	public void setJpaIndex(Index[] jpaIndex){
		this.jpaIndexHolders = buildJpaIndexHolder( jpaIndex );
	}

	public void setConstraints(String constraints) {
		this.constraints = constraints;
	}

	public void setJPA2ElementCollection(boolean isJPA2ElementCollection) {
		this.isJPA2ElementCollection = isJPA2ElementCollection;
	}

	private static class AssociationTableNameSource implements ObjectNameSource {
		private final String explicitName;
		private final String logicalName;

		private AssociationTableNameSource(String explicitName, String logicalName) {
			this.explicitName = explicitName;
			this.logicalName = logicalName;
		}

		public String getExplicitName() {
			return explicitName;
		}

		public String getLogicalName() {
			return logicalName;
		}
	}

	// only bind association table currently
	public Table bind() {
		final Identifier ownerEntityTableNameIdentifier = toIdentifier( ownerEntityTable );

		//logicalName only accurate for assoc table...
		final String unquotedOwnerTable = unquote( ownerEntityTable );
		final String unquotedAssocTable = unquote( associatedEntityTable );

		final ObjectNameSource nameSource = buildNameContext();

		final boolean ownerEntityTableQuoted = isQuoted( ownerEntityTable );
		final boolean associatedEntityTableQuoted = isQuoted( associatedEntityTable );
		final NamingStrategyHelper namingStrategyHelper = new NamingStrategyHelper() {
			@Override
			public Identifier determineImplicitName(final MetadataBuildingContext buildingContext) {
				final ImplicitNamingStrategy namingStrategy = buildingContext.getBuildingOptions().getImplicitNamingStrategy();

				Identifier name;
				if ( isJPA2ElementCollection ) {
					name = namingStrategy.determineCollectionTableName(
							new ImplicitCollectionTableNameSource() {
								private final EntityNaming entityNaming = new EntityNaming() {
									@Override
									public String getClassName() {
										return ownerClassName;
									}

									@Override
									public String getEntityName() {
										return ownerEntity;
									}

									@Override
									public String getJpaEntityName() {
										return ownerJpaEntity;
									}
								};

								@Override
								public Identifier getOwningPhysicalTableName() {
									return ownerEntityTableNameIdentifier;
								}

								@Override
								public EntityNaming getOwningEntityNaming() {
									return entityNaming;
								}

								@Override
								public AttributePath getOwningAttributePath() {
									return AttributePath.parse( propertyName );
								}

								@Override
								public MetadataBuildingContext getBuildingContext() {
									return buildingContext;
								}
							}
					);
				}
				else {
					name =  namingStrategy.determineJoinTableName(
							new ImplicitJoinTableNameSource() {
								private final EntityNaming owningEntityNaming = new EntityNaming() {
									@Override
									public String getClassName() {
										return ownerClassName;
									}

									@Override
									public String getEntityName() {
										return ownerEntity;
									}

									@Override
									public String getJpaEntityName() {
										return ownerJpaEntity;
									}
								};

								private final EntityNaming nonOwningEntityNaming = new EntityNaming() {
									@Override
									public String getClassName() {
										return associatedClassName;
									}

									@Override
									public String getEntityName() {
										return associatedEntity;
									}

									@Override
									public String getJpaEntityName() {
										return associatedJpaEntity;
									}
								};

								@Override
								public String getOwningPhysicalTableName() {
									return unquotedOwnerTable;
								}

								@Override
								public EntityNaming getOwningEntityNaming() {
									return owningEntityNaming;
								}

								@Override
								public String getNonOwningPhysicalTableName() {
									return unquotedAssocTable;
								}

								@Override
								public EntityNaming getNonOwningEntityNaming() {
									return nonOwningEntityNaming;
								}

								@Override
								public AttributePath getAssociationOwningAttributePath() {
									return AttributePath.parse( propertyName );
								}

								@Override
								public MetadataBuildingContext getBuildingContext() {
									return buildingContext;
								}
							}
					);
				}

				if ( ownerEntityTableQuoted || associatedEntityTableQuoted ) {
					name = Identifier.quote( name );
				}

				return name;
			}

			@Override
			public Identifier handleExplicitName(
					String explicitName, MetadataBuildingContext buildingContext) {
				return buildingContext.getMetadataCollector().getDatabase().toIdentifier( explicitName );
			}

			@Override
			public Identifier toPhysicalName(Identifier logicalName, MetadataBuildingContext buildingContext) {
				return buildingContext.getBuildingOptions().getPhysicalNamingStrategy().toPhysicalTableName(
						logicalName,
						buildingContext.getMetadataCollector().getDatabase().getJdbcEnvironment()
				);
			}
		};

		return buildAndFillTable(
				schema,
				catalog,
				nameSource,
				namingStrategyHelper,
				isAbstract,
				uniqueConstraints,
				jpaIndexHolders,
				constraints,
				buildingContext,
				null,
				null
		);
	}

	private Identifier toIdentifier(String tableName) {
		return buildingContext.getMetadataCollector()
				.getDatabase()
				.getJdbcEnvironment()
				.getIdentifierHelper()
				.toIdentifier( tableName );
	}

	private ObjectNameSource buildNameContext() {
		if ( name != null ) {
			return new AssociationTableNameSource( name, null );
		}

		final Identifier logicalName;
		if ( isJPA2ElementCollection ) {
			logicalName	= buildingContext.getBuildingOptions().getImplicitNamingStrategy().determineCollectionTableName(
					new ImplicitCollectionTableNameSource() {
						private final EntityNaming owningEntityNaming = new EntityNaming() {
							@Override
							public String getClassName() {
								return ownerClassName;
							}

							@Override
							public String getEntityName() {
								return ownerEntity;
							}

							@Override
							public String getJpaEntityName() {
								return ownerJpaEntity;
							}
						};

						@Override
						public Identifier getOwningPhysicalTableName() {
							return toIdentifier( ownerEntityTable );
						}

						@Override
						public EntityNaming getOwningEntityNaming() {
							return owningEntityNaming;
						}

						@Override
						public AttributePath getOwningAttributePath() {
							// we don't know path on the annotations side :(
							return AttributePath.parse( propertyName );
						}

						@Override
						public MetadataBuildingContext getBuildingContext() {
							return buildingContext;
						}
					}
			);
		}
		else {
			logicalName = buildingContext.getBuildingOptions().getImplicitNamingStrategy().determineJoinTableName(
					new ImplicitJoinTableNameSource() {
						private final EntityNaming owningEntityNaming = new EntityNaming() {
							@Override
							public String getClassName() {
								return ownerClassName;
							}

							@Override
							public String getEntityName() {
								return ownerEntity;
							}

							@Override
							public String getJpaEntityName() {
								return ownerJpaEntity;
							}
						};

						private final EntityNaming nonOwningEntityNaming = new EntityNaming() {
							@Override
							public String getClassName() {
								return associatedClassName;
							}

							@Override
							public String getEntityName() {
								return associatedEntity;
							}

							@Override
							public String getJpaEntityName() {
								return associatedJpaEntity;
							}
						};

						@Override
						public String getOwningPhysicalTableName() {
							return ownerEntityTable;
						}

						@Override
						public EntityNaming getOwningEntityNaming() {
							return owningEntityNaming;
						}

						@Override
						public String getNonOwningPhysicalTableName() {
							return associatedEntityTable;
						}

						@Override
						public EntityNaming getNonOwningEntityNaming() {
							return nonOwningEntityNaming;
						}

						@Override
						public AttributePath getAssociationOwningAttributePath() {
							return AttributePath.parse( propertyName );
						}

						@Override
						public MetadataBuildingContext getBuildingContext() {
							return buildingContext;
						}
					}
			);
		}

		return new AssociationTableNameSource( name, logicalName.render() );
	}

	public static Table buildAndFillTable(
			String schema,
			String catalog,
			ObjectNameSource nameSource,
			NamingStrategyHelper namingStrategyHelper,
			boolean isAbstract,
			List<UniqueConstraintHolder> uniqueConstraints,
			List<JPAIndexHolder> jpaIndexHolders,
			String constraints,
			MetadataBuildingContext buildingContext,
			String subselect,
			InFlightMetadataCollector.EntityTableXref denormalizedSuperTableXref) {
		final Identifier logicalName;
		if ( isNotEmpty( nameSource.getExplicitName() ) ) {
			logicalName = namingStrategyHelper.handleExplicitName( nameSource.getExplicitName(), buildingContext );
		}
		else {
			logicalName = namingStrategyHelper.determineImplicitName( buildingContext );
		}

		return buildAndFillTable(
				schema,
				catalog,
				logicalName,
				isAbstract,
				uniqueConstraints,
				jpaIndexHolders,
				constraints,
				buildingContext,
				subselect,
				denormalizedSuperTableXref
		);
	}

	public static Table buildAndFillTable(
			String schema,
			String catalog,
			Identifier logicalName,
			boolean isAbstract,
			List<UniqueConstraintHolder> uniqueConstraints,
			List<JPAIndexHolder> jpaIndexHolders,
			String constraints,
			MetadataBuildingContext buildingContext,
			String subselect,
			InFlightMetadataCollector.EntityTableXref denormalizedSuperTableXref) {
		schema = isEmptyOrNullAnnotationValue( schema )  ? null : schema;
		catalog = isEmptyOrNullAnnotationValue( catalog ) ? null : catalog;

		final Table table;
		if ( denormalizedSuperTableXref != null ) {
			table = buildingContext.getMetadataCollector().addDenormalizedTable(
					schema,
					catalog,
					logicalName.render(),
					isAbstract,
					subselect,
					denormalizedSuperTableXref.getPrimaryTable(),
					buildingContext
			);
		}
		else {
			table = buildingContext.getMetadataCollector().addTable(
					schema,
					catalog,
					logicalName.render(),
					subselect,
					isAbstract,
					buildingContext
			);
		}

		if ( CollectionHelper.isNotEmpty( uniqueConstraints ) ) {
			buildingContext.getMetadataCollector().addUniqueConstraintHolders( table, uniqueConstraints );
		}

		if ( CollectionHelper.isNotEmpty( jpaIndexHolders ) ) {
			buildingContext.getMetadataCollector().addJpaIndexHolders( table, jpaIndexHolders );
		}

		if ( constraints != null ) {
			table.addCheckConstraint( constraints );
		}

		buildingContext.getMetadataCollector().addTableNameBinding( logicalName, table );

		return table;
	}

	public static void bindForeignKey(
			PersistentClass referencedEntity,
			PersistentClass destinationEntity,
			AnnotatedJoinColumns joinColumns,
			SimpleValue value,
			boolean unique,
			MetadataBuildingContext buildingContext) {
		final PersistentClass associatedClass;
		if ( destinationEntity != null ) {
			//overridden destination
			associatedClass = destinationEntity;
		}
		else {
			final PropertyHolder holder = joinColumns.getPropertyHolder();
			associatedClass = holder == null ? null : holder.getPersistentClass();
		}

		final AnnotatedJoinColumn firstColumn = joinColumns.getJoinColumns().get(0);
		if ( joinColumns.hasMappedBy() ) {
			// use the columns of the property referenced by mappedBy
			// copy them and link the copy to the actual value
			bindUnownedAssociation( joinColumns, value, associatedClass, joinColumns.getMappedBy() );
		}
		else if ( firstColumn.isImplicit() ) {
			// if columns are implicit, then create the columns based
			// on the referenced entity id columns
			bindImplicitColumns( referencedEntity, joinColumns, value );
		}
		else {
			bindExplicitColumns( referencedEntity, joinColumns, value, buildingContext, associatedClass );
		}
		value.createForeignKey();
		if ( unique ) {
			value.createUniqueKey();
		}
	}

	private static void bindExplicitColumns(
			PersistentClass referencedEntity,
			AnnotatedJoinColumns joinColumns,
			SimpleValue value,
			MetadataBuildingContext buildingContext,
			PersistentClass associatedClass) {
		switch ( joinColumns.getReferencedColumnsType( referencedEntity ) ) {
			case NON_PRIMARY_KEY_REFERENCE:
				bindNonPrimaryKeyReference( referencedEntity, joinColumns, value );
				break;
			case IMPLICIT_PRIMARY_KEY_REFERENCE:
				bindImplicitPrimaryKeyReference( referencedEntity, joinColumns, value, associatedClass );
				break;
			case EXPLICIT_PRIMARY_KEY_REFERENCE:
				bindPrimaryKeyReference( referencedEntity, joinColumns, value, associatedClass, buildingContext );
				break;
		}
	}

	private static void bindImplicitPrimaryKeyReference(
			PersistentClass referencedEntity,
			AnnotatedJoinColumns joinColumns,
			SimpleValue value,
			PersistentClass associatedClass) {
		//implicit case, we hope PK and FK columns are in the same order
		if ( joinColumns.getColumns().size() != referencedEntity.getIdentifier().getColumnSpan() ) {
			// TODO: what about secondary tables?? associatedClass is null?
			throw new AnnotationException(
					"An association that targets entity '" + referencedEntity.getEntityName()
							+ "' from entity '" + associatedClass.getEntityName()
							+ "' has " + joinColumns.getColumns().size() + " '@JoinColumn's but the primary key has "
							+ referencedEntity.getIdentifier().getColumnSpan() + " columns"
			);
		}
		linkJoinColumnWithValueOverridingNameIfImplicit(
				referencedEntity,
				referencedEntity.getIdentifier(),
				joinColumns,
				value
		);
		if ( value instanceof SortableValue ) {
			( (SortableValue) value).sortProperties();
		}
	}

	private static void bindPrimaryKeyReference(
			PersistentClass referencedEntity,
			AnnotatedJoinColumns joinColumns,
			SimpleValue value,
			PersistentClass associatedClass,
			MetadataBuildingContext buildingContext) {
		// ensure the composite key is sorted so that we can simply
		// set sorted to true on the ToOne (below)
		final KeyValue key = referencedEntity.getKey();
		if ( key instanceof Component ) {
			( (Component) key).sortProperties();
		}
		// works because the pk has to be on the primary table
		final Dialect dialect = buildingContext.getMetadataCollector().getDatabase()
				.getJdbcEnvironment().getDialect();
		for ( Column column: key.getColumns() ) {
			boolean match = false;
			// for each PK column, find the associated FK column.
			final String quotedName = column.getQuotedName( dialect );
			for ( AnnotatedJoinColumn joinColumn : joinColumns.getJoinColumns() ) {
				final String referencedColumn = buildingContext.getMetadataCollector()
						.getPhysicalColumnName( referencedEntity.getTable(), joinColumn.getReferencedColumn() );
				// in JPA 2 referencedColumnName is case-insensitive
				if ( referencedColumn.equalsIgnoreCase( quotedName ) ) {
					// correct join column
					if ( joinColumn.isNameDeferred() ) {
						joinColumn.linkValueUsingDefaultColumnNaming( column, referencedEntity, value );
					}
					else {
						joinColumn.linkWithValue( value );
					}
					Column referenced = getReferencedColumn( referencedColumn, referencedEntity );
					if ( referenced != null ) {
						value.addReferencedColumn( referenced );
					}
					joinColumn.overrideFromReferencedColumnIfNecessary( column );
					match = true;
					break;
				}
			}
			if ( !match ) {
				// we can only get here if there's a dupe PK column in the @JoinColumns
				throw new AnnotationException(
						"An association that targets entity '" + referencedEntity.getEntityName()
								+ "' from entity '" + associatedClass.getEntityName()
								+ "' has no '@JoinColumn' referencing column '"+ column.getName()
				);
			}
		}
		if ( value instanceof ToOne ) {
			( (ToOne) value).setSorted( true );
		}
	}

	private static Column getReferencedColumn(String referencedColumnName, PersistentClass referencedEntity){
		for ( Column column : referencedEntity.getTable().getColumns() ){
			if (referencedColumnName.equals( column.getName() )) {
				return column;
			}
		}
		return null;
	}

	private static void bindNonPrimaryKeyReference(
			PersistentClass referencedEntity,
			AnnotatedJoinColumns joinColumns,
			SimpleValue value) {
		final String referencedPropertyName;
		if ( value instanceof ToOne ) {
			referencedPropertyName = ( (ToOne) value).getReferencedPropertyName();
		}
		else if ( value instanceof DependantValue ) {
			final String propertyName = joinColumns.getPropertyName();
			if ( propertyName != null ) {
				Collection collection = (Collection) referencedEntity.getRecursiveProperty( propertyName ).getValue();
				referencedPropertyName = collection.getReferencedPropertyName();
			}
			else {
				throw new AnnotationException( "The '@JoinColumn' for a secondary table must reference the primary key" );
			}
		}
		else {
			throw new AssertionFailure( "Property ref to an unexpected Value type: " + value.getClass().getName() );
		}
		if ( referencedPropertyName == null ) {
			throw new AssertionFailure( "No property ref found" );
		}

		final Property synthProp = referencedEntity.getReferencedProperty( referencedPropertyName );
		if ( synthProp == null ) {
			throw new AssertionFailure( "Cannot find synthetic property: "
					+ referencedEntity.getEntityName() + "." + referencedPropertyName );
		}
		linkJoinColumnWithValueOverridingNameIfImplicit( referencedEntity, synthProp.getValue(), joinColumns, value );
		( (SortableValue) value).sortProperties();
	}

	private static void bindImplicitColumns(
			PersistentClass referencedEntity,
			AnnotatedJoinColumns joinColumns,
			SimpleValue value) {
		final List<Column> idColumns = referencedEntity instanceof JoinedSubclass
				? referencedEntity.getKey().getColumns()
				: referencedEntity.getIdentifier().getColumns();
		for ( Column column: idColumns ) {
			final AnnotatedJoinColumn firstColumn = joinColumns.getJoinColumns().get(0);
			firstColumn.linkValueUsingDefaultColumnNaming( column, referencedEntity, value);
			firstColumn.overrideFromReferencedColumnIfNecessary( column );
		}
	}

	private static void bindUnownedAssociation(
			AnnotatedJoinColumns joinColumns,
			SimpleValue value,
			PersistentClass associatedClass,
			String mappedByProperty) {
		final AnnotatedJoinColumn firstColumn = joinColumns.getJoinColumns().get(0);
		for ( Column column: mappedByColumns( associatedClass, mappedByProperty ) ) {
			firstColumn.overrideFromReferencedColumnIfNecessary( column );
			firstColumn.linkValueUsingAColumnCopy( column, value);
		}
	}

	private static List<Column> mappedByColumns(PersistentClass associatedClass, String mappedByProperty) {
		LOG.debugf( "Retrieving property %s.%s", associatedClass.getEntityName(), mappedByProperty );
		final Value value = associatedClass.getRecursiveProperty( mappedByProperty ).getValue();
		if ( value instanceof Collection ) {
			final Value element = ((Collection) value).getElement();
			if ( element == null ) {
				throw new AnnotationException( "Both sides of the bidirectional association '"
						+ associatedClass.getEntityName() + "." + mappedByProperty + "' specify 'mappedBy'" );
			}
			return element.getColumns();
		}
		else {
			return value.getColumns();
		}
	}

	public static void linkJoinColumnWithValueOverridingNameIfImplicit(
			PersistentClass referencedEntity,
			Value value,
			AnnotatedJoinColumns joinColumns,
			SimpleValue simpleValue) {
		final List<Column> valueColumns = value.getColumns();
		final List<AnnotatedJoinColumn> columns = joinColumns.getJoinColumns();
		for ( int i = 0; i < columns.size(); i++ ) {
			final AnnotatedJoinColumn joinColumn = columns.get(i);
			final Column synthCol = valueColumns.get(i);
			if ( joinColumn.isNameDeferred() ) {
				//this has to be the default value
				joinColumn.linkValueUsingDefaultColumnNaming( synthCol, referencedEntity, simpleValue );
			}
			else {
				joinColumn.linkWithValue( simpleValue );
				joinColumn.overrideFromReferencedColumnIfNecessary( synthCol );
			}
		}
	}

	public static void addIndexes(Table table, org.hibernate.annotations.Index[] indexes, MetadataBuildingContext context) {
		for ( org.hibernate.annotations.Index index : indexes ) {
			//no need to handle inSecondPass here since it is only called from EntityBinder
			context.getMetadataCollector().addSecondPass(
					new IndexOrUniqueKeySecondPass( table, index.name(), index.columnNames(), context )
			);
		}
	}

	public static void addIndexes(Table table, Index[] indexes, MetadataBuildingContext context) {
		context.getMetadataCollector().addJpaIndexHolders( table, buildJpaIndexHolder( indexes ) );
	}

	public static List<JPAIndexHolder> buildJpaIndexHolder(Index[] indexes) {
		List<JPAIndexHolder> holders = new ArrayList<>( indexes.length );
		for ( Index index : indexes ) {
			holders.add( new JPAIndexHolder( index ) );
		}
		return holders;
	}

	/**
	 * Build a list of {@link UniqueConstraintHolder} instances given a list of
	 * {@link UniqueConstraint} annotations.
	 *
	 * @param annotations The {@link UniqueConstraint} annotations.
	 *
	 * @return The built {@link UniqueConstraintHolder} instances.
	 */
	public static List<UniqueConstraintHolder> buildUniqueConstraintHolders(UniqueConstraint[] annotations) {
		List<UniqueConstraintHolder> result;
		if ( annotations == null || annotations.length == 0 ) {
			result = java.util.Collections.emptyList();
		}
		else {
			result = arrayList( annotations.length );
			for ( UniqueConstraint uc : annotations ) {
				result.add( new UniqueConstraintHolder().setName( uc.name() ).setColumns( uc.columnNames() ) );
			}
		}
		return result;
	}

	public void setDefaultName(
			String ownerClassName,
			String ownerEntity,
			String ownerJpaEntity,
			String ownerEntityTable,
			String associatedClassName,
			String associatedEntity,
			String associatedJpaEntity,
			String associatedEntityTable,
			String propertyName) {
		this.ownerClassName = ownerClassName;
		this.ownerEntity = ownerEntity;
		this.ownerJpaEntity = ownerJpaEntity;
		this.ownerEntityTable = ownerEntityTable;
		this.associatedClassName = associatedClassName;
		this.associatedEntity = associatedEntity;
		this.associatedJpaEntity = associatedJpaEntity;
		this.associatedEntityTable = associatedEntityTable;
		this.propertyName = propertyName;
		this.name = null;
	}
}
