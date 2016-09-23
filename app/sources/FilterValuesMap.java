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


package sources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import model.EmbeddedMediaObject.WithMediaRights;
import model.EmbeddedMediaObject.WithMediaType;
import search.FiltersFields;
import search.Sources;
import sources.core.CommonFilter;
import sources.core.CommonFilterLogic;
import sources.core.ESpaceSources;
import sources.core.MapsConfig;
import sources.core.QueryModifier;
import utils.ListUtils;

public class FilterValuesMap {


	private static HashMap<Sources, FilterValuesMap> map;
	private HashMap<String, List<Object>> specificvalues;
	// private HashMap<String, List<Pair<String>>> queryTexts;
	private HashMap<String, List<Object>> commonvalues;
	private HashMap<String, Function<List<String>, QueryModifier>> writters;
	
	private HashMap<String, List<String>> restrictions;

	public FilterValuesMap() {
		map = new HashMap<>();
		restrictions = new HashMap<>();
		specificvalues = new HashMap<>();
		commonvalues = new HashMap<>();
		// queryTexts = new HashMap<String, List<Pair<String>>>();
		writters = new HashMap<>();
	}

	private String getKey(String filterID, Object value) {
		return filterID + "-" + value.toString();
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

	public void addMap(String filterID, Object commonValue, String... specificValue) {
		getOrset(specificvalues, getKey(filterID, commonValue)).addAll(Arrays.asList(specificValue));
		for (String string : specificValue) {
			getOrset(commonvalues, getKey(filterID, string)).add(commonValue);
		}
		// getOrset(queryTexts, getKey(filterID, commonValue)).add(queryText);
	}
	
	public void addMap(String filterID, Object commonValue, List<String> specificValue) {
		getOrset(specificvalues, getKey(filterID, commonValue)).addAll(specificValue);
		for (String string : specificValue) {
			getOrset(commonvalues, getKey(filterID, string)).add(commonValue);
		}
		// getOrset(queryTexts, getKey(filterID, commonValue)).add(queryText);
	}

	public List<Object> translateToCommon(String filterID, String specificValue) {
		if (specificValue != null) {
			String matchexpr = getKey(filterID, specificValue);
			List<Object> v = new ArrayList<>();			
			for (String kk : commonvalues.keySet()) {
				if (matchexpr.matches(kk) || matchexpr.equals(kk)) {
					// String k = getKey(filterID, specificValue);
					List<Object> orset = getOrset(commonvalues, kk, false);
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

	public List<Object> translateToSpecific(String filterID, String... commonValue) {
		return translateToSpecific(filterID, Arrays.asList(commonValue));
	}

	public List<Object> translateToSpecific(String filterID, List<String> commonValue) {
		if (commonValue != null) {
			ArrayList<Object> res = new ArrayList<>();
			for (String string : commonValue) {
				String k = getKey(filterID, string);
				List<Object> v = getOrset(specificvalues, k, false);
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
			List<Object> values = translateToSpecific(filterID, commonValue);
			Function<List<String>, QueryModifier> w = writters.get(filterID);
			if (w != null)
				res.add(w.apply(ListUtils.transform(values, (Object x)-> x.toString())));
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
	
	public void addRestriction(String filterId, String... values){
		restrictions.put(filterId, Arrays.asList(values));
	}
	
	public boolean checkRestriction(String filterId, List<String> values){
		List<String> a = restrictions.get(filterId);
		if (a!=null)
		return ListUtils.containsAny(a, values);
		else
			return true;
	}
	
	
	
	
	private void addMapping(String id, Object obj, String... string) {
		addMap(id, obj, string);
	}
	
	public static FilterValuesMap getMap(Sources source){
		FilterValuesMap ms = map.get(source);
		if (ms==null){
			ms = MapsConfig.buildFilterValuesMap(source);
			map.put(source, ms);
			switch (source) {
			case DPLA:
				ms.fillDPLA();
				break;
			case InternetArchive:
			case BritishLibrary:
				ms.fillFlickr();
				break;
			case NLA:
				ms.fillNLA();
				break;
			case DigitalNZ:
				ms.fillDNZ();
				break;
			case DDB:
				ms.fillDDB();
				break;
			case Rijksmuseum:
				ms.fillRijks();
				break;
			case Historypin:
				ms.fillHistorypin();
				break;

			default:
				break;
			}
		}
		return ms;
	}
	


	private void fillDBPedia() {
//		addMapping(CommonFilters.TYPE.getId(), WithMediaType.IMAGE, "Image", "Photograph",
//				"Poster, chart, other");
//		addMapping(CommonFilters.TYPE.getId(), WithMediaType.VIDEO, "Video");
//		addMapping(CommonFilters.TYPE.getId(), WithMediaType.AUDIO, "Sound", "Sheet music");
//		addMapping(CommonFilters.TYPE.getId(), WithMediaType.TEXT, "Books", "Article");
	}
	
	private void fillFlickr() {
		addMapping(FiltersFields.RIGHTS.getFilterId(), WithMediaRights.RR, FlickrSpaceSource.getLicence("0"));
		addMapping(FiltersFields.RIGHTS.getFilterId(), WithMediaRights.Creative_Not_Commercial, FlickrSpaceSource.getLicence("3"),
				BritishLibrarySpaceSource.getLicence("2"), FlickrSpaceSource.getLicence("1"));
		addMapping(FiltersFields.RIGHTS.getFilterId(), WithMediaRights.Modify, FlickrSpaceSource.getLicence("6"));
		addMapping(FiltersFields.RIGHTS.getFilterId(), WithMediaRights.Creative, FlickrSpaceSource.getLicence("1"), FlickrSpaceSource.getLicence("2"),
				BritishLibrarySpaceSource.getLicence("3"), FlickrSpaceSource.getLicence("4"), FlickrSpaceSource.getLicence("5"), FlickrSpaceSource.getLicence("6"));
		addMapping(FiltersFields.RIGHTS.getFilterId(), WithMediaRights.UNKNOWN, FlickrSpaceSource.getLicence("7"));
		addMapping(FiltersFields.RIGHTS.getFilterId(), WithMediaRights.Public, FlickrSpaceSource.getLicence("9"), FlickrSpaceSource.getLicence("10"));

	}
	private void fillNLA() {
		addMapping(FiltersFields.TYPE.getFilterId(), WithMediaType.IMAGE, "Image", "Photograph",
				"Poster, chart, other");
		addMapping(FiltersFields.TYPE.getFilterId(), WithMediaType.VIDEO, "Video");
		addMapping(FiltersFields.TYPE.getFilterId(), WithMediaType.AUDIO, "Sound", "Sheet music");
		addMapping(FiltersFields.TYPE.getFilterId(), WithMediaType.TEXT, "Books", "Article");
	}

	private void fillDDB() {
		
		addMapping(FiltersFields.RIGHTS.getFilterId(), WithMediaRights.Creative, ".*creative.*");
		addMapping(FiltersFields.RIGHTS.getFilterId(), WithMediaRights.Creative_Not_Commercial, ".*creative(?!.*nc).*");
		addMapping(FiltersFields.RIGHTS.getFilterId(), WithMediaRights.Modify, ".*creative(?!.*nd).*");
		addMapping(FiltersFields.RIGHTS.getFilterId(), WithMediaRights.RR, ".*rr-.*",".*rv-fz.*");
		addMapping(FiltersFields.RIGHTS.getFilterId(), WithMediaRights.UNKNOWN, ".*unknown.*");
		
	}
	
	
	private void fillDPLA() {
	}
	
	private void fillHistorypin() {
	}
	
	private void fillDNZ() {

	}

	

	private void fillRijks() {
		addRestriction(FiltersFields.TYPE.getFilterId(),WithMediaType.IMAGE.getName(), WithMediaType.TEXT.getName());
		addRestriction(FiltersFields.RIGHTS.getFilterId(),WithMediaRights.Creative.name());
	}

	public Set<String> getFilters() {
		return new TreeSet<>(writters.keySet());
	}

	public Set<String> getRestrictions() {
		return new TreeSet<>(restrictions.keySet());
	}
	
	/**
	 * builds the facets based on the restrictions of the source. Assumes all the items returned 
	 * in the query fulfill the restrictions.
	 * @param itemsCount the number of items returned in the query.
	 * @return
	 */
	public List<CommonFilterLogic> getRestrictionsAsFilters(int itemsCount) {
		ArrayList<CommonFilterLogic> res = new ArrayList<>();
		for (Entry<String, List<String>> restr : restrictions.entrySet()) {
			CommonFilterLogic f = new CommonFilterLogic(restr.getKey());
			f.addValue(restr.getValue(), itemsCount);
			res.add(f);
		}
		return res;
	}

}
