/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.hhh18829;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = EmployeeWithoutIdClass.class)
@JiraKey(" HHH-18829")
@SessionFactory
public class HHH18829Test {

	@BeforeAll
	void setUp(SessionFactoryScope sessionFactoryScope) {
		sessionFactoryScope.inTransaction( sess -> {
			final var one = new EmployeeWithoutIdClass();
			one.empName = "John Doe";
			one.empId = 1;
			one.address = "10 Downing Street, SW1A 2AA";
			sess.persist( one );

			final var two = new EmployeeWithoutIdClass();
			two.empName = "Dave Default";
			two.empId = 1;
			two.address = "1600 Pennsylvania Avenue";
			sess.persist( two );
		} );
	}

	@Test
	public void test(SessionFactoryScope sessionFactoryScope)
			throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException {
		final var idClass = Class.forName( EmployeeWithoutIdClass.class.getName() + "_$Id" );
		final var id = idClass.getConstructors()[0].newInstance( "John Doe", 1 );
		final var employees = sessionFactoryScope.fromSession(
				sess -> sess.createQuery( "from EmployeeWithoutIdClass where id=:id", EmployeeWithoutIdClass.class ).setParameter( "id", id )
						.getResultList()
		);
		assertEquals( 1, employees.size() );
		assertEquals( "10 Downing Street, SW1A 2AA", employees.get( 0 ).address );
	}
}
