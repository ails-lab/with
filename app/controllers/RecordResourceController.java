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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import actors.annotation.AnnotationControlActor;
import actors.annotation.AnnotatorActor;
import actors.annotation.RequestAnnotatorActor;
import actors.annotation.TextAnnotatorActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import annotators.AnnotatorConfig;
import controllers.WithController.Profile;
import db.DAO;
import db.DB;
import model.DescriptiveData;
import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.annotations.Annotation;
import model.annotations.Annotation.AnnotationAdmin;
import model.annotations.Annotation.AnnotationScore;
import model.annotations.ContextData;
import model.annotations.ContextData.ContextDataType;
import model.annotations.selectors.PropertyTextFragmentSelector;
import model.annotations.targets.AnnotationTarget;
import model.basicDataTypes.Language;
import model.basicDataTypes.MultiLiteral;
import model.resources.RecordResource;
import model.resources.collection.CollectionObject;
import model.resources.collection.CollectionObject.CollectionAdmin;
import model.resources.collection.SimpleCollection;
import model.usersAndGroups.User;
import play.Logger;
import play.Logger.ALogger;
import play.data.validation.Validation;
import play.libs.Akka;
import play.libs.F.Option;
import play.libs.F.Some;
import play.libs.Json;
import play.mvc.Result;
import utils.Tuple;

