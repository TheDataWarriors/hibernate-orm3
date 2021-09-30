package org.hibernate.orm.test.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@TestForIssue( jiraKey = "HHH-9029")
@Jpa(annotatedClasses = {
        EntityWithCompositeId.class,
        CompositeId.class
})
public class CompositeIdRowValueTest {

    @Test
    public void testTupleAfterSubQuery(EntityManagerFactoryScope scope) {
        scope.inTransaction(
                entityManager -> {
                    Query q = entityManager.createQuery("SELECT e FROM EntityWithCompositeId e "
                                                     + "WHERE EXISTS (SELECT 1 FROM EntityWithCompositeId) "
                                                     + "AND e.id = :id");

                    q.setParameter("id", new CompositeId(1, 2));

                    assertThat(q.getResultList().size(), is(0));
                }
        );
    }
}
