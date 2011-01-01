/*
 * Created on 18.03.2005
 * (c) 2004 - all rights reserved
 */
package panama.tests.entities;

import panama.persistence.PolyglotPersistentBean;

/**
 * @author Robert
 * 
 */
public class PolyglotEntry extends PolyglotPersistentBean {

	String message;
	String name;
	String email;
	
	public PolyglotEntry() {
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
