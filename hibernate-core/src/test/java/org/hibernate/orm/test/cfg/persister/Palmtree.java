/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cfg.persister;

import jakarta.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Palmtree extends Tree {
	private double leaveSize;

	public double getLeaveSize() {
		return leaveSize;
	}

	public void setLeaveSize(double leaveSize) {
		this.leaveSize = leaveSize;
	}
}
