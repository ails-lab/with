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


package general;

import general.daoTests.CollectionDAOTest;
import general.daoTests.SearchAndSearchResultDAOTest;
import general.daoTests.UserDAOTest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import model.Collection;
import model.CollectionRecord;

import org.bson.types.ObjectId;
import org.junit.Test;

import play.libs.F.Callback;

import com.mongodb.BasicDBObject;

import db.DB;

/**
 *
 * Simple (JUnit) tests that can call all parts of a play app. If you are
 * interested in mocking a whole application, see the wiki for more details.
 *
 */
// @FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DBTests {

	// @Test
	public void createMockupDB() {
		UserDAOTest userDAO = new UserDAOTest();
		SearchAndSearchResultDAOTest searchesDAO = new SearchAndSearchResultDAOTest();
		CollectionDAOTest colDAO = new CollectionDAOTest();

		// create a whole dummy database
		userDAO.massStorage();
		searchesDAO.storeSearchesWithSearchResults();
		colDAO.storeCollection();
	}

	// @Test
	public void createFavorites() throws Exception {
		List<ObjectId> users = DB.getUserDAO().findIds();
		List<ObjectId> usersWithFav = new ArrayList<ObjectId>();
		Callback<Collection> callback = new Callback<Collection>() {
			@Override
			public void invoke(Collection collection) throws Throwable {
				if (collection.getTitle().equals("_favorites")) {
					usersWithFav.add(collection.getOwnerId());
				}
			}
		};
		DB.getCollectionDAO().onAll(callback, false);
		users.removeAll(usersWithFav);
		Collection favCollection;
		for (ObjectId userId : users) {
			favCollection = new Collection();
			favCollection.setTitle("_favorites");
			favCollection.setCreated(new Date());
			favCollection.setOwnerId(userId);
			DB.getCollectionDAO().makePermanent(favCollection);
			System.out.println("Successfully created favorites for userId "
					+ userId.toString());

		}
	}

	// @Test
	public void cleanRecords() throws Exception {
		List<ObjectId> existingCollections = DB.getCollectionDAO().findIds();
		List<ObjectId> collections = new ArrayList<ObjectId>();
		Callback<CollectionRecord> callback = new Callback<CollectionRecord>() {
			@Override
			public void invoke(CollectionRecord record) throws Throwable {
				collections.add(record.getCollectionId());
			}
		};
		DB.getCollectionRecordDAO().onAll(callback, false);
		collections.removeAll(existingCollections);
		if (collections.size() > 0) {
			int removed = DB.getCollectionRecordDAO().removeAll("collectionId",
					"in", collections);
			System.out.println("Successfully removed " + removed
					+ " items from database");
		}
	}

	// @Test
	public void clearCache() {
		try {
			DB.getMediaDAO().deleteCached();
			System.out.println("Cache cleared");
		} catch (Exception e) {
			System.out.println("Couldn't clear cache:" + e.getMessage());
		}
	}

	@Test
	public void cacheStatistics() {
		BasicDBObject query = new BasicDBObject();
		query.containsField("externalId");
		int cached = DB.getMediaDAO().countAll(query);
		System.out.println("Cached items: " + cached);
		query.append("thumbnail", true);
		int thumbs = DB.getMediaDAO().countAll(query);
		System.out.println("Thumbnails: " + thumbs);
	}
}
