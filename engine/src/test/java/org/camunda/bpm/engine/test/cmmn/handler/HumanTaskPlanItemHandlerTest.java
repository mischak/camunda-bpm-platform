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
package org.camunda.bpm.engine.test.cmmn.handler;

import static org.camunda.bpm.engine.impl.cmmn.handler.ItemHandler.PROPERTY_IS_BLOCKING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Set;

import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.cmmn.behavior.CmmnActivityBehavior;
import org.camunda.bpm.engine.impl.cmmn.behavior.HumanTaskActivityBehavior;
import org.camunda.bpm.engine.impl.cmmn.handler.HumanTaskItemHandler;
import org.camunda.bpm.engine.impl.cmmn.model.CmmnActivity;
import org.camunda.bpm.engine.impl.cmmn.model.CmmnCaseDefinition;
import org.camunda.bpm.engine.impl.el.ExpressionManager;
import org.camunda.bpm.engine.impl.task.TaskDefinition;
import org.camunda.bpm.engine.impl.task.listener.ClassDelegateTaskListener;
import org.camunda.bpm.engine.impl.task.listener.DelegateExpressionTaskListener;
import org.camunda.bpm.engine.impl.task.listener.ExpressionTaskListener;
import org.camunda.bpm.model.cmmn.impl.instance.CaseRoles;
import org.camunda.bpm.model.cmmn.instance.ExtensionElements;
import org.camunda.bpm.model.cmmn.instance.HumanTask;
import org.camunda.bpm.model.cmmn.instance.PlanItem;
import org.camunda.bpm.model.cmmn.instance.camunda.CamundaTaskListener;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Roman Smirnov
 *
 */
public class HumanTaskPlanItemHandlerTest extends CmmnElementHandlerTest {

  protected HumanTask humanTask;
  protected PlanItem planItem;
  protected HumanTaskItemHandler handler = new HumanTaskItemHandler();

  @Before
  public void setUp() {
    humanTask = createElement(casePlanModel, "aHumanTask", HumanTask.class);

    planItem = createElement(casePlanModel, "PI_aHumanTask", PlanItem.class);
    planItem.setDefinition(humanTask);

  }

  @Test
  public void testHumanTaskActivityName() {
    // given:
    // the humanTask has a name "A HumanTask"
    String name = "A HumanTask";
    humanTask.setName(name);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertEquals(name, activity.getName());
  }

  @Test
  public void testPlanItemActivityName() {
    // given:
    // the humanTask has a name "A HumanTask"
    String humanTaskName = "A HumanTask";
    humanTask.setName(humanTaskName);

    // the planItem has an own name "My LocalName"
    String planItemName = "My LocalName";
    planItem.setName(planItemName);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertNotEquals(humanTaskName, activity.getName());
    assertEquals(planItemName, activity.getName());
  }

  @Test
  public void testActivityBehavior() {
    // given: a planItem

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    CmmnActivityBehavior behavior = activity.getActivityBehavior();
    assertTrue(behavior instanceof HumanTaskActivityBehavior);
  }

  @Test
  public void testIsBlockingEqualsTrueProperty() {
    // given: a humanTask with isBlocking = true (defaultValue)

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    Boolean isBlocking = (Boolean) activity.getProperty(PROPERTY_IS_BLOCKING);
    assertTrue(isBlocking);
  }

  @Test
  public void testIsBlockingEqualsFalseProperty() {
    // given:
    // a humanTask with isBlocking = false
    humanTask.setIsBlocking(false);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    // According to the specification:
    // When a HumanTask is not 'blocking'
    // (isBlocking is 'false'), it can be
    // considered a 'manual' Task, i.e.,
    // the Case management system is not
    // tracking the lifecycle of the HumanTask (instance).
    assertNull(activity);
  }

  @Test
  public void testWithoutParent() {
    // given: a planItem

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertNull(activity.getParent());
  }

  @Test
  public void testWithParent() {
    // given:
    // a new activity as parent
    CmmnCaseDefinition parent = new CmmnCaseDefinition("aParentActivity");
    context.setParent(parent);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertEquals(parent, activity.getParent());
    assertTrue(parent.getActivities().contains(activity));
  }

