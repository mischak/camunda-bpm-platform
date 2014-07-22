/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WICmmnElementHOUCmmnElement WARRANCmmnElementIES OR CONDICmmnElementIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.engine.impl.cmmn.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.camunda.bpm.engine.delegate.CaseExecutionListener;
import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.impl.bpmn.parser.FieldDeclaration;
import org.camunda.bpm.engine.impl.cmmn.CaseControlRule;
import org.camunda.bpm.engine.impl.cmmn.behavior.CaseControlRuleImpl;
import org.camunda.bpm.engine.impl.cmmn.listener.ClassDelegateCaseExecutionListener;
import org.camunda.bpm.engine.impl.cmmn.listener.DelegateExpressionCaseExecutionListener;
import org.camunda.bpm.engine.impl.cmmn.listener.ExpressionCaseExecutionListener;
import org.camunda.bpm.engine.impl.cmmn.listener.ScriptCaseExecutionListener;
import org.camunda.bpm.engine.impl.cmmn.model.CmmnActivity;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.el.ExpressionManager;
import org.camunda.bpm.engine.impl.el.FixedValue;
import org.camunda.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.camunda.bpm.engine.impl.scripting.DynamicResourceExecutableScript;
import org.camunda.bpm.engine.impl.scripting.DynamicSourceExecutableScript;
import org.camunda.bpm.engine.impl.scripting.ExecutableScript;
import org.camunda.bpm.engine.impl.scripting.engine.JuelScriptEngineFactory;
import org.camunda.bpm.engine.impl.util.ResourceUtil;
import org.camunda.bpm.engine.impl.util.StringUtil;
import org.camunda.bpm.model.cmmn.Query;
import org.camunda.bpm.model.cmmn.instance.CmmnElement;
import org.camunda.bpm.model.cmmn.instance.DiscretionaryItem;
import org.camunda.bpm.model.cmmn.instance.ExtensionElements;
import org.camunda.bpm.model.cmmn.instance.ManualActivationRule;
import org.camunda.bpm.model.cmmn.instance.PlanItem;
import org.camunda.bpm.model.cmmn.instance.PlanItemControl;
import org.camunda.bpm.model.cmmn.instance.PlanItemDefinition;
import org.camunda.bpm.model.cmmn.instance.RepetitionRule;
import org.camunda.bpm.model.cmmn.instance.RequiredRule;
import org.camunda.bpm.model.cmmn.instance.Sentry;
import org.camunda.bpm.model.cmmn.instance.camunda.CamundaCaseExecutionListener;
import org.camunda.bpm.model.cmmn.instance.camunda.CamundaField;
import org.camunda.bpm.model.cmmn.instance.camunda.CamundaScript;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

/**
 * @author Roman Smirnov
 *
 */
public abstract class ItemHandler extends CmmnElementHandler<CmmnElement> {

  protected static List<String> TASK_OR_STAGE_EVENTS = Arrays.asList(
      CaseExecutionListener.CREATE,
      CaseExecutionListener.ENABLE,
      CaseExecutionListener.DISABLE,
      CaseExecutionListener.RE_ENABLE,
      CaseExecutionListener.START,
      CaseExecutionListener.MANUAL_START,
      CaseExecutionListener.TERMINATE,
      CaseExecutionListener.EXIT,
      CaseExecutionListener.SUSPEND,
      CaseExecutionListener.PARENT_SUSPEND,
      CaseExecutionListener.RESUME,
      CaseExecutionListener.PARENT_RESUME,
      CaseExecutionListener.COMPLETE
    );

  protected static List<String> CASE_PLAN_MODEL_EVENTS = Arrays.asList(
      CaseExecutionListener.CREATE,
      CaseExecutionListener.TERMINATE,
      CaseExecutionListener.SUSPEND,
      CaseExecutionListener.COMPLETE,
      CaseExecutionListener.RE_ACTIVATE,
      CaseExecutionListener.CLOSE
    );



