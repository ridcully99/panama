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

import panama.persistence.PersistentBean;
import panama.util.DynaBeanUtils;



/**
 * This class defines a set of Fields.
 *
 * @author Robert
 *
 */
public class Form {

	protected static SimpleLogger log = new SimpleLogger(Form.class);

	protected Map<String, Field> fields = new HashMap<String, Field>();

	/**
	 * Default constructor, creates a form without any fields.
	 */
	public Form() {
	}
	
	/**
	 * Creates new Form object and adds all specified fields
	 * @param fields
	 */
	public Form(Field... fields) {
		for (Field f : fields) {
			addField(f);
		}
	}
	
	/**
	 * Creates new form object and adds fields of correct type 
	 * for all specified properties of the specified class.
	 *
	 * See {@link addFields(Class, String...)}
	 * 
	 * @param clazz
	 */
	public Form(Class<?> clazz, String... properties) {
		addFields(clazz, properties);
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
	
	public Field addField(Field field) {
		fields.put(field.getName(), field);
		return field;
	}

	/**
	 * Adds fields of correct type for all specified properties of the specified class.
	 * If no properties are specified, fields for _all_ properties of the specified class are added.
	 * Unknown properties are logged at warn-level and skipped.
	 * 
	 * Supports all Field-Types of the package panama.form.
	 * For other property-values a basic Field is created.
	 *
	 * @param clazz A bean class.
	 * @param properties names of properties; omit (or provide null) to add fields for _all_ properties
	 * @return the Form object (for fluid programming)
	 */
	public Form addFields(Class<?> clazz, String... properties) {
		try {
			Object bean = clazz.newInstance();
			List<String> allProperties = Arrays.asList(DynaBeanUtils.getPropertyNames(bean));
			List<String> props = new ArrayList<String>();
			if (properties == null || properties.length == 0) {
				props.addAll(allProperties);
			} else {
				props.addAll(Arrays.asList(properties));
			}
			for (String name : props) {
				if (!allProperties.contains(name)) {
					log.warn("Skipping property "+name+" as class "+clazz.getName()+" does not have such a property.");
					continue;
				}
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
			throw new RuntimeException("Adding fields to form failed", e);
		}
		return this;
	}
		
	/**
	 * Removes specified fields from the Form's list of Fields.
	 * If the form does not contain a specified field, that field is ignored.
	 * Used for fluid Form construction with new Form(Class).except(...)
	 * 
	 * @param fieldNames names of fields to be removed
	 * @return the Form object (for fluid programming)
	 */
	public Form except(String... fieldNames) {
		if( fieldNames != null) {
			for (String name : fieldNames) {
				fields.remove(name);
			}
		}
		return this;
	}
}
