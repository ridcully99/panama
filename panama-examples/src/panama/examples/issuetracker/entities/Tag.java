/*
 *  Copyright by Robert Brandner (robert.brandner@gmail.com)
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

import javax.persistence.Entity;
import javax.persistence.Id;

import panama.persistence.PersistentBean;

/**
 * @author ridcully
 *
 */
@Entity
public class Tag extends PersistentBean {

//	@Id
//	private String lang;

	private String name;

//  !! BUG in Ebean 2.7.7 breaks this bidirectional many-to-many relation!

//	@ManyToMany(mappedBy="tags", cascade=CascadeType.ALL)
//	private Set<Issue> issues;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

//	public Set<Issue> getIssues() {
//		return issues;
//	}
//
//	public void setIssues(Set<Issue> issues) {
//		this.issues = issues;
//	}
}
