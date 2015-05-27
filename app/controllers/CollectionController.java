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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;

import javax.validation.ConstraintViolation;

import model.Collection;
import model.CollectionRecord;
import model.User;
import model.User.Access;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.data.validation.Validation;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessManager;
import utils.AccessManager.Action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;
import elastic.ElasticIndexer;
import espace.core.ISpaceSource;
import espace.core.ParallelAPICall;
import espace.core.RecordJSONMetadata;

public class CollectionController extends Controller {
	public static final ALogger log = Logger.of(CollectionController.class);

	/**
	 * Get collection metadata (title, descr, thumbnail)
	 */
	public static Result getCollection(String collectionId) {

		ObjectNode result = Json.newObject();
		Collection c = null;
		User collectionOwner = null;
		List<String> userIds = effectiveUserIds();
		
		try {
			c = DB.getCollectionDAO().getById(new ObjectId(collectionId));
			if( c== null ) {
				result.put("error",
						"Cannot retrieve metadata for the specified collection!");	
				return internalServerError(result);
			}
			
			if (!AccessManager.checkAccess(c.getRights(), userIds, Action.READ)
					&& !c.getIsPublic()) {
				result.put("error",
						"User does not have read-access for the collection");
				return forbidden(result);
			}
			collectionOwner = DB.getUserDAO().getById(c.getOwnerId(), null);
		} catch (Exception e) {
			log.error(
					"Cannot retrieve metadata for the specified collection or user!",
					e);
			return internalServerError( );
		}
		Access maxAccess;

		if ((maxAccess = AccessManager.getMaxAccess(c.getRights(), userIds)) == Access.NONE) {
			maxAccess = Access.READ;
		}

		// check itemCount
		int itemCount;
		if ((itemCount = (int) DB.getCollectionRecordDAO().getItemCount(
				new ObjectId(collectionId))) != c.getItemCount())
			c.setItemCount(itemCount);
		result = (ObjectNode) Json.toJson(c);
		result.put("owner", collectionOwner.getUsername());
		result.put("access", maxAccess.toString());
		return ok(result);
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
		Collection c = null;
		c = DB.getCollectionDAO().getById(new ObjectId(id));
		try {
			if (DB.getCollectionDAO().removeById(new ObjectId(id)) != 1) {
				result.put("error", "Cannot delete collection from database!");
				return badRequest(result);
			}
		} catch (Exception e) {
			return internalServerError(e.toString());
		}
		if (session().get("effectiveUserIds") == null) {
			result.put("error",
					"User does not have permission to delete the collection");
			return forbidden(result);
		}
		List<String> userIds = Arrays.asList(session().get("effectiveUserIds")
				.split(","));
		if (!AccessManager.checkAccess(c.getRights(), userIds, Action.DELETE)) {
			result.put("error",
					"User does not have permission to delete the collection");
			return forbidden(result);
		}
		result.put("message", "Collection deleted succesfully from database");
		return ok(result);
	}

	/**
	 * still needs to be implemented in a better way
	 *
	 * @param id
	 * @return
	 * @throws IOException
	 * @throws JsonProcessingException
	 */
	public static Result editCollection(String id)
			throws JsonProcessingException, IOException {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		if (json == null) {
			result.put("message", "Invalid json!");
			return badRequest(result);
		}
		Collection oldVersion = DB.getCollectionDAO().getById(new ObjectId(id));

		if (session().get("effectiveUserIds") == null) {
			result.put("error",
					"User does not have permission to edit the collection");
			return forbidden(result);
		}
		List<String> userIds = Arrays.asList(session().get("effectiveUserIds")
				.split(","));
		if (!AccessManager.checkAccess(oldVersion.getRights(), userIds,
				Action.EDIT)) {
			result.put("error",
					"User does not have permission to edit the collection");
			return forbidden(result);
		}
		Access maxAccess = AccessManager.getMaxAccess(oldVersion.getRights(),
				userIds);
		String oldTitle = oldVersion.getTitle();
		ObjectMapper objectMapper = new ObjectMapper();
		ObjectReader updater = objectMapper.readerForUpdating(oldVersion);
		Collection newVersion = updater.readValue(json);
		newVersion.setLastModified(new Date());

		Set<ConstraintViolation<Collection>> violations = Validation
				.getValidator().validate(newVersion);
		for (ConstraintViolation<Collection> cv : violations) {
			result.put("message",
					"[" + cv.getPropertyPath() + "] " + cv.getMessage());
		}
		if (!violations.isEmpty()) {
			return badRequest(result);
		}
		if ((DB.getCollectionDAO().getByOwnerAndTitle(newVersion.getOwnerId(),
				newVersion.getTitle()) != null)
				&& (!oldTitle.equals(newVersion.getTitle()))) {
			result.put("message",
					"Title already exists! Please specify another title.");
			return internalServerError(result);
		}
		if (DB.getCollectionDAO().makePermanent(newVersion) == null) {
			log.error("Cannot save collection to database!");
			result.put("message", "Cannot save collection to database!");
			return internalServerError(result);
		}
		String m = DB.getJson(newVersion);
		result.put("access", maxAccess.toString());
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
		if (session().get("effectiveUserIds") == null) {
			result.put("error", "Must specify user for the collection");
			return forbidden(result);
		}
		List<String> userIds = Arrays.asList(session().get("effectiveUserIds")
				.split(","));
		String userId = userIds.get(0);
		Collection newCollection = Json.fromJson(json, Collection.class);
		newCollection.setCreated(new Date());
		newCollection.setLastModified(new Date());
		newCollection.setOwnerId(new ObjectId(userId));

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
		User owner = DB.getUserDAO().get(newCollection.getOwnerId());
		owner.getCollectionMetadata().add(newCollection.collectMetadata());
		DB.getUserDAO().makePermanent(owner);
		ObjectNode c = (ObjectNode) Json.toJson(newCollection);
		c.put("access", Access.OWN.toString());
		User user = DB.getUserDAO().getById(newCollection.getOwnerId(),
				new ArrayList<String>(Arrays.asList("username")));
		c.put("owner", user.getUsername());
		// result.put("message", "Collection succesfully stored!");
		// result.put("id", colKey.getId().toString());
		return ok(c);
	}

