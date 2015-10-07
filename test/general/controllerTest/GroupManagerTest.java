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
import model.User;
import model.UserGroup;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import play.libs.Json;
import play.mvc.Result;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import db.DB;

/**
 * The class <code>GroupManagerTest</code> contains tests for the class {@link <code>GroupManager</code>}
 *
 * @pattern JUnit Test Case
 *
 * @generatedBy CodePro at 9/23/15 12:07 PM
 *
 * @author mariaral
 *
 * @version $Revision$
 */
public class GroupManagerTest {

	/**
	 * Run the Result createGroup(String, String, String) method test
	 */
	@Test
	public void testCreateGroup() {

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
				json.put("username", "testGroup" +Math.random());
				json.put("description", "This is a test group");
				page.put("address", "Hrwwn Polutexneiou 2, Zwgrafou");
				page.put("city", "Athens");
				page.put("country", "Greece");
				json.put("page", page);
				Result result = route(fakeRequest("POST",
						"/organization/create").withJsonBody(json).withSession(
						"user", user.getDbId().toString()));
				// Create Project
				System.out.println(contentAsString(result));
				json.put("username", "testGroup" + Math.random());
				json.put("description", "This is a test group");
				page.put("address", "Hrwwn Polutexneiou 2, Zwgrafou");
				page.put("city", "Athens");
				page.put("country", "Greece");
				json.put("page", page);
				result = route(fakeRequest("POST", "/project/create")
						.withJsonBody(json).withSession("user",
								user.getDbId().toString()));
				DB.getUserDAO().makeTransient(user);
				System.out.println(contentAsString(result));
				//JsonElement el = parser.parse(contentAsString(result));
				//System.out.println(gson.toJson(el));
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
	//@Test
	public void testDeleteGroup() {
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
	//@Test
	public void testEditGroup() {
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

	@Test
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
				assertThat(res.size()).isEqualTo(0);
				UserGroupDAOTest.createChildGroup(group1);
				result = route(fakeRequest(GET,
						"/group/descendantGroups/" + group1.toString()));
				res = parser.parse(contentAsString(result))
						.getAsJsonArray();
				assertThat(res.size()).isEqualTo(1);
				UserGroupDAOTest.createChildGroup(group1);
				result = route(fakeRequest(GET,
						"/group/descendantGroups/" + group1.toString()));
				res = parser.parse(contentAsString(result))
						.getAsJsonArray();
				System.out.println(contentAsString(result));
				assertThat(res.size()).isEqualTo(2);
			}
		});
	}

}