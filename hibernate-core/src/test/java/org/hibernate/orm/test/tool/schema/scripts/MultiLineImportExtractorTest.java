/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schema.scripts;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.List;

import org.hibernate.tool.schema.internal.script.MultiLineSqlScriptExtractor;

import org.hibernate.testing.orm.junit.DialectContext;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class MultiLineImportExtractorTest {
	public static final String IMPORT_FILE = "org/hibernate/orm/test/tool/schema/scripts/multi-line-statements2.sql";

	private final MultiLineSqlScriptExtractor extractor = new MultiLineSqlScriptExtractor();

	@Test
	public void testExtraction() throws IOException {
		final ClassLoader classLoader = ClassLoader.getSystemClassLoader();

		try (final InputStream stream = classLoader.getResourceAsStream( IMPORT_FILE )) {
			assertThat( stream, notNullValue() );
			try (final InputStreamReader reader = new InputStreamReader( stream )) {
				final List<String> commands = extractor.extractCommands( reader, DialectContext.getDialect() );
				assertThat( commands, notNullValue() );
				assertThat( commands.size(), is( 6 ) );

				assertThat( commands.get( 0 ), startsWith( "CREATE TABLE test_data" ) );

				assertThat( commands.get( 1 ), is( "INSERT INTO test_data VALUES (1, 'sample')" ) );

				assertThat( commands.get( 2 ), is( "DELETE   FROM test_data" ) );

				assertThat( commands.get( 3 ), startsWith( "INSERT INTO test_data VALUES (2," ) );
				assertThat( commands.get( 3 ), containsString( "-- line 2" ) );

				assertThat( commands.get( 4 ), startsWith( "INSERT INTO test_data VALUES (3" ) );
				assertThat( commands.get( 4 ), not( containsString( "third record" ) ) );

				assertThat( commands.get( 5 ).replace( "\t", "" ), is( "INSERT INTO test_data VALUES (     4       , NULL     )" ) );
			}
		}
	}

	@Test
	public void testExtractionFromEmptyScript() {
		StringReader reader = new StringReader( "" );
		final List<String> commands = extractor.extractCommands( reader, DialectContext.getDialect() );
		assertThat( commands, notNullValue() );
		assertThat( commands.size(), is( 0 ) );
	}
}
