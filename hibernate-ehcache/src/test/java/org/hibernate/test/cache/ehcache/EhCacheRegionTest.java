package org.hibernate.test.cache.ehcache;

import org.hibernate.cache.internal.EhCacheRegionFactory;
import org.hibernate.cache.internal.strategy.ItemValueExtractor;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import java.util.Map;

/**
 * @author Alex Snaps
 */
public class EhCacheRegionTest extends EhCacheTest {
	@Override
	protected void configCache(final Configuration cfg) {
		cfg.setProperty(Environment.CACHE_REGION_FACTORY, EhCacheRegionFactory.class.getName());
		cfg.setProperty(Environment.CACHE_PROVIDER_CONFIG, "ehcache.xml");
	}

	@Override
	protected Map getMapFromCacheEntry(final Object entry) {
		final Map map;
		if (entry.getClass().getName().equals("org.hibernate.cache.internal.strategy.AbstractReadWriteEhcacheAccessStrategy$Item")) {
			map = ItemValueExtractor.getValue(entry);
		} else {
			map = (Map)entry;
		}
		return map;
	}
}
