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

import java.util.regex.Pattern;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expression;

/**
 * The Filter used for Table searching.
 * Basically it matches .*pattern.* and it is case insensitive.
 * @author Ridcully
 */
public class SearchPropertyComparator extends RegExpPropertyComparator {
	
	/**
	 * the specified pattern is quoted, so regular expressions in it are treated as normal text.
	 */
	public SearchPropertyComparator(String pattern, int mode, String... properties) {
		super("", mode, properties); // super compiles the pattern - to avoid errors, we give it an empty pattern here
		setPattern(pattern);		 // and set the pattern-string here (it is required in it's original form by the forProperty() method
		pattern = pattern.replace("\\Q", "\\\\q");	// quote the quoters
		pattern = pattern.replace("\\E", "\\\\e");	// quote the quoters
		pattern = "^.*\\Q"+pattern+"\\E.*$";
		regExpPattern = Pattern.compile(pattern, Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	}
	
	/**
	 * Expression for one Property: ILIKE '%pattern%'
	 * @param name
	 * @return an Expression object
	 */
	protected Expression forProperty(String name) {
		return Ebean.getExpressionFactory().ilike(name, "%"+pattern+"%");
	}
}
