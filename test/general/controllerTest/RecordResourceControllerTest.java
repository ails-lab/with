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
import static play.test.Helpers.POST;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;
import static play.test.Helpers.running;

import java.util.List;

import general.daoTests.UserDAOTest;

import org.bson.types.ObjectId;
import org.junit.Test;

import play.libs.Json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.mvc.Result;

public class RecordResourceControllerTest {
	public static long HOUR = 3600000;

	@Test
	public void testCreateWithResource() {
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

}
