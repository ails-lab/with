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

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import model.DescriptiveData;
import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.WithMediaType;
import model.MediaObject;
import model.annotations.ContextData;
import model.annotations.ContextData.ContextDataBody;
import model.annotations.ContextData.ContextDataTarget;
import model.annotations.ContextData.ContextDataType;
import model.basicDataTypes.Language;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.ProvenanceInfo.Sources;
import model.resources.CulturalObject.CulturalObjectData;
import model.resources.RecordResource;
import model.resources.ThesaurusObject;
import model.resources.WithResource.WithResourceType;
import model.resources.collection.CollectionObject;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Option;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import sources.core.ISpaceSource;
import sources.core.ParallelAPICall;
import sources.core.ParallelAPICall.Priority;
import sources.core.RecordJSONMetadata;
import utils.AccessManager;
import utils.AccessManager.Action;
import utils.Locks;

import com.aliasi.dict.MapDictionary;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;
import db.WithResourceDAO;
import elastic.Elastic;
import elastic.ElasticSearcher;
import elastic.ElasticSearcher.SearchOptions;

/**
 * @author mariaral
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ThesaurusController extends Controller {

	public static final ALogger log = Logger.of(ThesaurusController.class);

	public static Status errorIfNoAccessToWithResource(
			WithResourceDAO resourceDAO, Action action, ObjectId id) {
		ObjectNode result = Json.newObject();
		List<String> effectiveUserIds = AccessManager
				.effectiveUserIds(session().get("effectiveUserIds"));
		if (!resourceDAO.existsEntity(id)) {
			log.error("Cannot retrieve resource from database");
			result.put("error", "Cannot retrieve resource " + id
					+ " from database");
			return internalServerError(result);
			// TODO superuser
		} else if (!resourceDAO.hasAccess(
				AccessManager.effectiveUserDbIds(session().get(
						"effectiveUserIds")), action, id)
				&& !AccessManager.isSuperUser(effectiveUserIds.get(0))) {
			result.put("error", "User does not have " + action
					+ " access for resource " + id);
			return forbidden(result);
		} else {
			return ok();
		}
	}

	public static Status errorIfNoAccessToCollection(Action action,
			ObjectId collectionDbId) {
		return errorIfNoAccessToWithResource(DB.getCollectionObjectDAO(),
				action, collectionDbId);
	}

	public static Status errorIfNoAccessToRecord(Action action,
			ObjectId recordId) {
		return errorIfNoAccessToWithResource(DB.getRecordResourceDAO(), action,
				recordId);
	}


	public static Result addThesaurusTerm() {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

//		Locks locks = null;
		try {
//			locks = Locks.create().write("Collection #" + colId).acquire();
//			Status response = errorIfNoAccessToCollection(Action.EDIT,
//					collectionDbId);
//			if (!response.toString().equals(ok().toString())) {
//				return response;
//			} else {
				if (json == null) {
					result.put("error", "Invalid JSON");
					return badRequest(result);
				}
				return addThesaurusTerm(json);
//			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
//		} finally {
//			if (locks != null)
//				locks.release();
		}
	}

//	public static Result internalAddRecordToCollection(String colId,
//			RecordResource record, Option<Integer> position, ObjectNode result) {
//		return addRecordToCollection(Json.toJson(record), new ObjectId(colId),
//				position, false);
//	}
//
//	public static Result internalAddRecordToCollection(String colId,
//			RecordResource record, Option<Integer> position, ObjectNode result,
//			boolean noRepeated) {
//		return addRecordToCollection(Json.toJson(record), new ObjectId(colId),
//				position, noRepeated);
//	}

	public static Result addThesaurusTerm(JsonNode json) {
		
		ObjectNode result = Json.newObject();

		try {
			Class<?> clazz = Class.forName("model.resources.ThesaurusObject");
			
			ThesaurusObject record = (ThesaurusObject) Json.fromJson(json, clazz);
			String uri = record.getSemantic().getUri();
			
			if (uri == null) {
				return badRequest("A thesaurus term should have a uri");
			}
			
			ObjectId recordId;
			
			if (DB.getThesaurusDAO().existsWithExternalId(uri)) {
				ThesaurusObject resource = DB.getThesaurusDAO()
						.getUniqueByFieldAndValue("administrative.externalId",
								uri,
								new ArrayList<String>(Arrays.asList("_id")));
				DB.getThesaurusDAO().editRecord("semantic", resource.getDbId(), json.get("semantic"));

			} else {

				record.getAdministrative().setCreated(new Date());
				
				DB.getThesaurusDAO().makePermanent(record);
				recordId = record.getDbId();
				DB.getThesaurusDAO().updateField(recordId, "administrative.externalId", uri);
			}
			
			result.put("message", "Thesaurus term succesfully added");
			return ok(result);
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	public static Result addThesaurusTerms() {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
//		ObjectId collectionDbId = new ObjectId(colId);
//		Locks locks = null;
		try {
//			locks = Locks.create().write("Collection #" + colId).acquire();
//			Status response = errorIfNoAccessToCollection(Action.EDIT,
//					collectionDbId);
//			if (!response.toString().equals(ok().toString())) {
//				return response;
//			} else {
				if (json == null || !json.isArray()) {
					result.put("error", "Invalid JSON");
					return badRequest(result);
				} else {
					Iterator<JsonNode> iterator = json.iterator();
					while (iterator.hasNext()) {
						JsonNode recordJson = iterator.next();
						Result r = addThesaurusTerm(recordJson);
						if (!r.toString().equals(ok().toString())) {
							return r;
						}
					}
					result.put("message",
							"Thesaurus terms have been successfully added to db.");
					return ok(result);
//				}
			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		} finally {
//			if (locks != null)
//				locks.release();
		}
	}
	
	public static Result deleteThesaurus(String thesaurus) {
		ObjectNode result = Json.newObject();
//		Locks locks = null;
		try {
//			locks = Locks.create().write("Collection #" + id).acquire();
//			ObjectId collectionDbId = new ObjectId(id);
//			Result response = errorIfNoAccessToCollection(Action.DELETE,
//					collectionDbId);
//			if (!response.toString().equals(ok().toString()))
//				return response;
//			else {
				DB.getThesaurusDAO().removeAllTermsFromThesaurus(thesaurus);

				result.put("message", "Thesaurus was deleted successfully");
				return ok(result);
//			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		} finally {
//			if (locks != null)
//				locks.release();
		}
	}

}
