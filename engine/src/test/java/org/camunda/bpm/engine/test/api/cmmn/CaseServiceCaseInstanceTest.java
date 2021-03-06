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
package org.camunda.bpm.engine.test.api.cmmn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.exception.NotAllowedException;
import org.camunda.bpm.engine.exception.NotFoundException;
import org.camunda.bpm.engine.exception.NotValidException;
import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.camunda.bpm.engine.runtime.CaseExecution;
import org.camunda.bpm.engine.runtime.CaseInstance;
import org.camunda.bpm.engine.runtime.VariableInstance;
import org.camunda.bpm.engine.runtime.VariableInstanceQuery;
import org.camunda.bpm.engine.test.Deployment;

/**
 * @author Roman Smirnov
 *
 */
public class CaseServiceCaseInstanceTest extends PluggableProcessEngineTestCase {

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testCreateByKey() {
    // given a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // when
    CaseInstance caseInstance = caseService
        .withCaseDefinitionByKey("oneTaskCase")
        .create();

    // then
    assertNotNull(caseInstance);

    // check properties
    assertNull(caseInstance.getBusinessKey());
    assertEquals(caseDefinitionId, caseInstance.getCaseDefinitionId());
    assertEquals(caseInstance.getId(), caseInstance.getCaseInstanceId());
    assertTrue(caseInstance.isActive());
    assertFalse(caseInstance.isEnabled());

    // get persisted case instance
    CaseInstance instance = caseService
      .createCaseInstanceQuery()
      .singleResult();

    // should have the same properties
    assertEquals(caseInstance.getId(), instance.getId());
    assertEquals(caseInstance.getBusinessKey(), instance.getBusinessKey());
    assertEquals(caseInstance.getCaseDefinitionId(), instance.getCaseDefinitionId());
    assertEquals(caseInstance.getCaseInstanceId(), instance.getCaseInstanceId());
    assertEquals(caseInstance.isActive(), instance.isActive());
    assertEquals(caseInstance.isEnabled(), instance.isEnabled());
  }

