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
package panama.collections;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.rmi.dgc.VMID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.grlea.log.SimpleLogger;

import panama.core.Context;
import panama.filter.Filter;
import panama.filter.FilterExtension;
import panama.util.DynaBeanUtils;
import panama.util.TableController;




/**
 * Table implementing paging and sorting and filtering.
 * @author Ridcully
 */
public class DefaultTable implements Table, Comparator, Serializable {
	
	/* Logging */
	protected static SimpleLogger log = new SimpleLogger(DefaultTable.class);	
	
	protected final String key;
	/* model should be transient so registerTable can detect that the session was restored, 
	 * and take the model from the inital table; 
	 * at least for QueryTable this is mandatory as it's query is transient too 
	 * and would be null otherwise after a restored session 
	 */
	protected transient ListModel model = null;
	protected Set selected = new HashSet();
	protected Map<String, Filter> filters = new HashMap<String, Filter>();
	protected Map<String, Map<String, FilterExtension>> filterExtensions = new HashMap<String, Map<String, FilterExtension>>();
	protected String cacheCode = new VMID().toString();		/* a unique identifier */
	private String isSorted = cacheCode + "_isSorted";		/* another one */
	private String isFiltered = cacheCode + "_isFiltered";	/* and another one */
	
	protected String sortBy = null;
	protected String sortDirection = Table.SORT_ASC;
	protected String getterName = null;
	protected Method sortByGetter = null;
	protected int currentPage = 1;
	protected int entriesPerPage = 10;
	protected boolean pagingEnabled = true;	// can be set to false to avoid paging and show all rows
	
	public DefaultTable(String key) {
		this.key = key;
	}
	
	public DefaultTable(String key, ListModel model) {
		this.key = key;
		setModel(model);
	}	
	
	public String getKey() {
		return key;
	}
	
	public ListModel getModel() {
		return model;
	}

	public Table setModel(ListModel model) {
		this.model = model;
		model.setTable(this);
		/* clear cache */
		Context ctx = Context.getInstance();
		if (ctx != null) {
			ctx.getRequest().setAttribute(cacheCode, null);
		}
		return this;
	}

	/**
	 * Fetches rows from cache (i.e. request context) or model.
	 * Thus the list is only fetched from model once per request.
	 * @return List of rows.
	 */
	protected List fetchRows() {
		log.debug("about to fetch rows from cache or model");
		List rows = null;
		/* check if list already fetched in current Request */
		Context ctx = Context.getInstance();
		if (ctx != null) {
			rows = (List)ctx.get(cacheCode);
		}
		if (rows == null && model != null) {
			rows = model.getList();
			log.debug("fetched rows from model");
			if (ctx != null) {
				ctx.put(cacheCode, rows);
			}
		}
		return rows;
	}
	
	/**
	 * @see panama.collections.Table#getRows()
	 */	
	public List getRows() {
		try {
			List rows = fetchRows();
			if (rows == null) { return null; }
			Context ctx = Context.getInstance();

			/* Apply filter(s) if existent and not already applied in current request */
			if (!filters.isEmpty() && (ctx == null || ctx.get(isFiltered) == null)) {
				rows = applyFilters(rows);
				if (ctx != null) {
					ctx.put(isFiltered, Boolean.TRUE);
				}
			}
			if (getSortBy() != null) {
				/* check if list already sorted in current Request */
				if (ctx == null || ctx.get(isSorted) == null) {
					try {
						sortRows(rows);
					} catch (Exception e) {
						log.debug("Error while sorting: "+e.getMessage());
						/* do not sort */
					}
					if (ctx != null) {
						ctx.put(isSorted, Boolean.TRUE);
					}
				}
			}
			return rows;
		} catch (Exception e) {
			log.error("getRows() failed: "+e.getMessage());
			log.errorException(e);
			return null;
		}		
	}

	/**
	 * @see panama.collections.Table#getPageRows()
	 */	
	public List getPageRows() {
		List rows = getRows();
		if (rows == null) { return null; }
		/* invoke paging after sorting (which is done in getRows()) */
		if (getPageCount() > 1) {
			List part = new ArrayList();
			int start = (getCurrentPage()-1) * getEntriesPerPage();
			for (int i=start; (i<start+getEntriesPerPage()) && (i < rows.size()); i++) {
				part.add(rows.get(i));
			}
			return part;
		} else {
			return rows;
		}
	}

