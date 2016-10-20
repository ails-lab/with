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
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.Tuple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import controllers.RecordResourceController.CollectionAndRecordsCounts;
import controllers.WithController.Profile;
import db.DB;
import elastic.ElasticSearcher;
import elastic.ElasticSearcher.SearchOptions;

@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public class AnnotationController extends Controller {

	public static final ALogger log = Logger.of(AnnotationController.class);

	public static Result addAnnotation() {
		ObjectNode error = Json.newObject();
		JsonNode json = request().body().asJson();
		if (json == null) {
			error.put("error", "Invalid JSON");
			return badRequest();
		}
		Annotation annotation = getAnnotationFromJson(json);
		if (annotation.getTarget().getRecordId() == null) {
			RecordResource record = DB.getRecordResourceDAO().getByExternalId(
					annotation.getTarget().getExternalId());
			if (record == null)
				return badRequest();
			annotation.getTarget().setRecordId(record.getDbId());
			annotation.getTarget().setWithURI("/record/" + record.getDbId());
		}
		Annotation existingAnnotation = DB.getAnnotationDAO()
				.getExistingAnnotation(annotation);
		if (existingAnnotation == null) {
			DB.getAnnotationDAO().makePermanent(annotation);
			annotation.setAnnotationWithURI("/annotation/"
					+ annotation.getDbId());
			DB.getAnnotationDAO().makePermanent(annotation);
			DB.getRecordResourceDAO().addAnnotation(
					annotation.getTarget().getRecordId(), annotation.getDbId());
		} else {
			ArrayList<AnnotationAdmin> annotators = existingAnnotation
					.getAnnotators();
			for (AnnotationAdmin a : annotators) {
				if (a.getWithCreator().equals(
						WithController.effectiveUserDbId())) {
					return ok(Json.toJson(existingAnnotation));
				}
			}
			DB.getAnnotationDAO().addAnnotators(existingAnnotation.getDbId(),
					annotation.getAnnotators());
			annotation = DB.getAnnotationDAO()
					.get(existingAnnotation.getDbId());
		}
		return ok(Json.toJson(annotation));
	}
	
	public static Result approveAnnotation(String id) {
		try {			
			DB.getAnnotationDAO().addApprove(new ObjectId(id), WithController.effectiveUserDbId());
			return ok();
		} catch (Exception e) {
			return internalServerError();
		}
	}

	public static Result unscoreAnnotation(String id) {
		try {			
			DB.getAnnotationDAO().removeScore(new ObjectId(id), WithController.effectiveUserDbId());
			return ok();
		} catch (Exception e) {
			return internalServerError();
		}
	}
	
	public static Result rejectAnnotation(String id) {
		try {			
			DB.getAnnotationDAO().addReject(new ObjectId(id), WithController.effectiveUserDbId());
			return ok();
		} catch (Exception e) {
			return internalServerError();
		}
	}
	
	public static Annotation addAnnotation(Annotation annotation, ObjectId user) {
		
		annotation = updateAnnotationAdmin(annotation, user);
		
		Annotation existingAnnotation = DB.getAnnotationDAO().getExistingAnnotation(annotation);

		if (existingAnnotation == null) {
			DB.getAnnotationDAO().makePermanent(annotation);
			annotation.setAnnotationWithURI("/annotation/"
					+ annotation.getDbId());
			DB.getAnnotationDAO().makePermanent(annotation);
			DB.getRecordResourceDAO().addAnnotation(
					annotation.getTarget().getRecordId(), annotation.getDbId());
		} else {
			ArrayList<AnnotationAdmin> annotators = existingAnnotation.getAnnotators();
			for (AnnotationAdmin a : annotators) {
				if (a.getWithCreator().equals(user)) {
					return existingAnnotation;
				}
			}
			DB.getAnnotationDAO().addAnnotators(existingAnnotation.getDbId(),
					annotation.getAnnotators());
			annotation = DB.getAnnotationDAO()
					.get(existingAnnotation.getDbId());
		}
		return annotation;
	}
	

	public static Result getAnnotation(String id) {
		try {
			Annotation annotation = DB.getAnnotationDAO().getById(
					new ObjectId(id));
			return ok(Json.toJson(annotation));
		} catch (Exception e) {
			return internalServerError();
		}
	}

	public static Result getAnnotationCount(String groupId) {
		ObjectNode result = Json.newObject();
		ObjectId group = new ObjectId(groupId);
		CollectionAndRecordsCounts collectionsAndCount = RecordResourceController
				.getCollsAndCountAccessiblebyGroup(group);
		// int totalRecords = collectionsAndCount.totalRecordsCount;
		long annotatedRecords = 0;
		long annotations = 0;
		for (Tuple<ObjectId, Integer> collectionWithCount : collectionsAndCount.collectionsRecordCount) {
			ObjectId collectionId = collectionWithCount.x;
			annotatedRecords += DB.getRecordResourceDAO()
					.countAnnotatedRecords(collectionId);
			annotations += DB.getRecordResourceDAO().countAnnotations(
					collectionId);
		}
		result.put("annotatedRecords", annotatedRecords);
		result.put("annotations", annotations);
		return ok(result);
	}

	
	public static Result getUserAnnotations(int offset, int count) {
		ObjectId withUser = WithController.effectiveUserDbId();
		List<RecordResource> records = DB.getRecordResourceDAO()
				.getAnnotatedRecords(withUser, offset, count);
		long annotatedRecords = DB.getAnnotationDAO()
				.countUserAnnotatedRecords(withUser);
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
		recordsWithCount.put("annotatedRecordsCount", annotatedRecords);
		return ok(recordsWithCount);
	}

	
	public static Annotation getAnnotationFromJson(JsonNode json) {
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
			if (json.has("generated")) {
				SimpleDateFormat sdf = new SimpleDateFormat(
						"yyyy-MM-dd'T'HH:mm:ss'Z'");
				try {
					administrative.setGenerated(sdf.parse(json.get("generated")
							.asText()));
				} catch (ParseException e) {
					log.error(e.getMessage());
					administrative.setGenerated(new Date());
				}
			} else {
				administrative.setGenerated(new Date());
			}
			administrative.setCreated(administrative.getGenerated());
			administrative.setLastModified(new Date());
			if (json.has("generator"))
				administrative.setGenerator(json.get("generator").asText());
			if (json.has("body") && json.get("body").has("confidence")) {
				administrative.setConfidence(json.get("body").get("confidence")
						.asDouble());
			}
			annotation.setAnnotators(new ArrayList(Arrays
					.asList(administrative)));
			return annotation;
		} catch (ClassNotFoundException e) {
			return new Annotation();
		}
	}
	
	private static Annotation updateAnnotationAdmin(Annotation annotation, ObjectId user) {
		ArrayList<AnnotationAdmin> admins = annotation.getAnnotators();
		
		if (admins == null || admins.size() == 0) {
			AnnotationAdmin administrative = new AnnotationAdmin();
			administrative.setWithCreator(user);
			administrative.setCreated(administrative.getGenerated());
			administrative.setLastModified(new Date());

			annotation.setAnnotators(new ArrayList(Arrays.asList(administrative)));

		} else {
			for (AnnotationAdmin administrative : admins) {
				administrative.setWithCreator(user);
				administrative.setCreated(administrative.getGenerated());
				administrative.setLastModified(new Date());
			}
		}
			
		return annotation;
	}

	public static Result deleteAnnotation(String id) {
		try {
			ObjectId withUser = WithController.effectiveUserDbId();
			ObjectId annotationId = new ObjectId(id);
			Annotation annotation = DB.getAnnotationDAO().getById(annotationId,
					Arrays.asList("annotators"));
			if (annotation == null)
				return badRequest();
			ArrayList<AnnotationAdmin> annotators = annotation.getAnnotators();
			AnnotationAdmin annotator = null;
			for (AnnotationAdmin a : annotators) {
				if (a.getWithCreator().equals(withUser)) {
					annotator = a;
				}
			}
			if (annotator == null)
				return forbidden();
			if (annotators.size() == 1) {
				DB.getAnnotationDAO().deleteAnnotation(annotationId);
				return ok();
			} else {
				DB.getAnnotationDAO().removeAnnotators(annotationId,
						Arrays.asList(annotator));
				return ok(Json.toJson(DB.getAnnotationDAO().get(annotationId)));
			}
		} catch (Exception e) {
			return internalServerError();
		}
	}

	public static Result searchRecordsOfGroup(String groupId, String term) {
		ObjectNode result = Json.newObject();

		try {
			List<List<Tuple<ObjectId, Access>>> access = new ArrayList<List<Tuple<ObjectId, Access>>>();
			access.add(new ArrayList<Tuple<ObjectId, Access>>() {
				{
					add(new Tuple<ObjectId, WithAccess.Access>(new ObjectId(
							groupId), Access.READ));
				}
			});
			SearchOptions options = new SearchOptions();
			options.setCount(20);
			options.isPublic = false;

			/*
			 * Search for space collections
			 */
			ElasticSearcher recordSearcher = new ElasticSearcher();
			recordSearcher.setTypes(new ArrayList<String>() {
				{
					add(WithResourceType.SimpleCollection.toString()
							.toLowerCase());
					add(WithResourceType.Exhibition.toString().toLowerCase());
				}
			});
			/*SearchResponse resp = recordSearcher
					.searchAccessibleCollections(options);
			List<String> colIds = new ArrayList<String>();
			resp.getHits().forEach((h) -> {
				colIds.add(h.getId());
				return;
			});


			 * Search for records of this space

			options.accessList.clear();
			options.setFilterType("or");
			// options.addFilter("_all", term);
			// options.addFilter("description", term);
			// options.addFilter("keywords", term);
			recordSearcher.setTypes(new ArrayList<String>() {
				{
					addAll(Elastic.allTypes);
					remove(WithResourceType.SimpleCollection.toString()
							.toLowerCase());
					remove(WithResourceType.Exhibition.toString().toLowerCase());
				}
			});
			resp = recordSearcher.searchInSpecificCollections(
					term.toLowerCase(), colIds, options);
			List<ObjectId> recordIds = new ArrayList<ObjectId>();
			resp.getHits().forEach((h) -> {
				recordIds.add(new ObjectId(h.getId()));
				return;
			});
			if (recordIds.size() > 0) {
				List<RecordResource> hits = DB.getRecordResourceDAO().getByIds(
						recordIds);
				result.put("hits", Json.toJson(hits));
			} else {
				result.put("hits", Json.newObject().arrayNode());
			}*/

		} catch (Exception e) {
			log.error("Search encountered a problem", e);
			return internalServerError(e.getMessage());
		}

		return ok(result);
	}
}
