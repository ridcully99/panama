/*
 *  Copyright 2004-2016 Robert Brandner (robert.brandner@gmail.com)
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

import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;

import org.apache.commons.lang.StringUtils;

/**
 * @author robert.brandner
 *
 */
@Entity
@Embeddable
public class LocalizedId implements Serializable {

	private static final long serialVersionUID = 1L;

	@Basic @Column(length=64)
	private String id;
	@Basic @Column(name="language_", length=10)
	private String language;

	public LocalizedId() {}

	public LocalizedId(String id, String language) {
		this.id = id;
		this.language = language;
	}

	/**
	 * equals - based on id and language
	 */
	public boolean equals(Object other) {
		if (this == other) { return true; }
		if ((other == null) || !(other instanceof LocalizedId)) return false;
		LocalizedId otherLocalizedId = (LocalizedId)other;
		return StringUtils.equals(id, otherLocalizedId.getId()) && StringUtils.equals(language, otherLocalizedId.getLanguage());
	}

	/**
	 * hashcode - based on id and language
	 */
	public int hashCode() {
		int hash = 1;
		hash = hash + (id == null ? 0 : id.hashCode());
		hash = hash * 17 + (language == null ? 0 : language.hashCode());
		return hash;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}
}