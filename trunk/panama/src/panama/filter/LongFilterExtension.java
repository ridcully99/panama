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
package panama.filter;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expression;


/**
 * @author Ridcully
 *
 */
public class LongFilterExtension extends DefaultFilterExtension {
	
	public Expression forProperty(String name, String pattern) {
		try {
			return Ebean.getExpressionFactory().eq(name, new Long(pattern));
		} catch (Exception e) {
			return null;
		}
	}
}
