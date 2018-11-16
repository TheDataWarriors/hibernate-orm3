/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idprops;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Session;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Gail Badner
 */
public class PropertyNamedIdInNonJpaCompositeIdTest extends BaseCoreFunctionalTestCase {
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-13084")
	public void testHql() {
		Session s = openSession();
		s.beginTransaction();
		s.persist( new Person( "John Doe", 0 ) );
		s.persist( new Person( "John Doe", 1 ) );
		s.persist( new Person( "Jane Doe", 0 ) );
		s.flush();

		assertEquals( 2, s.createQuery( "from Person p where p.id = 0" ).list().size() );

		s.createQuery( "delete from Person" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Entity(name = "Person")
	public static class Person implements Serializable {
		@Id
		private String name;

		@Id
		private Integer id;

		public Person(String name, Integer id) {
			this();
			setName( name );
			setId( id );
		}
		public String getName() {
			return name;
		}

		public Integer getId() {
			return id;
		}

		protected Person() {
			// this form used by Hibernate
		}

		protected void setName(String name) {
			this.name = name;
		}

		protected void setId(Integer id) {
			this.id = id;
		}
	}

	public static class PersonId implements Serializable {
		private String name;
		private Integer id;

		public PersonId() {
		}

		public PersonId(String name, int id) {
			setName( name );
			setId( id );
		}

		public String getName() {
			return name;
		}

		public Integer getId() {
			return id;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			PersonId personId = (PersonId) o;

			if ( id != personId.id ) {
				return false;
			}
			return name.equals( personId.name );
		}

		@Override
		public int hashCode() {
			int result = name.hashCode();
			result = 31 * result + id;
			return result;
		}
	}
}
