/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.keymanytoone.bidir.component;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class Order {
	private Id id;
	private Set items = new HashSet();

	public Order() {
	}

	public Order(Id id) {
		this.id = id;
	}

	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}

	public Set getItems() {
		return items;
	}

	public void setItems(Set items) {
		this.items = items;
	}

	public static class Id implements Serializable {
		private Customer customer;
		private long number;

		public Id() {
		}

		public Id(Customer customer, long number) {
			this.customer = customer;
			this.number = number;
		}

		public Customer getCustomer() {
			return customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}

		public long getNumber() {
			return number;
		}

		public void setNumber(long number) {
			this.number = number;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			Id id = ( Id ) o;
			return number == id.number && customer.equals( id.customer );
		}

		@Override
		public int hashCode() {
			int result;
			result = customer.hashCode();
			result = 31 * result + ( int ) ( number ^ ( number >>> 32 ) );
			return result;
		}
	}
}
