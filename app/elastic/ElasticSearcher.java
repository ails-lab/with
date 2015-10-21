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



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import model.Rights.Access;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.DisMaxQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.NestedFilterBuilder;
import org.elasticsearch.index.query.NotFilterBuilder;
import org.elasticsearch.index.query.OrFilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder.Operator;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.facet.FacetBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import elastic.ElasticSearcher.SearchOptions;
import utils.Tuple;

public class ElasticSearcher {
	public static final int DEFAULT_RESPONSE_COUNT = 10;

	private final String name;
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
		public List<List<Tuple<ObjectId, Access>>> accessList = new ArrayList<List<Tuple<ObjectId, Access>>>();
		// used for method searchForCollections
		public boolean _idSearch = false;

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

		public void set_idSearch(boolean value) {
			this._idSearch = value;
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
		return this.execute(query, new SearchOptions(0, DEFAULT_RESPONSE_COUNT), false);
	}

	public SearchResponse execute(QueryBuilder query, SearchOptions options, boolean scroll) {
		SearchRequestBuilder search = this.getSearchRequestBuilder(query, options, scroll);
		return search.execute().actionGet();
	}

	public SearchResponse executeWithAggs(QueryBuilder query, SearchOptions options, boolean scroll) {
		SearchRequestBuilder search = this.getSearchRequestBuilder(query, options, scroll);
		TermsBuilder termAgg = AggregationBuilders.terms("types").field("type_all");
		TermsBuilder providerAgg = AggregationBuilders.terms("providers").field("provider_all");
		TermsBuilder dataProviderAgg = AggregationBuilders.terms("dataProviders").field("dataProvider_all");
		TermsBuilder creatorAgg = AggregationBuilders.terms("creators").field("creator_all");
		TermsBuilder rightsAgg = AggregationBuilders.terms("rights").field("rights_all");
		TermsBuilder countryAgg = AggregationBuilders.terms("countries").field("country_all");
		TermsBuilder yearAgg = AggregationBuilders.terms("years").field("year_all");
		TermsBuilder reuseAgg = AggregationBuilders.terms("reusability").field("reusability_all");
		search.addAggregation(termAgg)
			  .addAggregation(providerAgg)
			  .addAggregation(dataProviderAgg)
			  .addAggregation(creatorAgg)
			  .addAggregation(rightsAgg)
			  .addAggregation(countryAgg)
			  .addAggregation(yearAgg)
			  .addAggregation(reuseAgg);
		return search.execute().actionGet();
	}

