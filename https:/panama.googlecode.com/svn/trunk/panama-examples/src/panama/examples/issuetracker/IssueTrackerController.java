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
package panama.examples.issuetracker;

import panama.annotations.Action;
import panama.annotations.Controller;
import panama.collections.QueryListModel;
import panama.collections.QueryTable;
import panama.collections.Table;
import panama.core.BaseController;
import panama.core.Target;
import panama.examples.issuetracker.entities.Issue;
import panama.form.Form;
import panama.form.FormData;
import panama.persistence.PersistentBean;

import com.avaje.ebean.Ebean;

/**
 * @author ridcully
 */
@Controller(alias="issues", defaultAction="list")
public class IssueTrackerController extends BaseController {

	public final static String FORMDATA_KEY = "formdata";
	
	private Table table;
	
	private final static Form form;	
	static {
		form = new Form();
		form.addFields(Issue.class, Form.EXCLUDE_PROPERTIES, "createdAt");
	}
	
	public IssueTrackerController() {
		table = registerTable(new QueryTable("issuetable", new QueryListModel(Ebean.createQuery(Issue.class))));
	}
	
	@Action
	public Target list() {
		return render("issuelist.vm");
	}
	
	@Action
	public Target edit() {
		if (context.get(FORMDATA_KEY) == null) {
			String id = context.getParameter("id");
			Issue e = (Issue)PersistentBean.findOrCreate(Issue.class, id);
			FormData fd = new FormData(form);
			fd.setInput(e);
			context.put(FORMDATA_KEY, fd);	
		}
		return render("issueform.vm");
	}
	
	@Action
	public Target save() {
		if (context.getParameter("ok") != null) {
			FormData fd = new FormData(form);
			fd.setInput(context.getParameterMap());
			String id = fd.getString("id");
			Issue e = (Issue)PersistentBean.findOrCreate(Issue.class, id);
			fd.applyTo(e);
			if (fd.hasErrors()) {
				context.put(FORMDATA_KEY, fd);
				return edit();
			}
			Ebean.save(e);
		}
		return redirectToAction("list");
	}

	@Action
	public Target delete() {
		String id = context.getParameter("id");
		if (id != null) {
			int is = Ebean.delete(Issue.class, id);
		}
		return redirectToAction("list");
	}
}