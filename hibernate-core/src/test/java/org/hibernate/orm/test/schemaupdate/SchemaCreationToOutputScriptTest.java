/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.schemaupdate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.RequiresDialect;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

@TestForIssue(jiraKey = "HHH-11817")
@RequiresDialect(H2Dialect.class)
@BaseUnitTest
public class SchemaCreationToOutputScriptTest {

	private final String createTableMyEntity = "create table MyEntity";
	private final String createTableMySecondEntity = "create table MySecondEntity";

	private File output;
	private ServiceRegistry serviceRegistry;
	private MetadataImplementor metadata;

	@BeforeEach
	public void setUp() throws Exception {
		output = File.createTempFile( "creation_script", ".sql" );
		output.deleteOnExit();

		List<String> content = Arrays.asList(
				"This is the database creation script generated by Hibernate"
				, "This is the second line"
				, "This is the third line"
				, "This is the fourth line"
				, "This is the fifth line"
				, "This is the sixth line"
				, "This is the seventh line"
				, "This is the eighth line"
				, "This is the ninth line"
		);

		try (BufferedWriter bw = new BufferedWriter( new FileWriter( output ) )) {
			for ( String s : content ) {
				bw.write( s );
				bw.write( System.lineSeparator() );
			}
		}
	}

	private void createServiceRegistryAndMetadata(String append) {
		final StandardServiceRegistryBuilder standardServiceRegistryBuilder = new StandardServiceRegistryBuilder()
				.applySetting( Environment.FORMAT_SQL, "false" )
				.applySetting( Environment.JAKARTA_HBM2DDL_SCRIPTS_ACTION, "create" )
				.applySetting( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, output.getAbsolutePath() );
		if ( append != null ) {
			standardServiceRegistryBuilder.applySetting( AvailableSettings.HBM2DDL_SCRIPTS_CREATE_APPEND, append );
		}

		serviceRegistry = standardServiceRegistryBuilder.build();

		metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
				.addAnnotatedClass( MyEntity.class )
				.addAnnotatedClass( MySecondEntity.class )
				.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
	}

	@AfterEach
	public void tearDown() {
		ServiceRegistryBuilder.destroy( serviceRegistry );
	}

	@Test
	public void testAppendModeFalse() throws Exception {
		createServiceRegistryAndMetadata( "false" );
		HashMap properties = new HashMap<>();
		properties.putAll( serviceRegistry.getService( ConfigurationService.class ).getSettings() );

		SchemaManagementToolCoordinator.process(
				metadata,
				serviceRegistry,
				properties,
				null
		);
		List<String> commands = Files.readAllLines( output.toPath() );
		assertThat( commands.size(), is( 2 ) );
		assertThat( commands.get( 0 ), containsString( createTableMyEntity ) );
		assertThat( commands.get( 1 ), containsString( createTableMySecondEntity ) );
	}

	@Test
	public void testAppendModeTrue() throws Exception {
		createServiceRegistryAndMetadata( "true" );
		HashMap properties = new HashMap<>();
		properties.putAll( serviceRegistry.getService( ConfigurationService.class ).getSettings() );

		SchemaManagementToolCoordinator.process(
				metadata,
				serviceRegistry,
				properties,
				null
		);
		List<String> commands = Files.readAllLines( output.toPath() );
		assertThat( commands.size(), is( 11 ) );
		assertThat( commands.get( 9 ), containsString( createTableMyEntity ) );
		assertThat( commands.get( 10 ), containsString( createTableMySecondEntity ) );
	}

	@Test
	public void testDefaultAppendMode() throws Exception {
		createServiceRegistryAndMetadata( null );
		HashMap properties = new HashMap<>();
		properties.putAll( serviceRegistry.getService( ConfigurationService.class ).getSettings() );

		SchemaManagementToolCoordinator.process(
				metadata,
				serviceRegistry,
				properties,
				null
		);
		List<String> commands = Files.readAllLines( output.toPath() );
		assertThat( commands.size(), is( 11 ) );
		assertThat( commands.get( 9 ), containsString( createTableMyEntity ) );
		assertThat( commands.get( 10 ), containsString( createTableMySecondEntity ) );
	}

	@Entity(name = "MyEntity")
	public static class MyEntity {
		@Id
		private Long id;

		private String name;
	}

	@Entity(name = "MySecondEntity")
	public static class MySecondEntity {
		@Id
		private Long id;

		private String name;
	}

}
