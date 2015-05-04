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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.validation.ConstraintViolation;

import model.Collection;
import model.CollectionRecord;
import model.User;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.data.validation.Validation;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;
import espace.core.ISpaceSource;
import espace.core.RecordJSONMetadata;

public class CollectionController extends Controller {
	public static final ALogger log = Logger.of(CollectionController.class);

	/**
	 * Get collection metadata (title, descr, thumbnail)
	 */
	public static Result getCollection(String id) {
		ObjectNode result = Json.newObject();

		ObjectId colId = new ObjectId(id);
		Collection c = null;
		try {
			c = DB.getCollectionDAO().getById(colId);
		} catch (Exception e) {
			log.error("Cannot retrieve metadata for the specified collection!",
					e);
			result.put("message",
					"Cannot retrieve metadata for the specified collection!");
			return internalServerError(result);
		}

		return ok(Json.toJson(c));

	}

	/**
	 * Action to delete a Collection from database. Json input, the collection
	 * dbId
	 *
	 * @return
	 */
	// @With(UserLoggedIn.class)
	public static Result deleteCollection(String id) {
		ObjectNode result = Json.newObject();

		try {
			DB.getCollectionDAO().deleteById(new ObjectId(id));
		} catch (Exception e) {
			log.error("Collection not deleted!", e);
			result.put("message", "Could not delete collection from database!");
			return internalServerError(result);
		}
		result.put("message", "Collection deleted succesfully from database");
		return ok(result);
	}

	/**
	 * still needs to be implemented in a better way
	 *
	 * @param id
	 * @return
	 */
	public static Result editCollection(String id) {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

		if (json == null) {
			result.put("message", "Invalid json!");
			return badRequest(result);
		}

		Collection oldVersion = DB.getCollectionDAO().getById(new ObjectId(id));
		Collection newVersion = Json.fromJson(json, Collection.class);
		newVersion.setFirstEntries(oldVersion.getFirstEntries());
		newVersion.setDbId(new ObjectId(id));

		Set<ConstraintViolation<Collection>> violations = Validation
				.getValidator().validate(newVersion);
		for (ConstraintViolation<Collection> cv : violations) {
			result.put("message",
					"[" + cv.getPropertyPath() + "] " + cv.getMessage());
			return badRequest(result);
		}


		if (DB.getCollectionDAO().getByOwnerAndTitle(newVersion.getOwnerId(),
				newVersion.getTitle()) != null) {
			result.put("message",
					"Title already exists! Please specify another title.");
			return internalServerError(result);
		}
		if (DB.getCollectionDAO().makePermanent(newVersion) == null) {


			log.error("Cannot save collection to database!");
			result.put("message", "Cannot save collection to database!");
			return internalServerError(result);
		}

		return ok(Json.toJson(newVersion));
	}

	/**
	 * Action to store a Collection to the database. Json input with collection
	 * fields
	 *
	 * @return
	 */
	// @With(UserLoggedIn.class)
	public static Result createCollection() {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

		if (json == null) {
			result.put("message", "Invalid json!");
			return badRequest(result);
		}

		Collection newCollection = Json.fromJson(json, Collection.class);

		Set<ConstraintViolation<Collection>> violations = Validation
				.getValidator().validate(newCollection);
		for (ConstraintViolation<Collection> cv : violations) {
			result.put("message",
					"[" + cv.getPropertyPath() + "] " + cv.getMessage());
			return badRequest(result);
		}


		if (DB.getCollectionDAO().getByOwnerAndTitle(
				newCollection.getOwnerId(), newCollection.getTitle()) != null) {
			result.put("message",
					"Title already exists! Please specify another title.");
			return internalServerError(result);
		}
		if (DB.getCollectionDAO().makePermanent(newCollection) == null) {


			result.put("message", "Cannot save Collection to database");
			return internalServerError(result);
		}
		// result.put("message", "Collection succesfully stored!");
		// result.put("id", colKey.getId().toString());
		return ok(Json.toJson(newCollection));
	}

	/**
	 * list accessible collections
	 */
	public static Result list(String username, String ownerId, String email,
			String access, int offset, int count) {

		ObjectNode result = Json.newObject();
		List<Collection> userCollections;
		if (ownerId != null)
			userCollections = DB.getCollectionDAO().getByOwner(
					new ObjectId(ownerId), offset, count);
		else {
			User u = null;
			if (email != null)
				u = DB.getUserDAO().getByEmail(email);
			if (username != null)
				u = DB.getUserDAO().getByUsername(username);

			if (u == null) {
				result.put("message", "User did not specified!");
				return badRequest(result);
			}

			userCollections = DB.getCollectionDAO().getByOwner(u.getDbId(),
					offset, count);
		}

		return ok(Json.toJson(userCollections));
	}

