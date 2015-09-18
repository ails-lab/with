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
import model.Rights.Access;

import org.bson.types.ObjectId;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.script.ScriptService.ScriptType;

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
	private CollectionRecord oldRecord;
	private String json;


	public ElasticUpdater( Collection c) {
		this.collection = c;
	}

	public ElasticUpdater( CollectionRecord old, CollectionRecord r ) {
		this.oldRecord = old;
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
	/**
	 * Not fully functional
	 */
	public void updateRecordTags() {
		try {
			 Elastic.getTransportClient().prepareUpdate(
					Elastic.index,
					Elastic.type_within,
					record.getDbId().toString())
				.setDoc(prepareRecordDoc())
				.get();
			 updateMergedDoc();
		} catch (Exception e) {
			log.error("Cannot update record tags!", e);
		}
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
					if (entry.getKey().equals("exhibitionRecord")) {
						String introValue =  entry.getValue().get("annotation").asText();
						doc.field("annotation_all", introValue);
						doc.field("annotation", introValue);
					} else {
						doc.field(entry.getKey()+"_all", entry.getValue().asText());
						doc.field(entry.getKey(), entry.getValue().asText());
					}
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
		try {
			Elastic.getTransportClient().prepareUpdate()
				.setIndex(Elastic.index)
				.setType(Elastic.type_general)
				.setId(record.getExternalId())
			.addScriptParam("old_tags", oldRecord.getTags().toArray())
			.addScriptParam("new_tags", record.getTags().toArray())
			.setScript("for(String t: old_tags) {"
					+ "if(ctx._source.tags.contains(t))"
					+ "ctx._source.tags.remove(t)"
					+ "}; "
					+ "for(String t: new_tags) {"
					+ "if(!ctx._source.tags.contains(t))"
					+ "ctx._source.tags.add(t)"
					+ "}; ", ScriptType.INLINE)
			.execute().actionGet();
		} catch (ElasticsearchException  e) {
			log.error("Cannot update merged record document!", e);
		}
	}


	/*
	 * Update collection metadata method. Does NOT updates rights
	 */
	public void updateCollectionMetadata() {
		try {
			 Elastic.getTransportClient().prepareUpdate(
					Elastic.index,
					Elastic.type_collection,
					collection.getDbId().toString())
				.setDoc(prepareEditedCollectionDoc())
				.get();
		} catch (Exception e) {
			log.error("Cannot update collection metadata!", e);
		}
	}

	private XContentBuilder prepareEditedCollectionDoc() {
		Iterator<Entry<String, JsonNode>> collectionIt = Json.toJson(collection).fields();
		XContentBuilder doc = null;
		try {
			doc = jsonBuilder().startObject();

			while( collectionIt.hasNext() ) {
				Entry<String, JsonNode> entry = collectionIt.next();
				if( entry.getKey().equals("itemCount") ) {
					doc.field(entry.getKey(), entry.getValue().asInt());
				} else if( !entry.getKey().equals("firstEntries") &&
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
	 * Increment itemCount on collection type
	 */
	public void incItemCount() {
		try {
			Elastic.getTransportClient().prepareUpdate(
						Elastic.index,
						Elastic.type_collection,
						collection.getDbId().toString())
				.setScript("ctx._source.itemCount++;", ScriptType.INLINE)
				.execute().actionGet();
			} catch (Exception e) {
			log.error("Cannot increase itemCount!", e);
			}
	}

	/*
	 * Decrement itemCount on collection type
	 */
	public void decItemCount() {
		try {
			Elastic.getTransportClient().prepareUpdate(
						Elastic.index,
						Elastic.type_collection,
						collection.getDbId().toString())
				.setScript("ctx._source.itemCount--;", ScriptType.INLINE)
				.execute().actionGet();
			} catch (Exception e) {
			log.error("Cannot decrement itemCount!", e);
			}
	}

	/*
	 * Update rights on a collection
	 */
	public void updateCollectionRights() {
		try {
			Elastic.getTransportClient().prepareUpdate(
						Elastic.index,
						Elastic.type_collection,
						collection.getDbId().toString())
				.setDoc(prepareUpdateOnRights())
				.execute().actionGet();		} catch (Exception e) {
			log.error("Cannot update collection rights!", e);
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

		return doc;
	}


	/*
	 * Update isPublic field
	 */
	public void updateVisibility() {

		XContentBuilder doc = null;
		try {
			doc = jsonBuilder().startObject();
			doc.field("isPublic", collection.getIsPublic());
			doc.field("isPublic_all", collection.getIsPublic());
			doc.endObject();
		} catch(IOException io) {
			log.error("Cannot create document to update!", io);
		}

		List<CollectionRecord> records = DB.getCollectionRecordDAO()
				.getByCollection(collection.getDbId());

		if( records.size() == 0 ) {
			log.debug("No records within the collection to update!");
		} else if( records.size() == 1 ) {
					Elastic.getTransportClient().prepareUpdate(
							Elastic.index,
							Elastic.type_within,
							records.get(0).getDbId().toString())
						.setDoc(doc)
						.get();
					if(!collection.getIsPublic() &&
						DB.getCollectionRecordDAO()
						.checkMergedRecordVisibility(records.get(0).getExternalId(), records.get(0).getDbId()))
						return;

						Elastic.getTransportClient().prepareUpdate(
							Elastic.index,
							Elastic.type_general,
							records.get(0).getExternalId())
						.setDoc(doc)
						.get();
		} else {
				for(int i = 0; i<records.size(); i++) {
					Elastic.getBulkProcessor().add(new UpdateRequest(
							Elastic.index,
							Elastic.type_within,
							records.get(i).getDbId().toString())
						.doc(doc));
					if(!collection.getIsPublic() &&
						DB.getCollectionRecordDAO()
						.checkMergedRecordVisibility(records.get(i).getExternalId(), records.get(i).getDbId()))
						continue;

					Elastic.getBulkProcessor().add(new UpdateRequest(
							Elastic.index,
							Elastic.type_general,
							records.get(i).getExternalId())
						.doc(doc));
				}
				Elastic.getBulkProcessor().flush();
		}
	}


	/*
	 * Increment totalLikes on collection type
	 */
	/**
	 * Not fully functional
	 */
	public void incLikes() {
		incLikesToRecords();
	}

	/*
	 * Decrement totalLikes on collection type
	 */
	public void decLikes() {
		try {
			Elastic.getTransportClient().prepareUpdate(
						Elastic.index,
						Elastic.type_general,
						record.getExternalId())
				.setScript("ctx._source.totalLikes--;", ScriptType.INLINE)
				.execute().actionGet();
			} catch (Exception e) {
			log.error("Cannot update collection likes!", e);
			}
		decLikesToRecords();
	}

	/*
	 * Increment likes on records with same externalId
	 */
	public void incLikesToRecords() {

		List<CollectionRecord> records = DB.getCollectionRecordDAO()
				.getByUniqueId(record.getExternalId());
		if( records.size() < 1 ) {
			log.debug("No records within the collection to update!");
		} else {
				for(int i = 0; i<records.size(); i++) {
					System.out.println(records.get(i));
					if(!records.get(i).getDbId().equals(record.getDbId())) {
						Elastic.getBulkProcessor().add(new UpdateRequest(
								Elastic.index,
								Elastic.type_within,
								records.get(i).getDbId().toString())
							.script("ctx._source.totalLikes++;"
									+ "ctx._source.totalLikes_all++;"));
					}
				}
				Elastic.getBulkProcessor().flush();
		}
	}


	/*
	 * Decrement likes on records with same externalId
	 */
	public void decLikesToRecords() {

		List<CollectionRecord> records = DB.getCollectionRecordDAO()
				.getByUniqueId(record.getExternalId());

		if( records.size() == 0 ) {
			log.debug("No records within the collection to update!");
		} else if( records.size() == 1 ) {
					Elastic.getTransportClient().prepareUpdate(
							Elastic.index,
							Elastic.type_within,
							records.get(0).getDbId().toString())
						.setScript("ctx._source.totalLikes--;", ScriptType.INLINE)
						.get();
		} else {

				for(int i = 0; i<records.size(); i++) {
					Elastic.getBulkProcessor().add(new UpdateRequest(
							Elastic.index,
							Elastic.type_within,
							records.get(i).getDbId().toString())
						.script("ctx._source.totalLikes--;"
								+ "ctx._source.totalLikes_all--;"));
				}
				Elastic.getBulkProcessor().flush();
		}
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
