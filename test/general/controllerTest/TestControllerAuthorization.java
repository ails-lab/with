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
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.callAction;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.running;
import static play.test.Helpers.status;

import org.junit.Test;

import play.libs.Json;
import play.mvc.Result;
import play.test.FakeRequest;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestControllerAuthorization {

	@Test
	public void testAuthorization() {

		final ObjectNode json = Json.newObject();
		json.put("description", "TEst controller");
		json.put("title", "The test title");
		json.put("public", "true");
		json.put("ownerMail", "heres42@mongo.gr");
		running( fakeApplication(), new Runnable() {
			@Override
			public void run() {
				Result result = callAction(
						controllers.routes.ref
						.CollectionController.createCollection()
						, new FakeRequest("POST", "/collection/add")
						.withJsonBody(json)
						.withSession("user", "blabla"));

			    assertThat(status(result)).isEqualTo(OK);
			}
		});

	    }
	@Test
	public void testNoAuthorization() {
		running( fakeApplication(), new Runnable() {
			@Override
			public void run() {
				Result result = callAction(
						controllers.routes.ref
						.CollectionController.createCollection()
						, new FakeRequest("POST", "/collection/add"));

				System.out.println();
				assertThat(status(result)).isEqualTo(BAD_REQUEST);
			}
		});
	}
}
