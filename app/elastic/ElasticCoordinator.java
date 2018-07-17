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
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;

import db.DB;
import elastic.ElasticSearcher.SearchOptions;
import model.EmbeddedMediaObject;
import model.resources.RecordResource;
import model.resources.collection.CollectionObject;
import play.Logger;
import play.Logger.ALogger;
import search.Fields;
import search.Filter;
import search.Response.SingleResponse;
import search.Sources;

public class ElasticCoordinator {

	public static final ALogger log = Logger.of(ElasticCoordinator.class);

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
		List<List<Filter>> newFilters = filters.stream().map(
				clause -> {
					return clause.stream()
							.filter((f)-> f.value!=null)
					.map(filter -> {
						Filter newFilter = (Filter) filter.clone();
						if (newFilter.fieldId.equals("anywhere")) {
							newFilter.fieldId = "";
							newFilter.value = "dance";
						}
						if (Fields.media_withRights.fieldId().equals(newFilter.fieldId))
							newFilter.value = EmbeddedMediaObject.WithMediaRights.getRights(newFilter.value).name();
						if (newFilter.fieldId.equals("hasImage")) {
							newFilter.fieldId = "media.type";
							newFilter.value = "IMAGE";
						}
						return newFilter;
						
					}).collect(Collectors.toList());
				}).collect(Collectors.toList());
		List<QueryBuilder> musts = new ArrayList<QueryBuilder>();
		for (List<Filter> ors : newFilters) {
			musts.add(searcher.boolShouldQuery(ors));
		}
		List<QueryBuilder> must_not = new ArrayList<QueryBuilder>();
		must_not.add(searcher.boolShouldQuery(new ArrayList<Filter>() {
			{
				add(new Filter("descriptiveData.label.default", "_favorites", true));
			}
		}));
		
		SearchResponse elasticresp = searcher.executeWithAggs(musts, must_not, options);
				/*searcher.getBoolSearchRequestBuilder(musts, null, null, options)
				.execute().actionGet();*/


		SingleResponse sresp = new SingleResponse();
		List<ObjectId> ids = new ArrayList<ObjectId>();
		String type = null;
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		for(SearchHit h: elasticresp.getHits()) {
			ids.add(new ObjectId(h.getId()));
		}
		for( RecordResource<?> r: DB.getRecordResourceDAO().getByIds(ids)) { 
			resultMap.put( r.getDbId().toHexString(), r);
		}
		for( CollectionObject<?> co: DB.getCollectionObjectDAO().getByIds(ids)) { 
			resultMap.put( co.getDbId().toHexString(), co);
		}

		for(SearchHit h: elasticresp.getHits()) {
			Object obj = resultMap.get( h.getId());
			if( obj == null ) {
				log.warn( h.getId() + " was in index but not in Mongo?");
			}
			sresp.items.add( obj );
		}
		
		sresp.totalCount = (int) elasticresp.getHits().getTotalHits();
		sresp.source_id = Sources.WITHin.getID();
		sresp.count = resultMap.size();
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

	/*
	 * Supplement methods not implemented yet
	 */

	public SingleResponse relatedDisMaxSearch(List<List<Filter>> filters) {
		ElasticSearcher relator = new ElasticSearcher();

		SearchResponse elasticresp = relator.relatedWithDisMax(filters.get(0));

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

		SearchResponse elasticresp = relator.relatedWithDisMax(filters.get(0));

		SingleResponse sresp = new SingleResponse();
		List<ObjectId> ids = new ArrayList<ObjectId>();
		for(SearchHit h: elasticresp.getHits()) {
			ids.add(new ObjectId(h.getId()));
		}
		sresp.items = DB.getRecordResourceDAO().getByIds(ids);
		sresp.totalCount = (int) elasticresp.getHits().getTotalHits();

		return sresp;
	}


	/*
	 * ******************************************
	 */

	private void extractFacets(Aggregations aggs, SingleResponse sresp) {
			for (Aggregation agg : aggs.asList()) {
				Terms aggTerm = (Terms) agg;
				if (aggTerm.getBuckets().size() > 0) {
					for (int i=0; i< aggTerm.getBuckets().size(); i++) {
						// omit the last character of aggregation name
						// in order to take the exact name of the field name
						String id = aggTerm.getName().substring(0, aggTerm.getName().length()-1);
						String value = aggTerm.getBuckets().get(i).getKeyAsString();
						if (Fields.media_withRights.fieldId().equals(id)){
							value = EmbeddedMediaObject.WithMediaRights.getRights(value).toString();
						}
						sresp.addFacet(id,value,
								(int)aggTerm.getBuckets().get(i).getDocCount());
					}
				}
			}
	}
}
