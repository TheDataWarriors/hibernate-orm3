/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.TransactSQLTrimEmulation;
import org.hibernate.query.sqm.produce.function.spi.PairedFunctionTemplate;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.type.descriptor.sql.spi.BlobSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.ClobSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.StandardSpiBasicTypes;


/**
 * All Sybase dialects share an IN list size limit.
 *
 * @author Brett Meyer
 */
public class SybaseDialect extends AbstractTransactSQLDialect {
	private static final int PARAM_LIST_SIZE_LIMIT = 250000;

	public SybaseDialect() {
		super();

		//Sybase ASE didn't introduce bigint until version 15.0
		registerColumnType( Types.BIGINT, "numeric(19,0)" );
	}

	@Override
	public int getInExpressionCountLimit() {
		return PARAM_LIST_SIZE_LIMIT;
	}
	
	@Override
	protected SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
		switch (sqlCode) {
		case Types.BLOB:
			return BlobSqlDescriptor.PRIMITIVE_ARRAY_BINDING;
		case Types.CLOB:
			// Some Sybase drivers cannot support getClob.  See HHH-7889
			return ClobSqlDescriptor.STREAM_BINDING_EXTRACTING;
		default:
			return super.getSqlTypeDescriptorOverride( sqlCode );
		}
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry(queryEngine);

		//this doesn't work 100% on earlier versions of Sybase
		//which were missing the third parameter in charindex()
		//TODO: we could emulate it with substring() like in Postgres
		CommonFunctionFactory.locate_charindex( queryEngine );

		CommonFunctionFactory.replace_strReplace( queryEngine );

		queryEngine.getSqmFunctionRegistry().register(
				"trim", new TransactSQLTrimEmulation(
						TransactSQLTrimEmulation.LTRIM,
						TransactSQLTrimEmulation.RTRIM,
						"str_replace"
				)
		);

		//these functions need parens
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "current_date" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setUseParenthesesWhenNoArgs(true)
				.setExactArgumentCount( 0 )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "current_time" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setUseParenthesesWhenNoArgs(true)
				.setExactArgumentCount( 0 )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "current_timestamp" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setUseParenthesesWhenNoArgs(true)
				.setExactArgumentCount( 0 )
				.register();

	}

	@Override
	public String getNullColumnString() {
		return " null";
	}

	@Override
	public String getCurrentSchemaCommand() {
		return "select db_name()";
	}
}
