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
package panama.core;

import javax.servlet.http.HttpServletResponse;

import panama.log.SimpleLogger;


/**
 * Targets are returned by Controller#render() and Controller#redirect()
 * The target is interpreted by the Dispatcher
 * @author Robert
 */
abstract public class Target {

	protected static SimpleLogger log = new SimpleLogger(Target.class);

	// a value of 0 makes the target _not_ set the status in the go() method, thus allowing
	// direct modification of the context.response (for backward compatibility and special usecases)
	protected int statusCode = 0; 

	public Target() {
	}

	/**
	 * Creates target with given status code for the target to set in response.
	 *
	 * @param statusCode status codes as defined by {@link HttpServletResponse}.
	 * 			E.g. SC_ACCEPTED (202), SC_NOT_FOUND (404) etc.
	 */
	public Target(int statusCode) {
		this.statusCode = statusCode;
	}	
	
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	
	public int getStatusCode() {
		return statusCode;
	}
	
	/**
	 * Applies status code set with {@link #setStatusCode(int)} when actually going to target.
	 * Make sure to invoke this in your implementation of {@link #go()}.
	 */
	protected void applyStatusCode() {
		Context ctx = Context.getInstance();
		if (ctx != null && ctx.getResponse() != null && statusCode != 0) {
			ctx.getResponse().setStatus(statusCode);
		}
	}

	/**
	 * Goes to the target.
	 *
	 * Make sure to call {@link #applyStatusCode()} in your implementation of this method.
	 */
	abstract public void go() throws Exception;
}
