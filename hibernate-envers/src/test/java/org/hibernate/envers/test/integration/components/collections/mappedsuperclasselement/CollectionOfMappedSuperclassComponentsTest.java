/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.test.integration.components.collections.mappedsuperclasselement;

import java.util.Arrays;
import java.util.Set;
import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-8908" )
public class CollectionOfMappedSuperclassComponentsTest extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {MappedSuperclassComponentSetTestEntity.class, Code.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		MappedSuperclassComponentSetTestEntity cte1 = new MappedSuperclassComponentSetTestEntity();

		em.persist( cte1 );

		em.getTransaction().commit();

		// Revision 2
		em = getEntityManager();
		em.getTransaction().begin();

		cte1 = em.find( MappedSuperclassComponentSetTestEntity.class, cte1.getId() );

		cte1.getComps().add( new Code( 1 ) );
		cte1.getCompsNotAudited().add( new Code( 100 ) );

		em.getTransaction().commit();

		id1 = cte1.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assertEquals(
				Arrays.asList( 1, 2 ),
				getAuditReader().getRevisions( MappedSuperclassComponentSetTestEntity.class, id1 )
		);
	}

	@Test
	public void testHistoryOfId1() {
		MappedSuperclassComponentSetTestEntity entity = getAuditReader().find(
				MappedSuperclassComponentSetTestEntity.class,
				id1,
				1
		);
		assertEquals( 0, entity.getComps().size() );
		assertEquals( 0, entity.getCompsNotAudited().size() );

		entity = getAuditReader().find( MappedSuperclassComponentSetTestEntity.class, id1, 2 );
		Set<Code> comps1 = entity.getComps();
		assertEquals( 1, comps1.size() );
		assertTrue( comps1.contains( new Code( 1 ) ) );
		// The contents of entity.getCompsNotAudited() is unspecified, so no need to test.
	}
}