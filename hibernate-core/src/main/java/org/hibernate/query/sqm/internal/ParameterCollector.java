/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.query.sqm.tree.expression.SqmParameter;

/**
 * @author Steve Ebersole
 */
// todo (6.0) : how is this different from org.hibernate.query.sqm.tree.jpa.ParameterCollector?
public interface ParameterCollector {
	void addParameter(SqmParameter<?> parameter);
}