	/**
	 * Retrieve all fields from the first 20 items of all collections?
	 */
	public static Result listFirstRecordsOfUserCollections() {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

		if (json == null) {
			result.put("message", "Invalid json!");
			return badRequest(result);
		}

		if (json.has("ownerId")) {
			String userId = json.get("ownerId").asText();

			// create a Map <collectionTitle, firstEntries>
			Map<String, List<CollectionRecord>> firstEntries = new HashMap<String, List<CollectionRecord>>();
			for (Collection c : DB.getCollectionDAO().getByOwner(
					new ObjectId(userId)))
				firstEntries.put(c.getTitle(), c.getFirstEntries());

			ObjectNode collections = Json.newObject();
			for (Entry<String, ?> entry : firstEntries.entrySet()) {
				/*
				 * ArrayNode firstRecords = Json.newObject().arrayNode();
				 * for(CollectionRecord recLink:
				 * (List<CollectionRecord>)entry.getValue()) {
				 * firstRecords.add(Json.toJson(recLink));
				 * //firstRecords.add(Serializer.recordLinkToJson(recLink)); }
				 */
				collections.put(entry.getKey(), Json.toJson(entry.getValue()));
			}
			result.put("userCollections", collections);
			return ok(result);
		} else {
			result.put("message", "Did not specify the owner!");
			return badRequest(result);
		}
	}

	/**
	 * Adds a Record to a Collection
	 *
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	// @With(UserLoggedIn.class)
	// TODO: catch the exceptions
	public static Result addRecordToCollection(String collectionId)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

		if (json == null) {
			result.put("message", "Invalid json!");
			return badRequest(result);
		}

		CollectionRecord record = null;
		String recordLinkId;
		if (json.has("recordlink_id")) {
			recordLinkId = json.get("recordlink_id").asText();
			record = DB.getCollectionRecordDAO().getById(new ObjectId(recordLinkId));
			record.setDbId(null);
			record.setCollectionId(new ObjectId(collectionId));
		} else {
			record = Json.fromJson(json, CollectionRecord.class);
			String sourceId = record.getSourceId();
			String source = record.getSource();

			String sourceClassName = "espace.core.sources." + source
					+ "SpaceSource";
			Class<?> sourceClass = Class.forName(sourceClassName);
			ISpaceSource s = (ISpaceSource) sourceClass.newInstance();

			ArrayList<RecordJSONMetadata> recordsData = s.getRecordFromSource(sourceId);
			for(RecordJSONMetadata data : recordsData) {
				record.getContent().put(data.getFormat(), data.getJsonContent());

			}
			Set<ConstraintViolation<CollectionRecord>> violations = Validation
					.getValidator().validate(record);
			for (ConstraintViolation<CollectionRecord> cv : violations) {
				result.put("message",
						"[" + cv.getPropertyPath() + "] " + cv.getMessage());
				return badRequest(result);
			}
		}
		DB.getCollectionRecordDAO().makePermanent(record);

		Collection collection = DB.getCollectionDAO().getById(
				new ObjectId(collectionId));
		if (collection.getFirstEntries().size() < 20)
			collection.getFirstEntries().add(record);
		DB.getCollectionDAO().makePermanent(collection);

		if (record.getDbId() == null) {


			result.put("message", "Cannot save RecordLink to database!");
			return internalServerError(result);
		}

		return ok(Json.toJson(record));
	}

	/**
	 * Remove a Record from a Collection
	 */
	// @With(UserLoggedIn.class)
	public static Result removeRecordFromCollection(String collectionId,
			String recordId, int position, int version) {
		ObjectNode result = Json.newObject();

		// Remove record from collection.firstEntries
		Collection collection = DB.getCollectionDAO().getById(
				new ObjectId(collectionId));
		List<CollectionRecord> records = collection.getFirstEntries();
		CollectionRecord temp = null;
		for (CollectionRecord r : records) {
			if (r.getDbId().toString().equals(recordId))
				temp = r;
			break;
		}
		if (temp != null)
			records.remove(temp);
		DB.getCollectionDAO().makePermanent(collection);

		if (DB.getCollectionRecordDAO().deleteById(new ObjectId(recordId))
				.getN() == 0) {
			result.put("message", "Cannot delete CollectionEntry!");
			return internalServerError(result);
		}

		result.put("message",
				"RecordLink succesfully removed from Collection with id: "
						+ collectionId.toString());
		return ok(result);

	}

	/**
	 * List all Records from a Collection using a start item and a page size
	 */
	public static Result listCollectionRecords(String collectionId, int start,
			int count) {
		ObjectNode result = Json.newObject();

		ObjectId colId = new ObjectId(collectionId);
		List<CollectionRecord> records = DB.getCollectionRecordDAO()
				.getByCollectionOffsetCount(colId, start, count);
		if (records == null) {
			result.put("message", "Cannot retrieve records from database!");
			return internalServerError(result);
		}
		ArrayNode recordsList = Json.newObject().arrayNode();
		for (CollectionRecord e : records) {
			recordsList.add(Json.toJson(e));
		}

		return ok(recordsList);
	}

	public static Result download(String id) {
		return null;
	}
}
