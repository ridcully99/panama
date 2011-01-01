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

import panama.exceptions.ValidatorException;

/**
 * A factory for frequently used validators.
 * Keeps a single instance of each validator
 * 
 * @author Robert
 */
public class ValidatorFactory {
	
	private static Validator notEmptyValidator = new Validator() {
		public synchronized void validate(Object value) throws ValidatorException {
			if (value == null || value.toString().trim().length() == 0) {
				throw new ValidatorException(Validator.NOTEMPTY_VALIDATION_FAILED);
			}
		}			
	};
	
	private static Validator emailValidator = new Validator() {
		
		private final static String EMAIL_PATTERN = "^[a-z0-9\\-_.]+@[a-z0-9\\-_.]+\\.[a-z]+$";
		
		public synchronized void validate(Object value) throws ValidatorException {
			if (value != null && !value.toString().toLowerCase().matches(EMAIL_PATTERN)) {
				throw new ValidatorException(Validator.EMAIL_VALIDATION_FAILED);
			}
		}		
	};

	private static Validator urlValidator = new Validator() {

		private final static String URL_PATTERN = "^(http|https)://.+";
		
		public synchronized void validate(Object value) throws ValidatorException {
			if (value != null && !value.toString().toLowerCase().matches(URL_PATTERN)) {
				throw new ValidatorException(Validator.URL_VALIDATION_FAILED);
			}
		}		
	};
	
	public static Validator getNotEmptyValidator() {
		return notEmptyValidator;
	}
	
	public static Validator getEmailValidator() {
		return emailValidator;
	}

	public static Validator getUrlValidator() {
		return urlValidator;
	}
}
