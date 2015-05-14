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


package utils;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import model.Collection;
import model.CollectionRecord;

import org.bson.types.ObjectId;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
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

	private String json;
	private static Config conf;
	private static Client nodeClient;
	private static TransportClient transportClient;
	private static BulkProcessor bulkProcessor;

	private Collection collection;
	private CollectionRecord record;

	public ElasticIndexer( Collection collection ) {
		this.collection = collection;
	}

	public ElasticIndexer( CollectionRecord record ) {
		this.record = record;
	}

	private static JsonNode getJson(ObjectId recordId) {
		CollectionRecord record = DB.getCollectionRecordDAO().getById(recordId);
		return Json.toJson(record);
	}

	public static Config getConf() {
		if( conf == null ) {
			conf = ConfigFactory.load();
		}
		return conf;
	}

	public static Settings getSettings() {
		Settings settings = ImmutableSettings.settingsBuilder()
				.put("cluster.name", getConf().getString("elasticsearch.cluster")).build();
		return settings;
	}

	/*
	 * The benefit of using this Client
	 * is the fact that operations are automatically
	 * routed to the node(s) the operations need to be executed on,
	 * without performing a "double hop".
	 * For example, the index operation will automatically
	 * be executed on the shard that it will end up existing at.
	 */
	public static Client getNodeClient() {
		if( nodeClient == null) {
			Node node = nodeBuilder()
					.clusterName(getConf().getString("elasticsearch.cluster"))
					.client(true)
					.local(true)
					.node();
			nodeClient = node.client();
		}
		return nodeClient;
	}

	/*
	 * The TransportClient connects remotely to an
	 * elasticsearch cluster. It does not join the cluster,
	 * but simply gets one or more initial transport addresses
	 * and communicates with them.
	 * Though most actions will probably be "two hop" operations.
	 */
	public static TransportClient getTransportClient() {
		if( transportClient == null ) {
			transportClient = new TransportClient(getSettings())
					.addTransportAddress(new InetSocketTransportAddress(
					getConf().getString("elasticsearch.host"), getConf().getInt("elasticsearch.port")));
		}
		return transportClient;
	}


	public  void index() {
		if( collection != null )
			indexCollection();
		else if( record != null )
			indexSingleDocument();
		else {
			log.error("No records to index!");
		}
	}

	public  void indexCollection() {
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
			IndexResponse response = getTransportClient().prepareIndex(
					 getConf().getString("elasticsearch.index"),
					 "record",
					 record.getDbId().toString())
					 	.setSource(documents.get(0))
					 	.execute()
					 	.actionGet();
		} else {
			try {
				int i = 0;
				for(XContentBuilder doc: documents) {
					getBulkProcessor().add(new IndexRequest("with", "record", records.get(i).getDbId().toString())
													.source(doc));
					i++;
				}
				getBulkProcessor().close();
			} catch (Exception e) {
				log.error("Error in Bulk operations", e);
			}
		}
	}

	public IndexResponse indexSingleDocument() {
		IndexResponse response = getTransportClient().prepareIndex(
					getConf().getString("elasticsearch.index"),
										"record",
										record.getDbId().toString())
					.setSource(prepareRecordDocument())
					.execute()
					.actionGet();
		return response;
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
					!entry.getKey().equals("dbId"))
					doc.field(entry.getKey(), entry.getValue().asText());
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

	public static BulkProcessor getBulkProcessor() {
		if( bulkProcessor == null ) {
			bulkProcessor = BulkProcessor.builder(getTransportClient(),
				new BulkProcessor.Listener() {

					@Override
					public void beforeBulk(long arg0, BulkRequest arg1) {
						// TODO Auto-generated method stub

					}

					@Override
					public void afterBulk(long arg0, BulkRequest arg1, Throwable arg2) {

					}

					@Override
					public void afterBulk(long arg0, BulkRequest request,
							BulkResponse response) {
						if( response.hasFailures() ) {
							log.error(response.buildFailureMessage());
						}
					}

				})
				.setBulkActions(1000)
				.setBulkSize(new ByteSizeValue(5000))
				.setFlushInterval(TimeValue.timeValueSeconds(5))
				.setConcurrentRequests(1)
				.build();
		}
		return bulkProcessor;
	}
}
