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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import model.basicDataTypes.WithAccess.AccessEntry;
import model.resources.CollectionObject;
import model.usersAndGroups.User;
import model.usersAndGroups.UserGroup;
import notifications.Notification;
import notifications.Notification.Activity;
import notifications.ResourceNotification.ShareInfo;
import notifications.ResourceNotification;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Result;
import utils.AccessManager.Action;
import utils.AccessManager;
import utils.NotificationCenter;

import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class RightsController extends WithResourceController {
	public static final ALogger log = Logger.of(RightsController.class);
	
	public static Result editCollectionPublicity(String colId, Boolean isPublic, boolean membersDowngrade) {
		ObjectNode result = Json.newObject();
		/*if (isPublic == null) {
			result.put("error", "isPublic should be true or false");
			return badRequest(result);
		}*/
		ObjectId colDbId = new ObjectId(colId);
		Result response = errorIfNoAccessToCollection(Action.DELETE, colDbId);
		if (!response.toString().equals(ok().toString()))
			return response;
		else {
			CollectionObject collection = DB.getCollectionObjectDAO().
				getUniqueByFieldAndValue("_id", colDbId, new ArrayList<String>(Arrays.asList("administrative.access")));
			boolean oldIsPublic = collection.getAdministrative().getAccess().getIsPublic();
			if (oldIsPublic != isPublic) {
				DB.getCollectionObjectDAO().updateField(colDbId, "administrative.access.isPublic", isPublic);
				if (!isPublic) //downgrade
					if (membersDowngrade) {
						
					}
					else {
						
					}
			}
			return ok(result);
		}
	}

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
	public static Result editCollectionRights(String colId, String right, String username, boolean membersDowngrade) {
		ObjectNode result = Json.newObject();
		ObjectId colDbId = new ObjectId(colId);
		Access newAccess = Access.valueOf(right);
		if (newAccess == null) {
			result.put("error", right + " is not an admissible value for access rights " +
					"(should be one of NONE, READ, WRITE, OWN).");
			return badRequest(result);
		}
		else {
			ObjectId loggedIn = new ObjectId(session().get("user"));
			UserGroup userGroup = null;
			User user = null;
			// the receiver can be either a User or a UserGroup
			ObjectId userOrGroupId = null;
			if (username != null) {
				user = DB.getUserDAO().getUniqueByFieldAndValue("username", username, new ArrayList<String>(Arrays.asList("_id")));
				if (user != null) {
					userOrGroupId = user.getDbId();
				} else {
					userGroup = DB.getUserGroupDAO().getUniqueByFieldAndValue("username", username, new ArrayList<String>(Arrays.asList("_id", "adminIds")));
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
			else {
				//check whether the newAccess entails a downgrade or upgrade of the current access of the collection
				CollectionObject collection = DB.getCollectionObjectDAO().
						getUniqueByFieldAndValue("_id", colDbId, new ArrayList<String>(Arrays.asList("administrative.access")));
				WithAccess oldColAccess = collection.getAdministrative().getAccess();
				int downgrade = isDowngrade(oldColAccess.getAcl(), userOrGroupId, newAccess);
				//the logged in user has the right to downgrade his own access level (unshare)
				boolean hasDowngradeRight = loggedIn.equals(userOrGroupId);
				List<ObjectId> effectiveIds = AccessManager.effectiveUserDbIds(session().get("effectiveUserIds"));
				if (userGroup != null) {
					hasDowngradeRight = userGroup.getAdminIds().contains(loggedIn);
				}
				if (downgrade == 1 && hasDowngradeRight) {
						changeAccess(colDbId, userOrGroupId, newAccess, effectiveIds, true, membersDowngrade);
						return sendShareCollectionNotification(userGroup == null? false: true, userOrGroupId, colDbId, loggedIn, Access.NONE, newAccess, effectiveIds, 
								true, membersDowngrade);
				}
				else if (downgrade > -1){//downgrade and no downgradeRights or upgrade
					Result response = errorIfNoAccessToCollection(Action.DELETE, colDbId);
					if (!response.toString().equals(ok().toString()))
						return response;
					else {
						Access oldAccess = Access.NONE;
						for (AccessEntry ae: oldColAccess.getAcl()) {
							if (ae.getUser().equals(userOrGroupId)) {
								oldAccess = ae.getLevel();
							    break;
							}
						}
						changeAccess(colDbId, userOrGroupId, newAccess, effectiveIds, downgrade == 1, membersDowngrade);
						return sendShareCollectionNotification(userGroup == null? false: true, userOrGroupId, colDbId, loggedIn, oldAccess, newAccess, effectiveIds, 
								downgrade == 1, membersDowngrade);
					}
				}
				else {//if downgrade == -1, the rights are not changed, do nothing
					result.put("mesage", "The user already has the required access to the collection.");
					return ok(result);
				}
			}
		}
	}
	
	public static void changePublicity(ObjectId colId, boolean isPublic, List<ObjectId> effectiveIds, boolean downgrade, boolean membersDowngrade) {
		DB.getCollectionObjectDAO().updateField(colId, "administrative.access.isPublic", isPublic);
		if (downgrade && membersDowngrade) {//the publicity of all records that belong to the collection is downgraded
			DB.getRecordResourceDAO().updateMembersToNewPublicity(colId, isPublic, effectiveIds);
		}
		else {//if upgrade, or downgrade but !membersDowngrade the new rights of the collection are merged to all records that belong to the record. 
			DB.getRecordResourceDAO().updateMembersToMergedPublicity(colId, isPublic, effectiveIds);
		}	
	}
	
	public static void changeAccess(ObjectId colId, ObjectId userOrGroupId, Access newAccess, List<ObjectId> effectiveIds, 
			boolean downgrade, boolean membersDowngrade) {
		DB.getCollectionObjectDAO().changeAccess(colId, userOrGroupId, newAccess);
		if (downgrade && membersDowngrade) {//the rights of all records that belong to the collection are downgraded
			DB.getRecordResourceDAO().updateMembersToNewAccess(colId, userOrGroupId, newAccess, effectiveIds);
		}
		else {//if upgrade, or downgrade but !membersDowngrade the new rights of the collection are merged to all records that belong to the record. 
			DB.getRecordResourceDAO().updateMembersToMergedRights(colId, new AccessEntry(userOrGroupId, newAccess), effectiveIds);
		}		
	}
	
	public static int isDowngrade(List<AccessEntry> oldColAcl, ObjectId userOrGroupId, Access newAccess) {
		for (AccessEntry ae: oldColAcl) {
			if (ae.getUser().equals(userOrGroupId))
				if (ae.getLevel().ordinal() > newAccess.ordinal())
					return 1;
				else if (ae.getLevel().ordinal() == newAccess.ordinal())
					return -1;
		}
		return 0;
	}
	
				
	public static Result sendShareCollectionNotification(boolean userGroup, ObjectId userOrGroupId, ObjectId colDbId, 
			ObjectId ownerId, Access oldAccess, Access newAccess, List<ObjectId> effectiveIds, boolean downgrade, boolean membersDowngrade) {
		ObjectNode result = Json.newObject();	
		ResourceNotification notification = new ResourceNotification();
		//what if the receiver is a group?
		notification.setReceiver(userOrGroupId);
		notification.setResource(colDbId);
		notification.setSender(ownerId);
		ShareInfo shareInfo = new ShareInfo();
		shareInfo.setNewAccess(newAccess);
		shareInfo.setUserOrGroup(userOrGroupId);
		Date now = new Date();
		notification.setOpenedAt(new Timestamp(now.getTime()));
		DB.getNotificationDAO().makePermanent(notification);
		NotificationCenter.sendNotification(notification);
		if (downgrade) {
			notification.setPendingResponse(false);
			notification.setActivity(Activity.COLLECTION_UNSHARED);
			notification.setShareInfo(shareInfo);
			NotificationCenter.sendNotification(notification);
			result.put("mesage", "Access of user or group to collection has been downgraded.");
			return ok(result);
		}
		else if (DB.getUserDAO().isSuperUser(ownerId)) {//upgrade
			notification.setPendingResponse(false);
			notification.setActivity(Activity.COLLECTION_SHARED);
			notification.setShareInfo(shareInfo);
			NotificationCenter.sendNotification(notification);
			result.put("message", "Collection shared.");
			return ok(result);
		}
		else {//upgrade
			List<Notification> requests = DB.getNotificationDAO()
					.getPendingResourceNotifications(userOrGroupId,
							colDbId, Activity.COLLECTION_SHARE, newAccess);
			if (requests.isEmpty()) {
				// Find if there is a request for other type of access and
				// override it
				requests = DB.getNotificationDAO()
						.getPendingResourceNotifications(userOrGroupId,
								colDbId, Activity.COLLECTION_SHARE);
				for (Notification request : requests) {
					request.setPendingResponse(false);
					now = new Date();
					request.setReadAt(new Timestamp(now.getTime()));
					DB.getNotificationDAO().makePermanent(request);
				}
				// Make a new request for collection sharing request
				shareInfo.setPreviousAccess(oldAccess);
				notification.setPendingResponse(true);
				notification.setActivity(Activity.COLLECTION_SHARE);
				now = new Date();
				shareInfo.setOwnerEffectiveIds(effectiveIds);
				notification.setShareInfo(shareInfo);
				notification.setOpenedAt(new Timestamp(now.getTime()));
				DB.getNotificationDAO().makePermanent(notification);
				NotificationCenter.sendNotification(notification);
				result.put("message",
						"Request for collection sharing sent to the user.");
				return ok(result);
			} else {
				result.put("error", "Request has already been sent to the user.");
				return badRequest(result);
			}
		}
	}
}
