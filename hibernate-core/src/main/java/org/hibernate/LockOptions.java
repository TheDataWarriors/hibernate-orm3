/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.persistence.PessimisticLockScope;
import org.hibernate.query.Query;
import org.hibernate.query.spi.QueryOptions;

import static java.util.Collections.emptyList;

/**
 * Contains a set of options describing how a row of a database table
 * mapped by an entity should be locked. For
 * {@link Session#buildLockRequest(LockOptions)},
 * {@link Session#get(Class, Object, LockOptions)}, or
 * {@link Session#refresh(Object, LockOptions)}, the relevant options
 * are:
 * <ul>
 * <li>the {@link #getLockMode() lock mode},
 * <li>the {@link #getTimeOut() pessimistic lock timeout}, and
 * <li>the {@link #getLockScope() lock scope}, that is, whether the
 *     lock extends to rows of owned collections.
 * </ul>
 * In HQL and criteria queries, lock modes can be defined in an even
 * more granular fashion, with the option to specify a lock mode that
 * {@linkplain #setAliasSpecificLockMode(String, LockMode) applies
 * only to a certain query alias}.
 *
 * @author Scott Marlow
 */
public class LockOptions implements Serializable {
	/**
	 * Represents {@link LockMode#NONE}, where timeout and scope are
	 * not applicable.
	 */
	public static final LockOptions NONE = new LockOptions(LockMode.NONE);

	/**
	 * Represents {@link LockMode#READ}, where timeout and scope are
	 * not applicable.
	 */
	public static final LockOptions READ = new LockOptions(LockMode.READ);

	/**
	 * Represents {@link LockMode#PESSIMISTIC_WRITE} with
	 * {@linkplain #WAIT_FOREVER no timeout}, and
	 * {@linkplain PessimisticLockScope#NORMAL no extension of the
	 * lock to owned collections}.
	 */
	public static final LockOptions UPGRADE = new LockOptions(LockMode.PESSIMISTIC_WRITE);

	/**
	 * Indicates that the database should not wait at all to acquire
	 * a pessimistic lock which is not immediately available.
	 *
	 * @see #getTimeOut
	 */
	public static final int NO_WAIT = 0;

	/**
	 * Indicates that there is no timeout for the lock acquisition.
	 *
	 * @see #getTimeOut
	 */
	public static final int WAIT_FOREVER = -1;

	/**
	 * Indicates that rows which are already locked should be skipped.
	 *
	 * @see #getTimeOut()
	 */
	public static final int SKIP_LOCKED = -2;

	private LockMode lockMode = LockMode.NONE;
	private int timeout = WAIT_FOREVER;
	private boolean scope;

	private Map<String,LockMode> aliasSpecificLockModes;

	private Boolean followOnLocking;

	/**
	 * Constructs an instance with all default options.
	 */
	public LockOptions() {
	}

	/**
	 * Constructs an instance with the given {@linkplain LockMode
	 * lock mode}.
	 *
	 * @param lockMode The lock mode to use
	 */
	public LockOptions(LockMode lockMode) {
		this.lockMode = lockMode;
	}

	/**
	 * Determine of the lock options are empty.
	 *
	 * @return {@code true} if the lock options are equivalent to
	 *         {@link LockOptions#NONE}.
	 */
	public boolean isEmpty() {
		return lockMode == LockMode.NONE
			&& timeout == WAIT_FOREVER
			&& followOnLocking == null
			&& !scope
			&& !hasAliasSpecificLockModes();
	}

	/**
	 * Retrieve the overall lock mode in effect for this set of options.
	 *
	 * @return the overall lock mode
	 */
	public LockMode getLockMode() {
		return lockMode;
	}

	/**
	 * Set the overall {@linkplain LockMode lock mode}. The default is
	 * {@link LockMode#NONE}, that is, no locking at all.
	 *
	 * @param lockMode the new overall lock mode
	 * @return {@code this} for method chaining
	 */
	public LockOptions setLockMode(LockMode lockMode) {
		this.lockMode = lockMode;
		return this;
	}

	/**
	 * Specify the {@link LockMode} to be used for the given query alias.
	 *
	 * @param alias the query alias to which the lock mode applies
	 * @param lockMode the lock mode to apply to the given alias
	 * @return {@code this} for method chaining
	 *
	 * @see Query#setLockMode(String, LockMode)
	 */
	public LockOptions setAliasSpecificLockMode(String alias, LockMode lockMode) {
		if ( aliasSpecificLockModes == null ) {
			aliasSpecificLockModes = new LinkedHashMap<>();
		}
		if ( lockMode == null ) {
			aliasSpecificLockModes.remove( alias );
		}
		else {
			aliasSpecificLockModes.put( alias, lockMode );
		}
		return this;
	}

