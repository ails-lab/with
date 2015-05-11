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
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import model.Media;
import model.User;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Crypto;
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
	private static final long TOKENTIMEOUT = 10 * 1000l /* 10 sec */;

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
	 * Propose new username when it is already in use.
	 *
	 * @param initial
	 *            the initial username the user tried
	 * @param firstName
	 *            the first name of the user
	 * @param lastName
	 *            the last name of the user
	 * 
	 * @return the array node with two suggested alternative usernames
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
		if ((firstName == null) || (lastName == null))
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
	 * Validation checks for register and put user
	 * 
	 * * @param json the json of the user to create
	 * 
	 * @return result of checks, empty or error, may contain username proposal
	 */

	// only used by register() for now

	// change name?

	// TODO blank checks

	private static ObjectNode validateUser(JsonNode json) {

		ObjectNode result = Json.newObject();
		ObjectNode error = Json.newObject();

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
				error.put("usernposeUsernameame", "Username Already in Use");
				ArrayNode names = proposeUsername(username, firstName, lastName);
				result.put("proposal", names);

			}
		}

		if (error.size() != 0) {
			result.put("error", error);
		}

		return result;
	}

	/**
	 * Creates a user and stores him at the database
	 *
	 * @return the user JSON object (without the password) or JSON error
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result register() {

		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		// ObjectNode error = (ObjectNode) Json.newObject();

		// copied from here

		result = validateUser(json);

		// If everything is ok store the user at the database

		if (result.has("error")) {
			return badRequest(result);
		}

		User user = Json.fromJson(json, User.class);
		DB.getUserDAO().makePermanent(user);
		session().put("user", user.getDbId().toHexString());
		result = (ObjectNode) Json.parse(DB.getJson(user));
		result.remove("md5Password");
		return ok(result);

	}

	private static Result googleLogin(String googleId, String accessToken) {
		log.info(accessToken);
		User u = null;
		try {
			URL url = new URL(
					"https://www.googleapis.com/oauth2/v1/tokeninfo?access_token="
							+ accessToken);
			HttpsURLConnection connection = (HttpsURLConnection) url
					.openConnection();
			InputStream is = connection.getInputStream();
			JsonNode res = Json.parse(is);
			String email = res.get("email").asText();
			u = DB.getUserDAO().getByEmail(email);
			if (u == null) {
				return badRequest(Json
						.parse("{\"error\":\"User not registered\"}"));
			}
			u.setGoogleId(googleId);
			DB.getUserDAO().makePermanent(u);
			return ok(Json.parse(DB.getJson(u)));
		} catch (Exception e) {
			return badRequest(Json
					.parse("{\"error\":\"Couldn't validate user\"}"));
		}
	}

	private static Result facebookLogin(String facebookId, String accessToken) {
		log.info(accessToken);
		User u = null;
		try {
			URL url = new URL(
					"https://graph.facebook.com/me?fields=email&format=json&access_token="
							+ accessToken);
			HttpsURLConnection connection = (HttpsURLConnection) url
					.openConnection();
			InputStream is = connection.getInputStream();
			JsonNode res = Json.parse(is);
			String email = res.get("email").asText();
			u = DB.getUserDAO().getByEmail(email);
			if (u == null) {
				return badRequest(Json
						.parse("{\"error\":\"User not registered\"}"));
			}
			u.setFacebookId(facebookId);
			DB.getUserDAO().makePermanent(u);
			return ok(Json.parse(DB.getJson(u)));
		} catch (Exception e) {
			return badRequest(Json
					.parse("{\"error\":\"Couldn't validate user\"}"));
		}
	}

	/**
	 * Acquire a login cookie.
	 *
	 * @return OK status and the cookie or JSON error
	 */
	public static Result login() {

		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		ObjectNode error = Json.newObject();

		User u = null;
		if (json.has("facebookId")) {
			String facebookId = json.get("facebookId").asText();
			u = DB.getUserDAO().getByFacebookId(facebookId);
			if (u != null) {
				session().put("user", u.getDbId().toHexString());
				result = (ObjectNode) Json.parse(DB.getJson(u));
				result.remove("md5Password");
				return ok(result);
			} else {
				String accessToken = json.get("accessToken").asText();
				return facebookLogin(facebookId, accessToken);
			}
		}
		if (json.has("googleId")) {
			String googleId = json.get("googleId").asText();
			u = DB.getUserDAO().getByGoogleId(googleId);
			if (u != null) {
				session().put("user", u.getDbId().toHexString());
				session().put( "sourceIp", request().remoteAddress());
				session().put("lastAccessTime", Long.toString( System.currentTimeMillis()));
				result = (ObjectNode) Json.parse(DB.getJson(u));
				result.remove("md5Password");
				return ok(result);
			} else {
				String accessToken = json.get("accessToken").asText();
				return googleLogin(googleId, accessToken);
			}
		}
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
			result.remove("md5Password");
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
	 * @return OK status
	 */
	public static Result logout() {
		session().clear();
		return ok();
	}

	public static Result loginWithToken(String token) {
		try {
			JsonNode input = Json.parse(Crypto.decryptAES(token));
			String userId = input.get( "user").asText();
			long timestamp = input.get( "timestamp" ).asLong();
			if( new Date().getTime() < timestamp + TOKENTIMEOUT ) {
				User u = DB.getUserDAO().get( new ObjectId(userId));
				if( u != null ) {
					session().put( "user", userId );
					session().put( "sourceIp", request().remoteAddress());
					session().put("lastAccessTime", Long.toString( System.currentTimeMillis()));

					ObjectNode result = Json.newObject();
					result = (ObjectNode) Json.parse(DB.getJson(u));
					result.remove("md5Password");
					return ok(result);
				}
			}
		} catch (Exception e) {
			// likely invalid token
			log.error( "Login with token failed", e );
		}
		return badRequest();
	}

	public static Result getToken() {
		String userId = session().get("user");
		if (userId == null)
			return badRequest();
		ObjectNode result = Json.newObject();
		result.put("user", userId);
		result.put("timestamp", new Date().getTime());
		// just to make them all different
		result.put("random", new Random().nextInt());
		String enc = Crypto.encryptAES(result.toString());
		return ok(enc);
	}

	/**
	 * Get a list of matching usernames
	 *
	 * @param prefix
	 *            optional prefix of username
	 * @return JSON document with an array of matching usernames (or all of
	 *         them)
	 */
	public static Result listNames(String prefix) {
		List<User> users = DB.getUserDAO().getByUsernamePrefix(prefix);
		ArrayNode result = Json.newObject().arrayNode();
		for (User user : users) {
			result.add(user.getUsername());
		}
		return ok(result);
	}

	/**
	 * Find if email is already used.
	 *
	 * @param email
	 *            the email
	 * @return OK if the email is available or error if not
	 */
	public static Result emailAvailable(String email) {
		User user = DB.getUserDAO().getByEmail(email);
		if (user != null) {
			return badRequest(Json.parse("{\"error\":\"Email not available\"}"));
		} else {
			return ok();
		}
	}

	public static Result getUser(String id) {
		try {
			User user = DB.getUserDAO().getById(new ObjectId(id));
			if (user != null) {
				if (user.getPhoto() != null) {
					ObjectId photoId = user.getPhoto();
					Media photo = DB.getMediaDAO().findById(photoId);
					String image = photo.getMimeType() + ","
							+ new String(photo.getData());
					ObjectNode result = (ObjectNode) Json.parse(DB
							.getJson(user));
					result.put("image", image);
					return ok(result.toString());
				} else {
					return ok(DB.getJson(user));
				}
			} else {
				return badRequest(Json
						.parse("{\"error\":\"User does not exist\"}"));
			}
		} catch (Exception e) {
			return badRequest(Json.parse("{\"error\":\"" + e.getMessage()
					+ "\"}"));
		}
	}

	public static Result getUserPhoto(String id) {
		try {
			User user = DB.getUserDAO().getById(new ObjectId(id));
			if (user != null) {
				ObjectId photoId = user.getPhoto();
				return MediaController.getMetadataOrFile(photoId.toString(),
						true);
			} else {
				return badRequest(Json
						.parse("{\"error\":\"User does not exist\"}"));
			}
		} catch (Exception e) {
			return badRequest(Json.parse("{\"error\":\"" + e.getMessage()
					+ "\"}"));
		}
	}

	public static Result editUser(String id) {

		// Only changes first and last name for testing purposes
		//
		// should use validateRegister() in the future
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		ObjectNode error = (ObjectNode) Json.newObject();

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
		if (error.size() != 0) {
			result.put("error", error);
			return badRequest(result);

		}
		// If everything is ok store the user at the database
		try {
			User user = DB.getUserDAO().getById(new ObjectId(id));
			if (user != null) {
				if (json.has("image")) {
					String imageUpload = json.get("image").asText();
					String[] imageInfo = new String[2];
					imageInfo = imageUpload.split(",");
					String info = imageInfo[0];
					String base64Image = imageInfo[1];
					// byte[] image = DatatypeConverter
					// .parseBase64Binary(base64Image);
					byte[] image = base64Image.getBytes();
					Media media = new Media();
					media.setType("IMAGE");
					media.setMimeType(info);
					media.setHeight(100);
					media.setWidth(100);
					media.setOwnerId(user.getDbId());
					media.setData(image);
					try {
						DB.getMediaDAO().makePermanent(media);
						user.setPhoto(media.getDbId());
					} catch (Exception e) {
						return badRequest(e.getMessage());
					}
				}
				user.setFirstName(firstName);
				user.setLastName(lastName);
				DB.getUserDAO().makePermanent(user);
				result = (ObjectNode) Json.parse(DB.getJson(user));
				result.remove("md5Password");
				return ok(result);
			} else {
				return badRequest(Json
						.parse("{\"error\":\"User does not exist\"}"));

			}
		} catch (IllegalArgumentException e) {
			return badRequest(Json.parse("{\"error\":\"User does not exist\"}"));
		}

	}

}
