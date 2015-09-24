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

// all test should use those
import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.GET;
import static play.test.Helpers.callAction;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;
import static play.test.Helpers.running;
import static play.test.Helpers.session;
import static play.test.Helpers.status;

import java.io.IOException;

import model.User;
import model.UserGroup;

import org.junit.Assert;
import org.junit.Test;

import play.libs.Json;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import db.DB;

public class UserManagerTest {

	public static long HOUR = 3600000;

	// @Test
	public void testLogout() {
		running(fakeApplication(), new Runnable() {
			public void run() {
				Result result = route(fakeRequest(GET, "/api/logout"));
				assertThat(status(result)).isEqualTo(Status.OK);

				assertThat(session(result).isEmpty()).overridingErrorMessage(
						"Session after logout not empty").isTrue();
			}
		});
	}

	// @Test
	public void testLogin() {

		// make a user with password
		User u = new User();
		u.setEmail("my@you.me");
		u.setUserName("cool_url");
		// set password after email, email salts the password!
		u.setPassword("secret");
		DB.getUserDAO().makePermanent(u);

		try {
			running(fakeApplication(), new Runnable() {
				public void run() {
					Result result = route(fakeRequest(GET, "/api/login"), HOUR);
					assertThat(status(result)).isEqualTo(Status.BAD_REQUEST);

					result = route(
							fakeRequest(GET, "/api/login?password=secret"),
							HOUR);
					assertThat(status(result)).isEqualTo(Status.BAD_REQUEST);
					JsonNode j = Json.parse(contentAsString(result));
					assertThat(j.has("error")).isTrue();

					result = route(
							fakeRequest(GET,
									"/api/login?password=secret&email=my@you.me"),
							HOUR);
					assertThat(status(result)).isEqualTo(Status.OK);
					assertThat(session(result).isEmpty()).isFalse();
					assertThat(session(result).get("user")).isNotEmpty();

					j = Json.parse(contentAsString(result));
					assertThat(j.get("username").asText())
							.isEqualTo("cool_url");
				}
			});
		} finally {
			DB.getUserDAO().makeTransient(u);
		}
	}

	// @Test
	public void testGetByUsername() {

		// make a user with password
		User u = new User();
		u.setEmail("my@you.me");
		u.setUserName("cool_url");
		// set password after email, email salts the password!
		u.setPassword("secret");
		DB.getUserDAO().makePermanent(u);

		try {
			running(fakeApplication(), new Runnable() {
				public void run() {
					Result result = route(fakeRequest(GET, "/user/byEmail"),
							HOUR);
					System.out.println(contentAsString(result));

					assertThat(status(result)).isEqualTo(Status.BAD_REQUEST);

					result = route(
							fakeRequest(GET, "/user/byUsername?username=uncool"),
							HOUR);
					assertThat(status(result)).isEqualTo(Status.BAD_REQUEST);

					result = route(
							fakeRequest(GET,
									"/user/byUsername?username=cool_url"), HOUR);
					assertThat(status(result)).isEqualTo(Status.OK);
				}
			});
		} finally {
			DB.getUserDAO().makeTransient(u);
		}
	}

