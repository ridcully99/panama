/*
 *  Copyright 2004-2016 Robert Brandner (robert.brandner@gmail.com) 
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

import java.util.Date;

import panama.persistence.PersistentBean;

/**
 * Creates fields for all supported various value classes.
 *
 * TODO Nicht fertig -- in {@link Form#addFields(Class, String...)} einsetzen; auf erlaubte valueclasses prüfen ...
 *
 * @author ridcully
 *
 */
public class FieldFactory {

	/** Do not allow instantiation */
	private FieldFactory() {}

	/**
	 * Builds a field with given name that's able to handle given value class.
	 *
	 * @param name
	 * @param valueClass
	 * @return
	 */
	public Field buildField(String name, Class<?> valueClass) {
		// for arrays we create a Field for it's component type (formdata allows multiple inputs for it then)
		if (valueClass.isArray()) {
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
		return field;
	}
}