	/**
	 * list accessible collections
	 */
	@SuppressWarnings("serial")
	public static Result list(String filterByUser, String filterByUserId,
			String filterByEmail, String access, int offset, int count) {

		ArrayNode result = Json.newObject().arrayNode();
		List<Collection> userCollections;

		ObjectId ownerId = null;
		List<String> userIds = effectiveUserIds();
		
		if (filterByUserId != null) {
			ownerId = new ObjectId(filterByUserId);
		} else if (filterByUser != null) {
			ownerId = DB.getUserDAO().getByUsername(filterByUser).getDbId();
		} else if (filterByEmail != null) {
			ownerId = DB.getUserDAO().getByEmail(filterByEmail).getDbId();
		}

		if (userIds.isEmpty() ) {
			// return all public collections
			if (ownerId == null) {
				userCollections = DB.getCollectionDAO()
						.getPublic(offset, count);
			} else {
				userCollections = DB.getCollectionDAO().getPublicFiltered(
						ownerId, offset, count);
			}
			for (Collection collection : userCollections) {
				ObjectNode c = (ObjectNode) Json.toJson(collection);
				c.put("access", Access.READ.toString());
				result.add(c);
			}
			return ok(result);
		}
		// ok, so there is a user id effective
		String userId = userIds.get(0);
		switch (access) {
		case "read":
			if (ownerId == null) {
				userCollections = DB.getCollectionDAO().getByReadAccess(
						new ObjectId(userId), offset, count);
			} else {
				userCollections = DB.getCollectionDAO()
						.getByReadAccessFiltered(new ObjectId(userId), ownerId,
								offset, count);

			}
			break;
		case "write":
			if (ownerId == null) {
				userCollections = DB.getCollectionDAO().getByWriteAccess(
						new ObjectId(userId), offset, count);
			} else {
				userCollections = DB.getCollectionDAO()
						.getByWriteAccessFiltered(new ObjectId(userId),
								ownerId, offset, count);

			}
			break;
		case "owned":
			userCollections = DB.getCollectionDAO().getByOwner(
					new ObjectId(userId), offset, count);
			break;
		default:
			userCollections = DB.getCollectionDAO().getByOwner(
					new ObjectId(userId), offset, count);
			break;
		}
		Collections.sort(userCollections, new Comparator<Collection>() {
			public int compare(Collection c1, Collection c2) {
				return -c1.getCreated().compareTo(c2.getCreated());
			}
		});
		for (Collection collection : userCollections) {
			ObjectNode c = (ObjectNode) Json.toJson(collection);
			Access maxAccess = AccessManager.getMaxAccess(
					collection.getRights(), userIds );
			if (maxAccess.equals(Access.NONE)) {
				maxAccess = Access.READ;
			}
			c.put("access", maxAccess.toString());
			User user = DB.getUserDAO().getById(collection.getOwnerId(),
					new ArrayList<String>(Arrays.asList("username")));
			c.put("owner", user.getUsername());
			result.add(c);
		}
		return ok(result);
	}

