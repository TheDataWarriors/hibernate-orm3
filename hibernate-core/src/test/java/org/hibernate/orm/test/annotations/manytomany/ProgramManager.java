/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.manytomany;
import java.util.Collection;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class ProgramManager {
	int id;

	Collection<Employee> manages;

	@Id
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@OneToMany( mappedBy="jobInfo.pm", cascade= CascadeType.ALL )
	public Collection<Employee> getManages() {
		return manages;
	}

	public void setManages( Collection<Employee> manages ) {
		this.manages = manages;
	}

}
