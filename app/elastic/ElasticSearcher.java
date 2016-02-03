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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import model.basicDataTypes.WithAccess.Access;
import model.resources.RecordResource;
import model.resources.RecordResource.RecordDescriptiveData;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.Fuzziness;
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
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.NestedFilterBuilder;
import org.elasticsearch.index.query.NotFilterBuilder;
import org.elasticsearch.index.query.OrFilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder.Operator;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.index.query.functionscore.fieldvaluefactor.FieldValueFactorFunctionBuilder;
import org.elasticsearch.index.search.MultiMatchQuery;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.facet.FacetBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilder.SuggestionBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.elasticsearch.search.suggest.term.TermSuggestionBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonObject;

import elastic.ElasticSearcher.SearchOptions;
import utils.Tuple;

public class ElasticSearcher {
	public static final int DEFAULT_RESPONSE_COUNT = 10;

	private final String name;
	private List<String> types = new ArrayList<String>();
	private final Map<String, Float> fedSearchFieldsWithBoosts;
	private List<String> fieldsForSimilarity;
	private final List<String> aggregatedFields;

	private final Client client = null;
	public static final int DEFAULT_COUNT = 10;

	public static final int FILTER_AND = 1;
	public static final int FILTER_OR = 2;

	public static class SearchOptions {
		public boolean scroll;
		public int offset = 0;
		public int count = DEFAULT_COUNT;
		public HashMap<String, ArrayList<String>> filters = new HashMap<String, ArrayList<String>>();
		public int filterType = FILTER_AND;
		public List<List<Tuple<ObjectId, Access>>> accessList = new ArrayList<List<Tuple<ObjectId, Access>>>();
		// used for method searchForCollections

		public SearchOptions() {
		}

		public SearchOptions(int offset, int count) {
			this.offset = offset;
			this.count = count;
		}

		public boolean isScroll() {
			return scroll;
		}

		public void setScroll(boolean scroll) {
			this.scroll = scroll;
		}


		public void setOffset(int offset) {
			this.offset = offset;
		}

