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


package controllers;

import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.User;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class UserManager extends Controller {
	public static final ALogger log = Logger.of(UserManager.class);

	/**
	 * Free to call by anybody, so we don't give lots of info.
	 * 
	 * @param email
	 * @return
	 */
	public static Result findByEmail(String email) {
		User u = DB.getUserDAO().getByEmail(email);
		if (u != null) {
			ObjectNode res = Json.newObject();
			res.put("username", u.getUsername());
			res.put("email", u.getEmail());
			return ok(res);
		} else {
			return badRequest();
		}
	}

	public static Result findByUsername(String username) {
		User u = DB.getUserDAO().getByUsername(username);
		if (u != null) {
			return ok();
		} else {
			return badRequest();
		}
	}

	/**
	 * 
	 * @return
	 */
	private static ArrayNode proposeUsername(String initial, String firstName,
			String lastName) {
		ArrayNode names = Json.newObject().arrayNode();
		String proposedName;
		int i = 0;
		User u;
		do {
			proposedName = initial + i++;
			u = DB.getUserDAO().getByUsername(proposedName);
		} while (u != null);
		names.add(proposedName);
		if (firstName == null || lastName == null)
			return names;
		proposedName = firstName + "_" + lastName;
		i = 0;
		while (DB.getUserDAO().getByUsername(proposedName) != null) {
			proposedName = proposedName + i++;
		}
		names.add(proposedName);
		return names;
	}

	/**
	 * 
	 * @return
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result register() {

		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		ObjectNode error = (ObjectNode) Json.newObject();

		String email = null;
		if (json.has("email")) {
			email = json.get("email").asText();
			// Check if email is already used by another user
			if (DB.getUserDAO().getByEmail(email) != null) {
				error.put("email", "Email Address Already in Use");
			}
			// Validate email address with regular expression
			final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
					+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
			Pattern pattern = Pattern.compile(EMAIL_PATTERN);
			Matcher matcher = pattern.matcher(email);
			if (!matcher.matches()) {
				error.put("email", "Invalid Email Address");
			}
		} else {
			error.put("email", "Email Address is Empty");
		}
		String firstName = null;
		if (!json.has("firstName")) {
			error.put("firstName", "First Name is Empty");
		} else {
			firstName = json.get("firstName").asText();
		}
		String lastName = null;
		if (!json.has("lastName")) {
			error.put("lastName", "Last Name is Empty");
		} else {
			lastName = json.get("lastName").asText();
		}
		String password = null;
		if (!json.has("facebookId") && !json.has("googleId")) {
			if (!json.has("password")) {
				error.put("password", "Password is Empty");
			} else {
				password = json.get("password").asText();
				if (password.length() < 6) {
					error.put("password",
							"Password must contain more than 6 characters");
				}
			}
		}
		String username = null;
		// username unique
		if (!json.has("username")) {
			error.put("username", "Username is Empty");
		} else {
			username = json.get("username").asText();
			if (DB.getUserDAO().getByUsername(username) != null) {
				error.put("username", "Username Already in Use");
				ArrayNode names = proposeUsername(username, firstName, lastName);
				result.put("proposal", names);

			}
		}
		// If everything is ok store the user at the database
		if (error.size() != 0) {
			result.put("error", error);
			return badRequest(result);
		}
		User user = Json.fromJson(json, User.class);
		DB.getUserDAO().makePermanent(user);
		session().put("user", user.getDbId().toHexString());
		result = (ObjectNode) Json.parse(DB.getJson(user));
		result.remove("md5Password");
		return ok(result);

	}

	/**
	 * Should not be needed, is the same as login by email? Maybe need to store
	 * if its a facebook or google or password log inner
	 * 
	 * @return
	 */
	public static Result findByFacebookId() {
		return ok();
	}

	public static Result googleLogin(String accessToken) {
		log.info(accessToken);
		User u = null;
		try {
			HttpGet hg = new HttpGet(
					"https://www.googleapis.com/oauth2/v1/tokeninfo?access_token="
							+ accessToken);
			HttpClient client = HttpClientBuilder.create().build();
			HttpResponse response = client.execute(hg);
			InputStream is = response.getEntity().getContent();
			JsonNode res = Json.parse(is);
			String email = res.get("email").asText();
			u = DB.getUserDAO().getByEmail(email);
			if (u == null) {
				return badRequest(Json
						.parse("{\"error\":\"User not registered\""));
			}
			return ok(Json.parse(DB.getJson(u)));
		} catch (Exception e) {
			return badRequest(Json
					.parse("{\"error\":\"Couldn't validate user\""));
		}
	}

	public static Result facebookLogin(String accessToken) {
		log.info(accessToken);
		User u = null;
		try {
			HttpGet hg = new HttpGet(
					"https://graph.facebook.com/endpoint?access_token="
							+ accessToken);
			HttpClient client = HttpClientBuilder.create().build();
			HttpResponse response = client.execute(hg);
			InputStream is = response.getEntity().getContent();
			JsonNode res = Json.parse(is);
			String email = res.get("email").asText();
			u = DB.getUserDAO().getByEmail(email);
			if (u == null) {
				return badRequest(Json
						.parse("{\"error\":\"User not registered\""));
			}
			return ok(Json.parse(DB.getJson(u)));
		} catch (Exception e) {
			return badRequest(Json
					.parse("{\"error\":\"Couldn't validate user\""));
		}
	}

	public static Result login() {

		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		ObjectNode error = (ObjectNode) Json.newObject();

		User u = null;
		String emailOrUserName = null;
		if (json.has("email")) {
			emailOrUserName = json.get("email").asText();
			u = DB.getUserDAO().getByEmail(emailOrUserName);
			if (u == null) {
				u = DB.getUserDAO().getByUsername(emailOrUserName);
				if (u == null) {
					error.put("email", "Invalid Email Address or Username");
					result.put("error", error);
					return badRequest(result);
				}
			}
		} else {
			error.put("email", "Need Email or Username");
			result.put("error", error);
			return badRequest(result);
		}
		// check password
		String password = null;
		if (json.has("password")) {
			password = json.get("password").asText();
		} else {
			error.put("password", "Password is Empty");
			result.put("error", error);
			return badRequest(result);
		}
		if (u.checkPassword(password)) {
			session().put("user", u.getDbId().toHexString());
			// now return the whole user stuff, just for good measure
			result = (ObjectNode) Json.parse(DB.getJson(u));
			return ok(result);
		} else {
			error.put("password", "Invalid Password");
			result.put("error", error);
			return badRequest(result);
		}
	}

	/**
	 * This action clears the session, the user is logged out.
	 * 
	 * @return
	 */
	public static Result logout() {
		session().clear();
		return ok();
	}
}
