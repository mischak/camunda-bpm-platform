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
package org.camunda.bpm.engine.test.cmmn.milestone;

import org.camunda.bpm.engine.impl.cmmn.execution.CmmnExecution;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.camunda.bpm.engine.runtime.CaseExecution;
import org.camunda.bpm.engine.runtime.CaseInstance;
import org.camunda.bpm.engine.test.Deployment;

/**
 * @author Roman Smirnov
 *
 */
public class MilestoneTest extends PluggableProcessEngineTestCase {

  @Deployment(resources = {"org/camunda/bpm/engine/test/cmmn/milestone/MilestoneTest.testWithoutEntryCriterias.cmmn"})
  public void testWithoutEntryCriterias() {
    // given

    // when
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    // then
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .caseInstanceId(caseInstanceId)
        .singleResult();

    assertTrue(caseInstance.isCompleted());

    Object occurVariable = caseService.getVariable(caseInstanceId, "occur");
    assertNotNull(occurVariable);
    assertTrue((Boolean) occurVariable);
  }

  @Deployment(resources = {"org/camunda/bpm/engine/test/cmmn/milestone/MilestoneTest.testWithEntryCriterias.cmmn"})
  public void testWithEntryCriterias() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    CaseExecution milestone = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Milestone_1")
        .singleResult();

    String milestoneId = milestone.getId();

    assertTrue(milestone.isAvailable());

    // when
    occur(milestoneId);

    // then
    Object occurVariable = caseService.getVariable(caseInstanceId, "occur");
    assertNotNull(occurVariable);
    assertTrue((Boolean) occurVariable);
  }

  protected void occur(final String caseExecutionId) {
    processEngineConfiguration
      .getCommandExecutorTxRequired()
      .execute(new Command<Void>() {

        @Override
        public Void execute(CommandContext commandContext) {
          CmmnExecution caseTask = (CmmnExecution) caseService
              .createCaseExecutionQuery()
              .caseExecutionId(caseExecutionId)
              .singleResult();
          caseTask.occur();
          return null;
        }

      });
  }

}
