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
package panama.tests.form;

import junit.framework.TestCase;
import panama.form.Form;
import panama.form.FormData;
import panama.util.DynaBeanUtils;

public class FormTest extends TestCase {

	public FormTest(String arg0) {
		super(arg0);
	}

	public void testAllProperties() {
		TestBean b = new TestBean();
		String[] list = DynaBeanUtils.getPropertyNames(b);
		assertEquals(10, list.length);
	}
	
	public void testAddFields() {
		Form f = new Form(TestBean.class);
		assertEquals(10, f.getFields().size());	
		
		Form f2 = new Form(TestBean.class, "bln", "dt", "dbl");
		assertEquals(3, f2.getFields().size());

		Form f3 = new Form(TestBean.class).except("bln", "dt", "dbl");
		assertEquals(7, f3.getFields().size());
		assertNull(f3.getField("bln"));
		
		Form f4 = new Form(TestBean.class, "unknown");	// should log an error msg at debug level.
		assertEquals(0, f4.getFields().size());		
	}
	
	public void testPrimitives() {
		Form f = new Form();
		f.addFields(PrimitiveTestBean.class);
		FormData fd = new FormData(f);
		fd.setInput("lng", "42");
		assertTrue(42L == fd.getLong("lng"));
		PrimitiveTestBean dest = new PrimitiveTestBean();
		fd.applyTo(dest);
		assertTrue(42L == dest.getLng());
		fd.clearInput();
		dest = new PrimitiveTestBean();
		fd.applyTo(dest);
		assertTrue(0 == dest.getLng());
		assertTrue(0 == fd.getLong("lng"));
		fd.setInput("lng", "NaN");
		assertTrue(0 == fd.getLong("lng"));
	}

}
