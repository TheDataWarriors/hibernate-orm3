/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc..
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.source.annotations.xml.mocker;

import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import org.hibernate.metamodel.source.annotation.xml.XMLAccessType;
import org.hibernate.metamodel.source.annotation.xml.XMLEnumType;
import org.hibernate.metamodel.source.annotation.xml.XMLMapKey;
import org.hibernate.metamodel.source.annotation.xml.XMLMapKeyClass;
import org.hibernate.metamodel.source.annotation.xml.XMLMapKeyColumn;
import org.hibernate.metamodel.source.annotation.xml.XMLMapKeyJoinColumn;
import org.hibernate.metamodel.source.annotation.xml.XMLTemporalType;

/**
 * @author Strong Liu
 */
abstract class PropertyMocker extends AnnotationMocker {
	protected ClassInfo classInfo;
	protected XMLAccessType accessType;

	private void setTarget(AnnotationTarget target) {
		this.target = target;
	}

	private AnnotationTarget target;

	PropertyMocker(IndexBuilder indexBuilder, ClassInfo classInfo, EntityMappingsMocker.Default defaults) {
		super( indexBuilder, defaults );
		this.classInfo = classInfo;
	}

	protected abstract void processExtra();

	@Override
	protected AnnotationTarget getTarget() {
		return target;
	}

	protected abstract String getFieldName();

	protected abstract XMLAccessType getAccessType();

	@Override
	protected DotName getTargetName() {
		return classInfo.name();
	}


	@Override
	final void process() {
		setTarget(
				MockHelper.getTarget(
						indexBuilder.getServiceRegistry(), classInfo, getFieldName(), MockHelper.TargetType.FIELD
				)
		);
		parserAccessType( getAccessType(), getTarget() );
		processExtra();
		setTarget(
				MockHelper.getTarget(
						indexBuilder.getServiceRegistry(), classInfo, getFieldName(), MockHelper.TargetType.PROPERTY
				)
		);
		processExtra();
		parserAccessType( getAccessType(), getTarget() );
	}

	protected AnnotationInstance parserMapKeyColumn(XMLMapKeyColumn mapKeyColumn, AnnotationTarget target) {
		if ( mapKeyColumn == null ) {
			return null;
		}
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", mapKeyColumn.getName(), annotationValueList );
		MockHelper.stringValue( "columnDefinition", mapKeyColumn.getColumnDefinition(), annotationValueList );
		MockHelper.stringValue( "table", mapKeyColumn.getTable(), annotationValueList );
		MockHelper.booleanValue( "nullable", mapKeyColumn.isNullable(), annotationValueList );
		MockHelper.booleanValue( "insertable", mapKeyColumn.isInsertable(), annotationValueList );
		MockHelper.booleanValue( "updatable", mapKeyColumn.isUpdatable(), annotationValueList );
		MockHelper.booleanValue( "unique", mapKeyColumn.isUnique(), annotationValueList );
		MockHelper.integerValue( "length", mapKeyColumn.getLength(), annotationValueList );
		MockHelper.integerValue( "precision", mapKeyColumn.getPrecision(), annotationValueList );
		MockHelper.integerValue( "scale", mapKeyColumn.getScale(), annotationValueList );
		return create( MAP_KEY_COLUMN, target, annotationValueList );
	}

	protected AnnotationInstance parserMapKeyClass(XMLMapKeyClass mapKeyClass, AnnotationTarget target) {
		if ( mapKeyClass == null ) {
			return null;
		}
		return create(
				MAP_KEY_CLASS, target, MockHelper.classValueArray(
				"value", mapKeyClass.getClazz(), indexBuilder.getServiceRegistry()
		)
		);
	}

	protected AnnotationInstance parserMapKeyTemporal(XMLTemporalType temporalType, AnnotationTarget target) {
		if ( temporalType == null ) {
			return null;
		}
		return create(
				MAP_KEY_TEMPORAL, target,
				MockHelper.enumValueArray( "value", TEMPORAL_TYPE, temporalType )
		);
	}

	protected AnnotationInstance parserMapKeyEnumerated(XMLEnumType enumType, AnnotationTarget target) {
		if ( enumType == null ) {
			return null;
		}
		return create(
				MAP_KEY_ENUMERATED, target,
				MockHelper.enumValueArray( "value", ENUM_TYPE, enumType )
		);
	}

	protected AnnotationInstance parserMapKey(XMLMapKey mapKey, AnnotationTarget target) {
		if ( mapKey == null ) {
			return null;
		}
		return create( MAP_KEY, target, MockHelper.stringValueArray( "name", mapKey.getName() ) );
	}

	private AnnotationValue[] nestedMapKeyJoinColumnList(String name, List<XMLMapKeyJoinColumn> columns, List<AnnotationValue> annotationValueList) {
		if ( MockHelper.isNotEmpty( columns ) ) {
			AnnotationValue[] values = new AnnotationValue[columns.size()];
			for ( int i = 0; i < columns.size(); i++ ) {
				AnnotationInstance annotationInstance = parserMapKeyJoinColumn( columns.get( i ), null );
				values[i] = MockHelper.nestedAnnotationValue(
						"", annotationInstance
				);
			}
			MockHelper.addToCollectionIfNotNull( annotationValueList, AnnotationValue.createArrayValue( name, values ) );
			return values;
		}
		return MockHelper.EMPTY_ANNOTATION_VALUE_ARRAY;
	}

	protected AnnotationInstance parserMapKeyJoinColumnList(List<XMLMapKeyJoinColumn> joinColumnList, AnnotationTarget target) {
		if ( MockHelper.isNotEmpty( joinColumnList ) ) {
			if ( joinColumnList.size() == 1 ) {
				return parserMapKeyJoinColumn( joinColumnList.get( 0 ), target );
			}
			else {
				AnnotationValue[] values = nestedMapKeyJoinColumnList( "value", joinColumnList, null );
				return create(
						MAP_KEY_JOIN_COLUMNS,
						target,
						values
				);
			}
		}
		return null;

	}

	//@MapKeyJoinColumn
	private AnnotationInstance parserMapKeyJoinColumn(XMLMapKeyJoinColumn column, AnnotationTarget target) {
		if ( column == null ) {
			return null;
		}
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", column.getName(), annotationValueList );
		MockHelper.stringValue( "columnDefinition", column.getColumnDefinition(), annotationValueList );
		MockHelper.stringValue( "table", column.getTable(), annotationValueList );
		MockHelper.stringValue(
				"referencedColumnName", column.getReferencedColumnName(), annotationValueList
		);
		MockHelper.booleanValue( "unique", column.isUnique(), annotationValueList );
		MockHelper.booleanValue( "nullable", column.isNullable(), annotationValueList );
		MockHelper.booleanValue( "insertable", column.isInsertable(), annotationValueList );
		MockHelper.booleanValue( "updatable", column.isUpdatable(), annotationValueList );
		return create( MAP_KEY_JOIN_COLUMN, target, annotationValueList );
	}


}