  @Test
  public void testTaskDecorator() {
    // given: a plan item

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    // there exists a taskDecorator
    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();

    assertNotNull(behavior.getTaskDecorator());
  }

  @Test
  public void testTaskDefinition() {
    // given: a plan item

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    // there exists a taskDefinition
    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();

    assertNotNull(behavior.getTaskDefinition());
  }

  @Test
  public void testExpressionManager() {
    // given: a plan item

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();

    ExpressionManager expressionManager = behavior.getExpressionManager();
    assertNotNull(expressionManager);
    assertEquals(context.getExpressionManager(), expressionManager);
  }

  @Test
  public void testTaskDefinitionHumanTaskNameExpression() {
    // given
    String name = "A HumanTask";
    humanTask.setName(name);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();

    Expression nameExpression = behavior.getTaskDefinition().getNameExpression();
    assertNotNull(nameExpression);
    assertEquals("A HumanTask", nameExpression.getExpressionText());
  }

  @Test
  public void testTaskDefinitionPlanItemNameExpression() {
    // given
    String name = "A HumanTask";
    humanTask.setName(name);

    String planItemName = "My LocalName";
    planItem.setName(planItemName);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();
    TaskDefinition taskDefinition = behavior.getTaskDefinition();

    Expression nameExpression = taskDefinition.getNameExpression();
    assertNotNull(nameExpression);
    assertEquals("My LocalName", nameExpression.getExpressionText());
  }

  @Test
  public void testTaskDefinitionDueDateExpression() {
    // given
    String aDueDate = "aDueDate";
    humanTask.setCamundaDueDate(aDueDate);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();
    TaskDefinition taskDefinition = behavior.getTaskDefinition();

    Expression dueDateExpression = taskDefinition.getDueDateExpression();
    assertNotNull(dueDateExpression);
    assertEquals(aDueDate, dueDateExpression.getExpressionText());
  }

  @Test
  public void testTaskDefinitionPriorityExpression() {
    // given
    String aPriority = "aPriority";
    humanTask.setCamundaPriority(aPriority);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();
    TaskDefinition taskDefinition = behavior.getTaskDefinition();

    Expression priorityExpression = taskDefinition.getPriorityExpression();
    assertNotNull(priorityExpression);
    assertEquals(aPriority, priorityExpression.getExpressionText());
  }

  @Test
  public void testTaskDefinitionPeformerExpression() {
    // given
    CaseRoles role = createElement(caseDefinition, "aRole", CaseRoles.class);
    role.setName("aPerformerRole");

    humanTask.setPerformer(role);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();
    TaskDefinition taskDefinition = behavior.getTaskDefinition();

    Expression assigneeExpression = taskDefinition.getAssigneeExpression();
    assertNotNull(assigneeExpression);
    assertEquals("aPerformerRole", assigneeExpression.getExpressionText());
  }

  @Test
  public void testTaskDefinitionAssigneeExpression() {
    // given
    String aPriority = "aPriority";
    humanTask.setCamundaPriority(aPriority);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();
    TaskDefinition taskDefinition = behavior.getTaskDefinition();

    Expression priorityExpression = taskDefinition.getPriorityExpression();
    assertNotNull(priorityExpression);
    assertEquals(aPriority, priorityExpression.getExpressionText());
  }

  @Test
  public void testTaskDefinitionCandidateUsers() {
    // given
    String aCandidateUsers = "mary,john,peter";
    humanTask.setCamundaCandidateUsers(aCandidateUsers);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();
    TaskDefinition taskDefinition = behavior.getTaskDefinition();

    Set<Expression> candidateUserExpressions = taskDefinition.getCandidateUserIdExpressions();
    assertEquals(3, candidateUserExpressions.size());

    for (Expression candidateUserExpression : candidateUserExpressions) {
      String candidateUser = candidateUserExpression.getExpressionText();
      if ("mary".equals(candidateUser)) {
        assertEquals("mary", candidateUser);
      } else if ("john".equals(candidateUser)) {
        assertEquals("john", candidateUser);
      } else if ("peter".equals(candidateUser)) {
        assertEquals("peter", candidateUser);
      } else {
        fail("Unexpected candidate user: " + candidateUser);
      }
    }
  }

