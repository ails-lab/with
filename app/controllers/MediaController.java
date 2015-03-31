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
import model.Media;
import model.RecordLink;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class MediaController extends Controller {
	public static final ALogger log = Logger.of( MediaController.class);


	/**
	 * get the specified media object from DB
	 */
	public static Result getMetadataOrFile(String mediaId, boolean file) {


		Media media = null;
		try {
			media = DB.getMediaDAO().findById(new ObjectId(mediaId));
		} catch(Exception e) {
			log.error("Cannot  media document from database", e);
			return internalServerError("Cannot retrieve media document from database");
		}

		if(file) {
			return ok(media.getData()).as(media.getMimeType());
		} else {
			JsonNode result = Json.toJson(media);
			return ok(result);
		}

	}


	/**
	 * edit metadata or the actual file of the media object
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result editMetadataOrFile(String id, boolean file) {
		JsonNode json = request().body().asJson();
		ObjectNode result  = Json.newObject();

		if(file) {
			return ok(Json.newObject().put("message", "not implemeted yet!"));
		} else {
			Media newMedia = null;
			try {
				newMedia = DB.getMediaDAO().findById(new ObjectId(id));

				// set metadata

				if(json.has("width"))
					newMedia.setWidth(json.get("width").asInt());
				if(json.has("height"))
					newMedia.setHeight(json.get("height").asInt());
				if(json.has("duration"))
					newMedia.setDuration((float)json.get("duration").asDouble());
				if(json.has("mimeType"))
					newMedia.setMimeType(json.get("mimeType").asText());
				if(json.has("type"))
					newMedia.setType(json.get("type").asText());


				DB.getMediaDAO().makePermanent(newMedia);
			} catch(Exception e) {
				log.error("Cannot store Media object to database!", e);
				result.put("message", "Cannot store Media object to database");
				return internalServerError(result);
			}
			result.put("message", "Media object succesfully stored!");
			result.put("media_id", id);
			return ok(result);
		}
	}


	/**
	 * delete a media object from database
	 */
	public static Result deleteMedia(String id) {
		ObjectNode result  = Json.newObject();

		try {
			DB.getMediaDAO().deleteById(new ObjectId(id));
		} catch(Exception e) {
			result.put("message", "Cannot delete media object from database");
			return internalServerError(result);
		}
		result.put("message", "Succesfully delete object from database!");
		return ok(result);
	}

	/**
	 * get recordLink thumbnail
	 */
	public static Result getRecordLinkThumbnail(String rlinkId) {
		RecordLink r = null;
		Media thumbnail = null;
		try {
			r = DB.getRecordLinkDAO().getByDbId(new ObjectId(rlinkId));
			thumbnail = r.retrieveThumbnail();
		} catch(Exception e) {
			log.error("Cannot retrieve record link or media document from database", e);
			return internalServerError("Cannot retrieve record link or media document from database");
		}
		return ok(thumbnail.getData()).as(thumbnail.getMimeType());
	}

	/**
	 * Return the thumbnail (in raw bytes) of a collection
	 * @param colId
	 * @return
	 */
	public static Result getCollectionThumbnail(String colId) {

		Collection c = null;
		Media thumbnail = null;
		try {
			c = DB.getCollectionDAO().getById(new ObjectId(colId));
			thumbnail = c.retrieveThumbnail();
		} catch(Exception e) {
			log.error("Cannot retrieve collection or media document from database", e);
			return internalServerError("Cannot retrieve collection or media document from database");
		}
		return ok(thumbnail.getData()).as(thumbnail.getMimeType());
	}

}
