/*
 *  Copyright 2004-2012 Robert Brandner (robert.brandner@gmail.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package panama.form;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.grlea.log.SimpleLogger;

import panama.core.Context;
import panama.persistence.PersistentBean;
import panama.util.DynaBeanUtils;



/**
 * This class defines a set of Fields.
 *
 * @author Robert
 *
 */
public class Form {

	public final static int INCLUDE_PROPERTIES = 1;
	public final static int EXCLUDE_PROPERTIES = 2;

	protected static SimpleLogger log = new SimpleLogger(Form.class);

	protected Map<String, Field> fields = new HashMap<String, Field>();

	public Form(Field... fields) {
		for (Field f : fields) {
			addField(f);
		}
	}

	public Form(Class<?> clazz) {
		this(clazz, EXCLUDE_PROPERTIES);
	}

	public Form(Class<?> clazz, String... properties) {
		this(clazz, INCLUDE_PROPERTIES, properties);
	}

	public Form(Class<?> clazz, int method, String... properties) {
		addFields(clazz, method, properties);
	}

	public Field addField(Field field) {
		fields.put(field.getName(), field);
		return field;
	}

	public Map<String, Field> getFields() {
		return fields;
	}

	/**
	 * Gets Field for specified name.
	 * @param name
	 * @return A Field or <code>null</code> if Form does not contain a Field with specified name.
	 */
	public Field getField(String name) {
		return (Field)fields.get(name);
	}

	/**
	 * Adds fields of correct type for all properties of the specified class.
	 * @see #addFields(Class, int, String...)
	 * @param clazz A bean class.
	 */
	public void addFields(Class<?> clazz) {
		addFields(clazz, EXCLUDE_PROPERTIES);
	}

	/**
	 * Adds fields of correct type for all specified properties of the specified class.
	 * @see #addFields(Class, int, String...)
	 * @param clazz A bean class.
	 * @param properties names of properties
	 */
	public void addFields(Class<?> clazz, String... properties) {
		addFields(clazz, INCLUDE_PROPERTIES, properties);
	}

	/**
	 * Adds fields of correct type for all specified properties of the specified class.
	 *
	 * Supports all Field-Types of the package panama.form.
	 * For other property-values a basic Field is created.
	 *
	 * If properties are not found or some reflections goes wrong, errors are logged at debug-level.
	 * If unknown properties are specified, _no_ fields are added.
	 *
	 * @param clazz A class
	 * @param method what to do with properties: INCLUDE_PROPERTIES: add fields for specified properties, EXCLUDE_PROPERTIES: add fields for all _but_ the specified properties.
	 * @param properties names of properties to be included or excluded
	 */
	public void addFields(Class<?> clazz, int method, String... properties) {
		try {
			Object bean = clazz.newInstance();
			List<String> allProperties = Arrays.asList(DynaBeanUtils.getPropertyNames(bean));
			List<String> props = Arrays.asList(properties == null ? new String[0] : properties);
			if (!allProperties.containsAll(props)) {
				List<String> hlp = new ArrayList<String>(props);
				hlp.removeAll(allProperties);	// remove all existing properties --> the non existing stay in props.
				throw new Exception("Class "+clazz+" does not contain the specified properties "+hlp);
			}
			if (method == EXCLUDE_PROPERTIES) {
				List<String> hlp = new ArrayList<String>(allProperties);	// must create a real list here, the 'asList' arrays do not support the removeAll method
				hlp.removeAll(props);
				props = hlp;
			}
			for (String name : props) {
				Class<?> valueClass = DynaBeanUtils.getPropertyClass(bean, name);
				if (valueClass.isArray()) {									// for arrays we create a Field for it's component type (formdata allows multiple inputs for it then)
					valueClass = valueClass.getComponentType();
				}
				Object valueInstance;
				try {
					valueInstance = valueClass.newInstance();
				} catch (Exception e) {
					valueInstance = null;
				}
				Field field = null;
				if (valueInstance != null && valueInstance instanceof Date) {
					field = new DateField(name);
				} else if (valueInstance != null && valueInstance instanceof PersistentBean) {
					field = new PersistentBeanField(name, (Class<? extends PersistentBean>)valueClass);
				} else {
					field = new Field(name, valueClass);
				}
				addField(field);
			}
		} catch (Exception e) {
			log.error("Error adding fields: "+e.getMessage());
			log.errorException(e);
		}
	}
}
