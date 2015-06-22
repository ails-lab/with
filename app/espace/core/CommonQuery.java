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

import java.util.Arrays;
import java.util.List;

public class CommonQuery {

	public static class CoordinateRange {
		public String startPoint;
		public String endPoint;
	}

	public static class SpatialParams {
		public CoordinateRange latitude;
		public CoordinateRange longitude;
	}

	public static class Refinement {
		public List<String> refinementTerms;
		public SpatialParams spatialParams;
	}

	public static class Facets {
		public List<String> TYPE;
		public List<String> LANGUAGE;
		public List<String> YEAR;
		public List<String> COUNTRY;
		public List<String> RIGHTS;
		public List<String> PROVIDER;
		public List<String> UGC;

	}

	public static class EuropeanaAPI {
		public String who;
		public String where;
		public Facets facets;
		public Refinement refinement;
		public List<String> reusability;

	}

	public String page = "1";
	public String pageSize = "20";
	public String searchTerm;
	public String termToExclude;
	public String user;
	public List<String> source;

	public List<CommonFilter> filters;

	public EuropeanaAPI europeanaAPI;

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

	

	@Override
	public String toString() {
		return "CommonQuery [searchTerm=" + searchTerm + ", page=" + page
				+ ", pageSize=" + pageSize + ", source=" + source
				+ ", filters=" + filters + "]";
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
}
