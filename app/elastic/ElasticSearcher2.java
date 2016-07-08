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
import java.util.Map.Entry;

import play.Logger;
import play.Logger.ALogger;
import utils.Tuple;
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
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder;
import org.elasticsearch.index.query.NotFilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder.Operator;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.SuggestBuilder.SuggestionBuilder;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionFuzzyBuilder;
import org.elasticsearch.search.suggest.phrase.PhraseSuggestionBuilder;
import org.elasticsearch.search.suggest.term.TermSuggestionBuilder;

public class ElasticSearcher2 {
	public static final int DEFAULT_RESPONSE_COUNT = 10;
	public static final ALogger log = Logger.of(ElasticSearcher2.class);

	private final String name;
	private List<String> types = new ArrayList<String>();


	private final Map<String, Float> fedSearchFieldsWithBoosts;
	private List<String> fieldsForSimilarity;
	private final List<String> aggregatedFields;

	private final Client client = null;
	public static final int DEFAULT_COUNT = 10;

	public static class SearchOptions {
		public boolean scroll = false;
		public int offset = 0;
		public int count = DEFAULT_COUNT;
		public boolean isPublic = true;
		public boolean fetchSource = false;

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
	}

	public void closeClient(){
		if(this.client != null){
			this.client.close();
		}
	}

	public ElasticSearcher2() {
		this.name = Elastic.index;

		this.fedSearchFieldsWithBoosts = new HashMap<String, Float>();
		fedSearchFieldsWithBoosts.put("label", 1.8f);
		fedSearchFieldsWithBoosts.put("description", 1.5f);
		fedSearchFieldsWithBoosts.put("keywords", 1.3f);

		this.aggregatedFields = new ArrayList<String>();
		//aggregatedFields.add("resourceType.all");
		aggregatedFields.add("provider.all");
		aggregatedFields.add("dataProvider.all");
		aggregatedFields.add("media.type.all");
		aggregatedFields.add("dccreator.default.all");
		aggregatedFields.add("dccontributor.default.all");
		aggregatedFields.add("dctermsspatial.default.all");
		aggregatedFields.add("contentusage.all");
		aggregatedFields.add("dates");
		aggregatedFields.add("media.withRights.all");

	}


	/* Query Execution */

	public SearchResponse execute(List<QueryBuilder> must_qs, List<QueryBuilder> must_not_qs, SearchOptions options) {
		SearchRequestBuilder search = this.getBoolSearchRequestBuilder(must_qs, must_not_qs, options);
		return search.execute().actionGet();
	}

	public SearchResponse executeWithAggs(List<QueryBuilder> must_qs, List<QueryBuilder> must_not_qs, SearchOptions options) {
		SearchRequestBuilder search = this.getBoolSearchRequestBuilder(must_qs, must_not_qs, options);
		for(String aggName: aggregatedFields) {
			TermsBuilder agg = AggregationBuilders.terms(aggName.replace(".all", "")).field(aggName);
			search.addAggregation(agg);
		}

		//System.out.println(search.toString());
		return search.execute().actionGet();
	}

	public SuggestResponse executeSuggestion(SuggestionBuilder suggestion, SearchOptions options) {
		SuggestRequestBuilder sugg = this.getSuggestRequestBuilder(suggestion, options);
		return sugg.execute().actionGet();
	}

	/* Query Constractors */

	/* Function Score query */
	public QueryBuilder funcScoreQuery(String term) {
		QueryStringQueryBuilder qstr = QueryBuilders.queryStringQuery(term);
		for(Entry<String, Float> e: fedSearchFieldsWithBoosts.entrySet()) {
			qstr.field(e.getKey()+"_all", e.getValue());
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
	public QueryBuilder searchExactStringMatches(String lang, String fieldName, String value) {
		TermQueryBuilder term_query = QueryBuilders.termQuery(fieldName+"."+lang, value);
		return term_query;
	}

	/* Related using Dis_Max query */
	public QueryBuilder relatedWithDisMax(String terms, String provider, String excludeId) {

		if(terms == null) terms = "";

		DisMaxQueryBuilder dis_max_q = QueryBuilders.disMaxQuery();
		MatchQueryBuilder title_match = QueryBuilders.matchQuery("label_all", terms);
		MatchQueryBuilder desc_match = QueryBuilders.matchQuery("description_all", terms);
		MatchQueryBuilder provider_match = QueryBuilders.matchQuery("provider", provider);

		dis_max_q.add(title_match).add(desc_match).add(provider_match);
		dis_max_q.tieBreaker(0.3f);

		if(excludeId != null) {
			NotFilterBuilder not_filter = FilterBuilders.notFilter(this.filter("_id", excludeId));
			FilteredQueryBuilder filtered = new FilteredQueryBuilder(dis_max_q, not_filter);
			return filtered;
		}
		else
			return dis_max_q;


	}

	/* Related using More Like This query */
	public QueryBuilder relatedWithMLT(String text, List<String> ids, List<String> fields) {

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

		return mlt;
	}

	/* Related using Bool query with Should clauses */
	public QueryBuilder relatedWithShouldClauses(Map<String, List<String>> records) {

		BoolQueryBuilder bool = QueryBuilders.boolQuery();
		for(Entry<String, List<String>> e: records.entrySet()) {
			int boost = Integer.parseInt(e.getKey().split(":")[1]);
			for(String v: e.getValue()) {
				QueryBuilder query = QueryBuilders.matchQuery("_all", v).boost(boost);
				bool.should(query);
			}
		}

		return bool;
	}

	/*
	 * Term filter
	 * WILL BE DEPRECATED
	 */
	private FilterBuilder filter(String key, String value) {
		FilterBuilder filter = FilterBuilders.termFilter(key, value);
		return filter;
	}

	public SuggestResponse searchSuggestions(String term, String field, SearchOptions options) {

		TermSuggestionBuilder sugg = SuggestBuilders.termSuggestion(term);
		sugg.text(term);
		sugg.field(field);

		return this.executeSuggestion(sugg, options);
	}

	/*
	 * Geo filter
	 * WILL BE DEPRECATED
	 */
	/*
	* Tip 0: The default field to be used is "coordinates"
	* Tip 1: Location is a tuple in the mode <lat, lon>
	* Tip 2: distance will always be calculated in kilometers as km
	*/
	private FilterBuilder geodistanceFilter(String fieldName, Tuple<Double, Double> location, String distance) {
		GeoDistance geod = GeoDistance.PLANE;
		FilterBuilder geodistance = FilterBuilders.geoDistanceFilter(fieldName)
									.cache(true)
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
	private SearchRequestBuilder getBoolSearchRequestBuilder(List<QueryBuilder> must_qs, List<QueryBuilder> must_not_qs, SearchOptions options) {
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
		if(must_not_qs!=null)
			for(QueryBuilder q: must_not_qs)
				outer_bool.mustNot(q);

		search.setQuery(outer_bool);
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