/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.spi;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.boot.models.DialectOverrideAnnotations;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

/**
 * Base support for {@linkplain DialectOverrider} annotations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractOverrider<O extends Annotation> implements DialectOverrider<O> {
	private Class<? extends Dialect> dialect;
	private DialectOverride.Version before;
	private DialectOverride.Version sameOrAfter;

	public AbstractOverrider() {
	}

	public AbstractOverrider(
			AnnotationInstance annotation,
			AnnotationDescriptor<?> descriptor,
			SourceModelBuildingContext modelContext) {
		dialect( extractJandexValue( annotation, descriptor, "dialect", modelContext ) );
		before( extractJandexValue( annotation, descriptor, "before", modelContext ) );
		sameOrAfter( extractJandexValue( annotation, descriptor, "sameOrAfter", modelContext ) );
	}

	@Override
	public Class<? extends Dialect> dialect() {
		return dialect;
	}

	public void dialect(Class<? extends Dialect> dialect) {
		this.dialect = dialect;
	}

	@Override
	public DialectOverride.Version before() {
		return before;
	}

	public void before(DialectOverride.Version before) {
		this.before = before;
	}

	@Override
	public DialectOverride.Version sameOrAfter() {
		return sameOrAfter;
	}

	public void sameOrAfter(DialectOverride.Version sameOrAfter) {
		this.sameOrAfter = sameOrAfter;
	}

	@Override
	public boolean matches(Dialect dialectToMatch) {
		if ( !dialect().isAssignableFrom( dialectToMatch.getClass() ) ) {
			return false;
		}

		final DatabaseVersion versionToMatch = dialectToMatch.getVersion();
		return versionToMatch.isBefore( before().major(), before().minor() )
				&& versionToMatch.isSameOrAfter( sameOrAfter().major(), sameOrAfter().minor() );
	}
}