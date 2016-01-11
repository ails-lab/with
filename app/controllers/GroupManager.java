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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.validation.ConstraintViolation;

import org.bson.types.ObjectId;
import org.mongodb.morphia.geo.GeoJson;
import org.mongodb.morphia.geo.Point;
import org.mongodb.morphia.query.CriteriaContainer;
import org.mongodb.morphia.query.Query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;
import model.Collection;
import model.basicDataTypes.WithAccess.Access;
import model.usersAndGroups.Organization;
import model.usersAndGroups.Page;
import model.usersAndGroups.Project;
import model.usersAndGroups.User;
import model.usersAndGroups.UserGroup;
import model.usersAndGroups.UserOrGroup;
import play.Logger;
import play.Logger.ALogger;
import play.data.validation.Validation;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import sources.core.HttpConnector;
import utils.AccessManager;
import utils.Tuple;

public class GroupManager extends Controller {

	public static final ALogger log = Logger.of(UserGroup.class);

	public enum GroupType {
		All, Organization, Project, UserGroup
	}

	/**
	 * Creates a {@link UserGroup} with the specified user as administrator and
	 * with the given body as JSON.
	 * <p>
	 * The name of the group must be unique. If the administrator is not
	 * provided as a parameter the administrator of the group becomes the user
	 * who made the call.
	 *
	 * @param adminId
	 *            the administrator id
	 * @param adminUsername
	 *            the administrator username
	 * @return the JSON of the new group
	 */
	public static Result createGroup(String adminId, String adminUsername, String groupType) {

		ObjectId admin;
		UserGroup newGroup = null;
		ObjectNode error = Json.newObject();
		JsonNode json = request().body().asJson();
		try {
			if (json == null) {
				error.put("error", "Invalid JSON");
				return badRequest(error);
			}
			if (AccessManager.effectiveUserId(session().get("effectiveUserIds")).isEmpty()) {
				error.put("error", "No rights for group creation");
				return forbidden(error);
			}
			ObjectId creator = new ObjectId(AccessManager.effectiveUserId(session().get("effectiveUserIds")));
			if (!json.has("username")) {
				error.put("error", "Must specify name for the group");
				return badRequest(error);
			}
			if (!uniqueGroupName(json.get("username").asText())) {
				error.put("error", "Group name already exists! Please specify another name");
				return badRequest(error);
			}
			Class<?> clazz = Class.forName("model.usersAndGroups" + groupType);
			newGroup = (UserGroup) Json.fromJson(json, clazz);
			if (adminId != null) {
				admin = new ObjectId(adminId);
			} else if (adminUsername != null) {
				admin = DB.getUserDAO().getByUsername(adminUsername).getDbId();
			} else {
				admin = creator;
			}
			if (newGroup.getCreator() == null) {
				newGroup.setCreator(creator);
			}
			newGroup.addAdministrator(creator);
			newGroup.addAdministrator(admin);
			newGroup.getUsers().add(creator);
			newGroup.getUsers().add(admin);
			Set<ConstraintViolation<UserGroup>> violations = Validation.getValidator().validate(newGroup);
			if (!violations.isEmpty()) {
				ArrayNode properties = Json.newObject().arrayNode();
				for (ConstraintViolation<UserGroup> cv : violations) {
					properties.add(Json.parse("{\"" + cv.getPropertyPath() + "\":\"" + cv.getMessage() + "\"}"));
				}
				error.put("error", properties);
				return badRequest(error);
			}
			try {
				DB.getUserGroupDAO().makePermanent(newGroup);
				Set<ObjectId> parentGroups = newGroup.getParentGroups();
				parentGroups.add(newGroup.getDbId());
				User user = DB.getUserDAO().get(admin);
				user.addUserGroups(parentGroups);
				DB.getUserDAO().makePermanent(user);
			} catch (Exception e) {
				log.error("Cannot save group to database!", e.getMessage());
				error.put("error", "Cannot save group to database!");
				return internalServerError(error);
			}
			return ok(Json.toJson(newGroup));
		} catch (Exception e) {
			error.put("error", e.getMessage());
			return internalServerError(error);
		}
	}

	private static boolean uniqueGroupName(String name) {
		return ((DB.getUserGroupDAO().getByName(name) == null) && (DB.getUserDAO().getByUsername(name) == null));
	}

	private static String capitalizeFirst(String str) {
		return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
	}

