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
import java.util.Map.Entry;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;

import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;
import elastic.Elastic;
import elastic.ElasticSearcher;
import elastic.ElasticUtils;
import elastic.ElasticSearcher.SearchOptions;
import play.Logger;
import search.Sources;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import model.resources.RecordResource;
import model.resources.WithResource;
import model.resources.WithResource.WithResourceType;
import sources.core.CommonQuery;
import sources.core.ISpaceSource;
import sources.core.SourceResponse;
import utils.Tuple;

public class WithinASpaceSource extends ISpaceSource{
	public static final Logger.ALogger log = Logger.of(WithSpaceSource.class);


	public WithinASpaceSource() {
		super(Sources.WITHinASpace);
		// TODO Auto-generated constructor stub
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {

			int count = Integer.parseInt(q.pageSize);
			int offset = Integer.parseInt(q.page);

		try {
			List<List<Tuple<ObjectId, Access>>> access = new ArrayList<List<Tuple<ObjectId,Access>>>();
			access.add(new ArrayList<Tuple<ObjectId,Access>>() {{ add(new Tuple<ObjectId, WithAccess.Access>(new ObjectId("56e13d2e75fe2450755e553a"), Access.READ)); }} );
			SearchOptions options = new SearchOptions();
			options.accessList = access;
			options.setCount(count);
			options.isPublic = false;


			/*
			 * Search for space collections
			 */
			ElasticSearcher recordSearcher = new ElasticSearcher();
			recordSearcher.setTypes(new ArrayList<String>() {{ add(WithResourceType.SimpleCollection.toString().toLowerCase());
																add(WithResourceType.Exhibition.toString().toLowerCase());}});
			SearchResponse resp = recordSearcher.searchAccessibleCollections(options);
			List<String> colIds = new ArrayList<String>();
			resp.getHits().forEach( (h) -> {colIds.add(h.getId());return;} );

			/*
			 * Search for records of this space
			 */
			options.accessList.clear();
			options.setFilterType("or");
			//options.addFilter("_all", term);
			//options.addFilter("description", term);
			//options.addFilter("keywords", term);
			recordSearcher.setTypes(new ArrayList<String>() {{ addAll(Elastic.allTypes);
																remove(WithResourceType.SimpleCollection.toString().toLowerCase());
																remove(WithResourceType.Exhibition.toString().toLowerCase());}});
			resp = recordSearcher.searchInSpecificCollections(q.searchTerm, colIds, options);
			Map<String, List<?>> resourcesPerType = ElasticUtils.getResourcesPerType(resp);
			recordSearcher.closeClient();



			SourceResponse sourceRes = new SourceResponse((int)resp.getHits().getTotalHits(), offset, count);
			sourceRes.source = getSourceName().toString();
			sourceRes.setResourcesPerType(resourcesPerType);
			//sourceRes.transformResourcesToItems();
			filterRecordsOnly(sourceRes);
			return sourceRes;

		} catch(Exception e) {
			log.error("Search encountered a problem", e);
		}

		return null;

	}
	
	//TODO: When WIthin search is separated from external resources, 
		// and the response may contain resources of all types (not only CHO and RecordResource as defined in ItemsGrouping),
		//this method becomes obsolete.
		//Types should be passed from the API call, and handled at the within search controller level.
		private void filterRecordsOnly(SourceResponse sourceResponse) {
			List<WithResource<?, ?>> choItems = sourceResponse.items.getCulturalCHO();
			List<WithResource<?, ?>> recordResources = sourceResponse.items.getRecordResource();
			for (Entry<String, List<?>> e: sourceResponse.resourcesPerType.entrySet()) {
				if (e.getKey().equals(WithResourceType.CulturalObject.toString().toLowerCase())) 
					for (WithResource<?, ?>  record: (List<WithResource<?, ?>>) e.getValue()) {
						RecordResource profiledRecord = ((RecordResource) record).getRecordProfile("MEDIUM");
						choItems.add(profiledRecord);
					}
				if (e.getKey().equals(WithResourceType.RecordResource.toString().toLowerCase())) 
					for (WithResource<?, ?>  record: (List<WithResource<?, ?>>) e.getValue()) {
						RecordResource profiledRecord = ((RecordResource) record).getRecordProfile("MEDIUM");
						recordResources.add(profiledRecord);
					}
			}
			sourceResponse.items.setCulturalCHO(choItems);
			sourceResponse.items.setRecordResource(recordResources);
		}

}
