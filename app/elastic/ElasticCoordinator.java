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
import java.util.List;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.InternalTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;

import controllers.WithController.Profile;
import db.DB;
import search.Filter;
import search.Response.SingleResponse;
import search.Response.ValueCount;
import search.Sources;
import elastic.ElasticSearcher.SearchOptions;

public class ElasticCoordinator {

	private SearchOptions options;

	public ElasticCoordinator() {}

	public ElasticCoordinator(SearchOptions options) {
		this.options = options;
	}


	/*
	 * This method will be used for records, collections, whatever type 'normal' search.
	 * The type will be specified in the filter with 'type' field.
	 * And the rights will specify the group of resources we are searching for
	 * e.g. public, resources I have read access, resources I own, resources shared with me,
	 * resource of a group ...
	 */
	public SingleResponse federatedSearch(List<List<Filter>> filters) {
		ElasticSearcher searcher = new ElasticSearcher();

		filters.forEach( (a) -> {a.forEach( (f) ->{ if(f.fieldId.equals("anywhere")) f.fieldId = "";});});

		List<QueryBuilder> musts = new ArrayList<QueryBuilder>();
		for(List<Filter> ors: filters) {
			musts.add(searcher.boolShouldQuery(ors));
		}
		SearchResponse elasticresp = searcher.executeWithAggs(musts, null, options);
				/*searcher.getBoolSearchRequestBuilder(musts, null, null, options)
				.execute().actionGet();*/


		SingleResponse sresp = new SingleResponse();
		List<ObjectId> ids = new ArrayList<ObjectId>();
		String type = null;
		for(SearchHit h: elasticresp.getHits()) {
			type = h.getType();
			ids.add(new ObjectId(h.getId()));
		}
		if((type !=null) && type.contains("object"))
			sresp.items = DB.getRecordResourceDAO().getByIds(ids).stream().map( r -> {return r.getRecordProfile(Profile.BASIC.toString());}).collect(Collectors.toList());
		else
			sresp.items = DB.getCollectionObjectDAO().getByIds(ids).stream().map( r -> {return r.getCollectionProfile(Profile.BASIC.toString());}).collect(Collectors.toList());
		sresp.totalCount = (int) elasticresp.getHits().getTotalHits();
		sresp.source = Sources.WITHin;
		
		if(elasticresp.getAggregations() != null)
			extractFacets(elasticresp.getAggregations(), sresp);

		return sresp;
	}

	public SingleResponse annotationSearch() {
		return null;
	}


	public SearchResponse queryExcecution(QueryBuilder q, SearchOptions options) {
		ElasticSearcher searcher = new ElasticSearcher();
		return searcher.getSearchRequestBuilder(q, options).execute().actionGet();
	}

	public SingleResponse relatedDisMaxSearch(List<List<Filter>> filters) {
		ElasticSearcher relator = new ElasticSearcher();

		//SearchResponse elasticresp = relator.getSearchRequestBuilder(relator.relatedWithDisMax(terms, provider, excludeId)
		SearchResponse elasticresp = null;

		SingleResponse sresp = new SingleResponse();
		List<ObjectId> ids = new ArrayList<ObjectId>();
		for(SearchHit h: elasticresp.getHits()) {
			ids.add(new ObjectId(h.getId()));
		}
		sresp.items = DB.getRecordResourceDAO().getByIds(ids);
		sresp.totalCount = (int) elasticresp.getHits().getTotalHits();

		return sresp;
	}

	public SingleResponse relatedMLTSearch(List<List<Filter>> filters) {
		ElasticSearcher relator = new ElasticSearcher();

		//SearchResponse elasticresp = relator.getSearchRequestBuilder(relator.relatedWithDisMax(terms, provider, excludeId)
		SearchResponse elasticresp = null;

		SingleResponse sresp = new SingleResponse();
		List<ObjectId> ids = new ArrayList<ObjectId>();
		for(SearchHit h: elasticresp.getHits()) {
			ids.add(new ObjectId(h.getId()));
		}
		sresp.items = DB.getRecordResourceDAO().getByIds(ids);
		sresp.totalCount = (int) elasticresp.getHits().getTotalHits();

		return sresp;
	}

	public SingleResponse relatedBoolShouldSearch(List<List<Filter>> filters) {
		ElasticSearcher relator = new ElasticSearcher();

		//SearchResponse elasticresp = relator.getSearchRequestBuilder(relator.relatedWithDisMax(terms, provider, excludeId)
		SearchResponse elasticresp = null;

		SingleResponse sresp = new SingleResponse();
		List<ObjectId> ids = new ArrayList<ObjectId>();
		for(SearchHit h: elasticresp.getHits()) {
			ids.add(new ObjectId(h.getId()));
		}
		sresp.items = DB.getRecordResourceDAO().getByIds(ids);
		sresp.totalCount = (int) elasticresp.getHits().getTotalHits();

		return sresp;
	}


	private void extractFacets(Aggregations aggs, SingleResponse sresp) {
			for (Aggregation agg : aggs.asList()) {
				Terms aggTerm = (Terms) agg;
				if (aggTerm.getBuckets().size() > 0) {
					for (int i=0; i< aggTerm.getBuckets().size(); i++) {
						sresp.addFacet(aggTerm.getName(), aggTerm.getBuckets().get(i).getKeyAsString(), (int)aggTerm.getBuckets().get(i).getDocCount());
					}
				}
			}
	}
}
