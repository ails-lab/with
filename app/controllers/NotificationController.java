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
import java.util.HashSet;
import java.util.Set;
import model.usersAndGroups.User;
import model.usersAndGroups.UserGroup;
import notifications.Notification;
import notifications.Notification.Activity;
import notifications.GroupNotification;
import notifications.ResourceNotification;
import notifications.ResourceNotification.ShareInfo;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import utils.NotificationCenter;
import actors.NotificationActor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class NotificationController extends WithController {
	public static final ALogger log = Logger.of(NotificationController.class);

	public static WebSocket<JsonNode> socket() {
		return WebSocket.withActor(NotificationActor::props);
	}

	public static Result groupNotification(Notification notification, User user, boolean accept, ObjectId sender, ObjectId receiver, Activity activity) {
		ObjectNode result = Json.newObject();
		GroupNotification groupNotification = null;
		if (notification instanceof GroupNotification) {
			groupNotification = (GroupNotification) notification;
			ObjectId groupId = groupNotification.getGroup();
			UserGroup group = DB.getUserGroupDAO().get(groupId);
			Set<ObjectId> ancestorGroups;
			Date now = new Date();
			if (!accept) {
				// The user is added to the group
				/*ancestorGroups = group.getAncestorGroups();
				ancestorGroups.add(group.getDbId());
				group.getUsers().add(group.getDbId());
				user.addUserGroups(ancestorGroups);
				DB.getUserDAO().makePermanent(user);
				DB.getUserGroupDAO().makePermanent(group);*/
				ancestorGroups = group.getAncestorGroups();
				ancestorGroups.add(group.getDbId());
				group.removeUser(user.getDbId());
				user.removeUserGroups(ancestorGroups);
				DB.getUserDAO().makePermanent(user);
				DB.getUserGroupDAO().makePermanent(group);
			}
			notification.setPendingResponse(false);
			notification.setReadAt(new Timestamp(now.getTime()));
			DB.getNotificationDAO().makePermanent(notification);
			GroupNotification newNotification = new GroupNotification();
			if (accept) {
				newNotification.setActivity(Activity.GROUP_REQUEST_ACCEPT);
			} else {
				newNotification.setActivity(Activity.GROUP_REQUEST_DENIED);
			}
			newNotification.setGroup(group.getDbId());
			newNotification.setSender(sender);
			newNotification.setOpenedAt(new Timestamp(now.getTime()));
			// Notification for the administrators of the group
			newNotification.setReceiver(receiver);
			newNotification.setDbId(null);
			DB.getNotificationDAO().makePermanent(newNotification);
			// Send notification through socket (to user and group
			// administrators)
			NotificationCenter.sendNotification(newNotification);
			result.put("message",
					"Succesfully reponded to user request");
			return ok(result);
		}
		else {
			result.put("error", "User is not authorized for this action");
			return internalServerError(result);
		}
	}
	
	public static Result respondToRequest(String notificationId, boolean accept) {
		ObjectNode result = Json.newObject();
		try {
			String currentUserId = loggedInUser();
			if (currentUserId == null) {
				result.put("error", "User is not authorized for this action");
				return forbidden(result);
			}
			User currentUser = DB.getUserDAO().get(new ObjectId(currentUserId));
			Notification notification = DB.getNotificationDAO().get(
					new ObjectId(notificationId));
			if (!notification.isPendingResponse()) {
				result.put("error", "Notification does not require acceptance");
				return badRequest(result);
			}
			ObjectId receiver = notification.getReceiver();
			if (!receiver.equals(currentUser.getDbId())
					&& !currentUser.getAdminInGroups().contains(receiver)
					&& !currentUser.isSuperUser()) {
				result.put("error", "User is not authorized for this action");
				return forbidden(result);
			}
			Date now = new Date();
			//Notification newNotification;
			GroupNotification groupNotification;
			ResourceNotification resourceNotification;
			Activity activity;
			switch (notification.getActivity()) {
			case GROUP_INVITE:
				activity = accept ? Activity.GROUP_INVITE_ACCEPT: Activity.GROUP_INVITE_DECLINED;
				ObjectId groupId = ((GroupNotification) notification).getGroup();
				return groupNotification(notification, currentUser, accept, currentUser.getDbId(), groupId,  activity);
			case GROUP_REQUEST:
				activity = accept ? Activity.GROUP_REQUEST_ACCEPT: Activity.GROUP_REQUEST_DENIED;
				ObjectId userId = notification.getSender();
				User user = DB.getUserDAO().get(userId);
				return groupNotification(notification, user, accept, currentUser.getDbId(), user.getDbId(), activity);
			case COLLECTION_SHARE: //notification has been sent only in case of upgrade
				if (notification instanceof ResourceNotification) {
					resourceNotification = (ResourceNotification) notification;
					ShareInfo shareInfo = resourceNotification.getShareInfo();
					activity = accept ? Activity.COLLECTION_SHARED : Activity.COLLECTION_REJECTED;
					resourceNotification.setPendingResponse(false);
					resourceNotification.setReadAt(new Timestamp(now.getTime()));
					DB.getNotificationDAO().makePermanent(notification);
					if (!accept) {
						//TODO: downgrade to previous access
						ObjectId resourceId = resourceNotification.getResource();
						RightsController.changeAccess(resourceId, shareInfo.getUserOrGroup(), shareInfo.getPreviousAccess(), shareInfo.getOwnerEffectiveIds(), 
								true, false);
						ResourceNotification newNotification = new ResourceNotification();
						newNotification.setActivity(Activity.COLLECTION_REJECTED);
						newNotification.setResource(resourceNotification.getResource());
						newNotification.setShareInfo(shareInfo);
						newNotification.setSender(notification.getReceiver());
						newNotification.setReceiver(notification.getSender());
						newNotification.setOpenedAt(new Timestamp(now.getTime()));
						DB.getNotificationDAO().makePermanent(newNotification);
						NotificationCenter.sendNotification(newNotification);
					}
					result.put("message",
							"User succesfully responded to collection share request");
					return ok(result);
				}
				else  {
					result.put("error", "User is not authorized for this action");
					return internalServerError(result);
				}
				
			default:
				result.put("error", "Notification does not require acceptance");
				return badRequest(result);
			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}

	}

	public static Result getUserNotifications() {
		try {
			ObjectId userId = new ObjectId(loggedInUser());
			Set<ObjectId> userOrGroupIds = new HashSet<ObjectId>();
			Set<ObjectId> groups = DB.getUserDAO().get(userId)
					.getAdminInGroups();
			userOrGroupIds.add(userId);
			userOrGroupIds.addAll(groups);
			Set<Notification> unreadNotifications = new HashSet<Notification>(
					DB.getNotificationDAO().getUnreadByReceivers(
							userOrGroupIds, 0));
			Set<Notification> notifications;
			if (unreadNotifications.size() < 20) {
				notifications = new HashSet<Notification>(DB
						.getNotificationDAO().getAllByReceivers(userOrGroupIds,
								20 - unreadNotifications.size()));
				notifications.addAll(unreadNotifications);
			} else {
				notifications = unreadNotifications;
			}
			return ok(Json.toJson(notifications));
		} catch (Exception e) {
			ObjectNode result = Json.newObject();
			result.put("error", e.getMessage());
			return internalServerError(result);

		}
	}

	public static Result readNotifications() {
		ObjectNode result = Json.newObject();
		ArrayNode error = Json.newObject().arrayNode();
		ArrayNode json = (ArrayNode) request().body().asJson();
		Notification notification;
		for (JsonNode id : json) {
			try {
				notification = DB.getNotificationDAO().get(
						new ObjectId(id.asText()));
				if ((notification.getReadAt() == null)
						&& (notification.isPendingResponse() == false)) {
					Date now = new Date();
					notification.setReadAt(new Timestamp(now.getTime()));
					DB.getNotificationDAO().makePermanent(notification);
				}
			} catch (Exception e) {
				error.add(id.asText());
			}
		}
		if (error.size() > 0) {
			result.put("error", "Could not mark as read some notifications ");
			result.put("ids", error);
			return ok(result);
		}
		result.put("message", "All notifications are marked as read");
		return ok(result);
	}

	public static Result sendMessage(String receiverId) {
		ObjectId sender = new ObjectId(loggedInUser());
		JsonNode json = request().body().asJson();
		if (!json.has("message")) {
			ObjectNode error = Json.newObject();
			error.put("error", "Empty message");
			return badRequest(error);
		}
		String message = json.get("messsage").asText();
		ObjectId receiver = new ObjectId(receiverId);
		Notification notification = new Notification();
		notification.setActivity(Activity.MESSAGE);
		notification.setMessage(message);
		notification.setSender(sender);
		notification.setReceiver(receiver);
		Date now = new Date();
		notification.setOpenedAt(new Timestamp(now.getTime()));
		DB.getNotificationDAO().makePermanent(notification);
		NotificationCenter.sendNotification(notification);
		return ok();
	}

}