	public static Result listShared(String filterByUser, String filterByUserId,
			String filterByEmail, int offset, int count) {

		ArrayNode result = Json.newObject().arrayNode();
		List<Collection> userCollections;

		ObjectId ownerId = null;
		if (filterByUserId != null) {
			ownerId = new ObjectId(filterByUserId);
		} else if (filterByUser != null) {
			ownerId = DB.getUserDAO().getByUsername(filterByUser).getDbId();
		} else if (filterByEmail != null) {
			ownerId = DB.getUserDAO().getByEmail(filterByEmail).getDbId();
		}

		if (session().get("effectiveUserIds") == null) {
			return forbidden(Json
					.parse("\"error\", \"Must specify user for the collection\""));
		}
		// TODO: must expand to support user groups
		String[] userIds = session().get("effectiveUserIds").split(",");
		String userId = userIds[0];
		if (ownerId == null) {
			userCollections = DB.getCollectionDAO().getShared(
					new ObjectId(userId), offset, count);
		} else {
			userCollections = DB.getCollectionDAO().getSharedFiltered(
					new ObjectId(userId), ownerId, offset, count);

		}
		Collections.sort(userCollections, new Comparator<Collection>() {
			public int compare(Collection c1, Collection c2) {
				return -c1.getCreated().compareTo(c2.getCreated());
			}
		});
		for (Collection collection : userCollections) {
			ObjectNode c = (ObjectNode) Json.toJson(collection);
			Access maxAccess = AccessManager.getMaxAccess(
					collection.getRights(),
					new ArrayList<String>(Arrays.asList(userId)));
			if (maxAccess.equals(Access.NONE)) {
				maxAccess = Access.READ;
			}
			c.put("access", maxAccess.toString());
			User user = DB.getUserDAO().getById(collection.getOwnerId(),
					new ArrayList<String>(Arrays.asList("username")));
			c.put("owner", user.getUsername());
			result.add(c);
		}
		return ok(result);
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
		if (session().get("effectiveUserIds") == null) {
			result.put("error", "User not specified");
			return forbidden(result);
		}
		if (json.has("ownerId")) {
			String userId = json.get("ownerId").asText();
			List<String> userIds = Arrays.asList(session().get(
					"effectiveUserIds").split(","));
			String sessionId = userIds.get(0);
			if (!userId.equals(sessionId)) {
				result.put("error",
						"User does not have access to requested records");
				return forbidden(result);
			}
			// create a Map <collectionTitle, firstEntries>
			Map<String, List<CollectionRecord>> firstEntries = new HashMap<String, List<CollectionRecord>>();
			for (Collection c : DB.getCollectionDAO().getByOwner(
					new ObjectId(userId)))
				firstEntries.put(c.getTitle(), c.getFirstEntries());

			ObjectNode collections = Json.newObject();
			for (Entry<String, ?> entry : firstEntries.entrySet())
				collections.put(entry.getKey(), Json.toJson(entry.getValue()));

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

		if (session().get("effectiveUserIds") == null) {
			result.put("error",
					"User does not have permission to edit the collection");
			return forbidden(result);
		}
		Collection c = DB.getCollectionDAO()
				.getById(new ObjectId(collectionId));
		List<String> userIds = Arrays.asList(session().get("effectiveUserIds")
				.split(","));
		if (!AccessManager.checkAccess(c.getRights(), userIds, Action.EDIT)) {
			result.put("error",
					"User does not have permission to edit the collection");
			return forbidden(result);
		}
		CollectionRecord record = null;
		String recordLinkId;
		if (json.has("recordlink_id")) {
			recordLinkId = json.get("recordlink_id").asText();
			record = DB.getCollectionRecordDAO().getById(
					new ObjectId(recordLinkId));
			record.setDbId(null);
			record.setCollectionId(new ObjectId(collectionId));
			DB.getCollectionRecordDAO().makePermanent(record);
			return addRecordToFirstEntries(record, result, collectionId);
		} else {
			record = Json.fromJson(json, CollectionRecord.class);
			String sourceId = record.getSourceId();
			String source = record.getSource();
			record.setCollectionId(new ObjectId(collectionId));
			Set<ConstraintViolation<CollectionRecord>> violations = Validation
					.getValidator().validate(record);
			for (ConstraintViolation<CollectionRecord> cv : violations) {
				result.put("message",
						"[" + cv.getPropertyPath() + "] " + cv.getMessage());
				return badRequest(result);
			}
			DB.getCollectionRecordDAO().makePermanent(record);
			// record in first entries does not contain the content metadata
			Status status = addRecordToFirstEntries(record, result,
					collectionId);
			JsonNode content = json.get("content");
			if (content != null) {
				Iterator<String> contentTypes = content.fieldNames();
				while (contentTypes.hasNext()) {
					String contentType = contentTypes.next();
					String contentMetadata = content.get(contentType).asText();
					record.getContent().put(contentType, contentMetadata);
				}
				DB.getCollectionRecordDAO().makePermanent(record);
				ElasticIndexer indexer = new ElasticIndexer(record);
				indexer.index();
			} else
				addContentToRecord(record.getDbId(), source, sourceId);
			return status;
		}

	}

	private static Status addRecordToFirstEntries(CollectionRecord record,
			ObjectNode result, String collectionId) {
		Collection collection = DB.getCollectionDAO().getById(
				new ObjectId(collectionId));
		collection.itemCountIncr();
		collection.setLastModified(new Date());
		if (collection.getFirstEntries().size() < 20)
			collection.getFirstEntries().add(record);
		DB.getCollectionDAO().makePermanent(collection);
		if (record.getDbId() == null) {
			result.put("message", "Cannot save RecordLink to database!");
			return internalServerError(result);
		} else
			return ok(Json.toJson(record));
	}

	private static void addContentToRecord(ObjectId recordId, String source,
			String sourceId) {
		BiFunction<CollectionRecord, String, Boolean> methodQuery = (
				CollectionRecord record, String sourceClassName) -> {
			try {
				// TODO: create a Mint source class with respective methods
				Class<?> sourceClass = Class.forName(sourceClassName);
				ISpaceSource s = (ISpaceSource) sourceClass.newInstance();
				ArrayList<RecordJSONMetadata> recordsData = s
						.getRecordFromSource(sourceId);
				for (RecordJSONMetadata data : recordsData) {
					record.getContent().put(data.getFormat(),
							data.getJsonContent());
				}
				DB.getCollectionRecordDAO().makePermanent(record);
				ElasticIndexer indexer = new ElasticIndexer(record);
				indexer.index();
				return true;
			} catch (ClassNotFoundException e) {
				// my class isn't there!
				return false;
			} catch (InstantiationException e) {
				return false;
			} catch (IllegalAccessException e) {
				return false;
			}
		};
		CollectionRecord record = DB.getCollectionRecordDAO().getById(recordId);
		String sourceClassName = "espace.core.sources." + source
				+ "SpaceSource";
		ParallelAPICall.createPromise(methodQuery, record, sourceClassName);
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
		if (session().get("effectiveUserIds") == null) {
			result.put("error",
					"User does not have permission to edit the collection");
			return forbidden(result);
		}
		List<String> userIds = Arrays.asList(session().get("effectiveUserIds")
				.split(","));
		if (!AccessManager.checkAccess(collection.getRights(), userIds,
				Action.EDIT)) {
			result.put("error",
					"User does not have permission to edit the collection");
			return forbidden(result);
		}
		List<CollectionRecord> records = collection.getFirstEntries();
		CollectionRecord temp = null;
		for (CollectionRecord r : records) {
			if (recordId.equals(r.getDbId().toString())) {
				temp = r;
				break;
			}
		}
		if (temp != null)
			records.remove(temp);
		collection.setLastModified(new Date());
		collection.itemCountDec();
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
	public static Result listCollectionRecords(String collectionId,
			String format, int start, int count) {
		ObjectNode result = Json.newObject();

		ObjectId colId = new ObjectId(collectionId);
		Collection collection = DB.getCollectionDAO().getById(colId);
		if ((session().get("effectiveUserIds") == null)
				&& (!collection.getIsPublic())) {
			result.put("error",
					"User does not have read-access to the collection");
			return forbidden(result);
		}
		List<String> userIds = Arrays.asList(session().get("effectiveUserIds")
				.split(","));
		if (!AccessManager.checkAccess(collection.getRights(), userIds,
				Action.READ) && (!collection.getIsPublic())) {
			result.put("error",
					"User does not have permission to edit the collection");
			return forbidden(result);
		}
		List<CollectionRecord> records = DB.getCollectionRecordDAO()
				.getByCollectionOffsetCount(colId, start, count);
		if (records == null) {
			result.put("message", "Cannot retrieve records from database!");
			return internalServerError(result);
		}
		ArrayNode recordsList = Json.newObject().arrayNode();
		for (CollectionRecord e : records) {
			if (format.equals("all")) {
				recordsList.add(Json.toJson(e.getContent()));
			} else if (!format.equals("default")) {
				recordsList.add(e.getContent().get(format));
			} else {
				e.getContent().clear();
				recordsList.add(Json.toJson(e));
			}
		}
		return ok(recordsList);
	}

	public static Result download(String id) {
		return null;
	}
	
	private static List<String> effectiveUserIds() {
		String effectiveUserIds = session().get("effectiveUserIds");
		if( effectiveUserIds == null ) effectiveUserIds = "";
		List<String> userIds = new ArrayList<String>();
		for( String ui: effectiveUserIds.split(",")) {
			if( ui.trim().length() > 0 ) userIds.add(ui );
		}
		return userIds;
	}
}
