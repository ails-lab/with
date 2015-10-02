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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.validation.ConstraintViolation;

import model.Collection;
import model.Rights.Access;
import model.User;
import model.UserGroup;

import org.bson.types.ObjectId;
import org.elasticsearch.common.lang3.ArrayUtils;
import org.mongodb.morphia.query.CriteriaContainer;
import org.mongodb.morphia.query.Query;

import play.Logger;
import play.Logger.ALogger;
import play.data.validation.Validation;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessManager;
import utils.Tuple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

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
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		ObjectNode error = Json.newObject();
		try {
			if (json == null) {
				result.put("error", "Invalid JSON");
				return badRequest(result);
			}
			if (!json.has("username")) {
				error.put("username", "Must specify name for the group");
				result.put("error", error);
				return badRequest(result);
			}
			if (!uniqueGroupName(json.get("username").asText())) {
				error.put("username",
						"Group name already exists! Please specify another name.");
				result.put("error", error);
				return badRequest(result);
			}
			Class<?> clazz = Class.forName("model."
					+ capitalizeFirst(groupType));
			newGroup = (UserGroup) Json.fromJson(json, clazz);
			if (adminId != null) {
				admin = new ObjectId(adminId);
			} else if (adminUsername != null) {
				admin = DB.getUserDAO().getByUsername(adminUsername).getDbId();
			} else {
				if ((adminId = AccessManager.effectiveUserId(session().get(
						"effectiveUserIds"))).isEmpty()) {
					result.put("error", "Must specify administrator of group");
					return badRequest(result);
				}
				admin = new ObjectId(AccessManager.effectiveUserId(session()
						.get("effectiveUserIds")));
			}
			newGroup.addAdministrator(admin);
			newGroup.getUsers().add(admin);
			Set<ConstraintViolation<UserGroup>> violations = Validation
					.getValidator().validate(newGroup);
			for (ConstraintViolation<UserGroup> cv : violations) {
				result.put("error",
						"[" + cv.getPropertyPath() + "] " + cv.getMessage());
				return badRequest(result);
			}
			try {
				DB.getUserGroupDAO().makePermanent(newGroup);
				Set<ObjectId> parentGroups = newGroup.getParentGroups();
				parentGroups.add(newGroup.getDbId());
				User user = DB.getUserDAO().get(admin);
				user.addUserGroup(parentGroups);
				DB.getUserDAO().makePermanent(user);
			} catch (Exception e) {
				log.error("Cannot save group to database!", e);
				result.put("error", "Cannot save group to database");
				return internalServerError(result);
			}
			return ok(Json.toJson(newGroup));
		} catch (Exception e) {
			log.error("Cannot create group", e);
			return internalServerError(Json.parse("{\"error\":\""
					+ e.getMessage() + "\"}"));
		}
	}

	private static boolean uniqueGroupName(String name) {
		return (DB.getUserGroupDAO().getByName(name) == null);
	}

	private static String capitalizeFirst(String str) {
		return str.substring(0, 1).toUpperCase()
				+ str.substring(1).toLowerCase();
	}

	/**
	 * Edits group metadata and updates them according to the POST body.
	 * <p>
	 * Only the administrator of the group and the superuser have the right to
	 * edit the group.
	 * 
	 * @param groupId
	 *            the group id
	 * @return the updated group metadata
	 */
	public static Result editGroup(String groupId) {

		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

		String adminId = AccessManager.effectiveUserId(session().get(
				"effectiveUserIds"));
		if ((adminId == null) || (adminId.equals(""))) {
			result.put("error",
					"Only administrator of group has the right to edit the group");
			return forbidden(result);
		}
		try {
			User admin = DB.getUserDAO().get(new ObjectId(adminId));
			UserGroup group = DB.getUserGroupDAO().get(new ObjectId(groupId));
			if (group == null) {
				result.put("error", "Cannot retrieve group from database!");
				return internalServerError(result);
			}
			if (!group.getAdminIds().contains(new ObjectId(adminId))
					&& (!admin.isSuperUser())) {
				result.put("error",
						"Only administrator of group has the right to edit the group");
				return forbidden(result);
			}
			UserGroup oldVersion = group;
			ObjectMapper objectMapper = new ObjectMapper();
			ObjectReader updator = objectMapper.readerForUpdating(oldVersion);
			UserGroup newVersion;
			newVersion = updator.readValue(json);
			Set<ConstraintViolation<UserGroup>> violations = Validation
					.getValidator().validate(newVersion);
			for (ConstraintViolation<UserGroup> cv : violations) {
				result.put("error",
						"[" + cv.getPropertyPath() + "] " + cv.getMessage());
			}
			if (!violations.isEmpty()) {
				return badRequest(result);
			}
			if (!uniqueGroupName(newVersion.getUsername())) {
				result.put("error",
						"Group name already exists! Please specify another name.");
				return badRequest(result);
			}
			// update group on mongo
			if (DB.getUserGroupDAO().makePermanent(newVersion) == null) {
				log.error("Cannot save group to database!");
				result.put("error", "Cannot save group to database!");
				return internalServerError(result);
			}
			return ok(DB.getJson(newVersion));
		} catch (IOException e) {
			log.error("Cannot edit group", e);
			return internalServerError(Json.parse("{\"error\":\""
					+ e.getMessage() + "\"}"));
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

		try {
			DB.getUserGroupDAO().deleteById(new ObjectId(groupId));
		} catch (Exception e) {
			log.error("Cannot delete group from database", e);
			return internalServerError(Json
					.parse("{\"error\":\"Cannot delete group from database\"}"));
		}
		return ok(Json
				.parse("{\"message\":\"Group deleted succesfully from database\"}"));
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
			ObjectNode result = (ObjectNode) Json.parse(DB.getJson(group));
			String image = group.getThumbnailBase64();
			if (image != null) {
				result.put("image", image);
			}
			return ok(result);
		} catch (Exception e) {
			log.error("Cannot retrieve group from database", e);
			return internalServerError(Json
					.parse("{\"error\":\"Cannot retrieve group from database\""));
		}
	}

	/**
	 * Removes a user from the group.
	 * <p>
	 * The users allowed to remove a user from a group is the administrator of
	 * the group, the superuser and the user himself.
	 *
	 * @param userId
	 *            the user id
	 * @param groupId
	 *            the group id
	 * @return success message
	 */
	public static Result removeUserFromGroup(String userId, String groupId) {
		String userSession = AccessManager.effectiveUserId(session().get(
				"effectiveUserIds"));
		if ((userSession == null) || (userSession.equals(""))) {
			return forbidden("No rights for user removal");
		}
		User userS = DB.getUserDAO().get(new ObjectId(userSession));
		UserGroup group = DB.getUserGroupDAO().get(new ObjectId(groupId));
		if (group == null) {
			return internalServerError("Cannot retrieve group from database!");
		}
		if (!group.getAdminIds().contains(new ObjectId(userSession))
				&& (!userS.isSuperUser() && (!userSession.equals(userId)))) {
			return forbidden("No rights for user removal");
		}
		User user = DB.getUserDAO().get(new ObjectId(userId));
		group.getUsers().remove(new ObjectId(userId));
		user.recalculateGroups();
		return ok("User successfully removed from group");

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
				criteria[2] = DB.getCollectionDAO().createQuery().criteria("isPublic").equal(true);
				q.and(criteria);
				Tuple<Integer, Integer> hits = DB.getCollectionDAO().getHits(q, null);
				g.put("totalCollections", hits.x);
				g.put("totalExhiitions", hits.y);
			}
			result.add(g);
		}
		return result;
	}

	// TODO check user rights for these groups
	public static Result getDescendantGroups(String groupId, String groupType,
			boolean direct, boolean collectionHits) {
		List<UserGroup> childrenGroups;
		List<UserGroup> groups;
		UserGroup group;

		ObjectId parentId = new ObjectId(groupId);
		GroupType type = GroupType.valueOf(capitalizeFirst(groupType));

		childrenGroups = DB.getUserGroupDAO().findByParent(parentId, type);
		if (direct) {
			return ok(groupsAsJSON(childrenGroups, new ObjectId(groupId), collectionHits));
		}
		groups = childrenGroups;
		while (!childrenGroups.isEmpty()) {
			group = childrenGroups.remove(0);
			childrenGroups.addAll(DB.getUserGroupDAO().findByParent(
					group.getDbId(), type));
		}
		return ok(groupsAsJSON(groups, new ObjectId(groupId), collectionHits));
	}
}
