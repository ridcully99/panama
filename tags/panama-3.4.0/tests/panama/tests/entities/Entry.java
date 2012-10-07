/*
 * Created on 18.03.2005
 * (c) 2004 - all rights reserved
 */
package panama.tests.entities;

import panama.persistence.PersistentBean;

/**
 * @author Robert
 * 
 */
public class Entry extends PersistentBean {

	String message;
	String name;
	String email;
	
	public Entry() {
	}

	public Entry(String name) {
		setName(name);
	}
	
	public String toString() {
		return getName();
	}
	
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
}
