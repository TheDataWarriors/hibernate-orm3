/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.typedonetoone;

import java.util.List;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/typedonetoone/Customer.hbm.xml"
)
@SessionFactory
public class TypedOneToOneTest {

	@Test
	public void testCreateQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Customer cust = new Customer();
					cust.setCustomerId( "abc123" );
					cust.setName( "Matt" );

					Address ship = new Address();
					ship.setStreet( "peachtree rd" );
					ship.setState( "GA" );
					ship.setCity( "ATL" );
					ship.setZip( "30326" );
					ship.setAddressId( new AddressId( "SHIPPING", "abc123" ) );
					ship.setCustomer( cust );

					Address bill = new Address();
					bill.setStreet( "peachtree rd" );
					bill.setState( "GA" );
					bill.setCity( "ATL" );
					bill.setZip( "30326" );
					bill.setAddressId( new AddressId( "BILLING", "abc123" ) );
					bill.setCustomer( cust );

					cust.setBillingAddress( bill );
					cust.setShippingAddress( ship );

					session.persist( cust );
				}
		);

		scope.inTransaction(
				session -> {
					List<Customer> results = session.createQuery(
									"from Customer cust left join fetch cust.billingAddress where cust.customerId='abc123'" )
							.list();
					//List results = s.createQuery("from Customer cust left join fetch cust.billingAddress left join fetch cust.shippingAddress").list();
					Customer cust = results.get( 0 );
					assertTrue( Hibernate.isInitialized( cust.getShippingAddress() ) );
					assertTrue( Hibernate.isInitialized( cust.getBillingAddress() ) );
					assertEquals( "30326", cust.getBillingAddress().getZip() );
					assertEquals( "30326", cust.getShippingAddress().getZip() );
					assertEquals( "BILLING", cust.getBillingAddress().getAddressId().getType() );
					assertEquals( "SHIPPING", cust.getShippingAddress().getAddressId().getType() );
					session.delete( cust );
				}
		);
	}

	@Test
	public void testCreateQueryNull(SessionFactoryScope scope) {


		scope.inTransaction(
				session -> {
					Customer cust = new Customer();
					cust.setCustomerId( "xyz123" );
					cust.setName( "Matt" );

					session.persist( cust );
				}
		);

		scope.inTransaction(
				session -> {
					List<Customer> results = session.createQuery(
									"from Customer cust left join fetch cust.billingAddress where cust.customerId='xyz123'" )
							.list();
					//List results = s.createQuery("from Customer cust left join fetch cust.billingAddress left join fetch cust.shippingAddress").list();
					Customer cust = results.get( 0 );
					assertNull( cust.getShippingAddress() );
					assertNull( cust.getBillingAddress() );
					session.delete( cust );
				}
		);
	}

}

