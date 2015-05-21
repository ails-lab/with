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
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import model.Collection;
import model.CollectionRecord;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.node.Node;

import play.Logger;
import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

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



	public void index() {
		if( collection != null )
			indexCollection();
		else if( record != null )
			indexSingleDocument();
		else {
			log.error("No records to index!");
		}
	}

	public void indexCollection() {
		List<CollectionRecord> records = DB.getCollectionRecordDAO()
											.getByCollection(collection.getDbId());
		List<XContentBuilder> documents = new ArrayList<XContentBuilder>();
		for(CollectionRecord r: records) {
			this.record = r;
			documents.add(prepareRecordDocument());
		}
		if( documents.size() == 0 ) {
			log.debug("No records within the collection to index!");
		} else if( documents.size() == 1 ) {
				Elastic.getTransportClient().prepareIndex(
						Elastic.index,
						Elastic.type,
					 record.getDbId().toString())
					 	.setSource(documents.get(0))
					 	.execute()
					 	.actionGet();
		} else {
			try {
				int i = 0;
				for(XContentBuilder doc: documents) {
					Elastic.getBulkProcessor().add(new IndexRequest(
							Elastic.index,
							Elastic.type,
							records.get(i).getDbId().toString())
					.source(doc));
					i++;
				}
				Elastic.getBulkProcessor().close();
			} catch (Exception e) {
				log.error("Error in Bulk operations", e);
			}
		}
	}

	public IndexResponse indexSingleDocument() {
		IndexResponse response = Elastic.getTransportClient().prepareIndex(
				Elastic.index,
				Elastic.type,
										record.getDbId().toString())
					.setSource(prepareRecordDocument())
					.execute()
					.actionGet();
		return response;
	}

	private boolean hasMapping() {
		GetMappingsResponse mapResp = null;
		ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = null;
		try {
			mapResp = Elastic.getTransportClient().admin().indices().getMappings(new GetMappingsRequest().indices(Elastic.index)).get();
			mappings = mapResp.getMappings();
		} catch(ElasticsearchException ese) {
			log.error("Client or error on mappings", ese);
		} catch (InterruptedException e) {
			log.error("Client or error on mappings", e);
		} catch (ExecutionException e) {
			log.error("Client or error on mappings", e);
		}

		if( (mappings!=null) && mappings.containsKey(Elastic.index))
			return true;
		return false;

	}

	public CreateIndexResponse putMapping() {
		if(!hasMapping()) {
			//getNodeClient().admin().indices().prepareDelete("with-mapping").execute().actionGet();
			JsonNode mapping = null;
			CreateIndexRequestBuilder cireqb = null;
			CreateIndexResponse ciresp = null;
			try {
				mapping = Json.parse(new String(Files.readAllBytes(Paths.get("conf/"+Elastic.mapping))));
				cireqb = Elastic.getTransportClient().admin().indices().prepareCreate(Elastic.index);
				cireqb.addMapping(Elastic.type, mapping.toString());
				ciresp = cireqb.execute().actionGet();
			} catch(ElasticsearchException ese) {
				log.error("Cannot put mapping!", ese);
			} catch (IOException e) {
				log.error("Cannot read mapping from file!", e);
				return null;
			}
			return ciresp;
		}
		return null;
	}

	public XContentBuilder prepareRecordDocument() {
		Iterator<Entry<String, JsonNode>> recordIt = Json.toJson(record).fields();
		XContentBuilder doc = null;
		try {
			doc = jsonBuilder().startObject();
			while( recordIt.hasNext() ) {
				Entry<String, JsonNode> entry = recordIt.next();
				if( !entry.getKey().equals("content") &&
					!entry.getKey().equals("tags")    &&
					!entry.getKey().equals("dbId")) {
					//if( entry.getKey().equals("title") ) {
						doc.field(entry.getKey()+"_all", entry.getValue().asText());
					//}
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

	public void parseXmlIntoDoc( String xmlContent ) {

	}

}
