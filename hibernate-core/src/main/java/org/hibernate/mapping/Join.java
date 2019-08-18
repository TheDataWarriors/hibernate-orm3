/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.sql.Alias;

/**
 * @author Gavin King
 */
public class Join implements AttributeContainer, Serializable {

	private static final Alias PK_ALIAS = new Alias(15, "PK");

	private ArrayList properties = new ArrayList();
	private ArrayList declaredProperties = new ArrayList();
	private Table table;
	private KeyValue key;
	private PersistentClass persistentClass;
	private boolean sequentialSelect;
	private boolean inverse;
	private boolean optional;

	// Custom SQL
	private String customSQLInsert;
	private boolean customInsertCallable;
	private ExecuteUpdateResultCheckStyle insertCheckStyle;
	private String customSQLUpdate;
	private boolean customUpdateCallable;
	private ExecuteUpdateResultCheckStyle updateCheckStyle;
	private String customSQLDelete;
	private boolean customDeleteCallable;
	private ExecuteUpdateResultCheckStyle deleteCheckStyle;

	@Override
	public void addProperty(Property prop) {
		properties.add(prop);
		declaredProperties.add(prop);
		prop.setPersistentClass( getPersistentClass() );
	}

	public void addMappedsuperclassProperty(Property prop) {
		properties.add(prop);
		prop.setPersistentClass( getPersistentClass() );
	}

	public Iterator getDeclaredPropertyIterator() {
		return declaredProperties.iterator();
	}

	public boolean containsProperty(Property prop) {
		return properties.contains(prop);
	}
	public Iterator getPropertyIterator() {
		return properties.iterator();
	}

	public Table getTable() {
		return table;
	}
	public void setTable(Table table) {
		this.table = table;
	}

	public KeyValue getKey() {
		return key;
	}
	public void setKey(KeyValue key) {
		this.key = key;
	}

	public PersistentClass getPersistentClass() {
		return persistentClass;
	}

	public void setPersistentClass(PersistentClass persistentClass) {
		this.persistentClass = persistentClass;
	}

	public void createForeignKey() {
		getKey().createForeignKeyOfEntity( persistentClass.getEntityName() );
	}

	public void createPrimaryKey() {
		//Primary key constraint
		PrimaryKey pk = new PrimaryKey( table );
		pk.setName( PK_ALIAS.toAliasString( table.getName() ) );
		table.setPrimaryKey(pk);

		pk.addColumns( getKey().getColumnIterator() );
	}

	public int getPropertySpan() {
		return properties.size();
	}

	public void setCustomSQLInsert(String customSQLInsert, boolean callable, ExecuteUpdateResultCheckStyle checkStyle) {
		this.customSQLInsert = customSQLInsert;
		this.customInsertCallable = callable;
		this.insertCheckStyle = checkStyle;
	}

	public String getCustomSQLInsert() {
		return customSQLInsert;
	}

	public boolean isCustomInsertCallable() {
		return customInsertCallable;
	}

	public ExecuteUpdateResultCheckStyle getCustomSQLInsertCheckStyle() {
		return insertCheckStyle;
	}

	public void setCustomSQLUpdate(String customSQLUpdate, boolean callable, ExecuteUpdateResultCheckStyle checkStyle) {
		this.customSQLUpdate = customSQLUpdate;
		this.customUpdateCallable = callable;
		this.updateCheckStyle = checkStyle;
	}

	public String getCustomSQLUpdate() {
		return customSQLUpdate;
	}

	public boolean isCustomUpdateCallable() {
		return customUpdateCallable;
	}

	public ExecuteUpdateResultCheckStyle getCustomSQLUpdateCheckStyle() {
		return updateCheckStyle;
	}

	public void setCustomSQLDelete(String customSQLDelete, boolean callable, ExecuteUpdateResultCheckStyle checkStyle) {
		this.customSQLDelete = customSQLDelete;
		this.customDeleteCallable = callable;
		this.deleteCheckStyle = checkStyle;
	}

	public String getCustomSQLDelete() {
		return customSQLDelete;
	}

	public boolean isCustomDeleteCallable() {
		return customDeleteCallable;
	}

	public ExecuteUpdateResultCheckStyle getCustomSQLDeleteCheckStyle() {
		return deleteCheckStyle;
	}

	public boolean isSequentialSelect() {
		return sequentialSelect;
	}
	public void setSequentialSelect(boolean deferred) {
		this.sequentialSelect = deferred;
	}

	public boolean isInverse() {
		return inverse;
	}

	public void setInverse(boolean leftJoin) {
		this.inverse = leftJoin;
	}

	@Override
	public String toString() {
		return getClass().getName() + '(' + table.toString() + ')';
	}

	public boolean isLazy() {
		Iterator iter = getPropertyIterator();
		while ( iter.hasNext() ) {
			Property prop = (Property) iter.next();
			if ( !prop.isLazy() ) {
				return false;
			}
		}
		return true;
	}

	public boolean isOptional() {
		return optional;
	}
	public void setOptional(boolean nullable) {
		this.optional = nullable;
	}
}
