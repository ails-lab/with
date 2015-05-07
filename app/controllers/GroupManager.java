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

import model.UserGroup;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class GroupManager extends Controller {
	public static final ALogger log = Logger.of(UserGroup.class);


	public static Result createGroup() {
		ArrayNode json = (ArrayNode) request().body().asJson();
		ObjectNode result = Json.newObject();


		UserGroup newGroup = new UserGroup();
		for(JsonNode gid: json)
			newGroup.getParentGroups().add(new ObjectId(gid.asText()));
		try {
			DB.getUserGroupDAO().makePermanent(newGroup);
		} catch(Exception e) {
			log.error("Cannot save group to database!", e);
			result.put("message", "Cannot save group to database!");
			return internalServerError(result);
		}

		return ok(Json.toJson(newGroup));
	}

	public static Result deleteGroup(String gid) {
		ObjectNode result = Json.newObject();

		try {
			DB.getUserGroupDAO().deleteById(new ObjectId(gid));
		} catch(Exception e) {
			log.error("Cannot delete group from database!", e);
			result.put("message", "Cannot delete group from database!");
			return internalServerError(result);
		}

		result.put("message", "Group deleted succesfully from database");
		return ok(result);
	}
}
