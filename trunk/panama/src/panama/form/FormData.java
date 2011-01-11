/*
 *  Copyright 2004-2010 Robert Brandner (robert.brandner@gmail.com) 
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

import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.fileupload.FileItem;
import org.grlea.log.SimpleLogger;

import panama.exceptions.NoSuchFieldException;
import panama.exceptions.ValidatorException;
import panama.util.DynaBeanUtils;


/**
 * This class represents the data contained in the form passed to the constructor.
 * The form itself is just a description of a form and it's field and resides in the
 * application scope. It must never be changed by this class.
 * 
 * This FormData class contains the data and normally resides in request or session scope. 
 * 
 * Validation is done during getting the values from the form.
 * 
 * This class does not support Multipart Fileuploads, instead, 
 * please use the FileItem methods of Context class to access uploaded files. 
 * 
 * This class provides getters for common types. On error (cast etc.) they simply return null
 * For how to get values of other types
 * @see FormData#getValue(String)
 * @see FormData#getValues(String)
 * 
 * @author Ridcully
 *
 */
public class FormData {

	private Map<String, Object> input = new HashMap<String, Object>();
	private Map<String, ValidatorException> errors = new HashMap<String, ValidatorException>();
	private Form form;
	
	protected static SimpleLogger log = new SimpleLogger(Form.class);		

	public FormData(Form form) {
		this(form, null);
	}
	
	public FormData(Form form, Map inputMap) {
		this.form = form;
		// default inputs for Boolean Fields to FALSE (as we only get TRUE from forms)
		if (form != null) {
			for (Map.Entry<String, Field> e : form.getFields().entrySet()) {
				if (e.getValue() instanceof BooleanField) {
					setInput(e.getKey().toString(), Boolean.FALSE);
				}
			}
		}
		if (inputMap != null) {
			setInput(inputMap);
		}
	}

	/**
	 * Sets the form's input from the specified map.
	 * Does not touch values of fields not in map (useful if there are more form-fields
	 * than parameters from request (e.g. a wizard could have 1 large form across several
	 * pages, each of which gives just some values)
	 * The keys of the parameters map are the names of the fields
	 * The values of the map may be string, object, string[] or object[]
	 * String[] arrays as coming from request (even if there's only one value)
	 *
	 * @see #setInput(String, Object)
	 * 
	 * @param inputMap 
	 */
	@SuppressWarnings("unchecked")
	public void setInput(Map inputMap) {
		if (inputMap != null) {
			for (Object e : inputMap.entrySet()) {
				setInput(((Map.Entry)e).getKey().toString(), ((Map.Entry)e).getValue());
			}
		}
	}

	/**
	 * Sets the form's input from the specified bean.
	 * 
	 * @param bean The bean from which the values are fetched
	 */
	public void setInput(Object bean) {
		for (String fieldName : form.getFields().keySet()) {
			try {
				Object value = DynaBeanUtils.getProperty(bean, fieldName);
				setInput(fieldName, value);
			} catch (Exception e) {	// PropertyNotFoundException etc.
				/* simply do not set a value for this field in input map */
			}
		}
	}
	
	/**
	 * Sets the input value for one field
	 * 
	 * If value is an object or array of objects, the objects are converted to strings
	 * using the valueToString method of the field
	 * 
	 * Note, that the value is not validated here and now, but just when it is fetched using
	 * one of the getXXX() or the getValue() or getValues() methods.
	 * 
	 * @param fieldName
	 * @param value may be single string or object or an array of strings or objects
	 */
	public void setInput(String fieldName, Object value) {
		// convert value into array (String[] or Object[] if it is not already an array
		if (value instanceof String) {
			value = new String[] {(String)value};
		} else if (!(value instanceof String[]) && !(value instanceof Object[])) {
			value = new Object[] {value};
		}

		if (!(value instanceof String[])) {
			Field f = (Field)form.getFields().get(fieldName);
			if (f != null) {
				try {
					input.put(fieldName, f.valuesToStrings((Object[])value)); 
				} catch (Exception ex) {
					input.put(fieldName, value); // could not convert, leave as is
				}
			} else { /* no field with specified name, store nevertheless (possibly part of a multipart-field */
				input.put(fieldName, value);
			}
		} else {
			input.put(fieldName, value);
		}
	}	

