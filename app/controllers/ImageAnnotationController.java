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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import model.annotations.Annotation;
import model.annotations.Annotation.AnnotationAdmin;
import model.annotations.bodies.AnnotationBody;
import model.basicDataTypes.Language;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import model.resources.RecordResource;
import model.resources.WithResourceType;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Some;
import play.libs.Akka;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.Tuple;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import annotators.AnnotationControlActor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import controllers.RecordResourceController.CollectionAndRecordsCounts;
import controllers.WithController.Profile;
import db.DB;
import elastic.ElasticSearcher;
import elastic.ElasticSearcher.SearchOptions;

@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public class ImageAnnotationController extends Controller {

	public static final ALogger log = Logger.of(ImageAnnotationController.class);

	public static Result imageAnnotationResults() {
		ObjectNode error = Json.newObject();
		JsonNode json = request().body().asJson();
		if (json == null) {
			error.put("error", "Invalid JSON");
			return badRequest();
		}
		
		String requestId = json.get("requestId").asText();
		
		ActorSelection ac = Akka.system().actorSelection("user/" + requestId);
		
		for (JsonNode annotation : (ArrayNode)json.get("data")) {
			ac.tell(new AnnotationControlActor.RequestAnnotationReceived(annotation), ActorRef.noSender());
		}
		
		ac.tell(new AnnotationControlActor.RequestAnnotationsCompleted(requestId), ActorRef.noSender());
		
		return ok();
	}
	


	

}
