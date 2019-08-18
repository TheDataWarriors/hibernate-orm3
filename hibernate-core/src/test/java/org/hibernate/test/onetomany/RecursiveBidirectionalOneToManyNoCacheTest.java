/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.onetomany;

import org.hibernate.CacheMode;

/**
 * @author Burkhard Graves
 * @author Gail Badner
 */
public class RecursiveBidirectionalOneToManyNoCacheTest extends AbstractRecursiveBidirectionalOneToManyTest {
	@Override
	public String getCacheConcurrencyStrategy() {
			return null;
	}
	
	@Override
	protected CacheMode getSessionCacheMode() {
			return CacheMode.IGNORE;
	}
}
