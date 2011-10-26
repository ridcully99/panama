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
package panama.persistence;

import java.io.Serializable;
import java.rmi.dgc.VMID;
import java.util.Date;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

import com.avaje.ebean.Ebean;


/**
 * A basic bean for persistence, that provides a unique ID and matching equals() and hashCode() methods.
 * 
 * @author Ridcully
 */
@MappedSuperclass
public class PersistentBean implements Serializable {

	/**
	 * We use an assigned ID to avoid having to build equals/hashCode methods for each entity class
	 * VMID() contains an ID unique accross all Java Virtual Machines
	 * Note, that your mapping has to look like that:
	 * 
	 * <code>
	 * <id name="id" type="string" column="id">
     *     <generator class="assigned"/>
     * </id>
     * </code>
	 */
	@Id
    protected String id = new VMID().toString().replace(':', 'x');	// replacing colons as they are not allowed for id attributes in HTML tags.
    
    /**
     * As - due to the assigned id - hibernate does not know if an object is persisted or new and would always
     * check with the database we provide an optional timestamp that hibernate will use instead, if you
     * add the following lines to your mapping:
     * 
     * <code>
     * <timestamp
     *     column="tstamp"
     *     name="timeStamp" />
     * </code>
     * 
     * Using the timestamp you can easily check if an object is new or persistent by checking if timestamp is null
     */
	@Version
	protected Date timeStamp;

    public PersistentBean() {
    }
    
    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

	public Date getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	public static PersistentBean findOrCreate(Class<? extends PersistentBean> beanType, String id) {
		PersistentBean o = null;
		try {
			if (id == null) {
				o = beanType.getConstructor().newInstance();
			} else {
				o = Ebean.find(beanType, id);
				if (o == null) {
					o = beanType.getConstructor().newInstance();
					o.setId(id);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return o;
	}
	
	/**
	 * equals - based on id 
	 */
	public boolean equals(Object other) {
		if (this == other) { return true; }
		if ((other ==null) || (other.getClass() != this.getClass() ) ) return false;
		if (other == null) { return false; }
		return id.equals(((PersistentBean)other).getId());
	}
	
	/**
	 * hashcode - based on id
	 */
	public int hashCode() {
		return id.hashCode();
	} 
}