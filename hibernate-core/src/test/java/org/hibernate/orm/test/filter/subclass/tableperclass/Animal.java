/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter.subclass.tableperclass;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@Entity
@Inheritance(strategy=InheritanceType.TABLE_PER_CLASS)
@Table(name="ZOOLOGY_ANIMAL")
@FilterDef(name="ignoreSome", parameters={@ParamDef(name="name", type=String.class)})
@Filter(name="ignoreSome", condition=":name <> ANIMAL_NAME")
public class Animal {
	@Id
	@GeneratedValue
	@Column(name="ANIMAL_ID")
	private Integer id;

	@Column(name="ANIMAL_NAME")
	private String name;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
