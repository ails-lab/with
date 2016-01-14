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


package espace.core.sources;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import model.Collection;
import model.Rights;
import model.User;
import model.Rights.Access;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.terms.InternalTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.UnmappedTerms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.Logger;
import play.libs.Json;
import utils.ListUtils;
import utils.Tuple;
import elastic.Elastic;
import elastic.ElasticSearcher;
import elastic.ElasticSearcher.SearchOptions;
import elastic.ElasticUtils;
import espace.core.CommonFilter;
import espace.core.CommonFilterLogic;
import espace.core.CommonQuery;
import espace.core.ISpaceSource;
import espace.core.RecordJSONMetadata;
import espace.core.SourceResponse;

public class WithSpaceSource extends ISpaceSource {
	public static final Logger.ALogger log = Logger.of(WithSpaceSource.class);
	
	//there should be a filter on source
	//in general, more filters in new model for search within WITH db
	public enum WithinFilters {
		Provider("provider"), Type("type"), DataProvider("dataprovider"),
		Creator("creator"), Rights("rights"),
		Country("country"), Year("year");
		
		private String value;
		
		WithinFilters(String value) {
	        this.value = value;
	    }

	    @Override
	    public String toString() {
	        return value;
	    }
	}

	@Override
	public String getSourceName() {
		return "WITHin";
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		/*
		 * Set the basic search parameters
		 */
		ElasticSearcher searcher = new ElasticSearcher(Elastic.type_general);
		String term = q.getQuery();
		int count = Integer.parseInt(q.pageSize);
		int offset = (Integer.parseInt(q.page)-1)*count;

		/*
		 * Prepare access lists for searching
		 */
		SearchOptions elasticoptions = new SearchOptions();
		List<Collection> colFields = new ArrayList<Collection>();
		List<String> userIds = q.getEffectiveUserIds();
		List<Tuple<ObjectId, Access>> userAccess = new ArrayList<Tuple<ObjectId, Access>>();
		if ((userIds != null) && !userIds.isEmpty()) {
			for (String userId : userIds)
				userAccess.add(new Tuple<ObjectId, Access>(
						new ObjectId(userId), Access.READ));
		}
		List<List<Tuple<ObjectId, Access>>> accessFilters = mergeLists(
				q.getDirectlyAccessedByGroupName(),
				q.getDirectlyAccessedByUserName());
		if (!userAccess.isEmpty())
			accessFilters.add(userAccess);
		elasticoptions.accessList = accessFilters;

		/*
		 * Search index for accessible collections
		 */
		searcher.setType(Elastic.type_collection);
		SearchResponse response = searcher
				.searchAccessibleCollectionsScanScroll(elasticoptions);
		List<SearchHit> hits = getTotalHitsFromScroll(response);
		colFields = getCollectionMetadaFromHit(hits);

		/*
		 * Search index for merged records according to collection ids gathered
		 * above
		 */
		elasticoptions = new SearchOptions(offset, count);
		elasticoptions.addFilter("isPublic", "true");
		List<CommonFilter> filters = q.filters;
		for (CommonFilter f: filters) {
			for (String filterValue: f.values) {
				elasticoptions.addFilter(f.filterID, filterValue);
			}
		}
		searcher.setType(Elastic.type_general);
		for (Collection collection : colFields) {
			elasticoptions.addFilter("collections", collection.getDbId()
					.toString());
		}

		SearchResponse resp = searcher.search(term, elasticoptions);
		searcher.closeClient();

		SourceResponse res = new SourceResponse(resp, offset);
		
		/*
		for (WithinFilters filter: WithinFilters.values()) {
			//Create CommonFilterLogic dynamically!!!!!!!!!!!!!!!!! See changes and e.g. EuropeanaSource in new model.
		}
	    */
		CommonFilterLogic type = CommonFilterLogic.typeFilter();
		CommonFilterLogic provider = CommonFilterLogic.providerFilter();
		CommonFilterLogic dataprovider = CommonFilterLogic.dataproviderFilter();
		CommonFilterLogic creator = CommonFilterLogic.creatorFilter();
		CommonFilterLogic rights = CommonFilterLogic.rightsFilter();
		CommonFilterLogic country = CommonFilterLogic.countryFilter();
		CommonFilterLogic year = CommonFilterLogic.yearFilter();

		if (checkFilters(q)) {
			for (Entry<String, Aggregation> e : resp.getAggregations().asMap()
					.entrySet()) {
				e.getKey();

			}
			for (Aggregation agg : resp.getAggregations().asList()) {
				InternalTerms aggTerm = (InternalTerms) agg;
				if (aggTerm.getBuckets().size() > 0) {
					switch (agg.getName()) {
					case "types": {
						for (int i=0; i< aggTerm.getBuckets().size(); i++) {
							countValue(type, aggTerm.getBuckets().get(i)
									.getKey(), (int) aggTerm.getBuckets().get(i)
									.getDocCount());
						}
						break;
					}
					case "providers": {
						for (int i=0; i< aggTerm.getBuckets().size(); i++) {
							countValue(provider, aggTerm.getBuckets().get(i)
									.getKey(), (int) aggTerm.getBuckets().get(i)
									.getDocCount());
						}
						break;
					}
					case "dataProviders": {
						for (int i=0; i< aggTerm.getBuckets().size(); i++) {
							countValue(dataprovider, aggTerm.getBuckets().get(i)
									.getKey(), (int) aggTerm.getBuckets().get(i)
									.getDocCount());
						}
						break;
					}
					case "creators": {
						for (int i=0; i< aggTerm.getBuckets().size(); i++) {
							countValue(creator, aggTerm.getBuckets().get(i)
									.getKey(), (int) aggTerm.getBuckets().get(i)
									.getDocCount());
						}
						break;
					}
					case "rights": {
						for (int i=0; i< aggTerm.getBuckets().size(); i++) {
							countValue(rights, aggTerm.getBuckets().get(i)
									.getKey(), (int) aggTerm.getBuckets().get(i)
									.getDocCount());
						}
						break;
					}
					case "countries": {
						for (int i=0; i< aggTerm.getBuckets().size(); i++) {
							countValue(country, aggTerm.getBuckets().get(i)
									.getKey(), (int) aggTerm.getBuckets().get(i)
									.getDocCount());
						}
						break;
					}
					case "years": {
						for (int i=0; i< aggTerm.getBuckets().size(); i++) {
							countValue(year, aggTerm.getBuckets().get(i)
									.getKey(), (int) aggTerm.getBuckets().get(i)
									.getDocCount());
						}
						break;
					}
					default:
						break;
					}
				}
			}

			res.filtersLogic = new ArrayList<>();
			res.filtersLogic.add(type);
			res.filtersLogic.add(provider);
			res.filtersLogic.add(dataprovider);
			res.filtersLogic.add(creator);
			res.filtersLogic.add(rights);
			res.filtersLogic.add(country);
			res.filtersLogic.add(year);
		}

		return res;
	}

