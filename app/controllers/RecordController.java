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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;

import model.Collection;
import model.CollectionRecord;
import model.Rights.Access;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import play.Logger;
import play.Logger.ALogger;
import play.data.validation.Validation;
import play.libs.F.Option;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AccessManager;
import utils.Tuple;
import utils.AccessManager.Action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;
import elastic.Elastic;
import elastic.ElasticEraser;
import elastic.ElasticSearcher;
import elastic.ElasticSearcher.SearchOptions;
import elastic.ElasticUpdater;
import elastic.ElasticUtils;

public class RecordController extends Controller {
	public static final ALogger log = Logger.of(RecordController.class);

	/**
	 * Retrieve a collection entry or just a specific format
	 *
	 * @param entryId
	 * @param format
	 * @return
	 */
	public static Result getRecord(String id, Option<String> format) {
		ObjectNode result = Json.newObject();

		CollectionRecord record = DB.getCollectionRecordDAO().get(
				new ObjectId(id));

		if (record == null) {
			log.error("Cannot retrieve record from database");
			result.put("message",
					"Cannot retrieve record from database");
			return internalServerError(result);
		}
		else {
			if (format.isDefined() && record.getContent().containsKey(format)) {
				return ok(record.getContent().get(format));
			}
			else {
				return ok(Json.toJson(record));
			}
		}
	}

	/**
	 * Update a record. Needs to be implemented in a better way
	 *
	 * @param colEntryId
	 * @param format
	 * @return
	 */
	public static Result updateRecord(String recordId, String format) {

		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		List<String> userIds = AccessManager.effectiveUserIds(session().get(
				"effectiveUserIds"));

		if (json == null) {
			result.put("message", "Invalid json!");
			return badRequest(result);
		}

		CollectionRecord oldRecord = DB.getCollectionRecordDAO().get(
				new ObjectId(recordId));
		if (oldRecord == null) {
			log.error("Cannot retrieve user modifications from database");
			result.put("message",
					"Cannot retrieve user modifications from database");
			return internalServerError(result);
		}

		Collection collection = DB.getCollectionDAO().get(
				oldRecord.getCollectionId());
		if (!AccessManager.checkAccess(collection.getRights(), userIds,
				Action.EDIT)) {
			result.put("error",
					"User does not have permission to edit the collection");
			return forbidden(result);
		}
		ObjectMapper objectMapper = new ObjectMapper();
		ObjectReader updator = objectMapper.readerForUpdating(oldRecord);
		CollectionRecord newRecord;
		collection.getFirstEntries().remove(oldRecord);

		if (format == null) {
			// update record tags
			try {
				newRecord = updator.readValue(json);
			} catch (IOException e) {
				return internalServerError(e.getMessage());
			}
			Set<ConstraintViolation<CollectionRecord>> violations = Validation
					.getValidator().validate(newRecord);
			for (ConstraintViolation<CollectionRecord> cv : violations) {
				result.put("message",
						"[" + cv.getPropertyPath() + "] " + cv.getMessage());
			}
			collection.getFirstEntries()
					.add(newRecord.getPosition(), newRecord);
			if ((DB.getCollectionRecordDAO().makePermanent(newRecord) != null)
					&& (DB.getCollectionDAO().makePermanent(collection) != null)) {

				// update the record in the index
				ElasticUpdater updater = new ElasticUpdater(oldRecord,
						newRecord);
				updater.updateRecordTags();
				result.put("message", "Record updated sucessfully!");
			} else {
				result.put("message", "Record not updated!");
			}
		} else {
			// update only the specific format
			// input json like { "XML-EDM": " [...xml...] " }
			oldRecord.getContent().put(format, json.get(format).asText());
			collection.getFirstEntries().add(oldRecord);
			if ((DB.getCollectionRecordDAO().makePermanent(oldRecord) != null)
					&& (DB.getCollectionDAO().makePermanent(collection) != null))
				result.put("message", "Record updated sucessfully!");
			else
				result.put("message", "Record not updated!");
		}

		return ok(result);
	}

	/**
	 * Deletes a whole collection entry or just a format
	 *
	 * @param entryId
	 * @param format
	 * @return
	 */
	public static Result deleteRecord(String recordId, String format) {
		ObjectNode result = Json.newObject();

		CollectionRecord record = DB.getCollectionRecordDAO().get(
				new ObjectId(recordId));
		Collection c = DB.getCollectionDAO().get(record.getCollectionId());

		if (format.equals("")) {
			// update firstEntries
			if (c.getFirstEntries().contains(record))
				c.getFirstEntries().remove(record);
			DB.getCollectionDAO().makePermanent(c);

			if (DB.getCollectionRecordDAO().makeTransient(record) != 1) {
				log.error("Cannot delete Collection Entry from database!");
				result.put("message",
						"Cannot delete Collection Entry from database!");
				return internalServerError(result);
			} else {
				result.put("message", "Collection entry deleted successfully!");
				return ok(result);
			}
		} else {
			// update firstEntries
			if (c.getFirstEntries().contains(record))
				c.getFirstEntries().remove(record);

			if (record.getContent().containsKey(format)) {
				record.getContent().remove(format);
				// update firstEntries
				c.getFirstEntries().add(record);
			}
			if ((DB.getCollectionRecordDAO().makePermanent(record) != null)
					&& (DB.getCollectionDAO().makePermanent(c) != null)) {

				// delete record from index
				ElasticEraser eraser = new ElasticEraser(record);
				eraser.deleteRecord();
				eraser.deleteRecordEntryFromMerged();

				return ok(Json.toJson(record));
			} else {
				log.error("Cannot delete specific content from Collection item!");
				result.put("message",
						"Cannot delete specific content from Collection item!");
				return internalServerError(result);
			}
		}
	}

