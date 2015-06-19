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


package elastic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.CollectionRecord;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder.Type;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.OrFilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder.Operator;
import org.elasticsearch.index.search.MatchQuery;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.facet.FacetBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;

public class ElasticSearcher {
	public static final int DEFAULT_RESPONSE_COUNT = 10;

	private String name;
	private String type;

	private final Client client = null;
	public static final int DEFAULT_COUNT = 10;

	public static final int FILTER_AND = 1;
	public static final int FILTER_OR = 2;

	public static class SearchOptions {
		public int offset = 0;
		public int count = DEFAULT_COUNT;
		public HashMap<String, ArrayList<String>> filters = new HashMap<String, ArrayList<String>>();
		public int filterType = FILTER_AND;
		public String user;

		public SearchOptions() {
		}

		public SearchOptions(int offset, int count) {
			this.offset = offset;
			this.count = count;
		}

		public void setOffset(int offset) {
			this.offset = offset;
		}

		public void setCount(int count) {
			this.count = count;
		}
		
		public void setUser(String user) {
			this.user = user;
		}

		public void addFilter(String key, String value) {
			ArrayList<String> values = null;
			if(filters.containsKey(key)) {
				values = filters.get(key);
			} else {
				values = new ArrayList<String>();
				filters.put(key, values);
			}

			values.add(value);
		}

		public void setFilterType(String type) {
			if(type.equalsIgnoreCase("or")) filterType = FILTER_OR;
			else filterType = FILTER_AND;
		}
	}

	public void closeClient(){
		if(this.client != null){
			this.client.close();
		}
	}

	public ElasticSearcher(String type) {
		this.name = Elastic.index;
		this.type = type;
	}

	public SearchResponse execute(QueryBuilder query) {
		return this.execute(query, new SearchOptions(0, DEFAULT_RESPONSE_COUNT));
	}

	public SearchResponse execute(QueryBuilder query, SearchOptions options) {
		SearchRequestBuilder search = this.getSearchRequestBuilder(query, options);
		return search.execute().actionGet();
	}

	public SearchResponse executeWithFacets(QueryBuilder query, SearchOptions options) {
		SearchRequestBuilder search = this.getSearchRequestBuilder(query, options)
		.addFacet(this.facet("Designers", "Facets.Designers.text", "Facets.Designers"))
		.addFacet(this.facet("objectType", "Facets.objectType.uri", "Facets.objectType"))
		.addFacet(this.facet("colour", "Facets.colours.uri", "Facets.colours"))
		.addFacet(this.facet("techniques", "Facets.techniques.uri", "Facets.techniques"))
		.addFacet(this.facet("type", "Facets.type.value", "Facets.type"))
		.addFacet(this.facet("contemporaryDates", "Facets.datesContemporary.text", "Facets.datesContemporary"))
		.addFacet(this.facet("periods", "Facets.datesPeriod.text", "Facets.datesPeriods"))
		.addFacet(this.facet("periods", "Facets.datesPeriod.text", "Facets.datesPeriods"))
		.addFacet(this.facet("dataProviders", "Facets.dataProviders.text", "Facets.dataProviders"));
//		System.out.println("QUERY: " + search.toString());

		return search.execute().actionGet();
	}

	/**
	 * size defaults to DEFAULT_RESPONSE_COUNT (10).
	 * @param term search term
	 * @param from offset of results.
	 * @return
	 */
	public SearchResponse search(String term, int from, int count){ return search(term, new SearchOptions(from, count)); }
	public SearchResponse search(String term) { return search(term, new SearchOptions(0, DEFAULT_RESPONSE_COUNT)); }

	public SearchResponse search(String terms, SearchOptions options){
		if(terms == null) terms = "";
		
		BoolQueryBuilder bool = QueryBuilders.boolQuery();
		
		if(type.equals(Elastic.type_general)) {
			/*
			List<String> list = new ArrayList<String>();
			Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(terms);
			while (m.find()) list.add(m.group(1)); // Add .replace("\"", "") to remove surrounding quotes.
			 */
			//implementation with query_string query
			QueryStringQueryBuilder str = QueryBuilders.queryStringQuery(terms);
			str.defaultOperator(Operator.OR);
				
			bool.must(str);
		} else if(type.equals(Elastic.type_collection)) {
			
			MatchAllQueryBuilder match_all = QueryBuilders.matchAllQuery();
			
			BoolQueryBuilder user = QueryBuilders.boolQuery();
			MatchQueryBuilder user_match = QueryBuilders.matchQuery("rights.user", options.user);
			user.must(user_match);
			//MatchQueryBuilder access_match = QueryBuilders.matchQuery("rights.access", "");
			//user.must(access_match);
			NestedQueryBuilder nested = QueryBuilders.nestedQuery("rights", user);
			
			bool.must(match_all);
			bool.must(nested);
		} else {
		}
		
		return this.execute(bool, options);
		//return this.executeWithFacets(bool, options);
	}

