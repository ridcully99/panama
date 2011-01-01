/*
 * Created on 01.08.2005
 *
 */
package panama.tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import panama.collections.DefaultTable;
import panama.collections.ListModel;
import panama.collections.Table;
import panama.core.Context;
import panama.util.TestTimer;

import junit.framework.TestCase;

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
			Context.createInstance(null, null, new MockHttpRequest(), null, null);
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
	
	/* a mock to allow creating a pseudo context and test for caching in request scope */
	class MockHttpRequest implements HttpServletRequest {
		
		private Map attributes = new HashMap();
		private Map parameters = new HashMap();
		
		public Object getAttribute(String key) {
			return attributes.get(key);
		}
		
		public void setAttribute(String key, Object o) {
			attributes.put(key, o);
		}

		public String getAuthType() {
			// TODO Auto-generated method stub
			return null;
		}

		public Cookie[] getCookies() {
			// TODO Auto-generated method stub
			return null;
		}

		public long getDateHeader(String arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

		public String getHeader(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		public Enumeration getHeaders(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		public Enumeration getHeaderNames() {
			// TODO Auto-generated method stub
			return null;
		}

		public int getIntHeader(String arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

		public String getMethod() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getPathInfo() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getPathTranslated() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getContextPath() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getQueryString() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getRemoteUser() {
			// TODO Auto-generated method stub
			return null;
		}

		public boolean isUserInRole(String arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		public Principal getUserPrincipal() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getRequestedSessionId() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getRequestURI() {
			// TODO Auto-generated method stub
			return null;
		}

		public StringBuffer getRequestURL() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getServletPath() {
			// TODO Auto-generated method stub
			return null;
		}

		public HttpSession getSession(boolean arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		public HttpSession getSession() {
			// TODO Auto-generated method stub
			return null;
		}

		public boolean isRequestedSessionIdValid() {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean isRequestedSessionIdFromCookie() {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean isRequestedSessionIdFromURL() {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean isRequestedSessionIdFromUrl() {
			// TODO Auto-generated method stub
			return false;
		}

		public Enumeration getAttributeNames() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getCharacterEncoding() {
			// TODO Auto-generated method stub
			return null;
		}

		public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {
			// TODO Auto-generated method stub
			
		}

		public int getContentLength() {
			// TODO Auto-generated method stub
			return 0;
		}

		public String getContentType() {
			// TODO Auto-generated method stub
			return null;
		}

		public ServletInputStream getInputStream() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		public String getParameter(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		public Enumeration getParameterNames() {
			// TODO Auto-generated method stub
			return null;
		}

		public String[] getParameterValues(String arg0) {
			return new String[0];
		}

		public Map getParameterMap() {
			return parameters;
		}

		public String getProtocol() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getScheme() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getServerName() {
			// TODO Auto-generated method stub
			return null;
		}

		public int getServerPort() {
			// TODO Auto-generated method stub
			return 0;
		}

		public BufferedReader getReader() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		public String getRemoteAddr() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getRemoteHost() {
			// TODO Auto-generated method stub
			return null;
		}

		public void removeAttribute(String arg0) {
			// TODO Auto-generated method stub
			
		}

		public Locale getLocale() {
			// TODO Auto-generated method stub
			return null;
		}

		public Enumeration getLocales() {
			// TODO Auto-generated method stub
			return null;
		}

		public boolean isSecure() {
			// TODO Auto-generated method stub
			return false;
		}

		public RequestDispatcher getRequestDispatcher(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		public String getRealPath(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		public String getLocalAddr() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getLocalName() {
			// TODO Auto-generated method stub
			return null;
		}

		public int getLocalPort() {
			// TODO Auto-generated method stub
			return 0;
		}

		public int getRemotePort() {
			// TODO Auto-generated method stub
			return 0;
		}
	}
}
