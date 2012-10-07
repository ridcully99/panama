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

import java.text.ParseException;

import panama.persistence.PersistentBean;


/**
 * A Field representing one or many PersistentBeans.
 * This field can be used to directly work with values of classes derived from PersistentBean.
 * The string-representation in FormData is just the ID of the object (so meaningful data has to be fetched from elsewhere)
 * This field also works with collections, but they have to be converted to/from the arrays used in FormData.
 * Note, that this field accesses the DataBase (via Ebean), so this must be set up and initialized correctly.
 * 
 * @author Ridcully
 *
 */
public class PersistentBeanField extends Field {

	protected Class<? extends PersistentBean> beanClass; 

	public PersistentBeanField(String name, Class<? extends PersistentBean> beanClass) {
		this(name, false, beanClass);
	}	
	
	/**
	 * Constructor of the Field
	 * @param name
	 * @param notEmpty
	 * @param beanClass The concrete class (must be derived of the PersistentBean and mapped in your application)
	 */
	public PersistentBeanField(String name, boolean notEmpty, Class<? extends PersistentBean> beanClass) {
		super(name, beanClass, notEmpty);
		this.beanClass = beanClass;
	}
	
	/**
	 * This converts the given string into the value itself
	 * @see Field#stringToValue(String)
	 * @param valueString the id of an PersistentBean
	 * @return The PersistentBean (or an object of beanClass if specified by Constructor) for the specified id, or a new object if no persistent object for the id is found 
	 * @throws ParseException if something goes wrong
	 */
	protected Object stringToValue(String valueString) throws ParseException {
		try {
			return PersistentBean.findOrCreate(beanClass, valueString);
		} catch (Exception e) {
			throw new ParseException(e.getMessage(), 0);
		}
	}	

	/**
	 * This converts the given value into a string representation
	 * @see Field#valueToString(Object)
	 * @param value
	 * @return The ID of value
	 * @throws ParseException
	 */	
	public String valueToString(PersistentBean value) throws ParseException {
		return value.getId();
	}	
}
