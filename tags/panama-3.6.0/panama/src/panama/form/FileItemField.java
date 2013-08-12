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

import org.apache.commons.fileupload.FileItem;



/**
 * Form Field for FileItems.
 * 
 * Does not support string(s)ToValue(s) nor value(s)ToString(s) methods.
 * To use this field:
 * - Add an instance to your Form object (<code>form.addField(new FileItemField("fieldname"));</code>)
 * - Set the FileItemMap of the context as additional input to your formdata, like so:
 *   <code>
 *   formdata.setInput(ctx.getParameterMap());
 *   formdata.setInput(ctx.getFileItemMap());
 *   </code>
 * - Get the value from the formdata with <code>formdata.getFileItem("fieldname");</code>
 * 
 * @author Robert
 */
public class FileItemField extends Field {

	public FileItemField(String name) {
		this(name, false);
	}
	
	public FileItemField(String name, boolean notEmpty) {
		super(name, FileItem.class, notEmpty);
	}
	
	public synchronized Object[] stringsToValues(String[] texts) throws ParseException {
		throw new RuntimeException("Method not supported");
	}

	protected synchronized Object stringToValue(String valueString) throws ParseException {
		throw new RuntimeException("Method not supported");	
	}

	public synchronized String[] valuesToStrings(Object[] values) throws ParseException {
		throw new RuntimeException("Method not supported");
	}
	
	public synchronized String valueToString(Object value) throws ParseException {
		throw new RuntimeException("Method not supported");		
	}
	
	/**
	 * This method returns the value for the field if the string representation is an
	 * empty string.
	 * This method simply returns null; you may overwrite this, if you need some special treatment,
	 * but you must make sure, that you return a value that matches the expected value of your field
	 * @return null
	 */
	protected Object getNullValue() {
		return null;
	}	
}
