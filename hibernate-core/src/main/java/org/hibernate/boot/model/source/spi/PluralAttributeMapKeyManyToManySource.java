/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * Additional source information for {@code <map-key-many-to-many/>} and
 * {@code <index-many-to-many/>}.
 *
 * @author Steve Ebersole
 */
public interface PluralAttributeMapKeyManyToManySource
		extends PluralAttributeMapKeySource, RelationalValueSourceContainer {
	String getReferencedEntityName();

	String getExplicitForeignKeyName();
}
