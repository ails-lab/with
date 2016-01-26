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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;

import jdk.management.resource.ResourceId;
import model.DescriptiveData;
import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.WithMediaRights;
import model.resources.CulturalObject;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.ProvenanceInfo.Sources;
import model.resources.CollectionObject;
import model.resources.CollectionObject.CollectionAdmin;
import model.resources.CulturalObject.CulturalObjectData;
import model.resources.RecordResource;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Option;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results.Status;
import sources.core.ISpaceSource;
import sources.core.ParallelAPICall;
import sources.core.RecordJSONMetadata;
import sources.core.RecordJSONMetadata.Format;
import utils.AccessManager;
import utils.AccessManager.Action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;
import db.WithResourceDAO;

/**
 * @author mariaral
 *
 */
@SuppressWarnings("rawtypes")
public class WithResourceController extends Controller {

	public static final ALogger log = Logger.of(WithResourceController.class);
	
	public static Status errorIfNoAccessToWithResource(WithResourceDAO resourceDAO, Action action, ObjectId id) {
		ObjectNode result = Json.newObject();
		if (!resourceDAO.existsResource(id)) {
			log.error("Cannot retrieve resource from database");
			result.put("error", "Cannot retrieve resource from database");
			return internalServerError(result);
		}
		else
			if (!resourceDAO.hasAccess(AccessManager.effectiveUserDbIds(session().get(
					"effectiveUserIds")), action, id)) {
				result.put("error",
					"User does not have read-access for the resource");
				return forbidden(result);
			}
			else 
				return ok();
	}
	
	public static Status errorIfNoAccessToCollection(Action action, ObjectId collectionDbId) {
		return errorIfNoAccessToWithResource(DB.getCollectionObjectDAO(), action, collectionDbId);
	}
	
	public static Status errorIfNoAccessToRecord(Action action, ObjectId recordId) {
		return errorIfNoAccessToWithResource(DB.getCollectionObjectDAO(), action, recordId);
	}

	/**
	 * @param id
	 *            the collection id
	 * @param position
	 *            the position of the record in the collection
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Result addRecordToCollection(String colId,
			Option<Integer> position) {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		ObjectId collectionDbId = new ObjectId(colId);
		try {
			Result response = errorIfNoAccessToRecord(Action.EDIT, collectionDbId);
			if (!response.equals(ok()))
				return response;
			else {	
				if (json == null) {
					result.put("error", "Invalid JSON");
					return badRequest(result);
				}
				RecordResource record = Json.fromJson(json, RecordResource.class);
				int last = record.getProvenance().size() - 1;
				Sources source = Sources.valueOf(((ProvenanceInfo) record
						.getProvenance().get(last)).getProvider());
				String externalId = ((ProvenanceInfo) record.getProvenance()
						.get(last)).getResourceId();
				ObjectId recordId = null;
				if (externalId != null && DB.getRecordResourceDAO().existsWithExternalId(externalId)) {
						//get dbId of existing resource
						RecordResource resource = DB.getRecordResourceDAO().getByFieldAndValue("administrative.externalId", externalId, 
								new ArrayList<String>(Arrays.asList("_id")));
						recordId = resource.getDbId();
						// In case the record already exists we overwrite the existing
						// record's descriptive data
						DB.getRecordResourceDAO().editRecord("descriptiveData", resource.getDbId(), json.get("descriptiveData"));
				} else {  //create new record in db
					ObjectNode errors;
					record.getAdministrative().setExternalId(externalId);
					// Create a new record
					ObjectId userId = AccessManager.effectiveUserDbIds(session().get("effectiveUserIds")).get(0);
					record.getAdministrative().setWithCreator(userId);
					record.getAdministrative().setCreated(new Date());
					switch (source) {
					case UploadedByUser:
						//DB.getRecordResourceDAO().makePermanent(record);

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
								media = new EmbeddedMediaObject(DB
										.getMediaObjectDAO().getByUrl(mediaUrl));
								media.setWithRights(withRights);
								record.addMedia(version, media);
							}
						}
						DB.getRecordResourceDAO().makePermanent(record);
						//update provenance chain based on record dbId
						DB.getRecordResourceDAO().updateProvenance(record.getDbId(), last, new ProvenanceInfo("UploadedByUser", 
								"/records/" + record.getDbId().toString(), record.getDbId().toString()));
					case Mint:
						errors = RecordResourceController.validateRecord(record);
						if (errors != null) {
							return badRequest(errors);
						}
						DB.getRecordResourceDAO().makePermanent(record);
					default://imported first time from other sources
						errors = RecordResourceController.validateRecord(record);
						if (errors != null) {
							return badRequest(errors);
						}
						DB.getRecordResourceDAO().makePermanent(record);
						//TODO: how can record have a dbId?
						addContentToRecord(record.getDbId(), source.toString(),
								externalId);
					}
					DB.getRecordResourceDAO().makePermanent(record);
					recordId = record.getDbId();
				}
				// Update collection administrative metadata and record's usage metadata
				if (position.isDefined() && recordId != null) {
					Integer pos = position.get();
					DB.getRecordResourceDAO().addToCollection(recordId, collectionDbId, pos);
				} else {
					DB.getRecordResourceDAO().appendToCollection(recordId, collectionDbId);
				}				
				result.put("message", "Record succesfully added to collection");
				return ok(result);
			}
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
					if (data.getFormat().equals("JSON-WITH")) {
						ObjectMapper mapper = new ObjectMapper();
						JsonNode json = mapper.readTree(data.getJsonContent())
								.get("descriptiveData");
						DescriptiveData descriptiveData = Json.fromJson(json,
								CulturalObjectData.class);
						DB.getWithResourceDAO().updateDescriptiveData(recordId,
								descriptiveData);
/*						ArrayNode jsonArray;
						jsonArray = (ArrayNode) mapper.readTree(data.getJsonContent()).get(
								"media");
						for(JsonNode media : jsonArray) {
							EmbeddedMediaObject embeddedMediaObject = Json
									.fromJson(media, EmbeddedMediaObject.class);
							allMedia.
						}
						DB.getWithResourceDAO().updateEmbeddedMedia(recordId,
								embeddedMediaObject);*/
					} else {
						DB.getRecordResourceDAO().updateContent(
								record.getDbId(), data.getFormat(),
								data.getJsonContent());
					}
				}
				return true;
			} catch (Exception e) {
				return false;
			}
		};
		RecordResource record = DB.getRecordResourceDAO().getById(recordId);
		String sourceClassName = "sources." + source + "SpaceSource";
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
