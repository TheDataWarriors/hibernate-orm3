/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph;

import org.hibernate.engine.FetchTiming;

/**
 * Access to a FetchTiming
 *
 * @author Steve Ebersole
 */
public interface FetchTimingAccess {
	FetchTiming getTiming();
}
