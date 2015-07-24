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
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import model.Collection;

import org.bson.types.ObjectId;
import org.junit.Test;

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

	@Test
	public void createFavorites() {
		List<ObjectId> favoritesId = DB.getCollectionDAO().getIdsByTitle(
				"_favorites");
		List<Collection> collections = DB.getCollectionDAO()
				.getCollectionsByIds(favoritesId,
						new ArrayList<String>(Arrays.asList("ownerId")));
		List<ObjectId> usersWithFav = new ArrayList<ObjectId>();
		for (Collection col : collections) {
			usersWithFav.add(col.getOwnerId());
		}
		List<ObjectId> users = DB.getUserDAO().findIds();
		users.removeAll(usersWithFav);
		Collection favCollection = new Collection();
		favCollection.setTitle("_favorites");
		for (ObjectId userId : users) {
			favCollection.setCreated(new Date());
			favCollection.setOwnerId(userId);
			favCollection.setDbId(null);
			DB.getCollectionDAO().makePermanent(favCollection);
			System.out.println("Successfully created favorites for userId "
					+ userId.toString());

		}

	}
}
