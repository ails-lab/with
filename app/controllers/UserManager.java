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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import model.Collection;
import model.Media;
import model.User;
import model.User.Access;
import model.UserGroup;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Akka;
import play.libs.Crypto;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import scala.Unit;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import actors.ApiKeyManager;
import actors.ApiKeyManager.Create;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import akka.util.Timeout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;
import elastic.ElasticIndexer;

public class UserManager extends Controller {

	public static final ALogger log = Logger.of(UserManager.class);
	private static final long TOKENTIMEOUT = 10 * 1000l /* 10 sec */;

	// turns out this has to be called outside a controller else it turns to
	// null
	private static String APPLICATION_URL = play.Play.application()
			.configuration().getString("application.baseUrl");

	/**
	 * @param emailOrUsername
	 * @return User and image
	 */
	public static Result findByUsernameOrEmail(String emailOrUsername,
			String collectionId) {
		Function<User, Status> getUserJson = (User u) -> {
			ObjectNode userJSON = Json.newObject();
			userJSON.put("username", u.getUsername());
			userJSON.put("firstName", u.getFirstName());
			userJSON.put("lastName", u.getLastName());
			if (collectionId != null) {
				Collection collection = DB.getCollectionDAO().getById(
						new ObjectId(collectionId));
				if (collection != null) {
					Access accessRights = collection.getRights().get(
							u.getDbId());
					if (accessRights != null)
						userJSON.put("accessRights", accessRights.toString());
					else
						userJSON.put("accessRights", Access.NONE.toString());
				}
			}
			String image = UserManager.getImageBase64(u);
			if (image != null) {
				userJSON.put("image", image);
			}
			return ok(userJSON);
		};
		User user = DB.getUserDAO().getByEmail(emailOrUsername);
		if (user != null) {
			return getUserJson.apply(user);
		} else {
			user = DB.getUserDAO().getByUsername(emailOrUsername);
			if (user != null)
				return getUserJson.apply(user);
			else
				return badRequest("The string you provided does not match an existing email or username");
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
		Collection fav = new Collection();
		fav.setCreated(new Date());
		fav.setOwnerId(user.getDbId());
		fav.setTitle("_favorites");
		DB.getCollectionDAO().makePermanent(fav);
		ElasticIndexer indexer = new ElasticIndexer(fav);
		indexer.indexCollectionMetadata();
		session().put("user", user.getDbId().toHexString());
		result = (ObjectNode) Json.parse(DB.getJson(user));
		result.remove("md5Password");
		result.put("favoritesId", fav.getDbId().toString());
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
			return getUser(u.getDbId().toString());
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
			return getUser(u.getDbId().toString());
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
				return getUser(u.getDbId().toHexString());
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
				session().put("sourceIp", request().remoteAddress());
				session().put("lastAccessTime",
						Long.toString(System.currentTimeMillis()));
				return getUser(u.getDbId().toHexString());
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
			session().put("sourceIp", request().remoteAddress());
			session().put("lastAccessTime",
					Long.toString(System.currentTimeMillis()));

			// now return the whole user stuff, just for good measure
			return getUser(u.getDbId().toHexString());
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
			String userId = input.get("user").asText();
			long timestamp = input.get("timestamp").asLong();
			if (new Date().getTime() < (timestamp + TOKENTIMEOUT)) {
				User u = DB.getUserDAO().get(new ObjectId(userId));
				if (u != null) {
					session().put("user", userId);
					session().put("sourceIp", request().remoteAddress());
					session().put("lastAccessTime",
							Long.toString(System.currentTimeMillis()));
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
			User user = DB.getUserDAO().getById(new ObjectId(id), null);
			if (user != null) {
				ObjectNode result = (ObjectNode) Json.parse(DB.getJson(user));
				result.put("favoritesId", DB.getCollectionDAO()
						.getByOwnerAndTitle(new ObjectId(id), "_favorites")
						.getDbId().toString());
				String image = getImageBase64(user);
				if (image != null) {
					result.put("image", image);
					return ok(result);
				} else {
					return ok(result);
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
			User user = DB.getUserDAO().getById(new ObjectId(id), null);
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
		try {
			User user = DB.getUserDAO().getById(new ObjectId(id), null);
			if (user != null) {
				if (json.has("image")) {
					String imageUpload = json.get("image").asText();
					String mimeType = null;
					byte[] imageBytes = null;
					// check if image is given in bytes
					if (imageUpload.startsWith("data")) {
						String[] imageInfo = new String[2];
						imageInfo = imageUpload.split(",");
						String info = imageInfo[0];
						mimeType = info.substring(5);
						// check if image is encoded in base64 format
						imageBytes = imageInfo[1].getBytes();

						// check if image is given as URL
					} else if (imageUpload.startsWith("http")) {
						try {
							URL url = new URL(imageUpload);
							HttpsURLConnection connection = (HttpsURLConnection) url
									.openConnection();
							mimeType = connection
									.getHeaderField("content-type");
							imageBytes = IOUtils.toByteArray(connection
									.getInputStream());
						} catch (MalformedURLException e) {
							return badRequest(e.getMessage());
						} catch (IOException e) {
							return badRequest(e.getMessage());
						}
					} else {
						return badRequest(Json
								.parse("{\"error\":\"Unknown image format\"}"));
					}
					// check if image is encoded in base64 format
					if (mimeType.contains("base64")) {
						imageBytes = Base64.decodeBase64(imageBytes);
						mimeType = mimeType.replace(";base64", "");
					}
					Media media = new Media();
					media.setType(Media.BaseType.valueOf("IMAGE"));
					media.setMimeType(mimeType);
					media.setHeight(100);
					media.setWidth(100);
					media.setOwnerId(user.getDbId());
					media.setData(imageBytes);
					try {
						DB.getMediaDAO().makePermanent(media);
						user.setPhoto(media.getDbId());
					} catch (Exception e) {
						return badRequest(e.getMessage());
					}
				}
				if (json.has("gender"))
					user.setGender(json.get("gender").asText());
				user.setFirstName(firstName);
				user.setLastName(lastName);
				if (json.has("about")) {
					user.setAbout(json.get("about").asText());
				}
				if (json.has("location")) {
					user.setLocation(json.get("location").asText());
				}
				DB.getUserDAO().makePermanent(user);
				result = (ObjectNode) Json.parse(DB.getJson(user));
				result.remove("md5Password");
				return getUser(user.getDbId().toHexString());
			} else {
				return badRequest(Json
						.parse("{\"error\":\"User does not exist\"}"));

			}
		} catch (IllegalArgumentException e) {
			return badRequest(Json.parse("{\"error\":\"User does not exist\"}"));
		}

	}

	/**
	 * This is just a skeleton until design issues are solved
	 *
	 * @param id
	 * @return
	 */

	public static Result deleteUser(String id) {

		ObjectNode result = Json.newObject();
		// ObjectNode error = (ObjectNode) Json.newObject();

		try {
			User user = DB.getUserDAO().getById(new ObjectId(id), null);
			if (user != null) {
				return ok(result);
			} else {
				return badRequest(Json
						.parse("{\"error\":\"User does not exist\"}"));

			}
		} catch (IllegalArgumentException e) {
			return badRequest(Json.parse("{\"error\":\"User does not exist\"}"));
		}
	}

	public static Result addUserToGroup(String uid, String gid) {
		ObjectNode result = Json.newObject();

		UserGroup group = DB.getUserGroupDAO().get(new ObjectId(gid));
		if (group == null) {
			result.put("message", "Cannot retrieve group from database!");
			return internalServerError(result);
		}

		group.getUsers().add(new ObjectId(uid));
		Set<ObjectId> parentGroups = group.retrieveParents();

		User user = DB.getUserDAO().get(new ObjectId(uid));
		if (user == null) {
			result.put("message", "Cannot retrieve user from database!");
			return internalServerError(result);
		}
		parentGroups.add(group.getDbId());
		user.addUserGroup(parentGroups);

		if (!(DB.getUserDAO().makePermanent(user) == null)
				&& !(DB.getUserGroupDAO().makePermanent(group) == null)) {
			result.put("message", "Group succesfully added to User");
			return ok(result);
		}

		result.put("message", "Cannot store to database!");
		return internalServerError(result);

	}
	
	
	
	
	public static Result apikey(String email) {
		
		// need to limit calls like this and reset password to 3 times per day maximum!
		
		ObjectNode result = Json.newObject();
		ObjectNode error = (ObjectNode) Json.newObject();

        Create create = new Create();
		
		String userId = session().get("user");
		// hexString!
		
		User u;
		
		if(userId==null){
			if (StringUtils.isEmpty(email)) {
				error.put("email", "Email is empty. Either log in or provide an email.");
				result.put("error", error);
				return badRequest(result);
			} else {
				
				u = DB.getUserDAO().getByEmail(email);
				
				
				create.email = email;
				
				if (u == null) {
					result.put("email", "Email not linked to account, an email has been sent.");

				} else {
					
					result.put("email", "Registered user's email found, an email has been sent.");
					userId = u.getDbId().toHexString();
					create.proxyUserId = new ObjectId(userId);
				}				
			}
		} else {
			result.put("email", "An email has been sent to your email address.");
			
			create.proxyUserId = new ObjectId(userId);
			
			u = DB.getUserDAO().get(create.proxyUserId);
			
			create.email = u.getEmail();
			
		}
		
		// what if they already have an API key??
		
		
        final ActorSelection testActor = Akka.system().actorSelection("/user/apiKeyManager");
        
        create.dbId = "";
        create.call = "";
        create.ip = "";
        create.counterLimit = -1l;
        create.volumeLimit = -1l;
        //create.position = 1;
        
        Timeout timeout = new Timeout(Duration.create(5, "seconds"));
        
    	Future<Object> future = Patterns.ask(testActor, create, timeout);
    	
    	String s = "";
		try {
			s = (String) Await.result(future, timeout.duration());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		if (s == "") {
			error.put("API Key", "Could not create API key");
			result.put("error", error);
			result.remove("email");
			return internalServerError(result);
		}
		
		result.put("API Key", "Succesfully created a new API key: "+s);
		
		String newLine = System.getProperty("line.separator");
		
		// String url = APPLICATION_URL;
		String url = "http://localhost:9000/assets/developers.html";
		
		String fn = "";
		String ln = "";
		
		if (u!=null){
			fn = ": " + u.getFirstName();
			ln = " " +u.getLastName();
		} 
		
		
		String message = "Dear user"
				+ fn
				+ ln
				+ ","
				+ newLine
				+ newLine
				+ "We received a request for a new API key. Your new API key is : " + newLine
				+ newLine + s
				// token URL here
				+ newLine + newLine 
				+ "You can use this key to make calls to the WITH API. "
				+ "To check out the WITH API documentation follow this link : " + newLine
				+ newLine + url
				+ newLine + newLine 
				+ "Sincerely yours," + newLine
				+ "The WITH team.";
		
		try {
			sendEmail(u, email, message, "WITH API key");
		} catch (EmailException e) {
			error.put("email", "Could not send email - Email server error");
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
		if (md5 == "") {
			// NULL didn't work so i used "", maybe this is not a good fix
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
		String resetURL = "http://localhost:9000/assets/index.html#reset";

		// This will retrieve line separator dependent on OS.
		String newLine = System.getProperty("line.separator");

		// more complex email are schemes available - want to discuss first
		

		// I use this account for some other sites as well but we can use it for
		// testing
		try {
			String subject = "WITH password reset";
			
			String msg = "Dear user: "
					+ u.getFirstName()
					+ " "
					+ u.getLastName()
					+ ","
					+ newLine
					+ newLine
					+ "We received a request for a password reset. Please click on the "
					+ "following link to confirm this request:" + newLine
					+ newLine + resetURL + "/" + enc
					// token URL here
					+ newLine + newLine + "Sincerely yours," + newLine
					+ "The WITH team.";
			
			sendEmail(u, "", msg, subject);
		} catch (EmailException e) {
			error.put("email", "Email server error");
			result.put("error", error);
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
		email.setSmtpPort(587);
		email.setHostName("smtp.gmail.com");
		email.setDebug(false);
		// email.setBounceAddress("karonissz@gmail.com");
		email.setAuthenticator(new DefaultAuthenticator(
				"karonissz@gmail.com", "12345678kostas"));
		email.setStartTLSEnabled(true);
		email.setSSLOnConnect(false);
		email.setFrom("karonissz@gmail.com", "kostas"); //check if this can be whatever
		email.setSubject(subject);
		
		if(u==null){
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
				if (new Date().getTime() < (timestamp + (TOKENTIMEOUT * 360 * 24 /*
																				 * 24
																				 * hours
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
				if (new Date().getTime() < (timestamp + (TOKENTIMEOUT * 360 * 24 /*
																				 * 24
																				 * hours
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

	public static String getImageBase64(User user) {
		if (user.getPhoto() != null) {
			ObjectId photoId = user.getPhoto();
			Media photo = DB.getMediaDAO().findById(photoId);
			// convert to base64 format
			return "data:" + photo.getMimeType() + ";base64,"
					+ new String(Base64.encodeBase64(photo.getData()));
		} else
			return null;
	}

}
