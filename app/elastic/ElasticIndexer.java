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

import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;

import play.Logger;

public class ElasticIndexer {
	static private final Logger.ALogger log = Logger.of(ElasticIndexer.class);


	/*
	 * This method send to ElasticSearch the document to be indexed.
	 * No process to the document is being done!
	 *
	 * If the document is not indexed the method return "null"
	 */
	public static IndexResponse index(String type, ObjectId dbId, Map<String, Object> doc) {
		IndexResponse response = null;
		try {
			response = Elastic.getTransportClient().prepareIndex(
					Elastic.index,
					type,
					dbId.toString()
					)
			.setSource(doc)
			.execute()
			.actionGet();
		} catch(ElasticsearchException ee) {
			log.error(ee.getDetailedMessage(),ee );
		}
		return response;

	}

	/*
	 * This method takes advantage of ElasticSearch bulk processor
	 * and accumulates many documents to send with a single request.
	 *
	 * If the indexing process is not completed for any reason the method
	 * returns "null"
	 */
	public static String indexMany(String type, List<ObjectId> ids, List<Map<String, Object>> docs) {
		if(ids.size() != docs.size()) {
			log.error("Size of two provided lists is not equal");
			return null;
		}

		try {
			for(int i=0; i<ids.size(); i++) {
				Elastic.getBulkProcessor().add(new IndexRequest(
						Elastic.index,
						type,
						ids.get(i).toString())
				.source(docs.get(i)));
				
			}
			Elastic.getBulkProcessor().close();
		} catch(ElasticsearchException ee) {
			log.error(ee.getDetailedMessage(),ee);
		}

		return "Operation completed succesfully";
	}
}
