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
package panama.examples.guestbook;

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
import panama.form.Form;
import panama.form.FormData;

@Controller(alias="guestbook", defaultAction="list")
public class GuestbookController extends BaseController {

	/* create ListModel backed by a simple list. As this should be shared by all users, we make it static */
	private static List<GuestbookEntry> entries = new ArrayList<GuestbookEntry>();
	private static ListModel model = new SimpleListModel(entries);

	private final static Form form;
	static {
		/* create form model based on GuestbookEntry class. This may be done static as long as the form is not changed later (make it final to ensure this) */
		form = new Form();
		form.addFields(GuestbookEntry.class, Form.EXCLUDE_PROPERTIES, "date");
	}

	public GuestbookController() {
		registerTable(new DefaultTable("guestbookentries", model).setSortBy("date", Table.SORT_DESC));
	}

	@Action
	public Target list() {
		return render("guestbook.vm");
	}

	@Action(alias="add")
	public Target addEntry() {
		GuestbookEntry entry = new GuestbookEntry();
		FormData fd = new FormData(form);							// get input values according to form model
		fd.setInput(context.getParameterMap());
		fd.applyTo(entry);
		entry.setDate(new Date());
		entries.add(entry);
		return redirectToAction("list");
	}
}
