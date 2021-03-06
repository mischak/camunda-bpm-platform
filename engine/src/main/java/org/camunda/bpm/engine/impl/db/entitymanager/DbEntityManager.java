/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.engine.impl.db.entitymanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.*;
import org.camunda.bpm.engine.impl.cfg.IdGenerator;
import org.camunda.bpm.engine.impl.cmmn.entity.repository.CaseDefinitionQueryImpl;
import org.camunda.bpm.engine.impl.db.DbEntity;
import org.camunda.bpm.engine.impl.db.ListQueryParameterObject;
import org.camunda.bpm.engine.impl.db.PersistenceSession;
import org.camunda.bpm.engine.impl.db.entitymanager.cache.CachedDbEntity;
import org.camunda.bpm.engine.impl.db.entitymanager.cache.DbEntityCache;
import org.camunda.bpm.engine.impl.db.entitymanager.cache.DbEntityState;
import org.camunda.bpm.engine.impl.db.entitymanager.operation.*;
import org.camunda.bpm.engine.impl.identity.db.DbGroupQueryImpl;
import org.camunda.bpm.engine.impl.identity.db.DbUserQueryImpl;
import org.camunda.bpm.engine.impl.interceptor.Session;

import static org.camunda.bpm.engine.impl.db.entitymanager.cache.DbEntityState.*;
import static org.camunda.bpm.engine.impl.db.entitymanager.operation.DbOperationType.*;

/**
 *
 * @author Daniel Meyer
 *
 */
@SuppressWarnings({ "rawtypes" })
public class DbEntityManager implements Session {

  protected Logger log = Logger.getLogger(DbEntityManager.class.getName());

  protected IdGenerator idGenerator;

  protected DbEntityCache dbEntityCache;

  protected DbOperationManager dbOperationManager;

  protected PersistenceSession persistenceSession;

  public DbEntityManager(IdGenerator idGenerator, PersistenceSession persistenceSession) {
    this.idGenerator = idGenerator;
    this.persistenceSession = persistenceSession;
    dbEntityCache = new DbEntityCache();
    dbOperationManager = new DbOperationManager();
  }

  // selects /////////////////////////////////////////////////

  public List selectList(String statement) {
    return selectList(statement, null, 0, Integer.MAX_VALUE);
  }

  public List selectList(String statement, Object parameter) {
    return selectList(statement, parameter, 0, Integer.MAX_VALUE);
  }

  public List selectList(String statement, Object parameter, Page page) {
    if(page!=null) {
      return selectList(statement, parameter, page.getFirstResult(), page.getMaxResults());
    }else {
      return selectList(statement, parameter, 0, Integer.MAX_VALUE);
    }
  }

  public List selectList(String statement, ListQueryParameterObject parameter, Page page) {
    return selectList(statement, parameter);
  }

  public List selectList(String statement, Object parameter, int firstResult, int maxResults) {
    return selectList(statement, new ListQueryParameterObject(parameter, firstResult, maxResults));
  }

  public List selectList(String statement, ListQueryParameterObject parameter) {
    return selectListWithRawParameter(statement, parameter, parameter.getFirstResult(), parameter.getMaxResults());
  }

  @SuppressWarnings("unchecked")
  public List selectListWithRawParameter(String statement, Object parameter, int firstResult, int maxResults) {
    if(firstResult == -1 ||  maxResults==-1) {
      return Collections.EMPTY_LIST;
    }
    List loadedObjects = persistenceSession.selectList(statement, parameter);
    return filterLoadedObjects(loadedObjects);
  }

