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

import actors.NotificationActor;
import db.DB;
import elastic.ElasticUpdater;
import model.Collection;
import model.Notification;
import model.Notification.Activity;
import model.User;
import model.UserGroup;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import utils.AccessManager;
import utils.NotificationCenter;

public class NotificationController extends Controller {
	public static final ALogger log = Logger.of(NotificationController.class);

	public static WebSocket<JsonNode> socket() {
		return WebSocket.withActor(NotificationActor::props);
	}

	public static Result respondToRequest(String notificationId, boolean accept) {
		ObjectNode result = Json.newObject();
		try {
			String currentUserId = AccessManager.effectiveUserId(session().get("effectiveUserIds"));
			if (currentUserId == null) {
				result.put("error", "User is not authorized for this action");
				return forbidden(result);
			}
			User currentUser = DB.getUserDAO().get(new ObjectId(currentUserId));
			Notification notification = DB.getNotificationDAO().get(new ObjectId(notificationId));
			ObjectId receiver = notification.getReceiver();
			if (!receiver.equals(currentUser.getDbId()) && !currentUser.getAdminInGroups().contains(receiver)) {
				result.put("error", "User is not authorized for this action");
				return forbidden(result);
			}
			ObjectId groupId, userId;
			UserGroup group;
			User user;
			Set<ObjectId> ancestorGroups;
			Date now = new Date();
			Notification newNotification;
			switch (notification.getActivity()) {
			case GROUP_INVITE:
				groupId = notification.getGroup();
				group = DB.getUserGroupDAO().get(groupId);
				user = currentUser;
				if (accept) {
					// The user is added to the group
					ancestorGroups = group.getAncestorGroups();
					ancestorGroups.add(group.getDbId());
					group.getUsers().add(user.getDbId());
					user.addUserGroups(ancestorGroups);
					DB.getUserDAO().makePermanent(user);
					DB.getUserGroupDAO().makePermanent(group);
				}
				// Mark the invitation from the group as closed with the
				// appropriate timestamp
				notification.setPendingResponse(false);
				notification.setReadAt(new Timestamp(now.getTime()));
				DB.getNotificationDAO().makePermanent(notification);
				// Notification for the user join to the group
				// Notification for the administrators of the group
				newNotification = new Notification();
				if (accept) {
					newNotification.setActivity(Activity.GROUP_INVITE_ACCEPT);
				} else {
					newNotification.setActivity(Activity.GROUP_INVITE_DECLINED);
				}
				newNotification.setGroup(group.getDbId());
				newNotification.setReceiver(group.getDbId());
				newNotification.setSender(user.getDbId());
				newNotification.setOpenedAt(new Timestamp(now.getTime()));
				DB.getNotificationDAO().makePermanent(newNotification);
				NotificationCenter.sendNotification(newNotification);
				result.put("message", "User succesfully responded to invitation");
				return ok(result);
			case GROUP_REQUEST:
				groupId = notification.getGroup();
				group = DB.getUserGroupDAO().get(groupId);
				userId = notification.getSender();
				user = DB.getUserDAO().get(userId);
				if (accept) {
					// The user is added to the group
					ancestorGroups = group.getAncestorGroups();
					ancestorGroups.add(group.getDbId());
					group.getUsers().add(user.getDbId());
					user.addUserGroups(ancestorGroups);
					DB.getUserDAO().makePermanent(user);
					DB.getUserGroupDAO().makePermanent(group);
				}
				// Mark the request from the user as closed with the
				// appropriate timestamp
				notification.setPendingResponse(false);
				notification.setReadAt(new Timestamp(now.getTime()));
				DB.getNotificationDAO().makePermanent(notification);
				// Store new notification at the database for the user
				// acceptance
				// Notification for the user
				newNotification = new Notification();
				if (accept) {
					newNotification.setActivity(Activity.GROUP_REQUEST_ACCEPT);
				} else {
					newNotification.setActivity(Activity.GROUP_REQUEST_DENIED);
				}
				newNotification.setGroup(group.getDbId());
				newNotification.setSender(currentUser.getDbId());
				newNotification.setOpenedAt(new Timestamp(now.getTime()));
				// Notification for the administrators of the group
				newNotification.setReceiver(user.getDbId());
				newNotification.setDbId(null);
				DB.getNotificationDAO().makePermanent(newNotification);
				// Send notification through socket (to user and group
				// administrators)
				NotificationCenter.sendNotification(newNotification);
				result.put("message", "Group succesfully reponded to user request");
				return ok(result);
			case COLLECTION_REQUEST_SHARING:
				Collection collection = DB.getCollectionDAO().get(notification.getCollection());
				if (accept) {
					collection.getRights().put(notification.getReceiver(), notification.getAccess());
					if (DB.getCollectionDAO().makePermanent(collection) == null) {
						result.put("error", "Cannot store collection to database!");
						return internalServerError(result);
					}
					// update collection rights in index
					ElasticUpdater updater = new ElasticUpdater(collection);
					updater.updateCollectionRights();
				}
				notification.setPendingResponse(false);
				notification.setReadAt(new Timestamp(now.getTime()));
				DB.getNotificationDAO().makePermanent(notification);
				newNotification = new Notification();
				newNotification.setCollection(notification.getCollection());
				if (accept) {
					newNotification.setActivity(Activity.COLLECTION_SHARED);
				} else {
					newNotification.setActivity(Activity.COLLECTION_REJECTED);
				}
				newNotification.setAccess(notification.getAccess());
				newNotification.setSender(notification.getReceiver());
				newNotification.setReceiver(notification.getSender());
				newNotification.setOpenedAt(new Timestamp(now.getTime()));
				DB.getNotificationDAO().makePermanent(newNotification);
				NotificationCenter.sendNotification(newNotification);
				result.put("message", "User succesfully responded to collection share request");
				return ok(result);
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
