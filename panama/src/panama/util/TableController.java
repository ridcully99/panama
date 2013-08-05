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
package panama.util;

import java.util.HashMap;
import java.util.Map;

import panama.annotations.Action;
import panama.annotations.Controller;
import panama.collections.Table;
import panama.collections.filters.Filter;
import panama.collections.filters.SearchPropertyComparator;
import panama.core.BaseController;
import panama.core.Context;
import panama.core.Target;
import panama.exceptions.ForceTargetException;



/**
 * @author Ridcully
 * 
 */
@Controller
public class TableController extends BaseController {

	/**
	 * Key used to store DefaultTable-Map in Session-Scope
	 */
	public final static String TABLEMAP_KEY = "panama_tablemap";	
	
	/**
	 * This action sets sorting parameters in a table
	 * Expected parameters in request:
	 * - tableid
	 * - property
	 */
	@Action
	public Target sort() throws ForceTargetException {
		String tableId = context.getParameter("tableid");
		String sortBy = context.getParameter("property");

		Table table = getTable(tableId);

		if (table != null) {
			String oldSortBy = table.getSortBy();
			String oldDirection = table.getSortDirection();
			String direction = Table.SORT_ASC;
			if (sortBy.equals(oldSortBy)) {
				if (oldDirection == Table.SORT_ASC) {
					direction = Table.SORT_DESC;
				} else if (oldDirection == Table.SORT_DESC) {
					direction = Table.SORT_NONE;
					sortBy = null;
				}
			}
			table.setSortBy(sortBy);
			table.setSortDirection(direction);
		}
		return redirect(context.getRequest().getHeader("referer"));
	}
	
	/**
	 * This action sets the currentPage property in a table
	 * Expected parameters in request:
	 * - tableid
	 * - page
	 */
	@Action(alias="turnto")
	public Target turnTo() throws ForceTargetException {
		String tableId = context.getParameter("tableid");
		int page = new Integer(context.getParameter("page")).intValue();

		Table table = getTable(tableId);

		if (table != null) {
			table.setCurrentPage(page);
		}
		return redirect(context.getRequest().getHeader("referer"));
	}			
	
	/**
	 * This action sets the entriesPerPage property in a table model
	 * Expected parameters in request:
	 * - tableid
	 * - epp
	 */	
	@Action(alias="setepp")
	public Target setEpp() throws ForceTargetException {
		String tableId = context.getParameter("tableid");
		int epp = new Integer(context.getParameter("epp")).intValue();

		Table table = getTable(tableId);

		if (table != null) {
			table.setEntriesPerPage(epp);
		}
		return redirect(context.getRequest().getHeader("referer"));
	}			
	
	/**
	 * This action sets the entriesPerPage property in a table model
	 * Expected parameters in request:
	 * - tableid
	 * - enabled (values true or false)
	 */		
	@Action(alias="setpagingenabled")
	public Target setPagingEnabled() throws ForceTargetException {
		String tableId = context.getParameter("tableid");
		String enabled = context.getParameter("enabled");
		Table table = getTable(tableId);
		if (table != null) {
			table.setPagingEnabled(enabled != null && enabled.equals("true"));
		}
		return redirect(context.getRequest().getHeader("referer"));
	}	
	
	/**
	 * This action sets the entriesPerPage property in a table model
	 * Expected parameters in request:
	 * - tableid
	 * - properties a string with the names of the properties to search (separated by non-word character(s))
	 * - q the text to search for
	 */		
	@Action(alias="setsearchquery")
	public Target setSearchQuery() throws ForceTargetException {
		String tableId = context.getParameter("tableid");
		String properties = context.getParameter("properties");
		String query = context.getParameter("q");

		Table table = getTable(tableId);

		if (table != null) {
			if (query != null && query.trim().length() > 0) {
				// \W == non Word-Characters
				String[] propertyArray = properties != null ? properties.split("\\W+") : null;
				SearchPropertyComparator searchfilter = (SearchPropertyComparator)Filter.stdSearchFilter(query, propertyArray);
				table.getFilters().put(Table.SEARCH_FILTER, searchfilter);
			} else {
				table.getFilters().remove(Table.SEARCH_FILTER);
			}
		}
		return redirect(context.getRequest().getHeader("referer"));
	}			
	
	// -------------------------------------------------------------------------------------
	// Useful helper methods
	// -------------------------------------------------------------------------------------

	/**
	 * Gets a table from the table-map in session scope.
	 * @param tableId Unique ID
	 * @return a Table
	 */
	public Table getTable(String tableId) {
		return getTableMap().get(tableId);
	}

	/**
	 * Puts a table into the table-map in the session .
	 * @param table Some kind of table
	 */
	public void addTable(Table table) {
		getTableMap().put(table.getKey(), table);
	}

	/** lazily create tableMap */
	private Map<String, Table> getTableMap() {
		@SuppressWarnings("unchecked")
		Map<String, Table> map = (Map<String, Table>)Context.getInstance().session.get(TABLEMAP_KEY);
		if (map == null) {
			/* create map if not already there */
			map = new HashMap<String, Table>();
			Context.getInstance().session.put(TABLEMAP_KEY, map);			
		}
		return map;
	}
}
