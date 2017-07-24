/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.collection.internal.AbstractPersistentCollection;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.pretty.MessageHelper;

/**
 * We need an entry to tell us all about the current state
 * of a collection with respect to its persistent state
 *
 * @author Gavin King
 */
public final class CollectionEntry implements Serializable {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( CollectionEntry.class );

	//ATTRIBUTES MAINTAINED BETWEEN FLUSH CYCLES

	// session-start/post-flush persistent state
	private Serializable snapshot;
	// allow the CollectionSnapshot to be serialized
	private NavigableRole role;

	// "loaded" means the reference that is consistent
	// with the current database state
	private transient PersistentCollectionDescriptor loadedCollectionDescriptor;
	private Serializable loadedKey;

	// ATTRIBUTES USED ONLY DURING FLUSH CYCLE

	// during flush, we navigate the object graph to
	// collections and decide what to do with them
	private transient boolean reached;
	private transient boolean processed;
	private transient boolean doupdate;
	private transient boolean doremove;
	private transient boolean dorecreate;
	// if we instantiate a collection during the flush() process,
	// we must ignore it for the rest of the flush()
	private transient boolean ignore;

	// "current" means the reference that was found during flush()
	private transient PersistentCollectionDescriptor currentPersister;
	private transient Serializable currentKey;

	/**
	 * For newly wrapped collections, or dereferenced collection wrappers
	 */
	public CollectionEntry(PersistentCollectionDescriptor collectionDescriptor, PersistentCollection collection) {
		// new collections that get found + wrapped
		// during flush shouldn't be ignored
		ignore = false;

		collection.clearDirty(); //a newly wrapped collection is NOT dirty (or we get unnecessary version updates)

		snapshot = collectionDescriptor.getJavaTypeDescriptor().getMutabilityPlan().isMutable() ?
				collection.getSnapshot(collectionDescriptor) :
				null;
		collection.setSnapshot(loadedKey, collectionDescriptor.getNavigableRole(), snapshot);
	}

	/**
	 * For collections just loaded from the database
	 */
	public CollectionEntry(
			final PersistentCollection collection,
			final PersistentCollectionDescriptor loadedPersister,
			final Serializable loadedKey,
			final boolean ignore
	) {
		this.ignore=ignore;

		//collection.clearDirty()

		this.loadedKey = loadedKey;
		setLoadedDescriptor( loadedPersister);

		collection.setSnapshot(loadedKey, loadedPersister.getNavigableRole(), null);

		//postInitialize() will be called afterQuery initialization
	}

	/**
	 * For uninitialized detached collections
	 */
	public CollectionEntry(PersistentCollectionDescriptor loadedPersister, Serializable loadedKey) {
		// detached collection wrappers that get found + reattached
		// during flush shouldn't be ignored
		ignore = false;

		//collection.clearDirty()

		this.loadedKey = loadedKey;
		setLoadedDescriptor( loadedPersister);
	}

	/**
	 * For initialized detached collections
	 */
	public CollectionEntry(PersistentCollection collection, SessionFactoryImplementor factory) throws MappingException {
		// detached collections that get found + reattached
		// during flush shouldn't be ignored
		ignore = false;

		loadedKey = collection.getKey();
		setLoadedDescriptor( factory.getTypeConfiguration().findCollectionPersister( collection.getRole() ) );

		snapshot = collection.getStoredSnapshot();
	}

	/**
	 * Used from custom serialization.
	 *
	 * @see #serialize
	 * @see #deserialize
	 */
	private CollectionEntry(
			NavigableRole role,
			Serializable snapshot,
			Serializable loadedKey,
			SessionFactoryImplementor factory) {
		this.role = role;
		this.snapshot = snapshot;
		this.loadedKey = loadedKey;
		if ( role != null ) {
			afterDeserialize( factory );
		}
	}

	/**
	 * Determine if the collection is "really" dirty, by checking dirtiness
	 * of the collection elements, if necessary
	 */
	private void dirty(PersistentCollection collection) throws HibernateException {

		boolean forceDirty = collection.wasInitialized() &&
				!collection.isDirty() && //optimization
				getLoadedPersistentCollectionDescriptor() != null &&
				getLoadedPersistentCollectionDescriptor().getJavaTypeDescriptor().getMutabilityPlan().isMutable() && //optimization
				( collection.isDirectlyAccessible() || getLoadedPersistentCollectionDescriptor().getJavaTypeDescriptor().getMutabilityPlan().isMutable() ) && //optimization
				!collection.equalsSnapshot( getLoadedPersistentCollectionDescriptor() );

		if ( forceDirty ) {
			collection.dirty();
		}

	}

