/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.osgi.test.client;

import java.util.Hashtable;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.osgi.test.result.OsgiTestResult;
import org.hibernate.osgi.test.result.OsgiTestResultImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * @author Brett Meyer
 */
public class OsgiTestActivator implements BundleActivator {
	
	private OsgiTestResult testResult = new OsgiTestResultImpl();

	@Override
	public void start(BundleContext context) throws Exception {
        
		context.registerService( OsgiTestResult.class, testResult, new Hashtable() );
		
		testUnmanagedJpa( context );
		testUnmanagedNative( context );
		testLazyLoading( context );
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		
	}
	
	private void testUnmanagedJpa(BundleContext context) {
		try {
			ServiceReference serviceReference = context.getServiceReference( PersistenceProvider.class.getName() );
		    PersistenceProvider persistenceProvider = (PersistenceProvider) context.getService( serviceReference );
		    EntityManagerFactory emf = persistenceProvider.createEntityManagerFactory( "hibernate-osgi-test", null );
		    EntityManager em = emf.createEntityManager();
		    
		    DataPoint dp = new DataPoint();
	        dp.setName( "Brett" );
	        em.getTransaction().begin();
	        em.persist( dp );
	        em.getTransaction().commit();
	        em.clear();
	        
	        em.getTransaction().begin();
	        List<DataPoint> results = em.createQuery( "from DataPoint" ).getResultList();
	        if ( results.size() == 0 || !results.get(0).getName().equals( "Brett" ) ) {
	        	testResult.addFailure( "Unmanaged JPA: Unexpected data returned!" );
	        }
	        dp = results.get(0);
	        dp.setName( "Brett2" );
	        em.merge( dp );
	        em.getTransaction().commit();
	        em.clear();
	
	        em.getTransaction().begin();
	        results = em.createQuery( "from DataPoint" ).getResultList();
	        if ( results.size() == 0 || !results.get(0).getName().equals( "Brett2" ) ) {
	        	testResult.addFailure( "Unmanaged JPA: The update/merge failed!" );
	        }
	        em.getTransaction().commit();
	        em.clear();
	
	        em.getTransaction().begin();
	        em.createQuery( "delete from DataPoint" ).executeUpdate();
	        em.getTransaction().commit();
	        em.clear();
	
	        em.getTransaction().begin();
	        results = em.createQuery( "from DataPoint" ).getResultList();
	        if ( results.size() > 0 ) {
	        	testResult.addFailure( "Unmanaged JPA: The delete failed!" );
	        }
	        em.getTransaction().commit();
	        em.close();
		}
		catch ( Exception e ) {
			testResult.addFailure( "Exception: " + e.getMessage() );
		}
	}
	
	private void testUnmanagedNative(BundleContext context) {
		try {
			ServiceReference sr = context.getServiceReference( SessionFactory.class.getName() );
			SessionFactory sf = (SessionFactory) context.getService( sr );
		    Session s = sf.openSession();
		    
		    DataPoint dp = new DataPoint();
	        dp.setName( "Brett" );
	        s.getTransaction().begin();
	        s.persist( dp );
	        s.getTransaction().commit();
	        s.clear();
	        
	        s.getTransaction().begin();
	        List<DataPoint> results = s.createQuery( "from DataPoint" ).list();
	        if ( results.size() == 0 || !results.get(0).getName().equals( "Brett" ) ) {
	        	testResult.addFailure( "Native Hibernate: Unexpected data returned!" );
	        }
	        dp = results.get(0);
	        dp.setName( "Brett2" );
	        s.update( dp );
	        s.getTransaction().commit();
	        s.clear();
	
	        s.getTransaction().begin();
	        results = s.createQuery( "from DataPoint" ).list();
	        if ( results.size() == 0 || !results.get(0).getName().equals( "Brett2" ) ) {
	        	testResult.addFailure( "Native Hibernate: The update/merge failed!" );
	        }
	        s.getTransaction().commit();
	        s.clear();
	
	        s.getTransaction().begin();
	        s.createQuery( "delete from DataPoint" ).executeUpdate();
	        s.getTransaction().commit();
	        s.clear();
	
	        s.getTransaction().begin();
	        results = s.createQuery( "from DataPoint" ).list();
	        if ( results.size() > 0 ) {
	        	testResult.addFailure( "Native Hibernate: The delete failed!" );
	        }
	        s.getTransaction().commit();
	        s.close();
		}
		catch ( Exception e ) {
			testResult.addFailure( "Exception: " + e.getMessage() );
		}
	}
	
	private void testLazyLoading(BundleContext context) {
		try {
			ServiceReference sr = context.getServiceReference( SessionFactory.class.getName() );
			SessionFactory sf = (SessionFactory) context.getService( sr );
		    Session s = sf.openSession();
		    
		    DataPoint dp = new DataPoint();
	        dp.setName( "Brett" );
	        s.getTransaction().begin();
	        s.persist( dp );
	        s.getTransaction().commit();
	        s.clear();
	        
	        s.getTransaction().begin();
	        // ensure the proxy comes through ok
	        dp = (DataPoint) s.load( DataPoint.class, new Long(dp.getId()) );
	    	// initialize and test
	        if ( dp == null || !dp.getName().equals( "Brett" ) ) {
	        	testResult.addFailure( "Native Hibernate: Lazy loading/proxy failed!" );
	        }
	        s.getTransaction().commit();
	        s.close();
		}
		catch ( Exception e ) {
			testResult.addFailure( "Exception: " + e.getMessage() );
		}
	}

}
