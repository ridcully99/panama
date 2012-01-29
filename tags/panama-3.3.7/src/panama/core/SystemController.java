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
package panama.core;

import panama.annotations.Action;
import panama.annotations.Controller;
import panama.exceptions.ForceTargetException;

/**
 * An internal controller providing actions to gather some information.
 * @author Ridcully
 *
 */
@Controller(alias="system", defaultAction="info")
public class SystemController extends BaseController {

	public SystemController() {
	}

	@Action
	public Target info() throws ForceTargetException {
		return new PlainTextTarget(Version.LOGO_ASCIIART+"\n Uptime: "+context.getCore().getFormattedUptime());
	}	
}
