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

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.bson.types.ObjectId;
import org.mongodb.morphia.geo.GeoJson;
import org.mongodb.morphia.geo.Point;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import controllers.RecordResourceController.CollectionAndRecordsCounts;
import controllers.WithController.Profile;
import db.DB;
import elastic.ElasticSearcher;
import elastic.ElasticSearcher.SearchOptions;
import elastic.ElasticUtils;
import model.Campaign;
import model.EmbeddedMediaObject.MediaVersion;
import model.annotations.Annotation;
import model.annotations.Annotation.AnnotationAdmin;
import model.annotations.Annotation.AnnotationScore;
import model.annotations.Annotation.MotivationType;
import model.annotations.bodies.AnnotationBody;
import model.annotations.bodies.AnnotationBodyColorTagging;
import model.annotations.bodies.AnnotationBodyGeoTagging;
import model.annotations.bodies.AnnotationBodyTagging;
import model.annotations.targets.AnnotationTarget;
import model.basicDataTypes.Language;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import model.resources.RecordResource;
import model.resources.ThesaurusObject;
import model.resources.WithResourceType;
import model.usersAndGroups.User;
import play.Logger;
import play.Logger.ALogger;
import play.cache.Cached;
import play.libs.F.Promise;
import play.libs.F.Some;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import sources.core.ParallelAPICall;
import utils.Tuple;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class AnnotationController extends Controller {

	public static final ALogger log = Logger.of(AnnotationController.class);

	public static Result addAnnotation() throws ClientProtocolException, IOException {
		ObjectNode error = Json.newObject();
		JsonNode json = request().body().asJson();
		if (json == null) {
			error.put("error", "Invalid JSON");
			return badRequest(error);
		}
		if (WithController.effectiveUserDbId() == null) {
			error.put("error", "User not logged in");
			return badRequest(error);
		}
		Annotation annotation = getAnnotationFromJson(json);
		if (annotation.getMotivation().equals(MotivationType.GeoTagging)) {
			AnnotationBodyGeoTagging body = (AnnotationBodyGeoTagging) annotation.getBody();
			String geonameId = body.getUri();
			String url = "http://api.geonames.org/getJSON?geonameId=" + geonameId
					+ "&maxRows=10&type=json&username=SECRET_KEY";
			HttpClient client = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(url);
			HttpResponse response = client.execute(request);
			JsonNode jsonRes = Json.parse(response.getEntity().getContent());
			double lat = jsonRes.get("lat").asDouble();
			double lng = jsonRes.get("lng").asDouble();
			Point point = GeoJson.point(lat, lng);
			body.setUri("http://sws.geonames.org/" + geonameId + "/");
			body.setCoordinates(point);
			body.setCountryName(jsonRes.get("countryName").asText());
			body.setLabel(new MultiLiteral(Language.DEFAULT, jsonRes.get("asciiName").asText()));
		}

		if (annotation.getTarget().getRecordId() == null) {
			RecordResource record = DB.getRecordResourceDAO().getByExternalId(annotation.getTarget().getExternalId());
			if (record == null)
				return badRequest();
			annotation.getTarget().setRecordId(record.getDbId());
			annotation.getTarget().setWithURI("/record/" + record.getDbId());
		}
		Annotation existingAnnotation = DB.getAnnotationDAO().getExistingAnnotation(annotation);
		if (existingAnnotation == null) {
			DB.getAnnotationDAO().makePermanent(annotation);
			annotation.setAnnotationWithURI("/annotation/" + annotation.getDbId());
			DB.getAnnotationDAO().makePermanent(annotation); // is this needed for a second time?
			DB.getRecordResourceDAO().addAnnotation(annotation.getTarget().getRecordId(), annotation.getDbId(),
					WithController.effectiveUserId());
		} else {
			ArrayList<AnnotationAdmin> annotators = existingAnnotation.getAnnotators();
			ObjectId userId = WithController.effectiveUserDbId();
			for (AnnotationAdmin a : annotators) {
				if (a.getWithCreator().equals(userId)) {
					return ok(Json.toJson(existingAnnotation));
				}
			}
			DB.getAnnotationDAO().addAnnotators(existingAnnotation.getDbId(), annotation.getAnnotators());
			annotation = DB.getAnnotationDAO().get(existingAnnotation.getDbId());
		}
		return ok(Json.toJson(annotation));
	}

	public static Result approveAnnotation(String id) {
		try {
			ObjectId oid = new ObjectId(id);
			DB.getAnnotationDAO().addApprove(oid, WithController.effectiveUserDbId());
			ObjectId recordId = DB.getRecordResourceDAO().getByAnnotationId(oid).getDbId();
			DB.getRecordResourceDAO().addAnnotator(recordId, WithController.effectiveUserId());
			ElasticUtils.update(DB.getRecordResourceDAO().getByAnnotationId(oid));
			return ok();
		} catch (Exception e) {
			return internalServerError();
		}
	}

	public static Result approveAnnotationObject(String id) {
		try {
			ObjectNode error = Json.newObject();

			JsonNode json = request().body().asJson();
			if (json == null) {
				error.put("error", "Invalid JSON");
				return badRequest();
			}
			if (WithController.effectiveUserDbId() == null) {
				error.put("error", "User not logged in");
				return badRequest();
			}
			AnnotationAdmin administrative = getAnnotationAdminFromJson(json, WithController.effectiveUserDbId());

			ObjectId oid = new ObjectId(id);
			DB.getAnnotationDAO().addApproveObject(oid, WithController.effectiveUserDbId(), administrative);
			ObjectId recordId = DB.getRecordResourceDAO().getByAnnotationId(oid).getDbId();
			DB.getRecordResourceDAO().addAnnotator(recordId, WithController.effectiveUserId());
			ElasticUtils.update(DB.getRecordResourceDAO().getByAnnotationId(oid));
			return ok();
		} catch (Exception e) {
			log.error("", e);
			return internalServerError();
		}
	}

	public static Result rejectAnnotation(String id) {
		try {
			ObjectId oid = new ObjectId(id);
			DB.getAnnotationDAO().addReject(oid, WithController.effectiveUserDbId());
			ObjectId recordId = DB.getRecordResourceDAO().getByAnnotationId(oid).getDbId();
			DB.getRecordResourceDAO().addAnnotator(recordId, WithController.effectiveUserId());
			ElasticUtils.update(DB.getRecordResourceDAO().getByAnnotationId(oid));
			return ok();
		} catch (Exception e) {
			return internalServerError();
		}
	}

	public static Result rejectAnnotationObject(String id) {
		try {
			ObjectNode error = Json.newObject();

			JsonNode json = request().body().asJson();
			if (json == null) {
				error.put("error", "Invalid JSON");
				return badRequest();
			}
			if (WithController.effectiveUserDbId() == null) {
				error.put("error", "User not logged in");
				return badRequest();
			}
			AnnotationAdmin administrative = getAnnotationAdminFromJson(json, WithController.effectiveUserDbId());

			ObjectId oid = new ObjectId(id);
			DB.getAnnotationDAO().addRejectObject(oid, WithController.effectiveUserDbId(), administrative);
			ObjectId recordId = DB.getRecordResourceDAO().getByAnnotationId(oid).getDbId();
			DB.getRecordResourceDAO().addAnnotator(recordId, WithController.effectiveUserId());
			ElasticUtils.update(DB.getRecordResourceDAO().getByAnnotationId(oid));
			return ok();
		} catch (Exception e) {
			log.error("", e);
			return internalServerError();
		}
	}

	public static Result unscoreAnnotation(String id) {
		try {
			ObjectId oid = new ObjectId(id);
			DB.getAnnotationDAO().removeScore(oid, WithController.effectiveUserDbId());
			ObjectId recordId = DB.getRecordResourceDAO().getByAnnotationId(oid).getDbId();
			DB.getRecordResourceDAO().removeAnnotator(recordId, WithController.effectiveUserId());
			ElasticUtils.update(DB.getRecordResourceDAO().getByAnnotationId(oid));
			return ok();
		} catch (Exception e) {
			return internalServerError();
		}
	}

	public static Result unscoreAnnotationObject(String id) {
		try {
			ObjectId oid = new ObjectId(id);
			DB.getAnnotationDAO().removeScoreObject(oid, WithController.effectiveUserDbId());
			ObjectId recordId = DB.getRecordResourceDAO().getByAnnotationId(oid).getDbId();
			DB.getRecordResourceDAO().removeAnnotator(recordId, WithController.effectiveUserId());
			ElasticUtils.update(DB.getRecordResourceDAO().getByAnnotationId(oid));
			return ok();
		} catch (Exception e) {
			return internalServerError();
		}
	}

	public static Result approveMultipleAnnotations(List<String> id) {
		boolean ok = true;
		ObjectId user = WithController.effectiveUserDbId();
		for (String singleId : id) {
			try {
				ObjectId oid = new ObjectId(singleId);
				DB.getAnnotationDAO().addApprove(oid, user);
				ObjectId recordId = DB.getRecordResourceDAO().getByAnnotationId(oid).getDbId();
				DB.getRecordResourceDAO().addAnnotator(recordId, WithController.effectiveUserId());
				ElasticUtils.update(DB.getRecordResourceDAO().getByAnnotationId(oid));
			} catch (Exception e) {
				ok = false;
				log.error(e.getMessage(), e);
			}
		}
		if (ok) {
			return (ok());
		} else {
			return internalServerError();
		}
	}

	public static Result rejectMultipleAnnotations(List<String> id) {
		boolean ok = true;
		ObjectId user = WithController.effectiveUserDbId();
		for (String singleId : id) {
			try {
				ObjectId oid = new ObjectId(singleId);
				DB.getAnnotationDAO().addReject(oid, user);
				ObjectId recordId = DB.getRecordResourceDAO().getByAnnotationId(oid).getDbId();
				DB.getRecordResourceDAO().removeAnnotator(recordId, WithController.effectiveUserId());
				ElasticUtils.update(DB.getRecordResourceDAO().getByAnnotationId(oid));
			} catch (Exception e) {
				ok = false;
				log.error(e.getMessage(), e);
			}
		}
		if (ok) {
			return (ok());
		} else {
			return internalServerError();
		}
	}

	public static Result unscoreMultipleAnnotations(List<String> id) {
		boolean ok = true;
		ObjectId user = WithController.effectiveUserDbId();
		for (String singleId : id) {
			try {
				ObjectId oid = new ObjectId(singleId);
				DB.getAnnotationDAO().removeScore(oid, user);
				ElasticUtils.update(DB.getRecordResourceDAO().getByAnnotationId(oid));
			} catch (Exception e) {
				ok = false;
				log.error(e.getMessage(), e);
			}
		}
		if (ok) {
			return (ok());
		} else {
			return internalServerError();
		}
	}

	public static Annotation addAnnotation(Annotation annotation, ObjectId user) {

		annotation = updateAnnotationAdmin(annotation, user);

		Annotation existingAnnotation = DB.getAnnotationDAO().getExistingAnnotation(annotation);

		if (existingAnnotation == null) {
			DB.getAnnotationDAO().makePermanent(annotation);
			annotation.setAnnotationWithURI("/annotation/" + annotation.getDbId());
			// DB.getAnnotationDAO().makePermanent(annotation);
			DB.getRecordResourceDAO().addAnnotation(annotation.getTarget().getRecordId(), annotation.getDbId(),
					user.toHexString());
		} else {
			ArrayList<AnnotationAdmin> annotators = existingAnnotation.getAnnotators();
			for (AnnotationAdmin a : annotators) {
				if (a.getWithCreator().equals(user)) {
					return existingAnnotation;
				}
			}
			DB.getAnnotationDAO().addAnnotators(existingAnnotation.getDbId(), annotation.getAnnotators());
			annotation = DB.getAnnotationDAO().get(existingAnnotation.getDbId());
		}
		return annotation;
	}

	public static Result getAnnotation(String id) {
		try {
			Annotation annotation = DB.getAnnotationDAO().getById(new ObjectId(id));
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
			annotatedRecords += DB.getRecordResourceDAO().countAnnotatedRecords(collectionId);
			annotations += DB.getRecordResourceDAO().countAnnotations(collectionId);
		}
		result.put("annotatedRecords", annotatedRecords);
		result.put("annotations", annotations);
		return ok(result);
	}

	// caching 5 minutes should be ok
	@Cached(key = "annotationCounts", duration = 300)
	public static Promise<Result> getDeepAnnotationCount(String groupId) {
		ObjectNode result = Json.newObject();
		// everything on the frontend thread queue
		return ParallelAPICall.createPromise(() -> {
			try {
				ObjectId group = new ObjectId(groupId);
				List<ObjectId> allSharedRecords = DB.getRecordResourceDAO().allIdsSharedWithGroup(group);

				// iterate ove all Annotations and filter for records in this list
				// first make it a set
				Set<ObjectId> allIds = new HashSet<ObjectId>();
				allIds.addAll(allSharedRecords);

				// get an all annotation iterator (ideally project to the fields you want
				utils.Counter count = new utils.Counter(0), approved = new utils.Counter(0),
						rejected = new utils.Counter(0);

				DB.getAnnotationDAO().findAll("target.recordId", "score", "annotators.withCreator")
						.filter(annotation -> allIds.contains(annotation.getTarget().getRecordId()))
						.forEach(annotation -> {
							count.increase();
							if (annotation.getScore() != null) {
								if (annotation.getScore().getApprovedBy() != null)
									approved.increase(annotation.getScore().getApprovedBy().size());
								if (annotation.getScore().getRejectedBy() != null)
									rejected.increase(annotation.getScore().getRejectedBy().size());
							}
						});

				result.put("all", count.getValue()).put("approvals", approved.getValue()).put("rejects",
						rejected.getValue());

				return ok(result);
			} catch (Exception e) {
				log.error("Annotation counter failed", e);
				return badRequest();
			}
		}, ParallelAPICall.Priority.FRONTEND);
	}

	@Cached(key = "leaderBoard", duration = 300)
	public static Promise<Result> leaderboard(String groupId) {
		ArrayNode result = Json.newObject().arrayNode();

		// everything on the frontend thread queue
		return ParallelAPICall.createPromise(() -> {
			try {
				ObjectId group = new ObjectId(groupId);
				List<ObjectId> allSharedRecords = DB.getRecordResourceDAO().allIdsSharedWithGroup(group);

				// iterate ove all Annotations and filter for records in this list
				// first make it a set
				Set<ObjectId> allIds = new HashSet<ObjectId>();
				allIds.addAll(allSharedRecords);

				Hashtable<String, utils.Counter> counts = new Hashtable<String, utils.Counter>() {
					public utils.Counter get(Object key) {
						utils.Counter c = super.get(key);
						if (c == null) {
							c = new utils.Counter(0);
							put(key.toString(), c);
						}
						return c;
					}
				};

				// get an all annotation iterator (ideally project to the fields you want

				DB.getAnnotationDAO().findAll("target.recordId", "score", "annotators.withCreator")
						.filter(annotation -> allIds.contains(annotation.getTarget().getRecordId()))
						.forEach(annotation -> {
							for (Object obj : annotation.getAnnotators()) {
								AnnotationAdmin aa = (AnnotationAdmin) obj;
								if (aa.getWithCreator() != null) {
									String userId = aa.getWithCreator().toHexString();
									counts.get(userId).increase();
								}
							}
							if (annotation.getScore() != null) {
								if (annotation.getScore().getApprovedBy() != null) {
									for (Object obj : annotation.getScore().getApprovedBy()) {
										AnnotationAdmin aa = (AnnotationAdmin) obj;
										if (aa.getWithCreator() != null) {
											String userId = aa.getWithCreator().toHexString();
											counts.get(userId).increase();
										}
									}
									/*
									 * for(ObjectId userId: annotation.getScore().getApprovedBy()) counts.get(
									 * userId.toHexString()).increase();
									 */
								}
								if (annotation.getScore().getRejectedBy() != null) {
									for (Object obj : annotation.getScore().getRejectedBy()) {
										AnnotationAdmin aa = (AnnotationAdmin) obj;
										if (aa.getWithCreator() != null) {
											String userId = aa.getWithCreator().toHexString();
											counts.get(userId).increase();
										}
									}
									/*
									 * for(ObjectId userId: annotation.getScore().getRejectedBy()) counts.get(
									 * userId.toHexString()).increase();
									 */
								}
							}
						});

				// now find the 10 most active
				ArrayList<Map.Entry<String, utils.Counter>> resList = new ArrayList<Map.Entry<String, utils.Counter>>();
				resList.addAll(counts.entrySet());
				resList.sort((Map.Entry<String, utils.Counter> a, Map.Entry<String, utils.Counter> b) -> {
					return Integer.compare(b.getValue().getValue(), a.getValue().getValue());
				});

				int count = 10;
				for (Map.Entry<String, utils.Counter> e : resList) {
					User u = DB.getUserDAO().get(new ObjectId(e.getKey()));
					if (u != null) {
						ObjectNode unode = Json.newObject();
						unode.put("userId", u.getDbId().toHexString());
						unode.put("annotationCount", e.getValue().getValue());
						if ((u.getAvatar() != null) && (u.getAvatar().get(MediaVersion.Square) != null))
							unode.put("avatar", u.getAvatar().get(MediaVersion.Square));
						unode.put("username", u.getUsername());
						result.add(unode);
						count--;
						if (count == 0)
							break;
					}
				}
				return ok(result);
			} catch (Exception e) {
				log.error("Annotation leaderboard failed", e);
				return badRequest();
			}
		}, ParallelAPICall.Priority.FRONTEND);
	}

	public static Result getUserAnnotations(String userId, String project, String campaign, int offset, int count) {
		ObjectId withUser = null;

		if (userId != null)
			withUser = new ObjectId(userId);
		else
			withUser = WithController.effectiveUserDbId();
		if (withUser == null)
			return badRequest(Json.parse("{ 'error' : 'No user defined' }"));
		long annotatedRecords = DB.getAnnotationDAO().countUserAnnotatedRecords(withUser, project, campaign);
		long createdCount = DB.getAnnotationDAO().countUserCreatedAnnotations(withUser, project, campaign);
		long approvedCount = DB.getAnnotationDAO().countUserUpvotedAnnotations(withUser, project, campaign);
		long rejectedCount = DB.getAnnotationDAO().countUserDownvotedAnnotations(withUser, project, campaign);
		long annotationCount = createdCount + approvedCount + rejectedCount;
		JsonNode recordsList = getUserAnnotatedRecords(withUser, project, campaign, offset, count);
		ObjectNode recordsWithCount = Json.newObject();
		recordsWithCount.set("records", recordsList);
		recordsWithCount.put("annotationCount", annotationCount);
		recordsWithCount.put("createdCount", createdCount);
		recordsWithCount.put("approvedCount", approvedCount);
		recordsWithCount.put("rejectedCount", rejectedCount);
		recordsWithCount.put("annotatedRecordsCount", annotatedRecords);

		long karmaPoints = 0;
		if (campaign != null) {
			ObjectId creatorId;
			List<Annotation> annotations;
			Annotation current;
			annotations = DB.getAnnotationDAO().getCampaignAnnotations(campaign);
			int i;
			for (i = 0; i < annotations.size(); i++) {
				current = annotations.get(i);
				if (current.getScore() != null) {
					if ((current.getScore().getRejectedBy() != null) && (current.getScore().getApprovedBy() != null)) {
						if (current.getScore().getRejectedBy().size() >= current.getScore().getApprovedBy().size()) {
							if (current.getAnnotators() != null) {
								if ((AnnotationAdmin) (current.getAnnotators()).get(0) != null) {
									creatorId = ((AnnotationAdmin) (current.getAnnotators()).get(0)).getWithCreator();
									if (creatorId.equals(withUser)) {
										karmaPoints++;
									}
								}
							}
						}
					}
					else if (current.getScore().getRejectedBy() != null){
						if (current.getAnnotators() != null) {
							if ((AnnotationAdmin) (current.getAnnotators()).get(0) != null) {
								creatorId = ((AnnotationAdmin) (current.getAnnotators()).get(0)).getWithCreator();
								if (creatorId.equals(withUser)) {
									karmaPoints++;
								}
							}
						}
					}
				}
			}
		}
		recordsWithCount.put("karmaPoints", karmaPoints);

		return ok(recordsWithCount);
	}

	public static JsonNode getUserAnnotatedRecords(ObjectId withUser, String project, String campaign, int offset,
			int count) {
		ArrayNode recordsList = Json.newObject().arrayNode();
		List<RecordResource> records = DB.getRecordResourceDAO().getAnnotatedRecords(withUser, project, campaign,
				offset, count);
		for (RecordResource record : records) {
			Some<String> locale = new Some(Language.DEFAULT.toString());
			RecordResource profiledRecord = record.getRecordProfile(Profile.MEDIUM.toString());
			WithController.filterResourceByLocale(locale, profiledRecord);
			recordsList.add(Json.toJson(profiledRecord));
		}
		return recordsList;
	}

	public static Result getUserAnnotatedRecords(String userId, String project, String campaign, int offset,
			int count) {
		ObjectId withUser = null;
		if (userId != null)
			withUser = new ObjectId(userId);
		else
			withUser = WithController.effectiveUserDbId();
		if (withUser == null)
			return badRequest(Json.parse("{\"error\" : \"No user defined\" }"));
		return ok(getUserAnnotatedRecords(withUser, project, campaign, offset, count));

	}

	public static Annotation getAnnotationFromJson(JsonNode json) {
		return getAnnotationFromJson(json, WithController.effectiveUserDbId());
	}

	public static Annotation getAnnotationFromJson(JsonNode json, ObjectId userId) {
		try {
			Annotation annotation = Json.fromJson(json, Annotation.class);

			Class<?> clazz = Class.forName("model.annotations.bodies.AnnotationBody" + annotation.getMotivation());
			AnnotationBody body = (AnnotationBody) Json.fromJson(json.get("body"), clazz);
			body.adjustLabel();
			annotation.setBody(body);
			AnnotationAdmin administrative = new AnnotationAdmin();
			administrative.setWithCreator(userId);
			if (json.has("generated")) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
				try {
					administrative.setGenerated(sdf.parse(json.get("generated").asText()));
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
				administrative.setConfidence(json.get("body").get("confidence").asDouble());
			}
			if (json.has("confidence")) {
				administrative.setConfidence(json.get("confidence").asDouble());
			}
			annotation.setAnnotators(new ArrayList(Arrays.asList(administrative)));
			return annotation;
		} catch (ClassNotFoundException e) {
			return new Annotation();
		}
	}

	public static AnnotationAdmin getAnnotationAdminFromJson(JsonNode json, ObjectId userId) {
		AnnotationAdmin administrative = new AnnotationAdmin();
		administrative.setWithCreator(userId);
		if (json.has("generated")) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			try {
				administrative.setGenerated(sdf.parse(json.get("generated").asText()));
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
		if (json.has("confidence")) {
			administrative.setConfidence(json.get("confidence").asDouble());
		}

		return administrative;
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
			Annotation annotation = DB.getAnnotationDAO().getById(annotationId, Arrays.asList("annotators"));
			if (annotation == null)
				return badRequest();
			ArrayList<AnnotationAdmin> annotators = annotation.getAnnotators();
			AnnotationAdmin annotator = null;
			for (AnnotationAdmin a : annotators) {
				if (a.getWithCreator().equals(withUser)) {
					annotator = a;
				}
			}
			boolean isCreator = false;
			if (annotator == null) {
				if (annotators.get(0).getGenerator() != null) {
					if (annotation.getAnnotators() != null) {
						String[] generator = annotators.get(0).getGenerator().split(" ");
						String campaignName = generator[1];
						Campaign campaign = DB.getCampaignDAO().getCampaignByName(campaignName);
						if (campaign != null && campaign.getCreators() != null
								&& campaign.getCreators().contains(withUser))
							isCreator = true;
					}
					if (!isCreator)
						return forbidden();
				}
			}
			if (isCreator) {
				DB.getAnnotationDAO().deleteAnnotation(annotationId);
				return ok();
			}
			if (annotators.size() == 1) {
				DB.getAnnotationDAO().deleteAnnotation(annotationId);
				return ok();
			} else {
				DB.getAnnotationDAO().removeAnnotators(annotationId, Arrays.asList(annotator));
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
					add(new Tuple<ObjectId, WithAccess.Access>(new ObjectId(groupId), Access.READ));
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
					add(WithResourceType.SimpleCollection.toString().toLowerCase());
					add(WithResourceType.Exhibition.toString().toLowerCase());
				}
			});
			/*
			 * SearchResponse resp = recordSearcher .searchAccessibleCollections(options);
			 * List<String> colIds = new ArrayList<String>(); resp.getHits().forEach((h) ->
			 * { colIds.add(h.getId()); return; });
			 * 
			 * 
			 * Search for records of this space
			 * 
			 * options.accessList.clear(); options.setFilterType("or"); //
			 * options.addFilter("_all", term); // options.addFilter("description", term);
			 * // options.addFilter("keywords", term); recordSearcher.setTypes(new
			 * ArrayList<String>() { { addAll(Elastic.allTypes);
			 * remove(WithResourceType.SimpleCollection.toString() .toLowerCase());
			 * remove(WithResourceType.Exhibition.toString().toLowerCase()); } }); resp =
			 * recordSearcher.searchInSpecificCollections( term.toLowerCase(), colIds,
			 * options); List<ObjectId> recordIds = new ArrayList<ObjectId>();
			 * resp.getHits().forEach((h) -> { recordIds.add(new ObjectId(h.getId()));
			 * return; }); if (recordIds.size() > 0) { List<RecordResource> hits =
			 * DB.getRecordResourceDAO().getByIds( recordIds); result.put("hits",
			 * Json.toJson(hits)); } else { result.put("hits",
			 * Json.newObject().arrayNode()); }
			 */

		} catch (Exception e) {
			log.error("Search encountered a problem", e);
			return internalServerError(e.getMessage());
		}

		return ok(result);
	}

	private static void addAutomaticAnnotation(RecordResource record, String colour, Double score)
			throws ClientProtocolException, IOException {
		Annotation annotation = new Annotation();
		AnnotationAdmin annotationAdmin = new AnnotationAdmin();
		annotationAdmin.setWithCreator(new ObjectId("5c599c9c4c74793211258bbd")); // EFHA
		annotationAdmin.setGenerator("Image Analysis");
		annotationAdmin.setCreated(new Date());
		annotationAdmin.setGenerated(new Date());
		annotationAdmin.setLastModified(new Date());
		annotationAdmin.setConfidence(score);
		annotation.setAnnotators(new ArrayList(Arrays.asList(annotationAdmin)));
		annotation.setTarget(new AnnotationTarget());
		annotation.getTarget().setRecordId(record.getDbId());
		annotation.getTarget().setExternalId(record.getAdministrative().getExternalId());
		annotation.getTarget().setWithURI(record.getAdministrative().getWithURI());
		annotation.setMotivation(MotivationType.ColorTagging);
		annotation.setBody(new AnnotationBodyColorTagging());
		ThesaurusObject to = DB.getThesaurusDAO().getByPrefLabel(colour);
		((AnnotationBodyColorTagging) annotation.getBody()).setUriVocabulary("fashion");
		((AnnotationBodyColorTagging) annotation.getBody()).setLabel(new MultiLiteral(to.getSemantic().getPrefLabel()));
		((AnnotationBodyColorTagging) annotation.getBody()).setUri(to.getSemantic().getUri());
		annotation.getTarget().setWithURI("/record/" + record.getDbId());
		DB.getAnnotationDAO().makePermanent(annotation);
		annotation.setAnnotationWithURI("/annotation/" + annotation.getDbId());
		DB.getAnnotationDAO().makePermanent(annotation); // is this needed for a second time?
		DB.getRecordResourceDAO().addAnnotation(annotation.getTarget().getRecordId(), annotation.getDbId(),
				"5c599c9c4c74793211258bbd");
		return;
	}

	public static Result importAutomaticColourAnnotations()
			throws SQLException, ClassNotFoundException, ClientProtocolException, IOException {
		Class.forName("com.mysql.jdbc.Driver");
		java.sql.Connection con = java.sql.DriverManager.getConnection("jdbc:mysql://panic.image.ntua.gr:3306/portal",
				"USERNAME", "PASSWORD");
		// Database properties
		// String url = "jdbc:mysql://panic.image.ntua.gr:3306/";
		// the portal String dbName = "portal";
		// the DB name String driver = "com.mysql.jdbc.Driver";
		// the DB driver String userName = "USERNAME";
		// the portal username String password = "PASSWORD";
		// the portal password
		List<ObjectId> collectionIds = Arrays.asList(new ObjectId("5cf7891d4c74797c594a8383"),
				new ObjectId("5cf78d264c74797c594a8500"), new ObjectId("5cf8d8ca4c74797c594a9105"));
		for (ObjectId collectionId : collectionIds) {
			Iterator<RecordResource> i = DB.getRecordResourceDAO().getByCollection(collectionId).iterator();
			int errors = 0;
			while (i.hasNext()) {
				RecordResource record = i.next();
				String[] split = record.getDescriptiveData().getIsShownBy().toString().split("/");
				String filename = split[split.length - 1];
				java.sql.Statement stmt = con.createStatement();
				java.sql.ResultSet rs = stmt
						.executeQuery("select * from image_analysis where filename=\"" + filename + "\"");
				int j = 0;
				while (rs.next()) {
					for (int k = 3; k < 14; k++) {
						String[] colourInfo = rs.getString(k).split(":");
						String colour = colourInfo[0];
						Double score = Double.parseDouble(colourInfo[1]);
						if (score > 0.01) {
							addAutomaticAnnotation(record, colour, score);
						}
					}
					System.out.println(
							filename + ": " + rs.getString(3) + "  " + rs.getString(4) + "  " + rs.getString(5));
					j++;
				}
				if (j != 1) {
					errors++;
					System.out.println("ERROR: " + filename + " has " + j + "entries");
				}
			}
			System.out.println("Finished with " + errors + " errors");
		}
		con.close();
		return ok();
	}

	private static String getEuropeanaRecord(AnnotationTarget target) {
		String externalUrl = target.getExternalId();
		if (externalUrl == null) {
			try {
				externalUrl = DB.getRecordResourceDAO().get(target.getRecordId()).getAdministrative().getExternalId();
			} catch (Exception e) {
				Logger.error("Cannot find external url for record: " + target.getRecordId());
				return null;
			}
		}
		if (externalUrl.startsWith("/")) {
			externalUrl = "http://data.europeana.eu/item" + externalUrl;
		}
		return externalUrl;
	}

	private static JsonNode tranformToEuropeanaModel(Annotation ann) {
		ObjectNode europeanaAnnotation = Json.newObject();
		europeanaAnnotation.put("type", "Annotation");
		// Creation info
		AnnotationAdmin createdAdmin = (AnnotationAdmin) ann.getAnnotators().get(0);
		User creator = DB.getUserDAO().get(createdAdmin.getWithCreator());
		String creatorName = creator.getFirstName() + " " + creator.getLastName();
		europeanaAnnotation.put("created", createdAdmin.getCreated().toInstant().toString());
		europeanaAnnotation.put("creator", Json.newObject().put("type", "Person").put("name", creatorName));
		// Generated info
		ObjectNode generator = Json.newObject().put("type", "Software").put("name", "CrowdHeritage").put("homepage",
				"https://crowdheritage.eu");
		europeanaAnnotation.put("generator", generator);
		// Motivation, Body and Target
		europeanaAnnotation.put("motivation", "tagging");
		AnnotationBodyTagging body = (AnnotationBodyTagging) ann.getBody();
		if (body.getUri().equals("http://wordnet-rdf.princeton.edu/wn30/13645812-n"))
			body.setUri("http://thesaurus.europeanafashion.eu/thesaurus/10405");
		if (body.getUri().startsWith("https://www.wikidata.org")) {
			body.setUri(body.getUri().replace("https://", "http://"));
		}
		if (body.getUri().startsWith("http://www.wikidata.org/wiki/")) {
			body.setUri(body.getUri().replace("http://www.wikidata.org/wiki/", "http://www.wikidata.org/entity/"));
		}
		if (body.getUri().equals("http://www.mimo-db.eu/InstrumentsKeywords")) {
			body.setUri("http://www.mimo-db.eu/InstrumentsKeywords/2204");
		}
		if (body.getUri().contains("http://bib.arts.kuleuven.be/")) {
			String[] s = body.getUri().split("/");
			String photoId = s[s.length - 1];
			String alternativeURI = getGettyOrWikidataUri(photoId);
			if (alternativeURI != null) {
				System.out.println(
						"Alternative URI found for http://bib.arts.kuleuven.be/photoVocabulary/en/concepts/-photoVocabulary-"
								+ photoId + " : " + alternativeURI);
				body.setUri(alternativeURI);
			} else {
				System.out.println(
						"No alternative URI found: hhttp://bib.arts.kuleuven.be/photoVocabulary/en/concepts/-photoVocabulary-"
								+ photoId);
			}

		}
		europeanaAnnotation.put("body", body.getUri());
		europeanaAnnotation.put("target", getEuropeanaRecord(ann.getTarget()));
		return europeanaAnnotation;
	}

	private static String getGettyOrWikidataUri(String photoId) {
		try {
			Reader in = new FileReader("kal.csv");
			Iterable<CSVRecord> records = CSVFormat.DEFAULT.withDelimiter(',').withHeader().parse(in);
			for (CSVRecord record : records) {
				if (record.get("photoId").equals(photoId)) {
					if (record.get("gettyId") != null && (record.get("gettyId").trim().equals("") == false)) {
						return "http://vocab.getty.edu/aat/" + record.get("gettyId");
					} else if (record.get("wiki") != null) {
						return record.get("wiki");
					} else {
						return null;
					}
				}
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static class Score {
		int upvotes;
		int downvotes;
		int finalScore;

		private Score(int upvotes, int downvotes, int finalScore) {
			this.upvotes = upvotes;
			this.downvotes = downvotes;
			this.finalScore = finalScore;
		}
	}

	private static Score getFinalScore(Annotation annotation) {
		AnnotationScore score = annotation.getScore();
		if (annotation.getScore() == null) {
			return new Score(0, 0, 0);
		}
		int app = score.getApprovedBy() == null ? 0 : score.getApprovedBy().size();
		int rej = score.getRejectedBy() == null ? 0 : score.getRejectedBy().size();
		int finalScore = app - rej;
		return new Score(app, rej, finalScore);
	}

//	Criteria to pick the colours for the caltwalk colours campaign.
//	Publish only the first colour in terms of net score,
//	if two colours have the same net score we publish both,
// 	if three or more have the same net score we publish just
//	the first two with the higher number of upvotes.
//	If the colour “multicolor” appears as top colour together
//	with another one (same net score), we only publish the “multicolor”.
	private static void checkAndAdd(Annotation annotation, List<Annotation> existingAnnotations) {
		Score newScore = getFinalScore(annotation);
		if (existingAnnotations.size() == 0 || existingAnnotations.size() > 2) {
			Logger.error("Annotations for publishing are not conforming to the criteria");
			return;
		}
		boolean removed = false;
		removed = existingAnnotations.removeIf(a -> getFinalScore(a).finalScore < newScore.finalScore);
		removed = removed | existingAnnotations.removeIf(a -> (getFinalScore(a).finalScore == newScore.finalScore)
				&& ((AnnotationBodyTagging) annotation.getBody()).getLabel().get(Language.EN).get(0)
						.equals("multicoloured"));
		if (removed) {
			existingAnnotations.add(annotation);
			return;
		}
		if (getFinalScore(existingAnnotations.get(0)).finalScore == newScore.finalScore) {
			existingAnnotations.add(annotation);
			existingAnnotations.stream().sorted((a1, a2) -> {
				int up1 = getFinalScore(a1).upvotes;
				int up2 = getFinalScore(a2).upvotes;
				if (up1 == up2) {
					return 0;
				}
				return up1 > up2 ? -1 : 1;
			}).collect(Collectors.toList());
		}
		if (existingAnnotations.size() > 2) {
			existingAnnotations.remove(2);
		}
	}

	private static void prepareAnnotationForPublish(Map<ObjectId, List<Annotation>> annotationsPerRecord,
			Annotation annotation) {
		Score currentFinalScore = getFinalScore(annotation);
		if (currentFinalScore.finalScore <= 0)
			return;
		ObjectId recordId = annotation.getTarget().getRecordId();
		if (annotationsPerRecord.containsKey(recordId) == false) {
			annotationsPerRecord.put(recordId, new ArrayList<Annotation>(Arrays.asList(annotation)));
		} else {
			checkAndAdd(annotation, annotationsPerRecord.get(recordId));
		}
	}

	public static Result exportAnnotationsForEuropeanaApi(String project, String campaignName, int maxRanking,
			boolean mark) {
		List<Annotation> annotations = DB.getAnnotationDAO().getCampaignAnnotations(project + " transport-travel");
		annotations.addAll(DB.getAnnotationDAO().getCampaignAnnotations(project + " style-design"));
		annotations.addAll(DB.getAnnotationDAO().getCampaignAnnotations(project + " film-theatre"));
		annotations.addAll(DB.getAnnotationDAO().getCampaignAnnotations(project + " ladies"));
		List<Annotation> published = annotations;
		if (campaignName.equals("colours-catwalk")) {
			annotations.addAll(DB.getAnnotationDAO().getCampaignAnnotations("Image Analysis"));
			HashMap<ObjectId, List<Annotation>> annotationsPerRecord = new HashMap<ObjectId, List<Annotation>>();
			for (Annotation annotation : annotations) {
				prepareAnnotationForPublish(annotationsPerRecord, annotation);
			}
			published = annotationsPerRecord.values().stream().flatMap(List::stream).collect(Collectors.toList());
		}
		if (mark) {
			published.stream().filter(a -> getFinalScore(a).finalScore < 1)
					.forEach(a -> DB.getAnnotationDAO().unmarkAnnotationForPublish(a.getDbId()));
			published.stream().filter(a -> getFinalScore(a).finalScore > 0)
					.forEach(a -> DB.getAnnotationDAO().markAnnotationForPublish(a.getDbId()));
		}
		List<JsonNode> res = published.stream().filter(a -> getFinalScore(a).finalScore > 0)
				.map(a -> tranformToEuropeanaModel(a)).filter(a -> (a.get("target") != null))
				.collect(Collectors.toList());
		return ok(Json.toJson(res));
	}
}
