/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.indexcoll;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Drawer {
	private Long id;
	private List<Dress> dresses;

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Unidirectional one to many list
	 *
	 * @return
	 */
	@OneToMany
	@OrderColumn //default name test
	public List<Dress> getDresses() {
		return dresses;
	}

	public void setDresses(List<Dress> dresses) {
		this.dresses = dresses;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Drawer ) ) return false;

		final Drawer drawer = (Drawer) o;

		if ( !getId().equals( drawer.getId() ) ) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}
}
