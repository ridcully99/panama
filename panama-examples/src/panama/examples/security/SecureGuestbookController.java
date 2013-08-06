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
import panama.examples.tinyguestbook.GuestbookEntry;
import panama.exceptions.AuthorizationException;
import panama.exceptions.ForceTargetException;
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

	private final static String USERNAME = "panama";
	private final static String PASSWORD = "42";

	/* create ListModel backed by a simple list. As this should be shared by all users, we make it static */
	private static List<GuestbookEntry> entries = new ArrayList<GuestbookEntry>();
	private static ListModel model = new SimpleListModel(entries);

	private final static Form form;
	static {
		/* create form model based on GuestbookEntry class. This may be done static as long as the form is not changed later (make it final to ensure this) */
		form = new Form(GuestbookEntry.class).except("date");
		form.addField(new StringField("token"));
	}

	public SecureGuestbookController() {
		registerTable(new DefaultTable("secureguestbookentries", model).setSortBy("date", Table.SORT_DESC));
	}

	@Override
	public void beforeAction(String actionName) throws ForceTargetException, AuthorizationException {
		Boolean loggedIn = (Boolean)context.session.get("logged-in");
		if (loggedIn == null || !loggedIn) {	// if not logged in
			if ("login".equals(actionName)) {	// allow, if it's the login action
			} else {							// otherwise show login form
				throw new ForceTargetException(render("loginform.vm"));
			}
		}
	}

	@Action
	public Target login() {
		String username = context.getParameter("username");
		String password = context.getParameter("password");
		if (USERNAME.equals(username) && PASSWORD.equals(password)) {
			context.session.put("logged-in", true);
			return redirectToAction("list");
		} else {
			context.put("loginfailed", true);	// flag used in form to show error text
			return render("loginform.vm");
		}
	}

	@Action
	public Target logout() {
		context.session.put("logged-in", null);
		//return redirect("list");
		//return redirectToAction(SecureGuestbookController.class, "list").withParameters("a","1","b","2");
		//return redirect("http://localhost:8080/panama-examples/secureguestbook/list");
		//return redirectToAction("list").setAnchor("foo");	
		return redirectToAction("list");			// will be directed to login-page by beforeAction()
	}

	@Action
	public Target list() {
		context.tokens.create("secure");
		return render("guestbook.vm");
	}

	@Action(alias="add")
	public Target addEntry() {
		GuestbookEntry entry = new GuestbookEntry();
		FormData fd = new FormData(form).withDataFromRequest(context);		// get input values according to form model
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
