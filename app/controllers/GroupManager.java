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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import model.basicDataTypes.WithAccess.Access;
import model.resources.collection.CollectionObject;
import model.usersAndGroups.Organization;
import model.usersAndGroups.Page;
import model.usersAndGroups.Project;
import model.usersAndGroups.User;
import model.usersAndGroups.UserGroup;
import model.usersAndGroups.UserOrGroup;

import org.bson.types.ObjectId;
import org.mongodb.morphia.geo.GeoJson;
import org.mongodb.morphia.geo.Point;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.Query;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import sources.core.HttpConnector;
import utils.AccessManager;
import utils.Tuple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DAO.QueryOperator;
import db.DB;

@SuppressWarnings({ "unchecked", "rawtypes" })
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
	public static Result createGroup(String adminId, String adminUsername,
			String groupType) {

		ObjectId admin;
		UserGroup newGroup = null;
		ObjectNode error = Json.newObject();
		JsonNode json = request().body().asJson();
		try {
			if (json == null) {
				error.put("error", "Invalid JSON");
				return badRequest(error);
			}
			if (AccessManager
					.effectiveUserId(session().get("effectiveUserIds"))
					.isEmpty()) {
				error.put("error", "No rights for group creation");
				return forbidden(error);
			}
			ObjectId creator = new ObjectId(
					AccessManager.effectiveUserId(session().get(
							"effectiveUserIds")));
			if (!json.has("username")) {
				error.put("error", "Must specify name for the group");
				return badRequest(error);
			} else if(json.get("username").asText().length() < 3) {
				error.put("error", "Username of " + groupType + " must contain at least 3 characters");
				return badRequest(error);
			}
			if(json.has("friendlyName") &&
				(json.get("friendlyName").asText().length() < 3)) {
				error.put("error", "Short Name of  " + groupType + " must contain at least 3 characters");
				return badRequest(error);
			}

			if (!uniqueGroupName(json.get("username").asText())) {
				error.put("error",
						"Group name already exists! Please specify another name");
				return badRequest(error);
			}
			Class<?> clazz = Class.forName("model.usersAndGroups." + groupType);
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
			newGroup.setCreated(new Date());
			try {
				DB.getUserGroupDAO().makePermanent(newGroup);
				Set<ObjectId> parentGroups = newGroup.getParentGroups();
				parentGroups.add(newGroup.getDbId());
				User administrator = DB.getUserDAO().get(creator);
				administrator.addGroupForAdministration(newGroup.getDbId());
				administrator.addUserGroups(parentGroups);
				DB.getUserDAO().makePermanent(administrator);
				administrator = DB.getUserDAO().get(admin);
				administrator.addGroupForAdministration(newGroup.getDbId());
				administrator.addUserGroups(parentGroups);
				DB.getUserDAO().makePermanent(administrator);
			} catch (Exception e) {
				log.error("Cannot save group to database!", e.getMessage());
				error.put("error", "Cannot save group to database!");
				return internalServerError(error);
			}
			updatePage(newGroup.getDbId(), json);
			return ok(Json.toJson(newGroup));
		} catch (Exception e) {
			error.put("error", e.getMessage());
			return internalServerError(error);
		}
	}

	private static boolean uniqueGroupName(String name) {
		return ((DB.getUserGroupDAO().getByName(name) == null) && (DB
				.getUserDAO().getByUsername(name) == null));
	}

	private static String capitalizeFirst(String str) {
		return str.substring(0, 1).toUpperCase()
				+ str.substring(1).toLowerCase();
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
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		ObjectId groupDbId = new ObjectId(groupId);
		UserGroup group = DB.getUserGroupDAO().get(groupDbId);
		if (group == null) {
			result.put("error", "Cannot retrieve group from database.");
			return internalServerError(result);
		}
		ObjectId userId = new ObjectId(AccessManager.effectiveUserIds(
				session().get("effectiveUserIds")).get(0));
		User user = DB.getUserDAO().get(userId);
		Set<ObjectId> groupAdmins = group.getAdminIds();
		if (!groupAdmins.contains(userId) && !user.isSuperUser()) {
			result.put("error",
					"Only an admin of the group has the right to edit the group.");
			return forbidden(result);
		}
		if (json.has("username") && (json.get("username") != null)
				&& !group.getUsername().equals(json.get("username").asText())) {
			if (!uniqueGroupName(json.get("username").asText())) {
				result.put("error",
						"Group name already exists! Please specify another name.");
				return badRequest(result);
			}
		}
		DB.getUserGroupDAO().editGroup(groupDbId, json);
		updatePage(groupDbId, json);
		return ok(Json.toJson(DB.getUserGroupDAO().get(groupDbId)));
	}



	private static void updatePage(ObjectId groupId, JsonNode json) {
		UserGroup group = DB.getUserGroupDAO().get(groupId);
		if (!json.has("page") || (!(group instanceof Organization)
				&& !(group instanceof Project)))
			return;
		Page newPage = Json.fromJson(json.get("page"), Page.class);
		if ((newPage.getAddress() == null) && (newPage.getCity() == null)
				&& (newPage.getCountry() == null))
			return;
		Page oldPage = null;
		// Keep previous page fields
		if (group instanceof Organization)
			oldPage = ((Organization) group).getPage();
		else if (group instanceof Project)
			oldPage = ((Project) group).getPage();

		// In case that the location has changed we need to calculate the
		// new coordinates
		String address = (newPage.getAddress() != null) ? newPage.getAddress()
				: oldPage.getAddress();
		String city = (newPage.getCity() != null) ? newPage.getCity() : oldPage
				.getCity();
		String country = (newPage.getCountry() != null) ? newPage.getCountry()
				: oldPage.getCountry();
		String fullAddress = ((address == null) ? "" : address) + ","
				+ ((city == null) ? "" : city) + ","
				+ ((country == null) ? "" : country);
		fullAddress = fullAddress.replace(" ", "+");
		try {
			JsonNode response = HttpConnector.getWSHttpConnector()
					.getURLContent("https://maps.googleapis.com/maps/api/geocode/json?address="
							+ fullAddress);
			Point coordinates = GeoJson.point(
					response.get("results").get(0).get("geometry")
							.get("location").get("lat").asDouble(),
					response.get("results").get(0).get("geometry")
							.get("location").get("lng").asDouble());
			DB.getUserGroupDAO().updatePageCoordinates(groupId, coordinates);
		} catch (Exception e) {
			log.error("Cannot update coordinates of group Page", e);
			DB.getUserGroupDAO().updatePageCoordinates(groupId, null);

		}
	}

	/*
	 * This method get's as input a list of Collection and Exhibition
	 * id's and updates the featuredCollections & featuredExhibitions
	 * lists at a Page.
	 */
	public static Result updateFeatured(String groupId) {

		ObjectNode result = Json.newObject();
		JsonNode json = request().body().asJson();

		if(groupId == null) {
			result.put("error",
					"Invalid groupId specified!");
			return badRequest(result);
		}

		List<String> fCollections = json.findValuesAsText("fCollections");
		List<String> fExhibitions = json.findValuesAsText("fExhibitions");
		if(((fCollections == null) || (fCollections.size() == 1)) &&
			((fExhibitions == null) || (fExhibitions.size() == 1))) {
			result.put("success",
					"Nothing to update!");
			return ok(result);
		}

		UserGroup ug = DB.getUserGroupDAO().get(new ObjectId(groupId));
		Page pg = null;
		if(ug instanceof Organization) {
			pg = ((Organization)ug).getPage();
		} else if(ug instanceof Project) {
			pg = ((Project)ug).getPage();
		} else {
			result.put("error",
					"UserGroup is not an Organization nor a Project");
			return badRequest(result);
		}


		if(fCollections != null) {
			List<ObjectId> fCols = new ArrayList<ObjectId>();
			fCollections.forEach(  id -> fCols.add(new ObjectId(id)) );
			pg.setFeaturedCollections(fCols);
		}

		if(fExhibitions != null) {
			List<ObjectId> fExhs = new ArrayList<ObjectId>();
			fExhibitions.forEach(  id -> fExhs.add(new ObjectId(id)) );
			pg.setFeaturedExhibitions(fExhs);
		}

		if(ug instanceof Organization) {
			((Organization)ug).setPage(pg);
		} else {
			((Project)ug).setPage(pg);
		}

		if(DB.getUserGroupDAO().makePermanent(ug) != null) {
			result.put("success", "Featured Data succesfully updated!");
			return ok(result);
		} else {
			result.put("error", "Featured Data were not updated due to system error");
			return internalServerError(result);
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
		String userId = AccessManager.effectiveUserId(session().get(
				"effectiveUserIds"));
		if ((userId == null) || (userId.equals(""))) {
			result.put("error",
					"Only creator of the group has the right to delete the group");
			return forbidden(result);
		}
		try {
			UserGroup group = DB.getUserGroupDAO().get(new ObjectId(groupId));
			if (!group.getCreator().equals(new ObjectId(userId))) {
				result.put("error",
						"Only creator of the group has the right to delete the group");
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
				CollectionObject collection = DB.getCollectionObjectDAO()
						.getById(new ObjectId(collectionId));
				if (collection != null) {
					Access accessRights = collection.getAdministrative()
							.getAccess().getAcl(group.getDbId());
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

	public static ArrayNode groupsAsJSON(List<UserGroup> groups,
			ObjectId restrictedById, boolean collectionHits) {
		ArrayNode result = Json.newObject().arrayNode();
		for (UserGroup group : groups) {
			ObjectNode g = (ObjectNode) Json.toJson(group);
			if (collectionHits) {
				Query<CollectionObject> q = DB.getCollectionObjectDAO()
						.createQuery();
				Criteria criteria1 = DB.getCollectionObjectDAO()
						.formAccessLevelQuery(
								new Tuple(restrictedById, Access.READ),
								QueryOperator.GTE);
				Criteria criteria2 = DB.getCollectionObjectDAO()
						.formAccessLevelQuery(
								new Tuple(group.getDbId(), Access.WRITE),
								QueryOperator.GTE);
				// Criteria criteria3 =
				// DB.getCollectionObjectDAO().createQuery()
				// .criteria("administrative.access.isPublic").equal(true);
				q.and(criteria1, criteria2);
				Tuple<Integer, Integer> hits = DB.getCollectionObjectDAO()
						.getHits(q, Optional.ofNullable(null));
				g.put("totalCollections", hits.x);
				g.put("totalExhibitions", hits.y);
			}
			boolean add = true;
			for(int i=0; i<result.size(); i++) {
				if(group.getDbId().toString().equals(result.get(i).get("dbId").asText())) {
					add=false;
					break;
				}
			}
			if(add)
				result.add(g);
		}
		return result;
	}

	public static Set<UserGroup> recursiveDescendants(Set<UserGroup> list,
			GroupType type) {
		Set<UserGroup> descendantGroups = new HashSet<UserGroup>();
		for (UserGroup group : list) {
			List<UserGroup> descendants = DB.getUserGroupDAO().findByParent(
					group.getDbId(), type);
			if (descendants != null) {
				Set<UserGroup> descendantsSet = recursiveDescendants(
						new HashSet<UserGroup>(descendants), type);
				descendantGroups.addAll(descendantsSet);
			}
			descendantGroups.add(group);
		}
		return descendantGroups;
	}

	/**
	 * Return child groups or all descedant groups according to group type.
	 *
	 * @param groupId
	 * @param groupType
	 * @param direct
	 * @param collectionHits
	 * @return
	 */
	public static Result getDescendantGroups(String groupId, String groupType,
			boolean direct, boolean collectionHits) {
		List<UserGroup> childrenGroups;
		List<UserGroup> descendantGroups;
		ObjectId parentId = new ObjectId(groupId);
		GroupType type = GroupType.valueOf(capitalizeFirst(groupType));
		childrenGroups = DB.getUserGroupDAO().findByParent(parentId, type);
		if (childrenGroups != null) {
			if (direct) {
				return ok(groupsAsJSON(childrenGroups, new ObjectId(groupId),
						collectionHits));
			} else {
				descendantGroups = new ArrayList<UserGroup>(
						recursiveDescendants(new HashSet<UserGroup>(
								childrenGroups), type));
				return ok(groupsAsJSON(descendantGroups, new ObjectId(groupId),
						collectionHits));
			}
		} else
			return ok();
	}

	/**
	 * This call returns extra info about members of a group either they are
	 * users or groups.
	 *
	 * Category specifies either if we want only users information or groups or
	 * both.
	 *
	 * @param groupId
	 * @param category
	 *            (possible values: 'users', 'groups', 'both'
	 * @return A json stucture like the following { "users": [ {...},
	 *         {...},...], "groups": [ {...}, {...},...] }
	 */
	public static Result getGroupUsersInfo(String groupId, String category) {

		ObjectNode result = Json.newObject();
		ArrayNode users = Json.newObject().arrayNode();
		ArrayNode groups = Json.newObject().arrayNode();

		if (!category.equals("users") && !category.equals("groups")
				&& !category.equals("both")) {

			result.put("message", "Invalid category name");
			log.error("Invalid category name");
			return badRequest(result);
		}

		UserGroup group;
		if ((group = DB.getUserGroupDAO().get(new ObjectId(groupId))) == null) {
			result.put("message", "There is no such a group");
			log.error("There is no such a group");
			return badRequest(result);
		}
		if ((category.equals("users") || category.equals("both"))
				&& (group.getUsers().size() > group.getAdminIds().size())) {
			group.getUsers().removeAll(group.getAdminIds());
			User u;
			for (ObjectId oid : group.getUsers()) {
				if ((u = DB.getUserDAO().get(oid)) == null) {
					log.error("No User with dbId: " + oid);
				}
				users.add(userOrGroupJson(u));
			}
		}
		if ((category.equals("groups") || category.equals("both"))) {
			List<UserGroup> children = DB.getUserGroupDAO().findByParent(
					new ObjectId(groupId), GroupType.All);
			for (UserGroup g : children) {
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

	public static ArrayNode userGroupsToJSON(List<UserGroup> groups) {
		ArrayNode result = Json.newObject().arrayNode();
		for (UserGroup group : groups) {
			ObjectNode g = (ObjectNode) Json.toJson(group);

			ObjectId userId = AccessManager.effectiveUserDbId(session().get(
					"effectiveUserIds"));
			User user = DB.getUserDAO().get(userId);
			g.put("firstName", user.getFirstName());
			g.put("lastName", user.getLastName());
			Query<CollectionObject> q = DB.getCollectionObjectDAO()
					.createQuery();
			// Criteria criteria1 =
			// DB.getCollectionObjectDAO().formAccessLevelQuery(new
			// Tuple(restrictedById, Access.READ), QueryOperator.GTE);
			Criteria criteria2 = DB.getCollectionObjectDAO()
					.formAccessLevelQuery(
							new Tuple(group.getDbId(), Access.READ),
							QueryOperator.GT);
			// Criteria criteria3 = DB.getCollectionObjectDAO().createQuery()
			// .criteria("administrative.access.isPublic").equal(true);
			// q.and(criteria1, criteria2);
			q.and(criteria2);
			Tuple<Integer, Integer> hits = DB.getCollectionObjectDAO().getHits(
					q, Optional.ofNullable(null));
			ObjectNode count = Json.newObject();
			count.put("Collections", hits.x);
			count.put("Exhibitions", hits.y);
			g.put("count", count);
			result.add(g);
		}
		return result;
	}

	public static Result listUserGroups(String groupType, int offset,
			int count, boolean belongsOnly) {
		List<UserGroup> groups = new ArrayList<UserGroup>();
		try {
			GroupType type = GroupType.valueOf(groupType);
			ObjectId userId = AccessManager.effectiveUserDbId(session().get(
					"effectiveUserIds"));
			if (userId == null) {
				groups = DB.getUserGroupDAO().findPublic(type, offset, count);
				return ok(Json.toJson(groups));
			}
			User user = DB.getUserDAO().get(userId);
			Set<ObjectId> userGroupsIds = user.getUserGroupsIds();
			groups = DB.getUserGroupDAO().findByIds(userGroupsIds, type,
					offset, count);
			if (groups.size() == count)
				return ok(userGroupsToJSON(groups));
			int userGroupCount = DB.getUserGroupDAO().getGroupCount(
					userGroupsIds, type);
			if (offset < userGroupCount)
				offset = 0;
			else
				offset = offset - userGroupCount;
			count = count - groups.size();
			if (!belongsOnly)
				groups.addAll(DB.getUserGroupDAO().findPublicWithRestrictions(
						type, offset, count, userGroupsIds));
			return ok(userGroupsToJSON(groups));
			// return ok(Json.toJson(groups));
		} catch (Exception e) {
			return ok(userGroupsToJSON(groups));
			// return ok(Json.toJson(groups));
		}
	}
}