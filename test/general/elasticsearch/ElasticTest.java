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

import java.util.List;

import model.Collection;
import model.CollectionRecord;

import org.bson.types.ObjectId;
import org.junit.*;
import org.junit.Test;
import org.junit.Assert;

import db.DB;
import elastic.Elastic;
import elastic.ElasticIndexer;

public class ElasticTest {

	@Test
	public void testIndex() {

		/*IndexResponse resp = ElasticSearcher.indexDocument();
		System.out.println("Index name: " + resp.getIndex()
						 + " Type name: " + resp.getType()
						 + " Document ID: " + resp.getId());
		*/
		/*for(int i=0;i<=2550;i++) {
			Query<CollectionRecord> q = DB.getCollectionRecordDAO().createQuery().limit(50).offset(i);
			List<CollectionRecord> records = DB.getCollectionRecordDAO().find(q).asList();
			for(CollectionRecord rec: records) {
				ElasticSearcher.getBulkProcessor().add(new IndexRequest("with", "record").source(Json.toJson(rec).toString()));
			}
		}*/


		Collection c = DB.getCollectionDAO().get(new ObjectId("55802c0ee4b07326b34b57c2"));
		CollectionRecord r = DB.getCollectionRecordDAO().get(new ObjectId("5570299f44aefbeb0a81c4b7"));
		CollectionRecord r1 = DB.getCollectionRecordDAO().get(new ObjectId("5570330344ae86da914cb469"));
		//r.setSource("MINT");
		//r.setIsPublic(true);
		ElasticIndexer indexer = new ElasticIndexer( c, r );
		//ElasticUpdater updater = new ElasticUpdater( r1 );
		Elastic.putMapping();
		indexer.index();

		/*CollectionRecord r2 = DB.getCollectionRecordDAO().get(new ObjectId("55350eede4b0cd1145214523"));
		if( DB.getCollectionRecordDAO().countByUniqueId(r2.getExternalId()) > 1 ) {
			ElasticUpdater updater = new ElasticUpdater( r2 );
			updater.update();
		}*/

		/*CollectionRecord record = DB.getCollectionRecordDAO().getById(new ObjectId("5534fa5fe4b0144a2e409bf1"));
		ElasticSearcher indexer = new ElasticSearcher( record );
		indexer.indexSingleRecord();
*/
	}

	@Test
	public void reindex_collection_from_mongo() {
		List<Collection> allCols = DB.getCollectionDAO().find().asList();
		for(Collection c: allCols) {
			ElasticIndexer indexer = new ElasticIndexer(c);
			indexer.indexCollectionMetadata();
		}
	}

	@Test
	public void reindex_records_from_mongo() {
		List<CollectionRecord> allRecs = DB.getCollectionRecordDAO().find().asList();
		for(CollectionRecord r: allRecs) {
			ElasticIndexer indexer = new ElasticIndexer(r);
			indexer.index();
		}
	}


}
