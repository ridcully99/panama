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
package panama.tests.collections;

import java.util.ArrayList;
import java.util.List;

import panama.collections.DefaultTable;
import panama.collections.SimpleListModel;
import panama.tests.entities.Entry;

import junit.framework.TestCase;

public class DefaultTableTest extends TestCase {

	public DefaultTableTest(String arg0) {
		super(arg0);
	}

	public void testSorting() {
		List l = new ArrayList();
		l.add(new Entry("c"));
		l.add(new Entry("a"));
		l.add(new Entry("b"));
		DefaultTable t = new DefaultTable("key", new SimpleListModel(l));
		t.setSortBy("name");
		List sorted = t.getRows();
		assertNotNull(sorted);
		System.out.println(sorted);
	}
	
	public void testSortingWithNulls() {
		List l = new ArrayList();
		l.add(new Entry("c"));
		l.add(new Entry("a"));
		l.add(new Entry(null));		
		l.add(new Entry("b"));
		l.add(new Entry(null));		
		DefaultTable t = new DefaultTable("key", new SimpleListModel(l));
		t.setSortBy("name");
		List sorted = t.getRows();
		assertNotNull(sorted);
		System.out.println(sorted);
	}

	public void testSortingWithDesc() {
		List l = new ArrayList();
		l.add(new Entry("c"));
		l.add(new Entry("a"));
		l.add(new Entry(null));		
		l.add(new Entry("b"));
		l.add(new Entry(null));		
		DefaultTable t = new DefaultTable("key", new SimpleListModel(l));
		t.setSortBy("name");
		t.setSortDirection(DefaultTable.SORT_DESC);
		List sorted = t.getRows();
		assertNotNull(sorted);
		System.out.println(sorted);
	}	

	public void testSortingCaseInsensitive() {
		List l = new ArrayList();
		l.add(new Entry("c"));
		l.add(new Entry("a"));
		l.add(new Entry("b"));
		l.add(new Entry("C"));
		l.add(new Entry("A"));
		l.add(new Entry("B"));
		DefaultTable t = new DefaultTable("key", new SimpleListModel(l));
		t.setSortBy("name");
		List sorted = t.getRows();
		assertNotNull(sorted);
		System.out.println(sorted);
	}	
}
