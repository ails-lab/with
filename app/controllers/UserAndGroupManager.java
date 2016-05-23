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

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import model.basicDataTypes.WithAccess.Access;
import model.resources.collection.CollectionObject;
import model.usersAndGroups.User;
import model.usersAndGroups.UserGroup;
import model.usersAndGroups.UserOrGroup;

import org.apache.commons.codec.binary.Base64;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;
import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.MediaObject;
import model.basicDataTypes.WithAccess.Access;
import notifications.GroupNotification;
import notifications.Notification;
import notifications.Notification.Activity;
import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Option;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessManager;
import utils.NotificationCenter;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class UserAndGroupManager extends Controller {

	public static final ALogger log = Logger.of(UserGroup.class);

	/**
	 * Get a list of matching usernames or groupnames
	 *
	 * Used by autocomplete in share collection
	 *
	 * @param prefix
	 *            optional prefix of username or groupname
	 * @param specifyCategory
	 *            0 - return mix Users and UserGroups 1 - return only Users 2 -
	 *            return only Groups
	 * @return JSON document with an array of matching usernames or groupnames
	 *         (or all of them)
	 */
	public static Result listNames(String prefix, Boolean onlyParents,
			Boolean forUsers, Option<String> forGroupType) {
		try {
			ArrayNode suggestions = Json.newObject().arrayNode();

			if (forUsers) {
				List<User> users = DB.getUserDAO().getByUsernamePrefix(prefix);
				for (User user : users) {
					ObjectNode node = Json.newObject();
					ObjectNode data = Json.newObject().objectNode();
					data.put("category", "user");
					// costly?
					node.put("value", user.getUsername());
					node.put("data", data);
					suggestions.add(node);
				}
			}
			if (forGroupType.isDefined()) {
				Class<?> clazz = Class.forName("model.usersAndGroups."
						+ forGroupType.get());
				List<UserGroup> groups = DB.getUserGroupDAO()
						.getByGroupNamePrefix(prefix);
				List<UserGroup> groups2 = DB.getUserGroupDAO()
						.getByFriendlyNamePrefix(prefix);
				groups.addAll(groups2);
				List<String> effectiveUserIds = AccessManager
						.effectiveUserIds(session().get("effectiveUserIds"));
				ObjectId userId = new ObjectId(effectiveUserIds.get(0));
				for (UserGroup group : groups) {
					if (!onlyParents
							|| (onlyParents && group.getUsers()
									.contains(userId))) {
						if (clazz.isInstance(group)) {
							ObjectNode node = Json.newObject().objectNode();
							ObjectNode data = Json.newObject().objectNode();
							data.put("category", "group");
							node.put("value", group.getUsername());
							// check if direct ancestor of user
							/*
							 * if (group.getUsers().contains(userId)) {
							 * data.put("isParent", true); } else
							 * data.put("isParent", false);
							 */
							node.put("value", group.getUsername());
							node.put("data", data);
							suggestions.add(node);
						}
					}
				}

			}
			return ok(suggestions);
		} catch (Exception e) {
			return internalServerError(e.getMessage());
		}
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
				CollectionObject collection = DB.getCollectionObjectDAO()
						.getById(new ObjectId(collectionId));
				if (collection != null) {
					// TODO: have to do recursion here!
					Access accessRights = collection.getAdministrative()
							.getAccess().getAcl(u.getDbId());
					if (accessRights != null)
						userJSON.put("accessRights", accessRights.toString());
					else
						userJSON.put("accessRights", Access.NONE.toString());
				}
			}
			String image = getImageBase64(u);
			if (image != null) {
				userJSON.put("image", image);
			}
			if (u instanceof User)
				userJSON.put("category", "user");
			else if (u instanceof UserGroup)
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
				else
					return badRequest("The string you provided does not match an existing email or username");
			}
		}
	}

	public static Result getUserOrGroupThumbnail(String id) {
		try {
			User user = DB.getUserDAO().getById(new ObjectId(id), null);
			if (user != null) {
				String photoUrl = user.getAvatar().get(MediaVersion.Thumbnail);
				return MediaController.getMediaByUrl(photoUrl,
						MediaVersion.Thumbnail.toString());
			} else {
				UserGroup userGroup = DB.getUserGroupDAO()
						.get(new ObjectId(id));
				if (userGroup != null) {
					String photoUrl = userGroup.getAvatar().get(
							MediaVersion.Thumbnail);
					return MediaController.getMediaByUrl(photoUrl,
							MediaVersion.Thumbnail.toString());
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
	 * Add a user as a member of a group or a group as a child group. Only the
	 * administrators of the group have the right to add users/groups to the
	 * group. This action needs approval by the user/group that it concerns.
	 *
	 * @param id
	 *            the user/group id to add
	 * @param groupId
	 *            the group id
	 * @return success message
	 */
	// TODO: Implement with updates, not make permanent!
	public static Result addUserOrGroupToGroup(String id, String groupId) {
		ObjectNode result = Json.newObject();
		try {
			String adminId = AccessManager.effectiveUserId(session().get(
					"effectiveUserIds"));
			if ((adminId == null) || (adminId.equals(""))) {
				result.put("error",
						"Only administrators of the group have the right to edit the group");
				return forbidden(result);
			}
			if (id.equals(groupId)) {
				result.put("error",
						"Sorry! You cannot add an organization as a member of itself");
				return badRequest(result);
			}
			User admin = DB.getUserDAO().get(new ObjectId(adminId));
			UserGroup group = DB.getUserGroupDAO().get(new ObjectId(groupId));
			if (group == null) {
				result.put("error", "Cannot retrieve group from database");
				return internalServerError(result);
			}
			if (!group.getAdminIds().contains(new ObjectId(adminId))
					&& !admin.isSuperUser()
					&& !group.getCreator().equals(new ObjectId(adminId))) {
				result.put("error",
						"Only administrators of the group have the right to edit the group");
				return forbidden(result);
			}
			Set<ObjectId> ancestorGroups = group.getAncestorGroups();
			ancestorGroups.add(group.getDbId());
			ObjectId userOrGroupId = new ObjectId(id);
			// Add a user to the group
			if (DB.getUserDAO().get(userOrGroupId) != null) {
				User user = DB.getUserDAO().get(userOrGroupId);
				if (user.getUserGroupsIds().contains(group.getDbId())) {
					result.put("error", "User is already member of a group");
					return badRequest(result);
				}

				// add user to the group - database side
				group.getUsers().add(user.getDbId());
				user.addUserGroups(ancestorGroups);
				DB.getUserDAO().makePermanent(user);
				DB.getUserGroupDAO().makePermanent(group);

				List<Notification> requests = DB.getNotificationDAO()
						.getPendingGroupNotifications(user.getDbId(),
								group.getDbId(), Activity.GROUP_REQUEST);
				// If the user has not requested to join to the group, he gets a
				// notification
				if (requests.isEmpty()) {
					List<Notification> invitations = DB.getNotificationDAO()
							.getPendingGroupNotifications(user.getDbId(),
									group.getDbId(), Activity.GROUP_INVITE);
					if (invitations.isEmpty()) {
						// Store notification at the database
						GroupNotification notification = new GroupNotification();
						notification.setActivity(Activity.GROUP_INVITE);
						notification.setGroup(group.getDbId());
						notification.setReceiver(user.getDbId());
						notification.setSender(admin.getDbId());
						notification.setPendingResponse(true);
						Date now = new Date();
						notification.setOpenedAt(new Timestamp(now.getTime()));
						DB.getNotificationDAO().makePermanent(notification);
						// Send notification to the user through socket
						NotificationCenter.sendNotification(notification);
						result.put("message",
								"User succesfully invited to group");
						return ok(result);
					} else {
						result.put("error", "User already invited to group");
						return badRequest(result);
					}
				}
			}
			// Add group as a child of the group
			if (DB.getUserGroupDAO().get(userOrGroupId) != null) {
				UserGroup childGroup = DB.getUserGroupDAO().get(userOrGroupId);
				if (!group.getParentGroups().contains(userOrGroupId))
					childGroup.getParentGroups().add(new ObjectId(groupId));
				else {
					result.put(
							"error",
							"Sorry! The group/organization you are trying to add"
									+ "is already parent of your current group/organization");
					return badRequest(result);
				}

				for (ObjectId userId : childGroup.getUsers()) {
					User user = DB.getUserDAO().get(userId);
					user.addUserGroups(ancestorGroups);
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
			result.put("error", e.getMessage());
			return internalServerError(result);
		}

	}

	public static Result removeUserOrGroupFromGroup(String id, String groupId) {
		ObjectNode result = Json.newObject();
		try {
			String adminId = AccessManager.effectiveUserId(session().get(
					"effectiveUserIds"));
			if ((adminId == null) || (adminId.equals(""))) {
				result.put("error",
						"Only creator or administrators of the group have the right to edit the group");
				return forbidden(result);
			}
			User admin = DB.getUserDAO().get(new ObjectId(adminId));
			UserGroup group = DB.getUserGroupDAO().get(new ObjectId(groupId));
			if (group == null) {
				result.put("error", "Cannot retrieve group from database");
				return internalServerError(result);
			}
			if (!group.getAdminIds().contains(new ObjectId(adminId))
					&& !admin.isSuperUser()
					&& !group.getCreator().equals(new ObjectId(adminId))) {
				result.put("error",
						"Only creator or administrators of the group have the right to edit the group");
				return forbidden(result);
			}
			Set<ObjectId> ancestorGroups = group.getAncestorGroups();
			ObjectId userOrGroupId = new ObjectId(id);
			if (DB.getUserGroupDAO().get(userOrGroupId) != null) {
				UserGroup childGroup = DB.getUserGroupDAO().get(userOrGroupId);
				childGroup.getParentGroups().remove(group.getDbId());
				List<User> users = DB.getUserDAO().getByGroupId(
						childGroup.getDbId());
				for (User user : users) {
					user.removeUserGroups(ancestorGroups);
					DB.getUserDAO().makePermanent(user);
				}
				if (!(DB.getUserGroupDAO().makePermanent(childGroup) == null)) {
					result.put("message",
							"Group succesfully removed from group");
					return ok(result);
				}
			}
			if (DB.getUserDAO().get(userOrGroupId) != null) {
				User user = DB.getUserDAO().get(userOrGroupId);

				// remove the user from the group
				ancestorGroups.add(group.getDbId());
				group.removeUser(user.getDbId());
				user.removeUserGroups(ancestorGroups);
				if (!(DB.getUserDAO().makePermanent(user) == null)
						&& !(DB.getUserGroupDAO().makePermanent(group) == null)) {

					// remove pending invites on this group
					List<Notification> group_invites = DB.getNotificationDAO()
							.getPendingGroupNotifications(user.getDbId(),
									group.getDbId(), Activity.GROUP_INVITE);
					for (Notification not : group_invites) {
						if (DB.getNotificationDAO().makeTransient(not) != 1) {
							log.error("Cannot remove notification with id: "
									+ not.getDbId());
							result.put("error_" + not.getDbId(),
									"Cannot remove notification with id: "
											+ not.getDbId());
						}
					}

					GroupNotification notification = new GroupNotification();
					notification.setActivity(Activity.GROUP_REMOVAL);
					notification.setGroup(group.getDbId());
					notification.setReceiver(user.getDbId());
					notification.setSender(admin.getDbId());
					Date now = new Date();
					notification.setOpenedAt(new Timestamp(now.getTime()));
					DB.getNotificationDAO().makePermanent(notification);
					// Send notification to the user through socket
					NotificationCenter.sendNotification(notification);
					// Notification for the administrators of the group
					notification.setReceiver(group.getDbId());
					notification.setDbId(null);
					DB.getNotificationDAO().makePermanent(notification);
					// Send notification through socket to group
					// administrators
					NotificationCenter.sendNotification(notification);
					result.put("message", "User succesfully removed from group");
					return ok(result);
				} else {
					result.put("error", "Could not remove user from group");
					return internalServerError(result);
				}

			}
			result.put("error", "Wrong user or group id");
			return badRequest(result);

		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	public static Result joinGroup(String groupId) {
		ObjectNode result = Json.newObject();
		try {
			String userId = AccessManager.effectiveUserId(session().get(
					"effectiveUserIds"));
			if (userId == null) {
				result.put("error", "Must specify user for join request");
				return forbidden(result);
			}
			User user = DB.getUserDAO().get(new ObjectId(userId));
			UserGroup group = DB.getUserGroupDAO().get(new ObjectId(groupId));
			if ((group == null) || (user == null)) {
				result.put("error", "Cannot retrieve object from database");
				return internalServerError(result);
			}
			if (user.getUserGroupsIds().contains(group.getDbId())) {
				result.put("error", "User is already member of the group");
				return badRequest(result);
			}
			List<Notification> invites = DB.getNotificationDAO()
					.getPendingGroupNotifications(user.getDbId(),
							group.getDbId(), Activity.GROUP_INVITE);
			// If the user has not been invited to group a join request is sent
			// to the group
			if (invites.isEmpty()) {
				List<Notification> requests = DB.getNotificationDAO()
						.getPendingGroupNotifications(user.getDbId(),
								group.getDbId(), Activity.GROUP_REQUEST);
				if (requests.isEmpty()) {
					GroupNotification notification = new GroupNotification();
					notification.setActivity(Activity.GROUP_REQUEST);
					notification.setGroup(group.getDbId());
					notification.setReceiver(group.getDbId());
					notification.setSender(user.getDbId());
					notification.setPendingResponse(true);
					Date now = new Date();
					notification.setOpenedAt(new Timestamp(now.getTime()));
					DB.getNotificationDAO().makePermanent(notification);
					// Send notification through socket to group administrators
					NotificationCenter.sendNotification(notification);
					result.put("message", "Join request was sent for the group");
					return ok(result);
				} else {
					result.put("error", "User has already sent join request");
					return badRequest(result);
				}
			} else {
				result.put("error", "User is already invited to the group");
				return badRequest(result);
			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	public static Result leaveGroup(String groupId) {
		ObjectNode result = Json.newObject();
		try {
			String userId = AccessManager.effectiveUserId(session().get(
					"effectiveUserIds"));
			if (userId == null) {
				result.put("error", "Must specify user for join request");
				return forbidden(result);
			}
			User user = DB.getUserDAO().get(new ObjectId(userId));
			UserGroup group = DB.getUserGroupDAO().get(new ObjectId(groupId));
			if ((group == null) || (user == null)) {
				result.put("error", "Cannot retrieve object from database");
				return internalServerError(result);
			}
			List<Notification> requests = DB.getNotificationDAO()
					.getPendingGroupNotifications(user.getDbId(),
							group.getDbId(), Activity.GROUP_INVITE);
			if (requests.isEmpty()) {
				Set<ObjectId> ancestorGroups = group.getAncestorGroups();
				ancestorGroups.add(group.getDbId());
				group.removeUser(user.getDbId());
				user.removeUserGroups(ancestorGroups);
				if (!(DB.getUserDAO().makePermanent(user) == null)
						&& !(DB.getUserGroupDAO().makePermanent(group) == null)) {
					GroupNotification notification = new GroupNotification();
					notification.setActivity(Activity.GROUP_REMOVAL);
					notification.setGroup(group.getDbId());
					notification.setReceiver(group.getDbId());
					notification.setSender(user.getDbId());
					Date now = new Date();
					notification.setOpenedAt(new Timestamp(now.getTime()));
					DB.getNotificationDAO().makePermanent(notification);
					// Notification for the administrators of the group

					DB.getNotificationDAO().makePermanent(notification);
					// Send notification through socket to group
					// administrators
					NotificationCenter.sendNotification(notification);
					result.put("message", "User succesfully removed from group");
					return ok(result);
				} else {
					result.put("error", "Could not remove user from group");
					return internalServerError(result);
				}
			} else {
				result.put("error", "User is invited to the group");
				return badRequest(result);
			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}

	}

	public static String getImageBase64(UserOrGroup user) {
		if ((user.getAvatar() != null)
				&& (user.getAvatar().get(MediaVersion.Thumbnail) != null)) {
			String photoUrl = user.getAvatar().get(MediaVersion.Thumbnail);
			MediaObject photo = DB.getMediaObjectDAO().getByUrl(photoUrl);
			if (photo != null)
				// convert to base64 format
				return "data:"
						+ photo.getMimeType()
						+ ";base64,"
						+ new String(Base64.encodeBase64(photo.getMediaBytes()));
		}
		return null;
	}

	public static Result addAdminToGroup(String id, String groupId) {
		ObjectNode result = Json.newObject();
		try {
			String adminId = AccessManager.effectiveUserId(session().get(
					"effectiveUserIds"));
			if ((adminId == null) || (adminId.equals(""))) {
				result.put("error",
						"Only creator or administrators of the group have the right to edit the group");
				return forbidden(result);
			}
			User admin = DB.getUserDAO().get(new ObjectId(adminId));
			UserGroup group = DB.getUserGroupDAO().get(new ObjectId(groupId));
			if (group == null) {
				result.put("error", "Cannot retrieve group from database");
				return internalServerError(result);
			}
			if (!group.getAdminIds().contains(new ObjectId(adminId))
					&& !admin.isSuperUser()
					&& !group.getCreator().equals(new ObjectId(adminId))) {
				result.put("error",
						"Only creator or administrators of the group have the right to edit the group");
				return forbidden(result);
			}
			ObjectId userOrGroupId = new ObjectId(id);
			if (DB.getUserDAO().get(userOrGroupId) != null) {
				User user = DB.getUserDAO().get(userOrGroupId);

				group.addAdministrator(userOrGroupId);
				if (!(DB.getUserGroupDAO().makePermanent(group) == null)) {

					result.put("message", "User  is  a group administrator");
					return ok(result);
				} else {
					result.put("error", "There was an error");
					return internalServerError(result);
				}

			}
			result.put("error", "Wrong user id");
			return badRequest(result);

		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}

	}

	public static Result removeAdminFromGroup(String id, String groupId) {
		ObjectNode result = Json.newObject();
		try {
			String adminId = AccessManager.effectiveUserId(session().get(
					"effectiveUserIds"));
			if ((adminId == null) || (adminId.equals(""))) {
				result.put("error",
						"Only creator or administrators of the group have the right to edit the group");
				return forbidden(result);
			}
			User admin = DB.getUserDAO().get(new ObjectId(adminId));
			UserGroup group = DB.getUserGroupDAO().get(new ObjectId(groupId));
			if (group == null) {
				result.put("error", "Cannot retrieve group from database");
				return internalServerError(result);
			}
			if (!group.getAdminIds().contains(new ObjectId(adminId))
					&& !admin.isSuperUser()
					&& !group.getCreator().equals(new ObjectId(adminId))) {
				result.put("error",
						"Only creator or administrators of the group have the right to edit the group");
				return forbidden(result);
			}
			ObjectId userOrGroupId = new ObjectId(id);
			if (DB.getUserDAO().get(userOrGroupId) != null) {
				User user = DB.getUserDAO().get(userOrGroupId);
				if (group.getAdminIds().contains(userOrGroupId)) {
					group.removeAdministrator(userOrGroupId);
				}
				if (!(DB.getUserGroupDAO().makePermanent(group) == null)) {
					result.put("message", "User is not a group administrator");
					return ok(result);
				} else {
					result.put("error", "There was an error");
					return internalServerError(result);
				}

			}
			result.put("error", "Wrong user id");
			return badRequest(result);

		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}

	}

}