	/**
	 * @see panama.collections.Table#getRowCount()
	 */
	public int getRowCount() {
		List rows = getRows();
		return rows != null ? rows.size() : 0;
	}
	
	/**
	 * Sorts the specified list.
	 * @param rows
	 */
	protected void sortRows(List rows) throws Exception {
		if (rows == null || rows.size() == 0) {
			return;
		}
		/*
		 * find a getter method - this is just a speed up,
		 * under certain circumstances it will be necessary to find another one
		 * while sorting. 
		 * @see #compare()
		 */
		Class clazz = rows.get(0).getClass();
		try {
			sortByGetter = clazz.getMethod(getterName);
		} catch (Exception e) {
			sortByGetter = null;
		}
		Collections.sort(rows, this);
	}
	
	/**
	 * The compare method for sorting our rows,
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(Object o1, Object o2) {
		try {
			Comparable p1 = getCompareValue(o1);
			Comparable p2 = getCompareValue(o2);
			if (sortDirection == Table.SORT_DESC) {	// no equals() needed here
				return p2 == null ? (p1 == null ? 0 : -1 ) : (p1 == null ? 1 : p2.compareTo(p1));
			} else {
				return p1 == null ? (p2 == null ? 0 : -1 ) : (p2 == null ? 1 : p1.compareTo(p2));				
			}
		} catch (Exception e) {
			return 0; 	// do not sort if any errors occur
		}
	}
	
	/**
	 * Tries to get a value from specified object.
	 * If the sortByGetter is null DynaBeanUtils.getProperty() is used, Otherwise the predefined sortByGetter
	 * is used.
	 * This happens if the object is not from exactly the same class as the one the sortByGetter is created from.
	 * At last, if the value is instanceof String it is converted to lowercase for case-insensitive sorting 
	 * @param o
	 * @return the value of the property specified by sortBy of the specified object.
	 */
	protected Comparable getCompareValue(Object o) {
		Comparable v = null;
		if (o == null) { return null; }
		try {
			if (sortByGetter == null) {
				v = (Comparable)DynaBeanUtils.getProperty(o, getSortBy());
			} else {
				v = (Comparable)sortByGetter.invoke(o);
			}
		} catch (Exception e) {
			return null;  /* give up */
		}
		if (v != null && v instanceof String) {
			v = ((String)v).toLowerCase();
		}
		return v;
	}
	
	/**
	 * @see panama.collections.Table#getSortBy()
	 */
	public String getSortBy() {
		return sortBy;
	}
	
	/**
	 * @see panama.collections.Table#setSortBy(java.lang.String)
	 */
	public Table setSortBy(String sortBy) {
		if (sortBy != null && sortBy.length() == 0) { sortBy = null; }
		this.sortBy = sortBy;
		if (sortBy != null) {
			this.getterName = "get"+sortBy.substring(0, 1).toUpperCase();
			if (sortBy.length() > 1) {
				this.getterName += sortBy.substring(1);
			}
		}
		return this;
	}
	
	/**
	 * @see panama.collections.Table#getSortDirection()
	 */
	public String getSortDirection() {
		return sortDirection;
	}
		
	/**
	 * @see panama.collections.Table#setSortDirection(java.lang.String)
	 */
	public Table setSortDirection(String sortDirection) {
		this.sortDirection = sortDirection;
		return this;
	}

	public Table setSortBy(String sortBy, String sortDirection) {
		setSortBy(sortBy);
		setSortDirection(sortDirection);
		return this;
	}
	
	
	/**
	 * @see panama.collections.Table#getCurrentPage()
	 */
	public int getCurrentPage() {
		/* always check if pagecount is still valid (might become invalid due to changes in the model) */
		int pageCount = getPageCount();
		if (currentPage > pageCount) { currentPage = pageCount; }
		return currentPage;
	}

	/**
	 * @see panama.collections.Table#setCurrentPage(int)
	 */
	public Table setCurrentPage(int currentPage) {
		if (currentPage < 1) { currentPage = 1; }
		if (currentPage > getPageCount()) { currentPage = getPageCount(); }
		this.currentPage = currentPage;
		return this;
	}

