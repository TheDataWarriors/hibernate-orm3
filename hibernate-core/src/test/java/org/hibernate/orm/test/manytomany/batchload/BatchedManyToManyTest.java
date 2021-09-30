/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.manytomany.batchload;

import java.util.List;
import java.util.Locale;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Hibernate;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.CollectionStatistics;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests loading of many-to-many collection which should trigger
 * a batch load.
 *
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/manytomany/batchload/UserGroupBatchLoad.hbm.xml"
)
@SessionFactory(generateStatistics = true)
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.USE_SECOND_LEVEL_CACHE, value = "false"),
				@Setting(name = Environment.BATCH_STRATEGY, value = "org.hibernate.test.manytomany.batchload.TestingBatchBuilder"),
		}
)
public class BatchedManyToManyTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		// set up the test data
		User me = new User( "steve" );
		User you = new User( "not steve" );
		Group developers = new Group( "developers" );
		Group translators = new Group( "translators" );
		Group contributors = new Group( "contributors" );
		me.getGroups().add( developers );
		developers.getUsers().add( me );
		you.getGroups().add( translators );
		translators.getUsers().add( you );
		you.getGroups().add( contributors );
		contributors.getUsers().add( you );

		scope.inTransaction(
				session -> {
					session.save( me );
					session.save( you );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		// clean up the test data
		scope.inTransaction(
				session -> {
					// User is the non-inverse side...
					List<User> users = session.createQuery( "from User" ).list();
					for ( User user : users ) {
						session.delete( user );
					}
					session.flush();
					session.createQuery( "delete Group" ).executeUpdate();
				}
		);
	}

	@Test
	public void testLoadingNonInverseSide(SessionFactoryScope scope) {

		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		sessionFactory.getStatistics().clear();
		CollectionStatistics userGroupStats = sessionFactory.getStatistics()
				.getCollectionStatistics( User.class.getName() + ".groups" );
		CollectionStatistics groupUserStats = sessionFactory.getStatistics()
				.getCollectionStatistics( Group.class.getName() + ".users" );

		Interceptor testingInterceptor = new EmptyInterceptor() {
			@Override
			public String onPrepareStatement(String sql) {
				// ugh, this is the best way I could come up with to assert this.
				// unfortunately, this is highly dependent on the dialect and its
				// outer join fragment.  But at least this wil fail on the majority
				// of dialects...
				assertFalse(
						sql.toLowerCase( Locale.ROOT ).contains( "left join" ),
						"batch load of many-to-many should use inner join"
				);
				return super.onPrepareStatement( sql );
			}
		};

		try (final Session session = scope.getSessionFactory()
				.withOptions()
				.interceptor( testingInterceptor )
				.openSession()) {
			session.getTransaction().begin();
			try {
				List users = session.createQuery( "from User u" ).list();
				User user = (User) users.get( 0 );
				assertTrue( Hibernate.isInitialized( user ) );
				assertTrue( Hibernate.isInitialized( user.getGroups() ) );
				user = (User) users.get( 1 );
				assertTrue( Hibernate.isInitialized( user ) );
				assertTrue( Hibernate.isInitialized( user.getGroups() ) );
				assertEquals(
						1,
						userGroupStats.getFetchCount()
				); // should have been just one fetch (the batch fetch)
				assertEquals(
						1,
						groupUserStats.getFetchCount()
				); // should have been just one fetch (the batch fetch)
				session.getTransaction().commit();
			}
			finally {
				if ( session.getTransaction().isActive() ) {
					session.getTransaction().rollback();
				}
			}
		}
	}

}
