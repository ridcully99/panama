/*
 * Created on 24.09.2005
 *
 */
package panama.tests;

import java.util.List;

import panama.persistence.PolyglotPersistentBean;



public class TestBean extends PolyglotPersistentBean {
	
	Integer i;
	String s;
	List l;
	double d = 999.9;
	
	public Integer getI() {
		return i;
	}
	public void setI(Integer i) {
		this.i = i;
	}
	public List getL() {
		return l;
	}
	public void setL(List l) {
		this.l = l;
	}
	public String getS() {
		return s;
	}
	public void setS(String s) {
		this.s = s;
	}
	public double getD() {
		return d;
	}
	public void setD(double d) {
		this.d = d;
	}	
}