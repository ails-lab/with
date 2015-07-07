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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import espace.core.CommonFilters;
import espace.core.QueryModifier;
import espace.core.Utils.Pair;

public class FilterValuesMap {

	private HashMap<String, List<String>> specificvalues;
	// private HashMap<String, List<Pair<String>>> queryTexts;
	private HashMap<String, List<String>> commonvalues;
	private HashMap<String, Function<List<String>, QueryModifier>> writters;

	public FilterValuesMap() {
		super();
		specificvalues = new HashMap<String, List<String>>();
		commonvalues = new HashMap<String, List<String>>();
		// queryTexts = new HashMap<String, List<Pair<String>>>();
		writters = new HashMap<>();
	}

	private String getKey(String filterID, String value) {
		return filterID + "-" + value;
	}

	private <T> List<T> getOrset(HashMap<String, List<T>> map, String key, boolean addNew) {
		List<T> res;
		if (!map.containsKey(key)) {
			// check regular expr;
			res = new ArrayList<T>();
			for (String kk : map.keySet()) {
				if (key.matches(kk)) {
					res = map.get(kk);
					addNew = false; // for sure i am not adding a new value
				}
			}
			// not found
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

	public void addMap(String filterID, String commonValue, String... specificValue) {
		getOrset(specificvalues, getKey(filterID, commonValue)).addAll(Arrays.asList(specificValue));
		for (String string : specificValue) {
			getOrset(commonvalues, getKey(filterID, string)).add(commonValue);
		}
		// getOrset(queryTexts, getKey(filterID, commonValue)).add(queryText);
	}

	public List<String> translateToCommon(String filterID, String specificValue) {
		if (specificValue != null) {
			String matchexpr = getKey(filterID, specificValue);
			List<String> v = new ArrayList<>();

//			if (filterID.equals(CommonFilters.RIGHTS_ID)){
//				System.out.println(commonvalues.keySet());
//			}
			
			for (String kk : commonvalues.keySet()) {
//				if (filterID.equals(CommonFilters.RIGHTS_ID)){
//					System.out.println("------------------------------------------------");
//					System.out.println(kk+" match? "+specificValue);
//				}
				if (matchexpr.matches(kk)) {
					// String k = getKey(filterID, specificValue);
					List<String> orset = getOrset(commonvalues, kk, false);
//					if (filterID.equals(CommonFilters.RIGHTS_ID)){
//					System.out.println("MATCHED to "+orset);
//					}

					v.addAll(orset);
					return v;
				}
			}
			if (v.isEmpty()) {
				v.add(specificValue);
			}
			return v;
		}
		return null;
	}

	public List<String> translateToSpecific(String filterID, String... commonValue) {
		return translateToSpecific(filterID, Arrays.asList(commonValue));
	}

	public List<String> translateToSpecific(String filterID, List<String> commonValue) {
		if (commonValue != null) {
			ArrayList<String> res = new ArrayList<String>();
			for (String string : commonValue) {
				String k = getKey(filterID, string);
				List<String> v = getOrset(specificvalues, k, false);
				if (v.isEmpty()) {
					v.add(string);
				}
				res.addAll(v);
			}
			return res;
		}
		return null;
	}

	public List<QueryModifier> translateToQuery(String filterID, List<String> commonValue) {
		if (commonValue != null) {
			List<QueryModifier> res = new ArrayList<>();
			List<String> values = translateToSpecific(filterID, commonValue);
			Function<List<String>, QueryModifier> w = writters.get(filterID);
			if (w != null)
				res.add(w.apply(values));
			return res;
		}
		return null;
	}

	public void addDefaultWriter(String filterId, Function<List<String>, QueryModifier> function) {
		writters.put(filterId, function);
	}

	public Boolean containsFilter(String filterID) {
		return writters.containsKey(filterID);
	}

}
