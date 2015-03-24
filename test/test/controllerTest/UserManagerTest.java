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


package test.controllerTest;

// all test should use those
import static org.fest.assertions.Assertions.assertThat;
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
import java.util.List;

import model.User;

import org.junit.Test;

import play.libs.F.Promise;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
		u.setDisplayName("cool_url");
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
					assertThat(j.get("displayName").asText()).isEqualTo(
							"cool_url");
				}
			});
		} finally {
			DB.getUserDAO().makeTransient(u);
		}
	}

	// @Test
	public void testGetByDisplayName() {

		// make a user with password
		User u = new User();
		u.setEmail("my@you.me");
		u.setDisplayName("cool_url");
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
							fakeRequest(GET,
									"/user/byDisplayName?displayName=uncool"),
							HOUR);
					assertThat(status(result)).isEqualTo(Status.BAD_REQUEST);

					result = route(
							fakeRequest(GET,
									"/user/byDisplayName?displayName=cool_url"),
							HOUR);
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
						json = (ObjectNode) mapper.readTree("{"
								+ "\"email\": \"test@test.eu\","
								+ "\"firstName\" : \"first\","
								+ "\"lastName\" : \"last\","
								+ "\"password\" : \"pwd\","
								+ "\"username\" : \"user\"" + "}");
						Result result = callAction(
								controllers.routes.ref.UserManager.register(),
								new FakeRequest("POST", "/user/register")
										.withJsonBody(json));
						System.out.println(contentAsString(result));
						assertThat(status(result)).isEqualTo(Status.OK);
						User u = DB.getUserDAO().getByDisplayName("user");
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
						u = DB.getUserDAO().getByDisplayName("user");
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

						// Test for already used email and displayName
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
						u = DB.getUserDAO().getByDisplayName("user");
						DB.getUserDAO().makeTransient(u);

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
			User u = DB.getUserDAO().getByDisplayName("user");
			if (u != null) {
				DB.getUserDAO().makeTransient(u);
			}
		}
	}
}
