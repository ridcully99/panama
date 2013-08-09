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
package panama.collections;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import panama.util.TreeController;



/**
 * Holds a ListModel and keeps track of currently opened nodes and selected nodes.
 * @author Ridcully
 */
public class DefaultTree implements Tree, Serializable {
	
	protected transient ListModel rootModel = null;
	protected Map openNodes = new HashMap();
	protected Set selected = new HashSet();
	
	private final String key;
	
	public DefaultTree(String key) {
		this.key = key;
	}
	
	public DefaultTree(String key, ListModel model) {
		this(key);
		setModel(model);
	}

	/** {@inheritDoc} */
	@Override
	public String getKey() {
		return this.key;
	}
	
	/** {@inheritDoc} */
	@Override
	public ListModel getModel() {
		return rootModel;
	}

	/** {@inheritDoc} */
	@Override
	public Tree setModel(ListModel model) {
		this.rootModel = model;
		return this;
	}

	/** {@inheritDoc} */
	@Override
	public List getRootNodes() {
		if (rootModel != null) {
			return rootModel.getList();
		} else {
			return null;
		}
	}	
	
	/** {@inheritDoc} */
	@Override
	public void toggleNode(Object nodeId) {
		/* only the open nodes are stored, all others are closed */
		String current = (String)openNodes.get(nodeId);
		if (current == null) {
			openNodes.put(nodeId, nodeId);
		} else {
			openNodes.remove(nodeId);
		}
	}

	/** {@inheritDoc} */
	@Override
	public boolean isClosed(Object nodeId) {
		return !openNodes.containsKey(nodeId); 
	}

	/** {@inheritDoc} */
	@Override
	public boolean isOpen(Object nodeId) {
		boolean res = openNodes.containsKey(nodeId);
		return res;
	}
	
	/** {@inheritDoc} */
	@Override
	public Set getSelected() {
		return selected;
	}
	
	/** {@inheritDoc} */
	@Override
	public String toggleLink(Object nodeId) {
		return "../"+TreeController.class.getName()+"/toggle?treeid="+this.key+"&nodeid="+nodeId;		
	}	
}
