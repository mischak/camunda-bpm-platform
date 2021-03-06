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
package org.camunda.bpm.engine.rest.sub.impl;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import org.camunda.bpm.engine.BadUserRequestException;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.rest.dto.PatchVariablesDto;
import org.camunda.bpm.engine.rest.dto.runtime.VariableValueDto;
import org.camunda.bpm.engine.rest.exception.InvalidRequestException;
import org.camunda.bpm.engine.rest.exception.RestException;
import org.camunda.bpm.engine.rest.mapper.MultipartFormData;
import org.camunda.bpm.engine.rest.mapper.MultipartFormData.FormPart;
import org.camunda.bpm.engine.rest.sub.VariableResource;
import org.camunda.bpm.engine.rest.util.DtoUtil;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;


public abstract class AbstractVariablesResource implements VariableResource {

  protected ProcessEngine engine;
  protected String resourceId;

  public AbstractVariablesResource(ProcessEngine engine, String resourceId) {
    this.engine = engine;
    this.resourceId = resourceId;
  }

  @Override
  public Map<String, VariableValueDto> getVariables() {
    Map<String, VariableValueDto> values = new HashMap<String, VariableValueDto>();

    for (Map.Entry<String, Object> entry : getVariableEntities().entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      String simpleClassName = null;

      if (value != null) {
        // if the value is not equals null, then get the simple class name.
        simpleClassName = value.getClass().getSimpleName();
      } else {
        // if the value is equals null, then the simple class name is "Null".
        simpleClassName = "Null";
      }

      values.put(key, new VariableValueDto(value, simpleClassName));
    }

    return values;
  }

  @Override
  public VariableValueDto getVariable(String variableName) {
    Object variable = null;
    try {
       variable = getVariableEntity(variableName);
    } catch (ProcessEngineException e) {
      String errorMessage = String.format("Cannot get %s variable %s: %s", getResourceTypeName(), variableName, e.getMessage());
      throw new RestException(Status.INTERNAL_SERVER_ERROR, e, errorMessage);
    }

    if (variable == null) {
      String errorMessage = String.format("%s variable with name %s does not exist or is null", getResourceTypeName(), variableName);
      throw new InvalidRequestException(Status.NOT_FOUND, errorMessage);
    }

    return new VariableValueDto(variable, variable.getClass().getSimpleName());

  }

  @Override
  public void putVariable(String variableName, VariableValueDto variable) {

    try {
      if (variable.isPrimitiveVariableUpdate()) {
        putPrimitiveVariableValue(variableName, variable.getType(), variable.getValue());
      } else if (variable.isSerializedVariableUpdate()) {
        setVariableEntityFromSerialized(variableName, variable.getValue(),
          variable.getVariableType(), variable.getSerializationConfig());
      } else {
        throw new InvalidRequestException(Status.BAD_REQUEST,
          String.format("Cannot put %s variable %s: Invalid combination of variable type '%s' and value type '%s'",
            getResourceTypeName(), variableName, variable.getVariableType(), variable.getType()));
      }
    } catch (BadUserRequestException e) {
      throw new RestException(Status.BAD_REQUEST, e,
        String.format("Cannot put %s variable %s: %s", getResourceTypeName(), variableName, e.getMessage()));
    } catch (ProcessEngineException e) {
      throw new RestException(Status.INTERNAL_SERVER_ERROR, e,
          String.format("Cannot put %s variable %s: %s", getResourceTypeName(), variableName, e.getMessage()));
    }
  }

  protected void putPrimitiveVariableValue(String variableName, String valueType, Object serializedValue) {
    try {
      Object convertedValue = DtoUtil.toType(valueType, serializedValue);
      setVariableEntity(variableName, convertedValue);

    } catch (NumberFormatException e) {
      String errorMessage = String.format("Cannot put %s variable %s due to number format exception: %s", getResourceTypeName(), variableName, e.getMessage());
      throw new RestException(Status.BAD_REQUEST, e, errorMessage);

    } catch (ParseException e) {
      String errorMessage = String.format("Cannot put %s variable %s due to parse exception: %s", getResourceTypeName(), variableName, e.getMessage());
      throw new RestException(Status.BAD_REQUEST, e, errorMessage);

    } catch (IllegalArgumentException e) {
      String errorMessage = String.format("Cannot put %s variable %s: %s", getResourceTypeName(), variableName, e.getMessage());
      throw new RestException(Status.BAD_REQUEST, errorMessage);
    }
  }

