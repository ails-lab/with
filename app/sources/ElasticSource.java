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


package sources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import model.basicDataTypes.WithAccess.Access;
import model.resources.RecordResource;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.terms.InternalTerms;

import play.Logger;
import play.libs.F.Promise;
import search.Query;
import search.Response;
import search.Source;
import search.Sources;
import sources.core.CommonFilter;
import sources.core.CommonFilterLogic;
import sources.core.CommonQuery;
import sources.core.ISpaceSource;
import sources.core.RecordJSONMetadata;
import sources.core.SourceResponse;
import utils.Tuple;
import elastic.Elastic;
import elastic.ElasticSearcher;
import elastic.ElasticUtils;
import elastic.ElasticSearcher.SearchOptions;

/*
 * This source is for internal search to WITH collections
 */
public class ElasticSource implements Source {
	public static final Logger.ALogger log = Logger.of(ElasticSource.class);

	@Override
	public Sources thisSource() {
		return Sources.WITHin;
	}

	@Override
	public Promise<Response> execute(Query query) {
		ElasticSearcher searcher = new ElasticSearcher();
		SearchOptions elasticoptions = new SearchOptions();

		/*
		 * Get the search parameters from the query
		 */
		String term = ((query.
		List<String)> types) = q.getTypes();
		int count = Integer.parseInt(q.pageSize);
		int offset = (Integer.parseInt(q.page)-1)*count;

		/* Access parameters */
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

		/*
		 * Prepare options for searching
		 */
		searcher.setTypes(types);
		elasticoptions.setCount(count);
		elasticoptions.setOffset(offset);
		elasticoptions.setScroll(false);
		elasticoptions.accessList = accessFilters;

		/* Filters */
		//elasticoptions.addFilter("isPublic", "true");
		List<CommonFilter> filters = q.filters;
		if (filters!=null){
			for (CommonFilter f: filters) {
				for (String filterValue: f.values) {
		    		elasticoptions.addFilter(f.filterID+".all", filterValue);
				}
			}

		}

		/*
		 * Search index for accessible resources
		 */
		SearchResponse elasticResponse = searcher
				.searchResourceWithWeights(term, elasticoptions);
		Map<String, List<?>> resourcesPerType = ElasticUtils.getResourcesPerType(elasticResponse);

		/* Finalize the searcher client and create the SourceResponse */
		searcher.closeClient();
		SourceResponse sourceResponse =
				new SourceResponse((int) elasticResponse.getHits().getTotalHits(), offset, count);
		sourceResponse.source = getSourceName().toString();
		sourceResponse.setResourcesPerType(resourcesPerType);
		//sourceResponse.transformResourcesToItems();
		filterRecordsOnly(sourceResponse);
		/* Check whether we need the aggregated values or not */
		if (checkFilters(q)) {
			sourceResponse.filtersLogic = new ArrayList<CommonFilterLogic>();
			if(elasticResponse.getAggregations() != null)
				for (Aggregation agg : elasticResponse.getAggregations().asList()) {
					InternalTerms aggTerm = (InternalTerms) agg;
					if (aggTerm.getBuckets().size() > 0) {
						CommonFilterLogic filter = new CommonFilterLogic(agg.getName());
						//CommonFilters.valueOf(agg.getName()));
						for (int i=0; i< aggTerm.getBuckets().size(); i++) {
							countValue(filter, aggTerm.getBuckets().get(i).getKey(),
								(int) aggTerm.getBuckets().get(0).getDocCount());
						}
						sourceResponse.filtersLogic.add(filter);
					}
				}
		}
		return sourceResponse;
	}

	@Override
	public Promise<Object> completeRecord(Object incompleteRecord) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Promise<Object> getById(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> supportedFieldnames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Promise<String[]> autocomplete(String partialQueryString) {
		// TODO Auto-generated method stub
		return null;
	}


}
