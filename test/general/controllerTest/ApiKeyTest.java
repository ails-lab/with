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

import static akka.pattern.Patterns.ask;
import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;
import static play.test.Helpers.running;
import static play.test.Helpers.status;

import java.util.Date;
import java.util.List;

import model.ApiKey;
import model.ApiKey.CallLimit;

import org.junit.Test;

import play.libs.Akka;
import play.libs.Json;
import play.mvc.Result;
import play.test.Helpers;
import actors.ApiKeyManager;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;

import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class ApiKeyTest {
	public static long HOUR = 3600000;

	@Test
	public void testAuthorization() {

		final ObjectNode json = Json.newObject();
		json.put("searchTerm", "cars");
		json.put("page", 1 );
		json.put("pageSize", 20 );
		json.putArray("source" );
		
		running( fakeApplication(), new Runnable() {
			@Override
			public void run() {
				// create an ApiKey
				ApiKey k = new ApiKey();
				// should cover localhost
				k.setIpPattern("((0:){7}.*)|(127.0.0.1)");
				// set it to expired				
				k.setExpires(new Date(new Date().getTime()-HOUR));
				k.addCall(0, ".*");

				// store it
				DB.getApiKeyDAO().makePermanent(k);
				
				// read it into the Actor
				ActorSelection api = Akka.system().actorSelection("user/apiKeyManager"); 
				api.tell( new ApiKeyManager.Reset(), ActorRef.noSender());
				
				Result result = route(fakeRequest("POST", "/api/search")
						.withJsonBody(json)
						.withSession("user", "blabla"), HOUR);

			    assertThat(status(result)).isEqualTo(BAD_REQUEST);
			    assertThat( Helpers.contentAsString(result)).isEqualTo("EXPIRED_IP");
			    
			    k.setExpires(new Date(new Date().getTime()+1000*HOUR));
				DB.getApiKeyDAO().makePermanent(k);
				api.tell( new ApiKeyManager.Reset(), ActorRef.noSender());

				result = route(fakeRequest("POST", "/api/search")
						.withJsonBody(json)
						.withSession("user", "blabla"), HOUR);

			    assertThat(status(result)).isEqualTo(OK);
			    // assertThat( Helpers.contentAsString(result)).isEqualTo("EXPIRED_IP");
				
				ask( api, new ApiKeyManager.Store(), 2000 );
				ApiKey k2 = DB.getApiKeyDAO().get( k.getDbId());
				List<CallLimit> limits = k2.getLimits();
				assertThat( limits.get(0).counter ).isEqualTo(1); 
				
				// remove the ApiKeys from DB
				DB.getApiKeyDAO().delete(k2);
			}
		});

	}
}
