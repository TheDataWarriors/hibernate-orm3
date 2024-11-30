/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Defines an SPI for integration with CDI-like containers.
 * <p>
 * Because CDI may or may not be available, much of this support
 * is directed toward abstracting/encapsulating CDI.
 * <p>
 * The central contracts here from a client point of view are
 * {@link org.hibernate.resource.beans.spi.ManagedBean} and
 * {@link org.hibernate.resource.beans.spi.ManagedBeanRegistry},
 * which may or may not really be backed by CDI.
 */
package org.hibernate.resource.beans.spi;
