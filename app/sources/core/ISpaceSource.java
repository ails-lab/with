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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import model.resources.RecordResource;
import search.FiltersFields;
import search.Sources;
import sources.FilterValuesMap;
import sources.core.Utils.Pair;
import utils.ListUtils;

public abstract class ISpaceSource {

	private static Config conf;

	protected List<FiltersFields> filtersSupportedBySource = new ArrayList<FiltersFields>();
	protected HashMap<String, FiltersFields> sourceToFiltersMappings = new HashMap<String, FiltersFields>();
	protected HashMap<FiltersFields, String> filtersToSourceMappings = new HashMap<FiltersFields, String>();
	protected FilterValuesMap vmap = new FilterValuesMap();
	private Sources sourceLABEL = null;
	protected String apiKey="";
	protected JsonContextRecordFormatReader formatreader;
	
	public ISpaceSource(Sources source) {
		this.sourceLABEL = source;
		vmap = FilterValuesMap.getMap(source);
		setApiKey();
	}
	
	public HttpConnector getHttpConnector() {
		return ApacheHttpConnector.getApacheHttpConnector();
	}


	public Sources getSourceName() {
		return sourceLABEL;
	}

	public void setApiKey() {
		this.apiKey = getApiKeyFromConfig(sourceLABEL);
	}

	public static String getUserIdFromConfig(Sources source) {
		return getFromConfig("userid", source);
	}

	public static String getApiKeyFromConfig(Sources source) {
		return getFromConfig("apikey", source);
	}
	
	public static String getFromConfig(String key, Sources source) {
		if (conf == null) {
			conf = ConfigFactory.load();
		}
		if (conf.hasPath(key + "." + source.name())) {
			return conf.getString(key + "." + source.name());
		} else {
			return "UNKNOWN";
		}
	}

	public String getHttpQuery(CommonQuery q) {
		return "";
	};
	
	public QueryBuilder getBuilder(CommonQuery q){
		return new QueryBuilder();
	}

	public abstract SourceResponse getResults(CommonQuery q);

	public String autocompleteQuery(String term, int limit) {
		return "";
	}

	public AutocompleteResponse autocompleteResponse(String response) {
		return new AutocompleteResponse();
	}


	public List<RecordJSONMetadata> getRecordFromSource(
			String recordId, RecordResource fullRecord) {
		return new ArrayList<RecordJSONMetadata>();
	}
	


	protected void countValue(CommonFilterLogic type, String t) {
		countValue(type, t, true, 1);
	}
	
	protected void countValue(CommonFilterLogic type, Collection<String> t) {
		for (String string : t) {
			countValue(type, string, true, 1);
		}
	}

	protected void countValue(CommonFilterLogic type, String t, int count) {
		countValue(type, t, true, count);
	}

	protected void countValue(CommonFilterLogic type, String t, boolean toglobal, int count) {
		if (toglobal){
			String name = type.data.filterID;
			List<Object> translateToCommon = vmap.translateToCommon(name, t);
			Function<Object, String> function = (Object x)-> x.toString();
			List<String> transform = ListUtils.transform(translateToCommon, function);
			type.addValue(transform, count);
		} else
			type.addValue(t, count);

	}
	
	public boolean checkFilters(CommonQuery q) {
		if (q.filters == null || q.filters.size() == 0)
			return true;
		else{
			Function<CommonFilter, Boolean> condition = (CommonFilter f)->vmap.checkRestriction(f.filterID,f.values);
			return ListUtils.allof(q.filters,condition );
		}
	}
	
	private Function<List<String>, QueryModifier> transformer(Function<List<String>, List<Pair<String>>> old){
		return (List<String> pars)->{return new ParameterQueryModifier(old.apply(pars));};
	}
	
	protected void addDefaultWriter(String filterId, Function<List<String>, Pair<String>> function) {
		vmap.addDefaultWriter(filterId, transformer((List<String> x)->{return Arrays.asList(function.apply(x));}));
	}
	
	protected void addDefaultComplexWriter(String filterId, Function<List<String>, List<Pair<String>>> function) {
		vmap.addDefaultWriter(filterId, transformer(function));
	}

	protected void addDefaultQueryModifier(String filterId, Function<List<String>, QueryModifier> function) {
		vmap.addDefaultWriter(filterId, function);
	}
	
	protected void addMapping(String filterID, Object commonValue, String... specificValue) {
		vmap.addMap(filterID, commonValue, specificValue);
	}
	
	protected void addMapping(String filterID, String commonValue, String specificValue) {
		vmap.addMap(filterID, commonValue, specificValue);
	}
	
	protected List<Object> translateToSpecific(String filterID, String value) {
		return vmap.translateToSpecific(filterID, value);
	}

	protected List<Object> translateToCommon(String filterID, String value) {
		return vmap.translateToCommon(filterID, value);
	}
	
//	protected List<String> translateToQuery(String filterID, String value) {
//		return vmap.translateToQuery(filterID, value);
//	}
	
	protected void addRestriction(String filterId, String... values) {
		vmap.addRestriction(filterId, values);
	}
	
	protected List<QueryModifier> translateToQuery(String filterID, List<String> values) {
		return vmap.translateToQuery(filterID, values);
	}

//	protected String addfilters(CommonQuery q, String qstr) {
//		if (q.filters != null) {
//			for (CommonFilter filter : q.filters) {
//				for (String subq : translateToQuery(filter.filterID, filter.value)) {
//					qstr += subq;
//				}
//			}
//		}
//		return qstr;
//	}
	
	protected QueryBuilder addfilters(CommonQuery q, QueryBuilder builder) {
		if (q.filters != null) {
			for (CommonFilter filter : q.filters) {
				for (QueryModifier param : translateToQuery(filter.filterID, filter.values)) {
//					builder.add(param);
					param.modify(builder);
				}
			}
		}
		return builder;
	}

	public List<CommonQuery> splitFilters(CommonQuery q) {
		return Arrays.asList(q);
	}

	public FilterValuesMap getVmap() {
		return vmap;
	}

	public void setVmap(FilterValuesMap vmap) {
		this.vmap = vmap;
	}

	/**
	 * Override if you have a URL for an API console.
	 * @return
	 */
    public  String apiConsole() {
    	return null;
	}
	
	/*public WithResource fillObjectFrom(JsonNode rec) {
		return new WithResource();
	}*/
	
	
	
}
