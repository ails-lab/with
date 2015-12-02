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
import java.util.Set;

import javax.validation.ConstraintViolation;

import model.resources.RecordResource;

import org.bson.types.ObjectId;

import play.data.validation.Validation;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class CollectionObjectController extends Controller {
	/**
	 * Creates a new WITH resource from the JSON body
	 *
	 * @return the newly created resource
	 */
	// TODO check restrictions (unique fields e.t.c)
	public static Result createCollectionObject() {
		ObjectNode error = Json.newObject();
		JsonNode json = request().body().asJson();
		try {
			if (json == null) {
				error.put("error", "Invalid JSON");
				return badRequest(error);
			}
			if (session().get("user") == null) {
				error.put("error", "No rights for WITH resource creation");
				return forbidden(error);
			}
			ObjectId creator = new ObjectId(session().get("user"));
			String resourceType = json.get("resourceType").asText();
			Class<?> clazz = Class.forName("model.resources." + resourceType);
			RecordResource resource = (RecordResource) Json.fromJson(json,
					clazz);
			Set<ConstraintViolation<RecordResource>> violations = Validation
					.getValidator().validate(resource);
			if (!violations.isEmpty()) {
				ArrayNode properties = Json.newObject().arrayNode();
				for (ConstraintViolation<RecordResource> cv : violations) {
					properties.add(Json.parse("{\"" + cv.getPropertyPath()
							+ "\":\"" + cv.getMessage() + "\"}"));
				}
				error.put("error", properties);
				return badRequest(error);
			}
			// Fill with all the administrative metadata
			resource.getAdministrative().setWithCreator(creator);
			resource.getAdministrative().setCreated(new Date());
			resource.getAdministrative().setLastModified(new Date());
			// TODO: withURI?
			// TODO: maybe moderate usage statistics?
			DB.getRecordResourceDAO().makePermanent(resource);
			return ok(Json.toJson(resource));
		} catch (Exception e) {
			error.put("error", e.getMessage());
			return internalServerError(error);
		}
	}
}
