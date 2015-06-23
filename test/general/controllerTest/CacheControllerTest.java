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
import static play.test.Helpers.GET;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;
import static play.test.Helpers.running;
import static play.test.Helpers.status;

import org.junit.Test;

import play.mvc.Http.Status;
import play.mvc.Result;

public class CacheControllerTest {

	public static long HOUR = 3600000;

	@Test
	public void testGetThumbnail() {
		running(fakeApplication(), new Runnable() {
			public void run() {
				String url = "";
				Result result = route(fakeRequest(GET, "/cache/byUrl?url="
						+ url));
				assertThat(status(result)).isEqualTo(Status.OK);
			}
		});
	}
}