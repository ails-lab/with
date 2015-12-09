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
import java.util.Set;

import javax.validation.ConstraintViolation;

import model.resources.CollectionObject;
import model.resources.CollectionObject.CollectionAdmin;
import model.resources.RecordResource;
import model.resources.WithResource.WithResourceType;
import model.basicDataTypes.ProvenanceInfo;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.data.validation.Validation;
import play.libs.F.Option;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessManager;
import utils.AccessManager.Action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class CollectionObjectController extends Controller {

	public static final ALogger log = Logger
			.of(CollectionObjectController.class);

	/**
	 * Creates a new WITH resource from the JSON body
	 *
	 * @return the newly created resource
	 */
	// TODO check restrictions (unique fields e.t.c)
	public static Result createCollectionObject() {
		ObjectNode error = Json.newObject();
		JsonNode json = request().body().asJson();
		try {
			if (json == null) {
				error.put("error", "Invalid JSON");
				return badRequest(error);
			}
			if (session().get("user") == null) {
				error.put("error", "No rights for WITH resource creation");
				return forbidden(error);
			}
			ObjectId creator = new ObjectId(session().get("user"));
			CollectionObject collection = Json.fromJson(json,
					CollectionObject.class);
			Set<ConstraintViolation<CollectionObject>> violations = Validation
					.getValidator().validate(collection);
			if (!violations.isEmpty()) {
				ArrayNode properties = Json.newObject().arrayNode();
				for (ConstraintViolation<CollectionObject> cv : violations) {
					properties.add(Json.parse("{\"" + cv.getPropertyPath()
							+ "\":\"" + cv.getMessage() + "\"}"));
				}
				error.put("error", properties);
				return badRequest(error);
			}
			// Fill with all the administrative metadata
			collection.setResourceType(WithResourceType.CollectionObject);
			collection.getAdministrative().setWithCreator(creator);
			collection.getAdministrative().setCreated(new Date());
			collection.getAdministrative().setLastModified(new Date());
			if (collection.getAdministrative() instanceof CollectionAdmin) {
				((CollectionAdmin) collection.getAdministrative())
						.setEntryCount(0);
			}
			// TODO: withURI?
			// TODO: maybe moderate usage statistics?
			DB.getCollectionObjectDAO().makePermanent(collection);
			return ok(Json.toJson(collection));
		} catch (Exception e) {
			error.put("error", e.getMessage());
			return internalServerError(error);
		}
	}

	/**
	 * Retrieve a resource metadata. If the format is defined the specific
	 * serialization of the object is returned
	 *
	 * @param id
	 *            the resource id
	 * @return the resource metadata
	 */
	public static Result getCollectionObject(String id) {
		ObjectNode result = Json.newObject();
		try {
			CollectionObject collection = DB.getCollectionObjectDAO().get(
					new ObjectId(id));
			if (collection == null) {
				log.error("Cannot retrieve resource from database");
				result.put("error", "Cannot retrieve resource from database");
				return internalServerError(result);
			}
			if (!AccessManager.checkAccess(collection.getAdministrative()
					.getAccess(), session().get("effectiveUserIds"),
					Action.READ)) {
				result.put("error",
						"User does not have read-access for the resource");
				return forbidden(result);
			}
			return ok(Json.toJson(collection));
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	/**
	 * Deletes all resource metadata
	 *
	 * @param id
	 *            the resource id
	 * @return success message
	 */
	// TODO: cascaded delete (if needed)
	public static Result deleteCollectionObject(String id) {
		ObjectNode result = Json.newObject();
		try {
			CollectionObject collection = DB.getCollectionObjectDAO().get(
					new ObjectId(id));
			if (collection == null) {
				log.error("Cannot retrieve resource from database");
				result.put("error", "Cannot retrieve resource from database");
				return internalServerError(result);
			}
			if (!AccessManager.checkAccess(collection.getAdministrative()
					.getAccess(), session().get("effectiveUserIds"),
					Action.DELETE)) {
				result.put("error",
						"User does not have the right to delete the resource");
				return forbidden(result);
			}
			DB.getCollectionObjectDAO().makeTransient(collection);
			result.put("message", "Resource was deleted successfully");
			return ok(result);
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	/**
	 * Edits the WITH resource according to the JSON body. For every field
	 * mentioned in the JSON body it either edits the existing one or it adds it
	 * (in case it doesn't exist)
	 *
	 * @return the edited resource
	 */
	// TODO check restrictions (unique fields e.t.c)
	public static Result editCollectionObject(String id) {
		ObjectNode error = Json.newObject();
		JsonNode json = request().body().asJson();
		try {
			if (json == null) {
				error.put("error", "Invalid JSON");
				return badRequest(error);
			}
			CollectionObject oldCollection = DB.getCollectionObjectDAO().get(
					new ObjectId(id));
			if (oldCollection == null) {
				log.error("Cannot retrieve resource from database");
				error.put("error", "Cannot retrieve resource from database");
				return internalServerError(error);
			}
			if (!AccessManager.checkAccess(oldCollection.getAdministrative()
					.getAccess(), session().get("effectiveUserIds"),
					Action.EDIT)) {
				error.put("error",
						"User does not have the right to edit the resource");
				return forbidden(error);
			}
			// TODO change JSON at all its depth
			ObjectMapper objectMapper = new ObjectMapper();
			ObjectReader updator = objectMapper
					.readerForUpdating(oldCollection);
			CollectionObject newCollection;
			newCollection = updator.readValue(json);
			Set<ConstraintViolation<CollectionObject>> violations = Validation
					.getValidator().validate(newCollection);
			if (!violations.isEmpty()) {
				ArrayNode properties = Json.newObject().arrayNode();
				for (ConstraintViolation<CollectionObject> cv : violations) {
					properties.add(Json.parse("{\"" + cv.getPropertyPath()
							+ "\":\"" + cv.getMessage() + "\"}"));
				}
				error.put("error", properties);
				return badRequest(error);
			}
			newCollection.getAdministrative().setLastModified(new Date());
			DB.getCollectionObjectDAO().makePermanent(newCollection);
			return ok(Json.toJson(newCollection));
		} catch (Exception e) {
			error.put("error", e.getMessage());
			return internalServerError(error);
		}
	}

	public static Result addRecordToCollection(String id,
			Option<Integer> position) {
		ObjectNode result = Json.newObject();
		JsonNode json = request().body().asJson();
		try {
			ObjectId collectionDbId = new ObjectId(id);
			CollectionObject collection = DB.getCollectionObjectDAO().get(
					collectionDbId);
			if (collection == null) {
				log.error("Cannot retrieve resource from database");
				result.put("error", "Cannot retrieve resource from database");
				return internalServerError(result);
			}
			if (!AccessManager.checkAccess(collection.getAdministrative()
					.getAccess(), session().get("effectiveUserIds"),
					Action.EDIT)) {
				result.put("error",
						"User does not have the right to edit the resource");
				return forbidden(result);
			}
			if (json == null) {
				result.put("error", "Invalid JSON");
				return badRequest(result);
			}
			if (json.get("externalId") == null) {
				result.put("error",
						"Field \"externalId\" is mandatory for the record");
				return badRequest(result);
			}
			RecordResource record;
			// In case the record already exists we modify the existing
			// record
			if (DB.getRecordResourceDAO().getByExternalId(
					json.get("externalId").asText()) != null) {
				record = (RecordResource) DB.getRecordResourceDAO()
						.getByExternalId(json.get("externalId").asText());
			} else {
				record = Json.fromJson(json, RecordResource.class);
				record.getAdministrative().setCreated(new Date());
				Set<ConstraintViolation<RecordResource>> violations = Validation
						.getValidator().validate(record);
				if (!violations.isEmpty()) {
					ArrayNode properties = Json.newObject().arrayNode();
					for (ConstraintViolation<RecordResource> cv : violations) {
						properties.add(Json.parse("{\"" + cv.getPropertyPath()
								+ "\":\"" + cv.getMessage() + "\"}"));
					}
					result.put("error", properties);
					return badRequest(result);
				}
			}
			record.getAdministrative().setLastModified(new Date());
			if (position.isDefined()) {
				record.addPositionToCollectedIn(collectionDbId, position.get());
			} else {
				// If the position is not defined the record is added at the
				// end of the collection
				record.addPositionToCollectedIn(collectionDbId,
						((CollectionAdmin) collection.getAdministrative())
								.getEntryCount());
			}
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
}
