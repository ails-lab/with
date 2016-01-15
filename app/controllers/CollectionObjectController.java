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

import model.basicDataTypes.KeyValuesPair.MultiLiteral;
import model.basicDataTypes.WithAccess.Access;
import model.resources.CollectionObject;
import model.resources.CollectionObject.CollectionAdmin;
import model.resources.RecordResource;
import model.resources.WithResource.WithResourceType;
import model.usersAndGroups.User;

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

/**
 * @author mariaral
 *
 */
public class CollectionObjectController extends Controller {

	public static final ALogger log = Logger
			.of(CollectionObjectController.class);

	/**
	 * Creates a new WITH resource from the JSON body
	 * 
	 * @param exhibition
	 * @return the newly created resource
	 */
	// TODO check restrictions (unique fields e.t.c)
	public static Result createCollectionObject(boolean exhibition) {
		ObjectNode error = Json.newObject();
		JsonNode json = request().body().asJson();
		try {
			if (exhibition == false && json == null) {
				error.put("error", "Invalid JSON");
				return badRequest(error);
			}
			if (session().get("user") == null) {
				error.put("error", "No rights for WITH resource creation");
				return forbidden(error);
			}
			ObjectId creatorDbId = new ObjectId(session().get("user"));
			User creator = DB.getUserDAO().get(creatorDbId);
			CollectionObject collection = Json.fromJson(json,
					CollectionObject.class);
			if (exhibition) {
				collection.getDescriptiveData().setLabel(
						getAvailableTitle(creator));
				collection.getDescriptiveData().setDescription(
						new MultiLiteral("Description"));
				creator.addExhibitionsCreated();
				DB.getUserDAO().makePermanent(creator);
			}
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
			collection.getAdministrative().setWithCreator(creatorDbId);
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

	/* Find a unique dummy title for the user exhibition */
	/**
	 * @param user
	 * @return
	 */
	private static MultiLiteral getAvailableTitle(User user) {
		int exhibitionNum = user.getExhibitionsCreated();
		return new MultiLiteral("DummyTitle" + exhibitionNum);
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
			List<RecordResource> firstEntries = DB.getCollectionObjectDAO().getFirstEntries(new ObjectId(id), 5);
			result = (ObjectNode) Json.toJson(collection);
			result.put("firstEntries", Json.toJson(firstEntries));
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
	 * @param id
	 * @return the edited resource
	 */
	// TODO check restrictions (unique fields e.t.c)
	public static Result editCollectionObject(String id) {
		ObjectNode error = Json.newObject();
		JsonNode json = request().body().asJson();
		ObjectId dbId = new ObjectId(id);
		try {
			if (json == null) {
				error.put("error", "Invalid JSON");
				return badRequest(error);
			}
			// TODO: check rights from DAO
			CollectionObject oldCollection = DB.getCollectionObjectDAO().get(
					dbId);
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
			DB.getCollectionObjectDAO().editCollection(dbId, json);
			/*
			 * ObjectMapper objectMapper = new ObjectMapper(); ObjectReader
			 * updator = objectMapper .readerForUpdating(oldCollection);
			 * CollectionObject newCollection; newCollection =
			 * updator.readValue(json);
			 * Set<ConstraintViolation<CollectionObject>> violations =
			 * Validation .getValidator().validate(newCollection); if
			 * (!violations.isEmpty()) { ArrayNode properties =
			 * Json.newObject().arrayNode(); for
			 * (ConstraintViolation<CollectionObject> cv : violations) {
			 * properties.add(Json.parse("{\"" + cv.getPropertyPath() + "\":\""
			 * + cv.getMessage() + "\"}")); } error.put("error", properties);
			 * return badRequest(error); }
			 * newCollection.getAdministrative().setLastModified(new Date());
			 * DB.getCollectionObjectDAO().makePermanent(newCollection);
			 */
			return ok(Json.toJson(DB.getCollectionObjectDAO().get(dbId)));
		} catch (Exception e) {
			error.put("error", e.getMessage());
			return internalServerError(error);
		}
	}

	public static Result list(Option<String> userOrGroupName,
			Option<String> access, boolean exhibitions, int offset, int count) {

		Access accessLevel;
		List<CollectionObject> collections;
		if (access.isDefined()) {
			accessLevel = Access.valueOf(access.get());
		} else {
			accessLevel = Access.OWN;
		}
		ObjectId userOrGroupId;
		List<ObjectId> effectiveIds = AccessManager
				.effectiveUserDbIds(session().get("effectiveUserIds"));
		if (userOrGroupName.isDefined()) {
			String name = userOrGroupName.get();
			if (DB.getUserGroupDAO().getByName(name) != null) {
				userOrGroupId = DB.getUserGroupDAO().getByName(name).getDbId();
			} else {
				userOrGroupId = DB.getUserDAO().getByUsername(name).getDbId();
			}
			HashMap<ObjectId, Access> restrictions = new HashMap<ObjectId, Access>();
			restrictions.put(userOrGroupId, accessLevel);
			collections = DB.getCollectionObjectDAO()
					.getByMaxAccessWithRestrictions(effectiveIds, accessLevel,
							restrictions, exhibitions, offset, count);
		} else {
			collections = DB.getCollectionObjectDAO().getByMaxAccess(
					effectiveIds, accessLevel, exhibitions, offset, count);
		}
		return ok(Json.toJson(collections));
	}

	/**
	 * @return
	 */
	public static Result getFavoriteCollection() {
		ObjectId userId = new ObjectId(session().get("user"));
		String fav = DB.getCollectionObjectDAO()
				.getByOwnerAndLabel(userId, null, "_favorites").getDbId()
				.toString();
		return getCollectionObject(fav);

	}
}
