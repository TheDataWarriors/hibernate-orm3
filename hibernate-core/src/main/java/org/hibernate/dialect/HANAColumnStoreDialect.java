/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.mutation.internal.idtable.AfterUseAction;
import org.hibernate.query.sqm.mutation.internal.idtable.GlobalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.internal.idtable.IdTable;
import org.hibernate.query.sqm.mutation.internal.idtable.PhysicalIdTableExporter;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.type.StandardBasicTypes;

/**
 * An SQL dialect for the SAP HANA column store.
 * <p>
 * For more information on interacting with the SAP HANA database, refer to the
 * <a href="https://help.sap.com/viewer/4fe29514fd584807ac9f2a04f6754767/">SAP HANA SQL and System Views Reference</a>
 * and the <a href=
 * "https://help.sap.com/viewer/0eec0d68141541d1b07893a39944924e/latest/en-US/434e2962074540e18c802fd478de86d6.html">SAP
 * HANA Client Interface Programming Reference</a>.
 * <p>
 * Column tables are created by this dialect when using the auto-ddl feature.
 * 
 * @author <a href="mailto:andrew.clemons@sap.com">Andrew Clemons</a>
 * @author <a href="mailto:jonathan.bregler@sap.com">Jonathan Bregler</a>
 */
public class HANAColumnStoreDialect extends AbstractHANADialect {
	public HANAColumnStoreDialect(DialectResolutionInfo info) {
		this( info.makeCopy() );
		registerKeywords( info );
	}
	
	public HANAColumnStoreDialect() {
		this( DatabaseVersion.make( 3, 0 ) );
	}

	public HANAColumnStoreDialect(DatabaseVersion version) {
		super( version );
		if ( version.isSince( 4 ) ) {
			registerColumnType( Types.CHAR, "nvarchar(1)" );
			registerColumnType( Types.VARCHAR, 5000, "nvarchar($l)" );
			registerColumnType( Types.LONGVARCHAR, 5000, "nvarchar($l)" );

			// for longer values map to clob/nclob
			registerColumnType( Types.LONGVARCHAR, "nclob" );
			registerColumnType( Types.VARCHAR, "nclob" );
			registerColumnType( Types.CLOB, "nclob" );

			registerHibernateType( Types.CLOB, StandardBasicTypes.MATERIALIZED_NCLOB.getName() );
			registerHibernateType( Types.NCHAR, StandardBasicTypes.NSTRING.getName() );
			registerHibernateType( Types.CHAR, StandardBasicTypes.CHARACTER.getName() );
			registerHibernateType( Types.CHAR, 1, StandardBasicTypes.CHARACTER.getName() );
			registerHibernateType( Types.CHAR, 5000, StandardBasicTypes.NSTRING.getName() );
			registerHibernateType( Types.VARCHAR, StandardBasicTypes.NSTRING.getName() );
			registerHibernateType( Types.LONGVARCHAR, StandardBasicTypes.NTEXT.getName() );

			// register additional keywords
			registerHanaCloudKeywords();
		}
	}

	@Override
	public DatabaseVersion getVersion(){
		return version;
	}

	private void registerHanaCloudKeywords() {
		registerKeyword( "array" );
		registerKeyword( "at" );
		registerKeyword( "authorization" );
		registerKeyword( "between" );
		registerKeyword( "by" );
		registerKeyword( "collate" );
		registerKeyword( "empty" );
		registerKeyword( "filter" );
		registerKeyword( "grouping" );
		registerKeyword( "no" );
		registerKeyword( "not" );
		registerKeyword( "of" );
		registerKeyword( "over" );
		registerKeyword( "recursive" );
		registerKeyword( "row" );
		registerKeyword( "table" );
		registerKeyword( "to" );
		registerKeyword( "window" );
		registerKeyword( "within" );
	}


	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		// full-text search functions
		queryEngine.getSqmFunctionRegistry().registerNamed(
				"score",
				queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
		);
		queryEngine.getSqmFunctionRegistry().registerNamed( "snippets" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "highlighted" );
		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"contains",
				queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.BOOLEAN ),
				"contains(?1,?2)",
				"contains(?1,?2,?3)"
		);
	}

	@Override
	public String getCreateTableString() {
		return "create column table";
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType entityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new GlobalTemporaryTableStrategy(
				new IdTable( entityDescriptor, basename -> "HT_" + basename, this, runtimeModelCreationContext ),
				() -> new PhysicalIdTableExporter() {
					@Override
					protected String getCreateCommand() {
						return "create global temporary column table";
					}

					@Override
					protected String getTruncateIdTableCommand() {
						return "truncate table";
					}

					@Override
					public String getCreateOptions() {
						return "on commit delete rows";
					}
				},
				AfterUseAction.CLEAN,
				runtimeModelCreationContext.getSessionFactory()
		);
	}

	@Override
	protected boolean supportsAsciiStringTypes() {
		if ( version.isSince( 4 ) ) {
			return false;
		}
		return true;
	}

	@Override
	protected Boolean useUnicodeStringTypesDefault() {
		if ( version.isSince( 4 ) ) {
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}

	@Override
	public boolean isUseUnicodeStringTypes() {
		if ( version.isSince( 4 ) ) {
			return true;
		}
		return super.isUseUnicodeStringTypes();
	}
}
