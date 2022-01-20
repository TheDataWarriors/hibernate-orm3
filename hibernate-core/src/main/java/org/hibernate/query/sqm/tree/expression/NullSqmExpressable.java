/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class NullSqmExpressable implements SqmExpressable<Object> {
	/**
	 * Singleton access
	 */
	public static final NullSqmExpressable NULL_SQM_EXPRESSABLE = new NullSqmExpressable();

	@Override
	public Class<Object> getBindableJavaType() {
		return null;
	}

	@Override
	public JavaType<Object> getExpressableJavaType() {
		return null;
	}
}
