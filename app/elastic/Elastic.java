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

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;

import model.Collection;
import model.CollectionRecord;

import org.bson.types.ObjectId;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.node.Node;

import play.Logger;
import play.libs.Json;
import play.libs.F.Callback;

import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import db.DB;

public class Elastic {
	public static final Logger.ALogger log = Logger.of(Elastic.class);

	private static Config conf;
	private static Client nodeClient;
	private static TransportClient transportClient;
	private static BulkProcessor bulkProcessor;

	public static String cluster 		    = getConf().getString("elasticsearch.cluster");
	public static String index   		    = getConf().getString("elasticsearch.index.name");
	public static String mapping_collection = getConf().getString("elasticsearch.index.mapping.collection");
	public static String type_collection    = getConf().getString("elasticsearch.index.type.collection");
	public static String mapping_within     = getConf().getString("elasticsearch.index.mapping.within");
	public static String type_within        = getConf().getString("elasticsearch.index.type.within");
	public static String mapping_general    = getConf().getString("elasticsearch.index.mapping.general");
	public static String type_general       = getConf().getString("elasticsearch.index.type.general");


	private final static String host = getConf().getString("elasticsearch.host");
	private final static int    port = getConf().getInt("elasticsearch.port");

	private static Config getConf() {
		if (conf == null) {
			conf = ConfigFactory.load();
		}
		return conf;
	}

	public static Settings getSettings() {
		Settings settings = ImmutableSettings
				.settingsBuilder()
				.put("cluster.name",
						cluster).build();
		return settings;
	}

	public static void closeClient() {
		if((transportClient != null) && (nodeClient == null))
			transportClient.close();
		else if( (transportClient == null) && (nodeClient != null))
			nodeClient.close();
		else if( (transportClient != null) && (nodeClient != null)) {
			nodeClient.close();
			transportClient.close();
		} else
			log.debug("No client to close!");

	}

	/*
	 * The benefit of using this Client is the fact that operations are
	 * automatically routed to the node(s) the operations need to be executed
	 * on, without performing a "double hop". For example, the index operation
	 * will automatically be executed on the shard that it will end up existing
	 * at.
	 */
	public static Client getNodeClient() {
		if (nodeClient == null) {
			Node node = nodeBuilder()
					.clusterName(cluster)
					.client(true).local(true).node();
			nodeClient = node.client();
		}
		return nodeClient;
	}

	/*
	 * The TransportClient connects remotely to an elasticsearch cluster. It
	 * does not join the cluster, but simply gets one or more initial transport
	 * addresses and communicates with them. Though most actions will probably
	 * be "two hop" operations.
	 */
	public static TransportClient getTransportClient() {
		if (transportClient == null) {
			transportClient = new TransportClient(getSettings())
					.addTransportAddress(new InetSocketTransportAddress(
							host,
							port));
		}
		return transportClient;
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

	//have to check conditions on mapping Map whether a mapping exists or not
	//have to be checked to the local computer
	private static boolean hasMapping() {
		GetMappingsResponse mapResp = null;
		ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = null;
		try {
			mapResp = Elastic.getTransportClient().admin().indices()
					.getMappings(new GetMappingsRequest().indices(Elastic.index)).get();
			mappings = mapResp.getMappings();
		} catch(ElasticsearchException ese) {
			log.error("Client or error on mappings", ese);
		} catch (InterruptedException e) {
			log.error("Client or error on mappings", e);
		} catch (ExecutionException e) {
			log.error("Indice does not exist");
			return false;
		}

		if( (mappings!=null) && (mappings.containsKey(index))
				&& (mappings.get(index).containsKey(type_general)
					|| mappings.get(index).containsKey(type_within)
					|| mappings.get(index).containsKey(type_collection)) )
			return true;
		return false;

	}

	public static CreateIndexResponse putMapping() {
		if(!hasMapping()) {
			//getNodeClient().admin().indices().prepareDelete("with-mapping").execute().actionGet();
			JsonNode general_mapping = null;
			JsonNode within_mapping = null;
			JsonNode collection_mapping = null;
			CreateIndexRequestBuilder cireqb = null;
			CreateIndexResponse ciresp = null;
			try {
				general_mapping = Json.parse(new String(Files.readAllBytes(Paths.get("conf/"+mapping_general))));
				within_mapping = Json.parse(new String(Files.readAllBytes(Paths.get("conf/"+mapping_within))));
				collection_mapping = Json.parse(new String(Files.readAllBytes(Paths.get("conf/"+mapping_collection))));
				cireqb = Elastic.getTransportClient().admin().indices().prepareCreate(Elastic.index);
				cireqb.addMapping(type_general, general_mapping.toString());
				cireqb.addMapping(type_within, within_mapping.toString());
				cireqb.addMapping(type_collection, collection_mapping.toString());
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

	public static void reindex() {
		// hopefully delete index and reput it in place
		//getNodeClient().admin().indices().prepareDelete(index).execute().actionGet();
		//putMapping();

		Callback<Collection> callback = new Callback<Collection>() {
		@Override
			public void invoke(Collection c ) throws Throwable {
				ElasticIndexer ei = new ElasticIndexer( c );
				ei.index();
			}
		};
		try {
			DB.getCollectionDAO().onAll( callback, false );
		} catch( Exception e ) {
			log.error( "ReIndexing problem", e );
		}
	}

	public static void reindex_records() {
		// hopefully delete index and reput it in place
		//getNodeClient().admin().indices().prepareDelete(index).execute().actionGet();
		//putMapping();

				Callback<CollectionRecord> callback = new Callback<CollectionRecord>() {
				@Override
					public void invoke(CollectionRecord r ) throws Throwable {
						ElasticIndexer ei = new ElasticIndexer( r );
						ei.index();
					}
				};
				try {
					DB.getCollectionRecordDAO().onAll( callback, false );
				} catch( Exception e ) {
					log.error( "ReIndexing problem", e );
				}
	}

	public static void reindex_some(List<String> ids) {
		for(String id: ids) {
			CollectionRecord r = DB.getCollectionRecordDAO().get(new ObjectId(id));
			ElasticIndexer ei = new ElasticIndexer(r);
			ei.index();
		}
	}

}

