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
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Robert
 * 
 */
public class DateField extends Field {

	private String dateFormat = "dd.MM.yyyy";
	
	public DateField(String name) {
		this(name, false);
	}
	
	/**
	 * @param name
	 * @param notEmpty
	 */
	public DateField(String name, boolean notEmpty) {
		this(name, notEmpty, "dd.MM.yyyy");
	}

	/**
	 * @param name
	 * @param notEmpty
	 * @param dateFormat See SimpleDateFormat
	 */
	public DateField(String name, boolean notEmpty, String dateFormat) {
		super(name, Date.class, notEmpty);
		setDateFormat(dateFormat);
	}

	/**
	 * This converts the given string into the value itself
	 * @see Field#stringToValue(String)
	 * @param valueString
	 * @return A Date object
	 * @throws ParseException
	 */
	protected Object stringToValue(String valueString) throws ParseException {
		SimpleDateFormat fmttr = new SimpleDateFormat();
		fmttr.applyPattern(getDateFormat());
		return fmttr.parse(valueString);
	}	

	/**
	 * This converts the given value into a string representation
	 * @see Field#valueToString(Object)
	 * @param value
	 * @return A String
	 * @throws ParseException
	 */	
	public String valueToString(Object value) throws ParseException {
		SimpleDateFormat fmttr = new SimpleDateFormat();
		fmttr.applyPattern(getDateFormat());
		return fmttr.format(value);		
	}
	
	public String getDateFormat() {
		return dateFormat;
	}
	
	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}
}
