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
import java.util.Date;

import javax.persistence.EmbeddedId;
import javax.persistence.IdClass;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

import com.avaje.ebean.Ebean;

import panama.persistence.PersistentBean;

/**
 * @author robert.brandner
 *
 */
@MappedSuperclass
@IdClass(LocalizedId.class)
public class LocalizedPersistentBean implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Primary key consisting of id (same than base) and language
	 * Same ID than base allows for implementation of fast find() without knowing about actual base class
	 * (that knowledge is not possible due to the problem explained below at private T base
	 */
	@EmbeddedId
	private LocalizedId id = new LocalizedId();

	/**
	 * A version field
	 * Using the timestamp you can easily check if an object is new or persistent by checking if timestamp is null
	 */
	@Version
	protected Date timeStamp;

	public LocalizedPersistentBean() {
	}

	public LocalizedPersistentBean(PersistentBean base, String language) {
		this.id.setId(base != null ? base.getId() : null);
		this.id.setLanguage(language);
	}

	/**
	 * Tries to fetch localized part of specified base object for specified language and of type beanType.
	 * Always use this method to get specific translations for a base object.
	 *
	 * @param beanType
	 * @param base
	 * @param language
	 * @return an object of class beanType or null
	 */
	public static <T extends LocalizedPersistentBean> T find(Class<T> beanType, PersistentBean base, String language) {
		return Ebean.find(beanType, new LocalizedId(base.getId(), language));
	}

	public LocalizedId getId() {
		return id;
	}
	public void setId(LocalizedId id) {
		this.id = id;
	}

	public String getLanguage() {
		return id.getLanguage();
	}

	public void setLanguage(String language) {
		id.setLanguage(language);
	}

	public Date getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}

	/**
	 * equals - based on id
	 */
	public boolean equals(Object other) {
		if (this == other) { return true; }
		if ((other == null) || (other.getClass() != this.getClass() ) ) return false;
		return id.equals(((LocalizedPersistentBean)other).getId());
	}

	/**
	 * hashcode - based on id
	 */
	public int hashCode() {
		return id.hashCode();
	}
}
