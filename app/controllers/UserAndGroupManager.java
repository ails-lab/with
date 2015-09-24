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
import model.Rights.Access;
import model.UserGroup;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.data.validation.Validation;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class UserAndGroupManager extends Controller {

	public static final ALogger log = Logger.of(UserGroup.class);


	
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
