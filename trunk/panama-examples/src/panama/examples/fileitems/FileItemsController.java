/*
a *  Copyright 2004-2010 Robert Brandner (robert.brandner@gmail.com) 
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
package panama.examples.fileitems;

import org.apache.commons.fileupload.FileItem;

import panama.annotations.Action;
import panama.annotations.Controller;
import panama.core.BaseController;
import panama.core.Target;
import panama.form.FileItemField;
import panama.form.Form;
import panama.form.FormData;
import panama.form.StringField;

/**
 * @author ridcully
 *
 */
@Controller(alias="fileitems", defaultAction="view")
public class FileItemsController extends BaseController {

	private static Form form;
	static {
		form = new Form();
		form.addField(new StringField("msg"));
		form.addField(new FileItemField("attachment"));
	}
	
	@Action
	public Target view() {
		return render("view.vm");
	}
	
	@Action
	public Target save() {
		if (context.getParameter("submit") != null) {
			FormData fd = new FormData(form);
			fd.setInput(context.getParameterMap());
			fd.setInput(context.getFileItemMap());
			context.put("msg", fd.getString("msg"));
			context.put("attachment", fd.getFileItem("attachment"));
			context.put("showresults", Boolean.TRUE);
		}
		return view();
	}
}
