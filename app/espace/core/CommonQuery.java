/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package espace.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import espace.core.sources.DigitalNZSpaceSource;

public class CommonQuery implements Cloneable {

	public String page = "1";
	public String facetsMode = FacetsModes.DEFAULT;
	public String pageSize = "20";
	public String searchTerm;
	public String termToExclude;
	public String user;
	public List<String> source;
	public boolean mintSource = false;
	public boolean uploadedByUser = false;

	public List<CommonFilter> filters;

	public CommonQuery(String generalQueryBody) {
		super();
		this.searchTerm = generalQueryBody;
	}

	public CommonQuery() {
		super();
	}

	public String getQuery() {
		return searchTerm;
	}

	public void setQuery(String query) {
		this.searchTerm = query;
	}

	public List<CommonQuery> splitFilters(ISpaceSource src) {
		if (filters == null || filters.size() == 0)
			return Arrays.asList(this);
		else
			return splitFilters(0, new ArrayList<>(), new ArrayList<CommonQuery>(), src);
	}

	private List<CommonQuery> splitFilters(int i, ArrayList<CommonFilter> arrayList, ArrayList<CommonQuery> result,
			ISpaceSource src) {
		if (i == filters.size()) {
			CommonQuery clone;
			try {

				clone = (CommonQuery) clone();
				clone.filters = (List<CommonFilter>) arrayList.clone();
				result.add(clone);

			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		if (i < filters.size()) {
			for (CommonFilter f : filters.get(i).splitValues(src)) {
				arrayList.add(f);
				splitFilters(i + 1, arrayList, result, src);
				arrayList.remove(f);
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return "CommonQuery [searchTerm=" + searchTerm + ", page=" + page + ", pageSize=" + pageSize + ", source="
				+ source + ", filters=" + filters + "]";
	}

	public void validate() {
		if (!Utils.hasAny(page)) {
			page = "1";
		}
		if (!Utils.hasAny(pageSize)) {
			pageSize = "20";
		}
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getUser() {
		return user;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}
