/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.ops;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

/**
 * @author Emmanuel Bernard
 * @author Yanming Zhou
 */
@Jpa(annotatedClasses = {
		Workload.class
})
public class MergeNewTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from Workload" ).executeUpdate();
				}
		);
	}

	@Test
	public void testMergeNew(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						Workload load = new Workload();
						load.name = "Cleaning";
						load.load = 10;
						entityManager.getTransaction().begin();
						load = entityManager.merge( load );
						assertNotNull( load.id );
						entityManager.flush();
						assertNotNull( load.id );
					}
					finally {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
					}
				}
		);
	}

	@Test
	public void testMergeAfterRemove(EntityManagerFactoryScope scope) {

		Integer load_id = scope.fromTransaction(
				entityManager -> {
					Workload _load = new Workload();
					_load.name = "Cleaning";
					_load.load = 10;
					_load = entityManager.merge( _load );
					entityManager.flush();
					return _load.getId();
				}
		);

		Workload load =scope.fromTransaction(
				entityManager -> {
					Workload _load = entityManager.find( Workload.class, load_id );
					entityManager.remove( _load );
					entityManager.flush();
					return _load;
				}
		);

		scope.inTransaction(
				entityManager -> {
					assertThrowsExactly( IllegalArgumentException.class, () -> entityManager.merge( load ) );
				}
		);
	}
}
