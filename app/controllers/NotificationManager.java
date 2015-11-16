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

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;
import model.Notification;
import model.Notification.Activity;
import model.User;
import model.UserGroup;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessManager;
import utils.NotificationCenter;

public class NotificationManager extends Controller {

	public static final ALogger log = Logger.of(NotificationManager.class);

	public static Result accept(String notificationId) {
		ObjectNode result = Json.newObject();
		try {
			String userId = AccessManager.effectiveUserId(session().get("effectiveUserIds"));
			if (userId == null) {
				result.put("error", "User is not authorized for this action");
				return forbidden(result);
			}
			User user = DB.getUserDAO().get(new ObjectId(userId));
			Notification notification = DB.getNotificationDAO().get(new ObjectId(notificationId));
			ObjectId receiver = notification.getReceiver();
			if (!receiver.equals(user.getDbId()) && !user.getAdminInGroups().contains(receiver)) {
				result.put("error", "User is not authorized for this action");
				return forbidden(result);
			}
			switch (notification.getActivity()) {
			case GROUP_INVITE:
				ObjectId groupId = notification.getGroup();
				UserGroup group = DB.getUserGroupDAO().get(groupId);
				// The user is added to the group
				Set<ObjectId> ancestorGroups = group.getAncestorGroups();
				ancestorGroups.add(group.getDbId());
				group.getUsers().add(user.getDbId());
				user.addUserGroups(ancestorGroups);
				DB.getUserDAO().makePermanent(user);
				DB.getUserGroupDAO().makePermanent(group);
				// Mark the invitation from the group as closed with the
				// appropriate timestamp
				notification.setPendingResponse(false);
				Date now = new Date();
				notification.setReadAt(new Timestamp(now.getTime()));
				DB.getNotificationDAO().makePermanent(notification);
				// Notification for the user join to the group
				// Notification for the administrators of the group
				Notification newNotification = new Notification();
				newNotification.setActivity(Activity.GROUP_INVITE_ACCEPT);
				newNotification.setGroup(group.getDbId());
				newNotification.setReceiver(group.getDbId());
				newNotification.setSender(user.getDbId());
				newNotification.setOpenedAt(new Timestamp(now.getTime()));
				DB.getNotificationDAO().makePermanent(newNotification);
				NotificationCenter.sendNotification(newNotification);
				// Notification for the user itself
				newNotification.setReceiver(user.getDbId());
				newNotification.setDbId(null);
				DB.getNotificationDAO().makePermanent(newNotification);
				// Send notification through socket
				NotificationCenter.sendNotification(newNotification);
				result.put("message", "User succesfully added to group");
				return ok(result);
			case GROUP_REQUEST:
				break;
			case COLLECTION_REQUEST_SHARING:
				break;
			default:
				result.put("error", "Notification does not require acceptance");
				return badRequest(result);
			}
			result.put("message", "Notification was accepted");
			return ok(result);
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}

	}

	public static Result reject(String notificationId) {
		ObjectNode result = Json.newObject();
		try {
			String userId = AccessManager.effectiveUserId(session().get("effectiveUserIds"));
			if (userId == null) {
				result.put("error", "User is not authorized for this action");
				return forbidden(result);
			}
			User user = DB.getUserDAO().get(new ObjectId(userId));
			Notification notification = DB.getNotificationDAO().get(new ObjectId(notificationId));
			ObjectId receiver = notification.getReceiver();
			if (!receiver.equals(user.getDbId()) && !user.getAdminInGroups().contains(receiver)) {
				result.put("error", "User is not authorized for this action");
				return forbidden(result);
			}
			switch (notification.getActivity()) {
			case GROUP_INVITE:
				ObjectId groupId = notification.getGroup();
				UserGroup group = DB.getUserGroupDAO().get(groupId);
				// The group invitation gets declined
				notification.setPendingResponse(false);
				Date now = new Date();
				notification.setReadAt(new Timestamp(now.getTime()));
				DB.getNotificationDAO().makePermanent(notification);
				Notification newNotification = new Notification();
				newNotification.setActivity(Activity.GROUP_INVITE_DECLINED);
				newNotification.setGroup(group.getDbId());
				newNotification.setReceiver(group.getDbId());
				newNotification.setSender(user.getDbId());
				newNotification.setOpenedAt(new Timestamp(now.getTime()));
				DB.getNotificationDAO().makePermanent(newNotification);
				// Send notification to the group through socket
				NotificationCenter.sendNotification(newNotification);
				// Send notification to the user
				newNotification.setReceiver(user.getDbId());
				newNotification.setDbId(null);
				DB.getNotificationDAO().makePermanent(newNotification);
				// Send notification through socket to group administrators
				NotificationCenter.sendNotification(newNotification);
				result.put("message", "User denied invitation from group");
				return ok(result);
			case GROUP_REQUEST:
				break;
			case COLLECTION_REQUEST_SHARING:
				break;
			default:
				result.put("error", "Notification does not require acceptance");
				return badRequest(result);
			}
			result.put("message", "Notification was accepted");
			return ok(result);
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	public static Result getUserNotifications() {
		try {
			ObjectId userId = new ObjectId(AccessManager.effectiveUserId(session().get("effectiveUserIds")));
			// Get all unread notifications
			List<Notification> notifications = DB.getNotificationDAO().getUnreadByReceiver(userId, 0);
			Set<ObjectId> groups = DB.getUserDAO().get(userId).getAdminInGroups();
			if (groups != null && !groups.isEmpty()) {
				for (ObjectId groupId : groups) {
					notifications.addAll(DB.getNotificationDAO().getUnreadByReceiver(groupId, 0));
				}
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
				notification = DB.getNotificationDAO().get(new ObjectId(id.asText()));
				if (notification.getReadAt() == null && notification.isPendingResponse() == false) {
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
		return ok();
	}

	public static Result sendMessage(String receiverId) {
		ObjectId sender = new ObjectId(AccessManager.effectiveUserId(session().get("effectiveUserIds")));
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
