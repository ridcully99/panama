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

import panama.annotations.Action;
import panama.annotations.Controller;
import panama.collections.Table;
import panama.collections.filters.Filter;
import panama.collections.filters.SearchPropertyComparator;
import panama.core.BaseController;
import panama.core.Target;
import panama.exceptions.ForceTargetException;



/**
 * @author Ridcully
 * 
 */
@Controller
public class TableController extends BaseController {

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
				// Split at any non-word characters except for . which may be used for sub-properties
				String[] propertyArray = properties != null ? properties.split("[^.\\w]+") : null;
				SearchPropertyComparator searchfilter = (SearchPropertyComparator)Filter.stdSearchFilter(query, propertyArray);
				table.setFilter(Table.SEARCH_FILTER, searchfilter);
			} else {
				table.removeFilter(Table.SEARCH_FILTER);
			}
		}
		return redirect(context.getRequest().getHeader("referer"));
	}			
}
