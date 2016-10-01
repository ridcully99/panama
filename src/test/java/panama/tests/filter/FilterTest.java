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
package panama.tests.filter;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;
import panama.collections.filters.Filter;
import panama.tests.entities.Entry;
import panama.util.TestTimer;


public class FilterTest extends TestCase {

	Entry e;

	public FilterTest(String arg0) {
		super(arg0);
	}

	public void setUp() {
		e = new Entry();
		e.setEmail("foo@bar.at");
		e.setName("Mustrum Ridcully");
		e.setMessage("Hello Pandora!\r\n");
		e.setTimeStamp(new Date());
	}

	public void testPatternMatching() throws Exception {
		Pattern regEx = Pattern.compile(".*;locale=([a-z]*).*");
		String str2 = "http://localhost:8080/heureka/demo.Demo/page.now;locale=de";
		Matcher m = regEx.matcher(str2);
		assertTrue(m.matches());
		System.out.println(m.groupCount());
		System.out.println(m.group(1));
	}

	public void testPropertyComparator() throws Exception {
		assertTrue(Filter.anyEq("Mustrum Ridcully", "email", "name").match(e));
		assertFalse(Filter.noneEq("Mustrum Ridcully", "email", "name").match(e));
		assertFalse(Filter.allEq("Mustrum Ridcully", "email", "name").match(e));
	}

	public void testLogicalExpressions() {
		Filter f = Filter.or(Filter.anyEq("Mustrum Ridcully", "email", "name"),
							Filter.noneEq("Mustrum Ridcully", "email", "name"));
		System.out.println(f);
		assertTrue(f.match(e));
		f = Filter.and(Filter.anyEq("Mustrum Ridcully", "email", "name"),
						   Filter.noneEq("Mustrum Ridcully", "email", "name"));
		System.out.println(f);
		assertFalse(f.match(e));
		f = Filter.not(Filter.noneEq("Mustrum Ridcully", "email", "name"));
		System.out.println(f);
		assertTrue(f.match(e));
	}

	public void testRegExpPropertyComparator() {
		assertTrue(Filter.anyMatches("Mustrum Ridcully", "email", "name").match(e));
		assertFalse(Filter.noneMatches("Mustrum Ridcully", "email", "name").match(e));
		assertFalse(Filter.allMatch("Mustrum Ridcully", "email", "name").match(e));

		assertTrue(Filter.anyMatches("M.* Ridcully", "email", "name").match(e));
		assertTrue(Filter.allMatch(".*", "email", "name").match(e));
	}

	public void testPat() {
		assertTrue("ridcully".matches("rid.*"));
		assertFalse("ridcully".matches("\\Q\\\\qhuh\\\\e\\E"));
	}

	public void testSearchPropertyComparator() {
		assertTrue(Filter.stdSearchFilter("rid", "email", "name").match(e));
		// complete pattern is quoted, so this does _not_ match ridcully!
		assertFalse(Filter.stdSearchFilter("rid.*", "email", "name").match(e));
		assertFalse(Filter.stdSearchFilter("+", "email", "name").match(e));
		assertFalse(Filter.stdSearchFilter("\\Qhuh\\E", "email", "name").match(e));
		assertFalse(Filter.stdSearchFilter("\\E+", "email", "name").match(e));
	}

	public void testToString() {
		Filter f = Filter.or(Filter.anyEq("Mustrum Ridcully", "email", "name"),
							Filter.noneEq("Mustrum Ridcully", "email", "name"));
		Filter f2 = Filter.and(f, f);
		System.out.println(f2);
	}

//	public void testFilterExtensions() {
//		FilterExtension ex = new FilterExtension() {
//			public boolean matchProperty(String name, Object value, String pattern) {
//				return false;
//			}
//
//			public Expression forProperty(String name, String pattern) {
//				return null;
//			}
//		};
//		Filter f = Filter.anyMatches("Mustrum Ridcully", "email", "name");
//		assertTrue(f.match(e));
//		f.setExtension("name", ex);
//		assertFalse(f.match(e));
//	}

	public void testPerformance() {
		TestTimer tt = new TestTimer("anyMatches");
		boolean b = false;
		for (int i=0; i<10000; i++) {
			b = Filter.anyMatches("M.* Ridcully", "email", "name").match(e);
		}
		tt.done();
		assertTrue(b);

		tt = new TestTimer("stdSearchFilter");
		for (int i=0; i<100000; i++) {
			b = Filter.stdSearchFilter("rid", "email", "name").match(e);
		}
		tt.done();
		assertTrue(b);

		tt = new TestTimer("stdSearchFilter precompiled");
		Filter f = Filter.stdSearchFilter("rid", "email", "name");
		for (int i=0; i<100000; i++) {
			b = f.match(e);
		}
		tt.done();
		assertTrue(b);
	}
}
