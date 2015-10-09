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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.json.JSONObject;

import utils.ListUtils;

import com.fasterxml.jackson.databind.JsonNode;

import espace.core.RecordJSONMetadata.Format;
import espace.core.Utils.Pair;
import espace.core.sources.FilterValuesMap;
import espace.core.sources.TypeValues;

public abstract class ISpaceSource {

	public ISpaceSource() {
		super();
		vmap = new FilterValuesMap();
	}

	public abstract String getSourceName();

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


	public ArrayList<RecordJSONMetadata> getRecordFromSource(
			String recordId) {
		return new ArrayList<RecordJSONMetadata>();
	}
	
	private FilterValuesMap vmap;

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
		if (toglobal)
			type.addValue(vmap.translateToCommon(type.data.filterID, t), count);
		else
			type.addValue(t, count);

	}
	
	public boolean checkFilters(CommonQuery q) {
		if (q.filters == null || q.filters.size() == 0)
			return true;
		else{
			return ListUtils.allof(q.filters,(CommonFilter f)->{return vmap.containsFilter(f.filterID);} );
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


	protected void addMapping(String filterID, String commonValue, String... specificValue) {
		vmap.addMap(filterID, commonValue, specificValue);
	}
	
	protected void addMapping(String filterID, String commonValue, String specificValue) {
		vmap.addMap(filterID, commonValue, specificValue);
	}

	protected List<String> translateToSpecific(String filterID, String value) {
		return vmap.translateToSpecific(filterID, value);
	}

	protected List<String> translateToCommon(String filterID, String value) {
		return vmap.translateToCommon(filterID, value);
	}

//	protected List<String> translateToQuery(String filterID, String value) {
//		return vmap.translateToQuery(filterID, value);
//	}
	
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

}
