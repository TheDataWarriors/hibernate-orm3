/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.metamodel.CollectionClassification;

/**
 * Descriptor for the collection identifier.  Only used with {@link CollectionClassification#ID_BAG} collections
 *
 * @author Steve Ebersole
 */
public interface CollectionIdentifierDescriptor extends CollectionPart, BasicValuedModelPart {
}