	/*public SearchResponse related(Record record) {
		JsonNode object = record.getJsonObject(false);
		Set<String> creators = JSONUtils.getLabelsFromObject(object.get("creator"));
		Set<String> contributors = JSONUtils.getLabelsFromObject(object.get("contributor"));
		Set<String> types = JSONUtils.getLabelsFromObject(object.get("type"));
		Set<String> subjects = JSONUtils.getLabelsFromObject(object.get("subject"));
		Set<String> dataProviders = JSONUtils.getLabelsFromObject(object.get("dataProvider"));

		BoolQueryBuilder bool = QueryBuilders.boolQuery();
		this.populateBoolFromSet(bool, 1.8f, creators);
		this.populateBoolFromSet(bool, 1.6f, contributors);
		this.populateBoolFromSet(bool, 1.4f, types);
		this.populateBoolFromSet(bool, 1.2f, subjects);
		this.populateBoolFromSet(bool, 1.0f, dataProviders);

//		System.out.println(bool.toString());
		return this.execute(bool);
	}*/

	private void populateBoolFromSet(BoolQueryBuilder bool, float boost, Set<String> set) {
//		System.out.println(set);
		if(set.size() > 0) {
			String searchFor = "";
			for(String item: set) {
				searchFor += " " + item;
			}

			QueryBuilder query = QueryBuilders.matchQuery("_all", searchFor).boost(boost);
			bool.should(query);
		}
	}

	// private utility methods

	private Client getClient() {
		return Elastic.getTransportClient();
	}

	private SearchRequestBuilder getSearchRequestBuilder(String type) {
		return this.getClient()
		.prepareSearch(this.name)
		.setTypes(type)
		.setSearchType(SearchType.QUERY_THEN_FETCH);
	}

	private SearchRequestBuilder getSearchRequestBuilder(QueryBuilder query, SearchOptions options) {

		SearchRequestBuilder search = this.getSearchRequestBuilder(type)
		.setFrom(options.offset)
		.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
		.setSize(options.count);
		System.out.println("got in here!");
		search.addSort( new FieldSortBuilder("record.source").unmappedType("String").order(SortOrder.ASC).missing(""));
		FilterBuilder filterBuilder = null;

		if(options.filterType == FILTER_OR) filterBuilder = FilterBuilders.orFilter();
		else filterBuilder = FilterBuilders.andFilter();

		if(options.filters.size() > 0) {
			for(String key: options.filters.keySet()) {
				for(String value: options.filters.get(key)) {
					if(options.filterType == FILTER_OR) ((OrFilterBuilder) filterBuilder).add(this.filter(key, value));
					else ((AndFilterBuilder) filterBuilder).add(this.filter(key, value));
				}
			}

			QueryBuilder filtered = QueryBuilders.filteredQuery(query, filterBuilder);
			search.setQuery(filtered);
		} else {
			search.setQuery(query);
		}

		return search;
	}

	private FacetBuilder facet(String facetName, String fieldName, String nestedField) {
		FacetBuilder builder = FacetBuilders.termsFacet(facetName).field(fieldName).size(100);
		if(nestedField != null){
			builder.nested(nestedField);
		}

		return builder;
	}

	private FilterBuilder filter(String key, String value) {
		System.out.println("FILTER BUILDER: " + key + " - " + value);
		FilterBuilder filter = FilterBuilders.termFilter(key, value);
		return filter;
	}

	private FilterBuilder filter(String key, String fieldName, String nestedField, String value) {
		FilterBuilder filter = FilterBuilders.termFilter(fieldName, value);
		FilterBuilder nested = FilterBuilders.nestedFilter(nestedField, filter);
		return nested;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public static List<CollectionRecord> extractRecordsFromResponseHits(SearchResponse response){
    	ArrayList<CollectionRecord> result = new ArrayList<CollectionRecord>();
//    	play.Logger.debug("extract records");
    	for(SearchHit hit: response.getHits().getHits()) {
    		//result.add(JSONUtils.hitToRecord(hit));
    	}

    	return result;
    }

}
