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
package panama.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import panama.annotations.Controller;
import panama.collections.Table;
import panama.collections.Tree;
import panama.exceptions.AuthorizationException;
import panama.exceptions.ForceTargetException;
import panama.exceptions.HttpErrorException;
import panama.exceptions.NoSuchActionException;
import panama.log.SimpleLogger;



/**
 * Base class for all controllers.
 *
 * @author Robert
 */
public class BaseController {

	/** Key used to store DefaultTable-Map in Session-Scope */
	public final static String TABLEMAP_KEY = Dispatcher.PREFIX + "tablemap";

	/** Key used to store DefaultTree-Map in Session-Scope */
	public final static String TREEMAP_KEY = Dispatcher.PREFIX + "treemap";


	/** Logging */
	protected static SimpleLogger log = new SimpleLogger(BaseController.class);

	/** Current context for convinience */
	protected final Context context = Context.getInstance();

	/**
	 * This method is invoked every time, before an action of this controller is
	 * invoked by the dispatcher.
	 * Default implemention does nothing.
	 * Useful to overwrite and put e.g. needed things into context or do authorization
	 *
	 * @param actionName The name of the action to be invoked later on
	 * @throws ForceTargetException If necessary, this method may throw this exception to force a specific target (e.g. for checking login)
	 * @throws AuthorizationException If necessary, this method may throw this exception to force a HTTP-Response of 403 (forbidden)
	 */
	public void beforeAction(String actionName) throws ForceTargetException, AuthorizationException {
	}

	/**
	 * This method is invoked every time, after an action of this controller has been.
	 * invoked by the dispatcher.
	 * Default implementation does nothing.
	 * Overwrite it if you need things to be done after most or all of your actions.
	 *
	 * @param actionName The name of the action to be invoked later on
	 * @param target The result of the action
	 * @throws ForceTargetException If necessary, this method may throw this exception to force a specific target (e.g. for checking login)
	 */
	public void afterAction(String actionName, Target target) throws ForceTargetException {
	}

	/**
	 * Defines a redirect to other url (relative or absolute).
	 *
	 * Sends a temporary redirect response to the client using the specified redirect location
	 * URL. This method can accept relative URLs; the servlet container must convert the
	 * relative URL to an absolute URL before sending the response to the client.
	 * If the location is relative without a leading slash (/) the container interprets it as
	 * relative to the current request URI.
	 * If the location is relative with a leading slash (/) the container interprets it as relative to the webapp.
	 *
	 * To redirect to other action use {@link #redirectToAction(String, String...)} or {@link #redirectToAction(Class, String, String...)}
	 *
	 * Note: Use redirect only if you need the client browser's URL to have a new URL.
	 *
	 * @param url
	 * @return A Target object
	 */
	public RedirectTarget redirect(String url) {
		RedirectTarget t = new RedirectTarget(url);
		return t;
	}

	/**
	 * Defines a redirect to other action.
	 *
	 * Sends a temporary redirect response to the client using a URL created from the
	 * current Controller and the specified action and parameters
	 * The URL has the form: ../controllerName/action?param1=value1&param2=value2...
	 *
	 * The parameter names and values are url-encoded by this method.
	 *
	 * Note: Use redirect only if you need the client browser's URL to have a new URL.
	 *       Its <strong>much faster</strong> to use simply invoke the method of the required action and return it's result.
	 *
	 * @param action
	 * @param optionalParamsAndValues an optional list of parameters and values, alternating like so: param1, value1, param2, value2...
	 * @return A Target object
	 */
	public Target redirectToAction(String action, String... optionalParamsAndValues) {
		return redirectToAction(this.getClass(), action, optionalParamsAndValues);
	}

	/**
	 * Defines a redirect to other action.
	 *
	 * Sends a temporary redirect response to the client using a URL created from the
	 * specified controllerClass, action and the specified action and parameters
	 * The URL has the form: ../controllerName/action?param1=value1&param2=value2...
	 *
	 * The parameter names and values are url-encoded by this method.
	 *
	 * Note: Use redirect only if you need the client browser's URL to have a new URL.
	 *       Its <strong>much faster</strong> to use simply invoke the method of the required action and return it's result.
	 *
	 * @param controllerClass
	 * @param action
	 * @return A Target object
	 */
	public Target redirectToAction(Class<? extends BaseController> controllerClass, String action, String... optionalParamsAndValues) {
		Map<Object, Object> parameterMap = context.buildParameterMap(optionalParamsAndValues);
		return internalRedirectToAction(controllerClass, action, parameterMap);
	}

	/**
	 * Defines a redirect to other action.
	 * This internal method is used by all the public redirectToAction() methods.
	 *
	 * Sends a temporary redirect response to the client using a URL created from the
	 * specified controllerClass and action
	 * The URL has the form: ../controllerName/action
	 *
	 * To set parameters, use {@see RedirectTarget#setParameters(Map)} or {@see RedirectTarget#setParameters(String...)}
	 *
	 * Note: Use redirect only if you need the client browser's URL to have a new URL.
	 *       Its <strong>much faster</strong> to create a new instance of controllerClass, invoke the required action's method and return it's result.
	 *
	 * @param controllerClass
	 * @param action can be null to use default action
	 * @param parameterMap A map of parameter/value pairs.
	 * @return A Target object
	 */
	private RedirectTarget internalRedirectToAction(Class<? extends BaseController> controllerClass, String action, Map<Object, Object> parameterMap) {
		Controller annotation = controllerClass.getAnnotation(Controller.class);
		String ctrlName = annotation != null && !StringUtils.isEmpty(annotation.alias()) ? annotation.alias() : controllerClass.getName();
		StringBuffer url = new StringBuffer();
		url.append("../").append(ctrlName).append("/");
		if (action != null) {
			url.append(action);
		}
		return new RedirectTarget(url.toString()).setParameters(parameterMap);
	}

