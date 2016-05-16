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
import java.util.HashMap;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;
import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Result;

public class ExhibitionObjectController extends WithResourceController {
	public static final ALogger log = Logger.of(ExhibitionObjectController.class);

	public static Result addBackgroundImg(String exhΙd) {
		ObjectNode result = Json.newObject();
		JsonNode json = request().body().asJson();
		if (json.has("media")) {
			JsonNode mediaJson = json.get("media"); 
			if (mediaJson.isArray()) {
				TypeReference<HashMap<MediaVersion, EmbeddedMediaObject>> typeRef = 
						new TypeReference<HashMap<MediaVersion, EmbeddedMediaObject>>() {};
				 try {
					HashMap<MediaVersion, EmbeddedMediaObject> media = new ObjectMapper().readValue(
						json.traverse(), typeRef);
					DB.getCollectionObjectDAO().updateBackgroundImg(new ObjectId(exhΙd), media);				
				} catch (IOException e) {
					log.error("",e);
					return errorMsg();
				} 
			}
			else return errorMsg();
		}
		else return errorMsg();
		return ok(result);
	}
				
	public static Status errorMsg() {
		return badRequest("Input should have the format {media: [mediaType: {url: url}, ...]}, "
				+ "where mediaType one of (Original, Medium, Thumbnail, Square, Tiny).");
	}

}
