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


package general.controllerTest;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.PUT;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;
import static play.test.Helpers.running;
import general.daoTests.UserDAOTest;
import general.daoTests.UserGroupDAOTest;

import org.bson.types.ObjectId;
import org.junit.Test;

import db.DB;

/**
 * The class <code>UserAndGroupManagerTest</code> contains tests for the class
 * {@link <code>UserAndGroupManager</code>}
 *
 * @pattern JUnit Test Case
 *
 * @author mariaral
 *
 * @version $Revision$
 */
public class UserAndGroupManagerTest {

	/**
	 * Run the Result addUserOrGroupToGroup(String, String) method test
	 */
	@Test
	public void testAddUserOrGroupToGroup() {
		running(fakeApplication(), new Runnable() {
			@Override
			public void run() {
				ObjectId user1 = UserDAOTest.createTestUser();
				ObjectId group1 = UserGroupDAOTest.createTestUserGroup(user1);
				ObjectId user2 = UserDAOTest.createTestUser();
				ObjectId group2 = UserGroupDAOTest.createTestUserGroup(user2);
				assertThat(DB.getUserGroupDAO().get(group1).getUsers().size())
						.isEqualTo(1);
				route(fakeRequest(
						PUT,
						"/group/addUserOrGroup/" + group1.toString() + "?id="
								+ user2.toString()).withSession("user",
						user1.toString()));
				assertThat(DB.getUserGroupDAO().get(group1).getUsers().size())
						.isEqualTo(2);
				route(fakeRequest(
						PUT,
						"/group/addUserOrGroup/" + group1.toString() + "?id="
								+ group2.toString()).withSession("user",
						user1.toString()));
				assertThat(DB.getUserGroupDAO().get(group1).getUsers().size())
						.isEqualTo(2);
				assertThat(
						DB.getUserGroupDAO().get(group2).getParentGroups()
								.size()).isEqualTo(1);
				assertThat(DB.getUserGroupDAO().get(group2).getParentGroups())
						.contains(group1);
				assertThat(DB.getUserDAO().get(user2).getUserGroupsIds())
						.contains(group1);
			}
		});

	}
}