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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;

import model.Collection;
import model.User;
import model.User.Access;

import org.bson.types.ObjectId;

import play.data.validation.Validation;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessManager;
import utils.AccessManager.Action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class ExhibitionController extends Controller {

	public static Result createExhibition() {

		ObjectNode result = Json.newObject();
		List<String> userIds = AccessManager.effectiveUserIds(session().get(
				"effectiveUserIds"));
		if (userIds.isEmpty()) {
			result.put("error", "Must specify user for the exhibition");
			return forbidden(result);
		}
		String userId = userIds.get(0);
		User owner = DB.getUserDAO().get(new ObjectId(userId));

		Collection newExhibition = new Collection();
		newExhibition.setCreated(new Date());
		newExhibition.setLastModified(new Date());
		newExhibition.setOwnerId(new ObjectId(userId));
		newExhibition.setExhibition(true);
		newExhibition.setTitle(getAvailableTitle(owner));
		newExhibition.setDescription("Description");

		Set<ConstraintViolation<Collection>> violations = Validation
				.getValidator().validate(newExhibition);
		for (ConstraintViolation<Collection> cv : violations) {
			result.put("error",
					"[" + cv.getPropertyPath() + "] " + cv.getMessage());
			return badRequest(result);
		}
		if (DB.getCollectionDAO().makePermanent(newExhibition) == null) {
			result.put("error", "Cannot save Exhibition to database");
			return internalServerError(result);
		}
		owner.addExhibitionsCreated();
		DB.getUserDAO().makePermanent(owner);
		ObjectNode c = (ObjectNode) Json.toJson(newExhibition);
		c.put("access", Access.OWN.toString());
		User user = DB.getUserDAO().getById(newExhibition.getOwnerId(),
				new ArrayList<String>(Arrays.asList("username")));
		c.put("owner", user.getUsername());
		return ok(c);
	}

	public static Result editExhibition(String id) {

		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		List<String> userIds = AccessManager.effectiveUserIds(session().get(
				"effectiveUserIds"));
		if (json == null) {
			result.put("error", "Invalid json");
			return badRequest(result);
		}
		Collection exhibition = DB.getCollectionDAO().getById(new ObjectId(id));
		ObjectId ownerId = exhibition.getOwnerId();
		if (!AccessManager.checkAccess(exhibition.getRights().get(0), userIds,
				Action.EDIT)) {
			result.put("error",
					"User does not have permission to edit the exhibition");
			return forbidden(result);
		}
		if (json.has("title")) {
			String title = json.get("title").asText();
			if (DB.getCollectionDAO().getByOwnerAndTitle(ownerId, title) != null) {
				result.put("error", "Title exists");
				return badRequest(result);
			}
			exhibition.setTitle(title);
		}
		if (json.has("description")) {
			String description = json.get("description").asText();
			exhibition.setDescription(description);
		}
		exhibition.setLastModified(new Date());

		Set<ConstraintViolation<Collection>> violations = Validation
				.getValidator().validate(exhibition);
		for (ConstraintViolation<Collection> cv : violations) {
			result.put("error",
					"[" + cv.getPropertyPath() + "] " + cv.getMessage());
			return badRequest(result);
		}
		if (DB.getCollectionDAO().makePermanent(exhibition) == null) {
			result.put("error", "Cannot save Exhibition to database");
			return internalServerError(result);
		}
		ObjectNode c = (ObjectNode) Json.toJson(exhibition);
		c.put("access",
				AccessManager.getMaxAccess(exhibition.getRights().get(0), userIds)
						.toString());
		User user = DB.getUserDAO().getById(ownerId,
				new ArrayList<String>(Arrays.asList("username")));
		c.put("owner", user.getUsername());
		return ok(c);
	}

	public static Result getExhibition(String id) {

		ObjectNode result = Json.newObject();
		try {
			List<String> userIds = AccessManager.effectiveUserIds(session()
					.get("effectiveUserIds"));
			Collection exhibition;
			exhibition = DB.getCollectionDAO().getById(new ObjectId(id));
			if (!AccessManager.checkAccess(exhibition.getRights().get(0), userIds,
					Action.READ)) {
				result.put("error",
						"User does not have read-access to the exhibition");
				return forbidden(result);
			}
			if (!exhibition.isExhibition()) {
				result.put("error",
						"The requested resource is not an exhibition");
				return badRequest(result);
			}
			ObjectNode c = (ObjectNode) Json.toJson(exhibition);
			c.put("access",
					AccessManager.getMaxAccess(exhibition.getRights().get(0), userIds)
							.toString());
			User user = DB.getUserDAO().getById(exhibition.getOwnerId(),
					new ArrayList<String>(Arrays.asList("username")));
			c.put("owner", user.getUsername());
			return ok(c);
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	/* Find a unique dummy title for the user exhibition */
	private static String getAvailableTitle(User user) {
		int exhibitionNum = user.getExhibitionsCreated();
		return "Exhibition" + exhibitionNum;
	}
}
