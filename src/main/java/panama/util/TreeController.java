/*
 *  Copyright 2004-2016 Robert Brandner (robert.brandner@gmail.com) 
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

import panama.annotations.Action;
import panama.annotations.Controller;
import panama.collections.Tree;
import panama.core.BaseController;
import panama.core.Target;
import panama.exceptions.ForceTargetException;



/**
 * @author Ridcully
 *
 */
@Controller
public class TreeController extends BaseController {

	/**
	 * This actions toggles the open/closed state of one node of a DefaultTree
	 */
	@Action
	public Target toggle() throws ForceTargetException {
		String treeId = context.getParameter("treeid");
		String nodeId = context.getParameter("nodeid");

		Tree tree = getTree(treeId);

		if (tree != null && tree instanceof Tree) {
			((Tree)tree).toggleNode(nodeId);
		}
		return redirect(context.getRequest().getHeader("referer"));
	}
}
