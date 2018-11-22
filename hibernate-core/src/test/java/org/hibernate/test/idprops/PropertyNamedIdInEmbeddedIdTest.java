/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idprops;

import java.io.Serializable;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import org.hibernate.Session;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Gail Badner
 */
public class PropertyNamedIdInEmbeddedIdTest extends BaseCoreFunctionalTestCase {
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

		assertEquals(
				1, s.createQuery( "from Person p where p.id = :id", Person.class )
						.setParameter( "id", new PersonId( "John Doe", 0 ) )
						.list()
						.size()
		);

		assertEquals(
				2, s.createQuery( "from Person p where p.id.id = :id", Person.class )
						.setParameter( "id", 0 )
						.list()
						.size()
		);

		assertEquals( 3L, s.createQuery( "select count( p ) from Person p" ).uniqueResult() );

		s.createQuery( "delete from Person" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Entity(name = "Person")
	public static class Person implements Serializable {
		@EmbeddedId
		private PersonId personId;

		public Person(String name, int id) {
			this();
			personId = new PersonId( name, id );
		}

		protected Person() {
			// this form used by Hibernate
		}

		public PersonId getPersonId() {
			return personId;
		}
	}

	@Embeddable
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
