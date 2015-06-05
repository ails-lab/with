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

import play.data.validation.Validation;
import model.Collection;
import model.User;
import model.User.Access;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class ExhibitionController extends Controller {

	public static Result createExhibition() {

		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

		if (json == null) {
			result.put("message", "Invalid json!");
			return badRequest(result);
		}
		if (session().get("effectiveUserIds") == null) {
			result.put("error", "Must specify user for the collection");
			return forbidden(result);
		}
		List<String> userIds = Arrays.asList(session().get("effectiveUserIds")
				.split(","));
		String userId = userIds.get(0);

		Collection newCollection = Json.fromJson(json, Collection.class);
		newCollection.setCreated(new Date());
		newCollection.setLastModified(new Date());
		newCollection.setOwnerId(new ObjectId(userId));

		Set<ConstraintViolation<Collection>> violations = Validation
				.getValidator().validate(newCollection);
		for (ConstraintViolation<Collection> cv : violations) {
			result.put("message",
					"[" + cv.getPropertyPath() + "] " + cv.getMessage());
			return badRequest(result);
		}

		if (DB.getCollectionDAO().getByOwnerAndTitle(
				newCollection.getOwnerId(), newCollection.getTitle()) != null) {
			result.put("message",
					"Title already exists! Please specify another title.");
			return internalServerError(result);
		}
		if (DB.getCollectionDAO().makePermanent(newCollection) == null) {
			result.put("message", "Cannot save Collection to database");
			return internalServerError(result);
		}
		User owner = DB.getUserDAO().get(newCollection.getOwnerId());
		owner.getCollectionMetadata().add(newCollection.collectMetadata());
		DB.getUserDAO().makePermanent(owner);
		ObjectNode c = (ObjectNode) Json.toJson(newCollection);
		c.put("access", Access.OWN.toString());
		User user = DB.getUserDAO().getById(newCollection.getOwnerId(),
				new ArrayList<String>(Arrays.asList("username")));
		c.put("owner", user.getUsername());
		return ok(c);

	}
}
