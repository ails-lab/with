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


package test.daoTests;

import java.util.List;

import model.Collection;
import model.CollectionEntry;
import model.RecordLink;

import org.junit.Test;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;

import test.TestUtils;
import db.DB;

public class StoreCollectionEntries {

	
	@Test
	public void storeCollectionEntries() {
		
		List<Key<Collection>> allCollections =
				DB.getCollectionDAO().find().asKeyList();
		
		
		for(Key<Collection> collectionKey: allCollections) {
			
			Query<RecordLink> q = 
					DB.getRecordLinkDAO().createQuery()
					.offset(TestUtils.r.nextInt(23))
					.limit(40);
			
			List<RecordLink> rlinks =
					DB.getRecordLinkDAO().find(q).asList();
			
			for(RecordLink rl: rlinks) {
				CollectionEntry entry = new CollectionEntry();
				entry.setCollection(collectionKey.getId().toString());
				entry.setRecordLink(rl);
				DB.getCollectionEntryDAO().makePermanent(entry);
			}
			
		}
	}
}
