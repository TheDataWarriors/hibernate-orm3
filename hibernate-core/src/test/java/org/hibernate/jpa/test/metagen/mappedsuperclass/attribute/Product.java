/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metagen.mappedsuperclass.attribute;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Steve Ebersole
 */
@Entity
public class Product extends AbstractNameable {
	private Long id;

	public Product() {
	}

	public Product(String name) {
		super( name );
	}

	@Id
	public Long getId() {
		return id;
	}

	private void setId(Long id) {
		this.id = id;
	}

	@Column(name = "overridenName")
	public String getOverridenName() {
		return super.getOverridenName();
	}

	public void setOverridenName(String overridenName) {
		super.setOverridenName(overridenName);
	}
}