	/**
	 * Get the {@link LockMode} explicitly specified for the given alias
	 * via {@link #setAliasSpecificLockMode(String, LockMode)}.
	 * <p>
	 * Differs from {@link #getEffectiveLockMode(String)} in that here we
	 * only return an explicitly specified alias-specific lock mode.
	 *
	 * @param alias The alias for which to locate the explicit lock mode.
	 * @return The explicit lock mode for that alias.
	 */
	public LockMode getAliasSpecificLockMode(String alias) {
		return aliasSpecificLockModes == null ? null : aliasSpecificLockModes.get( alias );
	}

	/**
	 * Determine the {@link LockMode} to apply to the given alias. If no
	 * mode was {@linkplain #setAliasSpecificLockMode(String, LockMode)}
	 * explicitly set}, the {@linkplain #getLockMode()}  overall mode} is
	 * returned. If the overall lock mode is also {@code null},
	 * {@link LockMode#NONE} is returned.
	 * <p>
	 * Differs from {@link #getAliasSpecificLockMode(String)} in that here
	 * we fall back to only returning the overall lock mode.
	 *
	 * @param alias The alias for which to locate the effective lock mode.
	 * @return The effective lock mode.
	 */
	public LockMode getEffectiveLockMode(String alias) {
		LockMode lockMode = getAliasSpecificLockMode( alias );
		if ( lockMode == null ) {
			lockMode = this.lockMode;
		}
		return lockMode == null ? LockMode.NONE : lockMode;
	}

	/**
	 * Does this {@code LockOptions} instance define alias-specific lock
	 * modes?
	 *
	 * @return {@code true} if this object defines alias-specific lock modes;
	 *        {@code false} otherwise.
	 */
	public boolean hasAliasSpecificLockModes() {
		return aliasSpecificLockModes != null
			&& ! aliasSpecificLockModes.isEmpty();
	}

	/**
	 * The number of aliases that have alias-specific lock modes specified.
	 *
	 * @return the number of explicitly defined alias lock modes.
	 */
	public int getAliasLockCount() {
		return aliasSpecificLockModes == null ? 0 : aliasSpecificLockModes.size();
	}

	/**
	 * Iterator over {@link Map.Entry}s, each containing an alias and its
	 * {@link LockMode}.
	 *
	 * @return an iterator over the {@link Map.Entry}s
	 * @deprecated use {@link #getAliasSpecificLocks()}
	 */
	@Deprecated
	public Iterator<Map.Entry<String,LockMode>> getAliasLockIterator() {
		return getAliasSpecificLocks().iterator();
	}

	/**
	 * Iterable with {@link Map.Entry}s, each containing an alias and its
	 * {@link LockMode}.
	 *
	 * @return an iterable with the {@link Map.Entry}s
	 */
	public Iterable<Map.Entry<String,LockMode>> getAliasSpecificLocks() {
		return aliasSpecificLockModes == null ? emptyList() : aliasSpecificLockModes.entrySet();
	}

	/**
	 * Currently needed for follow-on locking.
	 *
	 * @return The greatest of all requested lock modes.
	 */
	public LockMode findGreatestLockMode() {
		LockMode lockModeToUse = getLockMode();
		if ( lockModeToUse == null ) {
			lockModeToUse = LockMode.NONE;
		}

		if ( aliasSpecificLockModes == null ) {
			return lockModeToUse;
		}

		for ( LockMode lockMode : aliasSpecificLockModes.values() ) {
			if ( lockMode.greaterThan( lockModeToUse ) ) {
				lockModeToUse = lockMode;
			}
		}

		return lockModeToUse;
	}

	/**
	 * The current timeout, a maximum amount of time in milliseconds
	 * that the database should wait to obtain a pessimistic lock before
	 * returning an error to the client.
	 * <p>
	 * {@link #NO_WAIT}, {@link #WAIT_FOREVER}, or {@link #SKIP_LOCKED}
	 * represent 3 "magic" values.
	 *
	 * @return a timeout in milliseconds, {@link #NO_WAIT},
	 *         {@link #WAIT_FOREVER}, or {@link #SKIP_LOCKED}
	 */
	public int getTimeOut() {
		return timeout;
	}

	/**
	 * Set the timeout, that is, the maximum amount of time in milliseconds
	 * that the database should wait to obtain a pessimistic lock before
	 * returning an error to the client.
	 * <p>
	 * {@link #NO_WAIT}, {@link #WAIT_FOREVER}, or {@link #SKIP_LOCKED}
	 * represent 3 "magic" values.
	 *
	 * @param timeout the new timeout setting, in milliseconds
	 * @return {@code this} for method chaining
	 *
	 * @see #getTimeOut
	 */
	public LockOptions setTimeOut(int timeout) {
		this.timeout = timeout;
		return this;
	}

