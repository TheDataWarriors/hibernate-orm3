/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.hhh18829;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import java.util.Arrays;

import static org.hibernate.processor.test.util.TestUtil.getMetamodelClassFor;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestForIssue(jiraKey = " HHH-18829")
public class HHH18829Test extends CompilationTest {

	@Test
	@WithClasses({Employee.class, AnotherEmployee.class, Address.class, EmployeeWithIdClass.class})
	@TestForIssue(jiraKey = "HHH-18829")
	public void test() {
		System.out.println( TestUtil.getMetaModelSourceAsString( Employee.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( AnotherEmployee.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( Address.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( EmployeeWithIdClass.class ) );

		checkIfIdClassIsGenerated( Employee.class );
		checkIfIdClassIsGenerated( AnotherEmployee.class );

		final var clazz = getMetamodelClassFor( EmployeeWithIdClass.class );
		assertTrue( Arrays.stream( clazz.getClasses() ).map( Class::getSimpleName )
						.noneMatch( "Id"::equals ),
				"EmployeeWithIdClass_ should not have inner class Id" );
	}

	private static void checkIfIdClassIsGenerated(Class<?> entityClass) {
		final var clazz = getMetamodelClassFor( entityClass );
		final var maybeIdClass = Arrays.stream( clazz.getClasses() )
				.filter( c -> c.getSimpleName().equals( "Id" ) ).findAny();
		assertTrue( maybeIdClass.isPresent(), () -> clazz.getSimpleName() + "_ should have inner class Id" );
		final Class<?> idClass = maybeIdClass.get();
		assertTrue( idClass.isRecord(), "Generated ID class should be a record" );
		final var recordComponents = idClass.getRecordComponents();
		// TODO : Check record components
	}
}
