/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.mappedsuperclass.mappedsuperclasswithoutid;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.jpamodelgen.test.accesstype.Shop;

/**
 * @author Hardy Ferentschik
 */
@MappedSuperclass
public class Product {
	@ManyToOne
	Shop shop;
}
