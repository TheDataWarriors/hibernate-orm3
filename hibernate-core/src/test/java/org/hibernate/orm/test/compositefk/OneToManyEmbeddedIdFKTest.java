/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.compositefk;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				OneToManyEmbeddedIdFKTest.System.class,
				OneToManyEmbeddedIdFKTest.SystemUser.class
		}
)
@SessionFactory
public class OneToManyEmbeddedIdFKTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {

					PK superUserKey = new PK( 1, "Fab" );
					SystemUser superUser = new SystemUser( superUserKey, "Fab" );

					PK userKey = new PK( 2, "Andrea" );
					SystemUser user = new SystemUser( userKey, "Andrea" );

					System system = new System( 1, "sub1" );
					system.addUser( superUser );
					system.addUser( user );

					session.save( superUser );
					session.save( user );
					session.save( system );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from System" ).executeUpdate();
					session.createQuery( "delete from SystemUser" ).executeUpdate();
				}
		);
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					System system = session.get( System.class, 1 );
					assertThat( system, is( notNullValue() ) );
				}
		);
	}

	@Test
	public void testHqlQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					System system = (System) session.createQuery( "from System e where e.id = :id" )
							.setParameter( "id", 1 ).uniqueResult();
					assertThat( system, is( notNullValue() ) );
					assertThat( system.getUsers().size(), is( 2 ) );
				}
		);
	}

	@Test
	public void testHqlJoin(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					System system = session.createQuery( "from System e join e.users where e.id = :id", System.class )
							.setParameter( "id", 1 ).uniqueResult();
					assertThat( system, is( notNullValue() ) );
					assertThat( system.getUsers().size(), is( 2 ) );
				}
		);
	}

	@Test
	public void testHqlJoinFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					System system = session.createQuery(
							"from System e join fetch e.users where e.id = :id",
							System.class
					)
							.setParameter( "id", 1 ).uniqueResult();
					assertThat( system, is( notNullValue() ) );
					assertThat( system.getUsers().size(), is( 2 ) );
				}
		);
	}

	@Test
	public void testEmbeddedIdParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					PK superUserKey = new PK( 1, "Fab" );

					System system = session.createQuery(
							"from System e join fetch e.users u where u.id = :id",
							System.class
					).setParameter( "id", superUserKey ).uniqueResult();

					assertThat( system, is( notNullValue() ) );
					assertThat( system.getUsers().size(), is( 1 ) );
				}
		);
	}

	@Entity(name = "System")
	@Table( name = "systems" )
	public static class System {
		@Id
		private Integer id;
		private String name;

		@OneToMany
		List<SystemUser> users = new ArrayList<>();

		public System() {
		}

		public System(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<SystemUser> getUsers() {
			return users;
		}

		public void setUsers(List<SystemUser> users) {
			this.users = users;
		}

		public void addUser(SystemUser user) {
			this.users.add( user );
		}
	}

	@Entity(name = "SystemUser")
	public static class SystemUser {

		@EmbeddedId
		private PK pk;

		private String name;

		public SystemUser() {
		}

		public SystemUser(PK pk, String name) {
			this.pk = pk;
			this.name = name;
		}

		public PK getPk() {
			return pk;
		}

		public void setPk(PK pk) {
			this.pk = pk;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Embeddable
	public static class PK implements Serializable {

		private Integer subsystem;

		private String username;

		public PK(Integer subsystem, String username) {
			this.subsystem = subsystem;
			this.username = username;
		}

		private PK() {
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			PK pk = (PK) o;
			return Objects.equals( subsystem, pk.subsystem ) &&
					Objects.equals( username, pk.username );
		}

		@Override
		public int hashCode() {
			return Objects.hash( subsystem, username );
		}
	}
}