  @Test
  public void testTaskDefinitionCandidateGroups() {
    // given
    String aCandidateGroups = "accounting,management,backoffice";
    humanTask.setCamundaCandidateGroups(aCandidateGroups);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();
    TaskDefinition taskDefinition = behavior.getTaskDefinition();

    Set<Expression> candidateGroupExpressions = taskDefinition.getCandidateGroupIdExpressions();
    assertEquals(3, candidateGroupExpressions.size());

    for (Expression candidateGroupExpression : candidateGroupExpressions) {
      String candidateGroup = candidateGroupExpression.getExpressionText();
      if ("accounting".equals(candidateGroup)) {
        assertEquals("accounting", candidateGroup);
      } else if ("management".equals(candidateGroup)) {
        assertEquals("management", candidateGroup);
      } else if ("backoffice".equals(candidateGroup)) {
        assertEquals("backoffice", candidateGroup);
      } else {
        fail("Unexpected candidate group: " + candidateGroup);
      }
    }
  }

  @Test
  public void testTaskDefinitionFormKey() {
    // given
    String aFormKey = "aFormKey";
    humanTask.setCamundaFormKey(aFormKey);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();
    TaskDefinition taskDefinition = behavior.getTaskDefinition();

    Expression formKeyExpression = taskDefinition.getFormKey();
    assertNotNull(formKeyExpression);
    assertEquals(aFormKey, formKeyExpression.getExpressionText());
  }

  @Test
  public void testHumanTaskDescription() {
    // given
    String description = "A description";
    humanTask.setDescription(description);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();
    TaskDefinition taskDefinition = behavior.getTaskDefinition();

    Expression descriptionExpression = taskDefinition.getDescriptionExpression();
    assertNotNull(descriptionExpression);
    assertEquals(description, descriptionExpression.getExpressionText());
  }

  @Test
  public void testPlanItemDescription() {
    // given
    String description = "A description";
    humanTask.setDescription(description);

    // the planItem has an own description
    String localDescription = "My Local Description";
    planItem.setDescription(localDescription);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();
    TaskDefinition taskDefinition = behavior.getTaskDefinition();

    Expression descriptionExpression = taskDefinition.getDescriptionExpression();
    assertNotNull(descriptionExpression);
    assertEquals(localDescription, descriptionExpression.getExpressionText());
  }

  @Test
  public void testCreateTaskListenerByClass() {
    // given:
    ExtensionElements extensionElements = addExtensionElements(humanTask);
    CamundaTaskListener taskListener = createElement(extensionElements, null, CamundaTaskListener.class);

    String className = "org.camunda.bpm.test.tasklistener.ABC";
    String event = TaskListener.EVENTNAME_CREATE;
    taskListener.setCamundaEvent(event);
    taskListener.setCamundaClass(className);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertEquals(0, activity.getListeners().size());

    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();
    TaskDefinition taskDefinition = behavior.getTaskDefinition();

    assertNotNull(taskDefinition);

    assertEquals(1, taskDefinition.getTaskListeners().size());

    List<TaskListener> createListeners = taskDefinition.getTaskListener(event);
    assertEquals(1, createListeners.size());
    TaskListener listener = createListeners.get(0);

    assertTrue(listener instanceof ClassDelegateTaskListener);

    ClassDelegateTaskListener classDelegateListener = (ClassDelegateTaskListener) listener;
    assertEquals(className, classDelegateListener.getClassName());
    assertTrue(classDelegateListener.getFieldDeclarations().isEmpty());

  }

