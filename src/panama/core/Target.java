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

import org.grlea.log.SimpleLogger;


/**
 * Targets are returned by Controller#render() and Controller#redirect()
 * The target is interpreted by the Dispatcher
 * @author Robert
 */
public abstract class Target {
	
	protected static SimpleLogger log = new SimpleLogger(Target.class);	
	
	public Target() {
	}
	
	/**
	 * Goes to the target
	 */
	public abstract void go() throws Exception;
}
