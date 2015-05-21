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
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import model.Collection;
import model.CollectionRecord;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;

import com.fasterxml.jackson.databind.JsonNode;

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

	public void update() {
		if( collection != null )
			try {
				updateCollection();
			} catch (Exception e) {
				log.error("Cannot execute update operation!", e);
			}
		else if( record != null )
			updateSingleDocument();
		else if( json != null) {

		}
		else {
			log.error("No records to update!");
		}
	}


	private UpdateResponse updateSingleDocument() {
		UpdateRequest updateRequest = new UpdateRequest(
				Elastic.index,
				Elastic.type,
				record.getDbId().toString())
        			.doc(json);

		UpdateResponse resp = null;
		try {
			resp = Elastic.getNodeClient().update(updateRequest).get();
		} catch (InterruptedException | ExecutionException e) {
			log.error("Cannot prepare document update", e);
		}
		return resp;
    }

	private void updateCollection() throws Exception {
		List<CollectionRecord> records = DB.getCollectionRecordDAO()
				.getByCollection(collection.getDbId());
		List<XContentBuilder> documents = new ArrayList<XContentBuilder>();
		for(CollectionRecord r: records) {
			this.record = r;
			documents.add(prepareRecordDocument());
		}
		if( documents.size() == 0 ) {
			log.debug("No records within the collection to update!");
		} else if( documents.size() == 1 ) {
					Elastic.getTransportClient().prepareUpdate(
							Elastic.index,
							Elastic.type,
							record.getDbId().toString())
							.setSource(documents.get(0))
							.get();
		} else {

				int i = 0;
				for(XContentBuilder doc: documents) {
					Elastic.getBulkProcessor().add(new UpdateRequest(
							Elastic.index,
							Elastic.type,
							records.get(i).getDbId().toString())
					.source(doc));
					i++;
				}
				Elastic.getBulkProcessor().close();
		}

	}

	private XContentBuilder prepareRecordDocument() {
		Iterator<Entry<String, JsonNode>> recordIt = Json.toJson(record).fields();
		XContentBuilder doc = null;
		try {
			doc = jsonBuilder().startObject();
			while( recordIt.hasNext() ) {
				Entry<String, JsonNode> entry = recordIt.next();
				if( !entry.getKey().equals("content") &&
					!entry.getKey().equals("tags")    &&
					!entry.getKey().equals("dbId")) {

					doc.field(entry.getKey()+"_all", entry.getValue().asText());
					doc.field(entry.getKey(), entry.getValue().asText());
				}
			}
			doc.endObject();
		} catch (IOException e) {
			log.error("Cannot create json document for updating", e);
			return null;
		}

		return doc;
	}
}