/**
 * @author mariaral
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RecordResourceController extends WithResourceController {

	public static final ALogger log = Logger.of(RecordResourceController.class);

	/**
	 * Retrieve a resource metadata. If the format is defined the specific
	 * serialization of the object is returned
	 *
	 * @param id     the resource id
	 * @param format the resource serialization
	 * @return the resource metadata
	 */
	public static Result getRecordResource(String id, Option<String> format, String profile, Option<String> locale) {
		ObjectNode result = Json.newObject();
		try {
			RecordResource record = DB.getRecordResourceDAO().get(new ObjectId(id));
			Result response = errorIfNoAccessToRecord(Action.READ, new ObjectId(id));
			if (!response.toString().equals(ok().toString()))
				return response;
			else {
				// filter out all context annotations refering to collections to
				// which the user has no read access rights
				// filterContextData(record);
				if (format.isDefined()) {
					String formats = format.get();
					if (formats.equals("contentOnly")) {
						return ok(Json.toJson(record.getContent()));
					} else {
						if (formats.equals("noContent")) {
							record.getContent().clear();
							RecordResource profiledRecord = record.getRecordProfile(profile);
							filterResourceByLocale(locale, profiledRecord);
							return ok(Json.toJson(profiledRecord));
						} else if (record.getContent() != null && record.getContent().containsKey(formats)) {
							return ok(record.getContent().get(formats).toString());
						} else {
							result.put("error", "Resource does not contain representation for format " + formats);
							return play.mvc.Results.notFound(result);
						}
					}
				} else
					return ok(Json.toJson(record));
			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	/**
	 * From the given List of collection ids retrieve count random records. There
	 * can be an error if one of the collections is not public. If there are not
	 * count records in the given collections, fewer are returned.
	 * 
	 * @param collectionIds
	 * @param count
	 * @return
	 */
	public static Result getRandomRecordsFromCollections(List<String> collectionIds, int count, boolean hideMine) {
		Random r = new Random();
		ArrayList<String> collectionIdCopy = new ArrayList<String>();
		// allow for comma separated list of ids
		String[] commaSplit = collectionIds.get(0).split(",");
		if (commaSplit != null) {
			collectionIdCopy.addAll(Arrays.asList(commaSplit));
		}
		// if there are more than one element, its made from standard parameters
		if (collectionIds.size() > 1) {
			collectionIdCopy.addAll(collectionIds.subList(1, collectionIds.size()));
		}
		if (hideMine) {
			List<ObjectId> collectionObjectIds = collectionIdCopy.stream().map(c -> new ObjectId(c))
					.collect(Collectors.toList());
			List<RecordResource> records = DB.getRecordResourceDAO().getRandomRecordsWithNoContributions(collectionObjectIds, count,
					WithController.effectiveUserId());
			Collections.shuffle(records);
			return ok(Json.toJson(records));
		}
		Collections.shuffle(collectionIdCopy, r);
		ArrayNode res = Json.newObject().arrayNode();
		// get a random Collection
		// check access
		// is size big enough for remaining count, then shuffle the first count entries
		// around with other entries
		// else get them all
		while ((count > 0) && collectionIdCopy.size() > 0) {
			String collectionId = collectionIdCopy.remove(0);
			try {
				CollectionObject sc = DB.getCollectionObjectDAO().getById(new ObjectId(collectionId));
				if (!sc.getAdministrative().getAccess().getIsPublic())
					return badRequest("Access to collection " + collectionId + " which is not public.");
				List<ContextData<?>> resList = sc.getCollectedResources();

				// mini optimization, dont shuffle huge lists if you only want few records
				if (count < (resList.size() / 10)) {
					HashSet<ObjectId> pickedRecordIds = new HashSet<ObjectId>();
					while (count > 0) {
						int idx = r.nextInt(resList.size());
						ObjectId recId = resList.get(idx).getTarget().getRecordId();
						if (!pickedRecordIds.contains(recId)) {
							pickedRecordIds.add(recId);
							RecordResource rec = DB.getRecordResourceDAO().get(recId);
							res.add((ObjectNode) Json.toJson(rec));
							count--;
						}
					}
				} else {
					// when you retrieve a lot of records, this should be the right method
					Collections.shuffle(resList, r);

					for (ContextData elem : resList) {
						if (count == 0)
							break;
						ObjectId recId = elem.getTarget().getRecordId();
						RecordResource rec = DB.getRecordResourceDAO().get(recId);
						res.add((ObjectNode) Json.toJson(rec));
						count--;
					}
				}
			} catch (Exception e) {
				log.error("Collection " + collectionId, e);
			}

		}

		return ok(res);
	}

	public static Result updateRights(String id) {
		ObjectId recordDbId = new ObjectId(id);
		DB.getWithResourceDAO().computeAndUpdateRights(recordDbId);
		return ok("record rights updated");
	}

	public static Result updateAllRights() {
		DB.getWithResourceDAO().findAll("_id").forEach((r) -> {
			ObjectId recordDbId = r.getDbId();
//			System.out.println(recordDbId);
			DB.getWithResourceDAO().computeAndUpdateRights(recordDbId);
		});
		// DB.getWithResourceDAO().computeAndUpdateRights(recordDbId);
		return ok("All record rights updated");
	}

	/**
	 * Edits the WITH resource according to the JSON body. For every field mentioned
	 * in the JSON body it either edits the existing one or it adds it (in case it
	 * doesn't exist)
	 *
	 * @param id
	 * @return the edited resource
	 */
	// TODO check restrictions (unique fields e.t.c)
	// TODO: edit contextData separately ONLY for collections for which the user
	// has access
	public static Result editRecordResource(String id) {
		ObjectNode error = Json.newObject();
		ObjectId recordDbId = new ObjectId(id);
		JsonNode json = request().body().asJson();
		try {
			if (json == null) {
				error.put("error", "Invalid JSON");
				return badRequest(error);
			} else {
				Result response = errorIfNoAccessToRecord(Action.EDIT, new ObjectId(id));
				if (!response.toString().equals(ok().toString()))
					return response;
				else {
					// TODO Check the JSON
					DB.getRecordResourceDAO().editRecord("", recordDbId, json);
					return ok("Record edited.");
				}
			}
		} catch (Exception e) {
			error.put("error", e.getMessage());
			return internalServerError(error);
		}
	}

	public static Result editContextData(String collectionId) throws Exception {
		ObjectNode error = Json.newObject();
		ObjectNode json = (ObjectNode) request().body().asJson();
		if (json == null) {
			error.put("error", "Invalid JSON");
			return badRequest(error);
		} else {
			String contextDataType = null;
			if (json.has("contextDataType")) {
				contextDataType = json.get("contextDataType").asText();
				ContextDataType dataType;
				if (contextDataType != null & (dataType = ContextDataType.valueOf(contextDataType)) != null) {
					Class clazz;
					try {
						int position = ((ObjectNode) json.get("target")).remove("position").asInt();
						if (dataType.equals(ContextDataType.ExhibitionData)) {
							clazz = Class.forName("model.annotations." + contextDataType);
							ContextData newContextData = (ContextData) Json.fromJson(json, clazz);
							ObjectId collectionDbId = new ObjectId(collectionId);
							// int position =
							// newContextData.getTarget().getPosition();
							if (collectionId != null && DB.getCollectionObjectDAO().existsEntity(collectionDbId)) {
								// filterContextData(record);
								Result response = errorIfNoAccessToCollection(Action.EDIT, collectionDbId);
								if (!response.toString().equals(ok().toString()))
									return response;
								else {
									DB.getCollectionObjectDAO().updateContextData(collectionDbId, newContextData,
											position);

								}
							}
						}
						return ok("Edited context data.");
					} catch (ClassNotFoundException e) {
						log.error("", e);
						return internalServerError(error);
					}
				} else
					return badRequest("Context data type should be one of supported types: ExhibitionData.");
			} else
				return badRequest("The contextDataType should be defined.");
		}
	}

	// TODO: Are we checking rights for every record alone?
	public static Result list(String collectionId, int start, int count) {
		List<RecordResource> records = DB.getRecordResourceDAO()
				.getByCollectionBetweenPositions(new ObjectId(collectionId), start, start + count);
		return ok(Json.toJson(records));
	}

	// TODO: Remove favorites

	/**
	 * @return
	 */
	public static Result getFavorites() {
		ObjectNode result = Json.newObject();
		if (loggedInUser() == null) {
			return forbidden();
		}
		ObjectId userId = new ObjectId(loggedInUser());
		CollectionObject favorite;
		ObjectId favoritesId;
		if ((favorite = DB.getCollectionObjectDAO().getByOwnerAndLabel(userId, null, "_favorites")) == null) {
			favoritesId = CollectionObjectController.createFavorites(userId);
		} else {
			favoritesId = favorite.getDbId();
		}
		List<RecordResource> records = DB.getRecordResourceDAO().getByCollection(favoritesId);
		if (records == null) {
			result.put("error", "Cannot retrieve records from database");
			return internalServerError(result);
		}
		ArrayNode recordsList = Json.newObject().arrayNode();
		for (RecordResource record : records) {
			recordsList.add(record.getAdministrative().getExternalId());
		}
		return ok(recordsList);
	}

	public static ObjectNode validateRecord(RecordResource record) {
		ObjectNode result = Json.newObject();
		Set<ConstraintViolation<RecordResource>> violations = Validation.getValidator().validate(record);
		if (!violations.isEmpty()) {
			ArrayNode properties = Json.newObject().arrayNode();
			for (ConstraintViolation<RecordResource> cv : violations) {
				properties.add(Json.parse("{\"" + cv.getPropertyPath() + "\":\"" + cv.getMessage() + "\"}"));
			}
			result.put("error", properties);
			log.error(properties.toString());
			return result;
		} else {
			return null;
		}
	}

	public static Result annotateRecord(String recordId) {
		ObjectNode result = Json.newObject();
		try {
//			Result response = errorIfNoAccessToRecord(Action.EDIT, new ObjectId(recordId));
//			if (!response.toString().equals(ok().toString())) {
//				return response;
//			} else {
			JsonNode json = request().body().asJson();

			List<AnnotatorConfig> annConfigs = AnnotatorConfig.createAnnotationConfigs(json);
			ObjectId user = WithController.effectiveUserDbId();

			Random rand = new Random();
			String requestId = "AR" + (System.currentTimeMillis() + Math.abs(rand.nextLong())) + ""
					+ Math.abs(rand.nextLong());

			Akka.system().actorOf(
					Props.create(AnnotationControlActor.class, requestId, new ObjectId(recordId), user, true),
					requestId);
			ActorSelection ac = Akka.system().actorSelection("user/" + requestId);

			annotateRecord(recordId, user, annConfigs, ac);

			ac.tell(new AnnotationControlActor.AnnotateRequestsEnd(), ActorRef.noSender());

			return ok();
//			}				
		} catch (Exception e) {
			e.printStackTrace();
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	public static void annotateRecord(String recordId, ObjectId user, List<AnnotatorConfig> annConfigs,
			ActorSelection ac) throws Exception {

		RecordResource record = DB.getRecordResourceDAO().get(new ObjectId(recordId));
		DescriptiveData dd = null;

		for (AnnotatorConfig annConfig : annConfigs) {
			Map<String, Object> props = annConfig.getProps();

			boolean imageAnnotator = props.get(AnnotatorActor.IMAGE_ANNOTATOR) != null
					&& (boolean) props.get(AnnotatorActor.IMAGE_ANNOTATOR);
			boolean textAnnotator = props.get(AnnotatorActor.TEXT_ANNOTATOR) != null
					&& (boolean) props.get(AnnotatorActor.TEXT_ANNOTATOR);

			if (imageAnnotator) {
				AnnotationTarget target = new AnnotationTarget();
				target.setRecordId(record.getDbId());

				List<String> urls = new ArrayList<String>();
				for (HashMap<MediaVersion, EmbeddedMediaObject> t : (List<HashMap<MediaVersion, EmbeddedMediaObject>>) record
						.getMedia()) {
					EmbeddedMediaObject emo = t.get(MediaVersion.Original);
					if (emo != null) {
						urls.add(emo.getWithUrl());
					}
				}

				ac.tell(new AnnotationControlActor.AnnotateRequest(user, urls.toArray(new String[urls.size()]), target,
						props, (RequestAnnotatorActor.Descriptor) annConfig.getAnnotatorDesctriptor()),
						ActorRef.noSender());

			} else if (textAnnotator) {
				if (dd == null) {
					dd = record.getDescriptiveData();
				}
				for (String p : (String[]) props.get(AnnotatorActor.TEXT_FIELDS)) {
					Method method = dd.getClass().getMethod("get" + p.substring(0, 1).toUpperCase() + p.substring(1));

					Object res = method.invoke(dd);
					if (res != null && res instanceof MultiLiteral) {
						MultiLiteral value = (MultiLiteral) res;

						value.remove(Language.DEFAULT);
						if (value.size() == 1 && value.contains(Language.UNKNOWN)) {
							List<String> unk = value.remove(Language.UNKNOWN);
							value.addMultiLiteral(Language.EN, unk);
						}

						for (Language lang : value.getLanguages()) {
							if (lang == Language.UNKNOWN) {
								continue;
							}

							if (value.get(lang) != null) {
								for (String text : value.get(lang)) {
									AnnotationTarget target = new AnnotationTarget();
									target.setRecordId(record.getDbId());

									PropertyTextFragmentSelector selector = new PropertyTextFragmentSelector();
									selector.setOrigValue(text);
									selector.setOrigLang(lang);
									selector.setProperty(p);

									target.setSelector(selector);

									ac.tell(new AnnotationControlActor.AnnotateText(user, text, target, props,
											(TextAnnotatorActor.Descriptor) annConfig.getAnnotatorDesctriptor(), lang),
											ActorRef.noSender());
								}
							}
						}
					}
				}
			}
		}
	}

	public static Result deleteRejectedAnnotations(String rid) {
		ObjectNode result = Json.newObject();
		try {
//			Result response = errorIfNoAccessToRecord(Action.EDIT, new ObjectId(rid));
//			if (!response.toString().equals(ok().toString())) {
//				return response;
//			} else {
			ObjectId userId = WithController.effectiveUserDbId();

			for (Annotation annotation : DB.getAnnotationDAO().getUserAnnotations(userId, new ObjectId(rid),
					Arrays.asList("annotators", "score.rejectedBy"))) {
				AnnotationScore as = annotation.getScore();
				if (as != null) {
					ArrayList<AnnotationAdmin> rej = as.getRejectedBy();
					if (rej != null) {
						for (AnnotationAdmin id : rej) {
							if (id.getWithCreator().equals(userId)) {
								ArrayList<AnnotationAdmin> annotators = annotation.getAnnotators();
								AnnotationAdmin annotator = null;
								for (AnnotationAdmin a : annotators) {
									if (a.getWithCreator().equals(userId)) {
										annotator = a;
									}
								}

								ObjectId annId = annotation.getDbId();
								if (annotators.size() == 1) {
									DB.getAnnotationDAO().deleteAnnotation(annId);
								} else {
									DB.getAnnotationDAO().removeAnnotators(annId, Arrays.asList(annotator));
								}
							}
						}
					}
				}
			}

			return ok();
//			}				
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	public static Result getRandomRecords(String groupId, int batchCount) {
		ObjectId group = new ObjectId(groupId);
		CollectionAndRecordsCounts collectionsAndCount = getCollsAndCountAccessiblebyGroup(group);
		// int collectionCount = collectionsAndCount.collectionsRecordCount.size();
		// generate batchCount unique numbers from 0 to totalRecordsCount
		Random rng = new Random();
		Set<Integer> randomNumbers = new HashSet<Integer>();
		List<RecordResource> records = new ArrayList<RecordResource>();
		while (randomNumbers.size() < batchCount) {
			Integer next = rng.nextInt(collectionsAndCount.totalRecordsCount);
			randomNumbers.add(next);
		}
		for (Integer random : randomNumbers) {
			int colPosition = -1;
			int previousRecords = 0;
			int recordCount = 0;
			while (random > previousRecords) {
				recordCount = collectionsAndCount.collectionsRecordCount.get(++colPosition).y;
				previousRecords += recordCount;
			}
			int recordPosition = random - (previousRecords - recordCount) - 1;
			CollectionObject collection = DB.getCollectionObjectDAO().getById(
					collectionsAndCount.collectionsRecordCount.get(colPosition).x, Arrays.asList("collectedResources"));
			ContextData contextData = (ContextData) collection.getCollectedResources().get(recordPosition);
			ObjectId recordId = contextData.getTarget().getRecordId();
			records.add(DB.getRecordResourceDAO().getById(recordId));
		}
		ArrayNode recordsList = Json.newObject().arrayNode();
		for (RecordResource record : records) {
			Some<String> locale = new Some(Language.DEFAULT.toString());
			RecordResource profiledRecord = record.getRecordProfile(Profile.MEDIUM.toString());
			filterResourceByLocale(locale, profiledRecord);
			recordsList.add(Json.toJson(profiledRecord));
		}
		return ok(recordsList);
	}

	/**
	 * Get records from this group (read write owned doesnt matter ) How many and
	 * how many annotations they have to have minimally. The minimum can only be
	 * 1,2,3 as each needs a different index to work.
	 * 
	 * @param groupId
	 * @param count
	 * @param minimum
	 * @return
	 */
	public static Result getRandomAnnotatedRecords(String groupId, int count, int minimum) {
		List<RecordResource> records = new ArrayList<RecordResource>();
		ArrayNode recordsList = Json.newObject().arrayNode();

		ObjectId groupObjId = null;
		if (StringUtils.isNotEmpty(groupId)) {
			groupObjId = new ObjectId(groupId);
		}

		records = DB.getRecordResourceDAO().getRandomAnnotatedRecords(groupObjId, count, minimum);

		for (RecordResource record : records) {
			Some<String> locale = new Some(Language.DEFAULT.toString());
			RecordResource profiledRecord = record.getRecordProfile(Profile.MEDIUM.toString());
			filterResourceByLocale(locale, profiledRecord);
			recordsList.add(Json.toJson(profiledRecord));
		}

		return ok(recordsList);
	}

	static CollectionAndRecordsCounts getCollsAndCountAccessiblebyGroup(ObjectId groupId) {
		List<CollectionObject> collections = DB.getCollectionObjectDAO().getAccessibleByGroupAndPublic(groupId);
		CollectionAndRecordsCounts colAndRecs = new CollectionAndRecordsCounts();
		for (CollectionObject col : collections) {
			int entryCount = ((CollectionAdmin) col.getAdministrative()).getEntryCount();
			colAndRecs.collectionsRecordCount.add(new Tuple(col.getDbId(), entryCount));
			colAndRecs.totalRecordsCount += entryCount;
		}
		return colAndRecs;
	}

	public static class CollectionAndRecordsCounts {
		List<Tuple<ObjectId, Integer>> collectionsRecordCount = new ArrayList<Tuple<ObjectId, Integer>>();
		int totalRecordsCount = 0;
	}

	public static Result getAnnotations(String id, String motivation) {
		ObjectNode result = Json.newObject();
		try {
//			RecordResource record = DB.getRecordResourceDAO().get(new ObjectId(id));
//			Result response = errorIfNoAccessToRecord(Action.READ, new ObjectId(id));
//			if (!response.toString().equals(ok().toString()))
//				return response;
//			else {
			ArrayNode array = result.arrayNode();
//				record.fillAnnotations();

			List<Annotation.MotivationType> motivations = new ArrayList<>();
			if (motivation != null && motivation.length() > 0) {
				for (String s : motivation.split(",")) {
					try {
						motivations.add(Annotation.MotivationType.valueOf(s.trim()));
					} catch (Exception ex) {

					}
				}
			}

			List<Annotation> anns = DB.getAnnotationDAO().getByRecordId(new ObjectId(id), motivations);

			for (Annotation ann : anns) {
				JsonNode json = Json.toJson(ann);
				for (Iterator<JsonNode> iter = json.get("annotators").elements(); iter.hasNext();) {
					JsonNode annotator = iter.next();

					if (annotator.has("withCreator")) {
						String userId = annotator.get("withCreator").asText();

						User user = DB.getUserDAO().getById(new ObjectId(userId));
						((ObjectNode) annotator).put("username", user.getUsername());
					}
				}

				array.add(json);
			}
			return ok(array);
//			}				
		} catch (Exception e) {
			e.printStackTrace();
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	public static Result getMultipleRecordResources(List<String> id, String profile, Option<String> locale) {
		ArrayNode result = Json.newObject().arrayNode();
		for (String singleId : id) {
			try {
				ObjectId recordId = new ObjectId(singleId);
				Result response = errorIfNoAccessToRecord(Action.READ, recordId);
				if (!response.toString().equals(ok().toString()))
					continue;
				RecordResource record = DB.getRecordResourceDAO().getByIdAndExclude(recordId, Arrays.asList("content"));
				RecordResource profiledRecord = record.getRecordProfile(profile);
				filterResourceByLocale(locale, profiledRecord);
				result.add(Json.toJson(profiledRecord));
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return ok(result);
	}
	
	public static Result getRecordIdsByAnnLabel(String label, List<String> generators) {
		ArrayNode result = Json.newObject().arrayNode();
		List<Annotation> anns = DB.getAnnotationDAO().getByLabel(generators, label);
		for (Annotation ann : anns) {
			// Do NOT return the actual records, TOO SLOW
			// Instead, return an array with the record ids
			result.add(Json.toJson(ann.getTarget().getRecordId().toHexString()));
			/*
			ObjectId recordId = ann.getTarget().getRecordId();
			try {
				Result response = errorIfNoAccessToRecord(Action.READ, recordId);
				if (!response.toString().equals(ok().toString()))
					continue;
				RecordResource record = DB.getRecordResourceDAO().getByIdAndExclude(recordId, Arrays.asList("content"));
				RecordResource profiledRecord = record.getRecordProfile(profile);
				filterResourceByLocale(locale, profiledRecord);
				result.add(Json.toJson(profiledRecord));
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			*/
		}
		return ok(result);
	}
}