	/**
	 * The current lock scope:
	 * <ul>
	 * <li>{@link PessimisticLockScope#EXTENDED} means the lock
	 *     extends to rows of owned collections, but
	 * <li>{@link PessimisticLockScope#NORMAL} means only the entity
	 *     table and secondary tables are locked.
	 * </ul>
	 *
	 * @return the current {@link PessimisticLockScope}
	 */
	public PessimisticLockScope getLockScope() {
		return scope ? PessimisticLockScope.EXTENDED : PessimisticLockScope.NORMAL;
	}

	/**
	 * Set the lock scope:
	 * <ul>
	 * <li>{@link PessimisticLockScope#EXTENDED} means the lock
	 *     extends to rows of owned collections, but
	 * <li>{@link PessimisticLockScope#NORMAL} means only the entity
	 *     table and secondary tables are locked.
	 * </ul>
	 *
	 * @param scope the new {@link PessimisticLockScope}
	 * @return {@code this} for method chaining
	 */
	public LockOptions setLockScope(PessimisticLockScope scope) {
		return setScope(scope==PessimisticLockScope.EXTENDED);
	}

	/**
	 * The current lock scope setting:
	 * <ul>
	 * <li>{@code true} means the lock extends to rows of owned
	 *     collections, but
	 * <li>{@code false} means only the entity table and secondary
	 *     tables are locked.
	 * </ul>
	 *
	 * @return {@code true} if the lock extends to owned associations
	 *
	 * @deprecated use {@link #getLockScope()}
	 */
	@Deprecated(since = "6.2")
	public boolean getScope() {
		return scope;
	}

	/**
	 * Set the lock scope setting:
	 * <ul>
	 * <li>{@code true} means the lock extends to rows of owned
	 *     collections, but
	 * <li>{@code false} means only the entity table and secondary
	 *     tables are locked.
	 * </ul>
	 *
	 * @param scope the new scope setting
	 * @return {@code this} for method chaining
	 *
	 * @deprecated use {@link #setLockScope(PessimisticLockScope)}
	 */
	@Deprecated(since = "6.2")
	public LockOptions setScope(boolean scope) {
		this.scope = scope;
		return this;
	}

	/**
	 * The current follow-on locking setting.
	 *
	 * @return {@code true} if follow-on locking is enabled
	 *
	 * @see org.hibernate.dialect.Dialect#useFollowOnLocking(String, QueryOptions)
	 */
	public Boolean getFollowOnLocking() {
		return followOnLocking;
	}

	/**
	 * Set the follow-on locking setting.
	 *
	 * @param followOnLocking The new follow-on locking setting
	 * @return {@code this} for method chaining
	 *
	 * @see org.hibernate.dialect.Dialect#useFollowOnLocking(String, QueryOptions)
	 */
	public LockOptions setFollowOnLocking(Boolean followOnLocking) {
		this.followOnLocking = followOnLocking;
		return this;
	}

	/**
	 * Make a copy.
	 *
	 * @return The copy
	 */
	public LockOptions makeCopy() {
		final LockOptions copy = new LockOptions();
		copy( this, copy );
		return copy;
	}

	public void overlay(LockOptions lockOptions) {
		copy( lockOptions, this );
	}

	/**
	 * Copy the options in the first given instance of
	 * {@code LockOptions} to the second given instance.
	 *
	 * @param source Source for the copy (copied from)
	 * @param destination Destination for the copy (copied to)
	 *
	 * @return destination
	 */
	public static LockOptions copy(LockOptions source, LockOptions destination) {
		destination.setLockMode( source.getLockMode() );
		destination.setScope( source.getScope() );
		destination.setTimeOut( source.getTimeOut() );
		if ( source.aliasSpecificLockModes != null ) {
			destination.aliasSpecificLockModes = new HashMap<>( source.aliasSpecificLockModes );
		}
		destination.setFollowOnLocking( source.getFollowOnLocking() );
		return destination;
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		else if ( !(object instanceof LockOptions) ) {
			return false;
		}
		else {
			final LockOptions that = (LockOptions) object;
			return timeout == that.timeout
				&& scope == that.scope
				&& lockMode == that.lockMode
				&& Objects.equals( aliasSpecificLockModes, that.aliasSpecificLockModes )
				&& Objects.equals( followOnLocking, that.followOnLocking );
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash( lockMode, timeout, aliasSpecificLockModes, followOnLocking, scope );
	}
}