  public CmmnActivity handleElement(CmmnElement element, CmmnHandlerContext context) {
    // create a new activity
    CmmnActivity newActivity = createActivity(element, context);

    // initialize activity
    initializeActivity(element, newActivity, context);

    return newActivity;
  }

  protected void initializeActivity(CmmnElement element, CmmnActivity activity, CmmnHandlerContext context) {
    if (isDiscretionaryItem(element)) {
      activity.setProperty("discretionary", true);
    }

    String name = getName(element);

    if (name == null) {
      PlanItemDefinition definition = getDefinition(element);
      name = definition.getName();
    }
    activity.setName(name);

    // requiredRule
    initializeRequiredRule(element, activity, context);

    // manualActivation
    initializeManualActivationRule(element, activity, context);

    // repetitionRule
    initializeRepetitionRule(element, activity, context);

    // case execution listeners
    initializeCaseExecutionListeners(element, activity, context);

  }

  protected void initializeRequiredRule(CmmnElement element, CmmnActivity activity, CmmnHandlerContext context) {
    PlanItemControl itemControl = getItemControl(element);
    PlanItemControl defaultControl = getDefaultControl(element);

    ExpressionManager expressionManager = context.getExpressionManager();

    RequiredRule requiredRule = null;
    if (itemControl != null) {
      requiredRule = itemControl.getRequiredRule();
    }
    if (requiredRule == null && defaultControl != null) {
      requiredRule = defaultControl.getRequiredRule();
    }

    if (requiredRule != null) {
      String rule = requiredRule.getCondition().getBody();
      Expression requiredRuleExpression = expressionManager.createExpression(rule);
      CaseControlRule caseRule = new CaseControlRuleImpl(requiredRuleExpression);
      activity.setProperty("requiredRule", caseRule);
    }

  }

  protected void initializeManualActivationRule(CmmnElement element, CmmnActivity activity, CmmnHandlerContext context) {
    PlanItemControl itemControl = getItemControl(element);
    PlanItemControl defaultControl = getDefaultControl(element);

    ExpressionManager expressionManager = context.getExpressionManager();

    ManualActivationRule manualActivationRule = null;
    if (itemControl != null) {
      manualActivationRule = itemControl.getManualActivationRule();
    }
    if (manualActivationRule == null && defaultControl != null) {
      manualActivationRule = defaultControl.getManualActivationRule();
    }

    if (manualActivationRule != null) {
      String rule = manualActivationRule.getCondition().getBody();
      Expression requiredRuleExpression = expressionManager.createExpression(rule);
      CaseControlRule caseRule = new CaseControlRuleImpl(requiredRuleExpression);
      activity.setProperty("manualActivationRule", caseRule);
    }

  }

  protected void initializeRepetitionRule(CmmnElement element, CmmnActivity activity, CmmnHandlerContext context) {
    PlanItemControl itemControl = getItemControl(element);
    PlanItemControl defaultControl = getDefaultControl(element);

    ExpressionManager expressionManager = context.getExpressionManager();

    RepetitionRule repetitionRule = null;
    if (itemControl != null) {
      repetitionRule = itemControl.getRepetitionRule();
    }
    if (repetitionRule == null && defaultControl != null) {
      repetitionRule = defaultControl.getRepetitionRule();
    }

    if (repetitionRule != null) {
      String rule = repetitionRule.getCondition().getBody();
      Expression requiredRuleExpression = expressionManager.createExpression(rule);
      CaseControlRule caseRule = new CaseControlRuleImpl(requiredRuleExpression);
      activity.setProperty("repetitionRule", caseRule);
    }

  }

  protected void initializeCaseExecutionListeners(CmmnElement element, CmmnActivity activity, CmmnHandlerContext context) {
    PlanItemDefinition definition = getDefinition(element);

    List<CamundaCaseExecutionListener> listeners = queryExtensionElementsByClass(definition, CamundaCaseExecutionListener.class);

    for (CamundaCaseExecutionListener listener : listeners) {
      CaseExecutionListener caseExecutionListener = initializeCaseExecutionListener(element, activity, context, listener);

      String eventName = listener.getCamundaEvent();
      if(eventName != null) {
        activity.addListener(eventName, caseExecutionListener);

      } else {
        for (String event : getStandardEvents(element)) {
          activity.addListener(event, caseExecutionListener);
        }
      }
    }
  }