	/**
	 * Creates a TemplateTarget for the specified templateName (building absolute path is a relativ path was provided)
	 *
	 * @param templateName The name (with relative or absolute path) of the template to render
	 * @return a Target object
	 */
	public Target render(String templateName) {
		if (!templateName.startsWith("/")) {
			// build absolute path
			String[] packageParts = this.getClass().getName().split("\\.");	// panama.Foo --> panama, Foo
			List<String> parts = new ArrayList<String>(Arrays.asList(packageParts));
			parts.remove(parts.size()-1);									// remove last element (the class name itself)
			String[] tplParts = templateName.split("/");
			for (String p : tplParts) {
				if (StringUtils.isEmpty(p) || p.equals(".")) { continue; }
				if (p.equals("..")) {
					parts.remove(parts.size()-1);
				} else {
					parts.add(p);
				}
			}
			templateName = "/"+StringUtils.join(parts, "/");
		}
		Target t = new TemplateTarget(templateName);
		return t;
	}

	/**
	 * Executes an action of the same controller with an optional set of parameters.
	 * After execution, the original parameters are restored.
	 * @param actionName Name of the action
	 * @param optionalParamsAndValues an optional list of parameters and values, alternating like so: param1, value1, param2, value2...; These replace the original parameters during the execution of the action.
	 * @return The resulting Target of the executed action
	 * @throws ForceTargetException
	 * @throws NoSuchActionException
	 * @throws HttpErrorException
	 */
	public Target executeAction(String actionName, String... optionalParamsAndValues) throws ForceTargetException, NoSuchActionException, HttpErrorException {
		@SuppressWarnings("rawtypes")
		Map originalParameters = context.getParameterMap();
		try {
			Map<Object, Object> parameterMap = context.buildParameterMap(optionalParamsAndValues);
			context.setParameterMap(parameterMap);
			return context.getCore().executeAction(context, this.getClass().getName(), actionName);
		} finally {
			context.setParameterMap(originalParameters);
		}
	}

	// --- Methods for using Tables and Trees -----------------------------------------------------

	/**
	 * If no table with specified key exists in current session, a new entry with the specified initialTable is created
	 * and returned, otherwise the already existing table is returned.
	 * In any case, the table is put into request scope using it's key as key.
	 * @param initialTable
	 * @return existing or initial table
	 */
	public Table registerTable(Table initialTable) {
		Table table = getTable(initialTable.getKey());
		if (table == null) {
			table = initialTable;
			getTableMap().put(table.getKey(), table);
		} else {
			/* table model is intentionally transient, so it might be null even if the table is still there,
			 * after a session got restored. In that case, we take the model from the initialTable.
			 */
			if (table.getModel() == null) {
				table.setModel(initialTable.getModel());
			}
		}
		context.put(table.getKey(), table);
		return table;
	}

	/**
	 * Gets a table from the table-map in session scope.
	 * @param tableId Unique ID
	 * @return a Table
	 */
	public Table getTable(String tableId) {
		return getTableMap().get(tableId);
	}

	/** lazily create tableMap */
	private Map<String, Table> getTableMap() {
		@SuppressWarnings("unchecked")
		Map<String, Table> map = (Map<String, Table>)context.session.get(TABLEMAP_KEY);
		if (map == null) {
			/* create map if not already there */
			map = new HashMap<String, Table>();
			Context.getInstance().session.put(TABLEMAP_KEY, map);
		}
		return map;
	}

	/**
	 * If no tree with specified key exists in current session, a new entry with the specified initialTree is created
	 * and returned, otherwise the already existing tree is returned.
	 * @param initialTree
	 * @return the existing or initial Tree object
	 */
	public Tree registerTree(Tree initialTree) {
		Tree tree = getTree(initialTree.getKey());
		if (tree == null) {
			tree = initialTree;
			getTreeMap().put(tree.getKey(), tree);
		} else {
			/* tree model is intentionally transient, so it might be null even if the tree is still there,
			 * after a session got restored. In that case, we take the model from the initialTree.
			 */
			if (tree.getModel() == null) {
				tree.setModel(initialTree.getModel());
			}
		}
		return tree;
	}

	/**
	 * Gets a tree from the tree-map in session scope.
	 * @param treeId Unique ID
	 * @return A Tree object
	 */
	public Tree getTree(String treeId) {
		return getTreeMap().get(treeId);
	}

	/** lazily create treeMap */
	private Map<String, Tree> getTreeMap() {
		@SuppressWarnings("unchecked")
		Map<String, Tree> map = (Map<String, Tree>)Context.getInstance().session.get(TREEMAP_KEY);
		if (map == null) {
			/* create map if not already there */
			map = new HashMap<String, Tree>();
			Context.getInstance().session.put(TREEMAP_KEY, map);
		}
		return map;
	}
}
