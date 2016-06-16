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
package panama.collections.filters;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expression;
import com.avaje.ebean.Query;

/**
 * A Filter encapsulating a Ebean Expression.
 * Only for use with QueryListModels.
 * The Expression is passed to the Constructor and can be arbitrarily complex.
 * 
 * @author ridcully
 */
public class ExpressionFilter extends Filter {

	private static final long serialVersionUID = 1L;

	private Expression expression = null;

	public ExpressionFilter(Expression expression) {
		this.expression = expression;
	}

	@Override
	public Expression asExpression(Query<?> query) {
		return expression;
	}
}
