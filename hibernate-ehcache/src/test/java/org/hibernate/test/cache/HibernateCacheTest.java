package org.hibernate.test.cache;

import org.hamcrest.CoreMatchers;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cache.internal.strategy.ItemValueExtractor;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cfg.Configuration;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.hibernate.test.domain.Event;
import org.hibernate.test.domain.EventManager;
import org.hibernate.test.domain.Item;
import org.hibernate.test.domain.Person;
import org.hibernate.test.domain.PhoneNumber;
import org.hibernate.test.domain.VersionedItem;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Chris Dennis
 */
public class HibernateCacheTest {

	private static SessionFactory sessionFactory;
	private static Configuration config;
	private static final String REGION_PREFIX = "hibernate.test.";

	public synchronized static SessionFactory getSessionFactory() {
		if (sessionFactory == null) {
			try {
				sessionFactory = config.buildSessionFactory();
			} catch (HibernateException ex) {
				System.err.println("Initial SessionFactory creation failed." + ex);
				throw new ExceptionInInitializerError(ex);
			}
		}
		return sessionFactory;
	}

	@BeforeClass
	public static void setUp() {
		System.setProperty("derby.system.home", "target/derby");
		config = new Configuration().configure("/hibernate-config/hibernate.cfg.xml");
		config.setProperty("hibernate.hbm2ddl.auto", "create");
		getSessionFactory().getStatistics().setStatisticsEnabled(true);
	}

	@AfterClass
	public static void tearDown() {
		getSessionFactory().close();
	}

	@Test
	public void testQueryCacheInvalidation() throws Exception {
		Session s = getSessionFactory().openSession();
		Transaction t = s.beginTransaction();
		Item i = new Item();
		i.setName("widget");
		i.setDescription("A really top-quality, full-featured widget.");
		s.persist(i);
		t.commit();
		s.close();

		SecondLevelCacheStatistics slcs = s.getSessionFactory().getStatistics().getSecondLevelCacheStatistics(REGION_PREFIX + Item.class.getName());

		assertThat(slcs.getPutCount(), equalTo(1L));
		assertThat(slcs.getElementCountInMemory(), equalTo(1L));
		assertThat(slcs.getEntries().size(), equalTo(1));

		s = getSessionFactory().openSession();
		t = s.beginTransaction();
		i = (Item)s.get(Item.class, i.getId());

		assertThat(slcs.getHitCount(), equalTo(1L));
		assertThat(slcs.getMissCount(), equalTo(0L));

		i.setDescription("A bog standard item");

		t.commit();
		s.close();

		assertThat(slcs.getPutCount(), equalTo(2L));

		Object entry = slcs.getEntries().get(i.getId());
		Map map;
		if (entry instanceof Map) {
			map = (Map)entry;
		} else {
			map = ItemValueExtractor.getValue(entry);
		}
		assertThat((String)map.get("description"), equalTo("A bog standard item"));
		assertThat((String)map.get("name"), equalTo("widget"));

		// cleanup
		s = getSessionFactory().openSession();
		t = s.beginTransaction();
		s.delete(i);
		t.commit();
		s.close();
	}

	@Test
	public void testEmptySecondLevelCacheEntry() throws Exception {
		getSessionFactory().evictEntity(Item.class.getName());
		Statistics stats = getSessionFactory().getStatistics();
		stats.clear();
		SecondLevelCacheStatistics statistics = stats.getSecondLevelCacheStatistics(REGION_PREFIX + Item.class.getName());
		Map cacheEntries = statistics.getEntries();
		assertThat(cacheEntries.size(), equalTo(0));
	}

