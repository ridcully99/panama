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
package panama.filter;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expression;
import com.avaje.ebean.ExpressionFactory;
import com.avaje.ebean.ExpressionList;

/**
 * Special filter treatment.
 * @author Ridcully
 *
 */
public class DefaultFilterExtension implements FilterExtension {

	public boolean matchProperty(String name, Object value, String pattern) {
		return value == null ? false : pattern.equals(value.toString());		
	}
	
	public Expression forProperty(String name, String pattern) {
		return Ebean.getExpressionFactory().eq(name, pattern);
	}
	
}
