/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.processor.Context;

import javax.lang.model.element.TypeElement;

public class NonManagedMetamodel extends AnnotationMetaEntity {

	public NonManagedMetamodel(TypeElement element, Context context, @Nullable AnnotationMeta parent) {
		super( element, context, false, false, parent );
	}

	public static NonManagedMetamodel create(
			TypeElement element, Context context,
			@Nullable AnnotationMetaEntity parent) {
		final NonManagedMetamodel metamodel =
				new NonManagedMetamodel( element, context, parent );
		if ( parent != null ) {
			metamodel.setParentElement( parent.getElement() );
			parent.addInnerClass( metamodel );
		}
		return metamodel;
	}

	protected void init() {
		// Initialization is not needed when non-managed class
	}

	@Override
	public String javadoc() {
		return "";
	}
}
