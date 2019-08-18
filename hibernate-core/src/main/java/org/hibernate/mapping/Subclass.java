/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.internal.util.collections.JoinedIterator;
import org.hibernate.internal.util.collections.SingletonIterator;

/**
 * A sublass in a table-per-class-hierarchy mapping
 * @author Gavin King
 */
public class Subclass extends PersistentClass {
	private PersistentClass superclass;
	private Class classPersisterClass;
	private final int subclassId;
	
	public Subclass(PersistentClass superclass, MetadataBuildingContext metadataBuildingContext) {
		super( metadataBuildingContext );
		this.superclass = superclass;
		this.subclassId = superclass.nextSubclassId();
	}

	@Override
	int nextSubclassId() {
		return getSuperclass().nextSubclassId();
	}
	
	@Override
	public int getSubclassId() {
		return subclassId;
	}
	
	@Override
	public String getNaturalIdCacheRegionName() {
		return getSuperclass().getNaturalIdCacheRegionName();
	}

	@Override
	public String getCacheConcurrencyStrategy() {
		return getRootClass().getCacheConcurrencyStrategy();
	}

	@Override
	public RootClass getRootClass() {
		return getSuperclass().getRootClass();
	}

	@Override
	public PersistentClass getSuperclass() {
		return superclass;
	}

	@Override
	public Property getIdentifierProperty() {
		return getSuperclass().getIdentifierProperty();
	}

	@Override
	public Property getDeclaredIdentifierProperty() {
		return null;
	}

	@Override
	public KeyValue getIdentifier() {
		return getSuperclass().getIdentifier();
	}
	@Override
	public boolean hasIdentifierProperty() {
		return getSuperclass().hasIdentifierProperty();
	}
	@Override
	public Value getDiscriminator() {
		return getSuperclass().getDiscriminator();
	}
	@Override
	public boolean isMutable() {
		return getSuperclass().isMutable();
	}
	@Override
	public boolean isInherited() {
		return true;
	}
	@Override
	public boolean isPolymorphic() {
		return true;
	}

	@Override
	public void addProperty(Property p) {
		super.addProperty(p);
		getSuperclass().addSubclassProperty(p);
	}

	@Override
	public void addMappedsuperclassProperty(Property p) {
		super.addMappedsuperclassProperty( p );
		getSuperclass().addSubclassProperty(p);
	}

	@Override
	public void addJoin(Join j) {
		super.addJoin(j);
		getSuperclass().addSubclassJoin(j);
	}

	@Override
	public Iterator getPropertyClosureIterator() {
		return new JoinedIterator(
				getSuperclass().getPropertyClosureIterator(),
				getPropertyIterator()
			);
	}
	@Override
	public Iterator getTableClosureIterator() {
		return new JoinedIterator(
				getSuperclass().getTableClosureIterator(),
				new SingletonIterator( getTable() )
			);
	}
	@Override
	public Iterator getKeyClosureIterator() {
		return new JoinedIterator(
				getSuperclass().getKeyClosureIterator(),
				new SingletonIterator( getKey() )
			);
	}
	@Override
	protected void addSubclassProperty(Property p) {
		super.addSubclassProperty(p);
		getSuperclass().addSubclassProperty(p);
	}
	@Override
	protected void addSubclassJoin(Join j) {
		super.addSubclassJoin(j);
		getSuperclass().addSubclassJoin(j);
	}

	@Override
	protected void addSubclassTable(Table table) {
		super.addSubclassTable(table);
		getSuperclass().addSubclassTable(table);
	}

	@Override
	public boolean isVersioned() {
		return getSuperclass().isVersioned();
	}
	@Override
	public Property getVersion() {
		return getSuperclass().getVersion();
	}

	@Override
	public Property getDeclaredVersion() {
		return null;
	}

	@Override
	public boolean hasEmbeddedIdentifier() {
		return getSuperclass().hasEmbeddedIdentifier();
	}
	@Override
	public Class getEntityPersisterClass() {
		if (classPersisterClass==null) {
			return getSuperclass().getEntityPersisterClass();
		}
		else {
			return classPersisterClass;
		}
	}

