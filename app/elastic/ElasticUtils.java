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

import model.Collection;
import model.basicDataTypes.CollectionInfo;
import model.resources.RecordResource;

import org.bson.types.ObjectId;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import play.libs.Json;
import utils.ExtendedCollectionRecord;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ElasticUtils {


	public static RecordResource hitToRecord(SearchHit hit) {
		JsonNode json = Json.parse(hit.getSourceAsString());
		RecordResource record = Json.fromJson(json, RecordResource.class);
		/*if (hit.type().equals(Elastic.type_general)) {
			List<CollectionInfo> colIds = new ArrayList<CollectionInfo>();
			List<String> tags = new ArrayList<String>();
			ArrayNode ids = (ArrayNode) json.get("descriptiveData.collectedIn");
			ArrayNode allTags = (ArrayNode) json.get("tags");

			for (JsonNode id : ids)
				if (!colIds.contains(id.asText()))
					colIds.add(id.asText());
			for (JsonNode t : allTags)
				if (!tags.contains(t.asText()))
					tags.add(t.asText());

			record.setCollectedIn(colIds);
			record.setAllTags(tags);
		}*/

		return record;
	}

	public static Collection hitToCollection(SearchHit hit) {
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
		return collection;
	}

	public static List<Collection> getCollectionMetadaFromHit(
			SearchHits hits) {


		List<Collection> colFields = new ArrayList<Collection>();
		for(SearchHit hit: hits.getHits()) {
			Collection collection = hitToCollection(hit);
			collection.setDbId(new ObjectId(hit.getId()));
			colFields.add(collection);
		}
		return colFields;
	}
}
