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
package panama.examples.tinyguestbook;

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

@Controller(alias="tinyguestbook", defaultAction="list")
public class TinyGuestbookController extends BaseController {

	private static List<GuestbookEntry> entries = new ArrayList<GuestbookEntry>();
	private static ListModel model = new SimpleListModel(entries);

	private final static Form form = new Form(GuestbookEntry.class);

	public TinyGuestbookController() {
		registerTable(new DefaultTable("tinyguestbookentries", model).setSortBy("date", Table.SORT_DESC));
	}

	@Action
	public Target list() {
		return render("guestbook.vm");
	}

	@Action(alias="add")
	public Target addEntry() {
		FormData fd = new FormData(form).withDataFromRequest(context);
		GuestbookEntry entry = new GuestbookEntry();
		fd.applyTo(entry);
		entry.setDate(new Date());
		entries.add(entry);
		return redirectToAction("list");
	}
}
