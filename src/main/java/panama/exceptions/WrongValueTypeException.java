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
package panama.exceptions;

/**
 * @author Robert
 * 
 */
public class WrongValueTypeException extends RuntimeException {

	private String propertyName;
	private Class objectClass;
	private Class expectedType;
	
	public WrongValueTypeException(Class objectClass, String propertyName, Class expectedType) {
		super();
		this.objectClass = objectClass;
		this.propertyName = propertyName;
		this.expectedType = expectedType;
	}
	
	public String toString() {
		return "Property '"+propertyName+"' of class '"+objectClass+"' is of type '"+expectedType+"'.";
	}
}


