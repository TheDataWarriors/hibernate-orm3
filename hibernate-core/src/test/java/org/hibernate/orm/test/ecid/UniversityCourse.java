/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ecid;


/**
 * @author Gavin King
 */
public class UniversityCourse extends Course {

	private int semester;

	UniversityCourse() {}

	public UniversityCourse(String courseCode, String org, String description, int semester) {
		super( courseCode, org, description );
		this.semester = semester;
	}

	public int getSemester() {
		return semester;
	}

}
