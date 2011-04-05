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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import panama.annotations.Action;
import panama.annotations.Controller;
import panama.collections.DefaultTable;
import panama.collections.ListModel;
import panama.collections.SimpleListModel;
import panama.collections.Table;
import panama.core.BaseController;
import panama.core.Target;
import panama.examples.guestbook.GuestbookEntry;
import panama.form.Form;
import panama.form.FormData;
import panama.form.StringField;

/**
 * Example on how to use tokens to prevent XSS and duplicate form submit.
 * @author ridcully
 *
 */
@Controller(alias="secureguestbook", defaultAction="list")
public class SecureGuestbookController extends BaseController {

	/* create ListModel backed by a simple list. As this should be shared by all users, we make it static */
	private static List<GuestbookEntry> entries = new ArrayList<GuestbookEntry>();
	private static ListModel model = new SimpleListModel(entries);
	
	private final static Form form;	
	static {
		/* create form model based on GuestbookEntry class. This may be done static as long as the form is not changed later (make it final to ensure this) */
		form = new Form();
		form.addFields(GuestbookEntry.class, Form.EXCLUDE_PROPERTIES, "date");
		form.addField(new StringField("token"));
	}
	
	public SecureGuestbookController() {
		registerTable(new DefaultTable("secureguestbookentries", model).setSortBy("date", Table.SORT_DESC));
	}
	
	@Action
	public Target list() {
		context.tokens.create("secure");
		return render("guestbook.vm");
	}

	@Action(alias="add")
	public Target addEntry() {
		GuestbookEntry entry = new GuestbookEntry();
		FormData fd = new FormData(form);							// get input values according to form model
		fd.setInput(context.getParameterMap());	
		if (!context.tokens.verify("secure", fd.getString("token"))) {
			log.error("Invalid token -- XSS or double submit?");
		} else {
			entry.setText(fd.getString("text"));
			entry.setDate(new Date());
			entries.add(entry);
		}
		context.tokens.invalidate("secure");
		return executeAction("list");
	}
}
