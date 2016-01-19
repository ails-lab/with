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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;

import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.WithMediaRights;
import model.basicDataTypes.ProvenanceInfo.Sources;
import model.basicDataTypes.ProvenanceInfo;
import model.resources.CollectionObject;
import model.resources.CollectionObject.CollectionAdmin;
import model.resources.RecordResource;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Option;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import sources.core.ISpaceSource;
import sources.core.ParallelAPICall;
import sources.core.RecordJSONMetadata;
import utils.AccessManager;
import utils.AccessManager.Action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

/**
 * @author mariaral
 *
 */
@SuppressWarnings("rawtypes")
public class WithResourceController extends Controller {

	public static final ALogger log = Logger.of(WithResourceController.class);

	/**
	 * @param id
	 *            the collection id
	 * @param position
	 *            the position of the record in the collection
	 * @return
	 */
	public static Result addRecordToCollection(String id,
			Option<Integer> position) {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		ObjectId collectionDbId = new ObjectId(id);
		try {
			if (!DB.getWithResourceDAO().hasAccess(
					AccessManager.effectiveUserDbIds(session().get(
							"effectiveUserIds")), Action.EDIT, collectionDbId)) {
				result.put("error",
						"User does not have the right to edit the resource");
				return forbidden(result);
			}
			CollectionObject collection = DB.getCollectionObjectDAO().get(
					collectionDbId);
			if (collection == null) {
				log.error("Cannot retrieve resource from database");
				result.put("error", "Cannot retrieve resource from database");
				return internalServerError(result);
			}
			if (json == null) {
				result.put("error", "Invalid JSON");
				return badRequest(result);
			}
			RecordResource record = Json.fromJson(json, RecordResource.class);
			int last = record.getProvenance().size() - 1;
			Sources source = Sources.valueOf(((ProvenanceInfo) record
					.getProvenance().get(last)).getProvider());
			String sourceId = ((ProvenanceInfo) record.getProvenance()
					.get(last)).getResourceId();
			if (json.get("externalId") != null) {
				String externalId = json.get("externalId").asText();
				// It should be at the database
				if (DB.getRecordResourceDAO().getByExternalId(externalId) != null) {
					record = (RecordResource) DB.getRecordResourceDAO()
							.getByExternalId(externalId);
				}
			} else if (sourceId != null
					&& DB.getRecordResourceDAO().getByExternalId(sourceId) != null) {
				record = (RecordResource) DB.getRecordResourceDAO()
						.getByExternalId(sourceId);
			} else {
				ObjectNode errors;
				record.getAdministrative().setExternalId(sourceId);
				// Create a new record
				switch (source) {
				case UploadedByUser:
					DB.getRecordResourceDAO().makePermanent(record);
					((ProvenanceInfo) record.getProvenance().get(last))
							.setResourceId(record.getDbId().toString());
					((ProvenanceInfo) record.getProvenance().get(last))
							.setUri("/records/" + record.getDbId().toString());
					// Fill the EmbeddedMediaObject from the MediaObject that
					// has been created
					String mediaUrl;
					WithMediaRights withRights;
					EmbeddedMediaObject media;
					EmbeddedMediaObject embeddedMedia;
					for (MediaVersion version : MediaVersion.values()) {
						if ((embeddedMedia = ((HashMap<MediaVersion, EmbeddedMediaObject>) record
								.getMedia().get(0)).get(version)) != null) {
							mediaUrl = embeddedMedia.getUrl();
							withRights = embeddedMedia.getWithRights();
							media = DB.getMediaObjectDAO().getByUrl(mediaUrl);
							media.setWithRights(withRights);
							record.addMedia(version, media);
						}
					}
					DB.getRecordResourceDAO().makePermanent(record);
					break;
				case Mint:
					record.getAdministrative().setCreated(new Date());
					errors = RecordResourceController.validateRecord(record);
					if (errors != null) {
						return badRequest(errors);
					}
					DB.getRecordResourceDAO().makePermanent(record);
					break;
				default:
					record.getAdministrative().setCreated(new Date());
					errors = RecordResourceController.validateRecord(record);
					if (errors != null) {
						return badRequest(errors);
					}
					DB.getRecordResourceDAO().makePermanent(record);
					addContentToRecord(record.getDbId(), source.toString(),
							sourceId);
					break;
				}
			}
			// Add the record to the collection
			if (position.isDefined()) {
				Integer pos = position.get();
				DB.getRecordResourceDAO().shiftRecordsToRight(collectionDbId,
						pos);
				record.addPositionToCollectedIn(collectionDbId, pos);
			} else {
				// If the position is not defined the record is added at the
				// end of the collection
				record.addPositionToCollectedIn(collectionDbId,
						((CollectionAdmin) collection.getAdministrative())
								.getEntryCount());
			}
			record.getAdministrative().setLastModified(new Date());
			// In case the record already exists we modify the existing
			// record
			/*
			 * if (DB.getRecordResourceDAO().getByExternalId(externalId) !=
			 * null) { record = (RecordResource) DB.getRecordResourceDAO()
			 * .getByExternalId(externalId); } else { record =
			 * Json.fromJson(json, RecordResource.class);
			 * record.getAdministrative().setCreated(new Date());
			 * Set<ConstraintViolation<RecordResource>> violations = Validation
			 * .getValidator().validate(record); if (!violations.isEmpty()) {
			 * ArrayNode properties = Json.newObject().arrayNode(); for
			 * (ConstraintViolation<RecordResource> cv : violations) {
			 * properties.add(Json.parse("{\"" + cv.getPropertyPath() + "\":\""
			 * + cv.getMessage() + "\"}")); } result.put("error", properties);
			 * return badRequest(result); } // Download the content of a record
			 * from the source int last = record.getProvenance().size() - 1;
			 * String source = ((ProvenanceInfo) record.getProvenance().get(
			 * last)).getProvider(); String sourceId = ((ProvenanceInfo)
			 * record.getProvenance().get( last)).getResourceId();
			 * addContentToRecord(record.getDbId(), source, sourceId);
			 * 
			 * }
			 */
			// TODO modify access
			if (collection.getDescriptiveData().getLabel().equals("_favorites")) {
				record.getUsage().incLikes();
			} else {
				record.getUsage().incCollected();
			}
			DB.getRecordResourceDAO().makePermanent(record);
			// Change the collection metadata as well
			((CollectionAdmin) collection.getAdministrative()).incEntryCount();
			collection.getAdministrative().setLastModified(new Date());
			DB.getCollectionObjectDAO().makePermanent(collection);
			result.put("message", "Record succesfully added to collection");
			return ok(result);
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	/**
	 * @param id
	 * @param recordId
	 * @param position
	 * @return
	 */
	public static Result removeRecordFromCollection(String id, String recordId,
			Option<Integer> position) {
		ObjectNode result = Json.newObject();
		try {
			ObjectId collectionDbId = new ObjectId(id);
			if (!DB.getWithResourceDAO().hasAccess(
					AccessManager.effectiveUserDbIds(session().get(
							"effectiveUserIds")), Action.EDIT, collectionDbId)) {
				result.put("error",
						"User does not have the right to edit the resource");
				return forbidden(result);
			}
			CollectionObject collection = DB.getCollectionObjectDAO().get(
					collectionDbId);
			if (collection == null) {
				log.error("Cannot retrieve resource from database");
				result.put("error", "Cannot retrieve resource from database");
				return internalServerError(result);
			}
			RecordResource record = DB.getRecordResourceDAO().get(
					new ObjectId(recordId));
			if (position.isDefined()) {
				// record.removePositionFromCollectedIn(collectionDbId,
				// position.get());
			}
			// TODO modify access
			if (collection.getDescriptiveData().getLabel().equals("_favorites")) {
				record.getUsage().decLikes();
			} else {
				record.getUsage().decCollected();
			}
			DB.getRecordResourceDAO().makePermanent(record);
			// Change the collection metadata as well
			((CollectionAdmin) collection.getAdministrative()).incEntryCount();
			collection.getAdministrative().setLastModified(new Date());
			DB.getCollectionObjectDAO().makePermanent(collection);
			result.put("message", "Record succesfully added to collection");
			return ok(result);
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	public static Result moveRecordInCollection(String id, String recordId,
			int oldPosition, int newPosition) {
		ObjectNode result = Json.newObject();
		try {
			ObjectId collectionDbId = new ObjectId(id);
			ObjectId recordDbId = new ObjectId(recordId);
			if (!DB.getWithResourceDAO().hasAccess(
					AccessManager.effectiveUserDbIds(session().get(
							"effectiveUserIds")), Action.EDIT, collectionDbId)) {
				result.put("error",
						"User does not have the right to edit the resource");
				return forbidden(result);
			}
			CollectionObject collection = DB.getCollectionObjectDAO().get(
					collectionDbId);
			if (collection == null) {
				log.error("Cannot retrieve resource from database");
				result.put("error", "Cannot retrieve resource from database");
				return internalServerError(result);
			}
			if (oldPosition > newPosition) {
				DB.getRecordResourceDAO().shiftRecordsToRight(collectionDbId,
						newPosition, oldPosition - 1);
			} else if (newPosition > oldPosition) {
				DB.getRecordResourceDAO().shiftRecordsToRight(collectionDbId,
						oldPosition + 1, newPosition - 1);
			}
			DB.getRecordResourceDAO().updatePosition(recordDbId,
					collectionDbId, oldPosition, newPosition);
			collection.getAdministrative().setLastModified(new Date());
			DB.getCollectionObjectDAO().makePermanent(collection);
			result.put("message", "Record succesfully added to collection");
			return ok(result);
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	/**
	 * @param recordId
	 * @param source
	 * @param sourceId
	 */
	private static void addContentToRecord(ObjectId recordId, String source,
			String sourceId) {
		BiFunction<RecordResource, String, Boolean> methodQuery = (
				RecordResource record, String sourceClassName) -> {
			try {
				Class<?> sourceClass = Class.forName(sourceClassName);
				ISpaceSource s = (ISpaceSource) sourceClass.newInstance();
				List<RecordJSONMetadata> recordsData = s
						.getRecordFromSource(sourceId);
				for (RecordJSONMetadata data : recordsData) {
					DB.getCollectionRecordDAO().updateContent(record.getDbId(),
							data.getFormat(), data.getJsonContent());
				}
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
		RecordResource record = DB.getRecordResourceDAO().getById(recordId);
		String sourceClassName = "espace.core.sources." + source
				+ "SpaceSource";
		ParallelAPICall.createPromise(methodQuery, record, sourceClassName);
	}

	/**
	 * @return
	 */
	public static Result addToFavorites() {
		ObjectId userId = new ObjectId(session().get("user"));
		String fav = DB.getCollectionObjectDAO()
				.getByOwnerAndLabel(userId, null, "_favorites").getDbId()
				.toString();
		return addRecordToCollection(fav, Option.None());
	}

	/**
	 * @return
	 */
	public static Result removeFromFavorites(String recordId) {
		ObjectId userId = new ObjectId(session().get("user"));
		String fav = DB.getCollectionObjectDAO()
				.getByOwnerAndLabel(userId, null, "_favorites").getDbId()
				.toString();
		return removeRecordFromCollection(fav, recordId, Option.None());
	}

}
