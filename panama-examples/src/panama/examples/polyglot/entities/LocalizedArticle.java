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

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Contains parts of Article that can be multi-lingual.
 * Every instance has properties in one specific language.
 * You should work with instances of this class,
 * refering to base-properties via the base property (you may want to define wrapper getters/setters for transparent usage)
 *
 * @author ridcully
 *
 */
@Entity
@Table(name="localizedarticle")
public class LocalizedArticle extends LocalizedPersistentBean {

	private static final long serialVersionUID = 1L;

	// reference to Base
	@ManyToOne(fetch=FetchType.EAGER, optional=false, cascade=CascadeType.ALL)
	private ArticleBase base;

	private String name;
	private String description;

	/**
	 * @param base
	 * @param language
	 */
	public LocalizedArticle(ArticleBase base, String language) {
		super(base, language);	// MUST call super constructor
		setBase(base);
	}

	// --- wrapped getters/setters for base -------------------------------------------------------

	public String getArticleNo() {
		return base.getArticleNo();
	}

	public void setArticleNo(String articleNo) {
		base.setArticleNo(articleNo);
	}

	// --- simple getters/setter from here --------------------------------------------------------

	public ArticleBase getBase() {
		return base;
	}

	public void setBase(ArticleBase base) {
		this.base = base;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