	@Test
	public void testRegister() {
		try {
			running(fakeApplication(), new Runnable() {
				public void run() {

					ObjectMapper mapper = new ObjectMapper();
					ObjectNode json;
					try {
						// Test when everything is ok
						json = (ObjectNode) mapper
								.readTree("{"
										+ "\"email\": \"test@test.eu\","
										+ "\"firstName\" : \"first\","
										+ "\"lastName\" : \"last\","
										+ "\"password\" : \"pwd123\","
										+ "\"page\" : {\"address\" : \"Athens\","
										+ "\"coordinates\" : { \"latitude\" : 2.3, \"longitude\" : 4.2 } },"
										+ "\"username\" : \"user\"}");
						Result result = callAction(
								controllers.routes.ref.UserManager.register(),
								new FakeRequest("POST", "/user/register")
										.withJsonBody(json));
						assertThat(status(result)).isEqualTo(Status.OK);
						User u = DB.getUserDAO().getByUsername("user");
						System.out.println(DB.getJson(u));
						assertThat(u.getEmail().equals("test@test.eu"));
						DB.getUserDAO().makeTransient(u);

						// Test for invalid Email Addresses

						// Valid Email Address
						json.put("email", "test+100@gmail.com");
						result = callAction(controllers.routes.ref.UserManager
								.register(), new FakeRequest("POST",
								"/user/register").withJsonBody(json));
						System.out.println(contentAsString(result));
						assertThat(status(result)).isEqualTo(Status.OK);
						u = DB.getUserDAO().getByUsername("user");
						DB.getUserDAO().makeTransient(u);

						// Invalid Email Addresses
						json.put("email", "wrongemail");
						result = callAction(controllers.routes.ref.UserManager
								.register(), new FakeRequest("POST",
								"/user/register").withJsonBody(json));
						assertThat(contentAsString(result)).contains(
								"Invalid Email Address");

						json.put("email", "test@.com.my");
						result = callAction(controllers.routes.ref.UserManager
								.register(), new FakeRequest("POST",
								"/user/register").withJsonBody(json));
						assertThat(status(result))
								.isEqualTo(Status.BAD_REQUEST);
						assertThat(contentAsString(result)).contains(
								"Invalid Email Address");

						json.put("email", "test@%*.com");
						result = callAction(controllers.routes.ref.UserManager
								.register(), new FakeRequest("POST",
								"/user/register").withJsonBody(json));
						assertThat(status(result))
								.isEqualTo(Status.BAD_REQUEST);
						assertThat(contentAsString(result)).contains(
								"Invalid Email Address");

						// Test for already used email and username
						json.put("email", "test@test.eu");
						result = callAction(controllers.routes.ref.UserManager
								.register(), new FakeRequest("POST",
								"/user/register").withJsonBody(json));
						assertThat(status(result)).isEqualTo(Status.OK);

						json.put("email", "test@test.eu");
						result = callAction(controllers.routes.ref.UserManager
								.register(), new FakeRequest("POST",
								"/user/register").withJsonBody(json));
						assertThat(status(result))
								.isEqualTo(Status.BAD_REQUEST);
						assertThat(contentAsString(result)).contains(
								"Email Address Already in Use");
						assertThat(contentAsString(result)).contains(
								"Username Already in Use");

						// Test displayName proposal
						assertThat(contentAsString(result).contains("proposal"));
						assertThat(contentAsString(result).contains("user0"));
						assertThat(contentAsString(result).contains(
								"first_last"));
						u = DB.getUserDAO().getByUsername("user");
						DB.getUserDAO().makeTransient(u);

						json.put("password", "pwd");
						result = callAction(controllers.routes.ref.UserManager
								.register(), new FakeRequest("POST",
								"/user/register").withJsonBody(json));
						assertThat(status(result))
								.isEqualTo(Status.BAD_REQUEST);
						assertThat(contentAsString(result)).contains(
								"Password must contain more than 6 characters");

						// Test for empty fields
						json.removeAll();
						result = callAction(controllers.routes.ref.UserManager
								.register(), new FakeRequest("POST",
								"/user/register").withJsonBody(json));
						assertThat(status(result))
								.isEqualTo(Status.BAD_REQUEST);
						assertThat(contentAsString(result)).contains(
								"Email Address is Empty");
						assertThat(contentAsString(result)).contains(
								"First Name is Empty");
						assertThat(contentAsString(result)).contains(
								"Password is Empty");
						assertThat(contentAsString(result)).contains(
								"Email Address is Empty");
						assertThat(contentAsString(result)).contains(
								"Username is Empty");

					} catch (JsonProcessingException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
		} finally {
			User u = DB.getUserDAO().getByUsername("user");
			if (u != null) {
				DB.getUserDAO().makeTransient(u);
			}
		}
	}

	// @Test
	public void testAddUserToGroup() {
		User user = new User();
		user.setEmail("test@controller.gr");
		user.setUserName("controller");
		DB.getUserDAO().makePermanent(user);

		UserGroup parentGroup = new UserGroup();
		DB.getUserGroupDAO().makePermanent(parentGroup);

		UserGroup parentGroup1 = new UserGroup();
		parentGroup1.getParentGroups().add(parentGroup.getDbId());
		DB.getUserGroupDAO().makePermanent(parentGroup1);

		running(fakeApplication(), new Runnable() {
			@Override
			public void run() {
				Result result = route(fakeRequest("GET", "/user/addToGroup/"
						+ user.getDbId() + "?gid=" + parentGroup1.getDbId()));

				JsonParser parser = new JsonParser();
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				JsonElement el = parser.parse(contentAsString(result));
				System.out.println(gson.toJson(el));

				if (status(result) == 200)
					assertThat(status(result)).isEqualTo(OK);
				else {
					System.out.println(status(result));
					Assert.fail();
				}

			}
		});
	}
}
