/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat.internal;

import java.io.Serializable;
import java.util.concurrent.atomic.LongAdder;

import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.EntityStatistics;

/**
 * Entity related statistics
 *
 * @author Alex Snaps
 */
public class EntityStatisticsImpl extends AbstractCacheableDataStatistics implements EntityStatistics, Serializable {

	private final String rootEntityName;
	private final LongAdder loadCount = new LongAdder();
	private final LongAdder updateCount = new LongAdder();
	private final LongAdder insertCount = new LongAdder();
	private final LongAdder deleteCount = new LongAdder();
	private final LongAdder fetchCount = new LongAdder();
	private final LongAdder optimisticFailureCount = new LongAdder();

	EntityStatisticsImpl(EntityPersister rootEntityDescriptor) {
		super(
				() -> rootEntityDescriptor.getCacheAccessStrategy() != null
						? rootEntityDescriptor.getCacheAccessStrategy().getRegion()
						: null
		);
		this.rootEntityName = rootEntityDescriptor.getRootEntityName();
	}

	@Override
	public long getDeleteCount() {
		return deleteCount.sum();
	}

	@Override
	public long getInsertCount() {
		return insertCount.sum();
	}

	@Override
	public long getLoadCount() {
		return loadCount.sum();
	}

	@Override
	public long getUpdateCount() {
		return updateCount.sum();
	}

	@Override
	public long getFetchCount() {
		return fetchCount.sum();
	}

	@Override
	public long getOptimisticFailureCount() {
		return optimisticFailureCount.sum();
	}

	void incrementLoadCount() {
		loadCount.increment();
	}

	void incrementFetchCount() {
		fetchCount.increment();
	}

	void incrementUpdateCount() {
		updateCount.increment();
	}

	void incrementInsertCount() {
		insertCount.increment();
	}

	void incrementDeleteCount() {
		deleteCount.increment();
	}

	void incrementOptimisticFailureCount() {
		optimisticFailureCount.increment();
	}

	@Override
	public String toString() {
		final StringBuilder buffer = new StringBuilder()
				.append( "EntityStatistics" )
				.append( "[rootEntityName=" ).append( rootEntityName )
				.append( ",loadCount=" ).append( this.loadCount )
				.append( ",updateCount=" ).append( this.updateCount )
				.append( ",insertCount=" ).append( this.insertCount )
				.append( ",deleteCount=" ).append( this.deleteCount )
				.append( ",fetchCount=" ).append( this.fetchCount )
				.append( ",optimisticLockFailureCount=" ).append( this.optimisticFailureCount );
		appendCacheStats( buffer );
		return buffer.append( ']' ).toString();
	}
}
