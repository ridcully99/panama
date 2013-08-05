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
import java.util.List;
import java.util.Map;

import panama.filter.Filter;

import com.avaje.ebean.Query;


/**
 * @author ridcully
 *
 */
public class QueryListModel implements ListModel, Serializable {

	private static final long serialVersionUID = 1L;

	protected Table table = null;
	protected transient Query query = null;

	public QueryListModel(Query query) {
		this.query = query;
	}

	/* (non-Javadoc)
	 * @see panama.collections.ListModel#getList()
	 */
	public List getList() {
		if (query == null) {
			return null;
		} else {
			applySorting(query);
			Query q = applyFilters(query);
			if (table.getPagingEnabled()) {
				q.setFirstRow((table.getCurrentPage() - 1) * table.getEntriesPerPage());
				q.setMaxRows(table.getEntriesPerPage());
			}
			return q.findList();
		}
	}

	protected void applySorting(Query q) {
		if (table.getSortBy() != null && table.getSortDirection() != Table.SORT_NONE) {
			q.orderBy(table.getSortBy()+" "+table.getSortDirection());
		} else {
			q.orderBy("");
		}
	}

	/**
	 * @param query
	 * @return a new Query object (even if no filters exist)
	 */
	protected Query applyFilters(Query query) {
		Query q = query.copy();
		if (table.getFilters().isEmpty()) {
			return q;
		}
		for (Map.Entry<String, Filter> e : table.getFilters().entrySet()) {
			String k = e.getKey();
			Filter f = e.getValue();
			q.where(f.asExpression(q));
		}
		return q;
	}

	public int getRowCount() {
		if (query == null) {
			return 0;
		} else {
			applySorting(query);
			Query q = applyFilters(query);
			return q.findRowCount();
		}
	}

	/* (non-Javadoc)
	 * @see panama.collections.ListModel#setTable(panama.collections.Table)
	 */
	public void setTable(Table table) {
		this.table = table;
	}
}
