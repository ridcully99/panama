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
package panama.collections;

import java.util.List;
import java.util.Map;
import java.util.Set;

import panama.collections.filters.Filter;


public interface Table {

	public final static String SORT_ASC = "asc";
	public final static String SORT_DESC = "desc";
	public final static String SORT_NONE = "none";

	public final static String SEARCH_FILTER = "searchfilter";

	/**
	 * Returns unique key for the table
	 * @return unique key of the table
	 */
	public String getKey();

	/**
	 * Sets model for the table.
	 * @param model
	 */
	public Table setModel(ListModel model);

	/**
	 * Gets model of table.
	 * @return a ListModel
	 */
	public ListModel getModel();

	/**
	 * Gets all Filters currently set for the table.
	 * @return a Map of Filters set via {@link #setFilter(String, Filter)}.
	 */
	public Map<String, Filter> getFilters();

	/**
	 * Sets specified filter using the specified name. 
	 * @param name
	 * @param filter
	 * @return the Table object
	 */
	public Table setFilter(String name, Filter filter);
	
	/**
	 * Removes filter with specified name. 
	 * If no filter with that name is found, nothing is done, especially no Exception is thrown.
	 * @param name
	 * @return the Table object.
	 */
	public Table removeFilter(String name);
	
	/**
	 * Removes all filters.
	 * @return the Table object.
	 */
	public Table clearFilters();
	
	/**
	 * Gets a set allowing you to keep track of selected rows.
	 * You may e.g. add or remove some rows or ids or whatever you like to this set
	 * and may query it later to see what rows are in there.
	 * @return a set
	 */
	public Set<? extends Object> getSelected();

	/**
	 * Gets a sorted list of rows (objects)
	 * Sorted by the property specified by setSortBy() and direction specified by setSortDirection()
	 * @return a list of objects or null
	 */
	public List<? extends Object> getRows();

	/**
	 * Gets list of rows on current page
	 * @return a list of objects or null
	 */
	public List<? extends Object> getPageRows();

	public String getSortBy();

	/**
	 * Sets the name of the property the rows are sorted by
	 * @param sortBy Name of a property of the rows - must be accessible by a matching getter; e.g. name --> getName()
	 */
	public Table setSortBy(String sortBy);

	/**
	 * Sets the name of the property the rows are sorted by and the sort direction.
	 * @see Table#setSortBy(String)
	 * @see #setSortDirection(String)
	 * @param sortBy
	 * @param sortDirection
	 * @return the Table object
	 */
	public Table setSortBy(String sortBy, String sortDirection);

	public String getSortDirection();

	/**
	 * Sets sortDirection
	 * @param sortDirection Must be one of SORT_ASC, SORT_DESC or SORT_NONE
	 */
	public Table setSortDirection(String sortDirection);

	/**
	 * Gets current page; is in interval 1 .. pageCount
	 * @return current page
	 */
	public int getCurrentPage();

	/**
	 * Sets current page; if currentPage is not in valid interval, it's clipped accordingly
	 * @param currentPage allowed values are from 1 to pageCount
	 */
	public Table setCurrentPage(int currentPage);

	/**
	 * Gets entries per page.
	 * @see #setEntriesPerPage
	 * @return The number of entries per page, specified for this table
	 */
	public int getEntriesPerPage();

	/**
	 * Sets entries per page to given value.
	 * @param entriesPerPage
	 */
	public Table setEntriesPerPage(int entriesPerPage);

	/**
	 * Returns the number of rows in the model.
	 * @return The number of rows
	 */
	public int getRowCount();

	/**
	 * Returns number of pages, depending to number of rows and entries per page.
	 * @return The number of pages
	 */
	public int getPageCount();

	/**
	 * Sets flag to enable/disable paging.
	 * If paging is disabled, all rows are returned.
	 * @param pagingEnabled
	 */
	public Table setPagingEnabled(boolean pagingEnabled);

	/**
	 * Gets current state of paging
	 * @return wether paging is enabled or not.
	 */
	public boolean getPagingEnabled();

	/**
	 * Creates a link (with TableController) for sorting model of this table by given property.
	 * The sort direction is calculated automatically based on the current direction.
	 * @param property
	 * @return a link invoking TableController
	 */
	public String sortLink(String property);

	/**
	 * Creates a link for setting current page of this table.
	 *
	 * @param page desired page, allowed values are 1 .. pageCount
	 * @return a link invoking TableController
	 */
	public String pageLink(int page);

	/**
	 * Creates a link for setting entries per page for this table.
	 *
	 * @param epp
	 * @return a link invoking TableController
	 */
	public String eppLink(int epp);

	/**
	 * Creates a link for use in search form
	 * @param propertyNames names of properties to search in
	 * @return a link invoking TableController
	 */
	public String searchLink(String... propertyNames);

	/**
	 * Creates a link for enabling/disabling paging for this table.
	 * @param enabled
	 * @return a link invoking TableController
	 */
	public String pagingEnabledLink(boolean enabled);
}