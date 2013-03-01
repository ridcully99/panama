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

import panama.core.Dispatcher;
import panama.exceptions.ValidatorException;

/**
 * @author Robert
 *
 */
public interface Validator {

	public final static String PARSING_FAILED = Dispatcher.PREFIX+"_validator_parsing_failed";
	public final static String NOTEMPTY_VALIDATION_FAILED = Dispatcher.PREFIX+"_validator_notempty_failed";
	public final static String EMAIL_VALIDATION_FAILED = Dispatcher.PREFIX+"_validator_email_failed";
	public final static String URL_VALIDATION_FAILED = Dispatcher.PREFIX+"_validator_url_failed";

	/**
	 * This method is to validate the given value
	 * Make sure to make it synchronized.
	 *
	 * @param value
	 * @throws ValidatorException
	 */
	public void validate(Object value) throws ValidatorException;
}
