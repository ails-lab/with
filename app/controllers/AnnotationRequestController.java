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

import play.Logger;
import play.Logger.ALogger;
import play.libs.Akka;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import annotators.AnnotationControlActor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public class AnnotationRequestController extends Controller {

	public static final ALogger log = Logger.of(AnnotationRequestController.class);

	public static Result newAnnotations() {
		ObjectNode error = Json.newObject();
		JsonNode json = request().body().asJson();
		if (json == null) {
			error.put("error", "Invalid JSON");
			return badRequest();
		}
		
//		System.out.println("RECEIVED RESPONSE: " + json);
		
		try {
			String requestId = json.get("responseTo").asText();
			int pos = requestId.lastIndexOf("Z");
			String acrequestId = requestId.substring(0, pos); 

			ActorSelection ac = Akka.system().actorSelection("user/" + acrequestId);
			
			for (JsonNode annotation : (ArrayNode)json.get("data")) {
				ac.tell(new AnnotationControlActor.AnnotateRequestPartialResult(annotation), ActorRef.noSender());
			}
			
			ac.tell(new AnnotationControlActor.AnnotateRequestBulkAnswered(requestId), ActorRef.noSender());
			
			return ok();
		} catch (Exception ex) {
			ex.printStackTrace();
			error.put("error", ex.getMessage());
            return internalServerError(error);
		}
	}
	


	

}
