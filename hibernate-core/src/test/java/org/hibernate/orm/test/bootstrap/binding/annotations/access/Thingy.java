/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.orm.test.bootstrap.binding.annotations.access;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
public class Thingy {
	private String god;

	@Transient
	public String getGod() {
		return god;
	}

	public void setGod(String god) {
		this.god = god;
	}
}
