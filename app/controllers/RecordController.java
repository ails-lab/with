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

import java.util.List;

import model.CollectionRecord;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class RecordController extends Controller {
	public static final ALogger log = Logger.of( RecordController.class);

	/**
	 * Retrive a colleciton entry or just a specific format
	 * @param entryId
	 * @param format
	 * @return
	 */
	public static Result getRecord(String entryId, String format) {
		ObjectNode result = Json.newObject();

		CollectionRecord record =
				DB.getCollectionRecordDAO().get(new ObjectId(entryId));

		if(record == null) {
			log.error("Cannot retrieve user modifications from database");
			result.put("message", "Cannot retrieve user modifications from database");
			return internalServerError(result);
		}

		if(!format.equals("") && record.getContent().containsKey(format)) {
			return ok(Json.toJson(record.getContent().get(format)));
		} else {
			return ok(Json.toJson(record));
		}


	}

	/**
	 * Update a record. Needs to be implemented in a better way
	 * @param colEntryId
	 * @param format
	 * @return
	 */
	public static Result updateRecord(String recordId, String format) {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

		CollectionRecord record =
				DB.getCollectionRecordDAO().get(new ObjectId(recordId));

		if(record == null) {
			log.error("Cannot retrieve user modifications from database");
			result.put("message", "Cannot retrieve user modifications from database");
			return internalServerError(result);
		}

		if(format.equals("")) {

		} else {

		}

		return ok();
	}

	/**
	 * Deletes a whole collection entry or just a format
	 * @param entryId
	 * @param format
	 * @return
	 */
	public static Result deleteRecord(String recordId, String format) {
		ObjectNode result = Json.newObject();

		if(format.equals("")) {
			 if( DB.getCollectionRecordDAO().deleteById(new ObjectId(recordId)).getN() != 1 ) {
				 log.error("Cannot delete Collection Entry from database!");
				 result.put("message", "Cannot delete Collection Entry from database!");
				 return internalServerError(result);
			 } else {
				 result.put("message", "Collection entry deleted successfully!");
				 return ok(result);
			 }
		} else {
			CollectionRecord record = DB.getCollectionRecordDAO().get(new ObjectId(recordId));
			if(record.getContent().containsKey(format))
				record.getContent().remove(format);
			if( DB.getCollectionRecordDAO().makePermanent(record) != null) {
				return ok(Json.toJson(record));
			} else {
				log.error("Cannot delete specific content from Collection entry!");
				result.put("message", "Cannot delete specific content from Collection entry!");
				return internalServerError(result);
			}
		}
	}

	/**
	 *
	 * @param source
	 * @param sourceId
	 * @param annotated
	 * @return
	 */
	public static Result findInCollections(String source, String sourceId, boolean annotated) {
		ObjectNode result = Json.newObject();

		List<CollectionRecord> records =
				DB.getCollectionRecordDAO().getBySource(source, sourceId);

		if(records != null)
			return ok(Json.toJson(records));

		result.put("message", "Cannot retrieve entries from db");
		return internalServerError(result);
	}

}
