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

package org.camunda.bpm.engine.history;

import org.camunda.bpm.engine.delegate.SerializedVariableValue;

/** Update of a process variable.  This is only available if history
 * level is configured to FULL.
 *
 * @author Tom Baeyens
 */
public interface HistoricVariableUpdate extends HistoricDetail {

  String getVariableName();
  String getVariableTypeName();
  Object getValue();
  String getValueTypeName();
  boolean storesCustomObjects();
  int getRevision();

  /**
   * If the variable value could not be loaded, this returns the error message.
   * @return an error message indicating why the variable value could not be loaded.
   */
  String getErrorMessage();

  SerializedVariableValue getSerializedValue();
}
