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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;

import model.Collection;
import model.CollectionRecord;
import model.basicDataTypes.Language;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.WithAccess.Access;
import model.resources.CollectionObject;
import model.resources.CollectionObject.CollectionAdmin;
import model.resources.RecordResource;
import model.resources.WithResource.WithResourceType;
import model.usersAndGroups.Organization;
import model.usersAndGroups.Page;
import model.usersAndGroups.Project;
import model.usersAndGroups.User;
import model.usersAndGroups.UserGroup;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.data.validation.Validation;
import play.libs.F.Option;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessManager;
import utils.Tuple;
import utils.AccessManager.Action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import controllers.parameterTypes.MyPlayList;
import controllers.parameterTypes.StringTuple;
import db.DB;

/**
 * @author mariaral
 *
 */
public class CollectionObjectController extends WithResourceController {

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
			ObjectId collectionDbId = new ObjectId(id);
			Result response = errorIfNoAccessToCollection(Action.READ, collectionDbId);
			if (!response.equals(ok()))
				return response;
			else {
				CollectionObject collection = DB.getCollectionObjectDAO().get(
						new ObjectId(id));
				List<RecordResource> firstEntries = DB.getCollectionObjectDAO()
						.getFirstEntries(collectionDbId, 5);
				result = (ObjectNode) Json.toJson(collection);
				result.put("firstEntries", Json.toJson(firstEntries));
				return ok(Json.toJson(collection));
			}
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
			ObjectId collectionDbId = new ObjectId(id);
			Result response = errorIfNoAccessToCollection(Action.DELETE, collectionDbId);
			if (!response.equals(ok()))
				return response;
			else {
				CollectionObject collection = DB.getCollectionObjectDAO().get(
						collectionDbId);
				DB.getCollectionObjectDAO().makeTransient(collection);
				result.put("message", "Resource was deleted successfully");
				return ok(result);
			}
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
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		ObjectId collectionDbId = new ObjectId(id);
		try {
			Result response = errorIfNoAccessToCollection(Action.EDIT, collectionDbId);
			if (!response.equals(ok()))
				return response;
			else {
				if (json == null) {
					result.put("error", "Invalid JSON");
					return badRequest(result);
				}
				// TODO change JSON at all its depth
				DB.getCollectionObjectDAO().editCollection(collectionDbId, json);
			}
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
			return ok(Json.toJson(DB.getCollectionObjectDAO().get(collectionDbId)));
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	/*
	public static Result list(Option<String> userOrGroupName,
			Option<String> access, Option<Boolean> isExhibition, int offset, int count) {

		Access accessLevel;
		Boolean isExhibitionBoolean = isExhibition.isDefined() ? isExhibition.get() : null;
		List<CollectionObject> collections;
		accessLevel = Access.valueOf(access.get());
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
							restrictions, isExhibitionBoolean, offset, count);
		} else {
			collections = DB.getCollectionObjectDAO().getByMaxAccess(
					effectiveIds, accessLevel, isExhibitionBoolean, offset, count);
		}
		return ok(Json.toJson(collections));
	}
*/
	
	public static Result list(Option<MyPlayList> directlyAccessedByUserOrGroup,
			Option<MyPlayList> recursivelyAccessedByUserOrGroup, Option<String> creator, Option<Boolean> isPublic,
			Option<Boolean> isExhibition, Boolean collectionHits, int offset, int count) {
		ObjectNode result = Json.newObject().objectNode();
		ArrayNode collArray = Json.newObject().arrayNode();
		List<CollectionObject> userCollections;
		List<String> effectiveUserIds = AccessManager.effectiveUserIds(session().get("effectiveUserIds"));
		List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup = accessibleByUserOrGroup(directlyAccessedByUserOrGroup,
				recursivelyAccessedByUserOrGroup);
		Boolean isExhibitionBoolean = isExhibition.isDefined() ? isExhibition.get() : null;
		ObjectId creatorId = null;
		if (creator.isDefined()) {
			User creatorUser = DB.getUserDAO().getByUsername(creator.get());
			if (creatorUser != null)
				creatorId = creatorUser.getDbId();
		}
		if (effectiveUserIds.isEmpty() || (isPublic.isDefined() && (isPublic.get() == true))) {// not logged or ask for public collections
			// return all public collections
			Tuple<List<CollectionObject>, Tuple<Integer, Integer>> info = DB.getCollectionObjectDAO()
					.getByPublicAndAcl(accessedByUserOrGroup, creatorId, isExhibitionBoolean, collectionHits, offset, count);
			userCollections = info.x;
			if (info.y != null) {
				result.put("totalCollections", info.y.x);
				result.put("totalExhibitions", info.y.y);
			}
			for (CollectionObject collection : userCollections) {
				ObjectNode c = (ObjectNode) Json.toJson(collection);
				if (effectiveUserIds.isEmpty())
					c.put("access", Access.READ.toString());
				collArray.add(c);
			}
			result.put("collectionsOrExhibitions", collArray);
			return ok(result);
		} else { //logged in, check if super user, if not, restrict query to accessible by effectiveUserIds
			Tuple<List<CollectionObject>, Tuple<Integer, Integer>> info;
			if (!AccessManager.isSuperUser(effectiveUserIds.get(0))) 
				info = DB.getCollectionObjectDAO().getByLoggedInUsersAndAcl(AccessManager.toObjectIds(effectiveUserIds), accessedByUserOrGroup, creatorId,
						isExhibitionBoolean, collectionHits, offset, count);
			else
				info = DB.getCollectionObjectDAO().getByAcl(accessedByUserOrGroup, creatorId, isExhibitionBoolean,
						collectionHits, offset, count);
			if (info.y != null) {
				result.put("totalCollections", info.y.x);
				result.put("totalExhibitions", info.y.y);
			}
			List<ObjectNode> collections = collectionWithUserData(info.x, effectiveUserIds);
			for (ObjectNode c : collections)
				collArray.add(c);
			result.put("collectionsOrExhibitions", collArray);
			return ok(result);
		}
	}
	

	// input parameter lists' (directlyAccessedByUserName etc) intended meaning
	// is AND of its entries
	// returned list of lists accessedByUserOrGroup represents AND of OR entries
	// i.e. each entry in directlyAccessedByUserName for example has to be
	// included in a separate list!
	private static List<List<Tuple<ObjectId, Access>>> accessibleByUserOrGroup(
			Option<MyPlayList> directlyAccessedByUserOrGroup, Option<MyPlayList> recursivelyAccessedByUserOrGroup) {
		List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup = new ArrayList<List<Tuple<ObjectId, Access>>>();
		if (directlyAccessedByUserOrGroup.isDefined()) {
			MyPlayList directlyUserNameList = directlyAccessedByUserOrGroup.get();
			for (StringTuple userAccess : directlyUserNameList.list) {
				List<Tuple<ObjectId, Access>> directlyAccessedByUser = new ArrayList<Tuple<ObjectId, Access>>();
				User user = DB.getUserDAO().getByUsername(userAccess.x);
				if (user != null) {
					ObjectId userId = user.getDbId();
					directlyAccessedByUser
							.add(new Tuple<ObjectId, Access>(userId, Access.valueOf(userAccess.y.toUpperCase())));
					accessedByUserOrGroup.add(directlyAccessedByUser);
				}
			}
		}
		if (recursivelyAccessedByUserOrGroup.isDefined()) {
			MyPlayList recursivelyUserNameList = recursivelyAccessedByUserOrGroup.get();
			for (StringTuple userAccess : recursivelyUserNameList.list) {
				List<Tuple<ObjectId, Access>> recursivelyAccessedByUser = new ArrayList<Tuple<ObjectId, Access>>();
				User user = DB.getUserDAO().getByUsername(userAccess.x);
				ObjectId userId = user.getDbId();
				Access access = Access.valueOf(userAccess.y.toUpperCase());
				recursivelyAccessedByUser.add(new Tuple<ObjectId, Access>(userId, access));
				Set<ObjectId> groupIds = user.getUserGroupsIds();
				for (ObjectId groupId : groupIds) {
					recursivelyAccessedByUser.add(new Tuple<ObjectId, Access>(groupId, access));
				}
				accessedByUserOrGroup.add(recursivelyAccessedByUser);
			}
		}
		return accessedByUserOrGroup;
	}

	private static List<ObjectNode> collectionWithUserData(List<CollectionObject> userCollections,
			List<String> effectiveUserIds) {
		List<ObjectNode> collections = new ArrayList<ObjectNode>(userCollections.size());
		Collections.sort(userCollections, new Comparator<CollectionObject>() {
			public int compare(CollectionObject c1, CollectionObject c2) {
				return -c1.getAdministrative().getCreated().compareTo(c2.getAdministrative().getCreated());
			}
		});
		for (CollectionObject collection : userCollections) {
			ObjectNode c = (ObjectNode) Json.toJson(collection);
			Access maxAccess = AccessManager.getMaxAccess(collection.getAdministrative().getAccess(), effectiveUserIds);
			if (!collection.getDescriptiveData().getLabel().get(Language.DEFAULT).equals("_favorites")) {
				if (maxAccess.equals(Access.NONE)) {
					maxAccess = Access.READ;
				}
				c.put("access", maxAccess.toString());
				User user = DB.getUserDAO().getById(collection.getAdministrative().getWithCreator(),
						new ArrayList<String>(Arrays.asList("username")));
				if (user != null) {
					c.put("creator", user.getUsername());
					collections.add(c);
				}
			}
		}
		return collections;
	}
	
	public static void addCollectionToList(int index, List<CollectionObject> collectionsOrExhibitions, List<ObjectId> colls,
			List<String> effectiveUserIds) {
		if (index < colls.size()) {
			ObjectId id = colls.get(index);
			CollectionObject c = DB.getCollectionObjectDAO().getById(id);
			if (effectiveUserIds.isEmpty()) {
				if (c.getAdministrative().getAccess().isPublic())
					collectionsOrExhibitions.add(c);
			} else {
				Access maxAccess = AccessManager.getMaxAccess(c.getAdministrative().getAccess(), effectiveUserIds);
				if (!maxAccess.equals(Access.NONE))
					collectionsOrExhibitions.add(c);
			}
		}
	}

	// If isExhibition is undefined, returns (max) countPerType collections and
	// countPerType exhibitions, i.e. (max) 2*countPerType
	// collectionsOrExhibitions
	public static Result getFeatured(String userOrGroupName, Option<Boolean> isExhibition, int offset,
			int countPerType) {
		Page page = null;
		UserGroup userGroup = DB.getUserGroupDAO().getByName(userOrGroupName);
		if (userGroup != null) {
			if (userGroup instanceof Organization)
				page = ((Organization) userGroup).getPage();
			else if (userGroup instanceof Project)
				page = ((Project) userGroup).getPage();
		} else {
			User user = DB.getUserDAO().getByUsername(userOrGroupName);
			if (user != null) {
				page = user.getPage();
			}
		}
		if (page != null) {
			List<String> effectiveUserIds = AccessManager.effectiveUserIds(session().get("effectiveUserIds"));
			ObjectNode result = Json.newObject().objectNode();
			int start = offset * countPerType;
			int collectionsSize = page.getFeaturedCollections().size();
			int exhibitionsSize = page.getFeaturedExhibitions().size();
			List<CollectionObject> collectionsOrExhibitions = new ArrayList<CollectionObject>();
			if (!isExhibition.isDefined()) {
				for (int i = start; (i < (start + countPerType)) && (i < collectionsSize); i++) {
					addCollectionToList(i, collectionsOrExhibitions, page.getFeaturedCollections(), effectiveUserIds);
					addCollectionToList(i, collectionsOrExhibitions, page.getFeaturedExhibitions(), effectiveUserIds);
				}
			} else {
				if (!isExhibition.get()) {
					for (int i = start; (i < (start + countPerType)) && (i < collectionsSize); i++)
						addCollectionToList(i, collectionsOrExhibitions, page.getFeaturedCollections(),
								effectiveUserIds);
				} else {
					for (int i = start; (i < (start + countPerType)) && (i < exhibitionsSize); i++)
						addCollectionToList(i, collectionsOrExhibitions, page.getFeaturedExhibitions(),
								effectiveUserIds);
				}
			}
			ArrayNode collArray = Json.newObject().arrayNode();
			List<ObjectNode> collections = collectionWithUserData(collectionsOrExhibitions, effectiveUserIds);
			for (ObjectNode c : collections)
				collArray.add(c);
			result.put("totalCollections", collectionsSize);
			result.put("totalExhibitions", exhibitionsSize);
			result.put("collectionsOrExhibitions", collArray);
			// TODO: put collection and exhibition hits in response
			return ok(result);
		} else
			return badRequest(
					"User or group with name " + userOrGroupName + " does not exist or has no specified page.");

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
	
	/**
	 * List all Records from a Collection using a start item and a page size
	 */
	public static Result listRecordResources(String collectionId, String contentFormat, int start, int count) {
		ObjectNode result = Json.newObject();
		ObjectId colId = new ObjectId(collectionId);
		//TODO: don't have to get the whiole collection, use DAO method
		//Collection collection = DB.getCollectionDAO().getById(colId);
		Result response = errorIfNoAccessToCollection(Action.READ, colId);
		if (!response.equals(ok()))
			return response;
		else {
			List<RecordResource> records = DB.getRecordResourceDAO().getByCollectionBetweenPositions(colId, start, count);
			if (records == null) {
				result.put("message", "Cannot retrieve records from database!");
				return internalServerError(result);
			}
			ArrayNode recordsList = Json.newObject().arrayNode();
			for (RecordResource e : records) {
				if (contentFormat.equals("contentOnly")) {
					recordsList.add(Json.toJson(e.getContent()));
				}
				else {
					if (contentFormat.equals("noContent")) {
						e.getContent().clear();
					}
					else if (e.getContent().containsKey(contentFormat)) {
						HashMap<String, String> newContent = new HashMap<String, String>(1);
						newContent.put(contentFormat, (String) e.getContent().get(contentFormat));
						e.setContent(newContent);
					}
					recordsList.add(Json.toJson(e));
				}
			}
			result.put("itemCount", ((CollectionAdmin) ((CollectionObject) DB.getCollectionObjectDAO().getById(colId, 
					new ArrayList<String>(Arrays.asList("administrative.entryCount")))).getAdministrative()).getEntryCount());
			result.put("records", recordsList);
			return ok(result);
		}
	}
}
