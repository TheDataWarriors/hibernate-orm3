/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.boot.model.source.internal.hbm.IndexedPluralAttributeSource;

/**
 * @author Steve Ebersole
 */
public interface PluralAttributeSourceArray extends IndexedPluralAttributeSource {
	String getElementClass();
}