  protected CaseExecutionListener initializeCaseExecutionListener(CmmnElement element, CmmnActivity activity, CmmnHandlerContext context, CamundaCaseExecutionListener listener) {
    Collection<CamundaField> fields = listener.getCamundaFields();
    List<FieldDeclaration> fieldDeclarations = initializeFieldDeclarations(element, activity, context, fields);

    ExpressionManager expressionManager = context.getExpressionManager();

    CaseExecutionListener caseExecutionListener = null;

    String className = listener.getCamundaClass();
    String expression = listener.getCamundaExpression();
    String delegateExpression = listener.getCamundaDelegateExpression();
    CamundaScript scriptElement = listener.getCamundaScript();

    if (className != null) {
      caseExecutionListener = new ClassDelegateCaseExecutionListener(className, fieldDeclarations);

    } else if (expression != null) {
      Expression expressionExp = expressionManager.createExpression(expression);
      caseExecutionListener = new ExpressionCaseExecutionListener(expressionExp);

    } else if (delegateExpression != null) {
      Expression delegateExp = expressionManager.createExpression(delegateExpression);
      caseExecutionListener = new DelegateExpressionCaseExecutionListener(delegateExp, fieldDeclarations);

    } else if (scriptElement != null) {
      ExecutableScript executableScript = initializeScript(element, activity, context, scriptElement);
      if (executableScript != null) {
        caseExecutionListener = new ScriptCaseExecutionListener(executableScript);
      }
    }

    return caseExecutionListener;
  }

  protected ExecutableScript initializeScript(CmmnElement element, CmmnActivity activity, CmmnHandlerContext context, CamundaScript script) {
    String language = script.getCamundaScriptFormat();
    String resource = script.getCamundaResource();
    String source = script.getTextContent();

    return initializeScriptDefinition(language, resource, source, context);
  }

  public ExecutableScript initializeScriptDefinition(String language, String resource, String source, CmmnHandlerContext context) {
    if (language != null) {
      if (resource != null && !resource.isEmpty()) {
        return parseScriptResource(resource, language, context);
      }
      else if(source != null) {
        return parseScriptSource(source, language, context);
      }
    }
    return null;
  }

  protected ExecutableScript parseScriptSource(String source, String language, CmmnHandlerContext context) {
    if (StringUtil.isExpression(source) && !JuelScriptEngineFactory.names.contains(language)) {
      ExpressionManager expressionManager = context.getExpressionManager();
      Expression scriptExpression = expressionManager.createExpression(source.trim());
      return new DynamicSourceExecutableScript(scriptExpression, language);
    }
    else {
      return parseScript(source, language);
    }
  }

  protected ExecutableScript parseScriptResource(String resource, String language, CmmnHandlerContext context) {
    if (StringUtil.isExpression(resource)) {
      ExpressionManager expressionManager = context.getExpressionManager();
      Expression scriptResourceExpression = expressionManager.createExpression(resource);
      return new DynamicResourceExecutableScript(scriptResourceExpression, language);
    }
    else {
      DeploymentEntity deployment = (DeploymentEntity) context.getDeployment();
      String scriptSource = ResourceUtil.loadResourceContent(resource, deployment);
      return parseScript(scriptSource, language);
    }
  }

  protected ExecutableScript parseScript(String script, String language) {
    return Context.getProcessEngineConfiguration()
      .getScriptFactory()
      .createScript(script, language);
  }

  protected List<FieldDeclaration> initializeFieldDeclarations(CmmnElement element, CmmnActivity activity, CmmnHandlerContext context, Collection<CamundaField> fields) {
    List<FieldDeclaration> fieldDeclarations = new ArrayList<FieldDeclaration>();

    for (CamundaField field : fields) {
      FieldDeclaration fieldDeclaration = initializeFieldDeclaration(element, activity, context, field);
      fieldDeclarations.add(fieldDeclaration);
    }

    return fieldDeclarations;
  }

