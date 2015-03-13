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

import java.util.ArrayList;

import model.Collection;
import model.RecordLink;
import model.User;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class Serializer {
	public static final ALogger log = Logger.of( Serializer.class);


	public static Collection jsonToCollectionObject(JsonNode json) throws Exception {
		Collection collection = new Collection();

		try {
			if(json.has("description"))
				collection.setDescription(json.get("description").toString());
			if(json.has("title"))
				collection.setTitle(json.get("title").toString());
			if(json.has("public")) {
				String isPublic = json.get("public").toString();
				if(isPublic.equals("true"))
					collection.setPublic(true);
				else
					collection.setPublic(false);
			}
	
			if(json.has("ownerMail")) {
				String ownerMail = json.get("ownerMail").toString();
				User owner = DB.getUserDAO().getByEmail(ownerMail);
				collection.setOwner(owner);
			}
	
			if(json.has("firstEntries")) {
				ArrayNode firstEntriesIds = (ArrayNode)json.get("firstEntries");
				ArrayList<RecordLink> firstEntries = new ArrayList<RecordLink>();
				for(JsonNode idNode: firstEntriesIds) {
					String id = idNode.get("id").toString();
					RecordLink rlink = DB.getRecordLinkDAO().getByDbId(id);
					firstEntries.add(rlink);
				}
				collection.setFirstEntries(firstEntries);
			}
		} catch(Exception e) {
			log.error("Cannot convert json to collection object!");
			throw new Exception("Cannot convert json to collection object!");
		}
		
		return collection;
	}


	public static JsonNode collectionToJson(Collection collection) {
		ObjectNode json = Json.newObject();
		json.put("title", collection.getTitle());
		json.put("description", collection.getDescription());
		json.put("date_created", collection.getCreated().toString());
		json.put("last_modified", collection.getLastModified().toString());
		
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
		
		return null;
	}
}