		public void setCount(int count) {
			this.count = count;
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

	public ElasticSearcher() {
		this.name = Elastic.index;

		this.fedSearchFieldsWithBoosts = new HashMap<String, Float>();
		fedSearchFieldsWithBoosts.put("label", 1.8f);
		fedSearchFieldsWithBoosts.put("description", 1.5f);
		fedSearchFieldsWithBoosts.put("keywords", 1.3f);

		this.aggregatedFields = new ArrayList<String>();
		aggregatedFields.add("resourceType.all");
		aggregatedFields.add("provider.all");
		aggregatedFields.add("dataProvider.all");
		aggregatedFields.add("media.type.all");
		aggregatedFields.add("dccreator.all");
		aggregatedFields.add("dccontributor.all");
		aggregatedFields.add("dctermsspatial.all");
		aggregatedFields.add("contentusage.all");
		aggregatedFields.add("dates");
		aggregatedFields.add("media.type.all");
		aggregatedFields.add("media.withRights.all");

	}


	/* Query Execution */

	public SearchResponse execute(QueryBuilder query) {
		return this.execute(query, new SearchOptions(0, DEFAULT_RESPONSE_COUNT));
	}

	public SearchResponse execute(QueryBuilder query, SearchOptions options) {
		SearchRequestBuilder search = this.getSearchRequestBuilder(query, options);
		return search.execute().actionGet();
	}

	public SearchResponse executeWithAggs(QueryBuilder query, SearchOptions options) {
		SearchRequestBuilder search = this.getSearchRequestBuilder(query, options);
		for(String aggName: aggregatedFields) {
			TermsBuilder agg = AggregationBuilders.terms(aggName+"+aggregation").field(aggName);
			search.addAggregation(agg);
		}

		System.out.println(search.toString());
		return search.execute().actionGet();
	}

	public SuggestResponse executeSuggestion(SuggestionBuilder suggestion, SearchOptions options) {
		SuggestRequestBuilder sugg = this.getSuggestRequestBuilder(suggestion, options);
		return sugg.execute().actionGet();
	}

	/* Query Constractors */

	/**
	 * size defaults to DEFAULT_RESPONSE_COUNT (10).
	 * @param term search term
	 * @param from offset of results.
	 * @return
	 */
	public SearchResponse search(String term, int from, int count) {
		return searchResourceWithWeights(term, new SearchOptions(from, count));
	}

	public SearchResponse search(String term, SearchOptions options) {
		return searchResourceWithWeights(term, options);
	}

	public SearchResponse search(String term) {
		return searchResourceWithWeights(term, new SearchOptions(0, DEFAULT_RESPONSE_COUNT));
	}



	public SearchResponse searchResourceWithWeights(String term, SearchOptions options) {

		QueryStringQueryBuilder qstr = QueryBuilders.queryStringQuery(term);
		for(Entry<String, Float> e: fedSearchFieldsWithBoosts.entrySet()) {
			qstr.field(e.getKey()+".all", e.getValue());
		}
		qstr.useDisMax(true);
		qstr.tieBreaker(0);
		qstr.defaultOperator(Operator.OR);
		qstr.defaultField("_all");
		qstr.analyzer("standard");
		qstr.analyzeWildcard(false);
		//
		qstr.fuzzyMaxExpansions(50);
		qstr.fuzziness(Fuzziness.AUTO);
		qstr.fuzzyPrefixLength(0);
		//
		qstr.phraseSlop(0);
		qstr.autoGeneratePhraseQueries(false);
		qstr.maxDeterminizedStates(10000);
		//qstr.minimumShouldMatch(minimumShouldMatch);
		qstr.lenient(true);


		FunctionScoreQueryBuilder func_score =  QueryBuilders.functionScoreQuery(qstr);

		return this.executeWithAggs(func_score, options);
	}


	/*
	 * List all available collections of a User
	 */
	public SearchResponse searchAccessibleCollections(SearchOptions options) {

		MatchAllQueryBuilder match_all = QueryBuilders.matchAllQuery();
		return this.execute(match_all, options);
	}


	/*
	 * Search for related records
	 */

	public SearchResponse relatedWithDisMax(String terms, String provider, String exclude, SearchOptions elasticoptions) {

		if(terms == null) terms = "";

		DisMaxQueryBuilder dis_max_q = QueryBuilders.disMaxQuery();
		MatchQueryBuilder title_match = QueryBuilders.matchQuery("label_all", terms);
		MatchQueryBuilder desc_match = QueryBuilders.matchQuery("description_all", terms);
		MatchQueryBuilder provider_match = QueryBuilders.matchQuery("provider", provider);

		dis_max_q.add(title_match).add(desc_match).add(provider_match);
		dis_max_q.tieBreaker(0.3f);

		if(exclude != null) {
			NotFilterBuilder not_filter = FilterBuilders.notFilter(this.filter("_id", exclude));
			FilteredQueryBuilder filtered = new FilteredQueryBuilder(dis_max_q, not_filter);
			return this.execute(filtered);
		}
		else
			return this.execute(dis_max_q);


	}

	public SearchResponse relatedWithMLT(String text, List<String> ids, List<String> fields, SearchOptions options) {

		if(text == null) text = "";

		MoreLikeThisQueryBuilder mlt;
		if(fields != null)
			mlt = QueryBuilders.moreLikeThisQuery(fields.toArray(new String[fields.size()]));
		else
			mlt = QueryBuilders.moreLikeThisQuery();

		mlt.likeText(text);
		if(ids != null) mlt.ids(ids.toArray(new String[ids.size()]));
		mlt.maxQueryTerms(20);
		mlt.minTermFreq(1);

		return this.execute(mlt, options);
	}

	public SearchResponse relatedWithShouldClauses(List<JsonNode> records) {

		Set<String> creators = new HashSet<String>();
		Set<String> contributors = new HashSet<String>();
		Set<String> types = new HashSet<String>();
		Set<String> subjects = new HashSet<String>();
		Set<String> dataProviders = new HashSet<String>();

		for(JsonNode rr: records) {
			creators.add(rr.get("descriptiveData.dccreator").asText());
			contributors.add(rr.get("").asText());
			types.add(rr.get("").asText());
			subjects.add(rr.get("").asText());
			dataProviders.add(rr.get("").asText());
		}

		BoolQueryBuilder bool = QueryBuilders.boolQuery();
		this.populateBoolFromSet(bool, 1.8f, creators);
		this.populateBoolFromSet(bool, 1.6f, contributors);
		this.populateBoolFromSet(bool, 1.4f, types);
		this.populateBoolFromSet(bool, 1.2f, subjects);
		this.populateBoolFromSet(bool, 1.0f, dataProviders);

//		System.out.println(bool.toString());
		return this.execute(bool);
	}


	public SuggestResponse searchSuggestions(String term, String field, SearchOptions options) {

		TermSuggestionBuilder sugg = SuggestBuilders.termSuggestion(term);
		sugg.text(term);
		sugg.field(field);

		return this.executeSuggestion(sugg, options);
	}

	/* Private utility methods */

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

	private void addQueryPermissions(FilterBuilder f, SearchOptions options) {

		AndFilterBuilder and_filter = FilterBuilders.andFilter();
		for(List<Tuple<ObjectId, Access>> ands: options.accessList) {
			OrFilterBuilder or_filter = FilterBuilders.orFilter();
			for(Tuple<ObjectId, Access> t: ands) {
				BoolFilterBuilder bool = FilterBuilders.boolFilter();
				RangeFilterBuilder range_filter = FilterBuilders.rangeFilter("access.acl.level").gte(t.y.ordinal());
				bool.must(this.filter("access.acl.user", t.x.toString()));
				bool.must(range_filter);
				or_filter.add(bool);
			}
			and_filter.add(or_filter);
		}

		OrFilterBuilder outer_or = FilterBuilders.orFilter();
		NestedFilterBuilder nested_filter = FilterBuilders.nestedFilter("access", and_filter);
		outer_or.add(nested_filter).add(this.filter("isPublic", "true"));
		if(options.filterType == FILTER_OR) {
			((OrFilterBuilder) f).add(outer_or);
		}
		else {
			((AndFilterBuilder) f).add(outer_or);
		}
	}

	private SearchRequestBuilder getSearchRequestBuilder(QueryBuilder query, SearchOptions options) {

		SearchRequestBuilder search = Elastic.getTransportClient()
									.prepareSearch(name)
									.setTypes(types.toArray(new String[types.size()]));

		if(!options.isScroll()) {
			search.setFrom(options.offset)
				  .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
				  .setSize(options.count);
		} else {
			search.setSearchType(SearchType.SCAN)
				  .setScroll(new TimeValue(60000))
				  .setSize(100);
		}
		//search.addSort( new FieldSortBuilder("record.source").unmappedType("String").order(SortOrder.ASC).missing(""));
		FilterBuilder filterBuilder = null;

		if(options.filterType == FILTER_OR) filterBuilder = FilterBuilders.orFilter();
		else filterBuilder = FilterBuilders.andFilter();

		if(options.filters.size() > 0) {
			for(String key: options.filters.keySet()) {
				OrFilterBuilder sameFieldFilter	= FilterBuilders.orFilter();
				for(String value: options.filters.get(key)) {
					sameFieldFilter.add(this.filter(key, value));
				}
				if(options.filterType == FILTER_OR) {
					((OrFilterBuilder) filterBuilder).add(sameFieldFilter).cache(true);
				}
				else {
					((AndFilterBuilder) filterBuilder).add(sameFieldFilter).cache(true);
				}
			}
		}

		addQueryPermissions(filterBuilder, options);
		QueryBuilder filtered = QueryBuilders.filteredQuery(query, filterBuilder);
		search.setQuery(filtered);

		return search;
	}

	private SuggestRequestBuilder getSuggestRequestBuilder(SuggestionBuilder suggestion, SearchOptions options) {

		SuggestRequestBuilder sugg = Elastic.getTransportClient()
									.prepareSuggest(name)
									.addSuggestion(suggestion);

		System.out.println(suggestion.toString());
		return sugg;
	}

	private FilterBuilder filter(String key, String value) {
		FilterBuilder filter = FilterBuilders.termFilter(key, value);
		return filter;
	}

	private FilterBuilder filter(String key, String fieldName, String nestedField, String value) {
		FilterBuilder filter = FilterBuilders.termFilter(fieldName, value);
		FilterBuilder nested = FilterBuilders.nestedFilter(nestedField, filter);
		return nested;
	}

	public void setTypes(List<String> types) {
		this.types = types;
	}

	public void addType(String type) {
		if(types != null)
			this.types.add(type);
		else {
			types = new ArrayList<String>();
			types.add(type);
		}
	}

}
