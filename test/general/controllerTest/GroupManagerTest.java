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
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.GET;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;
import static play.test.Helpers.running;
import static play.test.Helpers.status;
import general.TestUtils;
import general.daoTests.UserDAOTest;
import general.daoTests.UserGroupDAOTest;
import junit.framework.TestCase;
import model.User;
import model.UserGroup;

import org.bson.types.ObjectId;
import org.junit.Assert;

import play.libs.Json;
import play.mvc.Result;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import controllers.GroupManager;
import db.DB;

/**
<<<<<<< HEAD
 * The class <code>GroupManagerTest</code> contains tests for the class {@link <code>GroupManager</code>}
=======
 * The class <code>GroupManagerTest</code> contains tests for the class {@link
 * <code>GroupManager</code>}
>>>>>>> Save organizations and projects to database using userGroupDAO
 *
 * @pattern JUnit Test Case
 *
 * @generatedBy CodePro at 9/23/15 12:07 PM
 *
 * @author mariaral
 *
 * @version $Revision$
 */
public class GroupManagerTest extends TestCase {

	/**
	 * Construct new test instance
	 *
<<<<<<< HEAD
	 * @param name
	 *            the test name
=======
	 * @param name the test name
>>>>>>> Save organizations and projects to database using userGroupDAO
	 */
	public GroupManagerTest(String name) {
		super(name);
	}

	/**
	 * Run the Result addUserToGroup(String, String) method test
	 */
	public void addUserToGroup() {
		fail("Newly generated method - fix or disable");
		// add test code here
		String userId = null;
		String groupId = null;
		Result result = GroupManager.addUserToGroup(userId, groupId);
		assertTrue(false);
	}

	/**
	 * Run the Result createGroup(String, String, String) method test
	 */
	public void createGroup() {

		running(fakeApplication(), new Runnable() {
			@Override
			public void run() {
				final ObjectNode json = Json.newObject();
				final ObjectNode page = Json.newObject();
				User user = new User();
				user.setEmail("testuser@test.gr");
				user.setFirstName("FirstName");
				user.setLastName("LastName");
				user.setUsername("usrname");
				DB.getUserDAO().makePermanent(user);
				// Create Organization
				json.put("username", "testGroup" + TestUtils.randomString());
				json.put("description", "This is a test group");
				page.put("address", "Hrwwn Polutexneiou 2, Zwgrafou");
				page.put("city", "Athens");
				page.put("country", "Greece");
				json.put("page", page);
				Result result = route(fakeRequest("POST",
						"/organization/create").withJsonBody(json).withSession(
						"user", user.getDbId().toString()));
				// Create Project
				json.put("username", "testGroup" + TestUtils.randomString());
				json.put("description", "This is a test group");
				page.put("address", "Hrwwn Polutexneiou 2, Zwgrafou");
				page.put("city", "Athens");
				page.put("country", "Greece");
				json.put("page", page);
				result = route(fakeRequest("POST", "/project/create")
						.withJsonBody(json).withSession("user",
								user.getDbId().toString()));
				DB.getUserDAO().makeTransient(user);
				JsonParser parser = new JsonParser();
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				System.out.println(contentAsString(result));
				// JsonElement el = parser.parse(contentAsString(result));
				// System.out.println(gson.toJson(el));
				if (status(result) == 200)
					assertThat(status(result)).isEqualTo(OK);
				else {
					System.out.println(status(result));
					Assert.fail();
				}
			}
		});
	}

	/**
	 * Run the Result deleteGroup(String) method test
	 */
	public void deleteGroup() {
		fail("Newly generated method - fix or disable");
		// add test code here
		String groupId = null;
		Result result = GroupManager.deleteGroup(groupId);
		assertTrue(false);
		UserGroup parentGroup = new UserGroup();
		DB.getUserGroupDAO().makePermanent(parentGroup);
		running(fakeApplication(), new Runnable() {
			@Override
			public void run() {
				Result result = route(fakeRequest("DELETE", "/group/"
						+ parentGroup.getDbId()));
				assertThat(status(result)).isEqualTo(OK);

				JsonParser parser = new JsonParser();
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				JsonElement el = parser.parse(contentAsString(result));
				System.out.println(gson.toJson(el));
			}
		});

	}

	/**
	 * Run the Result editGroup(String) method test
	 */
	public void editGroup() {
		running(fakeApplication(), new Runnable() {
			@Override
			public void run() {
				final ObjectNode json = Json.newObject();
				final ObjectNode page = Json.newObject();
				ObjectId userId = UserDAOTest.createTestUser();
				ObjectId orgId = UserGroupDAOTest
						.createTestOrganization(userId);
				json.put("username", "testGroup" + TestUtils.randomString()
						+ TestUtils.randomString() + TestUtils.randomString());
				json.put("description", "This is a test group");
				page.put("address", "Hrwwn Polutexneiou 2, Zwgrafou");
				page.put("city", "Athens");
				page.put("country", "Greece");
				json.put("page", page);
				Result result = route(fakeRequest("PUT",
						"/group/" + orgId.toString()).withJsonBody(json)
						.withSession("user", userId.toString()));
				System.out.println(contentAsString(result));
			}
		});
	}

	public void testGetDescendantGroups() {
		running(fakeApplication(), new Runnable() {
			@Override
			public void run() {
				// Make test groups
				ObjectId userId = UserDAOTest.createTestUser();
				ObjectId group1 = UserGroupDAOTest.createTestUserGroup(userId);
				Result result = route(fakeRequest(GET,
						"/group/descendantGroups/" + group1.toString()));
				JsonParser parser = new JsonParser();
				JsonArray res = parser.parse(contentAsString(result))
						.getAsJsonArray();
				assertEquals(0, res.size());
				UserGroupDAOTest.createChildGroup(group1);
				result = route(fakeRequest(GET,
						"/group/descendantGroups/" + group1.toString()));
				res = parser.parse(contentAsString(result))
						.getAsJsonArray();
				assertEquals(1, res.size());
				UserGroupDAOTest.createChildGroup(group1);
				result = route(fakeRequest(GET,
						"/group/descendantGroups/" + group1.toString()));
				res = parser.parse(contentAsString(result))
						.getAsJsonArray();
				assertEquals(2, res.size());
			}
		});
	}

	/**
	 * Run the Result findByGroupName(String, String) method test
	 */
	public void testFindByGroupName() {
		fail("Newly generated method - fix or disable");
		// add test code here
		String name = null;
		String collectionId = null;
		Result result = GroupManager.findByGroupName(name, collectionId);
		assertTrue(false);
	}

	/**
	 * Run the Result getGroup(String) method test
	 */
	public void testGetGroup() {
		fail("Newly generated method - fix or disable");
		// add test code here
		String groupId = null;
		Result result = GroupManager.getGroup(groupId);
		assertTrue(false);
	}

	/**
	 * Run the Result removeUserFromGroup(String, String) method test
	 */
	public void testRemoveUserFromGroup() {
		fail("Newly generated method - fix or disable");
		// add test code here
		String userId = null;
		String groupId = null;
		Result result = GroupManager.removeUserFromGroup(userId, groupId);
		assertTrue(false);
	}

}