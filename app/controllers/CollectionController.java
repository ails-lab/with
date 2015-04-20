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


package controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import model.Collection;
import model.CollectionEntry;
import model.RecordLink;
import model.User;

import org.bson.types.ObjectId;
import org.mongodb.morphia.Key;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class CollectionController extends Controller {
	public static final ALogger log = Logger.of( CollectionController.class);




	/**
	 * Get collection metadata (title, descr, thumbnail)
	 */
	public static Result getCollection(String id) {
		ObjectNode result  = Json.newObject();

			ObjectId colId = new ObjectId(id);
			Collection c = null;
			try {
				c = DB.getCollectionDAO().getById(colId);
			} catch(Exception e) {
				log.error("Cannot retrieve metadata for the specified collection!", e);
				result.put("message", "Cannot retrieve metadata for the specified collection!");
				return internalServerError(result);
			}

			return ok(Json.toJson(c));

	}


	/**
	 * Action to delete a Collection from database.
	 * Json input, the collection dbId
	 * @return
	 */
	@With(UserLoggedIn.class)
	public static Result deleteCollection(String id) {
		ObjectNode result = Json.newObject();

			try {
				DB.getCollectionDAO().deleteById(new ObjectId(id));
			} catch(Exception e) {
				log.error("Collection not deleted!", e);
				result.put("message", "Could not delete collection from database!");
				return internalServerError(result);
			}
			result.put("message", "Collection deleted succesfully from database");
			return ok(result);
	}

	public static Result editCollection(String id) {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

		Collection collection = Json.fromJson(json, Collection.class);
		if(DB.getCollectionDAO().makePermanent(collection) == null) {
			result.put("message", "Cannot save collection to database!");
			return internalServerError(result);
		}

		return ok(Json.toJson(collection));
	}

	/**
	 * Action to store a Collection to the database.
	 * Json input with collection fields
	 * @return
	 */
	//@With(UserLoggedIn.class)
	@BodyParser.Of(BodyParser.Json.class)
	public static Result createCollection() {

		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

		Key<Collection> colKey;
		Collection newCollection = Json.fromJson(json, Collection.class);
			if( (colKey = DB.getCollectionDAO().makePermanent(newCollection)) == null) {
				result.put("message", "Cannot save Collection to database");
				return internalServerError(result);
			}
		newCollection.setDbId(new ObjectId(colKey.getId().toString()));
		//result.put("message", "Collection succesfully stored!");
		//result.put("id", colKey.getId().toString());
		return ok(Json.toJson(newCollection));
	}


	/**
	 * list accessible collections
	 */
	public static Result list(	String username, String ownerId, String email,
								String access,
								int offset, int count) {

		List<Collection> userCollections;
		if(ownerId != null)
			userCollections = DB.getCollectionDAO()
						.getByOwner(new ObjectId(ownerId), offset, count);
		else {
			User u = null;
			if(email != null)
				u = DB.getUserDAO().getByEmail(email);
			if(username != null)
				u = DB.getUserDAO().getByUsername(username);

			if( u == null)
				return badRequest("User did not specified!");

			userCollections = DB.getCollectionDAO()
						.getByOwner(u.getDbId(), offset, count);
		}

		return ok(Json.toJson(userCollections));
	}

	/**
	 * Retrieve all fields from the first 20 items
	 * of all collections?
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result listFirstRecordsOfUserCollections() {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

		if(json.has("userId")) {
			String userId = json.get("userId").asText();

			//create a Map <collectionTitle, firstEntries>
			Map<String, List<RecordLink>> firstEntries =
					new HashMap<String, List<RecordLink>>();
			for(Collection c: DB.getCollectionDAO().getByOwner(new ObjectId(userId)))
				firstEntries.put(c.getTitle(), c.getFirstEntries());

			ObjectNode collections = Json.newObject();
			for(Entry<String, ?> entry: firstEntries.entrySet()) {
				ArrayNode firstRecords = Json.newObject().arrayNode();
				for(RecordLink recLink: (List<RecordLink>)entry.getValue()) {
					firstRecords.add(Json.toJson(recLink));
					//firstRecords.add(Serializer.recordLinkToJson(recLink));
				}
				collections.put(entry.getKey(), firstRecords);
			}
			result.put("userCollections", collections);
			return ok(result);
		} else {
			result.put("message", "Did not specify the user!");
			return badRequest(result);
		}
	}

	/**
	 * Adds a Record to a Collection
	 */
	//@With(UserLoggedIn.class)
	@BodyParser.Of(BodyParser.Json.class)
	public static Result addRecordToCollection(String collectionId) {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

		RecordLink rLink = null;
		CollectionEntry colEntry = null;
		String recordLinkId;
		if(json.has("recordlink_id")) {
			recordLinkId = json.get("recordlink_id").asText();
			rLink = DB.getRecordLinkDAO().getByDbId(new ObjectId(recordLinkId));
		} else {
			rLink = Json.fromJson(json, RecordLink.class);
			Key<RecordLink> rLinkKey = DB.getRecordLinkDAO().makePermanent(rLink);
			recordLinkId = rLinkKey.getId().toString();
			rLink.setDbId(new ObjectId(recordLinkId));
		}

		colEntry = new CollectionEntry();
		colEntry.setRecordLink(rLink);
		colEntry.setCollection(collectionId);

		try {
			DB.getCollectionEntryDAO().makePermanent(colEntry);
		} catch(Exception e) {
			log.error("Cannot save CollectionEntry to database!", e);
			result.put("message", "Cannot save CollectionEntry to database!");
			return internalServerError(result);
		}
		return ok(Json.toJson(rLink));
	}

	/**
	 * Remove a Record from a Collection
	 */
	//@With(UserLoggedIn.class)
	@BodyParser.Of(BodyParser.Json.class)
	public static Result removeRecordFromCollection(String collectionId, String rLinkId,
													int position, int version) {
		ObjectNode result = Json.newObject();

		ObjectId colId     = new ObjectId(collectionId);
		ObjectId recLinkId = new ObjectId(rLinkId);

		 if(DB.getCollectionEntryDAO().deleteByCollectionRecLinkId(recLinkId, colId) == 0 ) {
			 result.put("message", "Cannot delete CollectionEntry!");
			 return internalServerError(result);
		 }

		result.put("message", "RecordLink succesfully removed from Collection with id: " + colId.toString());
		return ok(result);

	}

	/**
	 * List all Records from a Collection
	 * using a start item and a page size
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result listCollectionRecords(String collectionId, int start, int count) {

			ObjectId colId = new ObjectId(collectionId);

			List<CollectionEntry> entries =
				DB.getCollectionEntryDAO()
						.getByCollectionOffsetCount(colId, start, count);
			ArrayNode records = Json.newObject().arrayNode();
			for(CollectionEntry e: entries) {
				records.add(Json.toJson(e.getRecordLink()));
			}

			return ok(records);
	}

	public static Result download(String id) {
		return null;
	}
}

