/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.Date;
import java.util.Arrays;

import org.hibernate.dialect.Dialect;

import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.*;
import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.useArgType;

/**
 * Enumeratoes common function template definitions.
 * Centralized for easier use from dialects.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class CommonFunctionFactory {

	private final BasicType<Boolean> booleanType;
	private final BasicType<Character> characterType;
	private final BasicType<String> stringType;
	private final BasicType<Integer> integerType;
	private final BasicType<Long> longType;
	private final BasicType<Double> doubleType;
	private final BasicType<Date> dateType;
	private final BasicType<Date> timeType;
	private final BasicType<Date> timestampType;
	
	private final SqmFunctionRegistry functionRegistry;
	private final TypeConfiguration typeConfiguration;

	public CommonFunctionFactory(QueryEngine queryEngine) {
		functionRegistry = queryEngine.getSqmFunctionRegistry();
		typeConfiguration = queryEngine.getTypeConfiguration();
		
		BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
		dateType = basicTypeRegistry.resolve(StandardBasicTypes.DATE);
		timeType = basicTypeRegistry.resolve(StandardBasicTypes.TIME);
		timestampType = basicTypeRegistry.resolve(StandardBasicTypes.TIMESTAMP);
		longType = basicTypeRegistry.resolve(StandardBasicTypes.LONG);
		characterType = basicTypeRegistry.resolve(StandardBasicTypes.CHARACTER);
		booleanType = basicTypeRegistry.resolve(StandardBasicTypes.BOOLEAN);
		stringType = basicTypeRegistry.resolve(StandardBasicTypes.STRING);
		integerType = basicTypeRegistry.resolve(StandardBasicTypes.INTEGER);
		doubleType = basicTypeRegistry.resolve(StandardBasicTypes.DOUBLE);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// trigonometric/geometric functions

	public void cosh() {
		functionRegistry.namedDescriptorBuilder( "cosh" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	public void cot() {
		functionRegistry.namedDescriptorBuilder( "cot" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.setInvariantType(doubleType)
				.register();
	}

	public void degrees() {
		functionRegistry.namedDescriptorBuilder( "degrees" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.setInvariantType(doubleType)
				.register();
	}

	public void log() {
		functionRegistry.namedDescriptorBuilder( "log" )
				.setArgumentCountBetween( 1, 2 )
				.setParameterTypes(NUMERIC, NUMERIC)
				.setInvariantType(doubleType)
				.register();
	}

	public void ln_log() {
		functionRegistry.namedDescriptorBuilder( "ln", "log" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	public void log10() {
		functionRegistry.namedDescriptorBuilder( "log10" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.setInvariantType(doubleType)
				.register();
	}

	/**
	 * For Oracle and HANA
	 */
	public void log10_log() {
		functionRegistry.patternDescriptorBuilder( "log10", "log(10,?1)" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.setInvariantType(doubleType)
				.register();
	}

	public void log2() {
		functionRegistry.namedDescriptorBuilder( "log2" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	public void radians() {
		functionRegistry.namedDescriptorBuilder( "radians" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.setInvariantType(doubleType)
				.register();
	}

	public void sinh() {
		functionRegistry.namedDescriptorBuilder( "sinh" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.setInvariantType(doubleType)
				.register();
	}

	public void tanh() {
		functionRegistry.namedDescriptorBuilder( "tanh" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.setInvariantType(doubleType)
				.register();
	}

	public void moreHyperbolic() {
		functionRegistry.namedDescriptorBuilder( "acosh" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
		functionRegistry.namedDescriptorBuilder( "asinh" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
		functionRegistry.namedDescriptorBuilder( "atanh" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// basic math functions

	public void trunc() {
		functionRegistry.namedDescriptorBuilder( "trunc" )
				.setArgumentCountBetween( 1, 2 )
				.setParameterTypes(NUMERIC, INTEGER)
				.setInvariantType(doubleType)
				.setArgumentListSignature( "(NUMERIC number[, INTEGER places])" )
				.register();
	}

	public void truncate() {
		functionRegistry.namedDescriptorBuilder( "truncate" )
				.setExactArgumentCount( 2 ) //some databases allow 1 arg but in these it's a synonym for trunc()
				.setParameterTypes(NUMERIC, INTEGER)
				.setInvariantType(doubleType)
				.setArgumentListSignature( "(NUMERIC number, INTEGER places)" )
				.register();
	}

	/**
	 * SQL Server
	 */
	public void truncate_round() {
		functionRegistry.patternDescriptorBuilder( "truncate", "round(?1,?2,1)" )
				.setExactArgumentCount( 2 )
				.setParameterTypes(NUMERIC, INTEGER)
				.setInvariantType(doubleType)
				.setArgumentListSignature( "(NUMERIC number, INTEGER places)" )
				.register();
	}

	/**
	 * Returns double between 0.0 and 1.0. First call may specify a seed value.
	 */
	public void rand() {
		functionRegistry.namedDescriptorBuilder( "rand" )
				.setArgumentCountBetween( 0, 1 )
				.setParameterTypes(INTEGER)
				.setUseParenthesesWhenNoArgs( true )
				.setInvariantType(doubleType)
				.setArgumentListSignature( "([INTEGER seed])" )
				.register();
	}

	public void median() {
		functionRegistry.namedAggregateDescriptorBuilder( "median" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	public void median_percentileCont(boolean over) {
		functionRegistry.patternDescriptorBuilder(
						"median",
						"percentile_cont(0.5) within group (order by ?1)"
								+ ( over ? " over()" : "" )
				)
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	/**
	 * Warning: the semantics of this function are inconsistent between DBs.
	 *
	 * - On Postgres it means stdev_samp()
	 * - On Oracle, DB2, MySQL it means stdev_pop()
	 */
	public void stddev() {
		functionRegistry.namedAggregateDescriptorBuilder( "stddev" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	/**
	 * Warning: the semantics of this function are inconsistent between DBs.
	 *
	 * - On Postgres it means var_samp()
	 * - On Oracle, DB2, MySQL it means var_pop()
	 */
	public void variance() {
		functionRegistry.namedAggregateDescriptorBuilder( "variance" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	public void stddevPopSamp() {
		functionRegistry.namedAggregateDescriptorBuilder( "stddev_pop" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
		functionRegistry.namedAggregateDescriptorBuilder( "stddev_samp" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	public void varPopSamp() {
		functionRegistry.namedAggregateDescriptorBuilder( "var_pop" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
		functionRegistry.namedAggregateDescriptorBuilder( "var_samp" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	public void covarPopSamp() {
		functionRegistry.namedAggregateDescriptorBuilder( "covar_pop" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(NUMERIC, NUMERIC)
				.register();
		functionRegistry.namedAggregateDescriptorBuilder( "covar_samp" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(NUMERIC, NUMERIC)
				.register();
	}

	public void corr() {
		functionRegistry.namedAggregateDescriptorBuilder( "corr" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(NUMERIC, NUMERIC)
				.register();
	}

	public void regrLinearRegressionAggregates() {

		Arrays.asList(
						"regr_avgx", "regr_avgy", "regr_count", "regr_intercept", "regr_r2",
						"regr_slope", "regr_sxx", "regr_sxy", "regr_syy"
				)
				.forEach(
						fnName -> functionRegistry.namedAggregateDescriptorBuilder( fnName )
								.setInvariantType(doubleType)
								.setExactArgumentCount( 2 )
								.setParameterTypes(NUMERIC, NUMERIC)
								.register()
				);
	}

	/**
	 * DB2
	 */
	public void stdevVarianceSamp() {
		functionRegistry.namedAggregateDescriptorBuilder( "stddev_samp" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
		functionRegistry.namedAggregateDescriptorBuilder( "variance_samp" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	/**
	 * SQL Server-style
	 */
	public void stddevPopSamp_stdevp() {
		functionRegistry.namedAggregateDescriptorBuilder( "stdev" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
		functionRegistry.namedAggregateDescriptorBuilder( "stdevp" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
		functionRegistry.registerAlternateKey( "stddev_samp", "stdev" );
		functionRegistry.registerAlternateKey( "stddev_pop", "stdevp" );
	}

	/**
	 * SQL Server-style
	 */
	public void varPopSamp_varp() {
		functionRegistry.namedAggregateDescriptorBuilder( "var" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
		functionRegistry.namedAggregateDescriptorBuilder( "varp" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
		functionRegistry.registerAlternateKey( "var_samp", "var" );
		functionRegistry.registerAlternateKey( "var_pop", "varp" );
	}

	public void pi() {
		functionRegistry.noArgsBuilder( "pi" )
				.setInvariantType(doubleType)
				.setUseParenthesesWhenNoArgs( true )
				.register();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// character functions

	public void soundex() {
		functionRegistry.namedDescriptorBuilder( "soundex" )
				.setExactArgumentCount( 1 )
				.setInvariantType(stringType)
				.register();
	}

	public void trim2() {
		functionRegistry.namedDescriptorBuilder( "ltrim" )
				.setInvariantType(stringType)
				.setArgumentCountBetween( 1, 2 )
				.setParameterTypes(STRING, STRING)
				.setArgumentListSignature( "(STRING string[, STRING characters])" )
				.register();
		functionRegistry.namedDescriptorBuilder( "rtrim" )
				.setInvariantType(stringType)
				.setArgumentCountBetween( 1, 2 )
				.setParameterTypes(STRING, STRING)
				.setArgumentListSignature( "(STRING string[, STRING characters])" )
				.register();
	}

	public void trim1() {
		functionRegistry.namedDescriptorBuilder( "ltrim" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(STRING)
				.setArgumentListSignature( "(STRING string)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "rtrim" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(STRING)
				.setArgumentListSignature( "(STRING string)" )
				.register();
	}

	public void pad() {
		functionRegistry.namedDescriptorBuilder( "lpad" )
				.setInvariantType(stringType)
				.setArgumentCountBetween( 2, 3 )
				.setParameterTypes(STRING, INTEGER, STRING)
				.setArgumentListSignature( "(STRING string, INTEGER length[, STRING padding])" )
				.register();
		functionRegistry.namedDescriptorBuilder( "rpad" )
				.setInvariantType(stringType)
				.setArgumentCountBetween( 2, 3 )
				.setParameterTypes(STRING, INTEGER, STRING)
				.setArgumentListSignature( "(STRING string, INTEGER length[, STRING padding])" )
				.register();
	}

	/**
	 * In MySQL the third argument is required
	 */
	public void pad_space() {
		functionRegistry.registerBinaryTernaryPattern(
				"lpad",
				stringType,
				"lpad(?1,?2,' ')",
				"lpad(?1,?2,?3)",
				STRING, INTEGER, STRING
		).setArgumentListSignature( "(string, length[, padding])" );
		functionRegistry.registerBinaryTernaryPattern(
				"rpad",
				stringType,
				"rpad(?1,?2,' ')",
				"rpad(?1,?2,?3)",
				STRING, INTEGER, STRING
		).setArgumentListSignature( "(string, length[, padding])" );
	}

	/**
	 * Transact-SQL
	 */
	public void pad_replicate() {
		functionRegistry.registerBinaryTernaryPattern(
				"lpad",
				stringType,
				"(space(?2-len(?1))+?1)",
				"(replicate(?3,?2-len(?1))+?1)",
				STRING, INTEGER, STRING
		).setArgumentListSignature( "(string, length[, padding])" );
		functionRegistry.registerBinaryTernaryPattern(
				"rpad",
				stringType,
				"(?1+space(?2-len(?1)))",
				"(?1+replicate(?3,?2-len(?1)))",
				STRING, INTEGER, STRING
		).setArgumentListSignature( "(string, length[, padding])" );
	}

	public void pad_repeat() {
		functionRegistry.registerBinaryTernaryPattern(
				"lpad",
				stringType,
				"(repeat(' ',?2-character_length(?1))||?1)",
				"(repeat(?3,?2-character_length(?1))||?1)",
				STRING, INTEGER, STRING
		).setArgumentListSignature( "(string, length[, padding])" );
		functionRegistry.registerBinaryTernaryPattern(
				"rpad",
				stringType,
				"(?1||repeat(' ',?2-character_length(?1)))",
				"(?1||repeat(?3,?2-character_length(?1)))",
				STRING, INTEGER, STRING
		).setArgumentListSignature( "(string, length[, padding])" );
	}

	/**
	 * SAP DB
	 */
	public void pad_fill() {
		functionRegistry.registerBinaryTernaryPattern(
				"lpad",
				stringType,
				"lfill(?1,' ',?2)",
				"lfill(?1,?3,?2)",
				STRING, INTEGER, STRING
		).setArgumentListSignature( "(string, length[, padding])" );
		functionRegistry.registerBinaryTernaryPattern(
				"rpad",
				stringType,
				"rfill(?1,' ',?2)",
				"rfill(?1,?3,?2)",
				STRING, INTEGER, STRING
		).setArgumentListSignature( "(string, length[, padding])" );
	}

	public void reverse() {
		functionRegistry.namedDescriptorBuilder( "reverse" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(STRING)
				.register();
	}

	public void space() {
		functionRegistry.namedDescriptorBuilder( "space" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(INTEGER)
				.register();
	}

	public void repeat() {
		functionRegistry.namedDescriptorBuilder( "repeat" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(STRING, INTEGER)
				.setArgumentListSignature( "(STRING string, INTEGER times)" )
				.register();
	}

	public void leftRight() {
		functionRegistry.namedDescriptorBuilder( "left" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(STRING, INTEGER)
				.setArgumentListSignature( "(STRING string, INTEGER length)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "right" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(STRING, INTEGER)
				.setArgumentListSignature( "(STRING string, INTEGER length)" )
				.register();
	}

	public void leftRight_substr() {
		functionRegistry.patternDescriptorBuilder( "left", "substr(?1,1,?2)" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(STRING, INTEGER)
				.setArgumentListSignature( "(STRING string, INTEGER length)" )
				.register();
		functionRegistry.patternDescriptorBuilder( "right", "substr(?1,-?2)" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(STRING, INTEGER)
				.setArgumentListSignature( "(STRING string, INTEGER length)" )
				.register();
	}

	public void leftRight_substrLength() {
		functionRegistry.patternDescriptorBuilder( "left", "substr(?1,1,?2)" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(STRING, INTEGER)
				.setArgumentListSignature( "(STRING string, INTEGER length)" )
				.register();
		functionRegistry.patternDescriptorBuilder( "right", "substr(?1,length(?1)-?2+1)" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(STRING, INTEGER)
				.setArgumentListSignature( "(STRING string, INTEGER length)" )
				.register();
	}

	public void repeat_replicate() {
		functionRegistry.namedDescriptorBuilder( "replicate" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(STRING, INTEGER)
				.setArgumentListSignature( "(STRING string, INTEGER times)" )
				.register();
		functionRegistry.registerAlternateKey( "repeat", "replicate" );
	}

	public void md5() {
		functionRegistry.namedDescriptorBuilder( "md5" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 1 )
				.register();
	}

	public void initcap() {
		functionRegistry.namedDescriptorBuilder( "initcap" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 1 )
				.register();
	}

	public void instr() {
		functionRegistry.namedDescriptorBuilder( "instr" )
				.setInvariantType(integerType)
				.setArgumentCountBetween( 2, 4 )
				.setParameterTypes(STRING, STRING, INTEGER, INTEGER)
				.setArgumentListSignature( "(STRING string, STRING pattern[, INTEGER start[, INTEGER occurrence]])" )
				.register();
	}

	public void substr() {
		functionRegistry.namedDescriptorBuilder( "substr" )
				.setInvariantType(stringType)
				.setArgumentCountBetween( 2, 3 )
				.setParameterTypes(STRING, INTEGER, INTEGER)
				.setArgumentListSignature( "(STRING string, INTEGER start[, INTEGER length])" )
				.register();
	}

	public void translate() {
		functionRegistry.namedDescriptorBuilder( "translate" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 3 )
				.register();
	}

	public void bitand() {
		functionRegistry.namedDescriptorBuilder( "bitand" )
				.setExactArgumentCount( 2 )
				.register();
	}

	public void bitor() {
		functionRegistry.namedDescriptorBuilder( "bitor" )
				.setExactArgumentCount( 2 )
				.register();
	}

	public void bitxor() {
		functionRegistry.namedDescriptorBuilder( "bitxor" )
				.setExactArgumentCount( 2 )
				.register();
	}

	public void bitnot() {
		functionRegistry.namedDescriptorBuilder( "bitnot" )
				.setExactArgumentCount( 1 )
				.register();
	}

	/**
	 * Binary bitwise operators, not aggregate functions!
	 */
	public void bitandorxornot_bitAndOrXorNot() {
		functionRegistry.namedDescriptorBuilder( "bit_and" )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.registerAlternateKey( "bitand", "bit_and" );

		functionRegistry.namedDescriptorBuilder( "bit_or" )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.registerAlternateKey( "bitor", "bit_or" );

		functionRegistry.namedDescriptorBuilder( "bit_xor" )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.registerAlternateKey( "bitxor", "bit_xor" );

		functionRegistry.namedDescriptorBuilder( "bit_not" )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.registerAlternateKey( "bitnot", "bit_not" );
	}

	/**
	 * Bitwise operators, not aggregate functions!
	 */
	public void bitandorxornot_binAndOrXorNot() {
		functionRegistry.namedDescriptorBuilder( "bin_and" )
				.setMinArgumentCount( 1 )
				.register();
		functionRegistry.registerAlternateKey( "bitand", "bin_and" );

		functionRegistry.namedDescriptorBuilder( "bin_or" )
				.setMinArgumentCount( 1 )
				.register();
		functionRegistry.registerAlternateKey( "bitor", "bin_or" );

		functionRegistry.namedDescriptorBuilder( "bin_xor" )
				.setMinArgumentCount( 1 )
				.register();
		functionRegistry.registerAlternateKey( "bitxor", "bin_xor" );

		functionRegistry.namedDescriptorBuilder( "bin_not" )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.registerAlternateKey( "bitnot", "bin_not" );
	}

	/**
	 * Binary bitwise operators, not aggregate functions!
	 */
	public void bitandorxornot_operator() {
		functionRegistry.patternDescriptorBuilder( "bitand", "(?1&?2)" )
				.setExactArgumentCount( 2 )
				.register();

		functionRegistry.patternDescriptorBuilder( "bitor", "(?1|?2)" )
				.setExactArgumentCount( 2 )
				.register();

		functionRegistry.patternDescriptorBuilder( "bitxor", "(?1^?2)" )
				.setExactArgumentCount( 2 )
				.register();

		functionRegistry.patternDescriptorBuilder( "bitnot", "~?1" )
				.setExactArgumentCount( 1 )
				.register();
	}

	/**
	 * These are aggregate functions taking one argument!
	 */
	public void bitAndOr() {
		functionRegistry.namedAggregateDescriptorBuilder( "bit_and" )
				.setExactArgumentCount( 1 )
				.register();

		functionRegistry.namedAggregateDescriptorBuilder( "bit_or" )
				.setExactArgumentCount( 1 )
				.register();

		//MySQL has it but how is that even useful?
//		functionRegistry.namedTemplateBuilder( "bit_xor" )
//				.setExactArgumentCount( 1 )
//				.register();
	}

	/**
	 * These are aggregate functions taking one argument!
	 */
	public void everyAny() {
		functionRegistry.namedAggregateDescriptorBuilder( "every" )
				.setExactArgumentCount( 1 )
				.setInvariantType(booleanType)
				.setParameterTypes(BOOLEAN)
				.setArgumentListSignature( "(BOOLEAN predicate)" )
				.register();

		functionRegistry.namedAggregateDescriptorBuilder( "any" )
				.setExactArgumentCount( 1 )
				.setInvariantType(booleanType)
				.setParameterTypes(BOOLEAN)
				.setArgumentListSignature( "(BOOLEAN predicate)" )
				.register();
	}

	/**
	 * These are aggregate functions taking one argument, for
	 * databases that can directly aggregate both boolean columns
	 * and predicates!
	 */
	public void everyAny_boolAndOr() {
		functionRegistry.namedAggregateDescriptorBuilder( "bool_and" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(BOOLEAN)
				.setInvariantType(booleanType)
				.setArgumentListSignature( "(BOOLEAN predicate)" )
				.register();
		functionRegistry.registerAlternateKey( "every", "bool_and" );

		functionRegistry.namedAggregateDescriptorBuilder( "bool_or" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(BOOLEAN)
				.setInvariantType(booleanType)
				.setArgumentListSignature( "(BOOLEAN predicate)" )
				.register();
		functionRegistry.registerAlternateKey( "any", "bool_or" );
	}

	/**
	 * These are aggregate functions taking one argument,
	 * for databases that have to emulate the boolean
	 * aggregation functions using sum() and case.
	 */
	public void everyAny_sumCase() {
		functionRegistry.register( "every",
				new EveryAnyEmulation( typeConfiguration, true ) );
		functionRegistry.register( "any",
				new EveryAnyEmulation( typeConfiguration, false ) );
	}

	/**
	 * These are aggregate functions taking one argument,
	 * for SQL Server.
	 */
	public void everyAny_sumIif() {
		functionRegistry.register( "every",
				new SQLServerEveryAnyEmulation( typeConfiguration, true ) );
		functionRegistry.register( "any",
				new SQLServerEveryAnyEmulation( typeConfiguration, false ) );
	}


	/**
	 * These are aggregate functions taking one argument,
	 * for Oracle and Sybase.
	 */
	public void everyAny_sumCaseCase() {
		functionRegistry.register( "every",
				new CaseWhenEveryAnyEmulation( typeConfiguration, true ) );
		functionRegistry.register( "any",
				new CaseWhenEveryAnyEmulation( typeConfiguration, false ) );
	}

	/**
	 * Note that we include these for completeness, but
	 * since their names collide with the HQL abbreviations
	 * for extract(), they can't actually be called from HQL.
	 */
	public void yearMonthDay() {
		functionRegistry.namedDescriptorBuilder( "day" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
		functionRegistry.namedDescriptorBuilder( "month" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
		functionRegistry.namedDescriptorBuilder( "year" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
	}

	/**
	 * Note that we include these for completeness, but
	 * since their names collide with the HQL abbreviations
	 * for extract(), they can't actually be called from HQL.
	 */
	public void hourMinuteSecond() {
		functionRegistry.namedDescriptorBuilder( "hour" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(TIME)
				.register();
		functionRegistry.namedDescriptorBuilder( "minute" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(TIME)
				.register();
		functionRegistry.namedDescriptorBuilder( "second" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(TIME)
				.register();
		functionRegistry.namedDescriptorBuilder( "microsecond" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(TIME)
				.register();
	}

	public void dayofweekmonthyear() {
		functionRegistry.namedDescriptorBuilder( "dayofweek" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
		functionRegistry.namedDescriptorBuilder( "dayofmonth" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
		functionRegistry.registerAlternateKey( "day", "dayofmonth" );
		functionRegistry.namedDescriptorBuilder( "dayofyear" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
	}

	public void dayOfWeekMonthYear() {
		functionRegistry.namedDescriptorBuilder( "day_of_week" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
		functionRegistry.namedDescriptorBuilder( "day_of_month" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
		functionRegistry.registerAlternateKey( "day", "day_of_month" );
		functionRegistry.namedDescriptorBuilder( "day_of_year" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
	}

	public void daynameMonthname() {
		functionRegistry.namedDescriptorBuilder( "monthname" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
		functionRegistry.namedDescriptorBuilder( "dayname" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
	}

	public void weekQuarter() {
		functionRegistry.namedDescriptorBuilder( "week" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
		functionRegistry.namedDescriptorBuilder( "quarter" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.setInvariantType(integerType)
				.register();
	}

	public void lastDay() {
		functionRegistry.namedDescriptorBuilder( "last_day" )
				.setInvariantType(dateType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
	}

	public void lastDay_eomonth() {
		functionRegistry.namedDescriptorBuilder( "eomonth" )
				.setInvariantType(dateType)
				.setArgumentCountBetween( 1, 2 )
				.setParameterTypes(DATE, INTEGER)
				.register();
		functionRegistry.registerAlternateKey( "last_date", "eomonth" );
	}

	public void ceiling_ceil() {
		functionRegistry.namedDescriptorBuilder( "ceil" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				// To avoid truncating to a specific data type, we default to using the argument type
				.setReturnTypeResolver( useArgType( 1 ) )
				.register();
		functionRegistry.registerAlternateKey( "ceiling", "ceil" );
	}

	public void toCharNumberDateTimestamp() {
		//argument counts are right for Oracle, TimesTen, and CUBRID
		functionRegistry.namedDescriptorBuilder( "to_number" )
				//always 1 arg on HSQL and Cache, always 2 on Postgres
				.setArgumentCountBetween( 1, 3 )
				.setInvariantType(doubleType)
				.register();
		functionRegistry.namedDescriptorBuilder( "to_char" )
				.setArgumentCountBetween( 1, 3 )
				//always 2 args on HSQL and Postgres
				.setInvariantType(stringType)
				.register();
		functionRegistry.namedDescriptorBuilder( "to_date" )
				//always 2 args on HSQL and Postgres
				.setArgumentCountBetween( 1, 3 )
				.setInvariantType(dateType)
				.register();
		functionRegistry.namedDescriptorBuilder( "to_timestamp" )
				//always 2 args on HSQL and Postgres
				.setArgumentCountBetween( 1, 3 )
				.setInvariantType(timestampType)
				.register();
	}

	public void dateTimeTimestamp() {
		date();
		time();
		timestamp();
	}

	public void timestamp() {
		functionRegistry.namedDescriptorBuilder( "timestamp" )
				.setArgumentCountBetween( 1, 2 )
				//accepts (DATE,TIME) (DATE,INTEGER) or DATE or STRING
				.setInvariantType(timestampType)
				.register();
	}

	public void time() {
		functionRegistry.namedDescriptorBuilder( "time" )
				.setExactArgumentCount( 1 )
				//accepts TIME or STRING
				.setInvariantType(timeType)
				.register();
	}

	public void date() {
		functionRegistry.namedDescriptorBuilder( "date" )
				.setExactArgumentCount( 1 )
				//accepts DATE or STRING
				.setInvariantType(dateType)
				.register();
	}

	public void utcDateTimeTimestamp() {
		functionRegistry.noArgsBuilder( "utc_date" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType(dateType)
				.register();
		functionRegistry.noArgsBuilder( "utc_time" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType(timeType)
				.register();
		functionRegistry.noArgsBuilder( "utc_timestamp" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType(timestampType)
				.register();
	}

	public void currentUtcdatetimetimestamp() {
		functionRegistry.noArgsBuilder( "current_utcdate" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType(dateType)
				.register();
		functionRegistry.noArgsBuilder( "current_utctime" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType(timeType)
				.register();
		functionRegistry.noArgsBuilder( "current_utctimestamp" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType(timestampType)
				.register();
	}

	public void week_weekofyear() {
		functionRegistry.namedDescriptorBuilder( "weekofyear" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
		functionRegistry.registerAlternateKey( "week", "weekofyear" );
	}

	/**
	 * Almost every database
	 */
	public void concat_pipeOperator() {
		functionRegistry.patternDescriptorBuilder( "concat", "(?1||?2...)" )
				.setInvariantType(stringType)
				.setMinArgumentCount( 1 )
				.setParameterTypes(STRING)
				.setArgumentListSignature( "(STRING string0[, STRING string1[, ...]])" )
				.register();
	}

	/**
	 * Transact SQL-style
	 */
	public void concat_plusOperator() {
		functionRegistry.patternDescriptorBuilder( "concat", "(?1+?2...)" )
				.setInvariantType(stringType)
				.setMinArgumentCount( 1 )
				.setParameterTypes(STRING)
				.setArgumentListSignature( "(STRING string0[, STRING string1[, ...]])" )
				.register();
	}

	/**
	 * Oracle-style
	 */
	public void rownumRowid() {
		functionRegistry.noArgsBuilder( "rowid" )
				.setInvariantType(longType)
				.setUseParenthesesWhenNoArgs( false )
				.register();
		functionRegistry.noArgsBuilder( "rownum" )
				.setInvariantType(longType)
				.setUseParenthesesWhenNoArgs( false )
				.register();
	}

	/**
	 * H2/HSQL-style
	 */
	public void rownum() {
		functionRegistry.noArgsBuilder( "rownum" )
				.setInvariantType(longType)
				.setUseParenthesesWhenNoArgs( true ) //H2 and HSQL require the parens
				.register();
	}

	/**
	 * CUBRID
	 */
	public void rownumInstOrderbyGroupbyNum() {
		functionRegistry.noArgsBuilder( "rownum" )
				.setInvariantType(integerType)
				.setUseParenthesesWhenNoArgs( false )
				.register();

		functionRegistry.noArgsBuilder( "inst_num" )
				.setInvariantType(integerType)
				.setUseParenthesesWhenNoArgs( true )
				.register();
		functionRegistry.noArgsBuilder( "orderby_num" )
				.setInvariantType(integerType)
				.setUseParenthesesWhenNoArgs( true )
				.register();
		functionRegistry.noArgsBuilder( "groupby_num" )
				.setInvariantType(integerType)
				.setUseParenthesesWhenNoArgs( true )
				.register();
	}

	/**
	 * MySQL/CUBRID
	 */
	public void makedateMaketime() {
		functionRegistry.namedDescriptorBuilder( "makedate" )
				.setInvariantType(dateType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(INTEGER, INTEGER)
				.setArgumentListSignature( "(INTEGER year, INTEGER dayofyear)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "maketime" )
				.setInvariantType(timeType)
				.setExactArgumentCount( 3 )
				.setParameterTypes(INTEGER, INTEGER, INTEGER)
				.setArgumentListSignature( "(INTEGER hour, INTEGER min, INTEGER sec)" )
				.register();
	}

	/**
	 * Postgres
	 */
	public void makeDateTimeTimestamp() {
		functionRegistry.namedDescriptorBuilder( "make_date" )
				.setInvariantType(dateType)
				.setExactArgumentCount( 3 )
				.setParameterTypes(INTEGER, INTEGER, INTEGER)
				.register();
		functionRegistry.namedDescriptorBuilder( "make_time" )
				.setInvariantType(timeType)
				.setExactArgumentCount( 3 )
				.setParameterTypes(INTEGER, INTEGER, INTEGER)
				.register();
		functionRegistry.namedDescriptorBuilder( "make_timestamp" )
				.setInvariantType( timestampType )
				.setExactArgumentCount( 6 )
				.setParameterTypes(INTEGER, INTEGER, INTEGER, INTEGER, INTEGER, INTEGER)
				.register();
		functionRegistry.namedDescriptorBuilder( "make_timestamptz" )
				.setInvariantType( timestampType )
				.setArgumentCountBetween( 6, 7 )
				.setParameterTypes(INTEGER, INTEGER, INTEGER, INTEGER, INTEGER, INTEGER, INTEGER)
				.register();
	}

	public void sysdate() {
		// returns a local timestamp
		functionRegistry.noArgsBuilder( "sysdate" )
				.setInvariantType(timestampType)
				.setUseParenthesesWhenNoArgs( false )
				.register();
	}

	/**
	 * MySQL requires the parens in sysdate()
	 */
	public void sysdateParens() {
		functionRegistry.noArgsBuilder( "sysdate" )
				.setInvariantType(timestampType)
				.setUseParenthesesWhenNoArgs( true )
				.register();
	}

	/**
	 * MySQL 5.7 precision defaults to seconds, but microseconds is better
	 */
	public void sysdateExplicitMicros() {
		functionRegistry.patternDescriptorBuilder( "sysdate", "sysdate(6)" )
				.setInvariantType(timestampType)
				.setExactArgumentCount( 0 )
				.register();
	}

	public void systimestamp() {
		// returns a timestamp with timezone
		functionRegistry.noArgsBuilder( "systimestamp" )
				.setInvariantType(timestampType)
				.setUseParenthesesWhenNoArgs( false )
				.register();
	}

	public void localtimeLocaltimestamp() {
		//these functions return times without timezones
		functionRegistry.noArgsBuilder( "localtime" )
				.setInvariantType(timeType)
				.setUseParenthesesWhenNoArgs( false )
				.register();
		functionRegistry.noArgsBuilder( "localtimestamp" )
				.setInvariantType(timestampType)
				.setUseParenthesesWhenNoArgs( false )
				.register();

		final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
		functionRegistry.noArgsBuilder( "local_time", "localtime" )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.LOCAL_TIME ) )
				.setUseParenthesesWhenNoArgs( false )
				.register();
		functionRegistry.noArgsBuilder( "local_datetime", "localtimestamp" )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.LOCAL_DATE_TIME ) )
				.setUseParenthesesWhenNoArgs( false )
				.register();
	}

	public void trigonometry() {
		functionRegistry.namedDescriptorBuilder( "sin" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		functionRegistry.namedDescriptorBuilder( "cos" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		functionRegistry.namedDescriptorBuilder( "tan" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		functionRegistry.namedDescriptorBuilder( "asin" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		functionRegistry.namedDescriptorBuilder( "acos" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		functionRegistry.namedDescriptorBuilder( "atan" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		functionRegistry.namedDescriptorBuilder( "atan2" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(NUMERIC, NUMERIC)
				.register();
	}

	/**
	 * Transact-SQL atan2 is misspelled
	 */
	public void atan2_atn2() {
		functionRegistry.namedDescriptorBuilder( "atan2", "atn2" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(NUMERIC, NUMERIC)
				.register();
	}

	public void coalesce() {
		functionRegistry.namedDescriptorBuilder( "coalesce" )
				.setMinArgumentCount( 1 )
				.register();
	}

	/**
	 * SAP DB
	 */
	public void coalesce_value() {
		functionRegistry.namedDescriptorBuilder( "value" )
				.setMinArgumentCount( 1 )
				.register();
		functionRegistry.registerAlternateKey( "coalesce", "value" );
	}

	public void nullif() {
		functionRegistry.namedDescriptorBuilder( "nullif" )
				.setExactArgumentCount( 2 )
				.register();
	}

	/**
	 * ANSI SQL-style
	 */
	public void length_characterLength() {
		functionRegistry.namedDescriptorBuilder( "character_length" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(STRING)
				.register();
		functionRegistry.registerAlternateKey( "length", "character_length" );
	}

	/**
	 * Transact SQL-style
	 */
	public void characterLength_len() {
		functionRegistry.namedDescriptorBuilder( "len" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(STRING)
				.register();
		functionRegistry.registerAlternateKey( "character_length", "len" );
		functionRegistry.registerAlternateKey( "length", "len" );
	}

	/**
	 * Oracle-style
	 */
	public void characterLength_length(SqlAstNodeRenderingMode argumentRenderingMode) {
		functionRegistry.namedDescriptorBuilder( "length" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(STRING)
				.setArgumentRenderingMode( argumentRenderingMode )
				.register();
		functionRegistry.registerAlternateKey( "character_length", "length" );
	}

	public void octetLength() {
		functionRegistry.namedDescriptorBuilder( "octet_length" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(STRING)
				.register();
	}

	public void bitLength() {
		functionRegistry.namedDescriptorBuilder( "bit_length" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(STRING)
				.register();
	}

	public void bitLength_pattern(String pattern) {
		functionRegistry.patternDescriptorBuilder( "bit_length", pattern )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(STRING)
				.register();
	}

	/**
	 * ANSI-style
	 */
	public void position() {
		functionRegistry.patternDescriptorBuilder( "position", "position(?1 in ?2)" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(STRING, STRING)
				.setArgumentListSignature( "(STRING pattern in STRING string)" )
				.register();
	}

	public void locate() {
		functionRegistry.namedDescriptorBuilder( "locate" )
				.setInvariantType(integerType)
				.setArgumentCountBetween( 2, 3 )
				.setParameterTypes(STRING, STRING, INTEGER)
				.setArgumentListSignature( "(STRING pattern, STRING string[, INTEGER start])" )
				.register();
	}

	/**
	 * Transact SQL-style
	 */
	public void locate_charindex() {
		functionRegistry.namedDescriptorBuilder( "charindex" )
				.setInvariantType(integerType)
				.setArgumentCountBetween( 2, 3 )
				.setParameterTypes(STRING, STRING, INTEGER)
				.setArgumentListSignature( "(STRING pattern, STRING string[, INTEGER start])" )
				.register();
		functionRegistry.registerAlternateKey( "locate", "charindex" );
	}

	/**
	 * locate() in terms of ANSI position() and substring()
	 */
	public void locate_positionSubstring() {
		functionRegistry.registerBinaryTernaryPattern(
						"locate",
						integerType,
						"position(?1 in ?2)", "(position(?1 in substring(?2 from ?3))+(?3)-1)",
						STRING, STRING, INTEGER
				)
				.setArgumentListSignature( "(STRING pattern, STRING string[, INTEGER start])" );
	}
	/**
	 * ANSI-style substring
	 */
	public void substringFromFor() {
		functionRegistry.registerBinaryTernaryPattern(
						"substring",
						stringType,
						"substring(?1 from ?2)", "substring(?1 from ?2 for ?3)",
						STRING, INTEGER, INTEGER
				)
				.setArgumentListSignature( "(STRING string{ from|,} INTEGER start[{ for|,} INTEGER length])" );
	}

	/**
	 * Not the same as ANSI-style substring!
	 */
	public void substring() {
		functionRegistry.namedDescriptorBuilder( "substring" )
				.setInvariantType(stringType)
				.setArgumentCountBetween( 2, 3 )
				.setParameterTypes(STRING, INTEGER, INTEGER)
				.setArgumentListSignature( "(STRING string{ from|,} INTEGER start[{ for|,} INTEGER length])" )
				.register();
	}

	/**
	 * Transact SQL-style (3 required args)
	 */
	public void substring_substringLen() {
		functionRegistry
				.registerBinaryTernaryPattern(
						"substring",
						stringType,
						"substring(?1,?2,len(?1)-?2+1)",
						"substring(?1,?2,?3)",
						STRING, INTEGER, INTEGER
				)
				.setArgumentListSignature( "(STRING string{ from|,} INTEGER start[{ for|,} INTEGER length])" );
	}

	/**
	 * Oracle, and many others
	 */
	public void substring_substr() {
		functionRegistry.namedDescriptorBuilder( "substring", "substr" )
				.setArgumentListSignature( "(STRING string{ from|,} INTEGER start[{ for|,} INTEGER length])" )
				.setInvariantType(stringType)
				.setArgumentCountBetween( 2, 3 )
				.setParameterTypes(STRING, INTEGER, INTEGER)
				.register();
	}

	public void insert() {
		functionRegistry.namedDescriptorBuilder( "insert" )
				.setInvariantType(stringType)
				.setParameterTypes(STRING, INTEGER, INTEGER, STRING)
				.setArgumentListSignature( "(STRING string, INTEGER start, INTEGER length, STRING replacement)" )
				.register();
	}

	/**
	 * Postgres
	 */
	public void insert_overlay() {
		functionRegistry.patternDescriptorBuilder(
						"insert",
						"overlay(?1 placing ?4 from ?2 for ?3)"
				)
				.setInvariantType(stringType)
				.setExactArgumentCount( 4 )
				.setParameterTypes(STRING, INTEGER, INTEGER, STRING)
				.setArgumentListSignature( "(STRING string, INTEGER start, INTEGER length, STRING replacement)" )
				.register();
	}

	/**
	 * ANSI SQL form, supported by Postgres, HSQL
	 */
	public void overlay() {
		functionRegistry.registerTernaryQuaternaryPattern(
						"overlay",
						stringType,
						"overlay(?1 placing ?2 from ?3)",
						"overlay(?1 placing ?2 from ?3 for ?4)",
						STRING, STRING, INTEGER, INTEGER
				)
				.setArgumentListSignature( "(string placing replacement from start[ for length])" );
	}

	/**
	 * For DB2 which has a broken implementation of overlay()
	 */
	public void overlayCharacterLength_overlay() {
		functionRegistry.registerTernaryQuaternaryPattern(
						"overlay",
						stringType,
						//use character_length() here instead of length()
						//because DB2 doesn't like "length(?)"
						"overlay(?1 placing ?2 from ?3 for character_length(?2))",
						"overlay(?1 placing ?2 from ?3 for ?4)",
						STRING, STRING, INTEGER, INTEGER
				)
				.setArgumentListSignature( "(string placing replacement from start[ for length])" );
	}

	public void replace() {
		functionRegistry.namedDescriptorBuilder( "replace" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 3 )
				.setParameterTypes(STRING, STRING, STRING)
				.setArgumentListSignature( "(STRING string, STRING pattern, STRING replacement)" )
				.register();
	}

	/**
	 * Sybase
	 */
	public void replace_strReplace() {
		functionRegistry.namedDescriptorBuilder( "str_replace" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 3 )
				.setParameterTypes(STRING, STRING, STRING)
				.setArgumentListSignature( "(STRING string, STRING pattern, STRING replacement)" )
				.register();
		functionRegistry.registerAlternateKey( "replace", "str_replace" );
	}

	public void concat() {
		functionRegistry.namedDescriptorBuilder( "concat" )
				.setInvariantType(stringType)
				.setMinArgumentCount( 1 )
				.setParameterTypes(STRING)
				.setArgumentListSignature( "(STRING string0[, STRING string1[, ...]])" )
				.register();
	}

	public void lowerUpper() {
		functionRegistry.namedDescriptorBuilder( "lower" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(STRING)
				.setArgumentListSignature( "(STRING string)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "upper" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(STRING)
				.setArgumentListSignature( "(STRING string)" )
				.register();
	}

	public void ascii() {
		functionRegistry.namedDescriptorBuilder( "ascii" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(STRING)
				.setInvariantType(integerType)//should it be BYTE??
				.register();
	}

	public void char_chr() {
		functionRegistry.namedDescriptorBuilder( "chr" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(INTEGER)
				.setInvariantType(characterType)
				.register();
		functionRegistry.registerAlternateKey( "char", "chr" );
	}

	public void chr_char() {
		functionRegistry.namedDescriptorBuilder( "char" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(INTEGER)
				.setInvariantType(characterType)
				.register();
		functionRegistry.registerAlternateKey( "chr", "char" );
	}

	/**
	 * Transact SQL-style
	 */
	public void datepartDatename() {
		functionRegistry.namedDescriptorBuilder( "datepart" )
//				.setInvariantType( StandardBasicTypes.INTEGER )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setParameterTypes(TEMPORAL_UNIT, TEMPORAL)
				.setArgumentListSignature( "(TEMPORAL_UNIT field, TEMPORAL arg)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "datename" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(TEMPORAL_UNIT, TEMPORAL)
				.setArgumentListSignature( "(TEMPORAL_UNIT field, TEMPORAL arg)" )
				.register();
	}

	// No real consistency in the semantics of these functions:
	// H2, HSQL: now()/curtime()/curdate() mean localtimestamp/localtime/current_date
	// MySQL, Cache: now()/curtime()/curdate() mean current_timestamp/current_time/current_date
	// CUBRID: now()/curtime()/curdate() mean current_datetime/current_time/current_date
	// Postgres: now() means current_timestamp
	public void nowCurdateCurtime() {
		functionRegistry.noArgsBuilder( "curtime" )
				.setInvariantType(timeType)
				.setUseParenthesesWhenNoArgs( true )
				.register();
		functionRegistry.noArgsBuilder( "curdate" )
				.setInvariantType(dateType)
				.setUseParenthesesWhenNoArgs( true )
				.register();
		functionRegistry.noArgsBuilder( "now" )
				.setInvariantType(timestampType)
				.setUseParenthesesWhenNoArgs( true )
				.register();
	}

	public void leastGreatest() {
		functionRegistry.namedDescriptorBuilder( "least" )
				.setMinArgumentCount( 2 )
				.setParameterTypes(COMPARABLE, COMPARABLE)
				.register();
		functionRegistry.namedDescriptorBuilder( "greatest" )
				.setMinArgumentCount( 2 )
				.setParameterTypes(COMPARABLE, COMPARABLE)
				.register();
	}

	public void leastGreatest_minMax() {
		functionRegistry.namedDescriptorBuilder( "least", "min" )
				.setMinArgumentCount( 2 )
				.setParameterTypes(COMPARABLE, COMPARABLE)
				.register();
		functionRegistry.namedDescriptorBuilder( "greatest", "max" )
				.setMinArgumentCount( 2 )
				.setParameterTypes(COMPARABLE, COMPARABLE)
				.register();
	}

	public void leastGreatest_minMaxValue() {
		functionRegistry.namedDescriptorBuilder( "least", "minvalue" )
				.setMinArgumentCount( 2 )
				.setParameterTypes(COMPARABLE, COMPARABLE)
				.register();
		functionRegistry.namedDescriptorBuilder( "greatest", "maxvalue" )
				.setMinArgumentCount( 2 )
				.setParameterTypes(COMPARABLE, COMPARABLE)
				.register();
	}

	public void aggregates(
			Dialect dialect,
			SqlAstNodeRenderingMode inferenceArgumentRenderingMode,
			String concatOperator,
			String concatArgumentCastType) {
		functionRegistry.namedAggregateDescriptorBuilder( "max" )
				.setArgumentRenderingMode( inferenceArgumentRenderingMode )
				.setExactArgumentCount( 1 )
				.setParameterTypes(COMPARABLE)
				.register();

		functionRegistry.namedAggregateDescriptorBuilder( "min" )
				.setArgumentRenderingMode( inferenceArgumentRenderingMode )
				.setExactArgumentCount( 1 )
				.setParameterTypes(COMPARABLE)
				.register();

		functionRegistry.namedAggregateDescriptorBuilder( "sum" )
				.setArgumentRenderingMode( inferenceArgumentRenderingMode )
				.setReturnTypeResolver( new SumReturnTypeResolver( typeConfiguration ) )
				.setExactArgumentCount( 1 )
				.register();

		functionRegistry.namedAggregateDescriptorBuilder( "avg" )
				.setArgumentRenderingMode( inferenceArgumentRenderingMode )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		functionRegistry.register(
				"count",
				new CountFunction(
						dialect,
						typeConfiguration,
						inferenceArgumentRenderingMode,
						concatOperator,
						concatArgumentCastType
				)
		);
	}

	public void avg_castingNonDoubleArguments(
			Dialect dialect,
			SqlAstNodeRenderingMode inferenceArgumentRenderingMode) {
		functionRegistry.register(
				"avg",
				new AvgFunction(
						dialect,
						typeConfiguration,
						inferenceArgumentRenderingMode
				)
		);
	}

	public void listagg(String emptyWithinReplacement) {
		functionRegistry.register(
				"listagg",
				new ListaggFunction( emptyWithinReplacement, typeConfiguration )
		);
	}

	public void listagg_groupConcat() {
		functionRegistry.register(
				ListaggGroupConcatEmulation.FUNCTION_NAME,
				new ListaggGroupConcatEmulation( typeConfiguration )
		);
	}

	public void listagg_list(String stringType) {
		functionRegistry.register(
				ListaggStringAggEmulation.FUNCTION_NAME,
				new ListaggStringAggEmulation( "list", stringType, false, typeConfiguration )
		);
	}

	public void listagg_stringAgg(String stringType) {
		functionRegistry.register(
				ListaggStringAggEmulation.FUNCTION_NAME,
				new ListaggStringAggEmulation( "string_agg", stringType, false, typeConfiguration )
		);
	}

	public void listagg_stringAggWithinGroup(String stringType) {
		functionRegistry.register(
				ListaggStringAggEmulation.FUNCTION_NAME,
				new ListaggStringAggEmulation( "string_agg", stringType, true, typeConfiguration )
		);
	}

	public void inverseDistributionOrderedSetAggregates() {
		functionRegistry.register(
				"mode",
				new InverseDistributionFunction( "mode", null, typeConfiguration )
		);
		functionRegistry.register(
				"percentile_cont",
				new InverseDistributionFunction( "percentile_cont", NUMERIC, typeConfiguration )
		);
		functionRegistry.register(
				"percentile_disc",
				new InverseDistributionFunction( "percentile_disc", NUMERIC, typeConfiguration )
		);
	}

	public void hypotheticalOrderedSetAggregates() {
		functionRegistry.register(
				"rank",
				new HypotheticalSetFunction( "rank", StandardBasicTypes.LONG, typeConfiguration )
		);
		functionRegistry.register(
				"dense_rank",
				new HypotheticalSetFunction( "dense_rank", StandardBasicTypes.LONG, typeConfiguration )
		);
		functionRegistry.register(
				"percent_rank",
				new HypotheticalSetFunction( "percent_rank", StandardBasicTypes.DOUBLE, typeConfiguration )
		);
		functionRegistry.register(
				"cume_dist",
				new HypotheticalSetFunction( "cume_dist", StandardBasicTypes.DOUBLE, typeConfiguration )
		);
	}

	public void math() {
		functionRegistry.namedDescriptorBuilder( "round" )
				// To avoid truncating to a specific data type, we default to using the argument type
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setParameterTypes(NUMERIC, INTEGER)
				.register();

		functionRegistry.namedDescriptorBuilder( "floor" )
				// To avoid truncating to a specific data type, we default to using the argument type
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		functionRegistry.namedDescriptorBuilder( "ceiling" )
				// To avoid truncating to a specific data type, we default to using the argument type
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		functionRegistry.namedDescriptorBuilder( "mod" )
				// According to JPA spec 4.6.17.2.2.
				.setInvariantType(integerType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(INTEGER, INTEGER)
				.register();

		functionRegistry.namedDescriptorBuilder( "abs" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		functionRegistry.namedDescriptorBuilder( "sign" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		//transcendental functions are by nature of floating point type

		functionRegistry.namedDescriptorBuilder( "sqrt" )
				// According to JPA spec 4.6.17.2.2.
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		functionRegistry.namedDescriptorBuilder( "ln" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		functionRegistry.namedDescriptorBuilder( "exp" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		functionRegistry.namedDescriptorBuilder( "power" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(NUMERIC, NUMERIC)
				.register();
	}

	public void mod_operator() {
		functionRegistry.patternDescriptorBuilder( "mod", "(?1%?2)" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(INTEGER, INTEGER)
				.register();
	}

	public void power_expLn() {
		functionRegistry.patternDescriptorBuilder( "power", "exp(ln(?1)*?2)" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(NUMERIC, NUMERIC)
				.register();
	}

	public void round_floor() {
		functionRegistry.patternDescriptorBuilder( "round", "floor(?1*1e?2+0.5)/1e?2")
				.setReturnTypeResolver( useArgType(1) )
				.setExactArgumentCount( 2 )
				.setParameterTypes(NUMERIC, INTEGER)
				.register();
	}

	public void square() {
		functionRegistry.namedDescriptorBuilder( "square" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	public void cbrt() {
		functionRegistry.namedDescriptorBuilder( "cbrt" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	public void crc32() {
		functionRegistry.namedDescriptorBuilder( "crc32" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.register();
	}

	public void sha1() {
		functionRegistry.namedDescriptorBuilder( "sha1" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 1 )
				.register();
	}

	public void sha2() {
		functionRegistry.namedDescriptorBuilder( "sha2" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 2 )
				.register();
	}

	public void sha() {
		functionRegistry.namedDescriptorBuilder( "sha" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 1 )
				.register();
	}

	/**
	 * MySQL style, returns the number of days between two dates
	 */
	public void datediff() {
		functionRegistry.namedDescriptorBuilder( "datediff" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(DATE, DATE)
				.setArgumentListSignature( "(DATE end, DATE start)" )
				.register();
	}

	/**
	 * MySQL style
	 */
	public void adddateSubdateAddtimeSubtime() {
		functionRegistry.namedDescriptorBuilder( "adddate" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setParameterTypes(DATE, INTEGER)
				.setArgumentListSignature( "(DATE datetime, INTEGER days)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "subdate" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setParameterTypes(DATE, INTEGER)
				.setArgumentListSignature( "(DATE datetime, INTEGER days)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "addtime" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setParameterTypes(TIME, TIME)
				.setArgumentListSignature( "(TIME datetime, TIME time)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "subtime" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setParameterTypes(TIME, TIME)
				.setArgumentListSignature( "(TIME datetime, TIME time)" )
				.register();
	}

	public void addMonths() {
		functionRegistry.namedDescriptorBuilder( "add_months" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setArgumentListSignature( "(DATE datetime, INTEGER months)" )
				.setExactArgumentCount( 2 )
				.setParameterTypes(DATE, INTEGER)
				.register();
	}

	public void monthsBetween() {
		functionRegistry.namedDescriptorBuilder( "months_between" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(DATE end, DATE start)" )
				.setParameterTypes(DATE, DATE)
				.register();
	}

	public void daysBetween() {
		functionRegistry.namedDescriptorBuilder( "days_between" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(DATE, DATE)
				.setArgumentListSignature( "(DATE end, DATE start)" )
				.register();
	}

	public void secondsBetween() {
		functionRegistry.namedDescriptorBuilder( "seconds_between" )
				.setInvariantType(longType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(TIME, TIME)
				.setArgumentListSignature( "(TIME end, TIME start)" )
				.register();
	}

	public void yearsMonthsDaysHoursMinutesSecondsBetween() {
		functionRegistry.namedDescriptorBuilder( "years_between" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(DATE, DATE)
				.setArgumentListSignature( "(DATE end, DATE start)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "months_between" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(DATE, DATE)
				.setArgumentListSignature( "(DATE end, DATE start)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "days_between" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(DATE, DATE)
				.setArgumentListSignature( "(DATE end, DATE start)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "hours_between" )
				.setInvariantType(longType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(TIME, TIME)
				.setArgumentListSignature( "(TIME end, TIME start)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "minutes_between" )
				.setInvariantType(longType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(TIME, TIME)
				.setArgumentListSignature( "(TIME end, TIME start)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "seconds_between" )
				.setInvariantType(longType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(TIME, TIME)
				.setArgumentListSignature( "(TIME end, TIME start)" )
				.register();
	}

	public void addYearsMonthsDaysHoursMinutesSeconds() {
		functionRegistry.namedDescriptorBuilder( "add_years" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setParameterTypes(DATE, INTEGER)
				.setArgumentListSignature( "(DATE datetime, INTEGER years)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "add_months" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setParameterTypes(DATE, INTEGER)
				.setArgumentListSignature( "(DATE datetime, INTEGER months)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "add_days" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setParameterTypes(DATE, INTEGER)
				.setArgumentListSignature( "(DATE datetime, INTEGER days)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "add_hours" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setParameterTypes(TIME, INTEGER)
				.setArgumentListSignature( "(TIME datetime, INTEGER hours)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "add_minutes" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setParameterTypes(TIME, INTEGER)
				.setArgumentListSignature( "(TIME datetime, INTEGER minutes)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "add_seconds" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setParameterTypes(TIME, INTEGER)
				.setArgumentListSignature( "(TIME datetime, INTEGER seconds)" )
				.register();
	}

	/**
	 * H2-style (uses Java's SimpleDateFormat directly so no need to translate format)
	 */
	public void format_formatdatetime() {
		functionRegistry.namedDescriptorBuilder( "format", "formatdatetime" )
				.setInvariantType(stringType)
				.setArgumentsValidator( formatValidator() )
				.setArgumentListSignature( "(TEMPORAL datetime as STRING pattern)" )
				.register();
	}

	/**
	 * Usually Oracle-style (except for Informix which quite close to MySQL-style)
	 *
	 * @see org.hibernate.dialect.OracleDialect#datetimeFormat
	 */
	public void format_toChar() {
		functionRegistry.namedDescriptorBuilder( "format", "to_char" )
				.setInvariantType(stringType)
				.setArgumentsValidator( formatValidator() )
				.setArgumentListSignature( "(TEMPORAL datetime as STRING pattern)" )
				.register();
	}

	/**
	 * MySQL-style (also Ingres)
	 *
	 * @see org.hibernate.dialect.MySQLDialect#datetimeFormat
	 */
	public void format_dateFormat() {
		functionRegistry.namedDescriptorBuilder( "format", "date_format" )
				.setInvariantType(stringType)
				.setArgumentsValidator( formatValidator() )
				.setArgumentListSignature( "(TEMPORAL datetime as STRING pattern)" )
				.register();
	}

	/**
	 * HANA's name for to_char() is still Oracle-style
	 *
	 *  @see org.hibernate.dialect.OracleDialect#datetimeFormat
	 */
	public void format_toVarchar() {
		functionRegistry.namedDescriptorBuilder( "format", "to_varchar" )
				.setInvariantType(stringType)
				.setArgumentsValidator( formatValidator() )
				.setArgumentListSignature( "(TEMPORAL datetime as STRING pattern)" )
				.register();
	}

	public static ArgumentsValidator formatValidator() {
		return new ArgumentTypesValidator( StandardArgumentsValidators.exactly( 2 ), TEMPORAL, STRING );
	}

	/**
	 * Use the 'collate' operator which exists on at least Postgres, MySQL, Oracle, and SQL Server
	 */
	public void collate() {
		functionRegistry.patternDescriptorBuilder("collate", "(?1 collate ?2)")
				.setInvariantType(stringType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(STRING, COLLATION)
				.setArgumentListSignature("(STRING string as COLLATION collation)")
				.register();
	}

	/**
	 * HSQL requires quotes around certain collations
	 */
	public void collate_quoted() {
		functionRegistry.patternDescriptorBuilder("collate", "(?1 collate '?2')")
				.setInvariantType(stringType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(STRING, ANY)
				.setArgumentListSignature("(STRING string as COLLATION collation)")
				.register();
	}

	public void dateTrunc() {
		functionRegistry.patternDescriptorBuilder( "date_trunc", "date_trunc('?1',?2)" )
				.setInvariantType(timestampType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(TEMPORAL_UNIT, TEMPORAL)
				.setArgumentListSignature( "(TEMPORAL_UNIT field, TEMPORAL datetime)" )
				.register();
	}

}
