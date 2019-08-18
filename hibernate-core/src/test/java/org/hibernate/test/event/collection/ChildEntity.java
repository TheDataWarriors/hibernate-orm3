/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: $

package org.hibernate.test.event.collection;


/**
 *
 * @author Gail Badner
 */
public class ChildEntity extends ChildValue implements Entity {
	private Long id;

	public ChildEntity() {
		super();
	}

	public ChildEntity(String name) {
		super( name );
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

}
