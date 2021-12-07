/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * @deprecated use {@code PostgreSQLDialect(810)}
 */
@Deprecated
public class PostgreSQL81Dialect extends PostgreSQLDialect {

	public PostgreSQL81Dialect() {
		super( DatabaseVersion.make( 8, 1 ) );
	}

}

