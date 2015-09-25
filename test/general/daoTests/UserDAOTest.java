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
import java.util.Random;

import model.User;

import org.bson.types.ObjectId;
import org.junit.Test;

import db.DB;

public class UserDAOTest {

	// @Test
	public void testCRUD() {
		// create
		User testUser = new User();
		testUser.setEmail("test@ntua.gr");
		testUser.setPassword("secret");
		DB.getUserDAO().makePermanent(testUser);

		ObjectId id = testUser.getDbId();

		// find by Email
		User a = DB.getUserDAO().findOne("email", "test@ntua.gr");
		assertThat(a).isNotNull().overridingErrorMessage(
				"Test user not found after store.");

		// find with email and password
		User b = DB.getUserDAO().getByEmailPassword("test@ntua.gr", "wrong");
		assertThat(b).overridingErrorMessage(
				"User falsly retrieved with wrong password").isNull();
		b = DB.getUserDAO().getByEmailPassword("test@ntua.gr", "secret");
		assertThat(b).overridingErrorMessage(
				"User with password not retreived.").isNotNull();

		// update a user
		b.setFirstName("Bert");
		b.setLastName("Testuser");
		DB.getUserDAO().makePermanent(b);

		// check its correct in db
		User c = DB.getUserDAO().get(id);
		assertThat(c.getLastName()).isEqualTo("Testuser");

		// remove from db
		DB.getUserDAO().makeTransient(c);

		// check its gone
		User d = DB.getUserDAO().get(id);
		assertThat(d).overridingErrorMessage("User not deleted!").isNull();

	}

	// @Test
	public void massStorage() {
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
			if (i == 42) {
				testUser.setPassword("helloworld");
			} else {
				testUser.setPassword(TestUtils.randomString());
			}

			// search history
			/*
			 * if( i==42 ) { List<Search> searchHistory = new
			 * ArrayList<Search>(); for (int j = 0; j < 1000; j++) { Search s1 =
			 * new Search(); s1.setSearchDate(TestUtils.randomDate());
			 * DB.getSearchDAO().makePermanent(s1); testUser.addToHistory(s1);
			 * searchHistory.add(s1); } //
			 * testUser.setSearchHistory(searchHistory); }
			 */
			testUser.setUsername("Testuser");
			DB.getUserDAO().makePermanent(testUser);
		}

		List<User> l = DB.getUserDAO().find().asList();
		assertThat(l.size()).isGreaterThanOrEqualTo(1000);

		// get User 42
		User x = DB.getUserDAO().getByEmail("heres42@mongo.gr");
		assertThat(x).isNotNull();

		// mass delete
		/*
		 * int res = DB.getUserDAO().removeAll("lastName='Testuser'");
		 * assertThat( res )
		 * .overridingErrorMessage("Not enough Testusers deleted.")
		 * .isGreaterThanOrEqualTo(1000);
		 */
	}

	public static ObjectId createTestUser() {
		User testUser = new User();
		testUser.setUsername("TestUser" + TestUtils.randomString());
		testUser.setEmail(TestUtils.randomString() + "@mail.com");
		DB.getUserDAO().makePermanent(testUser);
		return testUser.getDbId();

	}

}
