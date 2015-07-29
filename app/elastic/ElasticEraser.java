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

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.script.ScriptService.ScriptType;

import db.DB;
import model.Collection;
import model.CollectionRecord;
import play.Logger;

public class ElasticEraser {
	static private final Logger.ALogger log = Logger.of(ElasticUpdater.class);

	private Collection collection;
	private CollectionRecord record;



	public ElasticEraser(Collection c) {
		this.collection = c;
	}

	public ElasticEraser(CollectionRecord r) {
		this.record = r;
	}


	public void deleteCollection() {
		try {
			Elastic.getTransportClient().prepareDelete(
					Elastic.index,
					Elastic.type_collection,
					collection.getDbId().toString())
				.setOperationThreaded(false)
				.execute()
				.actionGet();
		} catch(ElasticsearchException e) {
			log.error("Cannot delete the specified collection document", e);
		}
	}

	public void deleteRecord() {
		try {
			Elastic.getTransportClient().prepareDelete(
					Elastic.index,
					Elastic.type_within,
					record.getDbId().toString())
				.setOperationThreaded(false)
				.execute()
				.actionGet();
		} catch(ElasticsearchException e) {
			log.error("Cannot delete the specified record document", e);
		}
	}

	public void deleteMergedRecord() {
		try {
			Elastic.getTransportClient().prepareDelete(
					Elastic.index,
					Elastic.type_general,
					record.getExternalId())
				.setOperationThreaded(false)
				.execute()
				.actionGet();
		} catch(ElasticsearchException e) {
			log.error("Cannot delete the specified merged record document", e);
		}
	}

	public void deleteRecordEntryFromMerged() {
		try {
			if(record.getExternalId() == null)
				record.setExternalId(record.getDbId().toString());

			Elastic.getTransportClient().prepareUpdate(
						Elastic.index,
						Elastic.type_general,
						record.getExternalId())
				.addScriptParam("tags", record.getTags().toArray())
				.addScriptParam("id", record.getCollectionId().toString())
				.setScript("for(String t: tags) {"
					+ "if(ctx._source.tags.contains(t)){"
					+ "ctx._source.tags.remove(t)"
					+ "}}; "
					+ "ctx._source.collections.remove(id);", ScriptType.INLINE)
				.execute().actionGet();
			} catch (Exception e) {
			log.error("Cannot delete entries from merged record!", e);
		}
	}

	/*
	 * Bulk deletes all records of a deleted collection
	 */
	public void deleteAllCollectionRecords() {
		List<CollectionRecord> records = DB.getCollectionRecordDAO()
											.getByCollection(collection.getDbId());

		if( records.size() == 0 ) {
			log.debug("No records within the collection to index!");
		} else if( records.size() == 1 ) {
				Elastic.getTransportClient().prepareDelete(
								Elastic.index,
								Elastic.type_within,
								records.get(0).getDbId().toString())
					 	.execute()
					 	.actionGet();
		} else {
			try {
				for(CollectionRecord r: records) {
					Elastic.getBulkProcessor().add(new DeleteRequest(
							Elastic.index,
							Elastic.type_within,
							r.getDbId().toString()));
				}
				Elastic.getBulkProcessor().flush();
			} catch (Exception e) {
				log.error("Error in Bulk deletes all records of a deleted collection", e);
			}
		}
	}

	/*
	 * Bulk deletes entries from merged records
	 * when a collaction is deleted
	 */
	public void deleteAllEntriesFromMerged() {
		List<CollectionRecord> records = DB.getCollectionRecordDAO()
											.getByCollection(collection.getDbId());

		if( records.size() == 0 ) {
			log.debug("No records within the collection to index!");
		} else if( records.size() == 1 ) {
				this.record = records.get(0);
				deleteRecordEntryFromMerged();
		} else {
			try {
				for(CollectionRecord r: records) {
					Elastic.getBulkProcessor().add(new UpdateRequest(
							Elastic.index,
							Elastic.type_general,
							r.getExternalId())
						.addScriptParam("tags", r.getTags().toArray())
						.addScriptParam("id", r.getCollectionId().toString())
						.script(
							"for(String t: tags) {"
							+ "if(ctx._source.tags.contains(t)){"
							+ "ctx._source.tags.remove(t)"
							+ "}}; "
							+ "ctx._source.collections.remove(id);"
						));
				}
				Elastic.getBulkProcessor().flush();
			} catch (Exception e) {
				log.error("Error in Bulk delete record entries from merged", e);
			}
		}
	}
}
