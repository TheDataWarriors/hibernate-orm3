/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.tree.SqmStatement;

/**
 * @author Steve Ebersole
 *
 * @param <R> the query result type
 */
public interface HqlInterpretation<R> {
	SqmStatement<R> getSqmStatement();

	ParameterMetadataImplementor getParameterMetadata();

	DomainParameterXref getDomainParameterXref();

	void validateResultType(Class<?> resultType);

}
