/*
 *  Copyright 2004-2013 Robert Brandner (robert.brandner@gmail.com)
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
package panama.examples.polyglot;

import panama.annotations.Action;
import panama.annotations.Controller;
import panama.collections.QueryListModel;
import panama.collections.QueryTable;
import panama.collections.filters.Filter;
import panama.core.BaseController;
import panama.core.Target;
import panama.examples.polyglot.entities.ArticleBase;
import panama.examples.polyglot.entities.LocalizedArticle;
import panama.persistence.PersistentBean;

import com.avaje.ebean.Ebean;

/**
 * @author ridcully
 *
 */
@Controller(alias="polyglot",  defaultAction="list")
public class PolyglotController extends BaseController {

	public PolyglotController() {
		registerTable(new QueryTable("localizedarticletable", new QueryListModel(Ebean.createQuery(LocalizedArticle.class))));
	}

	@Action
	public Target list() {
		String language = context.getParameter("language");
		if (language == null) language = "en";

		// filter to show articles of selected language
		getTable("localizedarticletable").setFilter("language", Filter.eq("id.language", language));

		return render("list.vm");
	}


	@Action
	public Target create() {
		String id = context.getParameter("id");
		String no = context.getParameter("no");
		String name = context.getParameter("name");
		String description = context.getParameter("description");
		String language = context.getParameter("language");

		ArticleBase base = PersistentBean.findOrCreate(ArticleBase.class, id);
		LocalizedArticle article = new LocalizedArticle(base, language);
		article.setArticleNo(no);
		article.setName(name);
		article.setDescription(description);

		Ebean.save(article);

		return list();
	}
}
