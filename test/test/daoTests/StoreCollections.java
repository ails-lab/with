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

import java.util.ArrayList;

import model.Collection;
import model.CollectionMetadata;
import model.RecordLink;
import model.User;

import org.bson.types.ObjectId;
import org.junit.Test;
import org.mongodb.morphia.Key;

import db.DB;

public class StoreCollections {

	@Test
	public void storeCollection() {

		// store Collection
		User user;
		for (int i = 0; i < 1000; i++) {
			Collection collection = new Collection();
			CollectionMetadata colMeta = new CollectionMetadata();


			collection.setDescription("This is a test collection");
			colMeta.setDescription(collection.getDescription());
			collection.setPublic(true);
			collection.setTitle("The TEST collection");
			colMeta.setTitle(collection.getTitle());

			if (i == 42) {
				user = DB.getUserDAO().getByEmail("heres42@mongo.gr");
				collection.setOwner(user);
			} else {
				user = DB.getUserDAO().find().asList().get(i);
				collection.setOwner(user);
			}

			ArrayList<RecordLink> firstEntries = new ArrayList<RecordLink>();
			for (int j = 0; j < 20; j++) {
				RecordLink rlink = DB.getRecordLinkDAO().find().asList().get(j);
				firstEntries.add(rlink);
			}
			collection.setFirstEntries(firstEntries);
			Key<Collection> colKey = DB.getCollectionDAO().makePermanent(collection);
			colMeta.setCollection(new ObjectId(colKey.getId().toString()));

			user.getCollectionMetadata().add(colMeta);
			DB.getUserDAO().makePermanent(user);
		}
	}
}
