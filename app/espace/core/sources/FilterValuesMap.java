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


package espace.core.sources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FilterValuesMap {

	private HashMap<String, List<String>> specificvalues;
	private HashMap<String, List<String>> queryTexts;
	private HashMap<String, List<String>> commonvalues;

	public FilterValuesMap() {
		super();
		specificvalues = new HashMap<String, List<String>>();
		commonvalues = new HashMap<String, List<String>>();
		queryTexts = new HashMap<String, List<String>>();
	}

	private String getKey(String filterID, String value) {
		return filterID + "/" + value;
	}

	private List<String> getOrset(HashMap<String, List<String>> map, String key, boolean addNew) {
		List<String> res;
		if (!map.containsKey(key)) {
			res = new ArrayList<String>();
			if (addNew)
				map.put(key, res);
		} else {
			res = map.get(key);
		}
		return res;
	}

	private List<String> getOrset(HashMap<String, List<String>> map, String key) {
		return getOrset(map, key, true);
	}

	public void addMap(String filterID, String commonValue, String specificValue, String queryText) {
		getOrset(specificvalues, getKey(filterID, commonValue)).add(specificValue);
		getOrset(commonvalues, getKey(filterID, specificValue)).add(commonValue);
		getOrset(queryTexts, getKey(filterID, commonValue)).add(queryText);
	}

	public List<String> translateToCommon(String filterID, String specificValue) {
		if (specificValue != null) {
			String k = getKey(filterID, specificValue);
			List<String> v = getOrset(commonvalues, k, false);
			return v;
		}
		return null;
	}

	public List<String> translateToSpecific(String filterID, String commonValue) {
		if (commonValue != null) {
			String k = getKey(filterID, commonValue);
			List<String> v = getOrset(specificvalues, k, false);
			return v;
		}
		return null;
	}

	public List<String> translateToQuery(String filterID, String commonValue) {
		if (commonValue != null) {
			String k = getKey(filterID, commonValue);
			List<String> v = getOrset(queryTexts, k, false);
			return v;
		}
		return null;
	}

}
