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

import org.hibernate.metamodel.source.annotation.xml.XMLCollectionTable;
import org.hibernate.metamodel.source.annotation.xml.XMLJoinTable;
import org.hibernate.metamodel.source.annotation.xml.XMLSecondaryTable;
import org.hibernate.metamodel.source.annotation.xml.XMLTable;

/**
 * @author Strong Liu
 */
interface SchemaAware {
	String getSchema();

	void setSchema(String schema);

	String getCatalog();

	void setCatalog(String catalog);

	static class SecondaryTableSchemaAware implements SchemaAware {
		private XMLSecondaryTable table;

		SecondaryTableSchemaAware(XMLSecondaryTable table) {
			this.table = table;
		}

		@Override
		public String getCatalog() {
			return table.getCatalog();
		}

		@Override
		public String getSchema() {
			return table.getSchema();
		}

		@Override
		public void setSchema(String schema) {
			table.setSchema( schema );
		}

		@Override
		public void setCatalog(String catalog) {
			table.setCatalog( catalog );
		}
	}

	static class TableSchemaAware implements SchemaAware {
		private XMLTable table;

		public TableSchemaAware(XMLTable table) {
			this.table = table;
		}

		@Override
		public String getCatalog() {
			return table.getCatalog();
		}

		@Override
		public String getSchema() {
			return table.getSchema();
		}

		@Override
		public void setSchema(String schema) {
			table.setSchema( schema );
		}

		@Override
		public void setCatalog(String catalog) {
			table.setCatalog( catalog );
		}
	}

	static class JoinTableSchemaAware implements SchemaAware {
		private XMLJoinTable table;

		public JoinTableSchemaAware(XMLJoinTable table) {
			this.table = table;
		}

		@Override
		public String getCatalog() {
			return table.getCatalog();
		}

		@Override
		public String getSchema() {
			return table.getSchema();
		}

		@Override
		public void setSchema(String schema) {
			table.setSchema( schema );
		}

		@Override
		public void setCatalog(String catalog) {
			table.setCatalog( catalog );
		}
	}

	static class CollectionTableSchemaAware implements SchemaAware {
		private XMLCollectionTable table;

		public CollectionTableSchemaAware(XMLCollectionTable table) {
			this.table = table;
		}

		@Override
		public String getCatalog() {
			return table.getCatalog();
		}

		@Override
		public String getSchema() {
			return table.getSchema();
		}

		@Override
		public void setSchema(String schema) {
			table.setSchema( schema );
		}

		@Override
		public void setCatalog(String catalog) {
			table.setCatalog( catalog );
		}
	}
}
