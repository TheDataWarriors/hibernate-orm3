/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.legacy;
import java.io.Serializable;

/**
 * @author Stefano Travelli
 */
public class OuterKey implements Serializable {
	private Middle master;
	private String detailId;

	public Middle getMaster() {
		return master;
	}

	public void setMaster(Middle master) {
		this.master = master;
	}

	public String getDetailId() {
		return detailId;
	}

	public void setDetailId(String detailId) {
		this.detailId = detailId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof OuterKey)) return false;

		final OuterKey cidDetailID = (OuterKey) o;

		if (detailId != null ? !detailId.equals(cidDetailID.detailId) : cidDetailID.detailId != null) return false;
		if (master != null ? !master.equals(cidDetailID.master) : cidDetailID.master != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result;
		result = (master != null ? master.hashCode() : 0);
		result = 29 * result + (detailId != null ? detailId.hashCode() : 0);
		return result;
	}
}
