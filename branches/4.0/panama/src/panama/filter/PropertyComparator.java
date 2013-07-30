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

import java.util.Arrays;
import java.util.List;

import panama.util.DynaBeanUtils;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expression;
import com.avaje.ebean.Junction;
import com.avaje.ebean.Query;


/**
 * A Filter that checks if all, any or none of the specified properties match the specified pattern.
 * The comparation is done exactly and is on String basis (toString() is used on all properties).
 * This class is the base class for other Filters with more enhanced matching methods (RegExp, Search, ...)
 *
 * @author Ridcully
 */
public class PropertyComparator extends Filter {

	private static final long serialVersionUID = 1L;

	/**
	 * Allowed values for propertiesMode parameter of constructors.
	 */
	public final static int ALL_PROPERTIES = 1;
	public final static int ANY_PROPERTIES = 2;
	public final static int NO_PROPERTIES = 3;

	protected List<String> properties;
	protected String pattern;
	protected int mode;

	/**
	 * @param properties
	 * @param mode
	 * @param pattern
	 */
	public PropertyComparator(String pattern, int mode, String... properties) {
		this.properties = properties == null || properties.length == 0 ? null : Arrays.asList(properties);
		this.pattern = pattern == null || pattern.equals("") ? null : pattern;
		this.mode = mode >= ALL_PROPERTIES && mode <= NO_PROPERTIES ? mode : ANY_PROPERTIES;
	}

	/**
	 * Tests specified properties
	 */
	@Override
	public boolean match(Object object) {
		if (object == null || properties == null || pattern == null) {
			return true;
		}
		boolean all = true;
		boolean any = false;
		for (String name : properties) {
			boolean match = false;
			try {
				Object value = DynaBeanUtils.getProperty(object, name);
				FilterExtension extension = extensions.get(name);
				match = extension == null ? matchProperty(name, value) : extension.matchProperty(name, value, pattern);
				all = all && match;
				any = any || match;
				if (match && mode == NO_PROPERTIES) { return false; }
				if (match && mode == ANY_PROPERTIES) { return true; }
				if (!match && mode == ALL_PROPERTIES) { return false; }
			} catch (Exception e) {
				/* NOP */
			}
		}
		return (all && mode == ALL_PROPERTIES) || (any && mode == ANY_PROPERTIES) || ((!any) && mode == NO_PROPERTIES);
	}

	public String toString() {
		return pattern;
	}

	/**
	 * Tests one property - this method may be overwritten by extending classes.
	 * @param name
	 * @param value
	 * @return wether value matches the pattern
	 */
	protected boolean matchProperty(String name, Object value) {
		return value == null ? false : pattern.equals(value.toString());
	}

	/**
	 * Creates representation of the Comparator for use with Ebean's Expression model.
	 * Used by QueryListModel.
	 * This method uses the forProperty() method and joins the results of that method
	 * depending on this.mode.
	 * @param filterExtensions an optional Map of FilterExtensions to tweak the default behaviour of the filter for each property separately.
	 * @return Some sort of Expression representing the Filter.
	 */
	@Override
	public Expression asExpression(Query<?> query) {
		Expression result = null;
		if (getMode() == ALL_PROPERTIES) {
			result = Ebean.getExpressionFactory().conjunction(query);
		} else { // for NO_PROPERTIES we do a disjunction and at the end negate it -> NOT (A or B or C)
			result = Ebean.getExpressionFactory().disjunction(query);
		}
		for (String property : getProperties()) {
			FilterExtension extension = extensions.get(property);
			Expression exp = extension == null ? forProperty(property) : extension.forProperty(property, pattern);
			if (exp != null) {
				((Junction<?>)result).add(exp);
			}
		}
		if (getMode() == NO_PROPERTIES) {
			result = Ebean.getExpressionFactory().not(result);
		}
		return result;
	}

	/**
	 * Expression for one Property
	 * @param name
	 * @return eq(name, pattern)
	 */
	protected Expression forProperty(String name) {
		return Ebean.getExpressionFactory().eq(name, pattern);
	}

	public int getMode() {
		return mode;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public List<String> getProperties() {
		return properties;
	}

	public void setProperties(List<String> properties) {
		this.properties = properties;
	}
}
