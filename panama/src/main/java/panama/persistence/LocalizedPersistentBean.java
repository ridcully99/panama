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
package panama.persistence;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.IdClass;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

import org.apache.commons.lang.StringUtils;

import com.avaje.ebean.Ebean;

/**
 * @author robert.brandner
 *
 */
@MappedSuperclass
@IdClass(LocalizedPersistentBean.PK.class)
public class LocalizedPersistentBean/*<T extends PersistentBean>*/ implements Serializable {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Primary key consisting of id (same than base) and language
	 * Same ID than base allows for implementation of fast find() without knowing about actual base class
	 * (that knowledge is not possible due to the problem explained below at private T base
	 */
	@EmbeddedId
	private PK pk = new PK();

	/**
	 * A version field
	 * Using the timestamp you can easily check if an object is new or persistent by checking if timestamp is null
	 */
	@Version
	protected Date timeStamp;

// wäre schön geht aber nicht, da refleciton.Field.getType hierfür fix PersistentBean zurückliefert (wg. T extends PersistentBean), nicht die konkrete Klasse
//		@ManyToOne(fetch=FetchType.EAGER, optional=false)
//		private T base;
	
//	public LocalizedPersistentBean() {
//	}
//
//	public LocalizedPersistentBean(T base, String language) {
//		setBase(base);
//		pk.setLanguage(language);
//	}
//
//	public T getBase() {
//	return base;
//}
///**
// * Also manually let have translations same ID as the bean
// * The method for JPA 2.0 ManyToOne ID as described here (http://en.wikibooks.org/wiki/Java_Persistence/Identity_and_Sequencing) does not work with ebean :-/
// * @param bean
// */
//public void setBase(T bean) {
//	this.base = base;
//	pk.setId(base.getId());
//}
	
	public LocalizedPersistentBean(PersistentBean base, String language) {
		pk.setId(base != null ? base.getId() : null);
		pk.setLanguage(language);
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
		return Ebean.find(beanType, new PK(base.getId(), language));
	}

	public PK getPk() {
		return pk;
	}
	public void setPk(PK pk) {
		this.pk = pk;
	}

	public String getLanguage() {
		return pk.getLanguage();
	}

	public void setLanguage(String language) {
		pk.setLanguage(language);
	}
	
	public Date getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	/**
	 * Composite ID
	 * @author ridcully
	 *
	 */
	@Embeddable
	public static class PK implements Serializable {

		@Basic @Column(length=64)
		private String id;
		@Basic @Column(name="language_", length=10)
		private String language;

		public PK() {}

		public PK(String id, String language) {
			this.id = id;
			this.language = language;
		}

		/**
		 * equals - based on id and language
		 */
		public boolean equals(Object other) {
			if (this == other) { return true; }
			if ((other == null) || !(other instanceof PK)) return false;
			PK otherPK = (PK)other;
			return StringUtils.equals(id, otherPK.getId()) && StringUtils.equals(language, otherPK.getLanguage());
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
}