	private List<SearchHit> getTotalHitsFromScroll(SearchResponse scrollResp) {

		List<SearchHit> totalHits = new ArrayList<SearchHit>();
		while (true) {
			for (SearchHit hit : scrollResp.getHits().getHits())
				totalHits.add(hit);

			scrollResp = Elastic.getTransportClient()
					.prepareSearchScroll(scrollResp.getScrollId())
					.setScroll(new TimeValue(60000)).execute().actionGet();

			if (scrollResp.getHits().getHits().length == 0)
				break;
		}
		return totalHits;
	}

	private List<Collection> getCollectionMetadaFromHit(List<SearchHit> hits) {

		List<Collection> colFields = new ArrayList<Collection>();
		for (SearchHit hit : hits) {
			JsonNode json = Json.parse(hit.getSourceAsString());
			JsonNode accessRights = json.get("rights");
			if (!accessRights.isMissingNode()) {
				ObjectNode ar = Json.newObject();
				for (JsonNode r : accessRights) {
					String user = r.get("user").asText();
					String access = r.get("access").asText();
					ar.put(user, access);
				}
				((ObjectNode) json).remove("rights");
				((ObjectNode) json).put("rights", ar);
			}
			Collection collection = Json.fromJson(json, Collection.class);
			collection.setDbId(new ObjectId(hit.getId()));
			colFields.add(collection);
		}
		return colFields;
	}

	@Override
	public ArrayList<RecordJSONMetadata> getRecordFromSource(String recordId) {
		log.debug("Method not implemented yet");
		return null;
	}

	private List<List<Tuple<ObjectId, Access>>> mergeLists(
			List<Tuple<ObjectId, Access>>... lists) {
		List<List<Tuple<ObjectId, Access>>> outputList = new ArrayList<List<Tuple<ObjectId, Access>>>();
		for (List<Tuple<ObjectId, Access>> list : lists) {
			for (Tuple<ObjectId, Access> tuple : list) {
				List<Tuple<ObjectId, Access>> sepList = new ArrayList<Tuple<ObjectId, Access>>(
						1);
				sepList.add(tuple);
				outputList.add(sepList);
			}
		}
		return outputList;
	}

}
