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

import model.Collection;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.Serializer;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import db.DB;

public class CollectionController extends Controller {
	public static final ALogger log = Logger.of( CollectionController.class);

	/*
	 * Pretty print json
	 */
	private static void jsonPrettyPrint(String json) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonParser jp = new JsonParser();
		JsonElement je = jp.parse(json);
		String pretty = gson.toJson(je);
		System.out.println(pretty);
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result saveCollection() {
		JsonNode json = request().body().asJson();

		if(json == null)
			return badRequest("Empty json!\n");

		jsonPrettyPrint(json.toString());
		Collection newCollection = Serializer.jsonToCollectionObject(json);

		try {
			DB.getCollectionDAO().save(newCollection);
		} catch (Exception e) {
			log.error("Collection was not saved!", e);
			return status(INTERNAL_SERVER_ERROR);
		}
		return ok("Got json from request!\n");
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result deleteCollection() {
		JsonNode json = request().body().asJson();

		if(json == null)
			return badRequest("Empty json!");

		if(json.has("dbId")) {
			ObjectId id = new ObjectId(json.get("dbId").asText());
			DB.getCollectionDAO().deleteById(id);
			return ok("Collection deleted succesfully!\n");
		} else {
			return ok("No collection id specified!\n");
		}
	}
}
