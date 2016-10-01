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

import java.text.ParseException;

import panama.form.Field;

/**
 * @author robert.brandner
 *
 */
public class EnumField extends Field {

	private Class<? extends Enum<?>> enumClass;

	public EnumField(String name, Class<? extends Enum<?>> enumClass) {
		this(name, enumClass, false);
	}

	public EnumField(String fieldName, Class<? extends Enum<?>> enumClass, boolean notEmpty) {
		super(fieldName, enumClass, notEmpty);
		this.enumClass = enumClass;
	}

	/**
	 * This converts the given string into the value itself
	 * @see Field#stringToValue(String)
	 * @param valueString
	 * @return An Enum value
	 * @throws ParseException
	 */
	protected Object stringToValue(String valueString) throws ParseException {
		for (Enum<?> e : enumClass.getEnumConstants()) {
			if (e.toString().equals(valueString)) return e;
		}
		return null;
	}

	/**
	 * This converts the given value into a string representation
	 * @see Field#valueToString(Object)
	 * @param value
	 * @return A String
	 * @throws ParseException
	 */
	public String valueToString(Object value) throws ParseException {
		return ((Enum<?>)value).toString();
	}
}
