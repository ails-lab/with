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
import java.util.ArrayList;
import java.util.List;

import model.resources.RecordResource;
import model.resources.WithResource.WithResourceType;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.indices.IndexMissingException;
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
	private static Settings settings;
	private static Settings indexSettings;
	private static NodeClient nodeClient;
	private static Node node;
	private static TransportClient transportClient;
	private static BulkProcessor bulkProcessor;

	public static String cluster 		    = getConf().getString("elasticsearch.cluster");
	public static String index   		    = getConf().getString("elasticsearch.index.name");
	public static String old_index 		    = getConf().getString("elasticsearch.old_index.name");
	public static String shards   		    = getConf().getString("elasticsearch.index.num_of_shards");
	public static String replicas  		    = getConf().getString("elasticsearch.index.num_of_replicas");
	public static String alias   		    = getConf().getString("elasticsearch.alias.name");
	public static String mappingResource    = getConf().getString("elasticsearch.index.mapping.resource");

	public static final String typeResource       = WithResourceType.RecordResource.toString().toLowerCase();
	public static final String typeCollection     = WithResourceType.CollectionObject.toString().toLowerCase();
	public static final String typeCultural       = WithResourceType.CulturalObject.toString().toLowerCase();
	public static final String typeAgent          = WithResourceType.AgentObject.toString().toLowerCase();
	public static final String typeEvent		  = WithResourceType.EventObject.toString().toLowerCase();
	public static final String typePlace	      = WithResourceType.PlaceObject.toString().toLowerCase();
	public static final String typeTimespan	      = WithResourceType.TimespanObject.toString().toLowerCase();
	public static final String typeEuscreen	      = WithResourceType.EuScreenObject.toString().toLowerCase();
	public static final String typeThesaurus 	  = WithResourceType.ThesaurusObject.toString().toLowerCase();
	public static final List<String> allTypes 	  = new ArrayList<String>() {{
														add(typeCultural);
														add(typeResource);add(typeCollection); add(typeThesaurus);
														add(typeAgent);add(typeEvent);add(typePlace);add(typeTimespan); }};



	private final static String host = getConf().getString("elasticsearch.host");
	private final static int    port = getConf().getInt("elasticsearch.port");

	private static Config getConf() {
		if (conf == null) {
			conf = ConfigFactory.load();
		}
		return conf;
	}

	public static Settings getSettings() {
		if(settings == null) {
			settings = ImmutableSettings
				.settingsBuilder()
				.put("cluster.name",
						cluster).build();
		}
		return settings;
	}

	private static Settings getIndexSettings() {
		if(indexSettings == null) {
			indexSettings = ImmutableSettings
					.settingsBuilder()
					.put("number_of_shards", shards)
					.put("number_of_replicas", replicas)
					.build();
		}
		return indexSettings;
	}

	public static void closeClient() {
		if((transportClient != null) && (nodeClient == null))
			transportClient.close();
		else if( (transportClient == null) && (nodeClient != null)) {
			nodeClient.close();
			node.stop();
		}
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
	 *
	 * Best used for long-lived connections
	 */
	public static Client getNodeClient() {
		if (nodeClient == null) {
				if(node == null) {
					node = nodeBuilder()
							.clusterName(cluster)
							.client(true).local(true).node();
					nodeClient = (NodeClient) node.client();
				}
		}
		return nodeClient;
	}

	/*
	 * The TransportClient connects remotely to an elasticsearch cluster. It
	 * does not join the cluster, but simply gets one or more initial transport
	 * addresses and communicates with them. Though most actions will probably
	 * be "two hop" operations.
	 *
	 * Best used for multiple short connections
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
	public static void initializeIndex() {
		ImmutableOpenMap<String, IndexMetaData> indices = null;
		indices = Elastic.getTransportClient().admin().cluster()
					.prepareState().execute().actionGet()
					.getState().getMetaData().indices();
		if(indices.containsKey(index)) {
			return;
		}


		// take custom mappings
		JsonNode resourceMapping = null;
		try {
			resourceMapping = Json.parse(new String(Files.readAllBytes(Paths.get("conf/"+mappingResource))));
		} catch (IOException e) {
			log.error("Cannot read mapping from file!", e);
			return;
		}

		// create the index and put the alias to it
		try {
			CreateIndexRequestBuilder index_builder = Elastic.getTransportClient()
					.admin().indices().prepareCreate(Elastic.index)
					.setSettings(Elastic.getIndexSettings());

			/* Add mappings for all fields */
			for(String type: allTypes)
					index_builder.addMapping(type, resourceMapping.toString());

			/* Excecute the request */
					index_builder.execute().actionGet();


			if(!old_index.equals(""))
				Elastic.getTransportClient().admin().indices().prepareAliases()
						.removeAlias("old_index", alias)
						.execute().actionGet();
		} catch(IndexMissingException e) {
			log.debug(e.getDetailedMessage());
		} catch(Exception e) {
			log.debug(e.getMessage());
		} finally {
			if(!alias.equals(""))
				Elastic.getTransportClient().admin().indices().prepareAliases()
					.addAlias(index, alias)
					.execute().actionGet();
		}
	}

	public static void reindex_records() {
		// hopefully delete index and reput it in place
		//getNodeClient().admin().indices().prepareDelete(index).execute().actionGet();
		//putMapping();

				Callback<RecordResource> callback = new Callback<RecordResource>() {
				@Override
					public void invoke(RecordResource rr ) throws Throwable {
						ElasticIndexer.index(Elastic.typeResource, rr.getDbId(), rr.transform());
						}
				};
				try {
					DB.getRecordResourceDAO().onAll( callback, false );
				} catch( Exception e ) {
					log.error( "ReIndexing problem", e );
				}
	}

}

