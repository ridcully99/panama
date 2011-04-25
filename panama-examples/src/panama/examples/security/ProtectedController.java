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
package panama.examples.security;

import panama.core.BaseController;
import panama.core.TemplateTarget;
import panama.exceptions.AuthorizationException;
import panama.exceptions.ForceTargetException;

/**
 * @author ridcully
 *
 */
public class ProtectedController extends BaseController {

	@Override
	public void beforeAction(String actionName) throws ForceTargetException, AuthorizationException {
		// the login action is allowed for everyone
		if (actionName.equals("login")) {	
			return;
		}
		
		// supposed, that every logged in user has role 'user' 
		if (!context.getRequest().isUserInRole("user")) {
			// not logged in? simply render the login-form.
			throw new ForceTargetException(new TemplateTarget("login.vm"));
		} 
		
		// check if currently logged in user has 'access-all-areas' role
		if (!context.getRequest().isUserInRole("access-all-areas")) {
			// if not, create a 403 (Access Denied) HTTP Error
			throw new AuthorizationException();
		}
	}
}
