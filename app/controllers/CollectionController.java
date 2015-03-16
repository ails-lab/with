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

import java.util.List;

import model.Collection;
import model.CollectionEntry;
import model.RecordLink;

import org.mongodb.morphia.Key;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.Serializer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

	/**
	 * Action to store a Collection to the database.
	 * Json input with collection fields
	 * @return
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result saveCollection() {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

		if(json == null) {
			result.put("message", "Empty json sent to server!\n");
			return badRequest(result);
		}

		jsonPrettyPrint(json.toString());

		Key<Collection> colKey = null;
		try {
			Collection newCollection = Serializer.jsonToCollectionObject(json);
			colKey = DB.getCollectionDAO().save(newCollection);
		} catch (Exception e) {
			log.error("Cannot save Collection to database", e);
			result.put("message", e.getMessage());
			return internalServerError(result);
		}
		result.put("message", "Collection succesfully stored!");
		result.put("id", colKey.getId().toString());
		return ok(result);
	}


	/**
	 * Action to delete a Collection from database.
	 * Json input, the collection dbId
	 * @return
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result deleteCollection() {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

		if(json == null) {
			result.put("message", "Empty json sent to server!\n");
			return badRequest(result);
		}

		if(json.has("dbId")) {
			String id = json.get("dbId").asText();
			try {
				DB.getCollectionDAO().deleteByID(id);
			} catch(Exception e) {
				log.error("Collection not deleted!", e);
				result.put("message", "Could not delete collection from database!");
				return internalServerError(result);
			}
			result.put("message", "Collection deleted succesfully from database");
			return ok(result);
		} else {
			result.put("message", "Did not specify collection dbId field!");
			return badRequest(result);
		}
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result listUserCollections() {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

		if(json.has("userMail")) {
			String owner = json.get("userMail").asText();
			try {
				List<Collection> userCollections =
						DB.getUserDAO().getUserCollectionsByEmail(owner);
				ArrayNode collections = Json.newObject().arrayNode();
				for(Collection col: userCollections) {
					collections.add(Serializer.collectionToJson(col));
				}
				result.put("user_collections", collections);
				result.put("message", "Succesfully got user collections!");
			} catch(Exception e) {
				log.error("Cannot fetch user collections!", e);
				result.put("message", "Cannot fetch user collections!");
				return internalServerError(result);
			}
			return ok(result);
		} else {
			result.put("message", "Did not specify the user!");
			return badRequest(result);
		}
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result addRecordToCollection() {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

		RecordLink rLink = null;
		String colId = null;
		String recordLinkId = null;
		if(json.has("recordLink_id") && json.has("collection_id")) {
			recordLinkId = json.get("recordLink_id").asText();
			colId = json.get("collection_id").asText();
			rLink = DB.getRecordLinkDAO().getByDbId(recordLinkId);
		} else {
			result.put("message",
					"Cannot retrieve recordLink or collection from db, id is missing!");
			return badRequest(result);
		}

		CollectionEntry colEntry = new CollectionEntry();
		colEntry.setRecordLink(rLink);
		colEntry.setCollection(colId);

		try {
			DB.getCollectionEntryDAO().save(colEntry);
			result.put("message", "CollectionEntry saved succesfully to database!");
			return ok(result);
		} catch(Exception e) {
			log.error("Cannot save CollectionEntry to database!", e);
			result.put("message", "Cannot save CollectionEntry to database!");
			return internalServerError(result);
		}
	}
}