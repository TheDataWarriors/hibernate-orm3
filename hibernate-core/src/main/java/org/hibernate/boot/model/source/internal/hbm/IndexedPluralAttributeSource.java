/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.model.source.spi.PluralAttributeIndexSource;
import org.hibernate.boot.model.source.spi.PluralAttributeSource;

/**
 * Describes a plural attribute that is indexed.  This can mean either a list
 * or a map.
 */
public interface IndexedPluralAttributeSource extends PluralAttributeSource {
	PluralAttributeIndexSource getIndexSource();
}
