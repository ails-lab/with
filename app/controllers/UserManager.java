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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.User;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

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
			res.put("displayName", u.getDisplayName());
			res.put("email", u.getEmail());
			return ok(res);
		} else {
			return badRequest();
		}
	}

	public static Result findByDisplayName(String displayName) {
		User u = DB.getUserDAO().getByDisplayName(displayName);
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
	@BodyParser.Of(BodyParser.Json.class)
	public static Result register() {

		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		ArrayNode error = Json.newObject().arrayNode();

		String email = null;
		if (json.has("email")) {
			email = json.get("email").asText();
			// Check if email is already used by another user
			if (DB.getUserDAO().getByEmail(email) != null) {
				error.add("Email Address Already in Use");
			}
			// Validate email address with regular expression
			final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
					+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
			Pattern pattern = Pattern.compile(EMAIL_PATTERN);
			Matcher matcher = pattern.matcher(email);
			if (!matcher.matches()) {
				error.add("Invalid Email Address");
			}
		} else {
			error.add("Email Address is Empty");
		}
		String firstName = null;
		if (!json.has("firstName")) {
			error.add("First Name is Empty");
		} else {
			firstName = json.get("firstName").asText();
		}
		String lastName = null;
		if (!json.has("lastName")) {
			error.add("Last Name is Empty");
		} else {
			lastName = json.get("lastName").asText();
		}
		String password = null;
		// TODO: ask for password validation
		if (!json.has("password")) {
			error.add("Password is Empty");
		} else {
			password = json.get("password").asText();
		}
		String displayName = null;
		// displayName unique
		if (!json.has("displayName")) {
			error.add("Display Name is Empty");
		} else {
			displayName = json.get("displayName").asText();
			if (DB.getUserDAO().getByDisplayName(displayName) != null) {
				error.add("Display Name Already in Use");
			}
		}
		// If everything is ok store the user at the database
		if (error.size() != 0) {
			result.put("error", error);
			return badRequest(result);
		}
		User user = new User();
		user.setEmail(email);
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setDisplayName(displayName);
		user.setPassword(password);
		DB.getUserDAO().makePermanent(user);
		session().put("user", user.getDbId().toHexString());
		return ok();

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

	public static Result login(String email, String password, String displayName) {
		User u = null;
		ObjectNode result = Json.newObject();

		if (StringUtils.isNotEmpty(email)) {
			u = DB.getUserDAO().getByEmail(email);
			if (u == null) {
				result.put("error", "Invalid email");
			}
		} else if (StringUtils.isNotEmpty(displayName)) {
			u = DB.getUserDAO().getByDisplayName(displayName);
			if (u == null) {
				result.put("error", "Invalid displayName");
			}
		}

		if (u != null) {
			// check password
			if (u.checkPassword(password)) {
				session().put("user", u.getDbId().toHexString());
				// now return the whole user stuff, just for good measure
				result = (ObjectNode) Json.parse(DB.getJson(u));
				return ok(result);
			} else {
				result.put("error", "Invalid Password");
			}
		} else {
			result.put("error", "Need 'displayName' or 'email' parameter");
		}
		return badRequest(result);
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
