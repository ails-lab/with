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

import java.util.List;

public class CommonQuery {

	public int page = 1;
	public int pageSize = 20;
	public String searchTerm;
	public String termToExclude;
	public List<String> source;

	public CommonQuery(String generalQueryBody) {
		super();
		this.searchTerm = generalQueryBody;
	}

	public CommonQuery() {
		super();
		searchTerm = "*";
	}

	public String getQuery() {
		return searchTerm;
	}

	public void setQuery(String query) {
		this.searchTerm = query;
	}

	@Override
	public String toString() {
		return "CommonQuery [page=" + page + ", pageSize=" + pageSize + ", searchTerm=" + searchTerm + ", toExclude="
				+ termToExclude + ", source=" + source + "]";
	}

}
