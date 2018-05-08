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


package search;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import model.basicDataTypes.ProvenanceInfo;
import model.resources.RecordResource;
import model.resources.WithResource;
import play.libs.F.Promise;
import play.libs.Json;
import search.Response.SingleResponse;
import sources.FilterValuesMap;
import sources.core.ApacheHttpConnector;
import sources.core.CommonFilter;
import sources.core.CommonQuery;
import sources.core.HttpConnector;
import sources.core.JsonContextRecordFormatReader;
import sources.core.ParallelAPICall;
import sources.core.ParameterQueryModifier;
import sources.core.QueryBuilder;
import sources.core.QueryModifier;
import sources.core.SourceResponse;
import sources.core.Utils.Pair;
import sources.utils.JsonContextRecord;
import utils.ListUtils;

public abstract class ExternalSource extends EmptySource {
	
	protected FilterValuesMap valuesMap = new FilterValuesMap();
	private Sources source;
	
	@Override
	public Sources thisSource() {
		return source;
	}
	
	public ExternalSource(Sources source) {
		super();
		this.source= source;
		setValuesMap(FilterValuesMap.getMap(source));
	}
	@Override
	public Promise<Response.SingleResponse>  execute(Query query) {
		return ParallelAPICall.createPromise((q)->{
			// TODO implement all
			JsonNode response = getSourceResponse(q);
			Response.SingleResponse singleResponse = new Response.SingleResponse();
			try{
				parseResponse(response,singleResponse);
			} catch (Exception e){
				parseError(e, singleResponse);
			}
			return singleResponse;
		} , query);
	}
	/**
	 * reads the information from the Json response and translates it into a 
	 * SingleResponse.
	 * @param response
	 * @param resultResponse
	 */
	public abstract void parseResponse(JsonNode response, SingleResponse resultResponse)
	throws Exception;
	public void parseError(JsonNode response, SingleResponse resultResponse){
		resultResponse.errorMessage = response.get("error").asText();
	}
	public void parseError(Exception e, SingleResponse resultResponse){
		resultResponse.errorMessage = e.getMessage();
	}
	
	public HttpConnector getHttpConnector() {
		return ApacheHttpConnector.getApacheHttpConnector();
	}

	/**
	 * executes a query in the external source and gets its Json response.
	 * @param query
	 * @return
	 */
	public JsonNode getSourceResponse(Query query) {
		QueryBuilder b = getBuilder(query);
		try {
			return getHttpConnector().getURLContent(b.getHttp());
		} catch (Exception e) {
			ObjectNode node = Json.newObject();
			addError(node,e.getMessage());
			return node;
		}
	}
	/**
	 * reads the records from the response.
	 * @param node
	 * @param formatReader 
	 */
	public void readRecords(JsonNode node, SingleResponse response, 
			JsonContextRecordFormatReader formatReader){
		for (JsonNode jsonNode : node) {
			WithResource item = formatReader.readObjectFrom(jsonNode);
			readFields(item);
			response.addItem(item);
		} 
	}

	
	private void readFields(WithResource item) {
		// TODO Auto-generated method stub
		
	}
	
	private void readField(WithResource item, Fields field) {
		switch (field) {
		case resourceType:
			
			break;

		default:
//			String stringPath = field.fieldId();
//			String json = Json.toJson(f.overwriteObjectFrom(fullRecord,record)).toString();
//			
//			JsonContextRecord rec = new JsonContextRecord(jsonString)
			break;
		}
	}

	@Override
	public abstract Promise<Object> completeRecord(Object incompleteRecord);
	public abstract Promise<Object> getById(String id);
	
	/**
	 * gets a builder that helps building queries to be then translated to a query string.
	 * @param q
	 * @return
	 */
	public QueryBuilder getBuilder(Query q){
		return new QueryBuilder();
	}
	
	
	@Override
	public Set<String> supportedFieldIds() {
		Set<String> filters = getValuesMap().getFilters();
		filters.addAll(getValuesMap().getRestrictions());
		filters.add(FiltersFields.ANYWHERE.getFilterId());
		return filters;
	}

	/**
	 * gets the mapper that handles the translation of values from with to sources and vice versa. 
	 * @return
	 */
	public FilterValuesMap getValuesMap() {
		return valuesMap;
	}

	public void setValuesMap(FilterValuesMap valuesMap) {
		this.valuesMap = valuesMap;
	}

	protected QueryBuilder addfilters(Query q, QueryBuilder builder) {
		for (HashMap<String, List<String>> f : q.buildFactorizeFilters()) {
			for (Entry<String, List<String>> e : f.entrySet()) {
				if (!e.getKey().equals(Fields.anywhere.fieldId())){
					for (QueryModifier param : translateToQuery(e.getKey(), e.getValue())) {
						// builder.add(param);
						param.modify(builder);
					}
				}
			}
		}
		return builder;
	}
	
	
	protected List<Object> translateToSpecific(String filterID, String value) {
		return valuesMap.translateToSpecific(filterID, value);
	}

	protected List<Object> translateToCommon(String filterID, String value) {
		return valuesMap.translateToCommon(filterID, value);
	}
	
	protected void addRestriction(String filterId, String... values) {
		valuesMap.addRestriction(filterId, values);
	}
	
	protected List<QueryModifier> translateToQuery(String filterID, List<String> values) {
		return valuesMap.translateToQuery(filterID, values);
	}

	protected void addDefaultWriter(String filterId, Function<List<String>, Pair<String>> function) {
		valuesMap.addDefaultWriter(filterId, transformer((List<String> x)->{return Arrays.asList(function.apply(x));}));
	}
	
	protected void addDefaultComplexWriter(String filterId, Function<List<String>, List<Pair<String>>> function) {
		valuesMap.addDefaultWriter(filterId, transformer(function));
	}

	protected void addDefaultQueryModifier(String filterId, Function<List<String>, QueryModifier> function) {
		valuesMap.addDefaultWriter(filterId, function);
	}
	
	private Function<List<String>, QueryModifier> transformer(Function<List<String>, List<Pair<String>>> old){
		return (List<String> pars)->{return new ParameterQueryModifier(old.apply(pars));};
	}
	
	

}
