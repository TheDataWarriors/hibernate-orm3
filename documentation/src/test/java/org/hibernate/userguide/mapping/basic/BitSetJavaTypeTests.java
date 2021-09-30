/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.userguide.mapping.basic;

import java.util.BitSet;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JavaType;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Tests for using an explicit {@link JavaType}
 *
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = BitSetJavaTypeTests.Product.class )
@SessionFactory
public class BitSetJavaTypeTests {

	@Test
	public void testResolution(SessionFactoryScope scope) {
		final EntityPersister productType = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.findEntityDescriptor( Product.class );
		final SingularAttributeMapping bitSetAttribute = (SingularAttributeMapping) productType.findAttributeMapping( "bitSet" );
		// make sure BitSetTypeDescriptor was selected
		assertThat( bitSetAttribute.getJavaTypeDescriptor(), instanceOf( BitSetJavaType.class ) );
	}


	@Table(name = "Product")
	//tag::basic-bitset-example-java-type-local[]
	@Entity(name = "Product")
	public static class Product {
		@Id
		private Integer id;

		@JavaType( BitSetJavaType.class )
		private BitSet bitSet;

		//Constructors, getters, and setters are omitted for brevity
		//end::basic-bitset-example-java-type-local[]
		public Product() {
		}

		public Product(Number id, BitSet bitSet) {
			this.id = id.intValue();
			this.bitSet = bitSet;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public BitSet getBitSet() {
			return bitSet;
		}

		public void setBitSet(BitSet bitSet) {
			this.bitSet = bitSet;
		}
		//tag::basic-bitset-example-java-type-local[]
	}
	//end::basic-bitset-example-java-type-local[]
}
