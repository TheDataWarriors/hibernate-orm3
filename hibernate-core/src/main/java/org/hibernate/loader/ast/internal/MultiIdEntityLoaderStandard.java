/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.loader.ast.internal.CacheEntityLoaderHelper.PersistenceContextEntry;
import org.hibernate.loader.ast.spi.MultiIdLoadOptions;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import org.hibernate.type.descriptor.java.JavaType;
import org.jboss.logging.Logger;

import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static org.hibernate.loader.ast.internal.CacheEntityLoaderHelper.loadFromSessionCacheStatic;

/**
 * Standard MultiIdEntityLoader
 *
 * @author Steve Ebersole
 */
public class MultiIdEntityLoaderStandard<T> extends AbstractMultiIdEntityLoader<T> {
	private static final Logger log = Logger.getLogger( MultiIdEntityLoaderStandard.class );

	private final int idJdbcTypeCount;

	public MultiIdEntityLoaderStandard(
			EntityPersister entityDescriptor,
			int idColumnSpan,
			SessionFactoryImplementor sessionFactory) {
		super( entityDescriptor, sessionFactory );
		this.idJdbcTypeCount = idColumnSpan;

		assert idJdbcTypeCount > 0;
	}

	@Override
	protected void handleResults(
			MultiIdLoadOptions loadOptions,
			EventSource session,
			List<Integer> elementPositionsLoadedByBatch,
			List<Object> result) {
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		for ( Integer position : elementPositionsLoadedByBatch ) {
			// the element value at this position in the result List should be
			// the EntityKey for that entity; reuse it!
			final EntityKey entityKey = (EntityKey) result.get( position );
			Object entity = persistenceContext.getEntity( entityKey );
			if ( entity != null && !loadOptions.isReturnOfDeletedEntitiesEnabled() ) {
				// make sure it is not DELETED
				final EntityEntry entry = persistenceContext.getEntry( entity );
				if ( entry.getStatus().isDeletedOrGone() ) {
					// the entity is locally deleted, and the options ask that we not return such entities...
					entity = null;
				}
				else {
					entity = persistenceContext.proxyFor( entity );
				}
			}
			result.set( position, entity );
		}
	}

	@Override
	protected int maxBatchSize(Object[] ids, MultiIdLoadOptions loadOptions) {
		if ( loadOptions.getBatchSize() != null && loadOptions.getBatchSize() > 0 ) {
			return loadOptions.getBatchSize();
		}
		else {
			return getSessionFactory().getJdbcServices().getJdbcEnvironment().getDialect()
					.getBatchLoadSizingStrategy().determineOptimalBatchLoadSize(
							idJdbcTypeCount,
							ids.length,
							getSessionFactory().getSessionFactoryOptions().inClauseParameterPaddingEnabled()
					);
		}
	}

	@Override
	protected void loadEntitiesById(
			List<Object> idsInBatch,
			LockOptions lockOptions,
			MultiIdLoadOptions loadOptions,
			EventSource session) {
		assert idsInBatch != null;
		assert !idsInBatch.isEmpty();

		listEntitiesById( idsInBatch, lockOptions, loadOptions, session );
	}

	private List<T> listEntitiesById(
			List<Object> idsInBatch,
			LockOptions lockOptions,
			MultiIdLoadOptions loadOptions,
			EventSource session) {
		final int numberOfIdsInBatch = idsInBatch.size();
		if ( numberOfIdsInBatch == 1 ) {
			return performSingleMultiLoad( idsInBatch.get( 0 ), lockOptions, session );
		}
		else {
			if ( log.isTraceEnabled() ) {
				log.tracef( "#loadEntitiesById(`%s`, `%s`, ..)", getLoadable().getEntityName(), numberOfIdsInBatch );
			}

			final JdbcParametersList.Builder jdbcParametersBuilder =
					JdbcParametersList.newBuilder( numberOfIdsInBatch * idJdbcTypeCount );

			final SelectStatement sqlAst = LoaderSelectBuilder.createSelect(
					getLoadable(),
					// null here means to select everything
					null,
					getLoadable().getIdentifierMapping(),
					null,
					numberOfIdsInBatch,
					session.getLoadQueryInfluencers(),
					lockOptions,
					jdbcParametersBuilder::add,
					getSessionFactory()
			);
			JdbcParametersList jdbcParameters = jdbcParametersBuilder.build();

			final SqlAstTranslatorFactory sqlAstTranslatorFactory =
					getSessionFactory().getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory();

			final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
			int offset = 0;

			for ( int i = 0; i < numberOfIdsInBatch; i++ ) {
				final Object id = idsInBatch.get( i );

				offset += jdbcParameterBindings.registerParametersForEachJdbcValue(
						id,
						offset,
						getLoadable().getIdentifierMapping(),
						jdbcParameters,
						session
				);
			}

			// we should have used all the JdbcParameter references (created bindings for all)
			assert offset == jdbcParameters.size();
			final JdbcOperationQuerySelect jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( getSessionFactory(), sqlAst )
					.translate( jdbcParameterBindings, QueryOptions.NONE );

			final SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler;
			if ( session.getLoadQueryInfluencers().hasSubselectLoadableCollections( getLoadable().getEntityPersister() ) ) {
				subSelectFetchableKeysHandler = SubselectFetch.createRegistrationHandler(
						session.getPersistenceContext().getBatchFetchQueue(),
						sqlAst,
						jdbcParameters,
						jdbcParameterBindings
				);
			}
			else {
				subSelectFetchableKeysHandler = null;
			}

			return session.getJdbcServices().getJdbcSelectExecutor().list(
					jdbcSelect,
					jdbcParameterBindings,
					new ExecutionContextWithSubselectFetchHandler( session,
							subSelectFetchableKeysHandler,
							TRUE.equals( loadOptions.getReadOnly( session ) ) ),
					RowTransformerStandardImpl.instance(),
					null,
					ListResultsConsumer.UniqueSemantic.FILTER,
					idsInBatch.size()
			);
		}
	}

