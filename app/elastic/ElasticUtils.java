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

import org.elasticsearch.search.SearchHit;

import play.libs.Json;
import utils.ExtendedCollectionRecord;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class ElasticUtils {


	public static ExtendedCollectionRecord hitToRecord(SearchHit hit) {
		JsonNode json = Json.parse(hit.getSourceAsString());
		ExtendedCollectionRecord record = Json.fromJson(json, ExtendedCollectionRecord.class);
		if (hit.type().equals(Elastic.type_general)) {
			List<String> colIds = new ArrayList<String>();
			List<String> tags = new ArrayList<String>();
			ArrayNode ids = (ArrayNode) json.get("collections");
			ArrayNode allTags = (ArrayNode) json.get("tags");

			for (JsonNode id : ids)
				if (!colIds.contains(id.asText()))
					colIds.add(id.asText());
			for (JsonNode t : allTags)
				if (!tags.contains(t.asText()))
					tags.add(t.asText());

			record.setCollections(colIds);
			record.setAllTags(tags);
		}

		return record;
	}

	public static Collection hitToCollection(SearchHit hit) {
		JsonNode json = Json.parse(hit.getSourceAsString());
		Collection c = Json.fromJson(json, Collection.class);

		return c;
	}
}