  protected FieldDeclaration initializeFieldDeclaration(CmmnElement element, CmmnActivity activity, CmmnHandlerContext context, CamundaField field) {
    String name = field.getCamundaName();
    String type = Expression.class.getName(); // TODO: why?

    Object value = getFixedValue(field);

    if (value == null) {
      ExpressionManager expressionManager = context.getExpressionManager();
      value = getExpressionValue(field, expressionManager);
    }

    return new FieldDeclaration(name, type, value);
  }

  protected FixedValue getFixedValue(CamundaField field) {
    String value = field.getCamundaString().getTextContent();

    if (value == null) {
      value = field.getCamundaStringValue();
    }

    if (value != null) {
      return new FixedValue(value);
    }

    return null;
  }

  protected Expression getExpressionValue(CamundaField field, ExpressionManager expressionManager) {
    String value = field.getCamundaExpressionChild().getTextContent();

    if (value == null) {
      value = field.getCamundaExpression();
    }

    if (value != null) {
      return expressionManager.createExpression(value);
    }

    return null;
  }

  protected PlanItemControl getDefaultControl(CmmnElement element) {
    PlanItemDefinition definition = getDefinition(element);

    return definition.getDefaultControl();
  }

  protected <V extends ModelElementInstance> List<V> queryExtensionElementsByClass(CmmnElement element, Class<V> cls) {
    ExtensionElements extensionElements = getExtensionElements(element);

    if (extensionElements != null) {
      Query<ModelElementInstance> query = extensionElements.getElementsQuery();
      return query.filterByType(cls).list();

    } else {
      return new ArrayList<V>();
    }
  }

  protected ExtensionElements getExtensionElements(CmmnElement element) {
    return element.getExtensionElements();
  }

  protected PlanItemControl getItemControl(CmmnElement element) {
    if (isPlanItem(element)) {
      PlanItem planItem = (PlanItem) element;
      return planItem.getItemControl();
    } else
    if (isDiscretionaryItem(element)) {
      DiscretionaryItem discretionaryItem = (DiscretionaryItem) element;
      return discretionaryItem.getItemControl();
    }

    return null;
  }

  protected String getName(CmmnElement element) {
    String name = null;
    if (isPlanItem(element)) {
      PlanItem planItem = (PlanItem) element;
      name = planItem.getName();
    }

    if (name == null || name.isEmpty()) {
      PlanItemDefinition definition = getDefinition(element);
      name = definition.getName();
    }

    return name;
  }

  protected PlanItemDefinition getDefinition(CmmnElement element) {
    if (isPlanItem(element)) {
      PlanItem planItem = (PlanItem) element;
      return planItem.getDefinition();
    } else
    if (isDiscretionaryItem(element)) {
      DiscretionaryItem discretionaryItem = (DiscretionaryItem) element;
      return discretionaryItem.getDefinition();
    }

    return null;
  }

  protected List<Sentry> getEntryCriterias(CmmnElement element) {
    if (isPlanItem(element)) {
      PlanItem planItem = (PlanItem) element;
      return (List<Sentry>) planItem.getEntryCriterias();
    }

    return new ArrayList<Sentry>();
  }

  protected List<Sentry> getExitCriterias(CmmnElement element) {
    if (isPlanItem(element)) {
      PlanItem planItem = (PlanItem) element;
      return (List<Sentry>) planItem.getExitCriterias();
    }

    return new ArrayList<Sentry>();
  }

  protected String getDesciption(CmmnElement element) {
    String description = element.getDescription();

    if (description == null) {
      PlanItemDefinition definition = getDefinition(element);
      description = definition.getDescription();
    }

    return description;
  }

  protected boolean isPlanItem(CmmnElement element) {
    return element instanceof PlanItem;
  }

  protected boolean isDiscretionaryItem(CmmnElement element) {
    return element instanceof DiscretionaryItem;
  }

  protected abstract List<String> getStandardEvents(CmmnElement element);

}
