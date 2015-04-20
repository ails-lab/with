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
import java.util.List;

import espace.core.Utils.Pair;

public class EuropeanaQuery {

	private String europeanaKey = "ANnuDzRpW";

	private List<String> search;
	private List<Pair<String>> parameters;

	public EuropeanaQuery() {
		super();
		search = new ArrayList<String>();
		parameters = new ArrayList<Utils.Pair<String>>();
	}

	public String getHttp() {
		List<Pair<String>> p = new ArrayList<Utils.Pair<String>>();
		for (Pair<String> pair : parameters) {
			p.add(pair);
		}
		if (search != null && search.size() > 0) {
			p.add(new Pair<String>("query", search.get(0)));
			for (int i = 1; i < search.size(); i++) {
				p.add(new Pair<String>("qf", search.get(i)));
			}
		}
		String res = getQueryBody();
		for (Pair<String> pair : p) {
			res += "&" + pair.getHttp();
		}
		return res;
	}

	private String getQueryBody() {
		return "http://europeana.eu/api/v2/search.json?wskey=" + europeanaKey;
	}

	public void addSearch(String q) {
		if (q != null)
			search.add(q);
	}

	public void addSearchParam(String name, String value) {
		parameters.add(new Pair<String>(name, value));
	}

	public void addSearch(Pair<String> attr) {
		if (attr != null) {
			search.add(attr.first + ":" + attr.second);
		}
	}

}
