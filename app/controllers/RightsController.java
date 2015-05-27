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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Collection;
import model.User.Access;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessManager;
import utils.AccessManager.Action;

import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

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
	 * @param email
	 *            the email of the user
	 * @param userId
	 *            the Id of the user
	 * @return OK or Error with JSON detailing the problem
	 * 
	 */
	public static Result setRights(String colId, String right, String username,
			String email, String userId) {

		ObjectNode result = Json.newObject();
		Collection collection = null;
		try {
			collection = DB.getCollectionDAO().get(new ObjectId(colId));
		} catch (Exception e) {
			log.error("Cannot retrieve collection from database!", e);
			result.put("message", "Cannot retrieve collection from database!");
			return internalServerError(result);
		}
		List<String> userIds = Arrays.asList(session().get("effectiveUserIds")
				.split(","));
		if (!AccessManager.checkAccess(collection.getRights(), userIds,
				Action.DELETE)) {
			result.put("error",
					"Sorry! You do not own this collection so you cannot set rights. "
							+ "Please contact the owner of this collection");
			return forbidden(result);
		}
		// set rights
		// the receiver can be either a User or a UserGroup
		Map<ObjectId, Access> rightsMap = new HashMap<ObjectId, Access>();
		if (username != null) {
			userId = DB.getUserDAO().getByUsername(username).getDbId()
					.toHexString();
		} else if (email != null) {
			userId = DB.getUserDAO().getByEmail(email).getDbId().toHexString();
		} else if (userId == null) {
			result.put("error", "Must specify user to give rights");
			return badRequest(result);
		}
		rightsMap.put(new ObjectId(userId), Access.valueOf(right));
		collection.getRights().putAll(rightsMap);
		if (DB.getCollectionDAO().makePermanent(collection) == null) {
			result.put("message", "Cannot store collection to database!");
			return internalServerError(result);
		}
		return ok();
	}

	/*
	 * public static Result listRights(String ownerId) { ObjectNode result =
	 * Json.newObject();
	 * 
	 * if(ownerId.equals("")) { result.put("message", "No user specified!");
	 * return badRequest(result); }
	 * 
	 * 
	 * 
	 * }
	 */
}
