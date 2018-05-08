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
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.geo.GeoDistance;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.DisMaxQueryBuilder;
import org.elasticsearch.index.query.GeoDistanceQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder.Operator;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.suggest.SuggestBuilder.SuggestionBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionFuzzyBuilder;
import org.elasticsearch.search.suggest.phrase.PhraseSuggestionBuilder;
import org.elasticsearch.search.suggest.term.TermSuggestionBuilder;

import play.Logger;
import play.Logger.ALogger;
import search.Filter;
import utils.Tuple;

public class ElasticSearcher {
	public static final int DEFAULT_RESPONSE_COUNT = 10;
	public static final ALogger log = Logger.of(ElasticSearcher.class);

	private final String name;
	private List<String> types = new ArrayList<String>();


	private final Map<String, Float> fedSearchFieldsWithBoosts;
	private List<String> fieldsForSimilarity;


	private final Client client = null;
	public static final int DEFAULT_COUNT = 10;

	public static class SearchOptions {
		public boolean scroll = false;
		public int offset = 0;
		public int count = DEFAULT_COUNT;
		public boolean isPublic = true;
		public boolean fetchSource = false;
		public String[] searchFields;
		private final List<String> aggregatedFields = new ArrayList<String>();

		public SearchOptions() {
		}

		public SearchOptions(int offset, int count) {
			this.offset = offset;
			this.count = count;
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

		public void setPublic(boolean isPublic) {
			this.isPublic = isPublic;
		}

		public void setFetchSource(boolean source) {
			this.fetchSource = source;
		}

		public List<String> getAggregatedFields() {
			return aggregatedFields;
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
	}


	/* Query Execution */

	public SearchResponse execute(List<QueryBuilder> must_qs, List<QueryBuilder> must_not_qs, SearchOptions options) {
		SearchRequestBuilder search = this.getBoolSearchRequestBuilder(must_qs, null, must_not_qs, options);
		return search.execute().actionGet();
	}

	public SearchResponse executeWithAggs(List<QueryBuilder> must_qs, List<QueryBuilder> must_not_qs, SearchOptions options) {
		SearchRequestBuilder search = this.getBoolSearchRequestBuilder(must_qs, null, must_not_qs, options);
		for(String aggName: options.getAggregatedFields()) {
			TermsBuilder agg1 = AggregationBuilders.terms(aggName+"1").field(aggName+".string");
			TermsBuilder agg2 = AggregationBuilders.terms(aggName+"2").field(aggName+"._all.string");
			search.addAggregation(agg1).addAggregation(agg2);
		}

		//System.out.println(search.toString());
		return search.execute().actionGet();
	}

	public SuggestResponse executeSuggestion(SuggestionBuilder suggestion, SearchOptions options) {
		SuggestRequestBuilder sugg = this.getSuggestRequestBuilder(suggestion, options);
		return sugg.execute().actionGet();
	}

	/* Query Constractors */

	/* Bool Should query */
	public QueryBuilder boolShouldQuery(List<Filter> filters) {

		BoolQueryBuilder bool = QueryBuilders.boolQuery();
		for(Filter f: filters) {
			if( StringUtils.isNotEmpty(f.from) || StringUtils.isNotEmpty(f.to)) {
				bool.should( rangeQuery( f.fieldId+".string", f.from, f.to ));
			} else {
				if(!f.exact)
					bool.should(funcScoreQuery(f.fieldId, f.value));
				else
					bool.should(termQuery(f.fieldId, f.value));
			}
		}
		return bool;
		}

	public QueryBuilder rangeQuery( String field, String from, String to ) {
		RangeQueryBuilder q = QueryBuilders.rangeQuery( field );
		if( StringUtils.isNotEmpty(from )) {
			q.from( from );
			q.includeLower(true);
		}
		if( StringUtils.isNotEmpty( to )) {
			q.to( to );
			q.includeUpper(true);
		}
		return q;
	}

	/* Bool Must NOT query */
	public QueryBuilder boolMustNotQuery(List<Filter> filters) {

		BoolQueryBuilder bool = QueryBuilders.boolQuery();
		for(Filter f: filters) {
			if(!f.exact)
				bool.mustNot(funcScoreQuery(f.fieldId, f.value));
			else
				bool.mustNot(termQuery(f.fieldId, f.value));
		}
		return bool;
		}


	/*
	 * Function Score query
	 * Not exact match
	 *  */
	public QueryBuilder funcScoreQuery(String field, String term) {
		// sanitize the term
		// it will not support the query string syntax of lucene
		// no fields (thats handled somewhere else
		// no ranges (again somewhere else)
		term = term
				.replaceAll("[:\\[\\]+\\-~()!\"^]", " ")
				.replaceAll(" OR ", " or ")
				.replaceAll(" AND ", " and ")
				.replaceAll("^NOT ", "not ")
				.replaceAll(" NOT ", " not ");
				
		QueryStringQueryBuilder qstr = QueryBuilders.queryStringQuery(term);
		if(!field.equals("")) {
			qstr.field(field);
			qstr.field(field+"._all");
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
		qstr.fuzzyPrefixLength(2);
		//
		qstr.phraseSlop(0);
		qstr.autoGeneratePhraseQueries(false);
		qstr.maxDeterminizedStates(10000);
		//qstr.minimumShouldMatch(minimumShouldMatch);
		qstr.lenient(true);


		FunctionScoreQueryBuilder func_score =  QueryBuilders.functionScoreQuery(qstr);

		if(false) {
			ScoreFunctionBuilder decay = ScoreFunctionBuilders.exponentialDecayFunction(null, null, null);
			func_score.add(decay);
		}

		return func_score;
	}

	/* Terms query */
	public QueryBuilder termsQuery(String field, List<String> terms) {
		TermsQueryBuilder terms_q = QueryBuilders.termsQuery(field, terms);
		return terms_q;
	}

	/*
	* Term query used to bring documents that contain the EXACT value
	* in a not-analyzed field.
	* This query can be used within some DAO methods to get faster results.
	*
	* Eg could be used within getByLabel etc
	*/
	public QueryBuilder termQuery(String fieldName, String value) {
		TermQueryBuilder term_query = QueryBuilders.termQuery(fieldName+".string", value);
		if(fieldName.toLowerCase().contains("public"))
			term_query = QueryBuilders.termQuery(fieldName, value);
		return term_query;
	}

	/* Nested query
	 * For nested rights structure
	 */
	public QueryBuilder nestedQuery(String path, List<Filter> f	) {
		BoolQueryBuilder bool_q = QueryBuilders.boolQuery();
		NestedQueryBuilder nested_q = QueryBuilders.nestedQuery(path, bool_q);

		return nested_q;
	}

	/* Related using Dis_Max query */
	public SearchResponse relatedWithDisMax(List<Filter> filters) {


		DisMaxQueryBuilder dis_max_q = QueryBuilders.disMaxQuery();
		for(Filter f: filters) {
			MatchQueryBuilder match1 = QueryBuilders.matchQuery(f.fieldId+f.lang, f.value);
			MatchQueryBuilder match2 = QueryBuilders.matchQuery(f.fieldId+"._all", f.value);
			dis_max_q.add(match1).add(match2);
		}
		dis_max_q.tieBreaker(0.3f);

		return this.getSearchRequestBuilder(dis_max_q, new SearchOptions()).execute().actionGet();
	}

	/* Related using More Like This query */
	public SearchResponse relatedWithMLT(List<Filter> filters) {


		MoreLikeThisQueryBuilder mlt;
		List<String> fields = new ArrayList<String>();
		List<String> texts = new ArrayList<String>();
		List<String> ids = new ArrayList<String>();
		for(Filter f: filters) {
			if(f.fieldId.equals("_id")) {
				fields.add(f.fieldId);
				texts.add(f.value);
			} else {
				ids.add(f.value);
			}
		}
		mlt = QueryBuilders.moreLikeThisQuery(fields.toArray(new String[fields.size()]));
		mlt.like(texts.toArray(new String[texts.size()]));
		mlt.ids(ids.toArray(new String[ids.size()]));


		mlt.maxQueryTerms(20);
		mlt.minTermFreq(1);

		return this.getSearchRequestBuilder(mlt, new SearchOptions()).execute().actionGet();
	}

	/* Related using Bool query with Should clauses */
	public SearchResponse relatedWithShouldClauses(List<Filter> filters) {

		BoolQueryBuilder bool = QueryBuilders.boolQuery();
		for(Filter f: filters) {
			QueryBuilder query1 = QueryBuilders.matchQuery(f.fieldId+f.lang, f.value);
			QueryBuilder query2 = QueryBuilders.matchQuery(f.fieldId+"._all", f.value);
			bool.should(query1).should(query2);
		}
		return this.getSearchRequestBuilder(bool, new SearchOptions()).execute().actionGet();
	}


	public SuggestResponse searchSuggestions(String term, String field, SearchOptions options) {

		TermSuggestionBuilder sugg = SuggestBuilders.termSuggestion(term);
		sugg.text(term);
		sugg.field(field);

		return this.executeSuggestion(sugg, options);
	}

	/*
	 * Geo filter
	 */
	/*
	* Tip 0: The default field to be used is "coordinates"
	* Tip 1: Location is a tuple in the mode <lat, lon>
	* Tip 2: distance will always be calculated in kilometers as km
	*/
	private QueryBuilder geodistanceFilter(String fieldName, Tuple<Double, Double> location, String distance) {
		GeoDistance geod = GeoDistance.PLANE;
		GeoDistanceQueryBuilder geodistance = QueryBuilders.geoDistanceQuery(fieldName)
									.point(location.x, location.y)
									.distance(distance+"km")
									.geoDistance(geod);
		return geodistance;
	}

	/* Suggester builders */
	/*
	* Suggestions via term suggestions.
	* Functionality like "did you mean...?"
	*/
	public SuggestResponse searchTermSuggestions(String term, String field, SearchOptions options) {

		TermSuggestionBuilder sugg = SuggestBuilders.termSuggestion(term);
		sugg.text(term);
		sugg.field(field);

		return this.executeSuggestion(sugg, options);
	}


	/*
	* Suggestions using phrase suggestions.
	* Functionality like "did you mean...?"
	*/
	public SuggestResponse searchPhraseSuggestions(String term, String field, SearchOptions options) {

		PhraseSuggestionBuilder sugg = SuggestBuilders.phraseSuggestion(term);
		sugg.text(term);
		sugg.field(field);
		sugg.highlight("<em>", "</em>");
		sugg.realWordErrorLikelihood(0.95f);
		sugg.gramSize(1);
		//sugg.smoothingModel("stupid_backoff");

		return this.executeSuggestion(sugg, options);
	}


	/*
	* Completion suggester for real time auto-complition.
	*/
	public SuggestResponse searchCompletionSuggester(String term, String field, SearchOptions options, boolean fuzzy) {

		if(fuzzy) {
			CompletionSuggestionFuzzyBuilder sugg = SuggestBuilders.fuzzyCompletionSuggestion(term);
			sugg.text(term);
			sugg.field(field);
			sugg.setFuzziness(Fuzziness.TWO);
			return this.executeSuggestion(sugg, options);
		}
		else {
			CompletionSuggestionBuilder sugg = SuggestBuilders.completionSuggestion(term);
			sugg.text(term);
			sugg.field(field);
			return this.executeSuggestion(sugg, options);
		}
	}


	/*
	 * Build search query build on bool query
	 */
	public SearchRequestBuilder getBoolSearchRequestBuilder(List<QueryBuilder> must_qs, List<QueryBuilder> should_qs,
															List<QueryBuilder> must_not_qs, SearchOptions options) {
		SearchRequestBuilder search = Elastic.getTransportClient()
				.prepareSearch(name)
				.setTypes(types.toArray(new String[types.size()]))
				.setFetchSource(false);

		if(!options.scroll) {
			search.setFrom(options.offset)
				  .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
				  .setSize(options.count);
		} else {
			search.setSearchType(SearchType.SCAN)
				  .setScroll(new TimeValue(60000))
				  .setSize(100);
		}

		BoolQueryBuilder outer_bool = QueryBuilders.boolQuery();
		if(must_qs!=null)
			for(QueryBuilder q: must_qs)
				outer_bool.must(q);
		if(should_qs!=null)
			for(QueryBuilder q: should_qs)
				outer_bool.should(q);
		if(must_not_qs!=null)
			for(QueryBuilder q: must_not_qs)
				outer_bool.mustNot(q);

		search.setQuery(outer_bool);
		return search;
	}


	public SearchRequestBuilder getSearchRequestBuilder(QueryBuilder q, SearchOptions options) {
		SearchRequestBuilder search = Elastic.getTransportClient()
				.prepareSearch(name)
				.setTypes(types.toArray(new String[types.size()]))
				.setFetchSource(false);

		if(!options.scroll) {
			search.setFrom(options.offset)
				  .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
				  .setSize(options.count);
		} else {
			search.setSearchType(SearchType.SCAN)
				  .setScroll(new TimeValue(60000))
				  .setSize(100);
		}

		if((options.searchFields!=null) && (options.searchFields.length > 0)) {
			search.addFields(options.searchFields);
		}
		search.setQuery(q);
		return search;
	}
	/*
	 * Build suggestion query
	 */
	private SuggestRequestBuilder getSuggestRequestBuilder(SuggestionBuilder suggestion, SearchOptions options) {
		SuggestRequestBuilder sugg = Elastic.getTransportClient()
									.prepareSuggest(name)
									.addSuggestion(suggestion);

		return sugg;
	}


	/*
	 * Getters and Setters
	 */
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