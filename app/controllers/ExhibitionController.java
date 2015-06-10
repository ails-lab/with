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
		List<String> userIds = AccessManager.effectiveUserIds(session().get(
				"effectiveUserIds"));

		if (json == null) {
			result.put("message", "Invalid json!");
			return badRequest(result);
		}
		if (userIds.isEmpty()) {
			result.put("error", "Must specify user for the exhibition");
			return forbidden(result);
		}
		String userId = userIds.get(0);
		User owner = DB.getUserDAO().get(new ObjectId(userId));

		Collection newExhibition = Json.fromJson(json, Collection.class);
		newExhibition.setCreated(new Date());
		newExhibition.setLastModified(new Date());
		newExhibition.setOwnerId(new ObjectId(userId));
		newExhibition.setExhibition(true);
		newExhibition.setTitle(getAvailableTitle(owner));

		Set<ConstraintViolation<Collection>> violations = Validation
				.getValidator().validate(newExhibition);
		for (ConstraintViolation<Collection> cv : violations) {
			result.put("message",
					"[" + cv.getPropertyPath() + "] " + cv.getMessage());
			return badRequest(result);
		}
		if (DB.getCollectionDAO().makePermanent(newExhibition) == null) {
			result.put("message", "Cannot save Collection to database");
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

	/* Find a unique dummy title for the user exhibition */
	private static String getAvailableTitle(User user) {
		int exhibitionNum = user.getExhibitionsCreated();
		return "Exhibition" + exhibitionNum;
	}
}
