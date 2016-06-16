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
package panama.examples.issuetracker.entities;

import java.util.Date;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

import panama.persistence.PersistentBean;

import com.avaje.ebean.annotation.CreatedTimestamp;

/**
 * @author ridcully
 *
 */
@Entity
public class Issue extends PersistentBean {

	//@Id
	//private String lang;

	private String title;
	private String description;
	private String createdBy;
	@CreatedTimestamp
	private Date createdAt;
	private String state;
	@ManyToMany(cascade=CascadeType.ALL)
	@JoinTable(
			name="issue_tag",
			joinColumns={@JoinColumn(name="issue_id", referencedColumnName="id")},
			inverseJoinColumns={@JoinColumn(name="tag_id", referencedColumnName="id")})
	private Set<Tag> tags;

	public Set<Tag> getTags() {
		return tags;
	}
	public void setTags(Set<Tag> foos) {
		this.tags = foos;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getCreatedBy() {
		return createdBy;
	}
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}
	public Date getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
}
