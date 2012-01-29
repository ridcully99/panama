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
package panama.core;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.grlea.log.SimpleLogger;

import panama.annotations.Controller;
import panama.collections.Table;
import panama.collections.Tree;
import panama.exceptions.AuthorizationException;
import panama.exceptions.ForceTargetException;
import panama.exceptions.NoSuchActionException;
import panama.util.TableController;
import panama.util.TreeController;



/**
 * Base class for all controllers.
 * 
 * @author Robert
 */
public class BaseController {

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
	 * To redirect to other action use /controller/action.now
	 * 
	 * Note: Use redirect only if you need the client browser's URL to have a new URL.
	 * 
	 * @param url
	 * @return A Target object
	 */	
	public Target redirect(String url) {
		Target t = new RedirectTarget(url);
		return t;
	}

	/**
	 * Defines a redirect to other action.
	 *
	 * Sends a temporary redirect response to the client using a URL created from the
	 * current Controller and the specified action.
	 * The URL has the form: ../controllerName/action
	 * 
	 * Note: Use redirect only if you need the client browser's URL to have a new URL.
	 *       Its <strong>much faster</strong> to use simply invoke the method of the required action and return it's result.
	 * 
	 * @param action
	 * @return A Target object
	 */		
	public Target redirectToAction(String action) {
		return internalRedirectToAction(this.getClass(), action, new HashMap<Object, Object>());
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
	 * @param paramsAndValues a variable list of parameters and values, alternating like so: param1, value1, param2, value2...
	 * @return A Target object
	 */		
	public Target redirectToAction(String action, String... paramsAndValues) {
		Map<Object, Object> parameterMap = null;
		if (paramsAndValues != null && paramsAndValues.length > 0) {
			parameterMap = new HashMap<Object, Object>();
			for (int i=0; i<paramsAndValues.length; i+=2) {
				Object key = paramsAndValues[i];
				Object value = i+1 < paramsAndValues.length ? paramsAndValues[i+1] : "";
				parameterMap.put(key, value);
			}
		}
		return internalRedirectToAction(this.getClass(), action, parameterMap);
	}
	
	/**
	 * Defines a redirect to other action.
	 *
	 * Sends a temporary redirect response to the client using a URL created from the
	 * specified controllerClass and action.
	 * The URL has the form: ../controllerName/action.now
	 * 
	 * Note: Use redirect only if you need the client browser's URL to have a new URL.
	 *       Its <strong>much faster</strong> to create a new instance of controllerClass, invoke the required action's method and return it's result.
	 * 
	 * @param controllerClass
	 * @param action
	 * @return A Target object
	 */	
	public Target redirectToAction(Class<? extends BaseController> controllerClass, String action) {
		return internalRedirectToAction(controllerClass, action, new HashMap<Object, Object>());
	}

	/**
	 * Defines a redirect to other action.
	 *
	 * Sends a temporary redirect response to the client using a URL created from the
	 * specified controllerClass and the specified action and parameters
	 * The URL has the form: ../controllerName/action?param1=value1&param2=value2...
	 * 
	 * The parameter names and values are url-encoded by this method.
	 * 
	 * Note: Use redirect only if you need the client browser's URL to have a new URL.
	 *       Its <strong>much faster</strong> to use simply invoke the method of the required action and return it's result.
	 * 
	 * @param controllerClass
	 * @param action
	 * @param paramsAndValues a variable list of parameters and values, alternating like so: param1, value1, param2, value2...
	 * @return A Target object
	 */		
	public Target redirectToAction(Class<? extends BaseController> controllerClass, String action, String... paramsAndValues) {
		Map<Object, Object> parameterMap = null;
		if (paramsAndValues != null && paramsAndValues.length > 0) {
			parameterMap = new HashMap<Object, Object>();
			for (int i=0; i<paramsAndValues.length; i+=2) {
				Object key = paramsAndValues[i];
				Object value = i+1 < paramsAndValues.length ? paramsAndValues[i+1] : "";
				parameterMap.put(key, value);
			}
		}
		return internalRedirectToAction(controllerClass, action, parameterMap);
	}
	
	/**
	 * Defines a redirect to other action.
	 * This internal method is used by all the public redirectToAction() methods.
	 *
	 * Sends a temporary redirect response to the client using a URL created from the
	 * specified controllerClass, action and parameters.
	 * The URL has the form: ../controllerName/action?param1=value1&param2=value2 ....
	 * 
	 * The parameter names and values are url-encoded by this method.
	 * 
	 * Note: Use redirect only if you need the client browser's URL to have a new URL.
	 *       Its <strong>much faster</strong> to create a new instance of controllerClass, invoke the required action's method and return it's result.
	 * 
	 * @param controllerClass
	 * @param action can be null to use default action
	 * @param parameterMap A map of parameter/value pairs.
	 * @return A Target object
	 */	
	@SuppressWarnings("rawtypes")
	private Target internalRedirectToAction(Class<? extends BaseController> controllerClass, String action, Map<Object, Object> parameterMap) {
		Controller annotation = controllerClass.getAnnotation(Controller.class);
		String ctrlName = annotation != null && !StringUtils.isEmpty(annotation.alias()) ? annotation.alias() : controllerClass.getName();
		StringBuffer url = new StringBuffer();
		url.append("../").append(ctrlName).append("/");
		if (action != null) {
			url.append(action);
		}
		try {
			if (parameterMap != null && !parameterMap.isEmpty()) {
				boolean first = true;
				for (Iterator it = parameterMap.entrySet().iterator(); it.hasNext(); ) {
					url.append(first ? "?" : "&");
					first = false;
					Map.Entry entry = (Map.Entry)it.next();
					url.append(URLEncoder.encode(entry.getKey().toString(), "UTF-8"))
						.append("=")
						.append(URLEncoder.encode(entry.getValue().toString(), "UTF-8"));
				}
			}
		} catch (UnsupportedEncodingException e) {
			assert false : "No UTF-8 encoding supported?! Really?!";
		}
		return redirect(url.toString());
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
	 * Executes another action of same controller.
	 * @param actionName Name of the action
	 * @return The resulting Target of the executed action
	 * @throws ForceTargetException 
	 * @throws NoSuchActionException
	 * @throws AuthorizationException 
	 */
	public Target executeAction(String actionName) throws ForceTargetException, NoSuchActionException, AuthorizationException {
		Context ctx = Context.getInstance();
		return ctx.getCore().executeAction(ctx, this, actionName);
	}
	
	/**
	 * Executes an action of the same controller with the specified set of parameters.
	 * After execution, the original parameters are restored.
	 * @param actionName Name of the action
	 * @param parameterMap Map of parameters to use. These replace the original parameters during the execution of the action.
	 * @return The resulting Target of the executed action
	 * @throws ForceTargetException
	 * @throws NoSuchActionException
	 * @throws AuthorizationException
	 */
	public Target executeAction(String actionName, Map parameterMap) throws ForceTargetException, NoSuchActionException, AuthorizationException {
		Map originalParameters = Context.getInstance().getParameterMap();
		try {
			Context.getInstance().setParameterMap(parameterMap);
			return executeAction(actionName);
		} finally {
			Context.getInstance().setParameterMap(originalParameters);
		}
	}

	/**
	 * If no table with specified key exists in current session, a new entry with the specified initialTable is created
	 * and returned, otherwise the already existing table is returned.
	 * In any case, the table is put into request scope using it's key as key.
	 * @param initialTable
	 * @return existing or initial table
	 */
	public Table registerTable(Table initialTable) {
		TableController tableCtrl = new TableController();
		Table table = tableCtrl.getTable(initialTable.getKey());
		if (table == null) {
			table = initialTable;
			tableCtrl.addTable(initialTable);
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
	 * If no tree with specified key exists in current session, a new entry with the specified initialTree is created
	 * and returned, otherwise the already existing tree is returned.
	 * @param initialTree
	 * @return the existing or initial Tree object
	 */
	public Tree registerTree(Tree initialTree) {
		TreeController trees = new TreeController();
		Tree tree = trees.getTree(initialTree.getKey());
		if (tree == null) {
			tree = initialTree;
			trees.addTree(initialTree);
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
}