	/**
	 * Edits group metadata and updates them according to the JSON body.
	 * <p>
	 * Only the creator of the group has the right to edit the group.
	 *
	 * @param groupId
	 *            the group id
	 * @return the updated group metadata
	 */
	public static Result editGroup(String groupId) {

		ObjectNode json = (ObjectNode) request().body().asJson();
		ObjectNode result = Json.newObject();

		String adminId = AccessManager.effectiveUserId(session().get("effectiveUserIds"));
		if ((adminId == null) || (adminId.equals(""))) {
			result.put("error", "Only creator of the group has the right to edit the group");
			return forbidden(result);
		}
		try {
			User admin = DB.getUserDAO().get(new ObjectId(adminId));
			UserGroup group = DB.getUserGroupDAO().get(new ObjectId(groupId));
			if (group == null) {
				result.put("error", "Cannot retrieve group from database!");
				return internalServerError(result);
			}
			if (!group.getCreator().equals(new ObjectId(adminId)) && (!admin.isSuperUser())) {
				result.put("error", "Only creator of group has the right to edit the group");
				return forbidden(result);
			}
			if (json.has("username")) {
				if (json.get("username") != null) {
					if (!group.getUsername().equals(json.get("username").asText())) {
						if (!uniqueGroupName(json.get("username").asText())) {
							return badRequest("Group name already exists! Please specify another name.");
						}
					}
				}
			}
			// Update user page
			if (json.has("page") && ((group instanceof Organization) || (group instanceof Project))) {
				String address = null, city = null, country = null;
				Page oldPage = null;
				JsonNode newPage = json.get("page");
				// Keep previous page fields
				if (group instanceof Organization) {
					oldPage = ((Organization) group).getPage();
				} else if (group instanceof Project) {
					oldPage = ((Project) group).getPage();
				}
				// Update Page
				ObjectMapper pageObjectMapper = new ObjectMapper();
				ObjectReader pageUpdator = pageObjectMapper.readerForUpdating(oldPage);
				Page page;
				page = pageUpdator.readValue(newPage);
				// In case that the location has changed we need to calculate
				// the new coordinates
				if (((json.get("page").get("address") != null) || (json.get("page").get("city") != null)
						|| (json.get("page").get("country") != null))) {
					address = page.getAddress();
					city = page.getCity();
					country = page.getCountry();
					String fullAddress = ((address == null) ? "" : address) + "," + ((city == null) ? "" : city) + ","
							+ ((country == null) ? "" : country);
					fullAddress = fullAddress.replace(" ", "+");
					try {
						JsonNode response = HttpConnector.getURLContent(
								"https://maps.googleapis.com/maps/api/geocode/json?address=" + fullAddress);
						Point coordinates = GeoJson.point(
								response.get("results").get(0).get("geometry").get("location").get("lat").asDouble(),
								response.get("results").get(0).get("geometry").get("location").get("lng").asDouble());
						/*coordinates.setLatitude(
								response.get("results").get(0).get("geometry").get("location").get("lat").asDouble());
						coordinates.setLongitude(
								response.get("results").get(0).get("geometry").get("location").get("lng").asDouble());*/
						page.setCoordinates(coordinates);
					} catch (Exception e) {
						log.error("Cannot update coordinates of group Page", e);
						page.setCoordinates(null);
					}
				}
				json.remove("page");
				if (group instanceof Organization) {
					((Organization) group).setPage(page);
				} else if (group instanceof Project) {
					((Project) group).setPage(page);
				}
			}
			UserGroup oldVersion = group;
			ObjectMapper objectMapper = new ObjectMapper();
			ObjectReader updator = objectMapper.readerForUpdating(oldVersion);
			UserGroup newVersion;
			newVersion = updator.readValue(json);
			Set<ConstraintViolation<UserGroup>> violations = Validation.getValidator().validate(newVersion);
			if (!violations.isEmpty()) {
				ArrayNode properties = Json.newObject().arrayNode();
				for (ConstraintViolation<UserGroup> cv : violations) {
					properties.add(Json.parse("{\"" + cv.getPropertyPath() + "\":\"" + cv.getMessage() + "\"}"));
				}
				result.put("error", properties);
				return badRequest(result);
			}

			// update group on mongo
			if (DB.getUserGroupDAO().makePermanent(newVersion) == null) {
				log.error("Cannot save group to database!");
				return internalServerError("Cannot save group to database!");
			}
			return ok(Json.toJson(newVersion));
		} catch (

		IOException e)

		{
			e.printStackTrace();
			return internalServerError(e.getMessage());
		}

	}

