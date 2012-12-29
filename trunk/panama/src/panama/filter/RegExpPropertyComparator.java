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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expression;

/**
 * A Filter using regular expressions.
 * For optimum speed the pattern is precompiled.
 * <strong>IMPORTANT:</strong> As SQL does not know RegExps, the Expression-Representation of this Filter is: '<property> ilike <pattern>'
 * @see java.util.regex.Pattern
 * @author Ridcully
 */
public class RegExpPropertyComparator extends PropertyComparator {

	protected Pattern regExpPattern;
	
	/**
	 */
	public RegExpPropertyComparator(String pattern, int mode, String... properties) {
		this(pattern, Pattern.DOTALL, mode, properties);
	}

	/**
	 */
	public RegExpPropertyComparator(String pattern, int flags, int mode, String... properties) {
		super(pattern, mode, properties);
		regExpPattern = Pattern.compile(pattern, flags);
	}
	
	
	/**
	 * Tests if value matches the regular expression pattern specified in the constructor.
	 * @param name
	 * @param value
	 * @return wether value matches the pattern
	 */
	protected boolean matchProperty(String name, Object value) {
		boolean matches = false;
		if (value != null) {
			Matcher m = regExpPattern.matcher(value.toString());
			matches = m.matches();
		}
		return matches;
	}
	
	/**
	 * Expression for one Property.
	 * Note, that the pattern itself must contain SQL wildcards like % or _ here, whereas the {@link SearchPropertyComparator} automatically encloses the pattern with % wildcards.
	 * @param name
	 * @return ilike(name, pattern) Expression
	 */
	protected Expression forProperty(String name) {
		return Ebean.getExpressionFactory().ilike(name, pattern);
	}	
}
