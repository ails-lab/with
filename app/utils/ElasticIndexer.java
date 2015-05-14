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
import java.util.Iterator;
import java.util.Map.Entry;

import model.Collection;
import model.CollectionRecord;

import org.bson.types.ObjectId;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.node.Node;

import play.Logger;
import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import db.DB;

public class ElasticSearcher {
	static private final Logger.ALogger log = Logger.of(ElasticSearcher.class);

	private String json;
	private static Config conf;
	private static Client nodeClient;
	private static TransportClient transportClient;
	private static BulkProcessor bulkProcessor;

	private Collection collection;
	private CollectionRecord record;

	public ElasticSearcher( Collection collection ) {
		this.collection = collection;
	}

	public ElasticSearcher( CollectionRecord record ) {
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
	public static TransportClient getTrasportClient() {
		if( transportClient == null ) {
			transportClient = new TransportClient(getSettings())
					.addTransportAddress(new InetSocketTransportAddress(
					getConf().getString("elasticsearch.host"), getConf().getInt("elasticsearch.port")));
		}
		return transportClient;
	}

	public static IndexResponse indexDocument() {
		IndexResponse response = getTrasportClient().prepareIndex("with", "record")
						.setSource(getJson(new ObjectId("5534fa5fe4b0144a2e409bf1")).toString())
						.execute()
						.actionGet();
		return response;
	}

	public IndexResponse indexSingleRecord() {
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
		IndexResponse response = getTrasportClient().prepareIndex(
									 getConf().getString("elasticsearch.index"),
									 "record",
									 record.getDbId().toString())
				.setSource(doc)
				.execute()
				.actionGet();
		return response;
	}

	public void parseXmlIntoDoc( String xmlContent) {

	}

	public static BulkProcessor getBulkProcessor() {
		if( bulkProcessor == null ) {
			bulkProcessor = BulkProcessor.builder(getTrasportClient(),
				new BulkProcessor.Listener() {

					@Override
					public void beforeBulk(long arg0, BulkRequest arg1) {
						// TODO Auto-generated method stub

					}

					@Override
					public void afterBulk(long arg0, BulkRequest arg1, Throwable arg2) {
						// TODO Auto-generated method stub

					}

					@Override
					public void afterBulk(long arg0, BulkRequest arg1,
							BulkResponse arg2) {
						// TODO Auto-generated method stub

					}

				})
				.setBulkActions(200)
				.setFlushInterval(TimeValue.timeValueSeconds(5))
				.setConcurrentRequests(1)
				.build();
		}
		return bulkProcessor;
	}
}
