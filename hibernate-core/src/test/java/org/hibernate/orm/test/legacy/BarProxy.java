/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;


public interface BarProxy extends AbstractProxy {
	void setBaz(Baz arg0);
	Baz getBaz();
	void setBarComponent(FooComponent arg0);
	FooComponent getBarComponent();
	//public void setBarString(String arg0);
	String getBarString();
	Object getObject();
	void setObject(Object o);
}