  @Test
  public void testCreateTaskListenerByDelegateExpression() {
    // given:
    ExtensionElements extensionElements = addExtensionElements(humanTask);
    CamundaTaskListener taskListener = createElement(extensionElements, null, CamundaTaskListener.class);

    String delegateExpression = "${myDelegateExpression}";
    String event = TaskListener.EVENTNAME_CREATE;
    taskListener.setCamundaEvent(event);
    taskListener.setCamundaDelegateExpression(delegateExpression);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertEquals(0, activity.getListeners().size());

    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();
    TaskDefinition taskDefinition = behavior.getTaskDefinition();

    assertNotNull(taskDefinition);

    assertEquals(1, taskDefinition.getTaskListeners().size());

    List<TaskListener> createListeners = taskDefinition.getTaskListener(event);
    assertEquals(1, createListeners.size());
    TaskListener listener = createListeners.get(0);

    assertTrue(listener instanceof DelegateExpressionTaskListener);

    DelegateExpressionTaskListener delegateExpressionListener = (DelegateExpressionTaskListener) listener;
    assertEquals(delegateExpression, delegateExpressionListener.getExpressionText());
    assertTrue(delegateExpressionListener.getFieldDeclarations().isEmpty());

  }

  @Test
  public void testCreateTaskListenerByExpression() {
    // given:
    ExtensionElements extensionElements = addExtensionElements(humanTask);
    CamundaTaskListener taskListener = createElement(extensionElements, null, CamundaTaskListener.class);

    String expression = "${myExpression}";
    String event = TaskListener.EVENTNAME_CREATE;
    taskListener.setCamundaEvent(event);
    taskListener.setCamundaExpression(expression);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertEquals(0, activity.getListeners().size());

    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();
    TaskDefinition taskDefinition = behavior.getTaskDefinition();

    assertNotNull(taskDefinition);

    assertEquals(1, taskDefinition.getTaskListeners().size());

    List<TaskListener> createListeners = taskDefinition.getTaskListener(event);
    assertEquals(1, createListeners.size());
    TaskListener listener = createListeners.get(0);

    assertTrue(listener instanceof ExpressionTaskListener);

    ExpressionTaskListener expressionListener = (ExpressionTaskListener) listener;
    assertEquals(expression, expressionListener.getExpressionText());

  }

  @Test
  public void testCompleteTaskListenerByClass() {
    // given:
    ExtensionElements extensionElements = addExtensionElements(humanTask);
    CamundaTaskListener taskListener = createElement(extensionElements, null, CamundaTaskListener.class);

    String className = "org.camunda.bpm.test.tasklistener.ABC";
    String event = TaskListener.EVENTNAME_COMPLETE;
    taskListener.setCamundaEvent(event);
    taskListener.setCamundaClass(className);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertEquals(0, activity.getListeners().size());

    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();
    TaskDefinition taskDefinition = behavior.getTaskDefinition();

    assertNotNull(taskDefinition);

    assertEquals(1, taskDefinition.getTaskListeners().size());

    List<TaskListener> createListeners = taskDefinition.getTaskListener(event);
    assertEquals(1, createListeners.size());
    TaskListener listener = createListeners.get(0);

    assertTrue(listener instanceof ClassDelegateTaskListener);

    ClassDelegateTaskListener classDelegateListener = (ClassDelegateTaskListener) listener;
    assertEquals(className, classDelegateListener.getClassName());
    assertTrue(classDelegateListener.getFieldDeclarations().isEmpty());

  }

  @Test
  public void testCompleteTaskListenerByDelegateExpression() {
    // given:
    ExtensionElements extensionElements = addExtensionElements(humanTask);
    CamundaTaskListener taskListener = createElement(extensionElements, null, CamundaTaskListener.class);

    String delegateExpression = "${myDelegateExpression}";
    String event = TaskListener.EVENTNAME_COMPLETE;
    taskListener.setCamundaEvent(event);
    taskListener.setCamundaDelegateExpression(delegateExpression);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertEquals(0, activity.getListeners().size());

    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();
    TaskDefinition taskDefinition = behavior.getTaskDefinition();

    assertNotNull(taskDefinition);

    assertEquals(1, taskDefinition.getTaskListeners().size());

    List<TaskListener> createListeners = taskDefinition.getTaskListener(event);
    assertEquals(1, createListeners.size());
    TaskListener listener = createListeners.get(0);

    assertTrue(listener instanceof DelegateExpressionTaskListener);

    DelegateExpressionTaskListener delegateExpressionListener = (DelegateExpressionTaskListener) listener;
    assertEquals(delegateExpression, delegateExpressionListener.getExpressionText());
    assertTrue(delegateExpressionListener.getFieldDeclarations().isEmpty());

  }

