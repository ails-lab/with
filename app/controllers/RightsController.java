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

import java.util.HashMap;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;
import elastic.ElasticUpdater;
import model.Collection;
import model.Notification;
import model.Notification.Activity;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import model.basicDataTypes.WithAccess.AccessEntry;
import model.resources.CollectionObject;
import model.resources.RecordResource;
import model.usersAndGroups.User;
import model.usersAndGroups.UserGroup;
import model.usersAndGroups.UserOrGroup;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessManager;
import utils.AccessManager.Action;
import utils.NotificationCenter;

public class RightsController extends WithResourceController {
	public static final ALogger log = Logger.of(CollectionController.class);

	/**
	 * Set access rights for object for user.
	 *
	 * @param colId
	 *            the internal Id of the object you wish to share (or unshare)
	 * @param right
	 *            the right to give ("none" to withdraw previously given right)
	 * @param username
	 *            the username of user to give rights to (or take away from)
	 * @return OK or Error with JSON detailing the problem
	 *
	 */
	public static Result shareCollection(String colId, String right, String username, boolean membersDowngrade) {
		ObjectNode result = Json.newObject();
		ObjectId colDbId = new ObjectId(colId);
		Result response = errorIfNoAccessToRecord(Action.DELETE, colDbId);
		if (!response.toString().equals(ok().toString()))
			return response;
		else {//user is owner
			Access newAccess = Access.valueOf(right);
			if (newAccess == null) {
				result.put("error", right + " is not an admissible value for access rights " +
						"(should be one of NONE, READ, WRITE, OWN).");
				return badRequest(result);
			}
			else {
				ObjectId ownerId = new ObjectId(session().get("user"));
				UserGroup userGroup = null;
				// the receiver can be either a User or a UserGroup
				ObjectId userOrGroupId = null;
				if (username != null) {
					User user = DB.getUserDAO().getUniqueByFieldAndValue("username", username, new ArrayList<String>(Arrays.asList("_id")));
					if (user != null) {
						userOrGroupId = user.getDbId();
					} else {
						userGroup = DB.getUserGroupDAO().getUniqueByFieldAndValue("username", username, new ArrayList<String>(Arrays.asList("_id")));
						if (userGroup != null) {
							userOrGroupId = userGroup.getDbId();
						}
					}
				}
				if (userOrGroupId == null) {
					result.put("error",
							"No user or userGroup with given username");
					return badRequest(result);
				}
				//check whether the newAccess entails a downgrade or upgrade of the current access of the collection
				CollectionObject collection = DB.getCollectionObjectDAO().
						getUniqueByFieldAndValue("_id", colDbId, new ArrayList<String>(Arrays.asList("administrative.access")));
				WithAccess oldColAccess = collection.getAdministrative().getAccess();
				int downgrade = downgrade(oldColAccess.getAcl(), userOrGroupId, newAccess);
				if (downgrade > -1) //if downgrade == -1, the rights are not changed, do nothing
					if (downgrade == 1 && membersDowngrade) {//the rights of all records that belong to the collection are downgraded
						DB.getRecordResourceDAO().updateMembersToNewAccess(colDbId, userOrGroupId, newAccess);
						DB.getCollectionObjectDAO().changeAccess(colDbId, userOrGroupId, newAccess);
					}
					else {//if upgrade, or downgrade but !membersDowngrade the new rights of the collection are merged to all records that belong to the record. 
						DB.getRecordResourceDAO().updateMembersToMergedRights(colDbId, new AccessEntry(userOrGroupId, newAccess));
						DB.getCollectionObjectDAO().changeAccess(colDbId, userOrGroupId, newAccess);
					}
				return sendShareCollectionNotification(userGroup == null? false: true, userOrGroupId, colDbId, ownerId, newAccess);
			}
		}
	}
	
	public static int downgrade(List<AccessEntry> oldColAcl, ObjectId userOrGroupId, Access newAccess) {
		for (AccessEntry ae: oldColAcl) {
			if (ae.getUser().equals(userOrGroupId))
				if (ae.getLevel().ordinal() > newAccess.ordinal())
					return 1;
				else if (ae.getLevel().ordinal() == newAccess.ordinal())
					return -1;
		}
		return 0;
	}
	
	public void changeAllRecordMembersAccess(String colId, String access, String username) {
	}
	

				
	public static Result sendShareCollectionNotification(boolean userGroup, ObjectId userOrGroupId, ObjectId colDbId, 
			ObjectId ownerId, Access newAccess) {
		ObjectNode result = Json.newObject();	
		Notification notification = new Notification();
		if (userGroup) {
			notification.setGroup(userOrGroupId);
		}
		notification.setReceiver(userOrGroupId);
		notification.setCollection(colDbId);
		notification.setSender(ownerId);
		notification.setPendingResponse(false);
		Date now = new Date();
		notification.setOpenedAt(new Timestamp(now.getTime()));
		DB.getNotificationDAO().makePermanent(notification);
		NotificationCenter.sendNotification(notification);
		if (newAccess.equals(Access.NONE)) {
			notification.setActivity(Activity.COLLECTION_UNSHARED);
			NotificationCenter.sendNotification(notification);
			result.put("mesage", "Collection unshared with user or group");
			return ok(result);
		}
		else if (DB.getUserDAO().isSuperUser(ownerId)) {
			notification.setActivity(Activity.COLLECTION_SHARED);
			NotificationCenter.sendNotification(notification);
			result.put("message", "Collection shared");
			return ok(result);
		}
		else {
			List<Notification> requests = DB.getNotificationDAO()
					.getPendingCollectionNotifications(userOrGroupId,
							colDbId, Activity.COLLECTION_SHARE, newAccess);
			if (requests.isEmpty()) {
				// Find if there is a request for other type of access and
				// override it
				requests = DB.getNotificationDAO()
						.getPendingCollectionNotifications(userOrGroupId,
								colDbId, Activity.COLLECTION_SHARE);
				for (Notification request : requests) {
					request.setPendingResponse(false);
					now = new Date();
					request.setReadAt(new Timestamp(now.getTime()));
					DB.getNotificationDAO().makePermanent(request);
				}
				// Make a new request for collection sharing request
				notification.setActivity(Activity.COLLECTION_SHARE);
				now = new Date();
				notification.setOpenedAt(new Timestamp(now.getTime()));
				DB.getNotificationDAO().makePermanent(notification);
				NotificationCenter.sendNotification(notification);
				result.put("message",
						"Request for collection sharing sent to the user");
				return ok(result);
			} else {
				result.put("error", "Request has already been sent to the user");
				return badRequest(result);
			}
		}
	}
}
