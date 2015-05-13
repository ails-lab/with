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


package general.elasticsearch;

import model.CollectionRecord;

import org.bson.types.ObjectId;
import org.junit.Test;

import utils.ElasticSearcher;
import db.DB;

public class ElasticTest {

	@Test
	public void testIndex() {

		/*IndexResponse resp = ElasticSearcher.indexDocument();
		System.out.println("Index name: " + resp.getIndex()
						 + " Type name: " + resp.getType()
						 + " Document ID: " + resp.getId());
		*/
		/*for(int i=0;i<=2400;i++) {
			Query<CollectionRecord> q = DB.getCollectionRecordDAO().createQuery().limit(50).offset(i);
			List<CollectionRecord> records = DB.getCollectionRecordDAO().find(q).asList();
			for(CollectionRecord rec: records) {
				ElasticSearcher.getBulkProcessor().add(new IndexRequest("with", "record").source(Json.toJson(rec).toString()));
			}
		}*/

		CollectionRecord record = DB.getCollectionRecordDAO().getById(new ObjectId("5534fa5fe4b0144a2e409bf1"));
		ElasticSearcher indexer = new ElasticSearcher( record );
		indexer.indexSingleRecord();

	}


}