	/**
	 * Deletes a group from the database. The users who participate are not
	 * deleted as well.
	 *
	 * @param groupId
	 *            the group id
	 * @return success message
	 */
	public static Result deleteGroup(String groupId) {

		ObjectNode result = Json.newObject();
		String userId = AccessManager.effectiveUserId(session().get("effectiveUserIds"));
		if ((userId == null) || (userId.equals(""))) {
			result.put("error", "Only creator of the group has the right to delete the group");
			return forbidden(result);
		}
		try {
			UserGroup group = DB.getUserGroupDAO().get(new ObjectId(groupId));
			if (!group.getCreator().equals(new ObjectId(userId))) {
				result.put("error", "Only creator of the group has the right to delete the group");
				return forbidden(result);
			}
			Set<ObjectId> ancestorGroups = group.getAncestorGroups();
			ancestorGroups.add(group.getDbId());
			List<User> users = DB.getUserDAO().getByGroupId(group.getDbId());
			for (User user : users) {
				user.removeUserGroups(ancestorGroups);
				DB.getUserDAO().makePermanent(user);
			}
			DB.getUserGroupDAO().deleteById(new ObjectId(groupId));
		} catch (Exception e) {
			log.error("Cannot delete group from database!", e);
			result.put("error", "Cannot delete group from database!");
			return internalServerError(result);
		}
		result.put("message", "Group deleted succesfully from database");
		return ok(result);
	}

	/**
	 * Gets the group.
	 *
	 * @param groupId
	 *            the group id
	 * @return the group
	 */
	public static Result getGroup(String groupId) {
		try {
			UserGroup group = DB.getUserGroupDAO().get(new ObjectId(groupId));
			return ok(Json.toJson(group));
		} catch (Exception e) {
			log.error("Cannot retrieve group from database!", e);
			return internalServerError("Cannot retrieve group from database!");
		}
	}

	public static Result getUserOrGroupThumbnail(String id) {
		try {
			User user = DB.getUserDAO().getById(new ObjectId(id), null);
			if (user != null) {
				ObjectId photoId = user.getThumbnail();
				return MediaController.getMetadataOrFile(photoId.toString(), true);
			} else {
				UserGroup userGroup = DB.getUserGroupDAO().get(new ObjectId(id));
				if (userGroup != null) {
					ObjectId photoId = user.getThumbnail();
					return MediaController.getMetadataOrFile(photoId.toString(), true);
				} else
					return badRequest(Json.parse("{\"error\":\"User does not exist\"}"));
			}
		} catch (Exception e) {
			return badRequest(Json.parse("{\"error\":\"" + e.getMessage() + "\"}"));
		}
	}

	/**
	 * @param name
	 *            the group name
	 * @return the result
	 */
	public static Result findByGroupName(String name, String collectionId) {
		Function<UserGroup, Status> getGroupJson = (UserGroup group) -> {
			ObjectNode groupJSON = Json.newObject();
			groupJSON.put("groupId", group.getDbId().toString());
			groupJSON.put("username", group.getUsername());
			groupJSON.put("about", group.getAbout());
			if (collectionId != null) {
				Collection collection = DB.getCollectionDAO().getById(new ObjectId(collectionId));
				if (collection != null) {
					Access accessRights = collection.getRights().get(group.getDbId());
					if (accessRights != null)
						groupJSON.put("accessRights", accessRights.toString());
					else
						groupJSON.put("accessRights", Access.NONE.toString());
				}
			}
			return ok(groupJSON);
		};
		UserGroup group = DB.getUserGroupDAO().getByName(name);
		return getGroupJson.apply(group);
	}

	public static ArrayNode groupsAsJSON(List<UserGroup> groups, ObjectId restrictedById, boolean collectionHits) {
		ArrayNode result = Json.newObject().arrayNode();
		for (UserGroup group : groups) {
			ObjectNode g = (ObjectNode) Json.toJson(group);
			if (collectionHits) {
				Query<Collection> q = DB.getCollectionDAO().createQuery();
				CriteriaContainer[] criteria =  new CriteriaContainer[3];
				criteria[0] = DB.getCollectionDAO().createQuery().criteria("rights." + restrictedById.toHexString()).greaterThanOrEq(1);
				criteria[1] = DB.getCollectionDAO().createQuery().criteria("rights." + group.getDbId().toHexString()).equal(3);
				criteria[2] = DB.getCollectionDAO().createQuery().criteria("rights.isPublic").equal(true);
				q.and(criteria);
				Tuple<Integer, Integer> hits = DB.getCollectionDAO().getHits(q, null);
				g.put("totalCollections", hits.x);
				g.put("totalExhibitions", hits.y);
			}
			result.add(g);
		}
		return result;
	}

