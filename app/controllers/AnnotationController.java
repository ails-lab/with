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

import model.annotations.Annotation;
import model.annotations.Annotation.AnnotationAdmin;
import model.annotations.Annotation.MotivationType;
import model.annotations.bodies.AnnotationBody;
import model.basicDataTypes.Language;
import model.resources.RecordResource;

import org.bson.types.ObjectId;

import play.libs.F.Some;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import controllers.WithController.Profile;
import db.DB;

@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public class AnnotationController extends Controller {

	public static Result addAnnotation() {
		ObjectNode error = Json.newObject();
		JsonNode json = request().body().asJson();
		if (json == null) {
			error.put("error", "Invalid JSON");
			return badRequest();
		}
		Annotation annotation = getAnnotationFromJson(json);
		Annotation existingAnnotation = DB.getAnnotationDAO()
				.getExistingAnnotation(annotation);
		if (existingAnnotation == null) {
			DB.getAnnotationDAO().makePermanent(annotation);
			DB.getRecordResourceDAO().addAnnotation(
					annotation.getTarget().getRecordId(), annotation.getDbId());
		} else {
			DB.getAnnotationDAO().addAnnotators(existingAnnotation.getDbId(),
					annotation.getAnnotators());
		}
		return ok();
	}

	public static Result getUserAnnotations(int offset, int count) {
		ObjectId withUser = WithController.effectiveUserDbId();
		List<RecordResource> records = DB.getRecordResourceDAO()
				.getAnnotatedRecords(withUser, offset, count);
		long annotationCount = DB.getAnnotationDAO().countUserAnnotations(
				withUser);
		ObjectNode recordsWithCount = Json.newObject();
		ArrayNode recordsList = Json.newObject().arrayNode();
		for (RecordResource record : records) {
			Some<String> locale = new Some(Language.DEFAULT.toString());
			RecordResource profiledRecord = record
					.getRecordProfile(Profile.MEDIUM.toString());
			WithController.filterResourceByLocale(locale, profiledRecord);
			recordsList.add(Json.toJson(profiledRecord));
		}
		recordsWithCount.put("records", recordsList);
		recordsWithCount.put("annotationCount", annotationCount);
		return ok(recordsWithCount);
	}

	private static Annotation getAnnotationFromJson(JsonNode json) {
		try {
			Annotation annotation = Json.fromJson(json, Annotation.class);
			Class<?> clazz = Class
					.forName("model.annotations.bodies.AnnotationBody"
							+ annotation.getMotivation());
			AnnotationBody body = (AnnotationBody) Json.fromJson(
					json.get("body"), clazz);
			annotation.setBody(body);
			AnnotationAdmin administrative = new AnnotationAdmin();
			administrative.setWithCreator(WithController.effectiveUserDbId());
			administrative.setCreated(new Date());
			if (json.has("genarator"))
				administrative.setGenerator(json.get("generator").asText());
			if (json.has("body.confidence"))
				administrative.setConfidence(json.get("body.confidence")
						.asDouble());
			annotation.setAnnotators(new ArrayList(Arrays
					.asList(administrative)));
			return annotation;
		} catch (ClassNotFoundException e) {
			return new Annotation();
		}
	}

}
