/*
 *  Copyright 2004-2012 Robert Brandner (robert.brandner@gmail.com) 
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
package panama.tests.form;

import java.util.Date;

import panama.persistence.PersistentBean;


/**
 * A bean with all kinds of properties.
 * 
 * @author Ridcully
 *
 */
public class TestBean {

	private Boolean bln;
	private Date dt;
	private Double dbl;
	private Float flt;
	private Integer ntgr;
	private Long lng;
	private String strng;
	private PersistentBean prst;
	private String[] strngs;
	private Long[] lngs;
	
	public Boolean getBln() {
		return bln;
	}
	public void setBln(Boolean bln) {
		this.bln = bln;
	}
	public Double getDbl() {
		return dbl;
	}
	public void setDbl(Double dbl) {
		this.dbl = dbl;
	}
	public Date getDt() {
		return dt;
	}
	public void setDt(Date dt) {
		this.dt = dt;
	}
	public Float getFlt() {
		return flt;
	}
	public void setFlt(Float flt) {
		this.flt = flt;
	}
	public Long getLng() {
		return lng;
	}
	public void setLng(Long lng) {
		this.lng = lng;
	}
	public Integer getNtgr() {
		return ntgr;
	}
	public void setNtgr(Integer ntgr) {
		this.ntgr = ntgr;
	}
	public String getStrng() {
		return strng;
	}
	public void setStrng(String strng) {
		this.strng = strng;
	}
	public PersistentBean getPrst() {
		return prst;
	}
	public void setPrst(PersistentBean prst) {
		this.prst = prst;
	}
	public String[] getStrngs() {
		return strngs;
	}
	public void setStrngs(String[] strngs) {
		this.strngs = strngs;
	}
	public Long[] getLngs() {
		return lngs;
	}
	public void setLngs(Long[] lngs) {
		this.lngs = lngs;
	}
	
	
		
}
