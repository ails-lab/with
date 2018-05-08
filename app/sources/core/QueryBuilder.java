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


package sources.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import sources.core.Utils.LongPair;
import sources.core.Utils.Pair;
/**
 * is a helper class to assist when building a query that mainly has parameters. 
 * @author Enrique Matos Alfonso (gardero@gmail.com)
 *
 */
public class QueryBuilder {

	protected String baseUrl;
	protected String tail;
	protected Pair<String> query;
	protected List<Pair<String>> parameters;
	protected Object data;

	
	public QueryBuilder(String baseUrl) {
		this();
		this.baseUrl = baseUrl;
	}

	public QueryBuilder() {
		super();
		parameters = new ArrayList<Utils.Pair<String>>();
	}

	public String getHttp(boolean encode) {
		String res = getBaseUrl();
		Iterator<Pair<String>> it = parameters.iterator();
		boolean added = false;
		if (query != null && query.second != null) {
			res += ("?" + query.getHttp(encode));
			added = true;
		}
		for (; it.hasNext();) {
			String string = added ? "&" : "?";
			res += string + it.next().getHttp(encode);
			added = true;
		}
		if (Utils.hasInfo(tail)){
			res+=tail;
		}
		return res;
	}
	
	public String getHttp() {
		return getHttp(true);
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
	
	/**
	 * sets the query search term.
	 * @param q
	 * @return
	 */
	public QueryBuilder setQuery(Pair<String> q) {
		query = q;
		return this;
	}
	
	/**
	 * sets the query search term.
	 * @param q
	 * @return
	 */
	public QueryBuilder setQuery(String name, String value) {
		return setQuery(new Pair<String>(name, value));
	}
	
	/**
	 * a text is added to the search term of the query.
	 * @param q
	 * @return
	 */
	public QueryBuilder addToQuery(String q) {
		query.second+=q;
		return this;
	}

	/**
	 * mainly used for POST queries that need data to send to endpoint.
	 * @return
	 */
	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	/**
	 * the last part of the query. It is pasted exactly as it is. 
	 * @return
	 */
	public String getTail() {
		return tail;
	}

	public void setTail(String tail) {
		this.tail = tail;
	}
	
	
	

}
