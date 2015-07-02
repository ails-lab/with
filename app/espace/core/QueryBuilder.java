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
import java.util.Iterator;
import java.util.List;

import espace.core.Utils.Pair;
import espace.core.Utils.LongPair;
public class QueryBuilder {

	private String baseUrl;
	private Pair<String> query;
	private List<Pair<String>> parameters;

	
	public QueryBuilder(String baseUrl) {
		this();
		this.baseUrl = baseUrl;
	}

	public QueryBuilder() {
		super();
		parameters = new ArrayList<Utils.Pair<String>>();
	}

	public String getHttp() {
		String res = getBaseUrl();
		Iterator<Pair<String>> it = parameters.iterator();
		res+=("?"+query.getHttp());
		for (; it.hasNext();) {
			res += "&" + it.next().getHttp();
		}
		return res;
	}

	
	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public QueryBuilder addSearchParam(String name, String value) {
		parameters.add(new Pair<String>(name, value));
		return this;
	}
	
	public QueryBuilder addLongSearchParam(String name, String value) {
		parameters.add(new LongPair<String>(name, value));
		return this;
	}
	
	public QueryBuilder add(Pair<String> searchParam) {
		parameters.add(searchParam);
		return this;
	}
	
	public QueryBuilder addQuery(Pair<String> q) {
		query = q;
		return this;
	}
	
	public QueryBuilder addQuery(String name, String value) {
		return addQuery(new Pair<String>(name, value));
	}
	
	public QueryBuilder addToQuery(String q) {
		query.second+=q;
		return this;
	}
	

}
