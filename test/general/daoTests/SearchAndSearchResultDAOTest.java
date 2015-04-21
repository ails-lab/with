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

import model.Search;
import model.SearchResult;
import model.User;

import org.bson.types.ObjectId;
import org.junit.Test;
import org.mongodb.morphia.Key;

import db.DB;

public class SearchAndSearchResultDAOTest {


	@Test
	public void storeSearchesWithSearchResults() {

		for(int i=0;i<1000000;i++) {
			Search search = new Search();
			search.setQuery("SELECT * FROM europeana;");
			search.setSearchDate(TestUtils.randomDate("2014", "2015"));

			List<Key<User>> userKeys = DB.getUserDAO().find().asKeyList();
			assertThat(userKeys.size()).isEqualTo(1000);
			// change to i%(TestUtils.r.nextInt(999)) for a not normalized distribution of Searches to the Users
			ObjectId userId = new ObjectId(userKeys.get(i%((TestUtils.r.nextInt(999)+1))).getId().toString());
			search.setUser(DB.getUserDAO().getById(userId).getDbId());

			Key<Search> searchId = DB.getSearchDAO().makePermanent(search);
			assertThat(searchId).isNotNull();

			User user = DB.getUserDAO().getById(userId);
			if(user.getSearchHistory().size() < 20)
				user.getSearchHistory().add(search);
			DB.getUserDAO().makePermanent(user);


			SearchResult result = new SearchResult();
			result.setSearch(new ObjectId(searchId.getId().toString()));
			result.setOffset(i);

			Key<SearchResult> sresultId = DB.getSearchResultDAO().makePermanent(result);
			assertThat(sresultId).isNotNull();
		}
	}


}
