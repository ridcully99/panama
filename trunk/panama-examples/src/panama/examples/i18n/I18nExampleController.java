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
package panama.examples.i18n;

import java.util.Locale;

import panama.annotations.Action;
import panama.annotations.Controller;
import panama.core.BaseController;
import panama.core.Target;

@Controller(alias="i18n", defaultAction="view")
public class I18nExampleController extends BaseController {

	@Action
	public Target view() {
		return render("internationalization.vm");
	}
	
	@Action
	public Target set() {
		context.setLocale(new Locale(context.getParameter("language")));
		return view();
	}
	
	@Action
	public Target clear() {
		context.setLocale(null);
		return view();
	}
}
