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

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.delete.DeleteRequest;
import play.Logger;

public class ElasticEraser {
	static private final Logger.ALogger log = Logger.of(ElasticUpdater.class);


	/*
	 * Delete the specified Resource using it's db id
	 * from the index
	 */
	public static void deleteResource(String dbId) {
		try {
			Elastic.getTransportClient().prepareDelete(
					Elastic.index,
					Elastic.typeResource,
					dbId)
				.setOperationThreaded(false)
				.execute()
				.actionGet();
		} catch(ElasticsearchException e) {
			log.error("Cannot delete the specified resource document", e);
		}
	}



	/*
	 * Bulk deletes all resources of a deleted collection
	 */
	public static void deleteManyResources(List<String> ids) {

		if( ids.size() == 0 ) {
			log.debug("No records within the collection to index!");
		} else if( ids.size() == 1 ) {
				Elastic.getTransportClient().prepareDelete(
								Elastic.index,
								Elastic.typeResource,
								ids.get(0))
					 	.execute()
					 	.actionGet();
		} else {
			try {
				for(String id: ids) {
					Elastic.getBulkProcessor().add(new DeleteRequest(
							Elastic.index,
							Elastic.typeResource,
							id));
				}
				Elastic.getBulkProcessor().flush();
			} catch (Exception e) {
				log.error("Error in Bulk deletes all records of a deleted collection", e);
			}
		}
	}

}
