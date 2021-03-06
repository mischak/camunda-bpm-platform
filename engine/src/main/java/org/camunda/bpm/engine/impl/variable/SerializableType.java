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
package org.camunda.bpm.engine.impl.variable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.Map;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.delegate.ProcessEngineVariableType;
import org.camunda.bpm.engine.delegate.SerializedVariableValue;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.core.variable.SerializedVariableValueImpl;
import org.camunda.bpm.engine.impl.persistence.entity.VariableInstanceEntity;
import org.camunda.bpm.engine.impl.util.IoUtil;
import org.camunda.bpm.engine.impl.util.ReflectUtil;

/**
 * @author Tom Baeyens
 */
public class SerializableType extends ByteArrayType {

  public String getTypeName() {
    return ProcessEngineVariableType.SERIALIZABLE.getName();
  }

  public Object getValue(ValueFields valueFields) {
    Object cachedObject = valueFields.getCachedValue();
    if (cachedObject!=null) {
      return cachedObject;
    }

    byte[] bytes = (byte[]) super.getValue(valueFields);
    if(bytes != null) {
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      Object deserializedObject;
      try {
        ObjectInputStream ois = new ClassloaderAwareObjectInputStream(bais);
        deserializedObject = ois.readObject();
        valueFields.setCachedValue(deserializedObject);

        if (valueFields instanceof VariableInstanceEntity) {
          if (Context.getCommandContext() == null) {
            throw new ProcessEngineException("Unable to deserizable variable instance outside of a command context");
          }

          Context
            .getCommandContext()
            .getSession(DeserializedObjectsSession.class)
            .addDeserializedObject(deserializedObject, bytes, (VariableInstanceEntity) valueFields);
        }

      } catch (Exception e) {
        throw new ProcessEngineException("Couldn't deserialize object in variable '"+valueFields.getName()+"'", e);
      } finally {
        IoUtil.closeSilently(bais);
      }
      return deserializedObject;
    } else {
      return null;
    }

  }

  public void setValue(Object value, ValueFields valueFields) {
    byte[] byteArray = serialize(value, valueFields);
    valueFields.setCachedValue(value);

    if(valueFields.getByteArrayValue() == null) {
      if(valueFields instanceof VariableInstanceEntity) {
        Context
          .getCommandContext()
          .getSession(DeserializedObjectsSession.class)
          .addDeserializedObject(valueFields.getCachedValue(), byteArray, (VariableInstanceEntity)valueFields);
      }
    }

    super.setValue(byteArray, valueFields);
  }

  public static byte[] serialize(Object value, ValueFields valueFields) {
    if(value == null) {
      return null;
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream ois = null;
    try {
      ois = new ObjectOutputStream(baos);
      ois.writeObject(value);
    } catch (Exception e) {
      throw new ProcessEngineException("Couldn't serialize value '"+value+"' in variable '"+valueFields.getName()+"'", e);
    } finally {
      IoUtil.closeSilently(ois);
    }
    return baos.toByteArray();
  }

  public boolean isAbleToStore(Object value) {
    return value instanceof Serializable;
  }

  public String getTypeNameForValue(ValueFields valueFields) {
    return Serializable.class.getSimpleName();
  }

  public SerializedVariableValue getSerializedValue(ValueFields valueFields) {
    SerializedVariableValueImpl result = new SerializedVariableValueImpl();
    result.setValue(super.getValue(valueFields));
    return result;
  }

  public void setValueFromSerialized(Object serializedValue, Map<String, Object> configuration, ValueFields valueFields) {
    super.setValue(serializedValue, valueFields);
  }

  public boolean isAbleToStoreSerializedValue(Object value, Map<String, Object> configuration) {
    return super.isAbleToStoreSerializedValue(value, configuration);
  }

  public boolean storesCustomObjects() {
    return true;
  }

  protected static class ClassloaderAwareObjectInputStream extends ObjectInputStream {

    public ClassloaderAwareObjectInputStream(InputStream in) throws IOException {
      super(in);
    }

    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
      return ReflectUtil.loadClass(desc.getName());
    }

  }
}