  public Object selectOne(String statement, Object parameter) {
    Object result = persistenceSession.selectOne(statement, parameter);
    if (result instanceof DbEntity) {
      DbEntity loadedObject = (DbEntity) result;
      result = cacheFilter(loadedObject);
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  public boolean selectBoolean(String statement, Object parameter) {
    List<String> result = (List<String>) persistenceSession.selectList(statement, parameter);
    if(result != null) {
      return result.contains(1);
    }
    return false;

  }

  public <T extends DbEntity> T selectById(Class<T> entityClass, String id) {
    T persistentObject = dbEntityCache.get(entityClass, id);
    if (persistentObject!=null) {
      return persistentObject;
    }
    persistentObject = persistenceSession.selectById(entityClass, id);

    if (persistentObject==null) {
      return null;
    }
    dbEntityCache.putPersistent(persistentObject);
    return persistentObject;
  }

  public <T extends DbEntity> T getCachedEntity(Class<T> type, String id) {
    return dbEntityCache.get(type, id);
  }

  public <T extends DbEntity> List<T> getCachedEntitiesByType(Class<T> type) {
    return dbEntityCache.getEntitiesByType(type);
  }

  protected List filterLoadedObjects(List<Object> loadedObjects) {
    if (loadedObjects.isEmpty()) {
      return loadedObjects;
    }
    if (! (DbEntity.class.isAssignableFrom(loadedObjects.get(0).getClass()))) {
      return loadedObjects;
    }
    List<DbEntity> filteredObjects = new ArrayList<DbEntity>(loadedObjects.size());
    for (Object loadedObject: loadedObjects) {
      DbEntity cachedPersistentObject = cacheFilter((DbEntity) loadedObject);
      filteredObjects.add(cachedPersistentObject);
    }
    return filteredObjects;
  }

  /** returns the object in the cache.  if this object was loaded before,
   * then the original object is returned.  if this is the first time
   * this object is loaded, then the loadedObject is added to the cache. */
  protected DbEntity cacheFilter(DbEntity persistentObject) {
    DbEntity cachedPersistentObject = dbEntityCache.get(persistentObject.getClass(), persistentObject.getId());
    if (cachedPersistentObject!=null) {
      return cachedPersistentObject;
    }
    dbEntityCache.putPersistent(persistentObject);
    return persistentObject;
  }

  public void lock(String statement) {
    persistenceSession.lock(statement);
  }

  public boolean isDirty(DbEntity dbEntity) {
    CachedDbEntity cachedEntity = dbEntityCache.getCachedEntity(dbEntity);
    if(cachedEntity == null) {
      return false;
    } else {
      return cachedEntity.isDirty() || cachedEntity.getEntityState() == DbEntityState.MERGED;
    }
  }

  public void flush() {

    // flush the entity cache
    flushEntityCache();

    // obtain totally ordered operation list from operation manager
    List<DbOperation> operationsToFlush = dbOperationManager.calculateFlush();
    logFlushSummary(operationsToFlush);

    // execute the flush
    for (DbOperation dbOperation : operationsToFlush) {
      persistenceSession.executeDbOperation(dbOperation);
    }

  }

  /**
   * Flushes the entity cache:
   * Depending on the entity state, the required {@link DbOperation} is performed and the cache is updated.
   */
  protected void flushEntityCache() {
    List<CachedDbEntity> cachedEntities = dbEntityCache.getCachedEntities();
    for (CachedDbEntity cachedDbEntity : cachedEntities) {

      if(cachedDbEntity.getEntityState() == TRANSIENT) {
        // perform INSERT
        performEntityOperation(cachedDbEntity, INSERT);
        // mark PERSISTENT
        cachedDbEntity.setEntityState(PERSISTENT);

      } else if(cachedDbEntity.isDirty()) {
        // object is dirty -> perform UPDATE
        performEntityOperation(cachedDbEntity, UPDATE);

      } else if(cachedDbEntity.getEntityState() == MERGED) {
        // perform UPDATE
        performEntityOperation(cachedDbEntity, UPDATE);
        // mark PERSISTENT
        cachedDbEntity.setEntityState(PERSISTENT);

      } else if(cachedDbEntity.getEntityState() == DELETED_TRANSIENT) {
        // remove from cache
        dbEntityCache.remove(cachedDbEntity);

      } else if(cachedDbEntity.getEntityState() == DELETED_PERSISTENT
             || cachedDbEntity.getEntityState() == DELETED_MERGED) {
        // perform DELETE
        performEntityOperation(cachedDbEntity, DELETE);
        // remove from cache
        dbEntityCache.remove(cachedDbEntity);

      }

      // if object is PERSISTENT after flush
      if(cachedDbEntity.getEntityState() == PERSISTENT) {
        // make a new copy
        cachedDbEntity.makeCopy();
      }
    }
  }

  public void insert(DbEntity dbEntity) {
    // generate Id if not present
    ensureHasId(dbEntity);

    // put into cache
    dbEntityCache.putTransient(dbEntity);

  }

  public void merge(DbEntity dbEntity) {

    if(dbEntity.getId() == null) {
      throw new ProcessEngineException("Cannot merge dbEntity without id" + dbEntity);
    }

    // NOTE: a proper implementation of merge() would fetch the entity from the database
    // and merge the state changes. For now, we simply always perform an update.
    // Supposedly, the "proper" implementation would reduce the number of situations where
    // optimistic locking results in a conflict.

    dbEntityCache.putMerged(dbEntity);
  }

  public void delete(DbEntity dbEntity) {
    dbEntityCache.setDeleted(dbEntity);
  }

  public void update(Class<? extends DbEntity> entityType, String statement, Object parameter) {
    performBulkOperation(entityType, statement, parameter, UPDATE_BULK);
  }

  public void delete(Class<? extends DbEntity> entityType, String statement, Object parameter) {
    performBulkOperation(entityType, statement, parameter, DELETE_BULK);
  }

  protected DbBulkOperation performBulkOperation(Class<? extends DbEntity> entityType, String statement, Object parameter, DbOperationType operationType) {
    // create operation
    DbBulkOperation bulkOperation = new DbBulkOperation();

    // configure operation
    bulkOperation.setOperationType(operationType);
    bulkOperation.setEntityType(entityType);
    bulkOperation.setStatement(statement);
    bulkOperation.setParameter(parameter);

    // schedule operation
    dbOperationManager.addOperation(bulkOperation);
    return bulkOperation;
  }

  protected void performEntityOperation(CachedDbEntity cachedDbEntity, DbOperationType type) {
    DbEntityOperation dbOperation = new DbEntityOperation();
    dbOperation.setEntity(cachedDbEntity.getEntity());
    dbOperation.setOperationType(type);
    dbOperationManager.addOperation(dbOperation);
  }

  protected void logFlushSummary(Collection<DbOperation> operations) {
    log.fine("Flush Summary:");
    for (DbOperation dbOperation : operations) {
      log.fine("  " + dbOperation);
    }
  }

  public void close() {

  }

  public boolean isDeleted(DbEntity object) {
    return dbEntityCache.isDeleted(object);
  }

  protected void ensureHasId(DbEntity dbEntity) {
    if(dbEntity.getId() == null) {
      String nextId = idGenerator.getNextId();
      dbEntity.setId(nextId);
    }
  }

  public <T extends DbEntity> List<T> pruneDeletedEntities(List<T> listToPrune) {
    ArrayList<T> prunedList = new ArrayList<T>();
    for (T potentiallyDeleted : listToPrune) {
      if(!isDeleted(potentiallyDeleted)) {
        prunedList.add(potentiallyDeleted);
      }
    }
    return prunedList;
  }

  // getters / setters /////////////////////////////////

  public DbOperationManager getDbOperationManager() {
    return dbOperationManager;
  }

  public void setDbOperationManager(DbOperationManager operationManager) {
    this.dbOperationManager = operationManager;
  }

  public DbEntityCache getDbEntityCache() {
    return dbEntityCache;
  }

  public void setDbEntityCache(DbEntityCache dbEntityCache) {
    this.dbEntityCache = dbEntityCache;
  }

  // query factory methods ////////////////////////////////////////////////////

  public DeploymentQueryImpl createDeploymentQuery() {
    return new DeploymentQueryImpl();
  }

  public ProcessDefinitionQueryImpl createProcessDefinitionQuery() {
    return new ProcessDefinitionQueryImpl();
  }

  public CaseDefinitionQueryImpl createCaseDefinitionQuery() {
    return new CaseDefinitionQueryImpl();
  }

  public ProcessInstanceQueryImpl createProcessInstanceQuery() {
    return new ProcessInstanceQueryImpl();
  }

  public ExecutionQueryImpl createExecutionQuery() {
    return new ExecutionQueryImpl();
  }

  public TaskQueryImpl createTaskQuery() {
    return new TaskQueryImpl();
  }

  public JobQueryImpl createJobQuery() {
    return new JobQueryImpl();
  }

  public HistoricProcessInstanceQueryImpl createHistoricProcessInstanceQuery() {
    return new HistoricProcessInstanceQueryImpl();
  }

  public HistoricActivityInstanceQueryImpl createHistoricActivityInstanceQuery() {
    return new HistoricActivityInstanceQueryImpl();
  }

  public HistoricTaskInstanceQueryImpl createHistoricTaskInstanceQuery() {
    return new HistoricTaskInstanceQueryImpl();
  }

  public HistoricDetailQueryImpl createHistoricDetailQuery() {
    return new HistoricDetailQueryImpl();
  }

  public HistoricVariableInstanceQueryImpl createHistoricVariableInstanceQuery() {
    return new HistoricVariableInstanceQueryImpl();
  }

  public UserQueryImpl createUserQuery() {
    return new DbUserQueryImpl();
  }

  public GroupQueryImpl createGroupQuery() {
    return new DbGroupQueryImpl();
  }
}
