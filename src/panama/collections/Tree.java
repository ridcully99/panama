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
package panama.collections;

import java.util.List;
import java.util.Set;

public interface Tree {
	
	/**
	 * Returns unique key for the tree
	 * @return unique key for the tree
	 */
	public String getKey();
	
	/**
	 * Gets a list of all root nodes of the tree
	 * @return a list of objects or null
	 */
	public List getRootNodes();
	
	public void toggleNode(Object nodeId);
	
	public boolean isClosed(Object nodeId);
	
	public boolean isOpen(Object nodeId);
	
	public Set getSelected();
	
	/**
	 * Creates a link for toggling (open/close) specified node of the tree
	 * @param nodeId 
	 * @return a link invoking TreeController
	 */		
	public String toggleLink(Object nodeId);
}
