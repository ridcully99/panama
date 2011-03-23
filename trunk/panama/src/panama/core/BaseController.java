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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

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
	protected static SimpleLogger log = new SimpleLogger(Dispatcher.class);
	
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
	 * This method is invoked if access to given action of given controller is not allowed.
	 * according to plugged Authorization class.
	 * Default implemention does nothing.
	 * May be used to redirect to specific page (e.g. login)
	 * 
	 * @param controller
	 * @param action
	 * @throws ForceTargetException
	 */
	public void handleNotAuthorized(BaseController controller, String action) throws ForceTargetException {
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
	 *       Its <strong>much faster</strong> to use doAction().
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
	 *       Its <strong>much faster</strong> to use doAction().
	 * 
	 * @param action
	 * @return A Target object
	 */		
	public Target redirectToAction(String action) {
		return redirectToAction(this.getClass(), action, null);
	}	

	/**
	 * Defines a redirect to other action.
	 *
	 * Sends a temporary redirect response to the client using a URL created from the
	 * current Controller and the specified action and parameters.
	 * The URL has the form: ../controllerName/action.now?param1=value1&param2=value2 ....
	 * 
	 * The parameter names and values are url-encoded by this method.
	 * 
	 * Note: Use redirect only if you need the client browser's URL to have a new URL.
	 *       Its <strong>much faster</strong> to use doAction().
	 * 
	 * @param action
	 * @param parameterMap
	 * @return A Target object
	 */		
	public Target redirectToAction(String action, Map parameterMap) {
		return redirectToAction(this.getClass(), action, parameterMap);
	}
	
	/**
	 * Defines a redirect to other action.
	 *
	 * Sends a temporary redirect response to the client using a URL created from the
	 * specified controllerClass and action.
	 * The URL has the form: ../controllerName/action.now
	 * 
	 * Note: Use redirect only if you need the client browser's URL to have a new URL.
	 *       Its <strong>much faster</strong> to use doAction().
	 * 
	 * @param controllerClass
	 * @param action
	 * @return A Target object
	 */	
	public Target redirectToAction(Class<? extends BaseController> controllerClass, String action) {
		return redirectToAction(controllerClass, action, null);
	}

	/**
	 * Defines a redirect to other action.
	 *
	 * Sends a temporary redirect response to the client using a URL created from the
	 * specified controllerClass, action and parameters.
	 * The URL has the form: ../controllerName/action?param1=value1&param2=value2 ....
	 * 
	 * The parameter names and values are url-encoded by this method.
	 * 
	 * Note: Use redirect only if you need the client browser's URL to have a new URL.
	 *       Its <strong>much faster</strong> to use doAction().
	 * 
	 * @param controllerClass
	 * @param action
	 * @param parameterMap A map of parameter/value pairs.
	 * @return A Target object
	 */	
	public Target redirectToAction(Class<? extends BaseController> controllerClass, String action, Map parameterMap) {
		Controller annotation = controllerClass.getAnnotation(Controller.class);
		String ctrlName = annotation != null && !StringUtils.isEmpty(annotation.alias()) ? annotation.alias() : controllerClass.getName();
		StringBuffer url = new StringBuffer();
		url.append("../").append(ctrlName).append("/").append(action);
		try {
			if (parameterMap != null) {
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
	 * @param key
	 * @param initialTable
	 * @return existing or initial table
	 */
	public Table registerTable(Table initialTable) {
		TableController tableCtrl = new TableController();
		Table table = tableCtrl.getTable(initialTable.getKey());
		if (table == null) {
			table = initialTable;
			tableCtrl.addTable(initialTable);
		}
		context.put(table.getKey(), table);
		return table;
	}
	
	/**
	 * If no tree with specified key exists in current session, a new entry with the specified initialTree is created
	 * and returned, otherwise the already existing tree is returned.
	 * @param key
	 * @param initialTree
	 * @return
	 */
	public Tree registerTree(Tree initialTree) {
		TreeController trees = new TreeController();
		Tree tree = trees.getTree(initialTree.getKey());
		if (tree == null) {
			tree = initialTree;
			trees.addTree(initialTree);
		}
		return tree;
	}	
	
	/**
	 * Gets localized string for specified key and optional args, 
	 * based on default resource bundle (resources.properties and it's variations for 
	 * other languages like resources_de.properties) and current locale.
	 * 
	 * e.g. hello = Hello {1}!
	 * getResource("hello", "World") -> Hello World!
	 * 
	 * @param key
	 * @param args values for placesholders in resource value.
	 * @return the formatted string
	 */
	public String getLocalizedString(String key, Object... args) {
		return getLocalizedString("resources", key, args);
	}
		
	/**
	 * Gets localized string for specified key and optional args, based on specified resource bundle and current locale.
	 * @param bundleName name of resource bundle to use
	 * @param key
	 * @param args values for placesholders in resource value.
	 * @return the formatted string
	 */
	public String getLocalizedString(String bundleName, String key, Object... args) {
		ResourceBundle bundle = ResourceBundle.getBundle(bundleName, context.getLocale());
		if (bundle.containsKey(key)) {
			return MessageFormat.format(bundle.getString(key), args);
		} else {
			return "???"+key+"???";
		}
	}
}