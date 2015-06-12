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
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;
import static play.test.Helpers.running;
import static play.test.Helpers.status;
import model.User;

import org.junit.Test;

import play.libs.Json;
import play.mvc.Result;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import db.DB;

public class ExhibitionControllerTest {

	public static long HOUR = 3600000;

	@Test
	public void testCreateExhibition() {

		// make a user with password
		if (DB.getUserDAO().getByUsername("testExhibition") == null) {
			User user = new User();
			user.setEmail("test@test.com");
			user.setUsername("testExhibition");
			// set password after email, email salts the password!
			user.setPassword("secret");
			DB.getUserDAO().makePermanent(user);
		}

		try {
			running(fakeApplication(), new Runnable() {
				public void run() {
					try {
						User user = DB.getUserDAO().getByUsername(
								"testExhibition");
						Result result = route(fakeRequest("POST",
								"/exhibition/create").withSession("user",
								user.getDbId().toHexString()));
						assertThat(status(result)).isEqualTo(OK);
						JsonParser parser = new JsonParser();
						Gson gson = new GsonBuilder().setPrettyPrinting()
								.create();
						JsonElement el = parser.parse(contentAsString(result));
						System.out.println(gson.toJson(el));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		} finally {
		}
	}

	@Test
	public void testEditExhibition() {
		try {
			running(fakeApplication(), new Runnable() {
				public void run() {
					try {
						ObjectNode json = Json.newObject();
						json.put("title", "A title");
						json.put("description", "A description");
						User user = DB.getUserDAO().getByUsername(
								"testExhibition");
						Result result = route(fakeRequest("POST",
								"/exhibition/create").withSession("user",
								user.getDbId().toHexString()));
						JsonNode ex = Json.parse(contentAsString(result));
						result = route(fakeRequest("PUT",
								"/exhibition/" + ex.get("dbId").asText())
								.withJsonBody(json).withSession("user",
										user.getDbId().toHexString()));
						JsonParser parser = new JsonParser();
						Gson gson = new GsonBuilder().setPrettyPrinting()
								.create();
						JsonElement el = parser.parse(contentAsString(result));
						System.out.println(gson.toJson(el));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		} finally {
		}
	}
}
