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


package general.daoTests;

import static org.fest.assertions.Assertions.assertThat;
import general.TestUtils;

import java.util.List;

import model.Collection;
import model.CollectionMetadata;
import model.CollectionRecord;
import model.Media;
import model.User;

import org.bson.types.ObjectId;
import org.junit.Test;
import org.mongodb.morphia.Key;

import db.DB;

public class CollectionDAOTest {

	@Test
	public void testCRUD() {

		DB.getDs().ensureIndexes(Collection.class);
		Collection collection = new Collection();
		collection.setDescription("Collection to test CRUD");
		collection.setIsPublic(true);
		collection.setTitle("The TESTCRUD collection");

		//save the thumbnail for the collection
		Media thumb = null;
		MediaDAOTest imageDAO = new MediaDAOTest();
		try {
			thumb = imageDAO.testMediaStorage();
		} catch (Exception e) {
			System.out.println("Cannot save collection thumbnail to database!");
		}
		collection.setThumbnail(thumb);

		// metadata for connection with user
		CollectionMetadata colMeta = new CollectionMetadata();
		colMeta.setDescription(collection.getDescription());
		colMeta.setTitle(collection.getTitle());

		User user = DB.getUserDAO().getByEmail("heres42@mongo.gr");
		collection.setOwnerId(user);


		//save the new created collection
		Key<Collection> colKey = DB.getCollectionDAO().makePermanent(collection);
		System.out.println(colKey.getId());
		assertThat(colKey).isNotNull();

		//get by id
		Collection a = DB.getCollectionDAO().getById(new ObjectId(colKey.getId().toString()));
		assertThat(a).isNotNull()
		.overridingErrorMessage("Test collection not found using db id.");

		// get by title
		Collection b = DB.getCollectionDAO().getByTitle("The TESTCRUD collection");
		assertThat(b).isNotNull()
		.overridingErrorMessage("Test collection not found using title.");

		// get collection owner
		User u = DB.getCollectionDAO().getCollectionOwner(new ObjectId(colKey.getId().toString()));
		assertThat(u).isNotNull()
		.overridingErrorMessage("User not found using db id.");

		//get user collections
		List<Collection> c = DB.getCollectionDAO().getByOwner(new ObjectId("54fd8ee6e4b0f8b923eb66cb"));
		assertThat(c).isNotNull()
		.overridingErrorMessage("Test collections not found using owner id.");
		//assertThat(c.size()).isGreaterThan(0);

		//get, modify, save again
		Collection e = DB.getCollectionDAO().getById(new ObjectId(colKey.getId().toString()));
		e.setTitle("DbId test");
		colKey = DB.getCollectionDAO().makePermanent(e);
		System.out.println(colKey.getId());



		// remove from db
		DB.getCollectionDAO().makeTransient(b);
		DB.getCollectionDAO().deleteById(new ObjectId(colKey.getId().toString()));

		// check its gone
		Collection d = DB.getCollectionDAO().getById(new ObjectId(colKey.getId().toString()));
		assertThat( d )
			.overridingErrorMessage("User not deleted!")
			.isNull();
	}

	@Test
	public void storeCollection() {

		// store Collection
		User user;
		for (int i = 0; i < 1000; i++) {

			// a user creates a new collection
			Collection collection = new Collection();
			collection.setDescription("This is a test collection");
			collection.setIsPublic(true);
			collection.setTitle("Test Collection " + TestUtils.randomString());

			//save the thumbnail for the collection
			Media thumb = null;
			MediaDAOTest imageDAO = new MediaDAOTest();
			try {
				thumb = imageDAO.testMediaStorage();
			} catch (Exception e) {
				System.out.println("Cannot save collection thumbnail to database!");
			}
			collection.setThumbnail(thumb);

			// metadata for connection with user
			CollectionMetadata colMeta = new CollectionMetadata();
			colMeta.setDescription(collection.getDescription());
			colMeta.setTitle(collection.getTitle());

			if (i == 42) {
				user = DB.getUserDAO().getByEmail("heres42@mongo.gr");
				collection.setOwnerId(user);
			} else {
				user = DB.getUserDAO().find().asList().get(i);
				collection.setOwnerId(user);
			}

			//save the new created collection
			Key<Collection> colKey = DB.getCollectionDAO().makePermanent(collection);
			assertThat(colKey).isNotNull();

			// save metadata to user
			colMeta.setCollectionId(new ObjectId(colKey.getId().toString()));
			if(user.getCollectionMetadata().size() < 20 )
				user.getCollectionMetadata().add(colMeta);
			DB.getUserDAO().makePermanent(user);


			/* Process of collection an item */
			/*
			 * store an image first (media object)
			 * store a recordLink
			 * connect the recordlink with the collection
			 */
			//create a recordLInk (optionally a record)
			for(int j = 0; j < TestUtils.r.nextInt(10000); j++) {
				RecordDAO recordDAO = new RecordDAO();
				CollectionRecord record = recordDAO.storeRecordLink();
				record.setCollectionId(collection.getDbId());
				DB.getCollectionRecordDAO().makePermanent(record);
				assertThat(record.getDbId()).isNotNull();

				//fill with the to-20 recordLinks
				if(collection.getFirstEntries().size() < 20)
					collection.getFirstEntries().add(record);

				//save the updated collection
				DB.getCollectionDAO().makePermanent(collection);
			}
		}
	}
}
