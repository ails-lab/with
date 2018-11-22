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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.PlusScopes;
import com.google.api.services.plus.model.Person;

import actors.ApiKeyManager.Create;
import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import akka.util.Timeout;
import db.DB;
import facebook4j.Facebook;
import facebook4j.FacebookFactory;
import facebook4j.Reading;
import facebook4j.conf.Configuration;
import facebook4j.conf.ConfigurationBuilder;
import model.ApiKey;
import model.resources.collection.CollectionObject;
import model.usersAndGroups.User;
import model.usersAndGroups.UserGroup;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Akka;
import play.libs.Crypto;
import play.libs.Json;
import play.mvc.Result;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import utils.Serializer;

public class UserManager extends WithController {

	public static final ALogger log = Logger.of(UserManager.class);
	private static final long TOKENTIMEOUT = 10 * 1000l /* 10 sec */;
	private static final String facebookAccessTokenUrl = "https://graph.facebook.com/v2.8/oauth/access_token";
	private static final String facebookSecretWith =   "SECRET_KEY";
	private static final String facebookSecretEspace = "SECRET_KEY";
	private static final String facebookSecretLocalhost = "SECRET_KEY";
	
	private static final String googleSecret = "SECRET_KEY";

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
	private static ArrayNode proposeUsername(String initial, String firstName, String lastName) {
		ArrayNode names = Json.newObject().arrayNode();
		String proposedName;
		int i = 0;
		User u;
		UserGroup g;
		do {
			proposedName = initial + i++;
			u = DB.getUserDAO().getByUsername(proposedName);
			g = DB.getUserGroupDAO().getByName(proposedName);
		} while ((u != null) || (g != null));
		names.add(proposedName);
		if ((firstName == null) || (lastName == null))
			return names;
		proposedName = firstName + "_" + lastName;
		i = 0;
		while ((DB.getUserDAO().getByUsername(proposedName) != null)
				|| (DB.getUserGroupDAO().getByName(proposedName) != null)) {
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
					+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
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
					error.put("password", "Password must contain more than 6 characters");
				}
			}
		}
		String username = null;
		// username unique
		if (!json.has("username")) {
			error.put("username", "Username is Empty");
		} else {
			username = json.get("username").asText();
			if ((DB.getUserDAO().getByUsername(username) != null)
					|| (DB.getUserGroupDAO().getByName(username) != null)) {
				error.put("username", "Username Already in Use");
				ArrayNode names = proposeUsername(username, firstName, lastName);
				result.put("proposal", names);

			}
		}

		if (error.size() != 0) {
			result.put("error", error);
		}

		return result;
	}

	public static Result getUser(String id) {
		try {
			// User user = DB.getUserDAO().getById(new ObjectId(id), new
			// ArrayList<String>() {{ add("firstName"); add("lastName"); }});
			User user = DB.getUserDAO().getById(new ObjectId(id));
			if (user != null) {
				// return ok(lightUserSerialization(user));
				return ok(Json.toJson(user));
			}
			return badRequest();
		} catch (Exception e) {
			return internalServerError();
		}
	}

	public static Result getMyUser() {
		try {
			User user = effectiveUser();
			if (user != null) {
				return ok(Json.toJson(user));
			}
			return badRequest();
		} catch (Exception e) {
			return internalServerError();
		}
	}

	/**
	 * Creates a user and stores him at the database
	 *
	 * @return the user JSON object (without the password) or JSON error
	 */
	public static Result register() {

		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		result = validateUser(json);
		if (result.has("error")) {
			return badRequest(result);
		}
		// If everything is ok store the user at the database
		User user = Json.fromJson(json, User.class);
		DB.getUserDAO().makePermanent(user);
		ObjectId fav = CollectionObjectController.createFavorites(user.getDbId());
		user.setFavorites(fav);
		session().put("user", user.getDbId().toHexString());
		session().put("sourceIp", request().remoteAddress());
		session().put("lastAccessTime", Long.toString(System.currentTimeMillis()));
		return ok(Json.toJson(user));
	}

	/**
	 * Deletes a user from the database including all the collections and groups
	 * she owns. In case she shares any collections she owns with other people,
	 * or she is the creator of groups with others, the user is not deleted but
	 * has to contact the developers about that.
	 *
	 * @return success or JSON error
	 */
	public static Result deleteUser(String id) {
		try {
			ObjectId userId = new ObjectId(id);
			ObjectId currentUserId = WithController.effectiveUserDbId();
			Status errorMessage = badRequest(Json.parse("{'error':'User cannot be deleted due to ownership of "
					+ "shared collections or groups. Please contact the developers'}"));
			if ((currentUserId == null) || (!currentUserId.equals(userId) && !WithController.isSuperUser()))
				return forbidden(Json.parse("{'error' : 'No rights for user deletion'}"));
			Set<ObjectId> groupsToDelete = new HashSet<ObjectId>();
			Set<ObjectId> collectionsToDelete = new HashSet<ObjectId>();
			User user = DB.getUserDAO().getById(userId, Arrays.asList("adminInGroups"));
			Set<ObjectId> groups = user.getAdminInGroups();
			for (ObjectId groupId : groups) {
				UserGroup group = DB.getUserGroupDAO().getById(groupId, Arrays.asList("creator, users"));
				if ((group == null) || !group.getCreator().equals(userId))
					continue;
				if (group.getUsers().size() > 1) {
					return errorMessage;
				}
				groupsToDelete.add(groupId);
			}
			List<CollectionObject> collections = DB.getCollectionObjectDAO().getByCreator(userId, 0, 0);
			for (CollectionObject collection : collections) {
				if (collection.getAdministrative().getAccess().getAcl().size() > 1) {
					return errorMessage;
				}
				collectionsToDelete.add(collection.getDbId());
			}
			for (ObjectId groupId : groupsToDelete)
				DB.getUserGroupDAO().deleteById(groupId);
			for (ObjectId collectionId : collectionsToDelete)
				DB.getCollectionObjectDAO().deleteById(collectionId);
			DB.getUserDAO().deleteById(userId);
			return ok(Json.parse("{'success' : 'User was successfully deleted from the database'}"));
		} catch (Exception e) {
			return internalServerError(Json.parse("{'error' : '" + e.getMessage() + "'}"));
		}
	}

	private static Result googleLogin(String googleId, String accessToken) {
		log.info(accessToken);
		User u = null;
		try {
			URL url = new URL("https://www.googleapis.com/oauth2/v1/userinfo?alt=json&access_token=" + accessToken);
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			InputStream is = connection.getInputStream();
			JsonNode res = Json.parse(is);
			String email = res.get("email").asText();
			u = DB.getUserDAO().getByEmail(email);
			if (u == null) {
				u = new User();
				u.setEmail(email);
				if (res.has("given_name"))
					u.setFirstName(res.get("given_name").asText());
				if (res.has("family_name"))
					u.setLastName(res.get("family_name").asText());
				String[] split = email.split("@");
				u.setUsername(split[0]);
				DB.getUserDAO().makePermanent(u);
				ObjectId fav = CollectionObjectController.createFavorites(u.getDbId());
			}
			u.setGoogleId(googleId);
			DB.getUserDAO().makePermanent(u);
			// return ok(lightUserSerialization(u));
			if (u != null) {
				session().put("user", u.getDbId().toHexString());
				session().put("username", u.getUsername());
				session().put("sourceIp", request().remoteAddress());
				session().put("lastAccessTime", Long.toString(System.currentTimeMillis()));
			}
			return ok(Json.toJson(u));
		} catch (Exception e) {
			return badRequest(Json.parse("{\"error\":\"Couldn't validate user\"}"));
		}
	}

	private static Result login(User user) {
		session().put("user", user.getDbId().toHexString());
		session().put("username", user.getUsername());
		session().put("sourceIp", request().remoteAddress());
		session().put("lastAccessTime", Long.toString(System.currentTimeMillis()));
		return ok(Json.toJson(user));
	}

	private static Result facebookLogin(String facebookId, String accessToken) {
		log.info(accessToken);
		User u = null;
		try {
			URL url = new URL(
					"https://graph.facebook.com/me?fields=email,first_name,last_name&format=json&access_token="
							+ accessToken);
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			InputStream is = connection.getInputStream();
			JsonNode res = Json.parse(is);
			String email = res.get("email").asText();
			u = DB.getUserDAO().getByEmail(email);
			if (u == null) {
				u = new User();
				u.setEmail(email);
				if (res.has("first_name"))
					u.setFirstName(res.get("first_name").asText());
				if (res.has("last_name"))
					u.setLastName(res.get("last_name").asText());
				String[] split = email.split("@");
				u.setUsername(split[0]);
				DB.getUserDAO().makePermanent(u);
				ObjectId fav = CollectionObjectController.createFavorites(u.getDbId());
			}
			u.setFacebookId(facebookId);
			DB.getUserDAO().makePermanent(u);
			// return ok(lightUserSerialization(u));
			if (u != null) {
				session().put("user", u.getDbId().toHexString());
				session().put("username", u.getUsername());
				session().put("sourceIp", request().remoteAddress());
				session().put("lastAccessTime", Long.toString(System.currentTimeMillis()));
			}
			return ok(Json.toJson(u));
		} catch (Exception e) {
			return badRequest(Json.parse("{\"error\":\"Couldn't validate user\"}"));
		}
	}

	public static Result facebookLogin() {
		try {
			JsonNode json = request().body().asJson();
			String facebookSecret;
			if (json.get("redirectUri").asText().contains("espaceportal"))
				facebookSecret = facebookSecretEspace;
			else if (json.get("redirectUri").asText().contains("localhost"))
				facebookSecret = facebookSecretLocalhost;
			else
				facebookSecret = facebookSecretWith;
			// Exchange authorization code for access token.
			String params = "code=" + json.get("code").asText() + "&client_id=" + json.get("clientId").asText()
					+ "&redirect_uri=" + json.get("redirectUri").asText() + "&client_secret="
					// TODO: Put the facebookSecret in the configuration file
					+ facebookSecret;
			URL url = new URL(facebookAccessTokenUrl + "?" + params);
			InputStream is = ((HttpsURLConnection) url.openConnection()).getInputStream();
			String accessToken = Json.parse(is).get("access_token").asText();
			// Create conf builder and set authorization and access keys
			ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
			configurationBuilder.setDebugEnabled(true);
			configurationBuilder.setOAuthAppId(json.get("clientId").asText());
			configurationBuilder.setOAuthAppSecret(facebookSecret);
			configurationBuilder.setOAuthAccessToken(accessToken);
			configurationBuilder
					.setOAuthPermissions("email, id, name, first_name, last_name");
			configurationBuilder.setUseSSL(true);
			configurationBuilder.setJSONStoreEnabled(true);

			// Create configuration and get Facebook instance
			Configuration configuration = configurationBuilder.build();
			FacebookFactory ff = new FacebookFactory(configuration);
			Facebook facebook = ff.getInstance();
			facebook4j.User facebookUser = facebook.getMe(new Reading().fields("email","id","name","first_name", "last_name"));
			User user = DB.getUserDAO().getByFacebookId(facebookUser.getId());
			if (user != null)
				return login(user);
			String email = facebookUser.getEmail();
			user = DB.getUserDAO().getByEmail(email);
			if (user == null) {
				return register(facebookUser);
			}
			user.setFacebookId(facebookUser.getId());;
			DB.getUserDAO().makePermanent(user);
			return login(user);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return badRequest(Json.parse("{\"error\":\""+e.getMessage()+"\"}"));
		}
	}

	private static Result register(Person profile) {
		User user = new User();
		user.setFirstName(profile.getName().getGivenName());
		user.setLastName(profile.getName().getFamilyName());
		String email = profile.getEmails().get(0).getValue();
		String[] split = email.split("@");
		user.setUsername(split[0]);
		user.setEmail(email);
		user.setGoogleId(profile.getId());
		// TODO : Avatar
		DB.getUserDAO().makePermanent(user);
		ObjectId fav = CollectionObjectController.createFavorites(user.getDbId());
		user.setFavorites(fav);
		return login(user);
	}
	
	private static Result register(facebook4j.User facebookUser) {
		User user = new User();
		log.error(facebookUser.toString());
		log.error(facebookUser.getId());
		log.error(facebookUser.getFirstName());
		log.error(facebookUser.getLastName());
		log.error(facebookUser.getEmail());
		user.setFirstName(facebookUser.getFirstName());
		user.setLastName(facebookUser.getLastName());
		String email = facebookUser.getEmail();
		String[] split = email.split("@");
		user.setUsername(split[0]);
		user.setEmail(email);
		user.setFacebookId(facebookUser.getId());
		// TODO : Avatar
		DB.getUserDAO().makePermanent(user);
		ObjectId fav = CollectionObjectController.createFavorites(user.getDbId());
		user.setFavorites(fav);
		return login(user);
	}

	public static Result googleLogin() {
		try {
			JsonNode json = request().body().asJson();
			GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(new NetHttpTransport(),
					new JacksonFactory(), json.get("clientId").asText(), googleSecret,
					Collections.singleton(PlusScopes.PLUS_ME)).build();
			GoogleTokenResponse tokenResponse = flow.newTokenRequest(json.get("code").asText())
					.setRedirectUri(json.get("redirectUri").asText()).execute();
			String accessToken = tokenResponse.getAccessToken();
			// Build the credential from stored token data.
			GoogleCredential credential = new GoogleCredential.Builder()
					.setClientSecrets(json.get("clientId").asText(), googleSecret).build()
					.setFromTokenResponse(tokenResponse);
			Plus plus = new Plus.Builder(new NetHttpTransport(), new JacksonFactory(), credential).build();
			Person profile = plus.people().get("me").execute();
			User user = DB.getUserDAO().getByGoogleId(profile.getId());
			if (user != null)
				return login(user);
			String email = profile.getEmails().get(0).getValue();
			user = DB.getUserDAO().getByEmail(email);
			if (user == null) {
				return register(profile);
			}
			user.setGoogleId(profile.getId());
			DB.getUserDAO().makePermanent(user);
			return login(user);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return badRequest(Json.parse("{\"error\":\"Invalid credentials\"}"));
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
				session().put("username", u.getUsername());
				session().put("sourceIp", request().remoteAddress());
				session().put("lastAccessTime", Long.toString(System.currentTimeMillis()));
				return ok(Json.toJson(u));
			} else {
				if (!json.has("accessToken")) {
					result.put("error", "facebookId is not valid.");
					return badRequest(result);
				} else {
					String accessToken = json.get("accessToken").asText();
					return facebookLogin(facebookId, accessToken);
				}
			}
		}
		if (json.has("googleId")) {
			String googleId = json.get("googleId").asText();
			u = DB.getUserDAO().getByGoogleId(googleId);
			if (u != null) {
				session().put("user", u.getDbId().toHexString());
				session().put("username", u.getUsername());
				session().put("sourceIp", request().remoteAddress());
				session().put("lastAccessTime", Long.toString(System.currentTimeMillis()));
				return ok(Json.toJson(u));
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
			session().put("username", u.getUsername());
			session().put("sourceIp", request().remoteAddress());
			session().put("lastAccessTime", Long.toString(System.currentTimeMillis()));
			// return ok(lightUserSerialization(u));
			return ok(Json.toJson(u));
		} else {
			error.put("password", "Invalid Password");
			result.put("error", error);
			return badRequest(result);
		}
	}
	
	public static Result getUserByUsername(String username) {
		try {
			User user = DB.getUserDAO().getUniqueByFieldAndValue("username", username);
			return ok(Json.toJson(user));
		} catch (Exception e) {
			return badRequest(Json.parse("'error' : '"+ e.getMessage() +"'"));
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
			String userId = input.get("user").asText();
			long timestamp = input.get("timestamp").asLong();
			if (new Date().getTime() < (timestamp + TOKENTIMEOUT)) {
				User u = DB.getUserDAO().get(new ObjectId(userId));
				if (u != null) {
					session().put("user", userId);
					session().put("sourceIp", request().remoteAddress());
					session().put("lastAccessTime", Long.toString(System.currentTimeMillis()));
					ObjectNode result = Json.newObject();
					result = (ObjectNode) Json.parse(DB.getJson(u));
					result.remove("md5Password");
					return ok(result);
				}
			}
		} catch (Exception e) {
			// likely invalid token
			log.error("Login with token failed", e);
		}
		return badRequest();
	}

	private static String encryptToken(String id) {

		ObjectNode result = Json.newObject();
		result.put("user", id);
		result.put("timestamp", new Date().getTime());
		// just to make them all different
		result.put("random", new Random().nextInt());
		String enc = Crypto.encryptAES(result.toString());
		return enc;

	}

	public static Result getToken() {
		String userId = session().get("user");
		if (userId == null)
			return badRequest();
		String enc = encryptToken(userId);
		return ok(enc);
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

	public static Result editUser(String id) throws JsonProcessingException, IOException {

		// Only changes first and last name for testing purposes
		//
		// should use validateRegister() in the future
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		ObjectNode error = Json.newObject();

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
		User user = DB.getUserDAO().getById(new ObjectId(id), null);
		ObjectMapper objectMapper = new ObjectMapper();
		ObjectReader updator = objectMapper.readerForUpdating(user);
		updator.readValue(json);
		DB.getUserDAO().makePermanent(user);
		return ok(Json.toJson(user));
		/*
		 * try { User user = DB.getUserDAO().getById(new ObjectId(id), null); if
		 * (user != null) { if (json.has("image")) { String imageUpload =
		 * json.get("image").asText(); String mimeType = null; byte[] imageBytes
		 * = null; // check if image is given in bytes if
		 * (imageUpload.startsWith("data")) { String[] imageInfo = new
		 * String[2]; imageInfo = imageUpload.split(","); String info =
		 * imageInfo[0]; mimeType = info.substring(5); // check if image is
		 * encoded in base64 format imageBytes = imageInfo[1].getBytes();
		 *
		 * // check if image is given as URL } else if
		 * (imageUpload.startsWith("http")) { try { URL url = new
		 * URL(imageUpload); HttpsURLConnection connection =
		 * (HttpsURLConnection) url .openConnection(); mimeType = connection
		 * .getHeaderField("content-type"); imageBytes =
		 * IOUtils.toByteArray(connection .getInputStream()); } catch
		 * (MalformedURLException e) { return badRequest(e.getMessage()); }
		 * catch (IOException e) { return badRequest(e.getMessage()); } } else {
		 * return badRequest(Json
		 * .parse("{\"error\":\"Unknown image format\"}")); } // check if image
		 * is encoded in base64 format if (mimeType.contains("base64")) {
		 * imageBytes = Base64.decodeBase64(imageBytes); mimeType =
		 * mimeType.replace(";base64", ""); } Media media = new Media();
		 * media.setType(Media.BaseType.valueOf("IMAGE"));
		 * media.setMimeType(mimeType); media.setHeight(100);
		 * media.setWidth(100); media.setOwnerId(user.getDbId());
		 * media.setData(imageBytes); try {
		 * DB.getMediaDAO().makePermanent(media);
		 * user.setThumbnail(media.getDbId()); } catch (Exception e) { return
		 * badRequest(e.getMessage()); } } if (json.has("gender"))
		 * user.setGender(json.get("gender").asText());
		 * user.setFirstName(firstName); user.setLastName(lastName); if
		 * (json.has("about")) { user.setAbout(json.get("about").asText()); }
		 *
		 * if (json.has("location")) {
		 * user.setLocation(json.get("location").asText()); }
		 *
		 * DB.getUserDAO().makePermanent(user); result = (ObjectNode)
		 * Json.parse(DB.getJson(user)); result.remove("md5Password"); return
		 * ok(Json.toJson(user)); } else { return badRequest(Json
		 * .parse("{\"error\":\"User does not exist\"}"));
		 *
		 * } } catch (IllegalArgumentException e) { return
		 * badRequest(Json.parse("{\"error\":\"User does not exist\"}")); }
		 */

	}

	public static Result apikey() {

		// need to limit calls like this and reset password to 3 times per day
		// maximum!

		ObjectNode result = Json.newObject();
		ObjectNode error = Json.newObject();

		Create create = new Create();

		String userId = session().get("user");
		// hexString!

		User u;

		if (userId != null) {

			result.put("email", "An email has been sent to the email address you have registered with.");

			create.proxyUserId = new ObjectId(userId);

			u = DB.getUserDAO().get(create.proxyUserId);

			create.email = u.getEmail();

		} else {

			error.put("error", "You need to log in before an API key can be issued.");

			result.put("error", error);

			return badRequest(result);

		}

		// Discuss our policy when this happens. ( Resend? How many times? )

		ApiKey withKey = DB.getApiKeyDAO().getByEmail(create.email);

		if (withKey != null) {

			result.remove("email");

			error.put("error", "Your user account already has already been issued an API key. "
					+ "To request a new one, send an email to: withdev@image.ece.ntua.gr.");

			// result.put("Key", withKey.getKeyString());

			result.put("error", error);

			return badRequest(result);
		}

		final ActorSelection testActor = Akka.system().actorSelection("/user/apiKeyManager");

		create.dbId = "";
		create.call = "";
		// create.ip = "";
		create.counterLimit = -1l;
		create.volumeLimit = -1l;
		// create.position = 1;

		Timeout timeout = new Timeout(Duration.create(5, "seconds"));

		Future<Object> future = Patterns.ask(testActor, create, timeout);

		String s = "";

		try {
			s = (String) Await.result(future, timeout.duration());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error("", e);
		}

		if (s == "") {
			error.put("error", "Could not create API key.");
			result.put("error", error);
			result.remove("email");
			return internalServerError(result);
		}

		// result.put("APIKey", "Succesfully created a new API key: " + s);

		String newLine = System.getProperty("line.separator");

		// String url = APPLICATION_URL;
		String url = "http://with.image.ntua.gr/assets/developers-lite.html";

		String fn = "";
		String ln = "";

		if (u != null) {
			fn = ": " + u.getFirstName();
			ln = " " + u.getLastName();
		}

		String message = "Dear user" + fn + ln + "," + newLine + newLine
				+ "We received a request for a new API key. Your new API key is : " + newLine + newLine + s
				// token URL here
				+ newLine + newLine + "You can use this key to make calls to the WITH API. "
				+ "To return to the WITH API documentation follow this link : " + newLine + newLine + url + newLine
				+ newLine + "Sincerely yours," + newLine + "The WITH team.";

		try {
			sendEmail(u, create.email, message, "WITH API key");
		} catch (EmailException e) {
			error.put("error", "Could not send email - Email server error");
			result.put("error", error);
			return badRequest(result); // maybe change type?
		}

		return ok(result);
	}

	/**
	 * Checks email/username validity, checks if user has registered with
	 * Facebook or Google.
	 *
	 * Sends user an email with a url.
	 *
	 * The url has a token, to be parsed by changePassword().
	 *
	 * @param emailOrUserName
	 * @return OK status
	 */

	public static Result resetPassword(String emailOrUserName) {

		ObjectNode result = Json.newObject();
		ObjectNode error = Json.newObject();
		User u = null;
		String md5 = "";

		if (emailOrUserName == null) {
			error.put("email", "Email or Username are empty");
			result.put("error", error);
			return badRequest(result);
		}

		// can a user register using an email as username?
		// we use this kind of check in another function here

		u = DB.getUserDAO().getByEmail(emailOrUserName);
		if (u == null) {
			u = DB.getUserDAO().getByUsername(emailOrUserName);
			if (u == null) {
				error.put("email", "Invalid Email Address or Username");
				result.put("error", error);
				return badRequest(result);
			}
		}

		md5 = u.getMd5Password();

		if (md5 == null) {
			if (u.getFacebookId() != "") {
				result.put("email", "User has registered with Facebook account");
				return notFound(result); // maybe change type?
			} else if (u.getGoogleId() != "") {
				result.put("email", "User has registered with Google account");
				return notFound(result); // maybe change type?
			} // else {
				// user exists but has no password and not fb or google -
				// impossible?
				// }
		}

		String enc = encryptToken(u.getDbId().toString());

		// String resetURL = APPLICATION_URL;
		String resetURL = "http://with.image.ntua.gr/assets/index.html#reset";

		// This will retrieve line separator dependent on OS.
		String newLine = System.getProperty("line.separator");

		// more complex email are schemes available - want to discuss first

		// I use this account for some other sites as well but we can use it for
		// testing
		try {
			String subject = "WITH password reset";

			String msg = "Dear user: " + u.getFirstName() + " " + u.getLastName() + "," + newLine + newLine
					+ "We received a request for a password reset. Please click on the "
					+ "following link to confirm this request:" + newLine + newLine + resetURL + "/" + enc
					// token URL here
					+ newLine + newLine + "Sincerely yours," + newLine + "The WITH team.";

			sendEmail(u, "", msg, subject);
		} catch (EmailException e) {
			error.put("email", "Email server error");
			result.put("error", error);
			result.put("description", e.toString());
			return badRequest(result); // maybe change type?
		}

		result.put("message", "Email succesfully sent to user");

		// used for testing
		// result.put("url", resetURL + "/" + enc);

		return ok(result);

	}

	/**
	 * @param u
	 * @param enc
	 * @param resetURL
	 * @param newLine
	 * @param email
	 * @throws EmailException
	 */
	public static void sendEmail(User u, String mailAdress, String message, String subject) throws EmailException {
		Email email = new SimpleEmail();
		email.setSmtpPort(25);
		email.setHostName("smtp.image.ece.ntua.gr");
		email.setDebug(false);
		email.setAuthenticator(new DefaultAuthenticator("mikegiatzi", "tempforwith"));
		email.setStartTLSEnabled(true);
		email.setSSLOnConnect(false);
		email.setFrom("with-no-reply@image.ece.ntua.gr", "WITH"); // check if
																	// this can
																	// be
		// whatever
		email.addBcc("mikegiatzi@image.ece.ntua.gr");
		email.setBounceAddress("mikegiatzi@image.ece.ntua.gr.com");
		email.setSubject(subject);

		if (u == null) {
			email.addTo(mailAdress);

		} else {
			email.addTo(u.getEmail());
		}

		email.setMsg(message);

		email.send();

	}

	/***
	 * Parses token from the URL sent in the resetPassword() email.
	 *
	 * Changes stored password.
	 *
	 * @return OK and user data
	 ***/

	public static Result changePassword() {

		ObjectNode result = Json.newObject();

		JsonNode json = request().body().asJson();
		String newPassword = null;
		String token = null;

		if (json.has("password")) {
			newPassword = json.get("password").asText();
		}

		if (json.has("token")) {
			token = json.get("token").asText();
		} else {
			result.put("error", "Token is empty");
			return badRequest(result);
		}

		if (newPassword == null) {

			try {
				JsonNode input = Json.parse(Crypto.decryptAES(token));
				long timestamp = input.get("timestamp").asLong();
				if (new Date().getTime() < (timestamp
						+ (TOKENTIMEOUT * 360 * 24 /*
													 * 24 hours
													 */))) {
					result.put("message", "Token is valid");
					return ok(result);
				} else {
					result.put("error", "Token timeout");
					return badRequest(result);
				}
			} catch (Exception e) {
				result.put("error", "Invalid token");
			}

		} else if (newPassword.length() < 6) {
			result.put("error", "Password must contain more than 6 characters");
			return badRequest(result);

		} else {

			User u = null;

			try {
				JsonNode input = Json.parse(Crypto.decryptAES(token));
				String userId = input.get("user").asText();
				long timestamp = input.get("timestamp").asLong();
				if (new Date().getTime() < (timestamp
						+ (TOKENTIMEOUT * 360 * 24 /*
													 * 24 hours
													 */))) {
					u = DB.getUserDAO().get(new ObjectId(userId));
					if (u != null) {
						u.setPassword(newPassword);
						DB.getUserDAO().makePermanent(u);
						result = (ObjectNode) Json.parse(DB.getJson(u));
						result.remove("md5Password");
						return ok(result);
					} else {
						result.put("error", "User does not exist");
					}
				} else {
					result.put("error", "Token timeout");
					return badRequest(result);
				}
			} catch (Exception e) {
				result.put("error", "Invalid token");

			}
		}
		return badRequest(result);
	}

	private static ObjectNode lightUserSerialization(User u) {
		ObjectMapper uMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addSerializer(User.class, new Serializer.LightUserSerializer());
		uMapper.registerModule(module);
		return uMapper.valueToTree(u);
	}
}
