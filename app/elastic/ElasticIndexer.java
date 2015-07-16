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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import model.Collection;
import model.CollectionRecord;
import model.User.Access;

import org.bson.types.ObjectId;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.script.ScriptService.ScriptType;

import play.Logger;
import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class ElasticIndexer {
	static private final Logger.ALogger log = Logger.of(ElasticIndexer.class);


	private Collection collection;
	private CollectionRecord record;

	public ElasticIndexer( Collection collection ) {
		this.collection = collection;
	}

	public ElasticIndexer( CollectionRecord record ) {
		this.record = record;
	}

	public ElasticIndexer( Collection c, CollectionRecord r ) {
		this.collection = c;
		this.record = r;
	}

	/*
	 * General method to index documents aacording to the object the class was initialized with
	 */
	public void index() {
		if( (collection != null) && (record != null) ) {
			indexCollectionMetadata();
			indexForGeneralSearch();
			indexForWithinSearch();
		} else if( collection != null )
			indexCollectionMetadata();
		else if( record != null ) {
			indexForGeneralSearch();
			indexForWithinSearch();
		}
		else {
			log.error("No records to index!");
		}
	}

	/*
	 * Index a collection document excluding firstEntries
	 */
	public IndexResponse indexCollectionMetadata() {
		if( collection == null )
			log.error("No collection specified!");

		Iterator<Entry<String, JsonNode>> colIt = Json.toJson(collection).fields();
		XContentBuilder doc = null;
		try {
			doc = jsonBuilder().startObject();

			while( colIt.hasNext() ) {
				Entry<String, JsonNode> entry = colIt.next();
				if( entry.getKey().equals("rights") ) {
					ArrayNode array = Json.newObject().arrayNode();
					for(Entry<ObjectId, Access> e: collection.getRights().entrySet()) {
						ObjectNode right = Json.newObject();
						right.put("user", e.getKey().toString());
						right.put("access", e.getValue().toString());
						array.add(right);
					}
   					doc.rawField(entry.getKey(), array.toString().getBytes());
				} else if( entry.getKey().equals("itemCount") ) {
					doc.field(entry.getKey(), entry.getValue().asInt());
				} else if( !entry.getKey().equals("firstEntries") &&
						  !entry.getKey().equals("rights") &&
						  !entry.getKey().equals("dbId")) {
					doc.field(entry.getKey()+"_all", entry.getValue().asText());
					doc.field(entry.getKey(), entry.getValue().asText());
				}
			}
		} catch(IOException e) {
				log.error("Cannot create collection json document for indexing", e);
		}

		IndexResponse response = Elastic.getTransportClient().prepareIndex(
					Elastic.index,
					Elastic.type_collection,
					collection.getDbId().toString())
				.setSource(doc)
				.execute()
				.actionGet();
		return response;
	}


	/*
	 * Index record documents as merged_record for general search
	 */
	public UpdateResponse indexForGeneralSearch() {
		IndexRequest indexReq = new IndexRequest(
				Elastic.index, Elastic.type_general)
			.source(prepareMergedDocument());
		if( record.getExternalId() == null )
			record.setExternalId(record.getDbId().toString());
		indexReq.id(record.getExternalId());
		UpdateResponse resp = null;
		try {
		resp = Elastic.getTransportClient().prepareUpdate()
				.setIndex(Elastic.index)
				.setType(Elastic.type_general)
				.setId(record.getExternalId())
			.addScriptParam("tags", record.getTags().toArray())
			.addScriptParam("collectionId", record.getCollectionId().toString())
			.addScriptParam("totalLikes", record.getTotalLikes())
			.setScript("for(String t: tags) {"
					//+ "if(!ctx._source.tags.contains(t))"
					+ "ctx._source.tags.add(t)"
					+ "}; "
					+ "ctx._source.collections += collectionId;"
					+ "ctx._source.totalLikes = totalLikes", ScriptType.INLINE)
			//.setScript("ctx._source.collections += collectionId", ScriptType.INLINE)
			.setUpsert(indexReq)
			.execute().actionGet();
		} catch (ElasticsearchException  e) {
			log.error("Cannot update document!", e);
		}

	return resp;
	}


	private XContentBuilder prepareMergedDocument() {
		Iterator<Entry<String, JsonNode>> recordIt = Json.toJson(record).fields();
		XContentBuilder doc = null;
		try {
			doc = jsonBuilder().startObject();

			while( recordIt.hasNext() ) {
				Entry<String, JsonNode> entry = recordIt.next();
				if( entry.getKey().equals("itemCount") ||
					entry.getKey().equals("totalLikes")) {
					doc.field(entry.getKey(), entry.getValue().asInt());
				} else if( !entry.getKey().equals("content") &&
					!entry.getKey().equals("tags")    &&
					!entry.getKey().equals("externalId")    &&
					!entry.getKey().equals("collectionId")) {
						doc.field(entry.getKey()+"_all", entry.getValue().asText());
						doc.field(entry.getKey(), entry.getValue().asText());
				}
			}

			//add collections array
			ArrayNode collections = Json.newObject().arrayNode();
			collections.add(record.getCollectionId().toString());
			doc.rawField("collections", collections.toString().getBytes());

			//add tags array
			ArrayNode tags = Json.newObject().arrayNode();
			for(String tag: record.getTags())
				tags.add(tag);
			doc.rawField("tags", tags.toString().getBytes());

			doc.endObject();
		} catch (IOException e) {
			log.error("Cannot create json document for indexing", e);
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
	 * Index record documents excluding content for search within a collection
	 */
	public IndexResponse indexForWithinSearch() {
		IndexResponse response = Elastic.getTransportClient().prepareIndex(
						Elastic.index,
						Elastic.type_within,
						record.getDbId().toString())
				.setSource(prepareRecordDocument())
				.execute()
				.actionGet();
		return response;
	}


	private XContentBuilder prepareRecordDocument() {
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

		try {
			System.out.println(doc.string());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return doc;
	}

	public void parseXmlIntoDoc( String xmlContent ) {

	}

	/*
	 * Bulk operations
	 */
	public void indexCollection() {
		List<CollectionRecord> records = DB.getCollectionRecordDAO()
											.getByCollection(collection.getDbId());
		List<XContentBuilder> documents  = new ArrayList<XContentBuilder>();
		List<XContentBuilder> mergedDocs = new ArrayList<XContentBuilder>();
		for(CollectionRecord r: records) {
			this.record = r;
			documents.add(prepareRecordDocument());
			mergedDocs.add(prepareMergedDocument());
		}
		if( documents.size() == 0 ) {
			log.debug("No records within the collection to index!");
		} else if( documents.size() == 1 ) {
				Elastic.getTransportClient().prepareIndex(
								Elastic.index,
								Elastic.type_within,
								record.getDbId().toString())
					 	.setSource(documents.get(0))
					 	.execute()
					 	.actionGet();
				Elastic.getTransportClient().prepareIndex(
								Elastic.index,
								Elastic.type_general,
								record.getExternalId())
					 	.setSource(mergedDocs.get(0))
					 	.execute()
					 	.actionGet();
		} else {
			try {
				int i = 0;
				for(XContentBuilder doc: documents) {
					Elastic.getBulkProcessor().add(new IndexRequest(
							Elastic.index,
							Elastic.type_within,
							records.get(i).getDbId().toString())
					.source(doc));
					i++;
				}
				Elastic.getBulkProcessor().flush();
				i = 0;
				for(XContentBuilder mdoc: mergedDocs) {
					Elastic.getBulkProcessor().add(new IndexRequest(
							Elastic.index,
							Elastic.type_general,
							records.get(i).getExternalId())
					.source(mdoc));
					i++;
				}
				Elastic.getBulkProcessor().close();
			} catch (Exception e) {
				log.error("Error in Bulk operations", e);
			}
		}
	}
}
