/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria.query;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.criteria.CriteriaDelete;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

/**
 * @author Jan Schatteman
 */
@Jpa(annotatedClasses = {
		NonSelectCriteriaTest.Person.class
})
public class NonSelectCriteriaTest {

	@Test
	public void testNonSelectCriteriaCreation(EntityManagerFactoryScope scope) {
		// Tests that a non-select criteria can be created without getting IllegalArgumentExceptions of the type
		// "Non-select queries cannot be typed"
		scope.inTransaction(
				entityManager -> {
					CriteriaDelete<Person> deleteCriteria = entityManager.getCriteriaBuilder().createCriteriaDelete( Person.class );
					deleteCriteria.from( Person.class );
					entityManager.createQuery( deleteCriteria ).executeUpdate();
				}
		);
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		@GeneratedValue
		private Long id;
	}
}
