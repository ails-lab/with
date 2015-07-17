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

import model.Collection;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.Logger;
import play.libs.Json;
import elastic.Elastic;
import elastic.ElasticSearcher;
import elastic.ElasticSearcher.SearchOptions;
import espace.core.CommonQuery;
import espace.core.ISpaceSource;
import espace.core.RecordJSONMetadata;
import espace.core.SourceResponse;

public class WithSpaceSource extends ISpaceSource {
	public static final Logger.ALogger log = Logger.of(WithSpaceSource.class);


	@Override
	public String getSourceName() {
		return "With";
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		ElasticSearcher searcher = new ElasticSearcher(Elastic.type_general);
		String term = q.getQuery();
		int count = Integer.parseInt(q.pageSize);
		int offset = (Integer.parseInt(q.page)-1)*count;

		SearchOptions elasticoptions = null;
		List<Collection> colFields = new ArrayList<Collection>();

		if(q.user != null) {
			elasticoptions = new SearchOptions();
			elasticoptions.setUser(q.getUser());
			searcher.setType(Elastic.type_collection);
			SearchResponse response = searcher.search(null, elasticoptions);
			colFields = getCollectionMetadaFromHit(response.getHits());

		}
		elasticoptions = new SearchOptions(offset, count);
		elasticoptions.addFilter("isPublic", "true");

		//search merged_record type
		searcher.setType(Elastic.type_general);
		//elasticoptions.setFilterType("OR");
		//which collections are available
		//elasticoptions.addFilter("collections", "no_collections_found");
		for(Collection collection : colFields) {
			elasticoptions.addFilter("collections", collection.getDbId().toString());
		}
		//which source are available
		elasticoptions.addFilter("source", "no sources selected");
		if(q.mintSource)
			elasticoptions.addFilter("source", "mint");
		if(q.uploadedByUser)
			elasticoptions.addFilter("source", "uploadedByUser");

		SearchResponse resp = searcher.search(term, elasticoptions);
		searcher.closeClient();
		return new SourceResponse(resp, getSourceName(), count);
	}


	private List<Collection> getCollectionMetadaFromHit(
			SearchHits hits) {


		List<Collection> colFields = new ArrayList<Collection>();
		for(SearchHit hit: hits.getHits()) {
			JsonNode json         = Json.parse(hit.getSourceAsString());
			JsonNode accessRights = json.get("rights");
			if(!accessRights.isMissingNode()) {
				ObjectNode ar = Json.newObject();
				for(JsonNode r: accessRights) {
					String user   = r.get("user").asText();
					String access = r.get("access").asText();
					ar.put(user, access);
				}
				((ObjectNode)json).remove("rights");
				((ObjectNode)json).put("rights", ar);
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



}