	public void preFlush(PersistentCollection collection) throws HibernateException {
		if ( loadedKey == null && collection.getKey() != null ) {
			loadedKey = collection.getKey();
		}

		boolean nonMutableChange = collection.isDirty()
				&& getLoadedPersistentCollectionDescriptor() != null
				&& !getLoadedPersistentCollectionDescriptor().getJavaTypeDescriptor().getMutabilityPlan().isMutable();
		if ( nonMutableChange ) {
			throw new HibernateException(
					"changed an immutable collection instance: " +
					MessageHelper.collectionInfoString( getLoadedPersistentCollectionDescriptor().getNavigableRole().getFullPath(), getLoadedKey() )
			);
		}

		dirty( collection );

		if ( LOG.isDebugEnabled() && collection.isDirty() && getLoadedPersistentCollectionDescriptor() != null ) {
			LOG.debugf(
					"Collection dirty: %s",
					MessageHelper.collectionInfoString( getLoadedPersistentCollectionDescriptor().getNavigableRole().getFullPath(), getLoadedKey() )
			);
		}

		setReached( false );
		setProcessed( false );

		setDoupdate( false );
		setDoremove( false );
		setDorecreate( false );
	}

	public void postInitialize(PersistentCollection collection) throws HibernateException {
		snapshot = getLoadedPersistentCollectionDescriptor().getJavaTypeDescriptor().getMutabilityPlan().isMutable()
				? collection.getSnapshot( getLoadedPersistentCollectionDescriptor() )
				: null;
		collection.setSnapshot(loadedKey, role, snapshot);
		if ( getLoadedPersistentCollectionDescriptor().getBatchSize() > 1 ) {
			( (AbstractPersistentCollection) collection ).getSession()
					.getPersistenceContext()
					.getBatchFetchQueue()
					.removeBatchLoadableCollection( this );
		}
	}

	/**
	 * Called afterQuery a successful flush
	 */
	public void postFlush(PersistentCollection collection) throws HibernateException {
		if ( isIgnore() ) {
			ignore = false;
		}
		else if ( !isProcessed() ) {
			throw new HibernateException( LOG.collectionNotProcessedByFlush( collection.getRole() ) );
		}
		collection.setSnapshot(loadedKey, role, snapshot);
	}

	/**
	 * Called afterQuery execution of an action
	 */
	public void afterAction(PersistentCollection collection) {
		loadedKey = getCurrentKey();
		setLoadedDescriptor( getCurrentPersister() );

		boolean resnapshot = collection.wasInitialized() &&
				( isDoremove() || isDorecreate() || isDoupdate() );
		if ( resnapshot ) {
			snapshot = loadedCollectionDescriptor ==null || !loadedCollectionDescriptor.getJavaTypeDescriptor().getMutabilityPlan().isMutable() ?
					null :
					collection.getSnapshot( loadedCollectionDescriptor ); //re-snapshot
		}

		collection.postAction();
	}

	public Serializable getKey() {
		return getLoadedKey();
	}

	/**
	 * @deprecated (since 6.0) use {@link #getNavigableRole}
	 */
	@Deprecated
	public String getRole() {
		return role.getFullPath();
	}

	public NavigableRole getNavigableRole(){
		return role;
	}

	public Serializable getSnapshot() {
		return snapshot;
	}

	private boolean fromMerge;

	/**
	 * Reset the stored snapshot for both the persistent collection and this collection entry. 
	 * Used during the merge of detached collections.
	 * 
	 * @param collection the persistentcollection to be updated
	 * @param storedSnapshot the new stored snapshot
	 */
	public void resetStoredSnapshot(PersistentCollection collection, Serializable storedSnapshot) {
		LOG.debugf("Reset storedSnapshot to %s for %s", storedSnapshot, this);

		if ( fromMerge ) {
			return; // EARLY EXIT!
		}

		snapshot = storedSnapshot;
		collection.setSnapshot( loadedKey, role, snapshot );
		fromMerge = true;
	}

