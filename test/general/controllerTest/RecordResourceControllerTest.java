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

import static play.test.Helpers.DELETE;
import static play.test.Helpers.GET;
import static play.test.Helpers.POST;
import static play.test.Helpers.PUT;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;
import static play.test.Helpers.running;
import general.daoTests.UserDAOTest;
import model.resources.RecordResource;

import org.bson.types.ObjectId;
import org.junit.Test;

import play.libs.Json;
import play.mvc.Result;

import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class RecordResourceControllerTest {
	public static long HOUR = 3600000;

	// @Test
	public void testCreateRecordResource() {
		running(fakeApplication(), new Runnable() {
			@Override
			public void run() {
				ObjectId user = UserDAOTest.createTestUser();
				ObjectNode json = Json.newObject();
				json.put("resourceType", "CulturalObject");
				Result result = route(fakeRequest(POST, "/resources")
						.withJsonBody(json)
						.withSession("user", user.toString()), HOUR);
				System.out.println(contentAsString(result));
			}
		});
	}

	@Test
	public void testGetRecordResource() {
		running(fakeApplication(), new Runnable() {
			@Override
			public void run() {
				ObjectId user = UserDAOTest.createTestUser();
				RecordResource recordResource = new RecordResource();
				recordResource.getAdministrative().setWithCreator(user);
				DB.getRecordResourceDAO().makePermanent(recordResource);
				Result result = route(
						fakeRequest(
								GET,
								"/resources/"
										+ recordResource.getDbId().toString())
								.withSession("user", user.toString()), HOUR);
				System.out.println(contentAsString(result));
			}
		});
	}

	// @Test
	public void testEditRecordResource() {
		running(fakeApplication(), new Runnable() {
			@Override
			public void run() {
				ObjectId user = UserDAOTest.createTestUser();
				RecordResource recordResource = new RecordResource();
				recordResource.getAdministrative().setWithCreator(user);
				DB.getRecordResourceDAO().makePermanent(recordResource);
				ObjectNode json = Json.newObject();
				Result result = route(
						fakeRequest(
								PUT,
								"/resources/"
										+ recordResource.getDbId().toString())
								.withJsonBody(json).withSession("user",
										user.toString()), HOUR);
				System.out.println(contentAsString(result));
			}
		});
	}

	@Test
	public void testDeleteRecordResource() {
		running(fakeApplication(), new Runnable() {
			@Override
			public void run() {
				ObjectId user = UserDAOTest.createTestUser();
				RecordResource recordResource = new RecordResource();
				recordResource.getAdministrative().setWithCreator(user);
				DB.getRecordResourceDAO().makePermanent(recordResource);
				Result result = route(
						fakeRequest(
								DELETE,
								"/resources/"
										+ recordResource.getDbId().toString())
								.withSession("user", user.toString()), HOUR);
				System.out.println(contentAsString(result));
			}
		});
	}
}