	private List<T> performSingleMultiLoad(Object id, LockOptions lockOptions, SharedSessionContractImplementor session) {
		//noinspection unchecked
		T loaded = (T) getLoadable().getEntityPersister().load( id, null, lockOptions, session );
		return Collections.singletonList( loaded );
	}

	@Override
	protected List<T> performUnorderedMultiLoad(
			Object[] ids,
			MultiIdLoadOptions loadOptions,
			EventSource session) {
		assert !loadOptions.isOrderReturnEnabled();
		assert ids != null;

		if ( log.isTraceEnabled() ) {
			log.tracef( "#performUnorderedMultiLoad(`%s`, ..)", getLoadable().getEntityName() );
		}

		final List<T> result = CollectionHelper.arrayList( ids.length );

		final LockOptions lockOptions = loadOptions.getLockOptions() == null
				? new LockOptions( LockMode.NONE )
				: loadOptions.getLockOptions();

		if ( loadOptions.isSessionCheckingEnabled() || loadOptions.isSecondLevelCacheCheckingEnabled() ) {
			// the user requested that we exclude ids corresponding to already managed
			// entities from the generated load SQL.  So here we will iterate all
			// incoming id values and see whether it corresponds to an existing
			// entity associated with the PC - if it does we add it to the result
			// list immediately and remove its id from the group of ids to load.
			boolean foundAnyManagedEntities = false;
			final List<Object> nonManagedIds = new ArrayList<>();

			final boolean coerce = !getSessionFactory().getJpaMetamodel().getJpaCompliance().isLoadByIdComplianceEnabled();
			final JavaType<?> idType = getLoadable().getIdentifierMapping().getJavaType();

			for ( int i = 0; i < ids.length; i++ ) {
				final Object id = coerce ? idType.coerce( ids[i], session ) : ids[i];
				final EntityKey entityKey = new EntityKey( id, getLoadable().getEntityPersister() );

				final LoadEvent loadEvent = new LoadEvent(
						id,
						getLoadable().getJavaType().getJavaTypeClass().getName(),
						lockOptions,
						session,
						LoaderHelper.getReadOnlyFromLoadQueryInfluencers( session )
				);

				Object managedEntity = null;

				// look for it in the Session first
				final PersistenceContextEntry persistenceContextEntry =
						loadFromSessionCacheStatic( loadEvent, entityKey, LoadEventListener.GET );
				if ( loadOptions.isSessionCheckingEnabled() ) {
					managedEntity = persistenceContextEntry.getEntity();

					if ( managedEntity != null
							&& !loadOptions.isReturnOfDeletedEntitiesEnabled()
							&& !persistenceContextEntry.isManaged() ) {
						foundAnyManagedEntities = true;
						result.add( null );
						continue;
					}
				}

				if ( managedEntity == null && loadOptions.isSecondLevelCacheCheckingEnabled() ) {
					managedEntity = CacheEntityLoaderHelper.INSTANCE.loadFromSecondLevelCache(
							loadEvent,
							getLoadable().getEntityPersister(),
							entityKey
					);
				}

				if ( managedEntity != null ) {
					foundAnyManagedEntities = true;
					//noinspection unchecked
					result.add( (T) managedEntity );
				}
				else {
					nonManagedIds.add( id );
				}
			}

			if ( foundAnyManagedEntities ) {
				if ( nonManagedIds.isEmpty() ) {
					// all the given ids were already associated with the Session
					return result;
				}
				else {
					// over-write the ids to be loaded with the collection of
					// just non-managed ones
					ids = nonManagedIds.toArray(
							(Object[]) Array.newInstance(
									ids.getClass().getComponentType(),
									nonManagedIds.size()
							)
					);
				}
			}
		}

		int numberOfIdsLeft = ids.length;
		final int maxBatchSize;
		if ( loadOptions.getBatchSize() != null && loadOptions.getBatchSize() > 0 ) {
			maxBatchSize = loadOptions.getBatchSize();
		}
		else {
			maxBatchSize = session.getJdbcServices().getJdbcEnvironment().getDialect().getBatchLoadSizingStrategy().determineOptimalBatchLoadSize(
					getIdentifierMapping().getJdbcTypeCount(),
					numberOfIdsLeft,
					getSessionFactory().getSessionFactoryOptions().inClauseParameterPaddingEnabled()
			);
		}

		int idPosition = 0;
		while ( numberOfIdsLeft > 0 ) {
			final int batchSize =  Math.min( numberOfIdsLeft, maxBatchSize );

			final Object[] idsInBatch = new Object[ batchSize ];
			System.arraycopy( ids, idPosition, idsInBatch, 0, batchSize );

			result.addAll( listEntitiesById( asList( idsInBatch ), lockOptions, loadOptions, session ) );

			numberOfIdsLeft = numberOfIdsLeft - batchSize;
			idPosition += batchSize;
		}

		return result;
	}

}
