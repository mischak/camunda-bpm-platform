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
package org.camunda.bpm.engine.impl.el;

import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.impl.core.mapping.IoParameter;
import org.camunda.bpm.engine.impl.core.mapping.value.ParameterValueProvider;
import org.camunda.bpm.engine.impl.core.variable.CoreVariableScope;

/**
 * Makes it possible to use expression in {@link IoParameter} mappings.
 *
 * @author Daniel Meyer
 *
 */
public class ElValueProvider implements ParameterValueProvider {

  protected Expression expression;

  public ElValueProvider(Expression expression) {
    this.expression = expression;
  }

  public Object getValue(CoreVariableScope<?> variableScope) {
    return expression.getValue(variableScope);
  }

  public Expression getExpression() {
    return expression;
  }

  public void setExpression(Expression expression) {
    this.expression = expression;
  }

}
