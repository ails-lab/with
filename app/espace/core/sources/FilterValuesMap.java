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
import java.util.function.Function;

import espace.core.Utils.Pair;

public class FilterValuesMap {

	private HashMap<String, List<String>> specificvalues;
	private HashMap<String, List<Pair<String>>> queryTexts;
	private HashMap<String, List<String>> commonvalues;
	private HashMap<String, Function<String, Pair<String>>> writters;

	public FilterValuesMap() {
		super();
		specificvalues = new HashMap<String, List<String>>();
		commonvalues = new HashMap<String, List<String>>();
		queryTexts = new HashMap<String, List<Pair<String>>>();
		writters = new HashMap<>();
	}

	private String getKey(String filterID, String value) {
		return filterID + "/" + value;
	}

	private <T> List<T> getOrset(HashMap<String, List<T>> map, String key, boolean addNew) {
		List<T> res;
		if (!map.containsKey(key)) {
			res = new ArrayList<T>();
			if (addNew)
				map.put(key, res);
		} else {
			res = map.get(key);
		}
		return res;
	}

	private <T> List<T> getOrset(HashMap<String, List<T>> map, String key) {
		return getOrset(map, key, true);
	}

	public void addMap(String filterID, String commonValue, String specificValue, Pair<String> queryText) {
		getOrset(specificvalues, getKey(filterID, commonValue)).add(specificValue);
		getOrset(commonvalues, getKey(filterID, specificValue)).add(commonValue);
		getOrset(queryTexts, getKey(filterID, commonValue)).add(queryText);
	}

	public List<String> translateToCommon(String filterID, String specificValue) {
		if (specificValue != null) {
			String k = getKey(filterID, specificValue);
			List<String> v = getOrset(commonvalues, k, false);
			if (v.isEmpty()) {
				v.add(specificValue);
			}
			return v;
		}
		return null;
	}

	public List<String> translateToSpecific(String filterID, String commonValue) {
		if (commonValue != null) {
			String k = getKey(filterID, commonValue);
			List<String> v = getOrset(specificvalues, k, false);
			if (v.isEmpty()) {
				v.add(commonValue);
			}
			return v;
		}
		return null;
	}

	public List<Pair<String>> translateToQuery(String filterID, String commonValue) {
		if (commonValue != null) {
			String k = getKey(filterID, commonValue);
			List<Pair<String>> v = getOrset(queryTexts, k, false);
			if (v.isEmpty()) {
				Function<String, Pair<String>> w = writters.get(filterID);
				if (w != null)
					v.add(w.apply(commonValue));
			}
			return v;
		}
		return null;
	}

	public void addDefaultWriter(String filterId, Function<String, Pair<String>> function) {
		writters.put(filterId, function);
	}

}