  @Test
  public void testCompleteTaskListenerByExpression() {
    // given:
    ExtensionElements extensionElements = addExtensionElements(humanTask);
    CamundaTaskListener taskListener = createElement(extensionElements, null, CamundaTaskListener.class);

    String expression = "${myExpression}";
    String event = TaskListener.EVENTNAME_COMPLETE;
    taskListener.setCamundaEvent(event);
    taskListener.setCamundaExpression(expression);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertEquals(0, activity.getListeners().size());

    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();
    TaskDefinition taskDefinition = behavior.getTaskDefinition();

    assertNotNull(taskDefinition);

    assertEquals(1, taskDefinition.getTaskListeners().size());

    List<TaskListener> createListeners = taskDefinition.getTaskListener(event);
    assertEquals(1, createListeners.size());
    TaskListener listener = createListeners.get(0);

    assertTrue(listener instanceof ExpressionTaskListener);

    ExpressionTaskListener expressionListener = (ExpressionTaskListener) listener;
    assertEquals(expression, expressionListener.getExpressionText());

  }

  @Test
  public void testAssignmentTaskListenerByClass() {
    // given:
    ExtensionElements extensionElements = addExtensionElements(humanTask);
    CamundaTaskListener taskListener = createElement(extensionElements, null, CamundaTaskListener.class);

    String className = "org.camunda.bpm.test.tasklistener.ABC";
    String event = TaskListener.EVENTNAME_ASSIGNMENT;
    taskListener.setCamundaEvent(event);
    taskListener.setCamundaClass(className);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertEquals(0, activity.getListeners().size());

    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();
    TaskDefinition taskDefinition = behavior.getTaskDefinition();

    assertNotNull(taskDefinition);

    assertEquals(1, taskDefinition.getTaskListeners().size());

    List<TaskListener> createListeners = taskDefinition.getTaskListener(event);
    assertEquals(1, createListeners.size());
    TaskListener listener = createListeners.get(0);

    assertTrue(listener instanceof ClassDelegateTaskListener);

    ClassDelegateTaskListener classDelegateListener = (ClassDelegateTaskListener) listener;
    assertEquals(className, classDelegateListener.getClassName());
    assertTrue(classDelegateListener.getFieldDeclarations().isEmpty());

  }

  @Test
  public void testAssignmentTaskListenerByDelegateExpression() {
    // given:
    ExtensionElements extensionElements = addExtensionElements(humanTask);
    CamundaTaskListener taskListener = createElement(extensionElements, null, CamundaTaskListener.class);

    String delegateExpression = "${myDelegateExpression}";
    String event = TaskListener.EVENTNAME_ASSIGNMENT;
    taskListener.setCamundaEvent(event);
    taskListener.setCamundaDelegateExpression(delegateExpression);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertEquals(0, activity.getListeners().size());

    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();
    TaskDefinition taskDefinition = behavior.getTaskDefinition();

    assertNotNull(taskDefinition);

    assertEquals(1, taskDefinition.getTaskListeners().size());

    List<TaskListener> createListeners = taskDefinition.getTaskListener(event);
    assertEquals(1, createListeners.size());
    TaskListener listener = createListeners.get(0);

    assertTrue(listener instanceof DelegateExpressionTaskListener);

    DelegateExpressionTaskListener delegateExpressionListener = (DelegateExpressionTaskListener) listener;
    assertEquals(delegateExpression, delegateExpressionListener.getExpressionText());
    assertTrue(delegateExpressionListener.getFieldDeclarations().isEmpty());

  }

  @Test
  public void testAssignmentTaskListenerByExpression() {
    // given:
    ExtensionElements extensionElements = addExtensionElements(humanTask);
    CamundaTaskListener taskListener = createElement(extensionElements, null, CamundaTaskListener.class);

    String expression = "${myExpression}";
    String event = TaskListener.EVENTNAME_ASSIGNMENT;
    taskListener.setCamundaEvent(event);
    taskListener.setCamundaExpression(expression);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertEquals(0, activity.getListeners().size());

    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();
    TaskDefinition taskDefinition = behavior.getTaskDefinition();

    assertNotNull(taskDefinition);

    assertEquals(1, taskDefinition.getTaskListeners().size());

    List<TaskListener> createListeners = taskDefinition.getTaskListener(event);
    assertEquals(1, createListeners.size());
    TaskListener listener = createListeners.get(0);

    assertTrue(listener instanceof ExpressionTaskListener);

    ExpressionTaskListener expressionListener = (ExpressionTaskListener) listener;
    assertEquals(expression, expressionListener.getExpressionText());

  }

