/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;

/**
 * @author Christian Beikov
 */
public class ImmutableConvertedBasicTypeImpl<J> extends ConvertedBasicTypeImpl<J> {

	public ImmutableConvertedBasicTypeImpl(
			JavaTypeDescriptor<J> jtd,
			JdbcTypeDescriptor std,
			String name,
			BasicValueConverter<J, ?> converter) {
		super( jtd, std, name, converter );
	}

	@Override
	protected MutabilityPlan<J> getMutabilityPlan() {
		//noinspection unchecked
		return ImmutableMutabilityPlan.INSTANCE;
	}
}