  public void setBinaryVariable(String variableKey, MultipartFormData payload) {
    FormPart dataPart = payload.getNamedPart("data");
    FormPart valueTypePart = payload.getNamedPart("type");
    FormPart variableTypePart = payload.getNamedPart("variableType");

    if(valueTypePart != null) {
      Object object = null;

      if(dataPart.getContentType()!=null
          && dataPart.getContentType().toLowerCase().contains(MediaType.APPLICATION_JSON)) {

        object = deserializeJsonObject(valueTypePart.getTextContent(), dataPart.getBinaryContent());

      } else {
        // TODO: also support java de-serialization as byte stream?
        throw new InvalidRequestException(Status.BAD_REQUEST, "Unrecognized content type for serialized java type: "+dataPart.getContentType());
      }

      if(object != null) {
        setVariableEntity(variableKey, object);
      }
    } else {
      try {
        // missing variableType means variable is byte[] (for backwards compatibility)
        if (variableTypePart == null) {
          setVariableEntity(variableKey, dataPart.getBinaryContent());
        } else {
          setVariableEntityFromSerialized(variableKey, dataPart.getBinaryContent(), variableTypePart.getTextContent(), null);
        }
      } catch (ProcessEngineException e) {
        String errorMessage = String.format("Cannot put %s variable %s: %s", getResourceTypeName(), variableKey, e.getMessage());
        throw new RestException(Status.INTERNAL_SERVER_ERROR, e, errorMessage);
      }
    }
  }

  protected Object deserializeJsonObject(String className, byte[] data) {
    try {

      ObjectMapper objectMapper = new ObjectMapper();
      JavaType type = TypeFactory.fromCanonical(className);

      return objectMapper.readValue(new String(data), type);

    } catch(Exception e) {
      throw new InvalidRequestException(Status.INTERNAL_SERVER_ERROR, "Could not deserialize JSON object: "+e.getMessage());

    }
  }

  @Override
  public void deleteVariable(String variableName) {
    try {
      removeVariableEntity(variableName);
    } catch (ProcessEngineException e) {
      String errorMessage = String.format("Cannot delete %s variable %s: %s", getResourceTypeName(), variableName, e.getMessage());
      throw new RestException(Status.INTERNAL_SERVER_ERROR, e, errorMessage);
    }

  }

  @Override
  public void modifyVariables(PatchVariablesDto patch) {
    Map<String, Object> variableModifications = null;
    try {
      variableModifications = DtoUtil.toMap(patch.getModifications());

    } catch (NumberFormatException e) {
      String errorMessage = String.format("Cannot modify variables for %s due to number format exception: %s", getResourceTypeName(), e.getMessage());
      throw new RestException(Status.BAD_REQUEST, e, errorMessage);

    } catch (ParseException e) {
      String errorMessage = String.format("Cannot modify variables for %s due to parse exception: %s", getResourceTypeName(), e.getMessage());
      throw new RestException(Status.BAD_REQUEST, e, errorMessage);

    } catch (IllegalArgumentException e) {
      String errorMessage = String.format("Cannot modify variables for %s: %s", getResourceTypeName(), e.getMessage());
      throw new RestException(Status.BAD_REQUEST, errorMessage);
    }

    List<String> variableDeletions = patch.getDeletions();

    try {
      updateVariableEntities(variableModifications, variableDeletions);
    } catch (ProcessEngineException e) {
      String errorMessage = String.format("Cannot modify variables for %s %s: %s", getResourceTypeName(), resourceId, e.getMessage());
      throw new RestException(Status.INTERNAL_SERVER_ERROR, e, errorMessage);
    }


  }

  protected abstract Map<String, Object> getVariableEntities();

  protected abstract void updateVariableEntities(Map<String, Object> variables, List<String> deletions);

  protected abstract Object getVariableEntity(String variableKey);

  protected abstract void setVariableEntity(String variableKey, Object variableValue);

  protected abstract void setVariableEntityFromSerialized(String variableKey, Object serializedValue,
      String variableType, Map<String, Object> configuration);

  protected abstract void removeVariableEntity(String variableKey);

  protected abstract String getResourceTypeName();

}
