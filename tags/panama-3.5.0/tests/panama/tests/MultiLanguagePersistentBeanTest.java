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
package panama.tests;

import java.util.ArrayList;
import java.util.Locale;

import panama.persistence.PolyglotPersistentBean;

import junit.framework.TestCase;

public class MultiLanguagePersistentBeanTest extends TestCase {

	public MultiLanguagePersistentBeanTest(String arg0) {
		super(arg0);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testEqualsAndHashcode() throws Exception {
		PolyglotPersistentBean a = new PolyglotPersistentBean();
		PolyglotPersistentBean b = new PolyglotPersistentBean();
		
		assertFalse(a.equals(b));
		assertFalse(a.hashCode() == b.hashCode());
		
		b.setId(a.getId());
		b.setLanguage(a.getLanguage());
		
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}
	
	public void testTranslateTo() throws Exception {
		TestTestBean a = new TestTestBean();
		a.setI(new Integer(42));
		a.setS("Per Anhalter durch die Galaxis");
		a.setD(999.99d);
		a.setL(new ArrayList());
		a.setDd(new Double(Math.PI));
		TestTestBean b = (TestTestBean)a.translateTo(Locale.ENGLISH);
		assertEquals(a.getI(), b.getI());
		assertEquals(a.getS(), b.getS());
		assertFalse(a.getD() == b.getD()); /* cannot copy primitives */
		assertFalse(a.getL().equals(b.getL()));
		assertEquals(a.getDd(), b.getDd());
	}
}