	@Override
	public Table getRootTable() {
		return getSuperclass().getRootTable();
	}

	@Override
	public KeyValue getKey() {
		return getSuperclass().getIdentifier();
	}

	@Override
	public boolean isExplicitPolymorphism() {
		return getSuperclass().isExplicitPolymorphism();
	}

	public void setSuperclass(PersistentClass superclass) {
		this.superclass = superclass;
	}

	@Override
	public String getWhere() {
		return getSuperclass().getWhere();
	}

	@Override
	public boolean isJoinedSubclass() {
		return getTable()!=getRootTable();
	}

	public void createForeignKey() {
		if ( !isJoinedSubclass() ) {
			throw new AssertionFailure( "not a joined-subclass" );
		}
		getKey().createForeignKeyOfEntity( getSuperclass().getEntityName() );
	}

	@Override
	public void setEntityPersisterClass(Class classPersisterClass) {
		this.classPersisterClass = classPersisterClass;
	}

	@Override
	public int getJoinClosureSpan() {
		return getSuperclass().getJoinClosureSpan() + super.getJoinClosureSpan();
	}

	@Override
	public int getPropertyClosureSpan() {
		return getSuperclass().getPropertyClosureSpan() + super.getPropertyClosureSpan();
	}

	@Override
	public Iterator getJoinClosureIterator() {
		return new JoinedIterator(
			getSuperclass().getJoinClosureIterator(),
			super.getJoinClosureIterator()
		);
	}

	@Override
	public boolean isClassOrSuperclassJoin(Join join) {
		return super.isClassOrSuperclassJoin(join) || getSuperclass().isClassOrSuperclassJoin(join);
	}

	@Override
	public boolean isClassOrSuperclassTable(Table table) {
		return super.isClassOrSuperclassTable(table) || getSuperclass().isClassOrSuperclassTable(table);
	}

	@Override
	public Table getTable() {
		return getSuperclass().getTable();
	}

	@Override
	public boolean isForceDiscriminator() {
		return getSuperclass().isForceDiscriminator();
	}

	@Override
	public boolean isDiscriminatorInsertable() {
		return getSuperclass().isDiscriminatorInsertable();
	}

	@Override
	public java.util.Set getSynchronizedTables() {
		HashSet result = new HashSet();
		result.addAll(synchronizedTables);
		result.addAll( getSuperclass().getSynchronizedTables() );
		return result;
	}

	@Override
	public Object accept(PersistentClassVisitor mv) {
		return mv.accept(this);
	}

	@Override
	public java.util.List getFilters() {
		java.util.List filters = new ArrayList(super.getFilters());
		filters.addAll(getSuperclass().getFilters());
		return filters;
	}

	@Override
	public boolean hasSubselectLoadableCollections() {
		return super.hasSubselectLoadableCollections() || 
			getSuperclass().hasSubselectLoadableCollections();
	}

	@Override
	public String getTuplizerImplClassName(EntityMode mode) {
		String impl = super.getTuplizerImplClassName( mode );
		if ( impl == null ) {
			impl = getSuperclass().getTuplizerImplClassName( mode );
		}
		return impl;
	}

	@Override
	public Map getTuplizerMap() {
		Map specificTuplizerDefs = super.getTuplizerMap();
		Map superclassTuplizerDefs = getSuperclass().getTuplizerMap();
		if ( specificTuplizerDefs == null && superclassTuplizerDefs == null ) {
			return null;
		}
		else {
			Map combined = new HashMap();
			if ( superclassTuplizerDefs != null ) {
				combined.putAll( superclassTuplizerDefs );
			}
			if ( specificTuplizerDefs != null ) {
				combined.putAll( specificTuplizerDefs );
			}
			return java.util.Collections.unmodifiableMap( combined );
		}
	}

	@Override
	public Component getIdentifierMapper() {
		return superclass.getIdentifierMapper();
	}

	@Override
	public OptimisticLockStyle getOptimisticLockStyle() {
		return superclass.getOptimisticLockStyle();
	}
}
