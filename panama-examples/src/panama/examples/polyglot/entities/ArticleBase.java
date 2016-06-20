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
package panama.examples.polyglot.entities;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import panama.persistence.PersistentBean;

/**
 * Base, containing non-multilingual properties.
 * This should also be used for defining relations (one-to-many, many-to-one, ...)
 *
 * @author ridcully
 *
 */
@Entity
@Table(name="articlebase")
public class ArticleBase extends PersistentBean {

	private static final long serialVersionUID = 1L;

	private String articleNo;

	/**
	 * Collection of all localized variants - can be used if all translations are needed.
	 * NOTE: To get a specific localized version,
	 * always use {@link LocalizedPersistentBean#find(Class, String, String)
	 */
	@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="base")
	private Set<LocalizedArticle> localizations = new HashSet<LocalizedArticle>();

	// --- simple getters/setter from here --------------------------------------------------------

	public String getArticleNo() {
		return articleNo;
	}

	public void setArticleNo(String articleNo) {
		this.articleNo = articleNo;
	}

}
