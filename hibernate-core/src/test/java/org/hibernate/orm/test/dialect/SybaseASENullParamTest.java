/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NoResultException;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author Jan Schatteman
 */
@DomainModel(annotatedClasses = { SybaseASENullParamTest.Book.class })
@SessionFactory
@Jira( value = "https://hibernate.atlassian.net/browse/HHH-16216" )
public class SybaseASENullParamTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Book(1L, "LoremIpsum") );
			session.persist( new Book(2L, null) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createMutationQuery( "delete from Book" ).executeUpdate()
		);
	}

	@Test
	@RequiresDialect(value = SybaseASEDialect.class)
	public void testSybaseNullWithAnsiNullOn(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Book b = session.createQuery( "SELECT b FROM Book b WHERE b.title is distinct from null", Book.class ).getSingleResult();
					Assertions.assertEquals( 1L, b.id);

					b = session.createQuery( "SELECT b FROM Book b WHERE b.title is not distinct from null", Book.class ).getSingleResult();
					Assertions.assertEquals( 2L, b.id);

					// Neither of these should return anything given the fact that with ansinull on, these comparisons evaluate to 'unknown'
					Assertions.assertThrows( NoResultException.class, () -> session.createQuery( "SELECT b FROM Book b WHERE b.title = null", Book.class ).getSingleResult());
					Assertions.assertThrows( NoResultException.class, () -> session.createQuery( "SELECT b FROM Book b WHERE b.title != null", Book.class ).getSingleResult());

					List<Book> books = session.createQuery( "SELECT b FROM Book b WHERE 1 = 1", Book.class ).list();
					Assertions.assertEquals( 2, books.size());
				}
		);
	}

	@Test
	@RequiresDialect(value = SybaseASEDialect.class)
	@DomainModel(annotatedClasses = { SybaseASENullParamTest.Book.class })
	@SessionFactory
	@ServiceRegistry( settings = {@Setting(name = DriverManagerConnectionProviderImpl.INIT_SQL, value = "set ansinull off")} )
	public void testSybaseNullWithAnsiNullOff(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Book b = session.createQuery( "SELECT b FROM Book b WHERE b.title is distinct from null", Book.class ).getSingleResult();
					Assertions.assertEquals( 1L, b.id);

//fails because of the added and b1_0.title is not null in the where clause --> bug?
//					b = session.createQuery( "SELECT b FROM Book b WHERE b.title is not distinct from null", Book.class ).getSingleResult();
//					Assertions.assertEquals( 2L, b.id);

//fails because of the added and b1_0.title is not null in the where clause --> bug?
//					b = session.createQuery( "SELECT b FROM Book b WHERE b.title = null", Book.class ).getSingleResult();
//					Assertions.assertEquals( 2L, b.id);

					b = session.createQuery( "SELECT b FROM Book b WHERE b.title != null", Book.class ).getSingleResult();
					Assertions.assertEquals( 1L, b.id);

// Doesn't fail but has 2 unnecessary 'and 1 is not null' conditions in the where clause (doesn't happen when ansinull is on)
					List<Book> books = session.createQuery( "SELECT b FROM Book b WHERE 1 = 1", Book.class ).list();
					Assertions.assertEquals( 2, books.size());
				}
		);
	}

	@Entity(name = "Book")
	static class Book {
		@Id
		Long id;
		String title;

		public Book() {
		}

		public Book(Long id, String title) {
			this.id = id;
			this.title = title;
		}
	}

}
