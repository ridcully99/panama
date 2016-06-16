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

import java.util.List;
import java.util.Set;

import com.avaje.ebean.Ebean;

import panama.annotations.Action;
import panama.annotations.Controller;
import panama.collections.QueryListModel;
import panama.collections.QueryTable;
import panama.core.BaseController;
import panama.core.Target;
import panama.examples.issuetracker.entities.Issue;
import panama.examples.issuetracker.entities.Tag;
import panama.exceptions.NoSuchFieldException;
import panama.form.Form;
import panama.form.FormData;
import panama.form.PersistentBeanField;
import panama.form.ValidatorFactory;
import panama.persistence.PersistentBean;

/**
 * @author ridcully
 */
@Controller(alias="issues", defaultAction="list")
public class IssueTrackerController extends BaseController {

	public final static String FORMDATA_KEY = "formdata";

	private final static Form form = new Form(Issue.class).except("createdAt");
	static {
		form.getField("title").addValidator(ValidatorFactory.getNotEmptyValidator());
		form.addField(new PersistentBeanField("tags", Tag.class));
	}

	public IssueTrackerController() {
		createTagsIfNeeded();
		registerTable(new QueryTable("issuetable", new QueryListModel(Ebean.createQuery(Issue.class))));
	}

	@Action
	public Target list() {
		return render("issuelist.vm");
	}

	@Action
	public Target edit() {
		String id = context.getParameter("id");
		Issue e = PersistentBean.findOrCreate(Issue.class, id);
		FormData fd = new FormData(form).withDataFromBean(e);
		fd.setInput("tags", e.getTags().toArray(new Tag[0]));
		return showForm(fd);
	}

	private Target showForm(FormData fd) {
		context.put(FORMDATA_KEY, fd);
		List<Tag> tags = Ebean.createQuery(Tag.class).findList();
		context.put("alltags", tags);
		return render("issueform.vm");
	}

	@Action
	public Target save() {
		if (context.getParameter("ok") != null) {
			FormData fd = new FormData(form).withDataFromRequest(context);
			String id = fd.getString("id");
			Issue e = PersistentBean.findOrCreate(Issue.class, id);
			fd.applyToExcept(e, "tags");
			if (fd.hasErrors()) {
				return showForm(fd);
			}
			try {
				Set tags = fd.getValuesAsSet("tags");
				e.setTags(tags);
			} catch (NoSuchFieldException e1) {
				e1.printStackTrace();
			}
			Ebean.save(e);
		}
		return redirectToAction("list");
	}

	@Action
	public Target delete() {
		String id = context.getParameter("id");
		if (id != null) {
			Ebean.delete(Issue.class, id);
		}
		return redirectToAction("list");
	}

	/**
	 * Lazily create tags in the demo-database if they do not already exist.
	 * Database is created for us by Ebean here (see ebean.properties#ebean.ddl.run)
	 */
	private void createTagsIfNeeded() {
		int count = Ebean.find(Tag.class).findRowCount();
		if (count == 0) {
			for (String name : new String[] {"Bashful", "Doc", "Dopey", "Grumpy", "Happy", "Sleepy", "Sneezy"}) {
				Tag t = new Tag();
				t.setName(name);
				Ebean.save(t);
			}
		}
	}
}