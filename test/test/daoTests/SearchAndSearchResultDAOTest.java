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

import model.RecordLink;
import model.Search;
import model.SearchResult;
import model.User;

import org.bson.types.ObjectId;
import org.junit.Test;
import org.mongodb.morphia.Key;

import test.TestUtils;
import db.DB;

public class SearchAndSearchResultDAOTest {


	@Test
	public void storeSearchesWithSearchResults() {

		for(int i=0;i<10000;i++) {
			Search search = new Search();
			search.setQuery("SELECT * FROM europeana;");
			search.setSearchDate(TestUtils.randomDate("2014", "2015"));

			List<Key<User>> userKeys = DB.getUserDAO().find().asKeyList();
			search.setUser(DB.getUserDAO().getById(new ObjectId(userKeys.get(i%999).getId().toString())).getDbId());

			Key<Search> searchId = DB.getSearchDAO().makePermanent(search);

			SearchResult result = new SearchResult();
			result.setSearch(new ObjectId(searchId.getId().toString()));
			result.setOffset(i);

			List<Key<RecordLink>> rlinkKeys = DB.getRecordLinkDAO().find().asKeyList();
			result.setRecordLink(DB.getRecordLinkDAO().getByDbId(new ObjectId(rlinkKeys.get(i%1000).getId().toString())));

			DB.getSearchResultDAO().makePermanent(result);
		}
	}


}