	// ----------------------------------------------------------------------------

	/**
	 * Applies current input-data to the specified bean.
	 * The field names must match the property names for this method to work as expected.
	 * If unknown properties are encountered, _no_ values are set (an error message is logged at debug-level)
	 * If wrong property-types are encountered, or fields are missing for property-names a message is logged at debug-level.
	 * @param bean The bean, the current input-data will be applied to.
	 */
	public void applyTo(Object bean) {
		applyTo(bean, null, Form.EXCLUDE_PROPERTIES);
	}
	
	/**
	 * Applies current input-data to the specified properties of the specified bean.
	 * The field names must match the property names for this method to work as expected.
	 * If unknown properties are encountered, _no_ values are set (an error message is logged at debug-level)
	 * If wrong property-types are encountered, or fields are missing for property-names a message is logged at debug-level.
	 * @param bean The bean, the input should be applied to.
	 * @param properties the properties to be set
	 */
	public void applyTo(Object bean, String[] properties) {
		applyTo(bean, properties, Form.INCLUDE_PROPERTIES);
	}	

	/**
	 * Applies current input-data to the specified or all but the properties of the specified bean.
	 * The field names must match the property names for this method to work as expected.
	 * If unknown properties are encountered, _no_ values are set (an error message is logged at debug-level)
	 * If wrong property-types are encountered, or fields are missing for property-names a message is logged at debug-level.
	 * If a property is annotated as javax.persistence.Version it is NOT set!
	 * 
	 * @param bean The bean, the input should be applied to.
	 * @param properties the properties to be set or to be skipped, depending on method
	 * @param method wether to set the specified or all _but_ the specified properties. Value should be one of Form.INCLUDE_PROPERTIES or Form.EXCLUDE_PROPERTIES 
	 */
	public void applyTo(Object bean, String[] properties, int method) {
		List<String> allProperties = Arrays.asList(DynaBeanUtils.getPropertyNames(bean));
		List<String> props = Arrays.asList(properties == null ? new String[0] : properties);
		if (!allProperties.containsAll(props)) {
			List<String> hlp = new ArrayList<String>(props); 	// must create a real list here, the 'asList' arrays do not support the removeAll method
			hlp.removeAll(allProperties);						// remove all existing properties --> the non existing stay in props.
			log.debug("Class "+bean.getClass()+" does not contain the specified properties "+hlp);
			return;
		}
		if (method == Form.EXCLUDE_PROPERTIES) {
			List<String> hlp = new ArrayList<String>(allProperties);	// must create a real list here, the 'asList' arrays do not support the removeAll method
			hlp.removeAll(props);
			props = hlp;
		}
		for (String propertyName : props) {
			try {
				Class<?> propertyClass = DynaBeanUtils.getPropertyClass(bean, propertyName);
				if (propertyClass.isArray()) {
					// got to create an array of appropriate type
					Object[] values = getValues(propertyName);
					Object arr = Array.newInstance(propertyClass.getComponentType(), values.length);
					System.arraycopy(values, 0, arr, 0, values.length);
					DynaBeanUtils.setProperty(bean, propertyName, arr);
				} else {
					DynaBeanUtils.setProperty(bean, propertyName, getValue(propertyName));
				}
			} catch (Exception e) {
				/* NOP - continue; the error is already added to the errors-list. */
			}  
		}
	}
	
	// ----------------------------------------------------------------------------
	// getters for common types - on error (cast etc.) they simply return null
	// for how to get values of other types
	// @see FormData#getValue()
	// @see FormData#getValues()
	// ----------------------------------------------------------------------------
	
	public String getString(String fieldName) {
		try {
			return (String)getValue(fieldName);
		} catch (Exception e) {
			return null;
		}
	}

	public Integer getInteger(String fieldName) {
		try {
			return (Integer)getValue(fieldName);
		} catch (Exception e) {
			return null;
		}
	}

	public Long getLong(String fieldName) {
		try {
			return (Long)getValue(fieldName);
		} catch (Exception e) {
			return null;
		}
	}
	
	public Float getFloat(String fieldName) {
		try {
			return (Float)getValue(fieldName);
		} catch (Exception e) {
			return null;
		}
	}

