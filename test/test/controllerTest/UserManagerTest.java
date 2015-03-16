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
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;
import static play.test.Helpers.running;
import static play.test.Helpers.session;
import static play.test.Helpers.status;
import model.User;

import org.junit.Test;

import play.libs.Json;
import play.mvc.Http.Status;
import play.mvc.Result;

import com.fasterxml.jackson.databind.JsonNode;

import db.DB;

public class UserManagerTest {
	
	public static long HOUR = 3600000;
	// @Test
	public void testLogout() {
		running( fakeApplication(), new Runnable() {
			public void run() {
				Result result = route(fakeRequest(GET, "/api/logout"));
				assertThat( status( result )).isEqualTo( Status.OK );
				
				assertThat( session( result).isEmpty())
					.overridingErrorMessage( "Session after logout not empty")
					.isTrue();
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
		running( fakeApplication(), new Runnable() {
			public void run() {
				Result result = route(fakeRequest(GET, "/api/login"),HOUR );
				assertThat( status( result )).isEqualTo( Status.BAD_REQUEST );
				
				result = route(fakeRequest(GET, "/api/login?password=secret"), HOUR );
				assertThat( status( result )).isEqualTo( Status.BAD_REQUEST );
				JsonNode j = Json.parse( contentAsString( result ));
				assertThat(j.has("error")).isTrue();
				
				result = route(fakeRequest(GET, "/api/login?password=secret&email=my@you.me"), HOUR );
				assertThat( status( result )).isEqualTo( Status.OK );
				assertThat( session( result ).isEmpty()).isFalse();
				assertThat( session( result ).get("user")).isNotEmpty();
				
				j = Json.parse( contentAsString( result ));
				assertThat(j.get("displayName").asText()).isEqualTo("cool_url");
			}
		});
		} finally {
			DB.getUserDAO().makeTransient(u);
		}
	}
	
	@Test
	public void testGetByDisplayName() {
		
		// make a user with password
		User u = new User();
		u.setEmail("my@you.me");
		u.setDisplayName("cool_url");
		// set password after email, email salts the password!
		u.setPassword("secret");
		DB.getUserDAO().makePermanent(u);
		
		try {
		running( fakeApplication(), new Runnable() {
			public void run() {
				Result result = route(fakeRequest(GET, "/user/byEmail"),HOUR );
				System.out.println( contentAsString( result ));
				
				assertThat( status( result )).isEqualTo( Status.BAD_REQUEST );
				
				result = route(fakeRequest(GET, "/user/byDisplayName?displayName=uncool"), HOUR );
				assertThat( status( result )).isEqualTo( Status.BAD_REQUEST );
				
				result = route(fakeRequest(GET, "/user/byDisplayName?displayName=cool_url"), HOUR );
				assertThat( status( result )).isEqualTo( Status.OK );
			}
		});
		} finally {
			DB.getUserDAO().makeTransient(u);
		}
	}

}
