/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.embedded.one2many;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import jakarta.persistence.Entity;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Entity
public class Alias {
	private Long id;
	private Name name;
	private String source;

	public Alias() {
	}

	public Alias(String firstName, String lastName,  String source) {
		this( new PersonName( firstName, lastName ), source );
	}

	public Alias(Name name, String source) {
		this.name = name;
		this.source = source;
	}

	@Id @GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Name getName() {
		return name;
	}

	public void setName(Name name) {
		this.name = name;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}
}
