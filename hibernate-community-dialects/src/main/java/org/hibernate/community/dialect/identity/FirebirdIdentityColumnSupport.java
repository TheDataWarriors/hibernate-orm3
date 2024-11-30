/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.identity;

import org.hibernate.dialect.identity.IdentityColumnSupportImpl;

/**
 * @author Mark Rotteveel
 */
public class FirebirdIdentityColumnSupport extends IdentityColumnSupportImpl {

	public static final FirebirdIdentityColumnSupport INSTANCE = new FirebirdIdentityColumnSupport();

	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public boolean hasDataTypeInIdentityColumn() {
		return true;
	}

	@Override
	public String getIdentityColumnString(int type) {
		return "generated by default as identity";
	}
}
