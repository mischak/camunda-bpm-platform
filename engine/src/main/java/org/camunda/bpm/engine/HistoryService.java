/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.camunda.bpm.engine;

import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricActivityInstanceQuery;
import org.camunda.bpm.engine.history.HistoricActivityStatisticsQuery;
import org.camunda.bpm.engine.history.HistoricDetail;
import org.camunda.bpm.engine.history.HistoricDetailQuery;
import org.camunda.bpm.engine.history.HistoricIncident;
import org.camunda.bpm.engine.history.HistoricIncidentQuery;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstanceQuery;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.history.HistoricTaskInstanceQuery;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstanceQuery;
import org.camunda.bpm.engine.history.NativeHistoricActivityInstanceQuery;
import org.camunda.bpm.engine.history.NativeHistoricProcessInstanceQuery;
import org.camunda.bpm.engine.history.NativeHistoricTaskInstanceQuery;
import org.camunda.bpm.engine.history.UserOperationLogEntry;
import org.camunda.bpm.engine.history.UserOperationLogQuery;

/**
 * Service exposing information about ongoing and past process instances.  This is different
 * from the runtime information in the sense that this runtime information only contains
 * the actual runtime state at any given moment and it is optimized for runtime
 * process execution performance.  The history information is optimized for easy
 * querying and remains permanent in the persistent storage.
 *
 * @author Christian Stettler
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public interface HistoryService {

  /** Creates a new programmatic query to search for {@link HistoricProcessInstance}s. */
  HistoricProcessInstanceQuery createHistoricProcessInstanceQuery();

  /** Creates a new programmatic query to search for {@link HistoricActivityInstance}s. */
  HistoricActivityInstanceQuery createHistoricActivityInstanceQuery();

  /**
   * Query for the number of historic activity instances aggregated by activities of a single process definition.
   */
  HistoricActivityStatisticsQuery createHistoricActivityStatisticsQuery(String processDefinitionId);

  /** Creates a new programmatic query to search for {@link HistoricTaskInstance}s. */
  HistoricTaskInstanceQuery createHistoricTaskInstanceQuery();

  /** Creates a new programmatic query to search for {@link HistoricDetail}s. */
  HistoricDetailQuery createHistoricDetailQuery();

  /** Creates a new programmatic query to search for {@link HistoricVariableInstance}s. */
  HistoricVariableInstanceQuery createHistoricVariableInstanceQuery();

  /** Creates a new programmatic query to search for {@link UserOperationLogEntry} instances. */
  UserOperationLogQuery createUserOperationLogQuery();

  /** Creates a new programmatic query to search for {@link HistoricIncident historic incidents}. */
  HistoricIncidentQuery createHistoricIncidentQuery();

  /** Deletes historic task instance.  This might be useful for tasks that are
   * {@link TaskService#newTask() dynamically created} and then {@link TaskService#complete(String) completed}.
   * If the historic task instance doesn't exist, no exception is thrown and the
   * method returns normal.*/
  void deleteHistoricTaskInstance(String taskId);

  /**
   * Deletes historic process instance. All historic activities, historic task and
   * historic details (variable updates, form properties) are deleted as well.
   */
  void deleteHistoricProcessInstance(String processInstanceId);

  /**
   * Deletes a user operation log entry. Does not cascade to any related entities.
   */
  void deleteUserOperationLogEntry(String entryId);

  /**
   * creates a native query to search for {@link HistoricProcessInstance}s via SQL
   */
  NativeHistoricProcessInstanceQuery createNativeHistoricProcessInstanceQuery();

  /**
   * creates a native query to search for {@link HistoricTaskInstance}s via SQL
   */
  NativeHistoricTaskInstanceQuery createNativeHistoricTaskInstanceQuery();

  /**
   * creates a native query to search for {@link HistoricActivityInstance}s via SQL
   */
  NativeHistoricActivityInstanceQuery createNativeHistoricActivityInstanceQuery();

}
