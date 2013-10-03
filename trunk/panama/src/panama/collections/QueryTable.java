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

import java.util.List;

import panama.collections.DefaultTable;
import panama.core.Context;


/**
 * @author ridcully
 *
 */
public class QueryTable extends DefaultTable {

	private String rowCountCache = cacheCode + "_rowCount";

	public QueryTable(String key, QueryListModel model) {
		super(key, model);
	}

	/** {@inheritDoc} */
	@Override
	public List<? extends Object> getRows() {
		boolean paged = getPagingEnabled();
		try {
			setPagingEnabled(false);
			return fetchRows();
		} finally {
			setPagingEnabled(paged);
		}
	}

	/** {@inheritDoc} */
	@Override
	public List<? extends Object> getPageRows() {
		return fetchRows();
	}

	/**
	 * {@inheritDoc}
	 * The rowcount value if cached for the lifespan of the current request, as this method might be invoked several times during one request.
	 */
	@Override
	public int getRowCount() {
		Integer cnt = null;
		int n = 0;
		Context ctx = Context.getInstance();
		if (ctx != null) {
			cnt = (Integer)ctx.get(rowCountCache);
		}
		if (cnt == null) {
			n = ((QueryListModel)model).getRowCount();
			if (ctx != null) {
				ctx.put(rowCountCache, new Integer(n));
			}
		} else {
			n = cnt.intValue();
		}
		return n;
	}
}
