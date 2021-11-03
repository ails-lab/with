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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.mongodb.morphia.geo.GeoJson;
import org.mongodb.morphia.geo.Point;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryResults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DAO.QueryOperator;
import db.DB;
import model.basicDataTypes.WithAccess.Access;
import model.resources.collection.CollectionObject;
import model.usersAndGroups.Organization;
import model.usersAndGroups.Page;
import model.usersAndGroups.Project;
import model.usersAndGroups.User;
import model.usersAndGroups.UserGroup;
import model.usersAndGroups.UserOrGroup;
import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Promise;
import play.libs.F.RedeemablePromise;
import play.libs.Json;
import play.mvc.Result;
import sources.core.HttpConnector;
import utils.Locks;
import utils.Tuple;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class GroupManager extends WithController {

	public static final ALogger log = Logger.of(UserGroup.class);

	public enum GroupType {
		All, Organization, Project, UserGroup
	}

	/**
	 * Creates a {@link UserGroup} with the specified user as administrator and
	 * with the given body as JSON.
	 * <p>
	 * The name of the group must be unique. If the administrator is not
	 * provided as a parameter the administrator of the group becomes the user
	 * who made the call.
	 *
	 * @param adminId
	 *            the administrator id
	 * @param adminUsername
	 *            the administrator username
	 * @return the JSON of the new group
	 */
	public static Result createGroup(String adminId, String adminUsername,
			String groupType) {

		ObjectId admin;
		UserGroup newGroup = null;
		ObjectNode error = Json.newObject();
		JsonNode json = request().body().asJson();
		try {
			if (json == null) {
				error.put("error", "Invalid JSON");
				return badRequest(error);
			}
			ObjectId creator = effectiveUserDbId();
			if (creator == null) {
				error.put("error", "No rights for group creation");
				return forbidden(error);
			}
			if (!json.has("username")) {
				error.put("error", "Must specify name for the group");
				return badRequest(error);
			} else if (json.get("username").asText().length() < 3) {
				error.put("error", "Username of " + groupType
						+ " must contain at least 3 characters");
				return badRequest(error);
			}
			if (json.has("friendlyName")
					&& (json.get("friendlyName").asText().length() < 3)) {
				error.put("error", "Short Name of  " + groupType
						+ " must contain at least 3 characters");
				return badRequest(error);
			}

			if (!uniqueGroupName(json.get("username").asText())) {
				error.put("error",
						"Group name already exists! Please specify another name");
				return badRequest(error);
			}
			Class<?> clazz = Class.forName("model.usersAndGroups." + groupType);
			newGroup = (UserGroup) Json.fromJson(json, clazz);
			if (adminId != null) {
				admin = new ObjectId(adminId);
			} else if (adminUsername != null) {
				admin = DB.getUserDAO().getByUsername(adminUsername).getDbId();
			} else {
				admin = creator;
			}
			if (newGroup.getCreator() == null) {
				newGroup.setCreator(creator);
			}
			newGroup.addAdministrator(creator);
			newGroup.addAdministrator(admin);
			newGroup.getUsers().add(creator);
			newGroup.getUsers().add(admin);
			newGroup.setCreated(new Date());
			try {
				DB.getUserGroupDAO().makePermanent(newGroup);
				Set<ObjectId> parentGroups = newGroup.getParentGroups();
				parentGroups.add(newGroup.getDbId());
				User administrator = DB.getUserDAO().get(creator);
				administrator.addGroupForAdministration(newGroup.getDbId());
				administrator.addUserGroups(parentGroups);
				DB.getUserDAO().makePermanent(administrator);
				administrator = DB.getUserDAO().get(admin);
				administrator.addGroupForAdministration(newGroup.getDbId());
				administrator.addUserGroups(parentGroups);
				DB.getUserDAO().makePermanent(administrator);
			} catch (Exception e) {
				log.error("Cannot save group to database!", e.getMessage());
				error.put("error", "Cannot save group to database!");
				return internalServerError(error);
			}
			updatePage(newGroup.getDbId(), json);
			return ok(Json.toJson(newGroup));
		} catch (Exception e) {
			error.put("error", e.getMessage());
			return internalServerError(error);
		}
	}

	private static boolean uniqueGroupName(String name) {
		return ((DB.getUserGroupDAO().getByName(name) == null) && (DB
				.getUserDAO().getByUsername(name) == null));
	}

	private static String capitalizeFirst(String str) {
		return str.substring(0, 1).toUpperCase()
				+ str.substring(1).toLowerCase();
	}

	/**
	 * Edits group metadata and updates them according to the JSON body.
	 * <p>
	 * Only the creator of the group has the right to edit the group.
	 *
	 * @param groupId
	 *            the group id
	 * @return the updated group metadata
	 */
	public static Result editGroup(String groupId) {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		ObjectId groupDbId = new ObjectId(groupId);
		UserGroup group = DB.getUserGroupDAO().get(groupDbId);
		if (group == null) {
			result.put("error", "Cannot retrieve group from database.");
			return internalServerError(result);
		}
		User user = effectiveUser();
		Set<ObjectId> groupAdmins = group.getAdminIds();
		if (!groupAdmins.contains(user.getDbId()) && !user.isSuperUser()) {
			result.put("error",
					"Only an admin of the group has the right to edit the group.");
			return forbidden(result);
		}
		if (json.has("username") && (json.get("username") != null)
				&& !group.getUsername().equals(json.get("username").asText())) {
			if (!uniqueGroupName(json.get("username").asText())) {
				result.put("error",
						"Group name already exists! Please specify another name.");
				return badRequest(result);
			}
		}
		DB.getUserGroupDAO().editGroup(groupDbId, json);
		updatePage(groupDbId, json);
		return ok(Json.toJson(DB.getUserGroupDAO().get(groupDbId)));
	}

	private static void updatePage(ObjectId groupId, JsonNode json) {
		UserGroup group = DB.getUserGroupDAO().get(groupId);
		if (!json.has("page")
				|| (!(group instanceof Organization) && !(group instanceof Project)))
			return;
		Page newPage = Json.fromJson(json.get("page"), Page.class);
		if ((newPage.getAddress() == null) && (newPage.getCity() == null)
				&& (newPage.getCountry() == null))
			return;
		Page oldPage = null;
		// Keep previous page fields
		if (group instanceof Organization)
			oldPage = ((Organization) group).getPage();
		else if (group instanceof Project)
			oldPage = ((Project) group).getPage();

		// In case that the location has changed we need to calculate the
		// new coordinates
		String address = (newPage.getAddress() != null) ? newPage.getAddress()
				: oldPage.getAddress();
		String city = (newPage.getCity() != null) ? newPage.getCity() : oldPage
				.getCity();
		String country = (newPage.getCountry() != null) ? newPage.getCountry()
				: oldPage.getCountry();
		String fullAddress = "";
		if (address != null && !address.equals(""))
			fullAddress = fullAddress.concat(address+",");
		if (city != null && !city.equals(""))
			fullAddress = fullAddress.concat(city+",");
		if (country != null && !country.equals("") && !(country.length() == 1))
			fullAddress = fullAddress.concat(country);
		if (fullAddress.equals(""))
			return;
		if (fullAddress.charAt(fullAddress.length() - 1) == ',')
			fullAddress = fullAddress.substring(0, fullAddress.length()-1);
		fullAddress = fullAddress.replace(" ", "+");
		try {
			JsonNode response = HttpConnector.getWSHttpConnector()
					.getURLContent(
							"https://maps.googleapis.com/maps/api/geocode/json?address="
									+ fullAddress);
			Point coordinates = GeoJson.point(
					response.get("results").get(0).get("geometry")
							.get("location").get("lat").asDouble(),
					response.get("results").get(0).get("geometry")
							.get("location").get("lng").asDouble());
			DB.getUserGroupDAO().updatePageCoordinates(groupId, coordinates);
		} catch (Exception e) {
			log.error("Cannot update coordinates of group Page", e);
			DB.getUserGroupDAO().updatePageCoordinates(groupId, null);

		}
	}


	/*
	 * This method get's as input a list of Collection and Exhibition
	 * id's and updates the featuredCollections & featuredExhibitions
	 * lists at a Page.
	 */
	public static Result addFeatured(String groupId) {

		ObjectNode result = Json.newObject();
		JsonNode json = request().body().asJson();

		String userId = effectiveUserId();
		if ((userId == null) || (userId.equals(""))) {
			result.put("error",
					"Sorry! Only logged in users have access to this API call");
			return forbidden(result);
		}

		if(groupId == null) {
			result.put("error",
					"Invalid groupId specified!");
			return badRequest(result);
		}

		UserGroup group = DB.getUserGroupDAO().get(new ObjectId(groupId));
		if (!group.getAdminIds().contains(new ObjectId(userId)) &&
				!isSuperUser()) {
			result.put("error",
					"Only group admin or super user have the access to add featured collections/exhibitions");
			return forbidden(result);
		}


		if( !json.has("fCollections") &&	!json.has("fExhibitions")) {
			result.put("success",
					"Nothing to update!");
			return ok(result);
		}


		ArrayNode fCollections = Json.newObject().arrayNode();
		ArrayNode fExhibitions = Json.newObject().arrayNode();
		try{
			if(json.has("fCollections"))
				fCollections = (ArrayNode)json.withArray("fCollections");
			if(json.has("fExhibitions"))
					fExhibitions  = (ArrayNode)json.withArray("fExhibitions");
		} catch(UnsupportedOperationException opex) {
			log.debug("Unsupported cast", opex);
			result.put("error", "Bad value on json fields 'fCollection' or 'fExhibitions'");
			return internalServerError(result);
		}

		if((fCollections.size() == 0) &&
			(fExhibitions.size() == 0)) {
			result.put("success",
					"Nothing to update!");
			return ok(result);
		}

		List<ObjectId> fCols = new ArrayList<ObjectId>();
		fCollections.forEach(  id -> fCols.add(new ObjectId(id.asText())) );
		List<ObjectId> fExhs = new ArrayList<ObjectId>();
		fExhibitions.forEach(  id -> fExhs.add(new ObjectId(id.asText())) );

		if(DB.getUserGroupDAO().updateFeatured(new ObjectId(groupId), fCols, fExhs, "+") == 1) {
			result.put("success", "Featured Data succesfully updated!");
			return ok(result);
		} else {
			result.put("error", "Featured Data were not updated due to system error");
			return internalServerError(result);
		}
	}

	public static Result removeFeatured(String groupId) {
		ObjectNode result = Json.newObject();
		JsonNode json = request().body().asJson();

		String userId = effectiveUserId();
		if ((userId == null) || (userId.equals(""))) {
			result.put("error",
					"Sorry! Only logged in users have access to this API call");
			return forbidden(result);
		}

		if(groupId == null) {
			result.put("error",
					"Invalid groupId specified!");
			return badRequest(result);
		}

		UserGroup group = DB.getUserGroupDAO().get(new ObjectId(groupId));
		if (!group.getAdminIds().contains(new ObjectId(userId)) &&
				!isSuperUser()) {
			result.put("error",
					"Only admin of the group or the super user have the right to delete featured collections/exhibitions");
			return forbidden(result);
		}

		if( !json.has("fCollections") && !json.has("fExhibitions")) {
			result.put("success",
					"Nothing to update!");
			return ok(result);
		}


		ArrayNode fCollections = Json.newObject().arrayNode();
		ArrayNode fExhibitions = Json.newObject().arrayNode();
		try{
			if(json.has("fCollections"))
				fCollections = (ArrayNode)json.withArray("fCollections");
			if(json.has("fExhibitions"))
					fExhibitions  = (ArrayNode)json.withArray("fExhibitions");
		} catch(UnsupportedOperationException opex) {
			log.debug("Unsupported cast", opex);
			result.put("error", "Bad value on json fields 'fCollection' or 'fExhibitions'");
			return internalServerError(result);
		}

		if((fCollections.size() == 0) &&
			(fExhibitions.size() == 0)) {
			result.put("success",
					"Nothing to update!");
			return ok(result);
		}

		List<ObjectId> fCols = new ArrayList<ObjectId>();
		fCollections.forEach(  id -> fCols.add(new ObjectId(id.asText())) );
		List<ObjectId> fExhs = new ArrayList<ObjectId>();
		fExhibitions.forEach(  id -> fExhs.add(new ObjectId(id.asText())) );

		if(DB.getUserGroupDAO().updateFeatured(new ObjectId(groupId), fCols, fExhs, "-") == 1) {
			result.put("success", "Featured Data succesfully updated!");
			return ok(result);
		} else {
			result.put("error", "Featured Data were not updated due to system error");
			return internalServerError(result);
		}
	}

	/**
	 * Deletes a group from the database. The users who participate are not
	 * deleted as well.
	 *
	 * @param groupId
	 *            the group id
	 * @return success message
	 */
	public static Result deleteGroup(String groupId) {

		ObjectNode result = Json.newObject();
		String userId = effectiveUserId();
		if ((userId == null) || (userId.equals(""))) {
			result.put("error",
					"Only creator of the group has the right to delete the group");
			return forbidden(result);
		}
		try {
			UserGroup group = DB.getUserGroupDAO().get(new ObjectId(groupId));
			if (!group.getCreator().equals(new ObjectId(userId))) {
				result.put("error",
						"Only creator of the group has the right to delete the group");
				return forbidden(result);
			}
			Set<ObjectId> ancestorGroups = group.getAncestorGroups();
			ancestorGroups.add(group.getDbId());
			List<User> users = DB.getUserDAO().getByGroupId(group.getDbId());
			for (User user : users) {
				user.removeUserGroups(ancestorGroups);
				user.removeAdminUserGroupIds(ancestorGroups);
				DB.getUserDAO().makePermanent(user);
			}
			DB.getUserGroupDAO().deleteById(new ObjectId(groupId));
		} catch (Exception e) {
			log.error("Cannot delete group from database!", e);
			result.put("error", "Cannot delete group from database!");
			return internalServerError(result);
		}
		result.put("message", "Group deleted succesfully from database");
		return ok(result);
	}

	/**
	 * Gets the group.
	 *
	 * @param groupId
	 *            the group id
	 * @return the group
	 */
	public static Result getGroup(String groupId) {
		try {
			UserGroup group = DB.getUserGroupDAO().get(new ObjectId(groupId));
			return ok(userGroupToJSON(group));
		} catch (Exception e) {
			log.error("Cannot retrieve group from database!", e);
			return internalServerError("Cannot retrieve group from database!");
		}
	}

	/**
	 * @param name
	 *            the group name
	 * @return the result
	 */
	public static Result findByGroupName(String name, String collectionId) {
		Function<UserGroup, Status> getGroupJson = (UserGroup group) -> {
			ObjectNode groupJSON = Json.newObject();
			groupJSON.put("groupId", group.getDbId().toString());
			groupJSON.put("username", group.getUsername());
			groupJSON.put("about", group.getAbout());
			if (collectionId != null) {
				CollectionObject collection = DB.getCollectionObjectDAO()
						.getById(new ObjectId(collectionId));
				if (collection != null) {
					Access accessRights = collection.getAdministrative()
							.getAccess().getAcl(group.getDbId());
					if (accessRights != null)
						groupJSON.put("accessRights", accessRights.toString());
					else
						groupJSON.put("accessRights", Access.NONE.toString());
				}
			}
			return ok(groupJSON);
		};
		UserGroup group = DB.getUserGroupDAO().getByName(name);
		return getGroupJson.apply(group);
	}

	public static ArrayNode groupsAsJSON(List<UserGroup> groups,
			ObjectId restrictedById, boolean collectionHits) {
		ArrayNode result = Json.newObject().arrayNode();
		for (UserGroup group : groups) {
			ObjectNode g = (ObjectNode) Json.toJson(group);
			if (collectionHits) {
				Query<CollectionObject> q = DB.getCollectionObjectDAO()
						.createQuery();
				Criteria criteria1 = DB.getCollectionObjectDAO()
						.formAccessLevelQuery(
								new Tuple(restrictedById, Access.READ),
								QueryOperator.GTE);
				Criteria criteria2 = DB.getCollectionObjectDAO()
						.formAccessLevelQuery(
								new Tuple(group.getDbId(), Access.WRITE),
								QueryOperator.GTE);
				// Criteria criteria3 =
				// DB.getCollectionObjectDAO().createQuery()
				// .criteria("administrative.access.isPublic").equal(true);
				q.and(criteria1, criteria2);
				Tuple<Integer, Integer> hits = DB.getCollectionObjectDAO()
						.getHits(q, Optional.ofNullable(null));
				g.put("totalCollections", hits.x);
				g.put("totalExhibitions", hits.y);
			}
			boolean add = true;
			for (int i = 0; i < result.size(); i++) {
				if (group.getDbId().toString()
						.equals(result.get(i).get("dbId").asText())) {
					add = false;
					break;
				}
			}
			if (add)
				result.add(g);
		}
		return result;
	}

	public static Set<UserGroup> recursiveDescendants(Set<UserGroup> list,
			GroupType type) {
		Set<UserGroup> descendantGroups = new HashSet<UserGroup>();
		for (UserGroup group : list) {
			List<UserGroup> descendants = DB.getUserGroupDAO().findByParent(
					group.getDbId(), type);
			if (descendants != null) {
				Set<UserGroup> descendantsSet = recursiveDescendants(
						new HashSet<UserGroup>(descendants), type);
				descendantGroups.addAll(descendantsSet);
			}
			descendantGroups.add(group);
		}
		return descendantGroups;
	}

	/**
	 * Return child groups or all descendant groups according to group type.
	 *
	 * @param groupId
	 * @param groupType
	 * @param direct
	 * @param collectionHits
	 * @return
	 */
	public static Result getDescendantGroups(String groupId, String groupType,
			boolean direct, boolean collectionHits) {
		List<UserGroup> childrenGroups;
		List<UserGroup> descendantGroups;
		ObjectId parentId = new ObjectId(groupId);
		GroupType type = GroupType.valueOf(capitalizeFirst(groupType));
		childrenGroups = DB.getUserGroupDAO().findByParent(parentId, type);
		if (childrenGroups != null) {
			if (direct) {
				return ok(groupsAsJSON(childrenGroups, new ObjectId(groupId),
						collectionHits));
			} else {
				descendantGroups = new ArrayList<UserGroup>(
						recursiveDescendants(new HashSet<UserGroup>(
								childrenGroups), type));
				return ok(groupsAsJSON(descendantGroups, new ObjectId(groupId),
						collectionHits));
			}
		} else
			return ok();
	}

	/**
	 * This call returns extra info about members of a group either they are
	 * users or groups.
	 *
	 * Category specifies either if we want only users information or groups or
	 * both.
	 *
	 * @param groupId
	 * @param category
	 *            (possible values: 'users', 'groups', 'both'
	 * @return A json stucture like the following { "users": [ {...},
	 *         {...},...], "groups": [ {...}, {...},...] }
	 */
	public static Result getGroupUsersInfo(String groupId, String category) {

		ObjectNode result = Json.newObject();
		ArrayNode users = Json.newObject().arrayNode();
		ArrayNode groups = Json.newObject().arrayNode();

		if (!category.equals("users") && !category.equals("groups")
				&& !category.equals("both")) {

			result.put("message", "Invalid category name");
			log.error("Invalid category name");
			return badRequest(result);
		}

		UserGroup group;
		if ((group = DB.getUserGroupDAO().get(new ObjectId(groupId))) == null) {
			result.put("message", "There is no such group");
			log.error("There is no such group");
			return badRequest(result);
		}
		if ((category.equals("users") || category.equals("both"))
				&& (group.getUsers().size() >= group.getAdminIds().size())) {
		//	group.getUsers().removeAll(group.getAdminIds());
			User u;
			for (ObjectId oid : group.getUsers()) {
				if ((u = DB.getUserDAO().get(oid)) == null) {
					log.error("No User with dbId: " + oid);
				}
				ObjectNode userJSON = userOrGroupJson(u);
				if (group.getAdminIds().contains(oid))
					userJSON.put("admin", true);
				else
					userJSON.put("admin", false);
				users.add(userJSON);
			}
		}
		if ((category.equals("groups") || category.equals("both"))) {
			List<UserGroup> children = DB.getUserGroupDAO().findByParent(
					new ObjectId(groupId), GroupType.All);
			for (UserGroup g : children) {
				groups.add(userOrGroupJson(g));
			}
		}

		result.put("users", users);
		result.put("groups", groups);
		return ok(result);
	}
	
	private static UserGroup checkGroupAccess( String groupId, Access acc, RedeemablePromise<Result> res ) {
		UserGroup ug = DB.getUserGroupDAO().get(new ObjectId(groupId));
		if( ug == null ) { 
			ObjectNode on = Json.newObject()
					.put( "error", "Unknown Group");
			res.success(badRequest(on));
			return null;
		}
		// any of the effective users is an admin?
		if( effectiveUser() != null )
			if( effectiveUser().isSuperUser()) return ug;
		
		// read access
		if( acc == Access.READ ) {
			if( !ug.isPrivateGroup()) return ug;
			// need to hav the effectiveId
			for( ObjectId effId: effectiveUserDbIds() ) {
				if( effId.equals(ug.getDbId())) return ug;
			}
			ObjectNode on = Json.newObject()
					.put( "error", "Read access needs membership.");
			res.success(badRequest(on));
			
			return null;
		}

		// write or own access
		if( ug.getAdminIds().contains(effectiveUserDbId())) return ug;
		ObjectNode on = Json.newObject()
				.put( "error", "No write access to this group.");
		res.success(badRequest(on));
		return null;
	}
	
//	DELETE	/group/:groupId/uiSettings				controllers.GroupManager.deleteUiSettings( groupId, key:String )
	/**
	 * Delete the key from uiSettings. Doesnt complain when key is not there, only when there is
	 * no access. Logic is, if the key is not there, after this operation it is not there, exactly as expected.
	 * @param groupId
	 * @param key
	 * @return
	 */
	public static Promise<Result> deleteUiSettings( String groupId, String key ) throws Exception {
		// we want to updat the groupid ... write lock it
		Locks l = null;
		try {
			l  = Locks.create()
					.write( groupId )
					.acquire();
			RedeemablePromise<Result> res = RedeemablePromise.empty();
			UserGroup gr = checkGroupAccess( groupId, Access.WRITE, res );
			if( gr == null ) return res;
			Project pr = (Project) gr;

			ObjectNode uiSettings= pr.getUiSettings();
			if( uiSettings == null  ) {
				return	Promise.pure( ok() );
			}
			// remove from uisettings
			uiSettings.remove( key );
			// and back to db
			DB.getUserGroupDAO().makePermanent( pr );
			return Promise.pure( ok());
		} catch( Exception e ) {
			log.error( "Problem during deleteUiSettings!", e );
			throw e;
		} finally {
			if( l != null ) {
				l.release();
			}
		}
	}
	
//	GET		 /group/:groupId/uiSettings				controllers.GroupManager.getUiSettings( groupId, key )
	/**
	 * Return part of uiSettings or if key is empty, return all of uiSettings
	 * @param groupId
	 * @param key
	 * @return
	 */
	public static Promise<Result> getUiSettings( String groupId, String key ) throws Exception {
		Locks l = null;
		try {
			l  = Locks.create()
					.read( groupId )
					.acquire();
		RedeemablePromise<Result> res = RedeemablePromise.empty();
		UserGroup gr = checkGroupAccess( groupId, Access.READ, res );
		if( gr == null ) return res;
		Project pr = (Project) gr;
		
		ObjectNode uiSettings= pr.getUiSettings();
		if( StringUtils.isEmpty(key)) return Promise.pure( ok( uiSettings ));
		if( uiSettings == null  ) return Promise.pure( ok( Json.newObject()));

		JsonNode jn = uiSettings.get( key );
		return Promise.pure(ok( jn ));
		}  finally {
			if( l != null ) l.release();
		}
	}

//	PUT		/group/:groupId/uiSettings				controllers.GroupManager.updateUiSettings( groupId,  key:String )
	public static Promise<Result> updateUiSettings( String groupId, String key ) throws Exception {
		Locks l = null;
		try {
			l  = Locks.create()
					.write( groupId )
					.acquire();
			JsonNode json = request().body().asJson();

			RedeemablePromise<Result> res = RedeemablePromise.empty();
			UserGroup gr = checkGroupAccess( groupId, Access.WRITE, res );
			if( gr == null ) return res;
			Project pr = (Project) gr;

			ObjectNode uiSettings= pr.getUiSettings();
			if( uiSettings == null  ) {
				pr.setUiSettings( Json.newObject());
				uiSettings = pr.getUiSettings();
			}
			// store in uisettings
			uiSettings.put( key,  json );
			// and back to db
			DB.getUserGroupDAO().makePermanent( pr );
			return Promise.pure( ok());
		} catch( Exception e ) {
			log.error( "Problem during updateUiSettings!", e );
			throw e;
		} finally {
			if( l!= null ) l.release();
		}
	}
	
	
	private static ObjectNode userOrGroupJson(UserOrGroup user) {
		ObjectNode userJSON = Json.newObject();
		userJSON.put("userId", user.getDbId().toString());
		userJSON.put("username", user.getUsername());
		if (user instanceof User) {
			userJSON.put("category", "user");
			userJSON.put("firstName", ((User) user).getFirstName());
			userJSON.put("lastName", ((User) user).getLastName());
		} else
			userJSON.put("category", "group");
		String image = UserAndGroupManager.getImageBase64(user);
		if (image != null) {
			userJSON.put("image", image);
		}
		return userJSON;
	}

	public static ObjectNode userGroupToJSON(UserGroup group) {
		ObjectNode g = (ObjectNode) Json.toJson(group);

		ObjectId userId = effectiveUserDbId();
		if(userId != null) {
			User user = DB.getUserDAO().get(userId);
			g.put("firstName", user.getFirstName());
			g.put("lastName", user.getLastName());
		}
		Query<CollectionObject> q = DB.getCollectionObjectDAO().createQuery();
		// Criteria criteria1 =
		// DB.getCollectionObjectDAO().formAccessLevelQuery(new
		// Tuple(restrictedById, Access.READ), QueryOperator.GTE);
		Criteria criteria2 = DB.getCollectionObjectDAO().formAccessLevelQuery(
				new Tuple(group.getDbId(), Access.READ), QueryOperator.GT);
		// Criteria criteria3 = DB.getCollectionObjectDAO().createQuery()
		// .criteria("administrative.access.isPublic").equal(true);
		// q.and(criteria1, criteria2);
		q.and(criteria2);
		Tuple<Integer, Integer> hits = DB.getCollectionObjectDAO().getHits(q,
				Optional.ofNullable(null));
		ObjectNode count = Json.newObject();
		count.put("Collections", hits.x);
		count.put("Exhibitions", hits.y);
		g.put("count", count);
		return g;
	}

	public static ArrayNode userGroupsToJSON(List<UserGroup> groups) {
		ArrayNode result = Json.newObject().arrayNode();
		for (UserGroup group : groups) {
			result.add(userGroupToJSON(group));
		}
		return result;
	}

	public static ObjectNode groupsWithCount(List<UserGroup> groups,
			int groupCount) {
		ObjectNode result = Json.newObject();
		result.put("groups", userGroupsToJSON(groups));
		result.put("groupCount", groupCount);
		return result;
	}

	/**
	 * Retrieve the WITH space from the DB (with its UI settings)
	 * Although its private everybody is allowed to get it...
	 * Its private so it doesnt appear on normal project / space listings  
	 * @return
	 */
	public static Result getWithSpace() {
		UserGroup with = DB.getUserGroupDAO().findOne("username", "with");
		if( with != null ) {
			return ok( userGroupToJSON(with));
		}
		return badRequest( "WITH space missing!");
	}
	
	public static Result getSpace(String name) {
		UserGroup with = DB.getUserGroupDAO().findOne("username", name);
		if( with != null ) {
			return ok( userGroupToJSON(with));
		}
		return badRequest( "Space missing!");
	}
	
	/**
	 * Take the argument json and store it in the WITH space. Only superusers
	 * are allowed to do this.
	 * This implementation needs sanitation of the passed json (ideally together with any other group
	 * edit method )
	 * @return
	 */
	public static Result editWithSpace() {
		if( ! isSuperUser()) return badRequest( "Forbidden!");
		JsonNode json = request().body().asJson();
		UserGroup with = DB.getUserGroupDAO().findOne("username", "with");
		if( with != null ) {
			DB.getUserGroupDAO().editGroup(with.getDbId(), json);
			return getWithSpace();
		}  else {
			// create a with space
			Project p = new Project();
			ObjectMapper om = new ObjectMapper();
			p.setFriendlyName("WITH default space");
			p.setUsername("with");
			// default empty settings might not be good
			p.setUiSettings(om.createObjectNode());
			Query<User> adminQuery = DB.getUserDAO().createQuery();
			adminQuery.field( "superUser" ).equal(true );
			QueryResults<User> res = DB.getUserDAO().find( adminQuery );
			for( User u:res ) {
				p.addAdministrator(u.getDbId());
				p.setCreator(u.getDbId());
			}
			p.setPrivateGroup(true);
			DB.getUserGroupDAO().save(p);
			DB.getUserGroupDAO().editGroup( p.getDbId(), json );
			return getWithSpace();
		}
	}
	
	
	public static Result listUserGroups(String groupType, int offset,
			int count, boolean belongsOnly, String prefix) {

		List<UserGroup> groups = new ArrayList<UserGroup>();
		try {
			GroupType type = GroupType.valueOf(groupType);
			ObjectId userId = effectiveUserDbId();
			int userGroupCount = Math.toIntExact(DB.getUserGroupDAO().countPublic(type, prefix));
			if (userId == null) {
				if(prefix.equals("*")) {
					groups = DB.getUserGroupDAO().findPublic(type, offset, count);
					return ok(groupsWithCount(groups, userGroupCount));
				} else {
					groups = DB.getUserGroupDAO().findPublicByPrefix(type, prefix, offset, count);
					return ok(groupsWithCount(groups, userGroupCount));
				}
			}
			User user = DB.getUserDAO().get(userId);
			Set<ObjectId> userGroupsIds = user.getUserGroupsIds();
			if(prefix.equals("*")) {
				groups = DB.getUserGroupDAO().findByIds(userGroupsIds, type,
						offset, count);
			} else {
				groups = DB.getUserGroupDAO().findByIdsAndPrefix(userGroupsIds, type,
						prefix, offset, count);
			}
			userGroupCount = DB.getUserGroupDAO().getGroupCount(
					userGroupsIds, type);
			if (groups.size() == count)
				return ok(groupsWithCount(groups, userGroupCount));
			if (offset < userGroupCount)
				offset = 0;
			else
				offset = offset - userGroupCount;
			count = count - groups.size();
			if (!belongsOnly)
				groups.addAll(DB.getUserGroupDAO().findPublicWithRestrictionsAndPrefix(
						type, offset, count, userGroupsIds, prefix));
			return ok(groupsWithCount(groups, userGroupCount));
		} catch (Exception e) {
			return ok(groupsWithCount(groups, groups.size()));
		}
	}
}