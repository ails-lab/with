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

import java.util.Date;
import java.util.List;

import model.Rights;
import model.Rights.Access;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class RightsController extends Controller {
	public static final ALogger log = Logger.of( CollectionController.class);


	public static Result setRights(String objId, String right, String receiver, String owner, String collection) {
		ObjectNode result = Json.newObject();

		Rights rightsEntry = new Rights();

		try {
			rightsEntry.setCreated(new Date());
			rightsEntry.setObjectId(new ObjectId(objId));
			rightsEntry.setCollectionName(collection);
			rightsEntry.setReceiverId(DB.getUserDAO().getByUsername(receiver).getDbId());
			rightsEntry.setOwnerId(DB.getUserDAO().getByUsername(owner).getDbId());
			rightsEntry.setAccess(Access.valueOf(right));
		} catch (Exception e) {
			log.error("Cannot set properties to rights entity", e);
			result.put("message", "Cannot set properties to rights entity. Check parameters");
			return internalServerError(result);
		}

		if( DB.getRightsDAO().makePermanent(rightsEntry) == null) {
			result.put("mesage", "Cannot store rights entity to database!");
			return internalServerError(result);
		}

		return ok(Json.toJson(rightsEntry));

	}

	public static Result listRights(String ownerId) {
		ObjectNode result = Json.newObject();

		if(ownerId.equals("")) {
			result.put("message", "No user specified!");
			return badRequest(result);
		}

		List<Rights> rights = null;
		try {
			rights = DB.getRightsDAO().getByOwner(new ObjectId(ownerId));
		} catch(Exception e) {
			log.error("Cannot retrieve rights from database", e);
			result.put("message", "Cannot retrieve rights from database");
			return internalServerError(result);
		}

		return ok(Json.toJson(rights));

	}
}
