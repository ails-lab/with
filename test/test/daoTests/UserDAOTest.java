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

import static org.fest.assertions.Assertions.assertThat;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import model.CollectionMetadata;
import model.Search;
import model.User;

import org.junit.Test;

import test.TestUtils;

import com.mongodb.MongoException;

import db.DB;

public class UserDAOTest {
	@Test
	public void testUserDAO() {
		User user1 = DB.getUserDAO().getByEmail("heres42@mongo.gr");
		User user3 = DB.getUserDAO().getByEmailPassword("heres42@mongo.gr", "helloworld");
		// List<Search> searchList = DB.getUserDAO().getSearchResults("man42");
		System.out.println(user1.toString());
	}

	@Test
	public void userStorage(CollectionMetadata md) {
		/* Add 1000 random users */
		for (int i = 0; i < 1000; i++) {
			User testUser = new User();
			if (i == 42) {
				// email
				testUser.setEmail("heres42@mongo.gr");
			} else {
				// email
				testUser.setEmail(TestUtils.randomString() + "@mongo.gr");
			}
			// set an MD5 password
			MessageDigest digest = TestUtils.getMD5Digest();
			if (i == 42) {
				digest.update("helloworld".getBytes());
				testUser.setMd5Password(digest.digest().toString());
			} else {
				digest.update(TestUtils.randomString().getBytes());
				testUser.setMd5Password(digest.digest().toString());
			}
			// search history
			List<Search> searchHistory = new ArrayList<Search>();
			for (int j = 0; j < 1000; j++) {
				Search s1 = new Search();
				s1.setSearchDate(TestUtils.randomDate());
				searchHistory.add(s1);
				testUser.setSearchHistory(searchHistory);
			}
			if (testUser != null)
				try {
					DB.getUserDAO().makePermanent(testUser);
				} catch (MongoException e) {
					System.out.println("mongo exception");
				}
		}

		List<User> l = DB.getUserDAO().find().asList();
		assertThat(l.size()).isGreaterThanOrEqualTo(1);

		// int count = DB.getUserDAO().removeAll("obj.name='Tester'" );
		// assertThat( count )
		// .overridingErrorMessage("Not removed enough Testers")
		// .isGreaterThanOrEqualTo(1 );
	}

}
