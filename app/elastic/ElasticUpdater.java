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


package elastic;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import model.Collection;
import model.CollectionRecord;
import model.User.Access;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.Logger;
import play.libs.Json;
import db.DB;

public class ElasticUpdater {
	static private final Logger.ALogger log = Logger.of(ElasticUpdater.class);

	private Collection collection;
	private CollectionRecord record;
	private String json;


	public ElasticUpdater( Collection c) {
		this.collection = c;
	}

	public ElasticUpdater( CollectionRecord r ) {
		this.record = r;
	}

	public ElasticUpdater( String json ) {
		this.json = json;
	}

	private JsonNode toJson(String jsonString) {
		return Json.parse(jsonString);
	}

	/*
	 * Updates record metadata. NOT collection specific stuff
	 * Re-indexes the record type, deletes entry from collection_specific field
	 * of merged_record type and indexes a new merged_record type
	 */
	public UpdateResponse updateRecordMetadata() {
		try {
			 Elastic.getTransportClient().prepareUpdate(
					Elastic.index,
					Elastic.type_within,
					record.getDbId().toString())
				.setSource(prepareRecordDoc())
				.get();
			 updateMergedDoc();
		} catch (Exception e) {
			log.error("Cannot update collection metadata!", e);
			return null;
		}
		return null;
	}

	/*
	 * Prepares document for record type
	 */
	private XContentBuilder prepareRecordDoc() {
		Iterator<Entry<String, JsonNode>> recordIt = Json.toJson(record).fields();
		XContentBuilder doc = null;
		try {
			doc = jsonBuilder().startObject();
			while( recordIt.hasNext() ) {
				Entry<String, JsonNode> entry = recordIt.next();
				if( !entry.getKey().equals("content") &&
					!entry.getKey().equals("dbId")) {
						doc.field(entry.getKey()+"_all", entry.getValue().asText());
						doc.field(entry.getKey(), entry.getValue().asText());
				}
			}
			
			doc.endObject();
		} catch (IOException e) {
			log.error("Cannot create json document for indexing", e);
			return null;
		}
	
		return doc;
	}

	/*
	 * Completes the whole update process of a merged document
	 */
	private void updateMergedDoc() {
		SearchRequestBuilder resp = Elastic.getTransportClient().
				prepareSearch(Elastic.index)
				.setTypes(Elastic.type_within);
	}

	
	/*
	 * Update collection metadata method. Does NOT updates rights
	 */
	public UpdateResponse updateCollectionMetadata() {
		try {
			return Elastic.getTransportClient().prepareUpdate(
					Elastic.index,
					Elastic.type_collection,
					collection.getDbId().toString())
					.setSource(prepareEditedCollectionDoc())
					.get();
		} catch (Exception e) {
			log.error("Cannot update collection metadata!", e);
			return null;
		}
	}
	
	private XContentBuilder prepareEditedCollectionDoc() {
		Iterator<Entry<String, JsonNode>> collectionIt = Json.toJson(collection).fields();
		XContentBuilder doc = null;
		try {
			doc = jsonBuilder().startObject();
			
			while( collectionIt.hasNext() ) {
				Entry<String, JsonNode> entry = collectionIt.next();
				/*if( entry.getKey().equals("rights") ) {
					ArrayNode array = Json.newObject().arrayNode();
					for(Entry<ObjectId, Access> e: collection.getRights().entrySet()) {
						ObjectNode right = Json.newObject();
						right.put("user", e.getKey().toString());
						right.put("access", e.getValue().toString());
						array.add(right);
					}
   					doc.rawField(entry.getKey(), array.toString().getBytes());
				} else */
				if( !entry.getKey().equals("firstEntries") &&
					!entry.getKey().equals("rights") &&	
					!entry.getKey().equals("dbId")) {
					
						doc.field(entry.getKey()+"_all", entry.getValue().asText());
						doc.field(entry.getKey(), entry.getValue().asText());
				}
			}
		} catch(IOException io) {
			log.error("Cannot create document to update!", io);
			return null;
		}
		return doc;
	}
	
	
	/*
	 * Update rights on a collection
	 */
	public UpdateResponse updateCollectionRights() {
		try {
			return Elastic.getTransportClient().prepareUpdate(
						Elastic.index,
						Elastic.type_collection,
						collection.getDbId().toString())
					.setSource(prepareUpdateOnRights())
					.execute().actionGet();		} catch (Exception e) {
			log.error("Cannot update collection rights!", e);
			return null;
		}
	}
	
	private XContentBuilder prepareUpdateOnRights() {
		XContentBuilder doc = null;
		try {
			
			doc = jsonBuilder().startObject();
			ArrayNode array = Json.newObject().arrayNode();
			for(Entry<ObjectId, Access> e: collection.getRights().entrySet()) {
				ObjectNode right = Json.newObject();
				right.put("user", e.getKey().toString());
				right.put("access", e.getValue().toString());
				array.add(right);
			}
			doc.rawField("rights", array.toString().getBytes());
			doc.endObject();	
						
		} catch(IOException io) {
			log.error("Cannot create document to update!", io);
			return null;
		}
		
		try {
			System.out.println(doc.string());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return doc;
	}
	

	/*
	 * Bulk updates. Probably not going to be used
	 */
	private void updateCollection() throws Exception {
		List<CollectionRecord> records = DB.getCollectionRecordDAO()
				.getByCollection(collection.getDbId());
		List<XContentBuilder> documents = new ArrayList<XContentBuilder>();
		for(CollectionRecord r: records) {
			this.record = r;
			documents.add(null);
		}
		if( documents.size() == 0 ) {
			log.debug("No records within the collection to update!");
		} else if( documents.size() == 1 ) {
					Elastic.getTransportClient().prepareUpdate(
							Elastic.index,
							Elastic.type_general,
							record.getDbId().toString())
							.setSource(documents.get(0))
							.get();
		} else {

				int i = 0;
				for(XContentBuilder doc: documents) {
					Elastic.getBulkProcessor().add(new UpdateRequest(
							Elastic.index,
							Elastic.type_general,
							records.get(i).getDbId().toString())
					.source(doc));
					i++;
				}
				Elastic.getBulkProcessor().close();
		}

	}
}
