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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.text.Normalizer;
import java.net.URLEncoder;
import javax.validation.ConstraintViolation;
import org.apache.commons.lang3.*;
import org.bson.types.ObjectId;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

import actors.annotation.AnnotationControlActor;
import actors.annotation.AnnotatorActor;
import actors.annotation.RequestAnnotatorActor;
import actors.annotation.TextAnnotatorActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import annotators.AnnotatorConfig;
import db.DB;
import model.DescriptiveData;
import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.annotations.Annotation;
import model.annotations.Annotation.AnnotationAdmin;
import model.annotations.Annotation.AnnotationScore;
import model.annotations.Annotation.MotivationType;
import model.annotations.ContextData;
import model.annotations.ContextData.ContextDataType;
import model.annotations.bodies.AnnotationBody;
import model.annotations.bodies.AnnotationBodyGeoTagging;
import model.annotations.bodies.AnnotationBodyTagging;
import model.annotations.selectors.PropertyTextFragmentSelector;
import model.annotations.targets.AnnotationTarget;
import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.ProvenanceInfo;
import model.resources.CulturalObject;
import model.resources.RecordResource;
import model.resources.collection.CollectionObject;
import model.resources.collection.CollectionObject.CollectionAdmin;
import model.resources.CulturalObject.CulturalObjectData;
import model.usersAndGroups.User;
import model.resources.ThesaurusObject;
import play.Logger;
import play.Logger.ALogger;
import play.data.validation.Validation;
import play.libs.Akka;
import play.libs.F.Option;
import play.libs.F.Some;
import play.libs.Json;
import play.mvc.Result;
import utils.Tuple;
import sources.core.ParallelAPICall;
import controllers.ThesaurusController;
import org.mongodb.morphia.geo.GeoJson;
import org.mongodb.morphia.geo.Point;

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
			List<RecordResource> records = DB.getRecordResourceDAO()
					.getRandomRecordsWithNoContributions(collectionObjectIds, count, WithController.effectiveUserId());
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

	public static Result nerdRecord(String recordId, String tool) {
		ObjectNode request = Json.newObject();
		CulturalObject record = (CulturalObject)DB.getRecordResourceDAO().get(new ObjectId(recordId));
		if (record != null){
			User user = DB.getUserDAO().getUniqueByFieldAndValue("username", "AIDA");
			ObjectId aidaUser = user.getDbId();
			User user2 = DB.getUserDAO().getUniqueByFieldAndValue("username", "Geek");
			ObjectId geekUser = user2.getDbId();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			String createdString = sdf.format(new Date());
			/***** AIDA API Call *****/
			Function<String[],String> AidaAPICall = (String[] input) -> {
				try {
					String text = input[0];
					String property = input[1];
					int limit = input[0].length();
					String text2 = text;
					if (property == "provider"){
						text2 = text + " is the provider of this piece of art or cultural heritage item.";
					}
					else if (property == "country"){
						text2 = text + " is in the country field of this piece of art or cultural heritage item.";
					}
					else if (property == "city"){
						text2 = text + " is in the city field of this piece of art or cultural heritage item.";
					}
					else if (property == "label"){
						text2 = text + " is in the label field of this piece of art or cultural heritage item.";
					}
					else if (property == "creator"){
						text2 = text + " is in the creator of this piece of art or cultural heritage item.";
					}
					HttpClient client = HttpClientBuilder.create().build();
					HttpPost req = new HttpPost("https://gate.d5.mpi-inf.mpg.de/aida/service/disambiguate");
					req.setHeader("content-type", "application/json");
					ObjectNode requestJson = Json.newObject();
					requestJson.put("text", text2);
					req.setEntity(new StringEntity(requestJson.toString(), "UTF-8")); //setting the body for POST request
					HttpResponse response = client.execute(req);
					if (response.getStatusLine().getStatusCode() == 200) {
						String json = EntityUtils.toString(response.getEntity(), "UTF-8"); 
						JsonNode responseJson = new ObjectMapper().readTree(json);
						if (responseJson.has("mentions")){
							JsonNode mentions = responseJson.get("mentions");
							if (responseJson.has("entityMetadata")) {
								JsonNode entityMetadata = responseJson.get("entityMetadata");
								if (mentions.isArray()) {
									for (final JsonNode mentionJson : mentions){
										if (mentionJson.has("bestEntity")){
											JsonNode bestEntity = mentionJson.get("bestEntity");
											String identifier = bestEntity.get("kbIdentifier").asText();
											identifier = StringEscapeUtils.unescapeJava(identifier);
											Annotation annotation = new Annotation();
											AnnotationAdmin administrative = new AnnotationAdmin();
											administrative.setWithCreator(aidaUser);	
											Date createdDate = new Date();
											try {
												createdDate = sdf.parse(createdString);
												administrative.setCreated(createdDate);
											}
											catch(Exception e){}
											String nowString = sdf.format(new Date());
											Date nowDate = new Date();
											try {
												nowDate = sdf.parse(nowString);
												administrative.setGenerated(nowDate);
												administrative.setLastModified(nowDate);								
											}
											catch(Exception e){}
											administrative.setGenerator("AIDA");
											administrative.setConfidence(0.0);
											PropertyTextFragmentSelector selector = new PropertyTextFragmentSelector();

											selector.setOrigValue(text);
											selector.setProperty(property);
									
											if (mentionJson.has("offset") && mentionJson.has("length")){
												Integer AidaStart = mentionJson.get("offset").asInt();
												selector.setStart(AidaStart);

												Integer AidaLength = mentionJson.get("length").asInt();
												selector.setEnd(AidaLength + AidaStart);
												if (AidaLength + AidaStart > limit){
													continue;
												}
											}
											
											if (bestEntity.has("disambiguationScore")){
												Double AidaConfidence = bestEntity.get("disambiguationScore").asDouble();
												administrative.setConfidence(AidaConfidence);
											}
											AnnotationBodyTagging body = new AnnotationBodyTagging();
											AnnotationBodyGeoTagging geoBody = new AnnotationBodyGeoTagging();
											boolean isGeo = false;
											String AidaUri = "";
											String AidaLabel = "";
											if (entityMetadata.has(identifier))
											{
												JsonNode objNode = entityMetadata.get(identifier);
												if (objNode.has("readableRepr")){
													AidaLabel = objNode.get("readableRepr").asText();
													MultiLiteral labels = new MultiLiteral(Language.EN, AidaLabel);
													labels.addLiteral(Language.DEFAULT, AidaLabel);
													body.setLabel(labels);
													geoBody.setLabel(labels);
												}
												if (objNode.has("url")){
													AidaUri = objNode.get("url").asText();
												}
											}
											geoBody.setUriVocabulary("Geonames");
											// If the annotation is a place, use GeoNames

											String wikidataId = "";
											HttpClient client2;
											HttpGet req2;
											HttpResponse response2;
											String json2;
											JsonNode responseJson2;
											if (AidaUri != ""){
												String WikipediaToWikidata = "";
												if (AidaUri.contains("wikipedia")){
													String[] temp = AidaUri.split("/");
													if (temp.length > 0){
														WikipediaToWikidata = "https://en.wikipedia.org/w/api.php?action=query&prop=pageprops&format=json&titles=" + temp[temp.length - 1];
													}
													client2 = HttpClientBuilder.create().build();
													req2 = new HttpGet(WikipediaToWikidata);
													req2.setHeader("content-type", "application/json");
													response2 = client2.execute(req2);
													if (response2.getStatusLine().getStatusCode() == 200) {
														json2 = EntityUtils.toString(response2.getEntity(), "UTF-8"); 
														responseJson2 = new ObjectMapper().readTree(json2);
														if (responseJson2.has("query")){
															if(responseJson2.get("query").has("pages")){
																JsonNode pages = responseJson2.get("query").get("pages");
																for (JsonNode item : pages) {
																	if (item.has("pageprops")){
																		if (item.get("pageprops").has("wikibase_item")){
																			wikidataId = item.get("pageprops").get("wikibase_item").asText();
																		}
																	}
																}
															}
														}
													}
												}
											}
											String geonameId = "";
											if (wikidataId != ""){
												AidaUri = "http://www.wikidata.org/entity/" + wikidataId;
												String query = "SELECT ?geoNamesID ?coordinates WHERE {?place wdt:P1566 ?geoNamesID.  ?place wdt:P625 ?coordinates VALUES (?place)  {(wd:" + wikidataId + ")} }";
												String WikiQueryService = "https://query.wikidata.org/bigdata/namespace/wdq/sparql?format=json&query=" + URLEncoder.encode(query, "UTF-8");
												client2 = HttpClientBuilder.create().build();
												req2 = new HttpGet(WikiQueryService);
												req2.setHeader("content-type", "application/json");
												response2 = client2.execute(req2);
												if (response2.getStatusLine().getStatusCode() == 200) {
													json2 = EntityUtils.toString(response2.getEntity(), "UTF-8");
													responseJson2 = new ObjectMapper().readTree(json2);
													if (responseJson2.has("results")){
														if (responseJson2.get("results").has("bindings")){
															for (JsonNode item : responseJson2.get("results").get("bindings")){
																if (item.has("geoNamesID")){
																	if (item.get("geoNamesID").has("value")){
																		geonameId = item.get("geoNamesID").get("value").asText();
																		geoBody.setUri("http://sws.geonames.org/" + geonameId + "/");
																		isGeo = true;
																	}
																}
																if (item.has("coordinates")){
																	if (item.get("coordinates").has("value")){
																		String inputPoint = item.get("coordinates").get("value").asText();
																		String[] coordinates = ((inputPoint.split("\\(")[1]).split("\\)")[0]).split(" ");
																		Double lat = Double.parseDouble(coordinates[0]);
																		Double lng = Double.parseDouble(coordinates[1]);
																		if (lat != null && lng != null){
																			Point point = GeoJson.point(lat, lng);
																			geoBody.setCoordinates(point);
																		}	
																	}
																}
															}
														}
													}
												}
											}
											body.setUri(AidaUri);
											if (AidaUri.contains("wikipedia")){
												body.setUriVocabulary("Wikipedia");
											}
											else if (AidaUri.contains("wikidata")){
												body.setUriVocabulary("Wikidata");
											}
											selector.setOrigLang(Language.EN);
											annotation.setAnnotators(new ArrayList(Arrays.asList(administrative)));
											if (isGeo){
												annotation.setMotivation(MotivationType.GeoTagging);
											}
											else{
												annotation.setMotivation(MotivationType.Tagging);
											}
											annotation.getTarget().setRecordId(record.getDbId());
											annotation.getTarget().setWithURI("/record/" + record.getDbId());
											annotation.getTarget().setSelector(selector);
											annotation.setPublish(false);
											if (isGeo){
												annotation.setBody(geoBody);
											}
											else{
												annotation.setBody(body);
											}
											/* If annotation already exists add a new annotator, else create the new annotation*/
											Annotation existingAnnotation = DB.getAnnotationDAO().getExistingAnnotation(annotation);
											if (existingAnnotation == null) {
												DB.getAnnotationDAO().makePermanent(annotation);
												annotation.setAnnotationWithURI("/annotation/" + annotation.getDbId());
												DB.getAnnotationDAO().makePermanent(annotation); // is this needed for a second time?
												DB.getRecordResourceDAO().addAnnotation(annotation.getTarget().getRecordId(), annotation.getDbId(), aidaUser.toString());
											} else {
												ArrayList<AnnotationAdmin> annotators = existingAnnotation.getAnnotators();
												for (AnnotationAdmin a : annotators) {
													if (a.getWithCreator().equals(aidaUser)) {
														return "";
													}
												}
												DB.getAnnotationDAO().addAnnotators(existingAnnotation.getDbId(), annotation.getAnnotators());
												annotation = DB.getAnnotationDAO().get(existingAnnotation.getDbId());
											}
										}
									}
								}
							}
						}
						
						return "";
					}
					else{
						return "Error";
					}	
				} catch (Exception e) {
					e.printStackTrace();
					return "Error";
				}
			};

			/***** GEEK API Call *****/
			Function<String[],String> GeekAPICall = (String[] input) -> {
				try {
					String text = input[0];
					String property = input[1];
					int limit = input[0].length();
					String text2 = text;
					if (property == "provider"){
						text2 = text + " is the provider of this piece of art or cultural heritage item.";
					}
					else if (property == "country"){
						text2 = text + " is in the country field of this piece of art or cultural heritage item.";
					}
					else if (property == "city"){
						text2 = text + " is in the city field of this piece of art or cultural heritage item.";
					}
					else if (property == "label"){
						text2 = text + " is in the label field of this piece of art or cultural heritage item.";
					}
					else if (property == "creator"){
						text2 = text + " is in the creator of this piece of art or cultural heritage item.";
					}
					HttpClient client = HttpClientBuilder.create().build();
					HttpPost req = new HttpPost("http://spock.deep.islab.ntua.gr:25000");
					req.setHeader("content-type", "application/json");
					ObjectNode requestJson = Json.newObject();
					requestJson.put("text", text2);
					req.setEntity(new StringEntity(requestJson.toString(), "UTF-8")); //setting the body for POST request
					HttpResponse response = client.execute(req);
					if (response.getStatusLine().getStatusCode() == 200) {
						String json = EntityUtils.toString(response.getEntity(), "UTF-8"); 
						JsonNode responseJson = new ObjectMapper().readTree(json);
						if (responseJson.isArray()) {
							for (final JsonNode objNode : responseJson) {
								Annotation annotation = new Annotation();
								AnnotationAdmin administrative = new AnnotationAdmin();
								administrative.setWithCreator(geekUser);	
								Date createdDate = new Date();
								try {
									createdDate = sdf.parse(createdString);
									administrative.setCreated(createdDate);
								}
								catch(Exception e){}
								String nowString = sdf.format(new Date());
								Date nowDate = new Date();
								try {
									nowDate = sdf.parse(nowString);
									administrative.setGenerated(nowDate);
									administrative.setLastModified(nowDate);								
								}
								catch(Exception e){}
								administrative.setGenerator("GEEK");
								administrative.setConfidence(0.0);
								PropertyTextFragmentSelector selector = new PropertyTextFragmentSelector();

								selector.setOrigValue(text);
								selector.setProperty(property);
						
								if (objNode.has("start_offset") && objNode.has("end_offset")){
									Integer GeekStart = objNode.get("start_offset").asInt();
									selector.setStart(GeekStart);

									Integer GeekEnd = objNode.get("end_offset").asInt();
									selector.setEnd(GeekEnd);
									if (GeekStart > limit || GeekEnd > limit){
										continue;
									}
								}
								
								if (objNode.has("confidence")){
									Double GeekConfidence = objNode.get("confidence").asDouble();
									administrative.setConfidence(GeekConfidence);
								}
								AnnotationBodyTagging body = new AnnotationBodyTagging();
								AnnotationBodyGeoTagging geoBody = new AnnotationBodyGeoTagging();
								boolean isGeo = false;
								String GeekUri = "";
								String GeekLabel = "";
								String wikidataId = "";
								HttpClient client2;
								HttpGet req2;
								HttpResponse response2;
								String json2;
								JsonNode responseJson2;
								if (objNode.has("wiki_url"))
								{
									GeekUri = objNode.get("wiki_url").asText();
									body.setUri(GeekUri);
									body.setUriVocabulary("Wikidata");
									String[] splits = GeekUri.split("/");
									wikidataId = splits[splits.length-1];
									String query = "SELECT DISTINCT * WHERE {wd:" + wikidataId + " rdfs:label ?label . FILTER (langMatches( lang(?label), \"EN\"))}";
									String WikiQueryService = "https://query.wikidata.org/bigdata/namespace/wdq/sparql?format=json&query=" + URLEncoder.encode(query, "UTF-8");
									client2 = HttpClientBuilder.create().build();
									req2 = new HttpGet(WikiQueryService);
									req2.setHeader("content-type", "application/json");
									response2 = client2.execute(req2);
									if (response2.getStatusLine().getStatusCode() == 200) {
										json2 = EntityUtils.toString(response2.getEntity(), "UTF-8");
										responseJson2 = new ObjectMapper().readTree(json2);
										if (responseJson2.has("results")){
											if (responseJson2.get("results").has("bindings")){
												JsonNode items = responseJson2.get("results").get("bindings");
												if (items.isArray()){
													for (final JsonNode item : items){
														if (item.has("label")){
															if (item.get("label").has("value")){
																GeekLabel = item.get("label").get("value").asText();
																MultiLiteral labels = new MultiLiteral(Language.EN, GeekLabel);
																labels.addLiteral(Language.DEFAULT, GeekLabel);
																body.setLabel(labels);
																geoBody.setLabel(labels);
																break;
															}
														}
													}
												}
											}
										}
									}	
								}
								geoBody.setUriVocabulary("Geonames");
								// If the annotation is a place, use GeoNames

								String geonameId = "";
								if (wikidataId != ""){
									String query = "SELECT ?geoNamesID ?coordinates WHERE {?place wdt:P1566 ?geoNamesID.  ?place wdt:P625 ?coordinates VALUES (?place)  {(wd:" + wikidataId + ")} }";
									String WikiQueryService = "https://query.wikidata.org/bigdata/namespace/wdq/sparql?format=json&query=" + URLEncoder.encode(query, "UTF-8");
									client2 = HttpClientBuilder.create().build();
									req2 = new HttpGet(WikiQueryService);
									req2.setHeader("content-type", "application/json");
									response2 = client2.execute(req2);
									if (response2.getStatusLine().getStatusCode() == 200) {
										json2 = EntityUtils.toString(response2.getEntity(), "UTF-8");
										responseJson2 = new ObjectMapper().readTree(json2);
										if (responseJson2.has("results")){
											if (responseJson2.get("results").has("bindings")){
												for (JsonNode item : responseJson2.get("results").get("bindings")){
													if (item.has("geoNamesID")){
														if (item.get("geoNamesID").has("value")){
															geonameId = item.get("geoNamesID").get("value").asText();
															geoBody.setUri("http://sws.geonames.org/" + geonameId + "/");
															isGeo = true;
														}
													}
													if (item.has("coordinates")){
														if (item.get("coordinates").has("value")){
															String inputPoint = item.get("coordinates").get("value").asText();
															String[] coordinates = ((inputPoint.split("\\(")[1]).split("\\)")[0]).split(" ");
															Double lat = Double.parseDouble(coordinates[0]);
															Double lng = Double.parseDouble(coordinates[1]);
															if (lat != null && lng != null){
																Point point = GeoJson.point(lat, lng);
																geoBody.setCoordinates(point);
															}	
														}
													}
												}
											}
										}
									}
								}
							
								selector.setOrigLang(Language.EN);
								annotation.setAnnotators(new ArrayList(Arrays.asList(administrative)));
								if (isGeo){
									annotation.setMotivation(MotivationType.GeoTagging);
								}
								else{
									annotation.setMotivation(MotivationType.Tagging);
								}
								annotation.getTarget().setRecordId(record.getDbId());
								annotation.getTarget().setWithURI("/record/" + record.getDbId());
								annotation.getTarget().setSelector(selector);
								annotation.setPublish(false);
								if (isGeo){
									annotation.setBody(geoBody);
								}
								else{
									annotation.setBody(body);
								}
								/* If annotation already exists add a new annotator, else create the new annotation*/
								Annotation existingAnnotation = DB.getAnnotationDAO().getExistingAnnotation(annotation);
								if (existingAnnotation == null) {
									DB.getAnnotationDAO().makePermanent(annotation);
									annotation.setAnnotationWithURI("/annotation/" + annotation.getDbId());
									DB.getAnnotationDAO().makePermanent(annotation); // is this needed for a second time?
									DB.getRecordResourceDAO().addAnnotation(annotation.getTarget().getRecordId(), annotation.getDbId(), geekUser.toString());
								} else {
									ArrayList<AnnotationAdmin> annotators = existingAnnotation.getAnnotators();
									for (AnnotationAdmin a : annotators) {
										if (a.getWithCreator().equals(geekUser)) {
											return "";
										}
									}
									DB.getAnnotationDAO().addAnnotators(existingAnnotation.getDbId(), annotation.getAnnotators());
									annotation = DB.getAnnotationDAO().get(existingAnnotation.getDbId());
								}
							}
						}
						
						return "";
					}
					else{
						return "Error";
					}	
				} catch (Exception e) {
					e.printStackTrace();
					return "Error";
				}
			};


			String[] myRequest;

			List<ProvenanceInfo> provenanceList = record.getProvenance();
			List<String> providers = new ArrayList<String>();
			// /***** PROVIDERS *****/
			// if (provenanceList != null){
			// 	int counter = 0;
			// 	for (ProvenanceInfo prov : provenanceList){
			// 		if (prov.getProvider() != null){
			// 			myRequest = new String[2];
			// 			myRequest[0] = prov.getProvider();
			// 			myRequest[1] = "provider";
			// 			if (tool.equals("AIDA")){
			// 				AidaAPICall.apply(myRequest);
			// 				// ParallelAPICall.createPromise(AidaAPICall,myRequest);
			// 			}
			// 			else if(tool.equals("GEEK")){
			// 				GeekAPICall.apply(myRequest);
			// 				// ParallelAPICall.createPromise(GeekAPICall,myRequest);
			// 			}	
			// 		}
			// 	}
			// }
			CulturalObjectData dd = (CulturalObjectData)record.getDescriptiveData();
			String description,label,country,city,creator;
			List<String> altLabels;
			description = null;
			label = null;
			country = null;
			city = null;
			creator = null;
			List<String> multiliterals;
			Set<Language> languages;
			if (dd != null){
				/***** DESCRIPTION *****/
				if (dd.getDescription() != null){
					languages = dd.getDescription().getLanguages();
					if (languages.contains(Language.EN)){
						multiliterals = dd.getDescription().get(Language.EN);
						description = multiliterals.get(0);
					}
					else if(languages.contains(Language.DEFAULT)){
						multiliterals = dd.getDescription().get(Language.DEFAULT);
						description = multiliterals.get(0);
					}
					if (description != null){
						myRequest = new String[2];
						myRequest[0] = description;
						myRequest[1] = "description";
						if (tool.equals("AIDA")){
							AidaAPICall.apply(myRequest);
							// ParallelAPICall.createPromise(AidaAPICall,myRequest);
						}
						else if(tool.equals("GEEK")){
							GeekAPICall.apply(myRequest);
							// ParallelAPICall.createPromise(GeekAPICall,myRequest);
						}

					}
				}
			// 	/***** LABEL *****/
			// 	if (dd.getLabel() != null){
			// 		languages = dd.getLabel().getLanguages();
			// 		if (languages.contains(Language.EN)){
			// 			multiliterals = dd.getLabel().get(Language.EN);
			// 			label = multiliterals.toString();
			// 			label = label.substring(1, label.length() - 1); 
			// 		}
			// 		else if(languages.contains(Language.DEFAULT)){
			// 			multiliterals = dd.getLabel().get(Language.DEFAULT);
			// 			label = multiliterals.toString();
			// 			label = label.substring(1, label.length() - 1);
			// 		}
			// 		if (label != null){
			// 			myRequest = new String[2];
			// 			myRequest[0] = label;
			// 			myRequest[1] = "label";
			// 			if (tool.equals("AIDA")){
			// 				AidaAPICall.apply(myRequest);
			// 				// ParallelAPICall.createPromise(AidaAPICall,myRequest);
			// 			}
			// 			else if(tool.equals("GEEK")){
			// 				GeekAPICall.apply(myRequest);
			// 				// ParallelAPICall.createPromise(GeekAPICall,myRequest);
			// 			}
			// 		}
			// 	}				
			// 	/***** COUNTRY *****/
			// 	if (dd.getCountry() != null){
			// 		languages = dd.getCountry().getLanguages();
			// 		if (languages.contains(Language.EN)){
			// 			multiliterals = dd.getCountry().get(Language.EN);
			// 			country = multiliterals.toString();
			// 			country = country.substring(1, country.length() - 1); 
			// 		}
			// 		else if(languages.contains(Language.DEFAULT)){
			// 			multiliterals = dd.getCountry().get(Language.DEFAULT);
			// 			country = multiliterals.toString();
			// 			country = country.substring(1, country.length() - 1);
			// 		}
			// 		if (country != null){
			// 			myRequest = new String[2];
			// 			myRequest[0] = country;
			// 			myRequest[1] = "country";
			// 			if (tool.equals("AIDA")){
			// 				AidaAPICall.apply(myRequest);
			// 				// ParallelAPICall.createPromise(AidaAPICall,myRequest);
			// 			}
			// 			else if(tool.equals("GEEK")){
			// 				GeekAPICall.apply(myRequest);
			// 				// ParallelAPICall.createPromise(GeekAPICall,myRequest);
			// 			}
			// 		}
			// 	}	
			// 	/***** CITY *****/
			// 	if (dd.getCity() != null){
			// 		languages = dd.getCity().getLanguages();
			// 		if (languages.contains(Language.EN)){
			// 			multiliterals = dd.getCity().get(Language.EN);
			// 			city = multiliterals.toString();
			// 			city = city.substring(1, city.length() - 1); 
			// 		}
			// 		else if(languages.contains(Language.DEFAULT)){
			// 			multiliterals = dd.getCity().get(Language.DEFAULT);
			// 			city = multiliterals.toString();
			// 			city = city.substring(1, city.length() - 1);
			// 		}
			// 		if (city != null){
			// 			myRequest = new String[2];
			// 			myRequest[0] = city;
			// 			myRequest[1] = "city";
			// 			if (tool.equals("AIDA")){
			// 				AidaAPICall.apply(myRequest);
			// 				// ParallelAPICall.createPromise(AidaAPICall,myRequest);
			// 			}
			// 			else if(tool.equals("GEEK")){
			// 				GeekAPICall.apply(myRequest);
			// 				// ParallelAPICall.createPromise(GeekAPICall,myRequest);
			// 			}
			// 		}
			// 	}
				// /***** CREATOR *****/
				// if (dd.getDccreator() != null){
				// 	languages = dd.getDccreator().getLanguages();
				// 	if (languages.contains(Language.EN)){
				// 		multiliterals = dd.getDccreator().get(Language.EN);
				// 		creator = multiliterals.toString();
				// 		creator = creator.substring(1, creator.length() - 1);
				// 	}
				// 	else if(languages.contains(Language.DEFAULT)){
				// 		multiliterals = dd.getDccreator().get(Language.DEFAULT);
				// 		creator = multiliterals.toString();
				// 		creator = creator.substring(1, creator.length() - 1);						
				// 	}
				// 	if (creator != null){
				// 		myRequest = new String[2];
				// 		myRequest[0] = creator;
				// 		myRequest[1] = "creator";
				// 		if (tool.equals("AIDA")){
				// 			AidaAPICall.apply(myRequest);
				// 			// ParallelAPICall.createPromise(AidaAPICall,myRequest);
				// 		}
				// 		else if(tool.equals("GEEK")){
				// 			GeekAPICall.apply(myRequest);
				// 			// ParallelAPICall.createPromise(GeekAPICall,myRequest);
				// 		}
				// 	}
				// }
				
			}
			return ok();
		}
		else{
			return notFound();
		}
		
	}

	public static Result wikidataLabelMatch(String recordId) {
		ObjectNode request = Json.newObject();
		CulturalObject record = (CulturalObject)DB.getRecordResourceDAO().get(new ObjectId(recordId));
		if (record != null){
			User user = DB.getUserDAO().getUniqueByFieldAndValue("username", "WikiData");
			ObjectId wikiUser = user.getDbId();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			String createdString = sdf.format(new Date());
			/***** WikiData API Call *****/
			Function<String[],String> WikiAPICall = (String[] input) -> {
				try {
					String text = input[0];
					String initialInput = text;
					String property = input[1];
					// Delete the parentheses after the name if any 
					int parenthesisIndex = text.indexOf('(');
					if (parenthesisIndex > 0){
						text = text.substring(0,parenthesisIndex-1);
					}
					text = text.trim();
					int limit = text.length();
					HttpClient client = HttpClientBuilder.create().build();
					/* We search for people that their occupation is either artist, or designer, or fashion deisgner, photographers or creator.	*/
					String occupation = "{?item wdt:P106 wd:Q3501317.}	UNION {?item wdt:P106 wd:Q483501.} UNION {?item wdt:P106 wd:Q5322166.} UNION {?item wdt:P106 wd:Q2500638.} UNION {?item wdt:P106 wd:Q33231.}";
					/* We are also interested in fashion houses.*/
					String instanceOf = "{?item wdt:P31 wd:Q3661311.}";
					/* We are only interested in people or Businesses, not general terms etc. (When we search without the occupation and instanceOF filters) */
					String personOrBusiness = "{?item wdt:P31 wd:Q5.} UNION  {?item wdt:P31 wd:Q4830453.}.";
					String query = "SELECT ?item ?itemLabel WHERE { ?item rdfs:label ?itemLabel. ?item ?label \"" + text +"\"@en." + occupation + "UNION" + instanceOf + " FILTER (langMatches( lang(?itemLabel), \"EN\")) }";
					String WikiQueryService = "https://query.wikidata.org/bigdata/namespace/wdq/sparql?format=json&query=" + URLEncoder.encode(query, "UTF-8");
					HttpGet req = new HttpGet(WikiQueryService);
					req.setHeader("content-type", "application/json");
					/* if we find a person that matches the label and the occupation/instanceOf we set a high confidence. */
					Double confidence = 0.9;
					HttpResponse response = client.execute(req);

					/* If there is no person with the required occupations, we try without occupation limitation. */
					if (response.getStatusLine().getStatusCode() == 200) {
						//Check the Wikidata response, sometimes we get "java.io.IOException: Stream closed" when we try to get the json in the next line. 
						String json = EntityUtils.toString(response.getEntity(), "UTF-8");
						JsonNode responseJson = new ObjectMapper().readTree(json);
						if (responseJson.has("results")){
							if (responseJson.get("results").has("bindings")){
								Boolean flag = true;
								for (JsonNode entity : responseJson.get("results").get("bindings")){
									flag = false;
								}
								String[] words = text.split(" ");
								if (words.length < 2){
									flag = false;
								}
								if (flag){
									/* if we only match the label and not the occupation we set a low confidence. */
									confidence = 0.1;
									query = "SELECT ?item ?itemLabel WHERE { ?item rdfs:label ?itemLabel. ?item ?label \"" + text +"\"@en." + personOrBusiness + " FILTER (langMatches( lang(?itemLabel), \"EN\")) }";
									WikiQueryService = "https://query.wikidata.org/bigdata/namespace/wdq/sparql?format=json&query=" + URLEncoder.encode(query, "UTF-8");
									req = new HttpGet(WikiQueryService);
									req.setHeader("content-type", "application/json");
									response = client.execute(req);
								}
							}
						}
					}

					if (response.getStatusLine().getStatusCode() == 200) {
						String json = EntityUtils.toString(response.getEntity(), "UTF-8");
						JsonNode responseJson = new ObjectMapper().readTree(json);
						if (responseJson.has("results")){
							if (responseJson.get("results").has("bindings")){
								for (JsonNode entity : responseJson.get("results").get("bindings")){
									if (entity.has("item")){
										JsonNode item = entity.get("item");
										String uri = "";
										if (item.has("type") && item.has("value")){
											if (!item.get("type").asText().equals("uri")){
												continue;
											}
											else{
												uri = item.get("value").asText();
												if (!uri.contains("wikidata.org/entity")){
													uri = "";
												}
											}
										}
										if (uri.equals("")){
											continue;
										}
										Annotation annotation = new Annotation();
										AnnotationAdmin administrative = new AnnotationAdmin();
										administrative.setWithCreator(wikiUser);	
										Date createdDate = new Date();
										try {
											createdDate = sdf.parse(createdString);
											administrative.setCreated(createdDate);
										}
										catch(Exception e){}
										String nowString = sdf.format(new Date());
										Date nowDate = new Date();
										try {
											nowDate = sdf.parse(nowString);
											administrative.setGenerated(nowDate);
											administrative.setLastModified(nowDate);								
										}
										catch(Exception e){}
										administrative.setGenerator("WikidataLabelMatch");
										administrative.setConfidence(confidence);
										PropertyTextFragmentSelector selector = new PropertyTextFragmentSelector();
										selector.setOrigValue(initialInput);
										selector.setProperty(property);
										selector.setStart(0);
										selector.setEnd(limit);
										AnnotationBodyTagging body = new AnnotationBodyTagging();
										String myLabel = text;
										if (entity.has("itemLabel")){
											JsonNode itemLabel = entity.get("itemLabel");
											if (itemLabel.has("value")){
												myLabel = itemLabel.get("value").asText();
											}
										}
										MultiLiteral labels = new MultiLiteral(Language.EN, myLabel);
										labels.addLiteral(Language.DEFAULT, myLabel);
										body.setLabel(labels);

										body.setUri(uri);

										body.setUriVocabulary("Wikidata");
										selector.setOrigLang(Language.EN);
										annotation.setAnnotators(new ArrayList(Arrays.asList(administrative)));
										annotation.setMotivation(MotivationType.Tagging);
										annotation.getTarget().setRecordId(record.getDbId());
										annotation.getTarget().setWithURI("/record/" + record.getDbId());
										annotation.getTarget().setSelector(selector);
										annotation.setPublish(false);
										annotation.setBody(body);
										/* If annotation already exists add a new annotator, else create the new annotation*/
										Annotation existingAnnotation = DB.getAnnotationDAO().getExistingAnnotation(annotation);
										if (existingAnnotation == null) {
											DB.getAnnotationDAO().makePermanent(annotation);
											annotation.setAnnotationWithURI("/annotation/" + annotation.getDbId());
											DB.getAnnotationDAO().makePermanent(annotation); // is this needed for a second time?
											DB.getRecordResourceDAO().addAnnotation(annotation.getTarget().getRecordId(), annotation.getDbId(), wikiUser.toString());
										} else {
											ArrayList<AnnotationAdmin> annotators = existingAnnotation.getAnnotators();
											for (AnnotationAdmin a : annotators) {
												if (a.getWithCreator().equals(wikiUser)) {
													return "";
												}
											}
											DB.getAnnotationDAO().addAnnotators(existingAnnotation.getDbId(), annotation.getAnnotators());
											annotation = DB.getAnnotationDAO().get(existingAnnotation.getDbId());
										}
									}
								}
							}
						}
						return "";
					}
					else{
						return "Error";
					}	
				} catch (Exception e) {
					e.printStackTrace();
					return "Error";
				}
			};


			String[] myRequest;
			CulturalObjectData dd = (CulturalObjectData)record.getDescriptiveData();
			String creator;
			creator = null;
			List<String> multiliterals;
			Set<Language> languages;
			if (dd != null){
				/***** CREATOR *****/
				if (dd.getDccreator() != null){
					languages = dd.getDccreator().getLanguages();
					if (languages.contains(Language.EN)){
						multiliterals = dd.getDccreator().get(Language.EN);
						creator = multiliterals.toString();
						creator = creator.substring(1, creator.length() - 1);
					}
					else if(languages.contains(Language.DEFAULT)){
						multiliterals = dd.getDccreator().get(Language.DEFAULT);
						creator = multiliterals.toString();
						creator = creator.substring(1, creator.length() - 1);						
					}
					if (creator != null){
						myRequest = new String[2];
						myRequest[0] = creator;
						myRequest[1] = "creator";
						WikiAPICall.apply(myRequest);
						// ParallelAPICall.createPromise(WikiAPICall,myRequest);
					}
				}
				
			}
			return ok();
		}
		else{
			return notFound();
		}
		
	}

	public static Result artStyleRecord(String recordId, String myType, String model) {
		ObjectNode request = Json.newObject();
		RecordResource record = DB.getRecordResourceDAO().get(new ObjectId(recordId));
		if (record != null){
			if ((record.getMedia() != null) && (record.getMedia().size() > 0)){
				HashMap<MediaVersion, EmbeddedMediaObject> media = (HashMap<MediaVersion, EmbeddedMediaObject>)(record.getMedia()).get(0);
				EmbeddedMediaObject original = media.get(MediaVersion.Original);
				if (original == null){
					original = media.get(MediaVersion.Thumbnail);
				}
				if ((original != null) && (original.getUrl() != null)){
					String url = original.getUrl();
					if (url.startsWith("/media/")){
						url = "https://api.withculture.eu" + url;
					}
					request.put("url", url);
					request.put("type", myType);
					request.put("model", model);
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
					String createdString = sdf.format(new Date());
					Function<ObjectNode,String> ArtStyleAPI = (ObjectNode MyJson) -> {
						try {
							HttpClient client = HttpClientBuilder.create().build();
							HttpPost req = new HttpPost("http://pinkfloyd.deep.islab.ntua.gr:5000");
							req.setHeader("content-type", "application/json");
							req.setEntity(new StringEntity(MyJson.toString())); //setting the body for POST request
							HttpResponse response = client.execute(req);
							if (response.getStatusLine().getStatusCode() == 200) {
								String json = EntityUtils.toString(response.getEntity()); 
								if (!myType.equals("fuzzy")){
									JsonNode resp = Json.parse(json); 
									String ArtStyle = resp.get("ArtStyle").toString();
									ArtStyle = ArtStyle.substring(1, ArtStyle.length() - 1);
									String confidence = "0.0";
									if (resp.has("confidence")){
										confidence = resp.get("confidence").toString();
										confidence = confidence.substring(1, confidence.length() - 1);
									}
									ThesaurusObject thesaurusStyle = ThesaurusController.getThesaurusArtStyle(ArtStyle);
									if (thesaurusStyle != null){
										String uri = thesaurusStyle.getSemantic().getUri();
										User user = DB.getUserDAO().getUniqueByFieldAndValue("username", "ASARS");
										ObjectId ASARSUser = user.getDbId();
										Annotation annotation = new Annotation();
										AnnotationAdmin administrative = new AnnotationAdmin();
										administrative.setWithCreator(ASARSUser);	
										Date createdDate = new Date();
										try {
											createdDate = sdf.parse(createdString);
											administrative.setCreated(createdDate);
										}
										catch(Exception e){}
										String nowString = sdf.format(new Date());
										Date nowDate = new Date();
										try {
											nowDate = sdf.parse(nowString);
											administrative.setGenerated(nowDate);
											administrative.setLastModified(nowDate);								
										}
										catch(Exception e){}
										administrative.setGenerator("ASARS");
										try{
											administrative.setConfidence(Double.parseDouble(confidence));
										}
										catch(Exception e){
											administrative.setConfidence(0.0);
										}
										AnnotationBodyTagging body = new AnnotationBodyTagging();
										body.setUri(uri);
										Literal labels = thesaurusStyle.getSemantic().getPrefLabel();
										MultiLiteral labels2 = new MultiLiteral();
										for (Map.Entry<String, String> label : labels.entrySet()){
											labels2.add(label.getKey(), label.getValue());
										}
										body.setLabel(labels2);
										body.setUriVocabulary(thesaurusStyle.getSemantic().getVocabulary().getName());
										annotation.setAnnotators(new ArrayList(Arrays.asList(administrative)));
										annotation.setMotivation(MotivationType.Tagging);
										annotation.getTarget().setRecordId(record.getDbId());
										annotation.getTarget().setWithURI("/record/" + record.getDbId());
										annotation.setPublish(false);
										annotation.setBody(body);
										Annotation existingAnnotation = DB.getAnnotationDAO().getExistingAnnotation(annotation);
										if (existingAnnotation == null) {
											DB.getAnnotationDAO().makePermanent(annotation);
											annotation.setAnnotationWithURI("/annotation/" + annotation.getDbId());
											DB.getAnnotationDAO().makePermanent(annotation); // is this needed for a second time?
											DB.getRecordResourceDAO().addAnnotation(annotation.getTarget().getRecordId(), annotation.getDbId(), ASARSUser.toString());
										} else {
											ArrayList<AnnotationAdmin> annotators = existingAnnotation.getAnnotators();
											for (AnnotationAdmin a : annotators) {
												if (a.getWithCreator().equals(ASARSUser)) {
													return "";
												}
											}
											DB.getAnnotationDAO().addAnnotators(existingAnnotation.getDbId(), annotation.getAnnotators());
											annotation = DB.getAnnotationDAO().get(existingAnnotation.getDbId());
										}
									}
								}
								return json;
							}
							else{
								return "Error";
							}
						} catch (Exception e) {
							e.printStackTrace();
							return "Error";
						}
					};
					ParallelAPICall.createPromise(ArtStyleAPI,request);
					return ok();
				}
				else{
					return notFound();
				}
			}
			else{
				return notFound();
			}
		}
		else{
			return notFound();
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

	enum Order {
		UPVOTED, // upvoted first
		DOWNVOTED, // downvoted first
		NEUTRAL // neutral first
	}

	private static int getVoteDifference(Annotation a, Order order) {
		int upvoted = 1;
		int downvoted = 0;
		if (a.getScore() != null) {
			if (a.getScore().getApprovedBy() != null)
				upvoted = a.getScore().getApprovedBy().size();
			if (a.getScore().getRejectedBy() != null)
				downvoted = a.getScore().getRejectedBy().size();
		}
		switch (order) {
		case UPVOTED:
			return downvoted - upvoted;
		case DOWNVOTED:
			return upvoted - downvoted;
		case NEUTRAL:
			return Math.abs(upvoted - downvoted);
		}
		return downvoted - upvoted;
	}

	public static Result getRecordIdsByAnnLabel(String label, List<String> generators, String order) {
		ArrayNode result = Json.newObject().arrayNode();
		List<Annotation> anns = DB.getAnnotationDAO().getByLabel(generators, label);
		List<Annotation> sortedAnns = anns.stream()
				.sorted((a1, a2) -> Integer.compare(getVoteDifference(a1, Order.valueOf(order.toUpperCase())),
						getVoteDifference(a2, Order.valueOf(order.toUpperCase()))))
				.collect(Collectors.toList());
		for (Annotation ann : sortedAnns) {
			// Do NOT return the actual records, TOO SLOW
			// Instead, return an array with the record ids
			result.add(Json.toJson(ann.getTarget().getRecordId().toHexString()));
		}
		return ok(result);
	}
}
