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

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import model.DescriptiveData;
import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.WithMediaType;
import model.MediaObject;
import model.annotations.ContextData;
import model.annotations.ContextData.ContextDataBody;
import model.annotations.ContextData.ContextDataTarget;
import model.annotations.ContextData.ContextDataType;
import model.basicDataTypes.Language;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.ProvenanceInfo;
import model.quality.RecordQuality;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import model.resources.RecordResource;
import model.resources.WithResourceType;
import model.resources.collection.CollectionObject;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Option;
import play.libs.Json;
import play.mvc.Result;
import search.Sources;
import sources.core.ISpaceSource;
import sources.core.ParallelAPICall;
import sources.core.ParallelAPICall.Priority;
import sources.core.RecordJSONMetadata;
import sources.utils.JsonContextRecord;
import utils.Locks;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.qos.logback.core.Context;
import db.DB;
import db.WithResourceDAO;
import edu.stanford.nlp.ling.CoreAnnotations.ContextsAnnotation;

/**
 * @author mariaral
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class WithResourceController extends WithController {

	public static final ALogger log = Logger.of(WithResourceController.class);

	public static Status errorIfNoAccessToWithResource(
			WithResourceDAO resourceDAO, Action action, ObjectId id) {

		ObjectNode result = Json.newObject();
		if (!resourceDAO.existsEntity(id)) {
			log.error("Cannot retrieve resource from database");
			result.put("error", "Cannot retrieve resource " + id
					+ " from database");
			return internalServerError(result);
			// TODO superuser
		} else if (!resourceDAO.hasAccess(
				effectiveUserDbIds(), action, id)
				&& !isSuperUser()) {
			result.put("error", "User does not have " + action
					+ " access for resource " + id);
			return forbidden(result);
		} else {
			return ok();
		}
	}

	public static Status errorIfNoAccessToCollection(Action action,
			ObjectId collectionDbId) {
		return errorIfNoAccessToWithResource(DB.getCollectionObjectDAO(),
				action, collectionDbId);
	}

	public static Status errorIfNoAccessToRecord(Action action,
			ObjectId recordId) {
		return errorIfNoAccessToWithResource(DB.getRecordResourceDAO(), action,
				recordId);
	}

	/**
	 * @param id
	 *            the collection id
	 * @param position
	 *            the position of the record in the collection
	 * @return
	 */
	// If a record already exists in that collection-position, prohibit
	// addition-
	// have to remove first. This way, do not have to check if colId-position
	// already exists
	// in collectedIn and remove it (2 update operations).
	public static Result addRecordToCollection(String colId,
			Option<Integer> position, Boolean noDouble) {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		ObjectId collectionDbId = new ObjectId(colId);
		Locks locks = null;
		try {
			locks = Locks.create().write("Collection #" + colId).acquire();
			Status response = errorIfNoAccessToCollection(Action.EDIT,
					collectionDbId);
			if (!response.toString().equals(ok().toString())) {
				return response;
			} else {
				if (json == null) {
					result.put("error", "Invalid JSON");
					return badRequest(result);
				}
				return addRecordToCollection(json, collectionDbId, position,
						noDouble);
			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		} finally {
			if (locks != null)
				locks.release();
		}
	}

	public static Result internalAddRecordToCollection(String colId,
			RecordResource record, Option<Integer> position, ObjectNode result) {
		return addRecordToCollection(Json.toJson(record), new ObjectId(colId),
				position, false);
	}

	public static Result internalAddRecordToCollection(String colId,
			RecordResource record, Option<Integer> position, ObjectNode result,
			boolean noRepeated) {
		return addRecordToCollection(Json.toJson(record), new ObjectId(colId),
				position, noRepeated);
	}
	public static void updateRecord(ObjectId recordId, String source, String sourceId ){
//		DB.getRecordResourceDAO().editRecord("", recordId, json);
		addContentToRecord(recordId, source, sourceId,false);
	}

	// TODO: Mint import should be no different then user upload?
	// Mint uses the normal User upload API after all. 
	// But there is a question, should the upload user be in the provenance chain
	// when the record is maintained in a mint instance ?
	// Maybe, when the upload contains provenance, it shouldn't be messed with?
	// Since the provenance info is some kind of audit, it makes sense to document
	// the uploading user, even if the source is a mint instance
	// PS: provenance should potentially contain dates...
	public static Result addRecordToCollection(JsonNode json,
			ObjectId collectionDbId, Option<Integer> position, Boolean noDouble) {
		ObjectNode result = Json.newObject();
		String resourceType = null;
		ObjectId userId = effectiveUserDbIds().get(0);
		if (json.has("resourceType")) {
			resourceType = json.get("resourceType").asText();
		}
		if ((resourceType == null) || (WithResourceType.valueOf(resourceType) == null)) {
			resourceType = WithResourceType.CulturalObject.toString();

		}
		try {
			if (json.has("contextData")) {
				((ObjectNode) json).remove("contextData");
			}
			Class<?> clazz = Class.forName("model.resources." + resourceType);
			RecordResource record = (RecordResource) Json.fromJson(json, clazz);
			MultiLiteral label = record.getDescriptiveData().getLabel();
			if ((label == null) || (label.get(Language.DEFAULT) == null) || label.get(Language.DEFAULT).isEmpty() || (label.get(Language.DEFAULT).get(0) == "")) {
				log.error("A label for the record has to be provided");
				return badRequest("A label for the record has to be provided");
			}
			int last = 0;
			// Sources source = Sources.UploadedByUser;
			String source = "Upload by User " + effectiveUser().getUsername();
			if ((record.getProvenance() != null) && !record.getProvenance().isEmpty()) {
				last = record.getProvenance().size() - 1;
				source = (((ProvenanceInfo) record
						.getProvenance().get(last)).getProvider());
			} else {
				record.setProvenance(new ArrayList<ProvenanceInfo>(Arrays.asList(new ProvenanceInfo(source))));
			}

			String externalId = ((ProvenanceInfo) record.getProvenance().get(
					last)).getResourceId();
			if (externalId == null){
				externalId = record.getAdministrative().getExternalId();
			}
			if (record.getDescriptiveData().getDates() != null && !record.getDescriptiveData().getDates().isEmpty()) {
				record.getDescriptiveData().getDates().forEach((date) -> date.sanitizeDates());
			}
			ObjectId recordId = null;

			// TODO WTF does this do
			boolean owns = DB.getRecordResourceDAO().hasAccess(
					effectiveUserDbIds(), Action.DELETE, recordId);

			if ((externalId != null) && DB.getRecordResourceDAO().existsWithExternalId(externalId))
			{
				RecordResource resource = DB.getRecordResourceDAO().getUniqueByFieldAndValue("administrative.externalId", externalId, new ArrayList<String>(Arrays.asList("_id")));
				recordId = resource.getDbId();
				Status response = errorIfNoAccessToRecord(Action.READ, recordId);
				if (!response.toString().equals(ok().toString())) {
					return response;
				}
				else {// In case the record already exists we overwrite
						// the existing record's descriptive data for the fields
						// included in the json, if the user has WRITE access.
					boolean existsInSameCollection = DB.getRecordResourceDAO()
							.existsSameExternaIdInCollection(externalId,
									collectionDbId);

//					if (DB.getRecordResourceDAO().hasAccess(effectiveUserDbIds(), Action.EDIT, recordId) && (json.get("descriptiveData") != null)) {
					if ((!source.startsWith("Upload by User") && (json.get("descriptiveData") != null)) ||
						(source.startsWith("Upload by User") && DB.getRecordResourceDAO().hasAccess(effectiveUserDbIds(), Action.EDIT, recordId) && (json.get("descriptiveData") != null))) {
							addContentToRecord(resource.getDbId(), source.toString(), externalId, true);
							DB.getRecordResourceDAO().editRecord("descriptiveData", resource.getDbId(), json.get("descriptiveData"));
					}

					if (noDouble && existsInSameCollection) {
						result.put("error", "double");
						log.error("Record is in the collection already "+externalId);
						return forbidden(result);
					}

					addToCollection(position, recordId, collectionDbId, owns, existsInSameCollection);
				}
			} else { // create new record in db
				ObjectNode errors;
				record.getAdministrative().setCreated(new Date());
				if( source.startsWith("Upload by User")) {
					// Fill the EmbeddedMediaObject from the MediaObject
					// that has been created
					record.getAdministrative().setWithCreator(userId);
					String mediaUrl;
					for (HashMap<MediaVersion, EmbeddedMediaObject> embeddedMedia : (List<HashMap<MediaVersion, EmbeddedMediaObject>>) record
							.getMedia()) {
						for (MediaVersion version : embeddedMedia.keySet()) {
							EmbeddedMediaObject media = embeddedMedia
									.get(version);
							if (media != null) {
								mediaUrl = media.getUrl();
								EmbeddedMediaObject existingMedia = null;
								if (!mediaUrl.isEmpty()) {
									MediaObject mediaObject = DB
											.getMediaObjectDAO().getByUrl(
													mediaUrl);
									if (mediaObject != null) {
										// if user has access to at least one
										// recordResource pointing to that
										// media, or if media is an orphan
										// then the user has write access to the
										// media
										boolean hasAccessToMedia = MediaController
												.hasAccessToMedia(
														mediaUrl,
														effectiveUserDbIds(),
														Action.EDIT);
										if (!hasAccessToMedia)
											media = new EmbeddedMediaObject(
													existingMedia);
									}
									// TODO: careful, the user is allowed to set
									// the media fields as s/he wishes,
									// if the media url doesn't already exist.
									// The first time that a request for caching
									// that url is issued, the MediaObject that
									// is created in the db will be filled by
									// parseMediaURL, possibly with values that
									// are different from the
									// embeddedMediaObject.
									// in the record. In general, each user who
									// adds a record with the same media url,
									// before the media is actually saved in the
									// db, may have a different version of media
									// info.
									// The call allows the user to specify
									// a wrong version, e.g. create a media
									// which claims
									// to be thumbnail, but is full size. Can
									// Marios find out the version (and correct
									// it) during caching?
									record.addMedia(version, media);
								}	
							}
						}
					}
					DB.getRecordResourceDAO().makePermanent(record);
					recordId = record.getDbId();
					// setting of provevance's resourceId and externalId
					// (based on recordDbId) is taken care by postLoad in
					// WithResource
					DB.getRecordResourceDAO().updateWithURI(recordId,
							"/record/" + recordId);
					DB.getRecordResourceDAO().updateProvenance(
							recordId,
							last,
							new ProvenanceInfo("UploadedByUser", "record/"
									+ recordId, recordId.toString()));
					DB.getRecordResourceDAO().updateField(recordId,
							"administrative.externalId", recordId.toString());
				} else if ( source.startsWith( "Mint")) {
					errors = RecordResourceController.validateRecord(record);
					record.getAdministrative().setWithCreator(userId);
					List<ProvenanceInfo> provenance = record.getProvenance();
					record.getAdministrative().setExternalId(
							provenance.get(last).getResourceId());
					if (errors != null) {
						return badRequest(errors);
					}
					DB.getRecordResourceDAO().makePermanent(record);
					recordId = record.getDbId();
					DB.getRecordResourceDAO().updateWithURI(record.getDbId(),
							"/record/" + recordId);
				} else {
					// imported first time from other sources
					// there is no withCreator and the record is public
					record.getAdministrative().getAccess().setIsPublic(true);
					errors = RecordResourceController.validateRecord(record);
					List<ProvenanceInfo> provenance = record.getProvenance();
					record.getAdministrative().setExternalId(
							provenance.get(last).getResourceId());
					if (errors != null) {
						return badRequest(errors);
					}
					DB.getRecordResourceDAO().makePermanent(record);
					recordId = record.getDbId();
					DB.getRecordResourceDAO().updateWithURI(recordId,
							"/record/" + recordId);
					addContentToRecord(record.getDbId(), source.toString(),
							externalId, true);
				}
				addToCollection(position, recordId, collectionDbId, owns, false);
			}
			fillMissingThumbnailsAsync(recordId);
			result.put("message", "Record succesfully added to collection");
			JsonContextRecord rec = new JsonContextRecord(json);
			rec.setValue("dbId",recordId.toString());
			rec.setValue("administrative.withURI","/record/" + recordId);
			return ok(Json.toJson(record));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}
	
	

	public static Result addRecordsToCollection(String colId, Boolean noDouble) {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		ObjectId collectionDbId = new ObjectId(colId);
		Locks locks = null;
		try {
			locks = Locks.create().write("Collection #" + colId).acquire();
			Status response = errorIfNoAccessToCollection(Action.EDIT,
					collectionDbId);
			if (!response.toString().equals(ok().toString())) {
				return response;
			} else {
				if ((json == null) || !json.isArray()) {
					result.put("error", "Invalid JSON");
					return badRequest(result);
				} else {
					Iterator<JsonNode> iterator = json.iterator();
					while (iterator.hasNext()) {
						JsonNode recordJson = iterator.next();
						Result r = addRecordToCollection(recordJson,
								new ObjectId(colId), Option.None(), noDouble);
						if (!r.toString().equals(ok().toString())) {
							return r;
						}
					}
					result.put("message",
							"Records have been successfully added to to collection.");
					return ok(result);
				}
			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		} finally {
			if (locks != null)
				locks.release();
		}
	}

	// Updates collection administrative metadata, record's usage
	// and collectedIn. The rights of all collections the resource
	// belongs to are merged and are copied to the record
	// only if the user OWNs the resource.
	public static void addToCollection(Option<Integer> position,
			ObjectId recordId, ObjectId colId, boolean owns, boolean existsInSameCollection) {
		if (position.isDefined() && (recordId != null)) {
			Integer pos = position.get();
			DB.getRecordResourceDAO().addToCollection(recordId, colId, pos,
					owns, existsInSameCollection);
		} else {
			DB.getRecordResourceDAO().appendToCollection(recordId, colId, owns, existsInSameCollection);
		}
	}

	// ignores context data that do not refer to the colId-position where the
	// record is added
	// if target not defined, assumes it refers to colId-position.
	private static void fillInContextTarget(JsonNode json, String colId,
			int position) {
		if (json.has("contextData")) {
			JsonNode contextDataJson = json.get("contextData");
			if (contextDataJson.isArray()) {
				Iterator<JsonNode> itr = contextDataJson.iterator();
				while (itr.hasNext()) {
					JsonNode c = itr.next();
					if (c instanceof ObjectNode) {
						if (c.has("target")) {
							JsonNode targetNode = c.get("target");
							if (targetNode.has("collectionId")) {
								String collectionId = targetNode.get(
										"collectionId").asText();
								if (!colId.equals(collectionId))
									itr.remove();
								if (targetNode.has("position")) {
									int positionInJson = targetNode.get(
											"position").asInt();
									if (positionInJson != position)
										itr.remove();
								}
							}
						} else {
							ObjectNode targetNode = JsonNodeFactory.instance
									.objectNode();
							targetNode.put("collectionId", colId);
							targetNode.put("position", position);
							((ObjectNode) c).put("target", targetNode);
						}
					}
				}
			}
		}
	}

	// ignores context data that do not refer to the colId-position where the
	// record is added
	private static ContextData createContextData(JsonNode json, ObjectId colId,
			int position) {
		ContextData contextData = new ContextData();
		if (json.has("contextData")) {
			JsonNode contextDataJson = json.get("contextData");
			if (contextDataJson.isArray()) {
				for (final JsonNode c : contextDataJson) {
					if (c.has("contextDataType")) {
						String contextDataTypeString = c.get("contextDataType")
								.asText();
						ContextDataType contextDataType = null;
						if ((contextDataType = ContextDataType
								.valueOf(contextDataTypeString)) != null) {
							ContextDataTarget target = null;
							if (c.has("target")) {
								JsonNode targetNode = c.get("target");
								if (targetNode.has("collectionId")) {
									String collectionId = targetNode.get(
											"collectionId").asText();
									if (!colId
											.equals(new ObjectId(collectionId)))
										continue;
									if (targetNode.has("position")) {
										int positionInJson = targetNode.get(
												"position").asInt();
										if (positionInJson != position)
											continue;
									}
								}
							} else {
								target = new ContextDataTarget();
								// target.setPosition(position);
							}
							if (c.has("body")) {
								JsonNode bodyNode = c.get("body");
								Class<?> clazz;
								try {
									clazz = Class.forName("model.annotations."
											+ contextDataTypeString);
									ContextDataBody body = (ContextDataBody) Json
											.fromJson(bodyNode, clazz);
									contextData.setBody(body);
									contextData.setTarget(target);
									contextData
											.setContextDataType(contextDataType);
								} catch (ClassNotFoundException e) {
									log.error("",e);
								}
							}
						}
					}
				}
			}
		}
		return contextData;
	}

	private static void fillMissingThumbnailsAsync(ObjectId recordId) {
		Function<ObjectId, Boolean> methodQuery = (ObjectId recId) -> {
			RecordResource record = DB.getRecordResourceDAO().getById(recId,
					new ArrayList<String>(Arrays.asList("media")));
			int i = 0;
			for (HashMap<MediaVersion, EmbeddedMediaObject> embeddedMedia : (List<HashMap<MediaVersion, EmbeddedMediaObject>>) record
					.getMedia()) {
				try {
					if (embeddedMedia.containsKey(MediaVersion.Original)
							&& !embeddedMedia.containsKey(MediaVersion.Thumbnail)
							&& embeddedMedia.get(MediaVersion.Original).getType()
									.equals(WithMediaType.IMAGE)) {
						String originalUrl = embeddedMedia.get(
								MediaVersion.Original).getUrl();
						MediaObject original = MediaController.downloadMedia(
								originalUrl, MediaVersion.Original);
						MediaObject thumbnail = MediaController
								.makeThumbnail(original);
						if (thumbnail != null) {
							DB.getRecordResourceDAO().updateMedia(recId, i,
								MediaVersion.Thumbnail,
								new EmbeddedMediaObject(thumbnail));
						}
					}
					i++;
				} 
				catch (Throwable thr) {
					log.error("Thumbnail generation failed "+ recId.toString(), thr);
					throw thr;
				}
			}
			DB.getWithResourceDAO().computeAndUpdateQuality(recordId);
			return true;
		};
		ParallelAPICall.createPromise(methodQuery, recordId, Priority.BACKEND);
	}

	/**
	 * @param id
	 * @param recordId
	 * @param position
	 * @return
	 */
	public static Result removeRecordFromCollection(String id, String recordId,
			Option<Integer> position, boolean all) {
		ObjectNode result = Json.newObject();
		Locks locks = null;
		try {
			ObjectId collectionDbId = new ObjectId(id);
			locks = Locks.create().write("Collection #" + collectionDbId)
					.acquire();
			Result response = errorIfNoAccessToCollection(Action.EDIT,
					collectionDbId);
			ObjectId recordDbId = new ObjectId(recordId);
			if (!response.toString().equals(ok().toString()))
				return response;
			int pos;
			boolean first = false;
			if (position.isDefined()) {
				pos = position.get();
			} else {
				pos = -1;
				if (!all)
					first = true;
			}
			DB.getRecordResourceDAO().removeFromCollection(recordDbId,
					collectionDbId, pos, first, all);
			result.put("message", "Record succesfully removed from collection");
			return ok(result);
		} catch (FileNotFoundException e) {
			result.put("error", "Wrong record id or position in the collection");
			return badRequest(result);
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		} finally {
			if (locks != null)
				locks.release();
		}
	}

	// TODO: update collection's media
	public static Result moveRecordInCollection(String id, String recordId,
			int oldPosition, int newPosition) {
		ObjectNode result = Json.newObject();
		Locks locks = null;
		try {
			ObjectId collectionDbId = new ObjectId(id);
			locks = Locks.create().write("Collection #" + collectionDbId)
					.acquire();
			ObjectId recordDbId = new ObjectId(recordId);
			Result response = errorIfNoAccessToCollection(Action.EDIT,
					collectionDbId);
			if (!response.toString().equals(ok().toString()))
				return response;
			DB.getCollectionObjectDAO().moveInCollection(collectionDbId,
					recordDbId, oldPosition, newPosition);
			result.put("message", "Record succesfully moved in collection");
			return ok(result);
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		} finally {
			if (locks != null)
				locks.release();
		}
	}

	/**
	 * This method calls record API to retrieve extra info on a record.
	 * Uses EuropeanaItemRecordFormatter to parse stuff.
	 *
	 * @param recordId
	 * @param source
	 * @param sourceId
	 */
	private static void addContentToRecord(ObjectId recordId, String source,
			String sourceId, boolean later) {
			BiFunction<RecordResource, String, Boolean> methodQuery = (
				RecordResource record, String sourceClassName) -> {
			try {
				Class<?> sourceClass = Class.forName(sourceClassName);
				ISpaceSource s = (ISpaceSource) sourceClass.newInstance();
				RecordResource fullRecord = DB.getRecordResourceDAO().get(recordId);
				List<RecordJSONMetadata> recordsData = s.getRecordFromSource(sourceId, fullRecord);
				
				for (RecordJSONMetadata data : recordsData) {
					if (data.getFormat().equals("JSON-WITH")) {

						ObjectMapper mapper = new ObjectMapper();
						JsonNode json = mapper.readTree(data.getJsonContent());
						CulturalObject cho = Json.fromJson(json, CulturalObject.class);
						
						DB.getWithResourceDAO().computeAndUpdateQuality(recordId);
						
						DB.getWithResourceDAO().computeAndUpdateRights(recordId);
						log.debug(data.getJsonContent());
						
						DB.getWithResourceDAO().updateDescriptiveData(recordId,
								cho.getDescriptiveData());
						
						DB.getWithResourceDAO().updateEmbeddedMedia(recordId,
								cho.getMedia());
						
						DB.getWithResourceDAO().updateProvenance(recordId, cho.getProvenance());
						
//						DB.getWithResourceDAO().computeAndUpdateQuality(recordId);
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
		if (later)
		ParallelAPICall.createPromise(methodQuery, record, sourceClassName,
				Priority.BACKEND);
		else{
			methodQuery.apply(record, sourceClassName);
		}
	}

	/**
	 * @return
	 */
	public static Result addToFavorites() {
		ObjectId userId = new ObjectId(session().get("user"));
		String fav = DB.getCollectionObjectDAO()
				.getByOwnerAndLabel(userId, null, "_favorites").getDbId()
				.toString();
		return addRecordToCollection(fav, Option.None(), true);
	}

	/**
	 * @return
	 */
	public static Result removeFromFavorites(String externalId) {
		ObjectId userId = new ObjectId(session().get("user"));
		String fav = DB.getCollectionObjectDAO()
				.getByOwnerAndLabel(userId, null, "_favorites").getDbId()
				.toString();
		RecordResource record = DB.getRecordResourceDAO().getByExternalId(
				externalId);
		/*
		 * List<CollectionInfo> collected = record.getCollectedIn(); for
		 * (CollectionInfo c : collected) { if
		 * (c.getCollectionId().toString().equals(fav)) { return
		 * removeRecordFromCollection(fav, record.getDbId() .toString(),
		 * Option.Some(c.getPosition()), false); } }
		 */
		return removeRecordFromCollection(fav, record.getDbId().toString(),
				Option.None(), true);
	}

	public static Result removeFromFavorites() {
		JsonNode json = request().body().asJson();
		JsonNode externalIdNode = json.get("externalId");
		if (externalIdNode == null) {
			ObjectNode result = Json.newObject();
			result.put("error",
					"Json request should have format {externalId: id}");
			return badRequest(result);
		}
		String externalId = externalIdNode.asText();
		ObjectId userId = new ObjectId(session().get("user"));
		String fav = DB.getCollectionObjectDAO()
				.getByOwnerAndLabel(userId, null, "_favorites").getDbId()
				.toString();
		RecordResource record = DB.getRecordResourceDAO().getByExternalId(
				externalId);
		return removeRecordFromCollection(fav, record.getDbId().toString(),
				Option.None(), false);
	}
	
	public static Result removeIdsOfDeletedRecords() {
		Iterator<RecordResource> recordIterator = DB.getRecordResourceDAO().createQuery().iterator();
		int i = 1;
		Set recordIds = new HashSet<>();
		while (recordIterator.hasNext()) {
			log.info("Getting " + i++ + " record");
			RecordResource record = recordIterator.next();
			recordIds.add(record.getDbId());
		}
		Iterator<CollectionObject> collectionIterator = DB.getCollectionObjectDAO().createQuery().iterator();
		i = 1;
		while (collectionIterator.hasNext()) {
			log.info("Getting " + i++ + " collection");
			CollectionObject collection = collectionIterator.next();
			List<ContextData<ContextDataBody>> collectedResources = collection.getCollectedResources();
			int resourcesRemoved = 0;
			Iterator<ContextData<ContextDataBody>> it = collectedResources.iterator();
			while(it.hasNext()) {
				ContextData<ContextDataBody> cr = it.next();
				if (!recordIds.contains(cr.getTarget().getRecordId())) {
					it.remove();
					resourcesRemoved++;
				}	
			}
			if (resourcesRemoved > 0) {
				log.info("Found " + resourcesRemoved +" deleted records");
				Query<CollectionObject> q = DB.getCollectionObjectDAO().createQuery()
						.field("_id").equal(collection.getDbId());
				UpdateOperations<CollectionObject> collectionUpdate = DB
						.getCollectionObjectDAO().createUpdateOperations()
						.disableValidation();
				collectionUpdate.set("collectedResources", collectedResources);
				collectionUpdate.inc("administrative.entryCount", 0 - resourcesRemoved);
				collectionUpdate.set("administrative.lastModified", new Date());
				DB.getCollectionObjectDAO().update(q, collectionUpdate);
			}
		}
		return ok();		
	}
}