	private void setLoadedDescriptor(PersistentCollectionDescriptor collectionDescriptor) {
		loadedCollectionDescriptor = collectionDescriptor;
		setRole( collectionDescriptor == null ? null : collectionDescriptor.getNavigableRole() );
	}

	void afterDeserialize(SessionFactoryImplementor factory) {
		loadedCollectionDescriptor = ( factory == null ?
				null :
				factory.getTypeConfiguration().findCollectionPersister( role.getFullPath() ) );
	}

	public boolean wasDereferenced() {
		return getLoadedKey() == null;
	}

	public boolean isReached() {
		return reached;
	}

	public void setReached(boolean reached) {
		this.reached = reached;
	}

	public boolean isProcessed() {
		return processed;
	}

	public void setProcessed(boolean processed) {
		this.processed = processed;
	}

	public boolean isDoupdate() {
		return doupdate;
	}

	public void setDoupdate(boolean doupdate) {
		this.doupdate = doupdate;
	}

	public boolean isDoremove() {
		return doremove;
	}

	public void setDoremove(boolean doremove) {
		this.doremove = doremove;
	}

	public boolean isDorecreate() {
		return dorecreate;
	}

	public void setDorecreate(boolean dorecreate) {
		this.dorecreate = dorecreate;
	}

	public boolean isIgnore() {
		return ignore;
	}

	public PersistentCollectionDescriptor getCurrentPersister() {
		return currentPersister;
	}

	public void setCurrentPersister(PersistentCollectionDescriptor currentPersister) {
		this.currentPersister = currentPersister;
	}

	/**
	 * This is only available late during the flush
	 * cycle
	 */
	public Serializable getCurrentKey() {
		return currentKey;
	}

	public void setCurrentKey(Serializable currentKey) {
		this.currentKey = currentKey;
	}

	/**
	 * This is only available late during the flush cycle
	 */
	public PersistentCollectionDescriptor getLoadedPersistentCollectionDescriptor() {
		return loadedCollectionDescriptor;
	}

	public Serializable getLoadedKey() {
		return loadedKey;
	}

	private void setRole(NavigableRole role) {
		this.role = role;
	}

	@Override
	public String toString() {
		String result = "CollectionEntry" +
				MessageHelper.collectionInfoString( loadedCollectionDescriptor.getNavigableRole().getFullPath(), loadedKey );
		if ( currentPersister != null ) {
			result += "->" +
					MessageHelper.collectionInfoString( currentPersister.getNavigableRole().getFullPath(), currentKey );
		}
		return result;
	}

	/**
	 * Get the collection orphans (entities which were removed from the collection)
	 */
	public Collection getOrphans(String entityName, PersistentCollection collection)
	throws HibernateException {
		if (snapshot==null) {
			throw new AssertionFailure("no collection snapshot for orphan delete");
		}
		return collection.getOrphans( snapshot, entityName );
	}

	public boolean isSnapshotEmpty(PersistentCollection collection) {
		//TODO: does this really need to be here?
		//      does the collection already have
		//      it's own up-to-date snapshot?
		return collection.wasInitialized() &&
			( getLoadedPersistentCollectionDescriptor()==null || getLoadedPersistentCollectionDescriptor().getJavaTypeDescriptor().getMutabilityPlan().isMutable() ) &&
			collection.isSnapshotEmpty( getSnapshot() );
	}



	/**
	 * Custom serialization routine used during serialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param oos The stream to which we should write the serial data.
	 * @throws java.io.IOException
	 */
	public void serialize(ObjectOutputStream oos) throws IOException {
		oos.writeObject( role );
		oos.writeObject( snapshot );
		oos.writeObject( loadedKey );
	}

	/**
	 * Custom deserialization routine used during deserialization of a
	 * Session/PersistenceContext for increased performance.
	 *
	 * @param ois The stream from which to read the entry.
	 * @param session The session being deserialized.
	 *
	 * @return The deserialized CollectionEntry
	 *
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static CollectionEntry deserialize(
			ObjectInputStream ois,
			SessionImplementor session) throws IOException, ClassNotFoundException {
		return new CollectionEntry(
				(NavigableRole) ois.readObject(),
				(Serializable) ois.readObject(),
				(Serializable) ois.readObject(),
				(session == null ? null : session.getFactory())
		);
	}
}
