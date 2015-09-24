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

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.validation.ConstraintViolation;

import model.Collection;
import model.User;
import model.UserOrGroup;
import model.Rights.Access;
import model.UserGroup;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.data.validation.Validation;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results.Status;
import utils.AccessManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class UserAndGroupManager extends Controller {

	public static final ALogger log = Logger.of(UserGroup.class);
	
	/**
	 * Get a list of matching usernames or groupnames
	 *
	 * Used by autocomplete in share collection
	 *
	 * @param prefix
	 *            optional prefix of username or groupname
	 * @return JSON document with an array of matching usernames or groupnames (or all of
	 *         them)
	 */
	public static Result listNames(String prefix) {
		List<User> users = DB.getUserDAO().getByUsernamePrefix(prefix);
		List<UserGroup> groups  = DB.getUserGroupDAO().getByGroupNamePrefix(prefix);
		ArrayNode suggestions = Json.newObject().arrayNode();
		for (User user : users) {
			ObjectNode node = Json.newObject();
			ObjectNode data = Json.newObject().objectNode();
			data.put("category", "user");
			//costly?
			node.put("value", user.getUsername());
			node.put("data", data);
			suggestions.add(node);
		}
		for (UserGroup group : groups) {
			ObjectNode node = Json.newObject().objectNode();
			ObjectNode data = Json.newObject().objectNode();
			data.put("category", "group");
			node.put("value", group.getUsername());
			node.put("data", data);
			suggestions.add(node);
		}

		return ok(suggestions);
	}
	
	/**
	 * @param userOrGroupnameOrEmail
	 * @return User and image
	 */
	public static Result findByUserOrGroupNameOrEmail(String userOrGroupnameOrEmail,
			String collectionId) {
		Function<UserOrGroup, Status> getUserJson = (UserOrGroup u) -> {
			ObjectNode userJSON = Json.newObject();
			userJSON.put("userId", u.getDbId().toString());
			userJSON.put("username", u.getUsername());
			if (u instanceof User) {
				userJSON.put("firstName", ((User) u).getFirstName());
				userJSON.put("lastName", ((User) u).getLastName());
			}
			if (collectionId != null) {
				Collection collection = DB.getCollectionDAO().getById(
						new ObjectId(collectionId));
				if (collection != null) {
					//TODO: have to do recursion here!
					Access accessRights = collection.getRights().get(
							u.getDbId());
					if (accessRights != null)
						userJSON.put("accessRights", accessRights.toString());
					else
						userJSON.put("accessRights", Access.NONE.toString());
				}
			}
			String image = UserManager.getImageBase64(u);
			if (image != null) {
				userJSON.put("image", image);
			}
			return ok(userJSON);
		};
		User user = DB.getUserDAO().getByEmail(userOrGroupnameOrEmail);
		if (user != null) {
			return getUserJson.apply(user);
		} else {
			user = DB.getUserDAO().getByUsername(userOrGroupnameOrEmail);
			if (user != null)
				return getUserJson.apply(user);
			else {
				UserGroup userGroup = DB.getUserGroupDAO().getByName(userOrGroupnameOrEmail);
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
				ObjectId photoId = user.getThumbnail();
				return MediaController.getMetadataOrFile(photoId.toString(),
						true);
			} else {
				UserGroup userGroup = DB.getUserGroupDAO().get(new ObjectId(id));
				if (userGroup != null) {
					ObjectId photoId = user.getThumbnail();
					return MediaController.getMetadataOrFile(photoId.toString(),
							true);
				}
				else
					return badRequest(Json
						.parse("{\"error\":\"User does not exist\"}"));
			}
		} catch (Exception e) {
			return badRequest(Json.parse("{\"error\":\"" + e.getMessage()
					+ "\"}"));
		}
	}
}
