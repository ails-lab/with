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
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;
import elastic.ElasticUpdater;
import model.Collection;
import model.WithAccess.Access;
import model.usersAndGroups.UserGroup;
import model.usersAndGroups.UserOrGroup;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessManager;
import utils.AccessManager.Action;

public class RightsController extends Controller {
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
	public static Result setRights(String colId, String right, String username) {

		ObjectNode result = Json.newObject();
		Collection collection = null;
		try {
			collection = DB.getCollectionDAO().get(new ObjectId(colId));
		} catch (Exception e) {
			log.error("Cannot retrieve collection from database!", e);
			result.put("error", "Cannot retrieve collection from database!");
			return internalServerError(result);
		}
		List<String> userIds = AccessManager.effectiveUserIds(session().get("effectiveUserIds"));
		ObjectId userId = new ObjectId(AccessManager.effectiveUserId(session().get("effectiveUserIds")));
		if (!AccessManager.checkAccess(collection.getRights(), userIds, Action.DELETE)) {
			result.put("error", "Sorry! You do not own this collection so you cannot set rights. "
					+ "Please contact the owner of this collection");
			return forbidden(result);
		}
		// Set rights
		// The receiver can be either a User or a UserGroup
		Map<ObjectId, Access> rightsMap = new HashMap<ObjectId, Access>();
		UserOrGroup userOrGroup;
		ObjectId userOrGroupId = null;
		Access newRight = Access.valueOf(right);
		if (username != null) {
			// In case that the receiver is a user the rights are given
			// automatically
			if ((userOrGroup = DB.getUserDAO().getByUsername(username)) != null) {
				userOrGroupId = userOrGroup.getDbId();
				if (right.equals("NONE")) {
					collection.getRights().remove(userOrGroupId);
				} else {
					rightsMap.put(userOrGroupId, newRight);
					collection.getRights().putAll(rightsMap);
				}
				// In case that the receiver is a group extra checks are needed
			} else if ((userOrGroup = DB.getUserGroupDAO().getByName(username)) != null) {
				UserGroup group = (UserGroup) userOrGroup;
				userOrGroupId = group.getDbId();
				userOrGroupId = userOrGroup.getDbId();
				// If the user who shared the collection with the group belongs
				// to the collection, the rights are given automatically. Also,
				// the rights are given automatically if the access level is
				// decreased. (e.g. from WRITE to READ)
				if (AccessManager.increasedAccess(collection.getRights().get(userOrGroupId), newRight)
						&& !group.getUsers().contains(userId)) {
					collection.addForModeration(userOrGroupId, newRight);
				} else {
					collection.removeFromModeration(userOrGroupId);
					if (right.equals("NONE")) {
						collection.getRights().remove(userOrGroupId);
					} else {
						rightsMap.put(userOrGroupId, newRight);
						collection.getRights().putAll(rightsMap);
					}
				}
			} else {
				result.put("error", "No user or userGroup with given username");
				return badRequest(result);
			}
		}
		if (DB.getCollectionDAO().makePermanent(collection) == null) {
			result.put("error", "Cannot store collection to database!");
			return internalServerError(result);
		}
		// update collection rights in index
		ElasticUpdater updater = new ElasticUpdater(collection);
		updater.updateCollectionRights();
		result.put("message", "OK");
		return ok(result);
	}

	public static Result approveCollection(String collectionId, String groupId) {

		ObjectNode result = Json.newObject();
		Collection collection;
		UserGroup group;
		ObjectId userId = new ObjectId(AccessManager.effectiveUserId(session().get("effectiveUserIds")));
		ObjectId groupID = new ObjectId(groupId);
		try {
			group = DB.getUserGroupDAO().get(groupID);
			collection = DB.getCollectionDAO().get(new ObjectId(collectionId));
		} catch (Exception e) {
			log.error("Cannot retrieve object from database!", e);
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
		if (!group.getAdminIds().contains(userId)) {
			result.put("error",
					"Only the administrators of the group have the right to approve the shared collections");
			return forbidden(result);
		}
		Access access = collection.removeFromModeration(groupID);
		if (access == null) {
			result.put("error", "Collection was not listed for approval");
			return badRequest(result);
		}
		if (access.equals("NONE")) {
			collection.getRights().remove(groupID);
		} else {
			collection.getRights().put(groupID, access);
		}
		if (DB.getCollectionDAO().makePermanent(collection) == null) {
			result.put("error", "Cannot store collection to database!");
			return internalServerError(result);
		}
		result.put("message", "OK");
		return ok(result);
	}

	public static Result rejectCollection(String collectionId, String groupId) {

		ObjectNode result = Json.newObject();
		Collection collection;
		UserGroup group;
		ObjectId userId = new ObjectId(AccessManager.effectiveUserId(session().get("effectiveUserIds")));
		try {
			group = DB.getUserGroupDAO().get(new ObjectId(groupId));
			collection = DB.getCollectionDAO().get(new ObjectId(collectionId));
		} catch (Exception e) {
			log.error("Cannot retrieve object from database!", e);
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
		if (!group.getAdminIds().contains(userId)) {
			result.put("error", "Only the administrators of the group have the right to reject the shared collections");
			return forbidden(result);
		}
		collection.removeFromModeration(new ObjectId(groupId));
		if (DB.getCollectionDAO().makePermanent(collection) == null) {
			result.put("error", "Cannot store collection to database!");
			return internalServerError(result);
		}
		result.put("message", "OK");
		return ok(result);
	}

}
