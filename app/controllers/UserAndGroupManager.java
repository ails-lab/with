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

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import model.Collection;
import model.Media;
import model.Rights.Access;
import model.User;
import model.UserGroup;
import model.UserOrGroup;

import org.apache.commons.codec.binary.Base64;
import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessManager;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class UserAndGroupManager extends Controller {

	public static final ALogger log = Logger.of(UserGroup.class);

	/**
	 * Get a list of matching usernames or groupnames
	 *
	 * Used by autocomplete in share collection
	 *
	 * @param prefix
	 *            optional prefix of username or groupname
	 * @return JSON document with an array of matching usernames or groupnames
	 *         (or all of them)
	 */
	public static Result listNames(String prefix, Boolean onlyParents) {
		List<User> users = DB.getUserDAO().getByUsernamePrefix(prefix);
		List<UserGroup> groups = DB.getUserGroupDAO().getByGroupNamePrefix(
				prefix);
		ArrayNode suggestions = Json.newObject().arrayNode();
		for (User user : users) {
			ObjectNode node = Json.newObject();
			ObjectNode data = Json.newObject().objectNode();
			data.put("category", "user");
			// costly?
			node.put("value", user.getUsername());
			node.put("data", data);
			suggestions.add(node);
		}
		List<String> effectiveUserIds = AccessManager
				.effectiveUserIds(session().get("effectiveUserIds"));
		ObjectId userId = new ObjectId(effectiveUserIds.get(0));
		for (UserGroup group : groups) {
			if (!onlyParents
					|| (onlyParents && group.getUsers().contains(userId))) {
				ObjectNode node = Json.newObject().objectNode();
				ObjectNode data = Json.newObject().objectNode();
				data.put("category", "group");
				node.put("value", group.getUsername());
				// check if direct ancestor of user
				/*
				 * if (group.getUsers().contains(userId)) { data.put("isParent",
				 * true); } else data.put("isParent", false);
				 */
				node.put("value", group.getUsername());
				node.put("data", data);
				suggestions.add(node);
			}
		}

		return ok(suggestions);
	}

	/**
	 * @param userOrGroupnameOrEmail
	 * @return User and image
	 */
	public static Result findByUserOrGroupNameOrEmail(
			String userOrGroupnameOrEmail, String collectionId) {
		Function<UserOrGroup, Status> getUserJson = (UserOrGroup u) -> {
			ObjectNode userJSON = Json.newObject();
			userJSON.put("userId", u.getDbId().toString());
			userJSON.put("username", u.getUsername());
			if (u instanceof User) {
				userJSON.put("firstName", ((User) u).getFirstName());
				userJSON.put("lastName", ((User) u).getLastName());
			}
			if (collectionId != null) {
				Collection collection = DB.getCollectionDAO().getById(
						new ObjectId(collectionId));
				if (collection != null) {
					// TODO: have to do recursion here!
					Access accessRights = collection.getRights().get(
							u.getDbId());
					if (accessRights != null)
						userJSON.put("accessRights", accessRights.toString());
					else
						userJSON.put("accessRights", Access.NONE.toString());
				}
			}
			String image = u.getThumbnailBase64();
			if (image != null) {
				userJSON.put("image", image);
			}
			if (u instanceof User)
				userJSON.put("category", "user");
			if (u instanceof UserGroup)
				userJSON.put("category", "group");
			return ok(userJSON);
		};
		User user = DB.getUserDAO().getByEmail(userOrGroupnameOrEmail);
		if (user != null) {
			return getUserJson.apply(user);
		} else {
			user = DB.getUserDAO().getByUsername(userOrGroupnameOrEmail);
			if (user != null) {
				return getUserJson.apply(user);
			} else {
				UserGroup userGroup = DB.getUserGroupDAO().getByName(
						userOrGroupnameOrEmail);
				if (userGroup != null)
					return getUserJson.apply(userGroup);
				else {
					return badRequest(Json
							.parse("{\"error\":\"The string you provided does not match an existing email or username\"}"));
				}
			}
		}
	}

	public static Result getUserOrGroupThumbnail(String id) {
		try {
			User user = DB.getUserDAO().getById(new ObjectId(id), null);
			if (user != null) {
				ObjectId photoId = user.getThumbnail();
				return MediaController.getMetadataOrFile(photoId.toString(),
						true);
			} else {
				UserGroup userGroup = DB.getUserGroupDAO()
						.get(new ObjectId(id));
				if (userGroup != null) {
					ObjectId photoId = user.getThumbnail();
					return MediaController.getMetadataOrFile(
							photoId.toString(), true);
				} else
					return badRequest(Json
							.parse("{\"error\":\"User does not exist\"}"));
			}
		} catch (Exception e) {
			return badRequest(Json.parse("{\"error\":\"" + e.getMessage()
					+ "\"}"));
		}
	}

	/**
	 * Adds a user or group to group.
	 * <p>
	 * Right now only the administrator of the group and the superuser have the
	 * rights to add a user/group to the group.
	 *
	 * @param id
	 *            the user/group id to be added
	 * @param groupId
	 *            the group id
	 * @return success message
	 */
	public static Result addUserOrGroupToGroup(String id, String groupId) {
		try {
			ObjectNode result = Json.newObject();
			String adminId = AccessManager.effectiveUserId(session().get(
					"effectiveUserIds"));
			if ((adminId == null) || (adminId.equals(""))) {
				result.put("error",
						"Only administrator of group has the right to edit the group");
				return forbidden(result);
			}
			User admin = DB.getUserDAO().get(new ObjectId(adminId));
			UserGroup group = DB.getUserGroupDAO().get(new ObjectId(groupId));
			if (group == null) {
				result.put("error", "Cannot retrieve group from database");
				return internalServerError(result);
			}
			if (!group.getAdminIds().contains(new ObjectId(adminId))
					&& (!admin.isSuperUser())) {
				result.put("error",
						"Only administrator of group has the right to edit the group");
				return forbidden(result);
			}
			Set<ObjectId> ancestorGroups = group.getAncestorGroups();
			ancestorGroups.add(group.getDbId());
			ObjectId userOrGroupId = new ObjectId(id);
			if (DB.getUserDAO().get(userOrGroupId) != null) {
				User user = DB.getUserDAO().get(userOrGroupId);
				group.getUsers().add(user.getDbId());
				user.addUserGroup(ancestorGroups);
				if (!(DB.getUserDAO().makePermanent(user) == null)
						&& !(DB.getUserGroupDAO().makePermanent(group) == null)) {
					result.put("message", "User succesfully added to group");
					return ok(result);
				}
			}
			if (DB.getUserGroupDAO().get(userOrGroupId) != null) {
				UserGroup childGroup = DB.getUserGroupDAO().get(userOrGroupId);
				childGroup.getParentGroups().add(group.getDbId());
				for (ObjectId userId : childGroup.getUsers()) {
					User user = DB.getUserDAO().get(userId);
					user.addUserGroup(ancestorGroups);
					DB.getUserDAO().makePermanent(user);
				}
				if (!(DB.getUserGroupDAO().makePermanent(childGroup) == null)) {
					result.put("message", "Group succesfully added to group");
					return ok(result);
				}
			}
			result.put("error", "Wrong user or group id");
			return badRequest(result);

		} catch (Exception e) {
			return internalServerError(Json.parse("{\"error\":\""
					+ e.getMessage() + "\"}"));
		}
	}
}
