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


package utils;

import java.io.IOException;
import java.util.ArrayList;

import model.Collection;
import model.RecordLink;
import model.User;

import org.apache.commons.codec.binary.Base64;
import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class Serializer {
	public static final ALogger log = Logger.of( Serializer.class);



	public static Collection jsonToCollectionObject(JsonNode json) throws Exception {
		Collection collection = new Collection();

		try {
			if(json.has("description"))
				collection.setDescription(json.get("description").asText());
			if(json.has("title"))
				collection.setTitle(json.get("title").asText());
			if(json.has("public")) {
				String isPublic = json.get("public").asText();
				if(isPublic.equals("true"))
					collection.setPublic(true);
				else
					collection.setPublic(false);
			}

			if(json.has("ownerMail")) {
				String ownerMail = json.get("ownerMail").asText();
				User owner = DB.getUserDAO().getByEmail(ownerMail);
				collection.setOwner(owner);
			}

			if(json.has("firstEntries")) {
				ArrayNode firstEntriesIds = (ArrayNode)json.get("firstEntries");
				ArrayList<RecordLink> firstEntries = new ArrayList<RecordLink>();
				for(JsonNode idNode: firstEntriesIds) {
					ObjectId id = new ObjectId(idNode.get("id").asText());
					RecordLink rlink = DB.getRecordLinkDAO().getByDbId(id);
					firstEntries.add(rlink);
				}
				collection.setFirstEntries(firstEntries);
			}
		} catch(Exception e) {
			log.error("Cannot convert json to collection object!",e);
			throw new Exception("Cannot convert json to collection object!");
		}
		return collection;
	}

	public static JsonNode recordLinkToJson(RecordLink recLink) {
		ObjectNode json = Json.newObject();
		json.put("title", recLink.getTitle());
		json.put("type", recLink.getType());
		json.put("source", recLink.getSource());
		json.put("sourceId", recLink.getSourceId());
		json.put("sourceUrl", recLink.getSourceUrl());
		json.put("rights", recLink.getRights());

		return json;
	}

	public static JsonNode collectionToJson(Collection collection) {
		ObjectNode json = Json.newObject();

		// put title, description
		json.put("title", collection.getTitle());
		json.put("description", collection.getDescription());
		//json.put("date_created", collection.getCreated());
		//json.put("last_modified", collection.getLastModified());

		// put the capped array of first entries
		ArrayNode recordLinksArray = Json.newObject().arrayNode();
		for(RecordLink rlink: collection.getFirstEntries()) {
			ObjectNode jsonRecLink = Json.newObject();
			jsonRecLink.put("title", rlink.getTitle());
			jsonRecLink.put("description", rlink.getDescription());
			jsonRecLink.put("rights", rlink.getRights());
			jsonRecLink.put("source", rlink.getSource());
			jsonRecLink.put("sourceId", rlink.getSourceId());
			jsonRecLink.put("sourceUrl", rlink.getSourceUrl());
			jsonRecLink.put("type", rlink.getType());
			jsonRecLink.put("thumbnail_url", rlink.getThumbnailUrl());
			recordLinksArray.add(jsonRecLink);
		}
		json.put("first_entries", recordLinksArray);

		// put thumbnail metadata
		ObjectNode thumbnailMetadata = Json.newObject();
		thumbnailMetadata.put("data", Base64.encodeBase64(collection.retrieveThumbnail().getData()));
		thumbnailMetadata.put("type", collection.retrieveThumbnail().getType());
		thumbnailMetadata.put("mimeType", collection.retrieveThumbnail().getMimeType());
		thumbnailMetadata.put("duration", collection.retrieveThumbnail().getDuration());
		thumbnailMetadata.put("height", collection.retrieveThumbnail().getHeight());
		thumbnailMetadata.put("width", collection.retrieveThumbnail().getWidth());

		json.put("thumbnail", thumbnailMetadata);

		return json;
	}

	public static class ObjectIdSerializer extends JsonSerializer<Object> {
		@Override
		public void serialize(Object oid, JsonGenerator jsonGen, SerializerProvider provider)
				throws IOException,	JsonProcessingException {
					jsonGen.writeString(oid.toString());
		}

	}
}