	/**
	 * Get similar records based keywords on title
	 * and provider
	 */
	public static Result getSimilar(String externalId) {

		ArrayNode result = Json.newObject().arrayNode();
		CollectionRecord r = DB.getCollectionRecordDAO().getByExternalId(externalId).get(0);
		String title = r.getTitle();
		String provider = r.getProvenanceChain().get(0).providerName;

		/*
		 * Search for available collections
		 */
		ElasticSearcher searcher = new ElasticSearcher(Elastic.type_collection);
		SearchOptions elasticoptions = new SearchOptions(0, 1000);
		List<List<Tuple<ObjectId, Access>>> accessFilters = new ArrayList<List<Tuple<ObjectId,Access>>>();

		elasticoptions.accessList = accessFilters;
		SearchResponse response = searcher.searchAccessibleCollectionsScanScroll(elasticoptions);
		List<Collection> colFields = ElasticUtils.getCollectionMetadaFromHit(response.getHits());

		/*
		 * Search for similar records
		 */
		//elasticoptions = new SearchOptions(offset, count);
		elasticoptions = new SearchOptions();
		elasticoptions.addFilter("isPublic", "true");
		searcher.setType(Elastic.type_general);
		for(Collection collection : colFields) {
			elasticoptions.addFilter("collections", collection.getDbId().toString());
		}

		SearchResponse resp = searcher.searchForSimilar(title, provider, externalId, elasticoptions);
		searcher.closeClient();

		List<CollectionRecord> elasticrecords = new ArrayList<CollectionRecord>();
		for (SearchHit hit : resp.getHits().hits()) {
			elasticrecords.add(ElasticUtils.hitToRecord(hit));
			result.add(Json.toJson(ElasticUtils.hitToRecord(hit)));
		}


		return ok(result);
	}


	/**
	 *
	 * @param externalId
	 * @return
	 */
	public static Result getMergedRecord(String externalId) {

		ObjectNode result = Json.newObject();

		ElasticSearcher searchMerged = new ElasticSearcher(Elastic.type_general);
		SearchOptions options = new SearchOptions();
		options.set_idSearch(true);
		SearchResponse resp = searchMerged.search(externalId, options);

		List<String> userIds = AccessManager.effectiveUserIds(session().get(
				"effectiveUserIds"));


		if(resp.getHits().getTotalHits() == 0) {
			result.put("count",resp.getHits().getTotalHits());
			ArrayNode collections = Json.newObject().arrayNode();
			result.put("collections",collections);
			return ok(result);
	//		return internalServerError("message", "Invalid externalId given to Elastic Search");
		}

		SearchHit merged_hit = resp.getHits().getHits()[0];
		List<String> collectionIds =
				ElasticUtils.hitToRecord(merged_hit).getCollections();

		ElasticSearcher searchCollections = new ElasticSearcher(Elastic.type_collection);
		resp = searchCollections.searchForCollections(String.join(" ", collectionIds), new SearchOptions(0, 15));


		if(resp.getHits().getTotalHits() == 0) {
			result.put("count",resp.getHits().getTotalHits());
			ArrayNode collections = Json.newObject().arrayNode();
			result.put("collections",collections);
			return ok(result);

		//	return internalServerError("message", "No collections found for this merged record");
		}


		//result.put("count",resp.getHits().getTotalHits());
		ArrayNode collections = Json.newObject().arrayNode();
		int liked  = 0 ;
		int count = 0;
		for(SearchHit hit: resp.getHits().getHits()) {
			ObjectNode o = Json.newObject();
			Collection c = ElasticUtils.hitToCollection(hit);
			if (c.getTitle().equals("_favorites")) {
				liked++;
				continue;
			}
			count++;

			if(!c.getIsPublic() && !AccessManager.checkAccess(c.getRights(), userIds,
					Action.READ)){
				continue;
			}

			o.put("title", c.getTitle());
			o.put("description", c.getDescription());
			o.put("isExhibition", c.getIsExhibition());
			//o.put("isPublic", c.getIsPublic());
			o.put("thumbnail", c.getThumbnailUrl());
			o.put("userName" , DB.getUserDAO().get(c.getCreatorId()).getUsername());
			o.put("dbId", hit.getId());



			collections.add(o);
		}
		result.put("count",count);
		result.put("liked",liked);
		result.put("collections",collections);
		return ok(result);
	}

}
