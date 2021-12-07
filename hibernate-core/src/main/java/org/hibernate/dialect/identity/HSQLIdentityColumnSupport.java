/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.identity;

import org.hibernate.dialect.DatabaseVersion;

/**
 * @author Andrea Boriero
 */
public class HSQLIdentityColumnSupport extends IdentityColumnSupportImpl {
	final private DatabaseVersion dbVersion;

	public HSQLIdentityColumnSupport(DatabaseVersion dbVersion) {
		this.dbVersion = dbVersion;
	}

	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public String getIdentityColumnString(int type) {
		//not null is implicit
		return "generated by default as identity (start with 1)";
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type) {
		return "call identity()";
	}

	@Override
	public String getIdentityInsertString() {
		return dbVersion.isBefore( 2 ) ? "null" : "default";
	}
}