	@Test
	public void testStaleWritesLeaveCacheConsistent() {
		Session s = getSessionFactory().openSession();
		Transaction txn = s.beginTransaction();
		VersionedItem item = new VersionedItem();
		item.setName("steve");
		item.setDescription("steve's item");
		s.save(item);
		txn.commit();
		s.close();

		Long initialVersion = item.getVersion();

		// manually revert the version property
		item.setVersion(item.getVersion() - 1);

		try {
			s = getSessionFactory().openSession();
			txn = s.beginTransaction();
			s.update(item);
			txn.commit();
			s.close();
			fail("expected stale write to fail");
		} catch (Throwable expected) {
			// expected behavior here
			if (txn != null) {
				try {
					txn.rollback();
				} catch (Throwable ignore) {
				}
			}
		} finally {
			if (s != null && s.isOpen()) {
				try {
					s.close();
				} catch (Throwable ignore) {
				}
			}
		}

		// check the version value in the cache...
		SecondLevelCacheStatistics slcs = getSessionFactory().getStatistics().getSecondLevelCacheStatistics(REGION_PREFIX + VersionedItem.class.getName());
		assertThat(slcs, CoreMatchers.<Object>notNullValue());
		final Map entries = slcs.getEntries();
		Object entry = entries.get(item.getId());
		Long cachedVersionValue;
		if (entry instanceof SoftLock) {
			//FIXME don't know what to test here
			//cachedVersionValue = new Long( ( (ReadWriteCache.Lock) entry).getUnlockTimestamp() );
		} else {
			cachedVersionValue = (Long)((Map)entry).get("_version");
			assertThat(initialVersion, equalTo(cachedVersionValue));
		}


		// cleanup
		s = getSessionFactory().openSession();
		txn = s.beginTransaction();
		item = (VersionedItem)s.load(VersionedItem.class, item.getId());
		s.delete(item);
		txn.commit();
		s.close();

	}

	@Test
	public void testGeneralUsage() {
		EventManager mgr = new EventManager(getSessionFactory());
		Statistics stats = getSessionFactory().getStatistics();

		// create 3 persons Steve, Orion, Tim
		Person stevePerson = new Person();
		stevePerson.setFirstname("Steve");
		stevePerson.setLastname("Harris");
		Long steveId = mgr.createAndStorePerson(stevePerson);
		mgr.addEmailToPerson(steveId, "steve@tc.com");
		mgr.addEmailToPerson(steveId, "sharrif@tc.com");
		mgr.addTalismanToPerson(steveId, "rabbit foot");
		mgr.addTalismanToPerson(steveId, "john de conqueroo");

		PhoneNumber p1 = new PhoneNumber();
		p1.setNumberType("Office");
		p1.setPhone(111111);
		mgr.addPhoneNumberToPerson(steveId, p1);

		PhoneNumber p2 = new PhoneNumber();
		p2.setNumberType("Home");
		p2.setPhone(222222);
		mgr.addPhoneNumberToPerson(steveId, p2);

		Person orionPerson = new Person();
		orionPerson.setFirstname("Orion");
		orionPerson.setLastname("Letizi");
		Long orionId = mgr.createAndStorePerson(orionPerson);
		mgr.addEmailToPerson(orionId, "orion@tc.com");
		mgr.addTalismanToPerson(orionId, "voodoo doll");

		Long timId = mgr.createAndStorePerson("Tim", "Teck");
		mgr.addEmailToPerson(timId, "teck@tc.com");
		mgr.addTalismanToPerson(timId, "magic decoder ring");

		Long engMeetingId = mgr.createAndStoreEvent("Eng Meeting", stevePerson, new Date());
		mgr.addPersonToEvent(steveId, engMeetingId);
		mgr.addPersonToEvent(orionId, engMeetingId);
		mgr.addPersonToEvent(timId, engMeetingId);

		Long docMeetingId = mgr.createAndStoreEvent("Doc Meeting", orionPerson, new Date());
		mgr.addPersonToEvent(steveId, docMeetingId);
		mgr.addPersonToEvent(orionId, docMeetingId);

		for (Event event : (List<Event>)mgr.listEvents()) {
			mgr.listEmailsOfEvent(event.getId());
		}

		getSessionFactory().close();

		QueryStatistics queryStats = stats.getQueryStatistics("from Event");
		assertThat("Cache Miss Count", queryStats.getCacheMissCount(), equalTo(1L));
		assertThat("Cache Hit Count", queryStats.getCacheHitCount(), equalTo(0L));
		assertThat("Cache Put Count", queryStats.getCachePutCount(), equalTo(1L));
	}
}