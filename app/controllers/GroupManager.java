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

import java.util.Set;

import javax.validation.ConstraintViolation;

import model.User;
import model.UserGroup;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.data.validation.Validation;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessManager;

import com.fasterxml.jackson.databind.JsonNode;

import db.DB;

public class GroupManager extends Controller {

	public static final ALogger log = Logger.of(UserGroup.class);

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
	public static Result createGroup(String adminId, String adminUsername) {

		ObjectId admin;
		UserGroup newGroup = null;
		JsonNode json = request().body().asJson();

		if (json == null) {
			return badRequest("Invalid JSON");
		}
		if (!json.has("name")) {
			return badRequest("Must specify name for the group");
		}
		if (!uniqueGroupName(json.get("name").asText())) {
			return badRequest("Group name already exists");
		}
		newGroup = Json.fromJson(json, UserGroup.class);
		if (adminId != null) {
			admin = new ObjectId(adminId);
		} else if (adminUsername != null) {
			admin = DB.getUserDAO().getByUsername(adminUsername).getDbId();
		} else {
			if ((adminId = AccessManager.effectiveUserId(session().get(
					"effectiveUserIds"))).isEmpty()) {
				return internalServerError("Must specify administrator of group");
			}
			admin = new ObjectId(AccessManager.effectiveUserId(session().get(
					"effectiveUserIds")));
		}
		newGroup.setAdministrator(admin);
		newGroup.getUsers().add(admin);
		Set<ConstraintViolation<UserGroup>> violations = Validation
				.getValidator().validate(newGroup);
		for (ConstraintViolation<UserGroup> cv : violations) {
			return badRequest("[" + cv.getPropertyPath() + "] "
					+ cv.getMessage());
		}
		try {
			DB.getUserGroupDAO().makePermanent(newGroup);
			Set<ObjectId> parentGroups = newGroup.retrieveParents();
			parentGroups.add(newGroup.getDbId());
			User user = DB.getUserDAO().get(admin);
			user.addUserGroup(parentGroups);
			DB.getUserDAO().makePermanent(user);
		} catch (Exception e) {
			log.error("Cannot save group to database!", e);
			return internalServerError("Cannot save group to database!");
		}
		return ok(Json.toJson(newGroup));
	}

	private static boolean uniqueGroupName(String name) {
		return (DB.getUserGroupDAO().getByName(name) == null);
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
			log.error("Cannot delete group from database!", e);
			return internalServerError("Cannot delete group from database!");
		}
		return ok("Group deleted succesfully from database");
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
			return ok(DB.getJson(group));
		} catch (Exception e) {
			log.error("Cannot retrieve group from database!", e);
			return internalServerError("Cannot retrieve group from database!");
		}
	}

	/**
	 * Adds a user to group.
	 * <p>
	 * Right now only the administrator of the group and the superuser have the
	 * rights to add a group to the group.
	 *
	 * @param userId
	 *            the user id
	 * @param groupId
	 *            the group id
	 * @return success message
	 */
	public static Result addUserToGroup(String userId, String groupId) {

		String adminId = AccessManager.effectiveUserId(session().get(
				"effectiveUserIds"));
		if ((adminId == null) || (adminId.equals(""))) {
			return forbidden("Only administrator of group has the right to add users");
		}
		User admin = DB.getUserDAO().get(new ObjectId(adminId));
		UserGroup group = DB.getUserGroupDAO().get(new ObjectId(groupId));
		if (group == null) {
			return internalServerError("Cannot retrieve group from database!");
		}
		if (!group.getAdministrator().equals(new ObjectId(adminId))
				&& (!admin.isSuperUser())) {
			return forbidden("Only administrator of group has the right to add users");
		}
		User user = DB.getUserDAO().get(new ObjectId(userId));
		group.getUsers().add(new ObjectId(userId));
		Set<ObjectId> parentGroups = group.retrieveParents();

		if (user == null) {
			return internalServerError("Cannot retrieve user from database!");
		}
		parentGroups.add(group.getDbId());
		user.addUserGroup(parentGroups);

		if (!(DB.getUserDAO().makePermanent(user) == null)
				&& !(DB.getUserGroupDAO().makePermanent(group) == null)) {
			return ok("Group succesfully added to User");
		}
		return internalServerError("Cannot store to database!");

	}
	
	public static Result editGroup(String groupId) {
		return TODO;

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
		return TODO;

	}
}