	public SearchResponse executeWithFacets(QueryBuilder query, SearchOptions options) {
		SearchRequestBuilder search = this.getSearchRequestBuilder(query, options, false)
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

	public SearchResponse searchAccessibleCollections(SearchOptions options) {
		return searchAccessibleCollections(options, false);
	}

	public SearchResponse searchAccessibleCollectionsScanScroll(SearchOptions options) {
		return searchAccessibleCollections(options, true);
	}

	public SearchResponse searchAccessibleCollections(SearchOptions options, boolean scroll) {

		MatchAllQueryBuilder match_all = QueryBuilders.matchAllQuery();

		AndFilterBuilder and_filter = FilterBuilders.andFilter();
		for(List<Tuple<ObjectId, Access>> ands: options.accessList) {
			OrFilterBuilder or_filter = FilterBuilders.orFilter();
			for(Tuple<ObjectId, Access> t: ands) {
				BoolFilterBuilder bool = FilterBuilders.boolFilter();
				RangeFilterBuilder range_filter = FilterBuilders.rangeFilter("rights.access").gte(t.y.ordinal());
				bool.must(this.filter("rights.user", t.x.toString()));
				bool.must(range_filter);
				or_filter.add(bool);
			}
			and_filter.add(or_filter);
		}

		OrFilterBuilder outer_or = FilterBuilders.orFilter();
		NestedFilterBuilder nested_filter = FilterBuilders.nestedFilter("rights", and_filter);
		outer_or.add(nested_filter).add(this.filter("isPublic", "true"));
		FilteredQueryBuilder filtered = QueryBuilders.filteredQuery(match_all, outer_or);
		return this.execute(filtered, options, scroll);
	}

	public SearchResponse searchForCollections(String terms, SearchOptions options) {

		if(terms == null) terms = "";

		BoolQueryBuilder bool = QueryBuilders.boolQuery();
		QueryStringQueryBuilder str = QueryBuilders.queryStringQuery(terms);
		str.defaultOperator(Operator.OR);
		str.defaultField("_id");
		bool.must(str);

		return this.execute(bool, options, false);
	}

	public SearchResponse search(String terms, SearchOptions options){
		if(terms == null) terms = "";

		BoolQueryBuilder bool = QueryBuilders.boolQuery();
		/*
		List<String> list = new ArrayList<String>();
		Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(terms);
		while (m.find()) list.add(m.group(1)); // Add .replace("\"", "") to remove surrounding quotes.
		 */
		//implementation with query_string query
		QueryStringQueryBuilder str = QueryBuilders.queryStringQuery(terms);
		str.defaultOperator(Operator.OR);
		if(options._idSearch)
			str.defaultField("_id");

		bool.must(str);
		//return this.execute(bool, options, false);
		return this.executeWithAggs(bool, options, false);
	}

	public SearchResponse searchForSimilar(String terms, String provider, String exclude, SearchOptions elasticoptions) {

		if(terms == null) terms = "";

		DisMaxQueryBuilder dis_max_q = QueryBuilders.disMaxQuery();
		MatchQueryBuilder title_match = QueryBuilders.matchQuery("title", terms);
		MatchQueryBuilder desc_match = QueryBuilders.matchQuery("description", terms);
		MatchQueryBuilder provider_match = QueryBuilders.matchQuery("provider", provider);

		dis_max_q.add(title_match).add(desc_match).add(provider_match);
		dis_max_q.tieBreaker(0.3f);

		NotFilterBuilder not_filter = FilterBuilders.notFilter(this.filter("_id", exclude));
		FilteredQueryBuilder filtered = new FilteredQueryBuilder(dis_max_q, not_filter);

		return this.execute(filtered);
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

	private SearchRequestBuilder getSearchRequestBuilder(QueryBuilder query, SearchOptions options, boolean scroll) {

		SearchRequestBuilder search = this.getSearchRequestBuilder(type);
		if(!scroll) {
			search.setFrom(options.offset)
				  .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
				  .setSize(options.count);
		} else {
			search.setSearchType(SearchType.SCAN)
				  .setScroll(new TimeValue(60000))
				  .setSize(100);
		}
		System.out.println("got in here!");
		//search.addSort( new FieldSortBuilder("record.source").unmappedType("String").order(SortOrder.ASC).missing(""));
		FilterBuilder filterBuilder = null;

		if(options.filterType == FILTER_OR) filterBuilder = FilterBuilders.orFilter();
		else filterBuilder = FilterBuilders.andFilter();

		if(options.filters.size() > 0) {
			OrFilterBuilder accessibles = FilterBuilders.orFilter();
			OrFilterBuilder sources 	= FilterBuilders.orFilter();
			OrFilterBuilder others 		= FilterBuilders.orFilter();
			for(String key: options.filters.keySet()) {
				for(String value: options.filters.get(key)) {
					if(key.equals("isPublic") || key.equals("collections")) {
						accessibles.add(this.filter(key, value));
					} else if(key.equals("source")) {
						sources.add(this.filter(key, value));
					} else {
						others.add(this.filter(key, value));
					}
				}
			}
			if(options.filterType == FILTER_OR) {
				((OrFilterBuilder) filterBuilder).add(accessibles).add(sources).add(others);
			}
			else {
				((AndFilterBuilder) filterBuilder).add(accessibles).add(sources).add(others);
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



}
