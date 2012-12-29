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
package panama.exceptions;

import panama.core.Target;

/**
 * Throw this exception to force the given target to be rendered, even if your action 
 * might be invoked by another action that would perhaps change your desired target.
 * Use this in case of errors etc.
 * 
 * @author Ridcully
 *
 */
public class ForceTargetException extends RuntimeException {

	Target target = null;
	
	public ForceTargetException(Target t) {
		setTarget(t);
	}
	/**
	 * @return Returns the target.
	 */
	public Target getTarget() {
		return target;
	}
	
	/**
	 * @param target The target to set.
	 */
	public void setTarget(Target target) {
		this.target = target;
	}
}
