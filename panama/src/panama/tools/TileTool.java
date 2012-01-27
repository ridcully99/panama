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
package panama.tools;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.velocity.tools.Scope;
import org.apache.velocity.tools.config.DefaultKey;
import org.apache.velocity.tools.config.InvalidScope;

import panama.core.Context;
import panama.core.Dispatcher;
import panama.core.Target;
import panama.core.TemplateTarget;



/**
 * Support for Tiles.
 * Must be placed in Request-Scope!
 * @author Ridcully
 */
@DefaultKey(value="tiles")
@InvalidScope({Scope.APPLICATION,Scope.SESSION})
public class TileTool {

	/**
	 * Executes specified action and renders the resulting template.
	 * This method executes the given action. If the action returns a TemplateTarget, it's go() method is invoked.
	 * So this method can easily be used to create tiles, blocks and the like.
	 * For example if you have a ShoppingCart Controller with a view action returning
	 * a template rendering your cart, you may execute that action from within every
	 * template you like and #include the result to include the cart-template within your
	 * template.
	 * 
	 * @see Dispatcher#handleAction(Context, String)
	 *
	 * @param controllerName The name of the controller to use.
	 * @param actionName The name of the action to execute.
	 */
	public void embed(String controllerName, String actionName) throws Exception {
		Context ctx = Context.getInstance();
		Target t = ctx.getCore().handleAction(ctx, controllerName+"/"+actionName);
		if (t instanceof TemplateTarget) {
			t.go();
		}
	}   
	
	/**
	 * Executes specified action and returns parsed resulting template.
	 * This method allows to add additional parameters to ctx or to overwrite existing ones.
	 * 
	 * Note, that the additional parameters only exist during the execution of the action.
	 * After that, the original parameters are restored.
	 * 
	 * @see #embed(String, String)
	 * @param controllerName The name of the controller to use.
	 * @param actionName The name of the action to execute.
	 * @param parameterMap A map of additional parameters; all keys an values should be strings or will converted using toString().
	 */
	public void embed(String controllerName, String actionName, Map parameterMap) throws Exception {
		Context ctx = Context.getInstance();
		Map backup = null;
		try {
			if (parameterMap != null) {
				backup = new HashMap(ctx.getParameterMap());
				for (Iterator<Map.Entry> it = parameterMap.entrySet().iterator(); it.hasNext(); ) {
					Map.Entry e = it.next();
					ctx.setParameter(e.getKey().toString(), e.getValue() != null ? e.getValue().toString() : null);
				}
			}
			embed(controllerName, actionName);
		} finally {
			if (backup != null) {
				ctx.setParameterMap(backup);
			}
		}
	} 	
}
