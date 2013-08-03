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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.DefaultFormatter;


import panama.exceptions.ValidatorException;
import panama.log.SimpleLogger;
import panama.util.DynaBeanUtils;



/**
 * Represents a field in a form
 * Supports multi-value
 * Supports pluggable validators
 *
 * @author Robert
 *
 */
public class Field {

	private String name;
	private List<Validator> validators = new ArrayList<Validator>();
	private Class<?> valueClass;
	private DefaultFormatter fmttr = new DefaultFormatter();
	protected static SimpleLogger log = new SimpleLogger(Form.class);


	protected Field() {}

	protected Field(String name, Class<?> valueClass) {
		this(name, valueClass, false);
	}

	protected Field(String name, Class<?> valueClass, boolean notEmpty) {
		setName(name);
		setValueClass(valueClass);
		if (notEmpty) {
			addValidator(ValidatorFactory.getNotEmptyValidator());
		}
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Class<?> getValueClass() {
		return valueClass;
	}

	public void setValueClass(Class<?> valueClass) {
		this.valueClass = valueClass;
	}

	/**
	 * Adds validator to validator-list
	 * @param validator
	 * @return the Field itself, to allow fluid adding of more than one validator
	 */
	public Field addValidator(Validator validator) {
		validators.add(validator);
		return this;
	}

	/**
	 * Adds validator at given position to validator-list
	 * @param position
	 * @param validator
	 * @return the Field itself, to allow fluid adding of more than one validator.
	 */
	public Field addValidator(int position, Validator validator) {
		validators.add(position, validator);
		return this;
	}

	/**
	 * Returns the list of validators of this field.
	 * @return a list of validators, or an empty list, if the field has no associated validators.
	 */
	public List<Validator> getValidators() {
		return validators;
	}

	/**
	 * This converts the given value strings into the values itself
	 * @param texts
	 * @return Array of objects
	 * @throws ParseException
	 */
	public synchronized Object[] stringsToValues(String[] texts) throws ParseException {
		Object[] result = new Object[texts.length];
		for (int i=0; i<result.length; i++) {
			if (texts[i].trim().length()==0) {
				result[i] = getNullValue();
			} else {
				String text = texts[i];
				result[i] = stringToValue(text);
			}
		}
		return result;
	}

	/**
	 * This converts one valuestring into the value object itself.
	 *
	 * Overwrite this for special Fields (like Date etc.)
	 *
	 * <b>Make sure your version of this method matches your version of valueToString()</b>
	 *
	 * @param valueString
	 * @return The value as an object
	 */
	protected synchronized Object stringToValue(String valueString) throws ParseException {
		if (getValueClass().isPrimitive()) {
			try {
				return DynaBeanUtils.parsePrimitive(getValueClass(), valueString);
			} catch (Exception nfe) {
				throw new ParseException(nfe.getMessage(), 0);
			}
		} else {
			fmttr.setValueClass(getValueClass());
			return fmttr.stringToValue(valueString);
		}
	}

	/**
	 * This converts an array of values into an array of strings the field would accept
	 *
	 * @param values an array of values
	 * @return An array of string representations of the given values
	 */
	public synchronized String[] valuesToStrings(Object[] values) throws ParseException {
		if (values == null) { return null; }
		String[] result = new String[values.length];
		for (int i=0; i<values.length; i++) {
			result[i] = valueToString(values[i]);
		}
		return result;
	}

	/**
	 * This converts a value into a string the field would accept
	 *
	 * Overwrite this for special Fields (like Date etc.)
	 * <b>Make sure your version of this method matches your version of stringToValue()</b>
	 *
	 * @param value
	 * @return A string representation of the given value
	 */
	public synchronized String valueToString(Object value) throws ParseException {
		fmttr.setValueClass(getValueClass());
		return fmttr.valueToString(value);
	}

	/**
	 * This validates the given values with all validators of this field
	 *
	 * @param values
	 * @throws ValidatorException
	 */
	public synchronized void validate(Object[] values) throws ValidatorException {
		if ((values == null || values.length == 0)
				&& validators.contains(ValidatorFactory.getNotEmptyValidator())) {
			ValidatorFactory.getNotEmptyValidator().validate(null);	// throws Exception
		}
		for (int i=0; i<values.length; i++) {
			for (Validator v : validators) {
				v.validate(values[i]);	// throws Exception when invalid
			}
		}
	}

	/**
	 * This method returns the value for the field if the string representation is an
	 * empty string.
	 * This method simply returns null; you may overwrite this, if you need some special treatment,
	 * but you must make sure, that you return a value that matches the expected value of your field
	 * @return null
	 */
	protected Object getNullValue() {
		if (getValueClass().isPrimitive()) {
			return DynaBeanUtils.getNullValueForPrimitive(getValueClass());
		} else {
			return null;
		}
	}
}