	public Double getDouble(String fieldName) {
		try {
			return (Double)getValue(fieldName);
		} catch (Exception e) {
			return null;
		}
	}
		
	public Date getDate(String fieldName) {
		try {
			return (Date)getValue(fieldName);
		} catch (Exception e) {
			return null;
		}		
	}
	
	public Boolean getBoolean(String fieldName) {
		try {
			return (Boolean)getValue(fieldName);
		} catch (Exception e) {
			return null;
		}		
	}
	
	public FileItem getFileItem(String fieldName) {
		try {
			return (FileItem)getValue(fieldName);
		} catch (Exception e) {
			return null;
		}		
	}
	
	public String[] getStrings(String fieldName) {
		try {
			return (String[])castArrayType(String.class, getValues(fieldName));
		} catch (Exception e) {
			return null;
		}
	}

	public Integer[] getIntegers(String fieldName) {
		try {
			return (Integer[])castArrayType(Integer.class, getValues(fieldName));
		} catch (Exception e) {
			return null;
		}
	}

	public Long[] getLongs(String fieldName) {
		try {
			return (Long[])castArrayType(Long.class, getValues(fieldName));
		} catch (Exception e) {
			return null;
		}
	}
	
	public Float[] getFloats(String fieldName) {
		try {
			return (Float[])castArrayType(Float.class, getValues(fieldName));
		} catch (Exception e) {
			return null;
		}
	}

	public Double[] getDoubles(String fieldName) {
		try {
			return (Double[])castArrayType(Double.class, getValues(fieldName));
		} catch (Exception e) {
			return null;
		}
	}
		
	public Date[] getDates(String fieldName) {
		try {
			return (Date[])castArrayType(Date.class, getValues(fieldName));
		} catch (Exception e) {
			return null;
		}		
	}
	
	public Boolean[] getBooleans(String fieldName) {
		try {
			return (Boolean[])castArrayType(Boolean.class, getValues(fieldName));
		} catch (Exception e) {
			return null;
		}		
	}
	
	public FileItem[] getFileItems(String fieldName) {
		try {
			return (FileItem[])castArrayType(FileItem.class, getValues(fieldName));
		} catch (Exception e) {
			return null;
		}		
	}
	
	/* converts Object[] array to array of specified componentType */
	private Object castArrayType(Class<?> componentType, Object[] array) {
		Object castArray = Array.newInstance(componentType, array.length);
		System.arraycopy(array, 0, castArray, 0, array.length);
		return castArray;
	}		
	
	/**
	 * Gets the raw value object for the specified field.
	 * @param fieldName
	 * @return Raw value object as a single object.
	 * @throws NoSuchFieldException
	 */
	public Object getValue(String fieldName) throws NoSuchFieldException {		
		Field field = (Field)form.getFields().get(fieldName);
		if (field == null) {
			throw new NoSuchFieldException("No field named '"+fieldName+"'");
		}
		Object[] values = validateAndParse(field, input.get(fieldName));
		return (values != null && values.length > 0) ? values[0] : null;
	}

	/**
	 * Gets the raw value objects for the specified field.
	 * @param fieldName
	 * @return Raw value objects as object array.
	 * @throws NoSuchFieldException
	 */	
	public Object[] getValues(String fieldName) throws NoSuchFieldException {		
		Field field = (Field)form.getFields().get(fieldName);
		if (field == null) {
			throw new NoSuchFieldException("No field named '"+fieldName+"'");
		}
		Object[] values = validateAndParse(field, input.get(fieldName));	
		return values;
	}

	/**
	 * Gets the raw value objects for the specified field.
	 * @param fieldName
	 * @return Raw value objects as list or an empty list
	 * @throws NoSuchFieldException
	 */
	public List<Object> getValuesAsList(String fieldName) throws NoSuchFieldException {
		Object[] values = getValues(fieldName);
		List<Object> result = new ArrayList<Object>();
		if (values != null) {
			// add only not null values -- getValues returns [null] as a result for values not in input.
			for (int i=0; i<values.length; i++) {
				if (values[i] != null) {
					result.add(values[i]);
				}
			}
		}
		return result;
	}

