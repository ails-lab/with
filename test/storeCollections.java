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


import model.Collection;
import model.User;

import org.junit.Before;
import org.junit.Test;

import db.DB;


public class storeCollections  {

	@Before
	public void setUp() {
		DB.initialize();
	}

	@Test
	public void storeCollection() {

		// store Collection
		User user;
		for(int i = 0;i < 1000; i++) {
			Collection collection = new Collection();
			collection.setDescription("This is a test collection");
			collection.setPublic(true);
			collection.setTitle("The TEST collection");

			if(i == 42) {
				user = DB.getUserDAO().getByEmail("heres42_@mongo.gr");
				collection.setOwner(user);
			} else {
				user = DB.getUserDAO().find().asList().get(i);
				collection.setOwner(user);
			}
			DB.getCollectionDAO().save(collection);
		}

	}
}
