/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.xml;

import org.hibernate.jpamodelgen.model.MetaEntity;

import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

/**
 * @author Hardy Ferentschik
 */
public class XmlMetaMap extends XmlMetaCollection {

	private final String keyType;

	public XmlMetaMap(XmlMetaEntity parent, String propertyName, String type, String collectionType, String keyType) {
		super( parent, propertyName, type, collectionType );
		this.keyType = keyType;
	}

	@Override
	public String getTypedAttributeDeclarationString(MetaEntity entityForImports, String mtype, List<? extends TypeMirror> toImport) {
		return "public static volatile " +
				entityForImports.importType(getMetaType()) +
				"<" +
				entityForImports.importType(getHostingEntity().getQualifiedName()) +
				getNonGenericTypesFromTypeMirror(entityForImports, toImport) +
				", " +
				getHostingEntity().importType( keyType ) +
				", " +
				entityForImports.importType(mtype) +
				"> " +
				getPropertyName() +
				";";
	}

	@Override
	public String getAttributeDeclarationString(List<? extends TypeParameterElement> toImport) {
		return "public static volatile " +
				getHostingEntity().importType(getMetaType()) +
				"<" +
				getHostingEntity().importType(getHostingEntity().getQualifiedName()) +
				getNonGenericTypesFromTypeParameter(getHostingEntity(), toImport) +
				", " +
				getHostingEntity().importType( keyType ) +
				", " +
				getHostingEntity().importType(getTypeDeclaration()) +
				"> " +
				getPropertyName() +
				";";
	}

	@Override
	public String getAttributeDeclarationString() {
		final MetaEntity hostingEntity = getHostingEntity();
		return new StringBuilder().append( "public static volatile " )
				.append( hostingEntity.importType( getMetaType() ) )
				.append( "<" )
				.append( hostingEntity.importType( hostingEntity.getQualifiedName() ) )
				.append( ", " )
				.append( hostingEntity.importType( keyType ) )
				.append( ", " )
				.append( hostingEntity.importType( getTypeDeclaration() ) )
				.append( "> " )
				.append( getPropertyName() )
				.append( ";" )
				.toString();
	}
}