	/**
	 * Gets the raw value objects for the specified field.
	 * @param fieldName
	 * @return Raw value objects as set or an empty set
	 * @throws NoSuchFieldException
	 */
	public Set<Object> getValuesAsSet(String fieldName) throws NoSuchFieldException {
		Object[] values = getValues(fieldName);
		Set<Object> result = new HashSet<Object>();
		if (values != null) {
			// add only not null values -- getValues returns [null] as a result for values not in input.
			for (int i=0; i<values.length; i++) {
				if (values[i] != null) {
					result.add(values[i]);
				}
			}
		}
		return result;
	}	
	
	/**
	 * Sets value(s) for specified field from input map
	 * This invokes the fields validators, validation errors are added to error map
	 * 
	 * @param f
	 * @param values An array of strings, array of objects or null
	 */
	private Object[] validateAndParse(Field f, Object values) {
		//Object value = input.get(f.getName()); // array of Strings or objects or null
		try {
			if (values == null) {
				return validateAndParseValues(f, new Object[] {});	// 2009-11-05: was {null}
			}
			else if (values instanceof String[]) {
				return validateAndParseTexts(f, (String[])values);
			} else {
				return validateAndParseValues(f, (Object[])values);					
			}
		} catch (ValidatorException ve) {
			errors.put(f.getName(), ve);
			return (Object[])values;
		}				
	}	
	
	/**
	 * Sets values of this field after parsing and validating the values
	 * @param texts An array of Strings (most of the time just one element)
	 */
	private Object[] validateAndParseTexts(Field f, String[] texts) throws ValidatorException {
		try {
			Object[] values = f.stringsToValues(texts);
			return validateAndParseValues(f, values);
		} catch (ParseException pe) {
			throw new ValidatorException(Validator.PARSING_FAILED, pe);
		}
	}

	/**
	 * Sets values of this field after validating
	 * @param values An array of Objects (most of the time just one element)
	 */
	private Object[] validateAndParseValues(Field f, Object[] values) throws ValidatorException {
		f.validate(values);	// throws ValidatorException on error
		return values;
	}	
	
	// ----------------------------------------------------------------------------
	
	/**
	 * Gets the original input for the specified field.
	 * Useful to fill a field where validation failed.
	 * 
	 * Intentionally named just get() as this allows 
	 * to use $formdata.fieldName in velocity which invokes 
	 * this method as form.get('fieldName')
	 * 
	 * If (as it is most of the time) there is just one value, the array is flattened
	 * 
	 * @param fieldName
	 * @return the original input
	 */
	public Object get(String fieldName) {
		Object[] o = (Object[])input.get(fieldName);
		if (o == null) { return null; }
		if (o.length == 1) {
			return o[0];
		} else {
			return o;
		}
	}
	
	/**
	 * Provides a copy of the input map.
	 * To be used in JSPs with JSTL as ${formdata.input["fieldName"]}
	 */
	public Map getInput() {
		Map m = new HashMap();
		for (String k : input.keySet()) {
			Object value = input.get(k);
			if (value != null) {
				if (((Object[])value).length == 1) {
					value = ((Object[])value)[0];
				} else if (((Object[])value).length == 0) {
					value = null;
				}
			}
			m.put(k, value);
		}
		return m;
	}
	
	/**
	 * Gets the original input for the specified field.
	 * This method does not flatten the input array if there's only one value.
	 * Useful if you need to iterate over the input, regardless whether it has one or more values.
	 * 
	 * @param fieldName
	 * @return the original input
	 */
	public Object[] getInput(String fieldName) {
		Object[] o = (Object[])input.get(fieldName);
		if (o == null) { o = new Object[0]; }
		return o;
	}	
	
	// ----------------------------------------------------------------------------
	
	/**
	 * Gets map of errors, key == fieldname, value = ValidatorException
	 * @return A map of errors or an empty map
	 */
	public Map<String, ValidatorException> getErrors() {
		return errors;
	}
	
	/**
	 * Checks if there are errors
	 * @return true if there are errors, false otherwise
	 */
	public boolean hasErrors() {
		return errors.size() > 0;
	}
	
	// ----------------------------------------------------------------------------
	// Helper methods
	// ----------------------------------------------------------------------------
	
	public void clearInput() {
		input = new HashMap<String, Object>();
	}
	
	/**
	 * Clears error map
	 */
	public void clearErrors() {
		errors = new HashMap<String, ValidatorException>();		
	}
}
