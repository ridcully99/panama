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
package panama.util;

import java.util.HashMap;
import java.util.Map;

import panama.annotations.Controller;
import panama.collections.Tree;
import panama.core.BaseController;
import panama.core.Context;
import panama.core.Dispatcher;
import panama.core.Target;
import panama.exceptions.ForceTargetException;



/**
 * @author Ridcully
 * 
 */
@Controller
public class TreeController extends BaseController {

	/** Key used to store DefaultTree-Map in Session-Scope */
	public final static String TREEMAP_KEY = Dispatcher.PREFIX + "treemap";
		
	/**
	 * This actions toggles the open/closed state of one node of a DefaultTree
	 */
	public Target toggle() throws ForceTargetException {
		String treeId = context.getParameter("treeid");
		String nodeId = context.getParameter("nodeid");

		Tree tree = getTree(treeId);

		if (tree != null && tree instanceof Tree) {
			((Tree)tree).toggleNode(nodeId);
		}
		return redirect(context.getRequest().getHeader("referer"));
	}
	
	// -------------------------------------------------------------------------------------
	// Useful helper methods
	// -------------------------------------------------------------------------------------

	/**
	 * Gets a tree from the tree-map in session scope.
	 * @param treeId Unique ID
	 * @return A Tree object
	 */
	public Tree getTree(String treeId) {
		return getTreeMap().get(treeId);
	}

	/**
	 * Puts a tree into the tree-map in session scope .
	 * @param tree Some kind of tree
	 */
	public void addTree(Tree tree) {
		getTreeMap().put(tree.getKey(), tree);
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
