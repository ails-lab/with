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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import db.DB;

public class Serializer {
	public static final ALogger log = Logger.of( Serializer.class);


	public static Collection jsonToCollectionObject(JsonNode json) {
		Collection collection = new Collection();

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

		if(collection == null)
			log.debug("Null collection! No database storage!");
		return collection;
	}
}
