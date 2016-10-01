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
package panama.tests;

import junit.framework.TestCase;

/**
 * @author ridcully
 *
 */
public class SplitTest extends TestCase {

	public void testSplit() {
		String[] parts  = "bla/".split("/");
		parts = "/bla/".split("/");
		parts = "bla/foo".split("/");
		parts = "bla/foo/".split("/");
		parts = "".split("/");
		parts = "/".split("/");
		parts = "foo".split("/");
	}
	
	public void testLastIndex() {
		String[] a = extractControllerAndActionNames("");
		a = extractControllerAndActionNames("/");
		a = extractControllerAndActionNames("bla/");
		a = extractControllerAndActionNames("bla/foo");
		a = extractControllerAndActionNames("bla/bla/foo");
		a = extractControllerAndActionNames("/bla/bla/foo");
	}
	
	private String[] extractControllerAndActionNames(String path) {
		path = path.replaceFirst("^/+", "");	// remove leading slashs
		String ctrlName = null;
		String actionName = null;
		if (path.length() > 0) {
			int divider = path.lastIndexOf("/");
			if (divider == -1) {
				ctrlName = path;
			} else {
				ctrlName = path.substring(0, divider).trim();
				if (divider+1 < path.length()) {
					actionName = path.substring(divider+1).trim();
				}
			}
		}
		return new String[] {ctrlName, actionName};
	}
	
}