  @Test
  public void testDeleteTaskListenerByClass() {
    // given:
    ExtensionElements extensionElements = addExtensionElements(humanTask);
    CamundaTaskListener taskListener = createElement(extensionElements, null, CamundaTaskListener.class);

    String className = "org.camunda.bpm.test.tasklistener.ABC";
    String event = TaskListener.EVENTNAME_DELETE;
    taskListener.setCamundaEvent(event);
    taskListener.setCamundaClass(className);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertEquals(0, activity.getListeners().size());

    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();
    TaskDefinition taskDefinition = behavior.getTaskDefinition();

    assertNotNull(taskDefinition);

    assertEquals(1, taskDefinition.getTaskListeners().size());

    List<TaskListener> createListeners = taskDefinition.getTaskListener(event);
    assertEquals(1, createListeners.size());
    TaskListener listener = createListeners.get(0);

    assertTrue(listener instanceof ClassDelegateTaskListener);

    ClassDelegateTaskListener classDelegateListener = (ClassDelegateTaskListener) listener;
    assertEquals(className, classDelegateListener.getClassName());
    assertTrue(classDelegateListener.getFieldDeclarations().isEmpty());

  }

  @Test
  public void testDeleteTaskListenerByDelegateExpression() {
    // given:
    ExtensionElements extensionElements = addExtensionElements(humanTask);
    CamundaTaskListener taskListener = createElement(extensionElements, null, CamundaTaskListener.class);

    String delegateExpression = "${myDelegateExpression}";
    String event = TaskListener.EVENTNAME_DELETE;
    taskListener.setCamundaEvent(event);
    taskListener.setCamundaDelegateExpression(delegateExpression);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertEquals(0, activity.getListeners().size());

    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();
    TaskDefinition taskDefinition = behavior.getTaskDefinition();

    assertNotNull(taskDefinition);

    assertEquals(1, taskDefinition.getTaskListeners().size());

    List<TaskListener> createListeners = taskDefinition.getTaskListener(event);
    assertEquals(1, createListeners.size());
    TaskListener listener = createListeners.get(0);

    assertTrue(listener instanceof DelegateExpressionTaskListener);

    DelegateExpressionTaskListener delegateExpressionListener = (DelegateExpressionTaskListener) listener;
    assertEquals(delegateExpression, delegateExpressionListener.getExpressionText());
    assertTrue(delegateExpressionListener.getFieldDeclarations().isEmpty());

  }

  @Test
  public void testDeleteTaskListenerByExpression() {
    // given:
    ExtensionElements extensionElements = addExtensionElements(humanTask);
    CamundaTaskListener taskListener = createElement(extensionElements, null, CamundaTaskListener.class);

    String expression = "${myExpression}";
    String event = TaskListener.EVENTNAME_DELETE;
    taskListener.setCamundaEvent(event);
    taskListener.setCamundaExpression(expression);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertEquals(0, activity.getListeners().size());

    HumanTaskActivityBehavior behavior = (HumanTaskActivityBehavior) activity.getActivityBehavior();
    TaskDefinition taskDefinition = behavior.getTaskDefinition();

    assertNotNull(taskDefinition);

    assertEquals(1, taskDefinition.getTaskListeners().size());

    List<TaskListener> createListeners = taskDefinition.getTaskListener(event);
    assertEquals(1, createListeners.size());
    TaskListener listener = createListeners.get(0);

    assertTrue(listener instanceof ExpressionTaskListener);

    ExpressionTaskListener expressionListener = (ExpressionTaskListener) listener;
    assertEquals(expression, expressionListener.getExpressionText());

  }

}