	/**
	 * @see panama.collections.Table#getEntriesPerPage()
	 */
	public int getEntriesPerPage() {
		if (getPagingEnabled()) {
			return entriesPerPage;			
		} else {
			return Integer.MAX_VALUE;
		}
	}

	/**
	 * @see panama.collections.Table#setEntriesPerPage(int)
	 */
	public Table setEntriesPerPage(int entriesPerPage) {
		if (entriesPerPage <= 0) {
			entriesPerPage = 10;
		}
		this.entriesPerPage = entriesPerPage;
		return this;
	}

	/**
	 * @see panama.collections.Table#getPageCount()
	 */
	public int getPageCount() {
		if (getEntriesPerPage() == 0) { return 1; }
		int rowCount = getRowCount();	//List rows = getRows();
		if (rowCount == 0) { return 1; }		
		/* next line: *1.0 to convert to double, otherwise division would be made on int basis */
		return new Double(Math.ceil((rowCount*1.0) / getEntriesPerPage())).intValue();
	}

	public Table setPagingEnabled(boolean pagingEnabled) {
		this.pagingEnabled = pagingEnabled;
		return this;
	}
		
	public boolean getPagingEnabled() {
		return pagingEnabled;
	}

	public Set getSelected() {
		return selected;
	}

	/**
	 * Gets the tables Map of Filters. 
	 * @return A Map of Filters
	 */
	public Map<String, Filter> getFilters() {
		return filters;
	}

	/**
	 * Gets the tables Map of FilterExtensions. 
	 * @return A Map of Map<propertyName, FilterExtension>
	 */
	public Map<String, Map<String, FilterExtension>> getFilterExtensions() {
		return filterExtensions;
	}	
	
	public Table putFilterExtension(String filterKey, String propertyName, FilterExtension extension) {
		if (!filterExtensions.containsKey(filterKey)) {
			filterExtensions.put(filterKey, new HashMap<String, FilterExtension>());
		}
		filterExtensions.get(filterKey).put(propertyName, extension);
		return this;
	}
	
	/**
	 * Applies all filters onto the specified rows. The original list is not changed by this method.
	 * It is probably faster to iterate over the rows for every filter as with some luck
	 * the first filter will already remove some rows, so further filters have less work.
	 * @param rows A list of rows
	 * @return The filtered list of rows (the original list is not changed by this method)
	 */
	protected List applyFilters(List rows) {
		List result = new ArrayList(rows);
		for (Map.Entry<String, Filter> e : getFilters().entrySet()) {
			String k = e.getKey();
			Filter f = e.getValue();
			Map<String, FilterExtension> extensions = filterExtensions.get(k);
			for (Iterator it = result.iterator(); it.hasNext(); ) {
				if (!f.match(it.next(), extensions)) {
					it.remove();
				}
			}
		}
		return result;
	}
	
	/**
	 * @see panama.collections.Table#sortLink(java.lang.String)
	 */
	public String sortLink(String property) {
		return "../"+TableController.class.getName()+"/sort?tableid="+key+"&property="+property;
	}
	
	/**
	 * @see panama.collections.Table#pageLink(int)
	 */
	public String pageLink(int page) {
		return "../"+TableController.class.getName()+"/turnto?tableid="+key+"&page="+page;
	}
	
	/**
	 * @see panama.collections.Table#eppLink(int)
	 */
	public String eppLink(int epp) {
		return "../"+TableController.class.getName()+"/setepp?tableid="+key+"&epp="+epp;
	}

	/**
	 * @see panama.collections.Table#searchLink(String...)
	 */
	public String searchLink(String... properties) {
		StringBuffer b = new StringBuffer("../"+TableController.class.getName()+"/setsearchquery?tableid="+key+"&properties=");
		if (properties != null) {
			boolean first = true;
			for (String p : properties) {
				if (!first) {
					b.append(",");
				}
				b.append(p);
				first = false; 
			}
		}
		return b.toString();
	}
	
	/**
	 * @see panama.collections.Table#pagingEnabledLink(boolean)
	 */
	public String pagingEnabledLink(boolean enabled) {
		return "../"+TableController.class.getName()+"/setpagingenabled?tableid="+key+"&enabled="+enabled;		
	}	
}
