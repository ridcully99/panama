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
import java.util.List;

import junit.framework.TestCase;
import panama.collections.DefaultTable;
import panama.collections.ListModel;
import panama.collections.Table;
import panama.core.Context;
import panama.util.TestTimer;

public class TableTest extends TestCase {

	private DefaultTable table;

	public TableTest(String arg0) {
		super(arg0);
	}

	protected void setUp() throws Exception {
		table = new DefaultTable("key");
		startNewRequest();
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Simulates beginning of new request
	 *
	 */
	private void startNewRequest() {
		/**
		 * Create a pseudo context to test caching
		 */
		try {
			Context.createInstance(null, null, new MockRequest(), null, null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void testGetPageCount() throws Exception {
		/* with no rows */
		startNewRequest();
		assertEquals(1, table.getPageCount());
		/* with rows */
		startNewRequest();
		table.setEntriesPerPage(10);
		table.setModel(new ArrayListModel(9));
		assertEquals(1, table.getPageCount());
		startNewRequest();
		table.setModel(new ArrayListModel(10));
		assertEquals(1, table.getPageCount());
		startNewRequest();
		table.setModel(new ArrayListModel(11));
		assertEquals(2, table.getPageCount());
	}

	public void testGetRowsPaged() throws Exception {
		/* with no rows */
		startNewRequest();
		assertEquals(null, table.getRows());
		/* with rows */
		startNewRequest();
		table.setModel(new ArrayListModel(37));
		List l = table.getPageRows();
		assertEquals(10, l.size());
		startNewRequest();
		table.setCurrentPage(4);
		l = table.getPageRows();
		assertEquals(7, l.size());
		startNewRequest();
		table.setCurrentPage(99); /* should be trimmed to 4 */
		l = table.getPageRows();
		assertEquals(7, l.size());
	}

	public void testSortSpeed() {
		/* with no rows */
		startNewRequest();
		table.setSortBy("s");
		table.setSortDirection(Table.SORT_ASC);
		TestTimer timer = new TestTimer("no rows");
		table.getRows();
		timer.done();
		/* with different list sizes */
		int[] sizes = new int[] {100, 500, 1000, 5000, 10000, 20000};
		/* sort by string property ascending */
		table.setSortBy("s");
		table.setSortDirection(Table.SORT_ASC);
		for (int i=0; i<sizes.length; i++) {
			startNewRequest();
			table.setModel(new ArrayListModel(sizes[i]));
			timer = new TestTimer(sizes[i]+" rows sort by string asc");
			table.getRows();
			timer.done();
		}
		/* sort by double property ascending */
		table.setSortBy("d");
		table.setSortDirection(Table.SORT_ASC);
		for (int i=0; i<sizes.length; i++) {
			startNewRequest();
			table.setModel(new ArrayListModel(sizes[i]));
			timer = new TestTimer(sizes[i]+" rows sort by double asc");
			table.getRows();
			timer.done();
		}
	}

	public void testCaching() {
		startNewRequest();
		table.setModel(new SlowListModel(10));
		TestTimer timer = new TestTimer("slow list first time");
		assertEquals(10, table.getRowCount());
		timer.done();
		timer = new TestTimer("slow list second time (cached)");
		assertEquals(10, table.getRowCount());
		timer.done();
	}

	// Mock Classes

	class ArrayListModel implements ListModel {

		List l;
		Table table;

		public ArrayListModel(int n) {
			l = new ArrayList();
			for (int i=0; i<n; i++) {
				double d = Math.random()*1000d;
				l.add(new MockEntry("entry"+d, new Double(d)));
			}
		}

		public List getList() {
			return l;
		}

		public void setTable(Table table) {
			this.table = table;
		}
	}

	class SlowListModel extends ArrayListModel {
		public SlowListModel(int n) {
			super(n);
		}
		public List getList() {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return super.getList();
		}
	}

	class MockEntry {

		private String s;
		private Double d;

		public MockEntry(String s, Double d) {
			setS(s);
			setD(d);
		}

		public Double getD() {
			return d;
		}

		public void setD(Double d) {
			this.d = d;
		}

		public String getS() {
			return s;
		}

		public void setS(String s) {
			this.s = s;
		}
	}
}