	public static Set<UserGroup> recursiveDescendants(Set<UserGroup> list, GroupType type) {
		Set<UserGroup> descendantGroups = new HashSet<UserGroup>();
		for (UserGroup group: list) {
			List<UserGroup> descendants = DB.getUserGroupDAO().findByParent(group.getDbId(), type);
			if ( descendants != null) {
				Set<UserGroup> descendantsSet = recursiveDescendants(new HashSet<UserGroup>(descendants), type);
				descendantGroups.addAll(descendantsSet);
			}
			descendantGroups.add(group);
		}
		return descendantGroups;
	}


	/**
	 * Return child groups or all descedant groups according to group type.
	 * @param groupId
	 * @param groupType
	 * @param direct
	 * @param collectionHits
	 * @return
	 */
	public static Result getDescendantGroups(String groupId, String groupType, boolean direct, boolean collectionHits) {
		List<UserGroup> childrenGroups;
		List<UserGroup> descendantGroups;
		ObjectId parentId = new ObjectId(groupId);
		GroupType type = GroupType.valueOf(capitalizeFirst(groupType));
		childrenGroups = DB.getUserGroupDAO().findByParent(parentId, type);

		if (childrenGroups != null) {
			if (direct) {
				return ok(groupsAsJSON(childrenGroups, new ObjectId(groupId), collectionHits));
			}
			else {
				descendantGroups = new ArrayList<UserGroup>(recursiveDescendants(new HashSet<UserGroup>(childrenGroups), type));
				return ok(groupsAsJSON(descendantGroups, new ObjectId(groupId), collectionHits));
			}
		}
		else
			return ok();
	}

	/**
	 * This call returns extra info about members of a group either they are
	 * users or groups.
	 *
	 * Category specifies either if we want only users information or groups or both.
	 *
	 * @param groupId
	 * @param category (possible values: 'users', 'groups', 'both'
	 * @return A json stucture  like the following
	 * 			{
	 * 				"users": [ {...}, {...},...],
	 * 				"groups": [ {...}, {...},...]
	 * 			}
	 */
	public static Result getGroupUsersInfo(String groupId, String category) {

		ObjectNode result = Json.newObject();
		ArrayNode users = Json.newObject().arrayNode();
		ArrayNode groups = Json.newObject().arrayNode();

		if(!category.equals("users")
			&& !category.equals("groups")
			&& !category.equals("both")) {

			result.put("message", "Invalid category name");
			log.error("Invalid category name");
			return badRequest(result);
			}

		UserGroup group;
		if( (group = DB.getUserGroupDAO().get(new ObjectId(groupId))) == null) {
			result.put("message", "There is no such a group");
			log.error("There is no such a group");
			return badRequest(result);
		}
		if((category.equals("users") || category.equals("both"))
				&& (group.getUsers().size() > group.getAdminIds().size())) {
			group.getUsers().removeAll(group.getAdminIds());
			User u;
			for(ObjectId oid : group.getUsers()) {
				if((u = DB.getUserDAO().get(oid)) == null) {
					log.error("Not a User with dbId: " + oid);
				}
				users.add(userOrGroupJson(u));
			}
		}
		if((category.equals("groups") || category.equals("both"))) {
			List<UserGroup> children =
					DB.getUserGroupDAO().findByParent(new ObjectId(groupId), GroupType.All);
			for(UserGroup g: children) {
				groups.add(userOrGroupJson(g));
			}
		}



		result.put("users", users);
		result.put("groups", groups);
		return ok(result);
	}


	private static ObjectNode userOrGroupJson(UserOrGroup user) {
		ObjectNode userJSON = Json.newObject();
		userJSON.put("userId", user.getDbId().toString());
		userJSON.put("username", user.getUsername());
		if (user instanceof User) {
			userJSON.put("category", "user");
			userJSON.put("firstName", ((User) user).getFirstName());
			userJSON.put("lastName", ((User) user).getLastName());
		} else
			userJSON.put("category", "group");
		String image = UserAndGroupManager.getImageBase64(user);
		if (image != null) {
			userJSON.put("image", image);
		}
		return userJSON;
	}
}
