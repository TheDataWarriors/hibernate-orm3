/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schema.scripts;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.tool.hbm2ddl.MultipleLinesSqlCommandExtractor;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-2403")
@RequiresDialect(value = H2Dialect.class,
		jiraKey = "HHH-6286",
		comment = "Only running the tests against H2, because the sql statements in the import file are not generic. " +
				"This test should actually not test directly against the db")
public class MultiLineImportFileTest extends BaseCoreFunctionalTestCase {
	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty(
				Environment.HBM2DDL_IMPORT_FILES,
				"/org/hibernate/orm/test/tool/schema/scripts/multi-line-statements.sql"
		);
		cfg.setProperty(
				Environment.HBM2DDL_IMPORT_FILES_SQL_EXTRACTOR,
				MultipleLinesSqlCommandExtractor.class.getName()
		);
	}


	@Override
	protected String getBaseForMappings() {
		return "";
	}

	@Override
	public String[] getMappings() {
		return new String[] {
				"/org/hibernate/orm/test/tool/schema/scripts/Human.hbm.xml"
		};
	}

	@Test
	public void testImportFile() throws Exception {
		inTransaction(
				session -> {
					final Long count = session.createQuery( "select count(h.id) from Human h", Long.class ).uniqueResult();
					assertEquals( "Incorrect row count", 3L, count.longValue() );
				}
		);
	}

	@AfterClassOnce
	public void tearDown() {
		inTransaction(
				session -> session.createQuery( "delete Human" ).executeUpdate()
		);
	}
}
