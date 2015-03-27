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
import model.Media;
import model.RecordLink;

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
	 * get recordLink thumbnail
	 */
	public static Result getRecordLinkThumbnail(String rlinkId) {
		RecordLink r = null;
		Media thumbnail = null;
		try {
			r = DB.getRecordLinkDAO().getByDbId(new ObjectId(rlinkId));
			thumbnail = r.retrieveThumbnail();
		} catch(Exception e) {
			log.error("Cannot retrieve record link or media document from database", e);
			return internalServerError("Cannot retrieve record link or media document from database");
		}
		return ok(thumbnail.getData()).as(thumbnail.getMimeType());
	}

	/**
	 * Return the thumbnail (in raw bytes) of a collection
	 * @param colId
	 * @return
	 */
	public static Result getCollectionThumbnail(String colId) {

		Collection c = null;
		Media thumbnail = null;
		try {
			c = DB.getCollectionDAO().getById(new ObjectId(colId));
			thumbnail = c.retrieveThumbnail();
		} catch(Exception e) {
			log.error("Cannot retrieve collection or media document from database", e);
			return internalServerError("Cannot retrieve collection or media document from database");
		}
		return ok(thumbnail.getData()).as(thumbnail.getMimeType());
	}

	/**
	 * Get collection metadata (title, descr, thumbnail)
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result getCollection() {
		JsonNode json = request().body().asJson();
		ObjectNode result  = Json.newObject();

		if(json.has("collection_id")) {
			ObjectId colId = new ObjectId(json.get("collection_id").asText());
			Collection c = null;
			try {
				c = DB.getCollectionDAO().getById(colId);
			} catch(Exception e) {
				log.error("Cannot retrieve metadata for the specified collection!", e);
				result.put("message", "Cannot retrieve metadata for the specified collection!");
				return internalServerError(result);
			}
			result.put("collection", Json.toJson(c));
			result.put("message", "Collection retrieved succesfully!");
			return ok(result);
		} else {
			log.error("Collection id was not specified!");
			result.put("message", "Collection id was not specified!");
			return badRequest(result);
		}
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
		//DBObject jsonObj = DB.getMorphia().toDBObject(json);
		ObjectNode result = Json.newObject();
		Key<Collection> colKey = null;
		try {
			//Collection newCollection = Serializer.jsonToCollectionObject(json);
			Collection newCollection = Json.fromJson(json, Collection.class);
			colKey = DB.getCollectionDAO().save(newCollection);
		} catch (Exception e) {
			log.error("Cannot save Collection to database", e);
			result.put("message", "Cannot save Collection to database");
			return internalServerError(result);
		}
		result.put("message", "Collection succesfully stored!");
		result.put("id", colKey.getId().toString());
		return ok(result);
	}


	/**
	 * Action to delete a Collection from database.
	 * Json input, the collection dbId
	 * @return
	 */
	@With(UserLoggedIn.class)
	@BodyParser.Of(BodyParser.Json.class)
	public static Result deleteCollection() {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

		if(json.has("dbId")) {
			String id = json.get("dbId").asText();
			try {
				DB.getCollectionDAO().deleteById(new ObjectId(id));
			} catch(Exception e) {
				log.error("Collection not deleted!", e);
				result.put("message", "Could not delete collection from database!");
				return internalServerError(result);
			}
			result.put("message", "Collection deleted succesfully from database");
			return ok(result);
		} else {
			result.put("message", "Did not specify collection dbId field!");
			return badRequest(result);
		}
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
	public static Result addRecordToCollection() {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

		RecordLink rLink = null;
		CollectionEntry colEntry = null;
		if(json.has("recordlink_id") && json.has("collection_id")) {
			String recordLinkId = json.get("recordlink_id").asText();
			String colId = json.get("collection_id").asText();

			rLink = DB.getRecordLinkDAO().getByDbId(new ObjectId(recordLinkId));
			colEntry = new CollectionEntry();
			colEntry.setRecordLink(rLink);
			colEntry.setCollection(colId);

			try {
				DB.getCollectionEntryDAO().save(colEntry);
				result.put("collection_id", colId);
				result.put("message", "CollectionEntry saved succesfully to database!");
				return ok(result);
			} catch(Exception e) {
				log.error("Cannot save CollectionEntry to database!", e);
				result.put("message", "Cannot save CollectionEntry to database!");
				return internalServerError(result);
			}
		} else {
			result.put("message",
					"Cannot retrieve recordLink or collection from db, id is missing!");
			return badRequest(result);
		}

	}

	/**
	 * Remove a Record from a Collection
	 */
	//@With(UserLoggedIn.class)
	@BodyParser.Of(BodyParser.Json.class)
	public static Result removeRecordFromCollection() {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

		if(json.has("recordlink_id") && json.has("collection_id")) {
			ObjectId recLinkId = new ObjectId(json.get("recordlink_id").asText());
			ObjectId colId = new ObjectId(json.get("collection_id").asText());
			try {
				 if(DB.getCollectionEntryDAO().deleteByCollectionRecLinkId(recLinkId, colId) == 0 ) {
					 result.put("message", "Cannot delete CollectionEntry!");
					 return internalServerError(result);
				 }
			} catch(Exception e) {

			}
			result.put("message", "RecordLink succesfully removed from Collection with id: " + colId.toString());
			return ok(result);
		} else {
			result.put("message",
					"Cannot retrieve recordLink or collection from db, id is missing!");
			return badRequest(result);
		}
	}

	/**
	 * List all Records from a Collection
	 * using a start item and a page size
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result listCollectionRecords() {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

		if(json.has("collection_id")) {
			ObjectId colId = new ObjectId(json.get("collection_id").asText());
			int start =
					(json.has("start"))?json.get("start").asInt():1;
			int count =
					(json.has("count"))?json.get("count").asInt():20;

			List<CollectionEntry> entries =
				DB.getCollectionEntryDAO()
				.getByCollectionOffsetCount(colId, start, count);
			ArrayNode records = Json.newObject().arrayNode();
			for(CollectionEntry e: entries) {
				records.add(Json.toJson(e.getRecordLink()));
				//records.add(Serializer.recordLinkToJson(e.getRecordLink()));
			}
			result.put("records", records);
			return ok(result);
		} else {
			result.put("message", "Cannot list records, collection id is missing!");
			return badRequest(result);
		}
	}

}