  public void testCreateByInvalidKey() {
    try {
      caseService
          .withCaseDefinitionByKey("invalid")
          .create();
      fail();
    } catch (NotFoundException e) { }

    try {
      caseService
          .withCaseDefinitionByKey(null)
          .create();
      fail();
    } catch (NotValidException e) { }

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testCreateById() {
    // given a deployed case definition
    String caseDefinitionId = repositoryService
      .createCaseDefinitionQuery()
      .singleResult()
      .getId();

    // when
    CaseInstance caseInstance = caseService
        .withCaseDefinition(caseDefinitionId)
        .create();

    // then
    assertNotNull(caseInstance);

    // check properties
    assertNull(caseInstance.getBusinessKey());
    assertEquals(caseDefinitionId, caseInstance.getCaseDefinitionId());
    assertEquals(caseInstance.getId(), caseInstance.getCaseInstanceId());
    assertTrue(caseInstance.isActive());
    assertFalse(caseInstance.isEnabled());

    // get persistent case instance
    CaseInstance instance = caseService
      .createCaseInstanceQuery()
      .singleResult();

    // should have the same properties
    assertEquals(caseInstance.getId(), instance.getId());
    assertEquals(caseInstance.getBusinessKey(), instance.getBusinessKey());
    assertEquals(caseInstance.getCaseDefinitionId(), instance.getCaseDefinitionId());
    assertEquals(caseInstance.getCaseInstanceId(), instance.getCaseInstanceId());
    assertEquals(caseInstance.isActive(), instance.isActive());
    assertEquals(caseInstance.isEnabled(), instance.isEnabled());

  }

  public void testCreateByInvalidId() {
    try {
      caseService
          .withCaseDefinition("invalid")
          .create();
      fail();
    } catch (NotFoundException e) { }

    try {
      caseService
          .withCaseDefinition(null)
          .create();
      fail();
    } catch (NotValidException e) { }

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testCreateByKeyWithBusinessKey() {
    // given a deployed case definition
    String caseDefinitionId = repositoryService
      .createCaseDefinitionQuery()
      .singleResult()
      .getId();

    // when
    CaseInstance caseInstance = caseService
        .withCaseDefinitionByKey("oneTaskCase")
        .businessKey("aBusinessKey")
        .create();

    // then
    assertNotNull(caseInstance);

    // check properties
    assertEquals("aBusinessKey", caseInstance.getBusinessKey());
    assertEquals(caseDefinitionId, caseInstance.getCaseDefinitionId());
    assertEquals(caseInstance.getId(), caseInstance.getCaseInstanceId());
    assertTrue(caseInstance.isActive());
    assertFalse(caseInstance.isEnabled());

    // get persistend case instance
    CaseInstance instance = caseService
      .createCaseInstanceQuery()
      .singleResult();

    // should have the same properties
    assertEquals(caseInstance.getId(), instance.getId());
    assertEquals(caseInstance.getBusinessKey(), instance.getBusinessKey());
    assertEquals(caseInstance.getCaseDefinitionId(), instance.getCaseDefinitionId());
    assertEquals(caseInstance.getCaseInstanceId(), instance.getCaseInstanceId());
    assertEquals(caseInstance.isActive(), instance.isActive());
    assertEquals(caseInstance.isEnabled(), instance.isEnabled());

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testCreateByIdWithBusinessKey() {
    // given a deployed case definition
    String caseDefinitionId = repositoryService
      .createCaseDefinitionQuery()
      .singleResult()
      .getId();

    // when
    CaseInstance caseInstance = caseService
        .withCaseDefinition(caseDefinitionId)
        .businessKey("aBusinessKey")
        .create();

    // then
    assertNotNull(caseInstance);

    // check properties
    assertEquals("aBusinessKey", caseInstance.getBusinessKey());
    assertEquals(caseDefinitionId, caseInstance.getCaseDefinitionId());
    assertEquals(caseInstance.getId(), caseInstance.getCaseInstanceId());
    assertTrue(caseInstance.isActive());
    assertFalse(caseInstance.isEnabled());

    // get persistend case instance
    CaseInstance instance = caseService
      .createCaseInstanceQuery()
      .singleResult();

    // should have the same properties
    assertEquals(caseInstance.getId(), instance.getId());
    assertEquals(caseInstance.getBusinessKey(), instance.getBusinessKey());
    assertEquals(caseInstance.getCaseDefinitionId(), instance.getCaseDefinitionId());
    assertEquals(caseInstance.getCaseInstanceId(), instance.getCaseInstanceId());
    assertEquals(caseInstance.isActive(), instance.isActive());
    assertEquals(caseInstance.isEnabled(), instance.isEnabled());

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testCreateByKeyWithVariable() {
    // given a deployed case definition

    // when
    CaseInstance caseInstance = caseService
        .withCaseDefinitionByKey("oneTaskCase")
        .setVariable("aVariableName", "aVariableValue")
        .setVariable("anotherVariableName", 999)
        .create();

    // then
    assertNotNull(caseInstance);

    // there should exist two variables
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    List<VariableInstance> result = query
      .caseInstanceIdIn(caseInstance.getId())
      .orderByVariableName()
      .asc()
      .list();

    assertFalse(result.isEmpty());
    assertEquals(2, result.size());

    for (VariableInstance variableInstance : result) {
      if (variableInstance.getName().equals("aVariableName")) {
        assertEquals("aVariableName", variableInstance.getName());
        assertEquals("aVariableValue", variableInstance.getValue());
      } else if (variableInstance.getName().equals("anotherVariableName")) {
        assertEquals("anotherVariableName", variableInstance.getName());
        assertEquals(999, variableInstance.getValue());
      } else {
        fail("Unexpected variable: " + variableInstance.getName());
      }

    }

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testCreateByKeyWithVariables() {
    // given a deployed case definition
    Map<String, Object> variables = new HashMap<String, Object>();

    variables.put("aVariableName", "aVariableValue");
    variables.put("anotherVariableName", 999);

    // when
    CaseInstance caseInstance = caseService
        .withCaseDefinitionByKey("oneTaskCase")
        .setVariables(variables)
        .create();

    // then
    assertNotNull(caseInstance);

    // there should exist two variables
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    List<VariableInstance> result = query
      .caseInstanceIdIn(caseInstance.getId())
      .orderByVariableName()
      .asc()
      .list();

    assertFalse(result.isEmpty());
    assertEquals(2, result.size());

    for (VariableInstance variableInstance : result) {
      if (variableInstance.getName().equals("aVariableName")) {
        assertEquals("aVariableName", variableInstance.getName());
        assertEquals("aVariableValue", variableInstance.getValue());
      } else if (variableInstance.getName().equals("anotherVariableName")) {
        assertEquals("anotherVariableName", variableInstance.getName());
        assertEquals(999, variableInstance.getValue());
      } else {
        fail("Unexpected variable: " + variableInstance.getName());
      }

    }

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testCreateByIdWithVariable() {
    // given a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // when
    CaseInstance caseInstance = caseService
        .withCaseDefinition(caseDefinitionId)
        .setVariable("aVariableName", "aVariableValue")
        .setVariable("anotherVariableName", 999)
        .create();

    // then
    assertNotNull(caseInstance);

    // there should exist two variables
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    List<VariableInstance> result = query
      .caseInstanceIdIn(caseInstance.getId())
      .orderByVariableName()
      .asc()
      .list();

    assertFalse(result.isEmpty());
    assertEquals(2, result.size());

    for (VariableInstance variableInstance : result) {
      if (variableInstance.getName().equals("aVariableName")) {
        assertEquals("aVariableName", variableInstance.getName());
        assertEquals("aVariableValue", variableInstance.getValue());
      } else if (variableInstance.getName().equals("anotherVariableName")) {
        assertEquals("anotherVariableName", variableInstance.getName());
        assertEquals(999, variableInstance.getValue());
      } else {
        fail("Unexpected variable: " + variableInstance.getName());
      }

    }

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testCreateByIdWithVariables() {
    // given a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    Map<String, Object> variables = new HashMap<String, Object>();

    variables.put("aVariableName", "aVariableValue");
    variables.put("anotherVariableName", 999);

    // when
    CaseInstance caseInstance = caseService
        .withCaseDefinition(caseDefinitionId)
        .setVariables(variables)
        .create();

    // then
    assertNotNull(caseInstance);

    // there should exist two variables
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    List<VariableInstance> result = query
      .caseInstanceIdIn(caseInstance.getId())
      .orderByVariableName()
      .asc()
      .list();

    assertFalse(result.isEmpty());
    assertEquals(2, result.size());

    for (VariableInstance variableInstance : result) {
      if (variableInstance.getName().equals("aVariableName")) {
        assertEquals("aVariableName", variableInstance.getName());
        assertEquals("aVariableValue", variableInstance.getValue());
      } else if (variableInstance.getName().equals("anotherVariableName")) {
        assertEquals("anotherVariableName", variableInstance.getName());
        assertEquals(999, variableInstance.getValue());
      } else {
        fail("Unexpected variable: " + variableInstance.getName());
      }

    }

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testManualStart() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    String caseInstanceId = caseService
        .withCaseDefinition(caseDefinitionId)
        .create()
        .getId();

    // when
    try {
      caseService
        .withCaseExecution(caseInstanceId)
        .manualStart();
      fail("It should not be possible to start a case instance manually.");
    } catch (NotAllowedException e) {
    }

  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testDisable() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    String caseInstanceId = caseService
        .withCaseDefinition(caseDefinitionId)
        .create()
        .getId();

    // when
    try {
      caseService
        .withCaseExecution(caseInstanceId)
        .disable();
      fail("It should not be possible to disable a case instance.");
    } catch (NotAllowedException e) {
    }
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testReenable() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    String caseInstanceId = caseService
        .withCaseDefinition(caseDefinitionId)
        .create()
        .getId();

    // when
    try {
      caseService
        .withCaseExecution(caseInstanceId)
        .reenable();
      fail("It should not be possible to re-enable a case instance.");
    } catch (NotAllowedException e) {
    }
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testCompleteWithEnabledTask() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    String caseInstanceId = caseService
       .withCaseDefinition(caseDefinitionId)
       .create()
       .getId();

    // when

    caseService
      .withCaseExecution(caseInstanceId)
      .complete();

    // then

    // the corresponding case execution has been also
    // deleted and completed
    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertNull(caseExecution);

    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .singleResult();

    assertNotNull(caseInstance);
    assertTrue(caseInstance.isCompleted());
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneStageCase.cmmn"})
  public void testCompleteWithEnabledStage() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    String caseInstanceId = caseService
       .withCaseDefinition(caseDefinitionId)
       .create()
       .getId();

    // when

    caseService
      .withCaseExecution(caseInstanceId)
      .complete();

    // then

    // the corresponding case execution has been also
    // deleted and completed
    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Stage_1")
        .singleResult();

    assertNull(caseExecution);

    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .singleResult();

    assertNotNull(caseInstance);
    assertTrue(caseInstance.isCompleted());
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testCompleteWithActiveTask() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    String caseInstanceId = caseService
       .withCaseDefinition(caseDefinitionId)
       .create()
       .getId();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    caseService
      .withCaseExecution(caseExecutionId)
      .manualStart();

    // when

    try {
      caseService
        .withCaseExecution(caseInstanceId)
        .complete();
      fail("It should not be possible to complete a case instance containing an active task.");
    } catch (ProcessEngineException e) {}

    // then

    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertNotNull(caseExecution);
    assertTrue(caseExecution.isActive());

    // the case instance is still active
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .singleResult();

    assertNotNull(caseInstance);
    assertTrue(caseInstance.isActive());
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneStageCase.cmmn"})
  public void testCompleteWithActiveStage() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    String caseInstanceId = caseService
       .withCaseDefinition(caseDefinitionId)
       .create()
       .getId();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Stage_1")
        .singleResult()
        .getId();

    caseService
      .withCaseExecution(caseExecutionId)
      .manualStart();

    // when

    try {
      caseService
        .withCaseExecution(caseInstanceId)
        .complete();
      fail("It should not be possible to complete a case instance containing an active stage.");
    } catch (ProcessEngineException e) {}

    // then

    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Stage_1")
        .singleResult();

    assertNotNull(caseExecution);
    assertTrue(caseExecution.isActive());

    // the case instance is still active
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .singleResult();

    assertNotNull(caseInstance);
    assertTrue(caseInstance.isActive());
  }


  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/emptyCasePlanModelCase.cmmn"})
  public void testAutoCompletionOfEmptyCase() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // when
    caseService
       .withCaseDefinition(caseDefinitionId)
       .create();

    // then
    CaseInstance caseInstance = caseService
      .createCaseInstanceQuery()
      .completed()
      .singleResult();

    assertNotNull(caseInstance);
    assertTrue(caseInstance.isCompleted());
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testCloseAnActiveCaseInstance() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    String caseInstanceId = caseService
       .withCaseDefinition(caseDefinitionId)
       .create()
       .getId();

    try {
      // when
      caseService
        .withCaseExecution(caseInstanceId)
        .close();
      fail("It should not be possible to close an active case instance.");
    } catch (ProcessEngineException e) {
    }

    // then
    CaseInstance caseInstance = caseService
      .createCaseInstanceQuery()
      .singleResult();

    assertNotNull(caseInstance);
    assertTrue(caseInstance.isActive());
  }

  @Deployment(resources={"org/camunda/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  public void testCloseACompletedCaseInstance() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    String caseInstanceId = caseService
       .withCaseDefinition(caseDefinitionId)
       .create()
       .getId();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // disable human task -> case instance is completed
    caseService
      .withCaseExecution(caseExecutionId)
      .disable();

    // when
    caseService
      .withCaseExecution(caseInstanceId)
      .close();

    // then
    CaseInstance caseInstance = caseService
      .createCaseInstanceQuery()
      .singleResult();

    assertNull(caseInstance);
  }

}
