/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bootstrap.registry.classloading;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import jakarta.persistence.EntityManagerFactory;

/**
 * A Runnable which initializes an EntityManagerFactory;
 * this is meant to test against classloader leaks, so needs
 * to be packaged as a Runnable rather than using our usual
 * testing facilities.
 */
public class HibernateLoadingTestAction extends NotLeakingTestAction implements Runnable {

	@Override
	public void run() {
		super.run(); //for basic sanity self-check
		final Map config = new HashMap();
		EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder(
				new BaseEntityManagerFunctionalTestCase.TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() ),
				config
		).build();
		try {
			emf.close();
		}
		finally {
			cleanupJDBCDrivers();
		}
	}

	private void cleanupJDBCDrivers() {
		DriverManager.drivers().forEach( this::deregister );
	}

	private void deregister(final Driver driver) {
		System.out.println( "Unregistering driver: " +driver);
		try {
			DriverManager.deregisterDriver( driver );
		}
		catch ( SQLException e ) {
			throw new RuntimeException( e );
		}
	}

}
