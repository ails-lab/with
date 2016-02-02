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
import java.util.Set;

import javax.validation.ConstraintViolation;

import model.resources.CollectionObject;
import model.resources.RecordResource;
import model.resources.WithResource.WithResourceType;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.data.validation.Validation;
import play.libs.F.Option;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results.Status;
import utils.AccessManager;
import utils.AccessManager.Action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

/**
 * @author mariaral
 *
 */
public class RecordResourceController extends WithResourceController {

	public static final ALogger log = Logger.of(RecordResourceController.class);

	/**
	 * Retrieve a resource metadata. If the format is defined the specific
	 * serialization of the object is returned
	 *
	 * @param id
	 *            the resource id
	 * @param format
	 *            the resource serialization
	 * @return the resource metadata
	 */
	public static Result getRecordResource(String id, Option<String> format) {
		ObjectNode result = Json.newObject();
		try {
			RecordResource resource = DB.getRecordResourceDAO().get(
					new ObjectId(id));
			Result response = errorIfNoAccessToRecord(Action.READ, new ObjectId(id));
			if (!response.toString().equals(ok().toString()))
				return response;
			else {
				if (format.isDefined()) {
					if (format.equals("allContent")) {
						return ok(Json.toJson(resource.getContent()));
					}
					else {
						if (format.equals("noContent")) {
							resource.getContent().clear();
							return ok(Json.toJson(resource));
						}
						else if (resource.getContent().containsKey(format)) {
							return ok(resource.getContent().get(format).toString());
						}
						else {
							result.put("error", "Resource does not contain representation for format" + format);
							return play.mvc.Results.notFound(result);
						}
					}
				}
				else 
					return ok(Json.toJson(resource));
			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	/**
	 * Deletes all resource metadata or just a serialization
	 *
	 * @param id
	 *            the resource id
	 * @param format
	 *            the resource serialization
	 * @return success message
	 */
	// TODO: cascaded delete (if needed)
	public static Result deleteRecordResource(String id, Option<String> format) {
		ObjectNode result = Json.newObject();
		try {
			RecordResource resource = DB.getRecordResourceDAO().get(
					new ObjectId(id));
			Result response = errorIfNoAccessToRecord(Action.DELETE, new ObjectId(id));
			if (!response.toString().equals(ok().toString()))
				return response;
			else {
				if (format.isDefined() && resource.getContent().containsKey(format)) {
					resource.getContent().remove(format);
					result.put("message",
							"Serialization of resource was deleted successfully");
					return ok(result);
				}
				else {
					DB.getRecordResourceDAO().makeTransient(resource);
					result.put("message", "Resource was deleted successfully");
					return ok(result);
				}
			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	/**
	 * Creates a new WITH resource from the JSON body
	 *
	 * @return the newly created resource
	 */
	// TODO check restrictions (unique fields e.t.c)
	public static Result createRecordResource() {
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
			String resourceType = json.get("resourceType").asText();
			Class<?> clazz = Class.forName("model.resources." + resourceType);
			RecordResource resource = (RecordResource) Json.fromJson(json,
					clazz);
			Set<ConstraintViolation<RecordResource>> violations = Validation
					.getValidator().validate(resource);
			if (!violations.isEmpty()) {
				ArrayNode properties = Json.newObject().arrayNode();
				for (ConstraintViolation<RecordResource> cv : violations) {
					properties.add(Json.parse("{\"" + cv.getPropertyPath()
							+ "\":\"" + cv.getMessage() + "\"}"));
					Logger.info(cv.getPropertyPath() + " : " + cv.getMessage());
				}
				error.put("error", properties);
				return badRequest(error);
			} 
			// Fill with all the administrative metadata
			resource.setResourceType(WithResourceType.valueOf(resourceType));
			resource.getAdministrative().setWithCreator(creator);
			resource.getAdministrative().setCreated(new Date());
			resource.getAdministrative().setLastModified(new Date());
			DB.getRecordResourceDAO().makePermanent(resource);
			resource.getAdministrative().setWithURI(
					request().host() + resource.getDbId().toString());
			// TODO: maybe moderate usage statistics?
			DB.getRecordResourceDAO().makePermanent(resource);
			return ok(Json.toJson(resource));
		} catch (Exception e) {
			error.put("error", e.getMessage());
			return internalServerError(error);
		}
	}

	/**
	 * Edits the WITH resource according to the JSON body. For every field
	 * mentioned in the JSON body it either edits the existing one or it adds it
	 * (in case it doesn't exist)
	 *
	 * @param id
	 * @return the edited resource
	 */
	// TODO check restrictions (unique fields e.t.c)
	public static Result editRecordResource(String id) {
		ObjectNode error = Json.newObject();
		JsonNode json = request().body().asJson();
		try {
			if (json == null) {
				error.put("error", "Invalid JSON");
				return badRequest(error);
			}
			else {
				RecordResource oldResource = DB.getRecordResourceDAO().get(
						new ObjectId(id));
				Result response = errorIfNoAccessToRecord(Action.EDIT, new ObjectId(id));
				if (!response.toString().equals(ok().toString()))
					return response;
				else {
					// TODO change JSON at all its depth
					ObjectMapper objectMapper = new ObjectMapper();
					ObjectReader updator = objectMapper.readerForUpdating(oldResource);
					RecordResource newResource;
					newResource = updator.readValue(json);
					Set<ConstraintViolation<RecordResource>> violations = Validation
							.getValidator().validate(newResource);
					if (!violations.isEmpty()) {
						ArrayNode properties = Json.newObject().arrayNode();
						for (ConstraintViolation<RecordResource> cv : violations) {
							properties.add(Json.parse("{\"" + cv.getPropertyPath()
									+ "\":\"" + cv.getMessage() + "\"}"));
						}
						error.put("error", properties);
						return badRequest(error);
					}
					newResource.getAdministrative().setLastModified(new Date());
					DB.getRecordResourceDAO().makePermanent(newResource);
					return ok(Json.toJson(newResource));
				}
			}
		} catch (Exception e) {
			error.put("error", e.getMessage());
			return internalServerError(error);
		}
	}

	// TODO: Are we checking rights for every record alone?
	public static Result list(String collectionId, int start, int count) {
		List<RecordResource> records = DB.getRecordResourceDAO()
				.getByCollectionBetweenPositions(new ObjectId(collectionId),
						start, start + count);
		return ok(Json.toJson(records));
	}

	// TODO: Remove favorites

	/**
	 * @return
	 */
	public static Result getFavorites() {
		ObjectNode result = Json.newObject();
		if (session().get("user") == null) {
			return forbidden();
		}
		ObjectId userId = new ObjectId(session().get("user"));
		CollectionObject favorite;
		ObjectId favoritesId;
		if ((favorite = DB.getCollectionObjectDAO().getByOwnerAndLabel(userId,
				null, "_favorites")) == null) {
			favoritesId = CollectionObjectController.createFavorites(userId);
		} else {
			favoritesId = favorite.getDbId();
		}
		List<RecordResource> records = DB.getRecordResourceDAO()
				.getByCollection(favoritesId);
		if (records == null) {
			result.put("error", "Cannot retrieve records from database");
			return internalServerError(result);
		}
		ArrayNode recordsList = Json.newObject().arrayNode();
		for (RecordResource record : records) {
			recordsList.add(record.getAdministrative().getExternalId());
		}
		return ok(recordsList);
	}

	public static ObjectNode validateRecord(RecordResource record) {
		ObjectNode result = Json.newObject();
		Set<ConstraintViolation<RecordResource>> violations = Validation
				.getValidator().validate(record);
		if (!violations.isEmpty()) {
			ArrayNode properties = Json.newObject().arrayNode();
			for (ConstraintViolation<RecordResource> cv : violations) {
				properties.add(Json.parse("{\"" + cv.getPropertyPath()
						+ "\":\"" + cv.getMessage() + "\"}"));
			}
			result.put("error", properties);
			return result;
		} else {
			return null;
		}
	}
}