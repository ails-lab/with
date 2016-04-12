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


package model.usersAndGroups;

import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

import model.resources.collection.CollectionObject;
import notifications.Notification;

import org.apache.commons.codec.binary.Hex;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import utils.Serializer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import controllers.CollectionObjectController;
import db.DB;

@Entity
@Indexes({
		@Index(fields = @Field(value = "email", type = IndexType.ASC), options = @IndexOptions()),
		@Index(fields = @Field(value = "username", type = IndexType.ASC), options = @IndexOptions()),
		@Index(fields = @Field(value = "username", type = IndexType.TEXT), options = @IndexOptions(background = true, name = "username_text")),
		@Index(fields = @Field(value = "facebookId", type = IndexType.ASC), options = @IndexOptions()),
		@Index(fields = @Field(value = "googleId", type = IndexType.ASC), options = @IndexOptions()),
		@Index(fields = @Field(value = "userGroupsIds", type = IndexType.ASC), options = @IndexOptions()) })
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class User extends UserOrGroup {

	public static final ALogger log = Logger.of(User.class);

	private enum Gender {
		MALE("Male"), FEMALE("Female"), UNSPECIFIED("Unspecified");

		private final String name;

		private Gender(String name) {
			this.name = name;
		}

		public static Gender getGender(String string) {
			for (Gender v : Gender.values()) {
				if (v.toString().equals(string))
					return v;
			}
			return UNSPECIFIED;
		}

		@Override
		public String toString() {
			return name;
		}

	}

	private String email;

	private String firstName;
	private String lastName;

	private Gender gender;

	private String facebookId;
	private String googleId;
	@JsonIgnore
	private String md5Password;
	@JsonIgnore
	private boolean superUser;
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId favorites;
	// we should experiment here with an array of fixed-size
	// We keep a complete search history, but have the first
	// k entries in here as a copy
	/*
	 * @Embedded private List<Search> searchHistory = new ArrayList<Search>();
	 */
	@Embedded
	private Page page;

	// @JsonIgnore
	// private int exhibitionsCreated;

	@JsonSerialize(using = Serializer.ObjectIdArraySerializer.class)
	private final Set<ObjectId> userGroupsIds = new HashSet<ObjectId>();

	@JsonSerialize(using = Serializer.ObjectIdArraySerializer.class)
	private final Set<ObjectId> adminInGroups = new HashSet<ObjectId>();

	/**
	 * The search should already be stored in the database separately
	 *
	 * @param search
	 */
	/*
	 * public void addToHistory(Search search) { if (search.getDbID() == null) {
	 * log.error("Search is  not saved!"); return; }
	 * 
	 * searchHistory.add(search); if (searchHistory.size() > EMBEDDED_CAP) {
	 * searchHistory.remove(0); } }
	 */

	/**
	 * md5 the password and set it in the right field
	 *
	 * @param password
	 */
	public void setPassword(String password) {
		if (!password.isEmpty()) {
			String pass = computeMD5(this.getEmail(), password);
			this.setMd5Password(pass);
		}
	}

	/**
	 * Is this the right password for the user?
	 *
	 * @param password
	 * @return
	 */
	public boolean checkPassword(String password) {
		String md5 = computeMD5(this.getEmail(), password);
		return md5.equals(getMd5Password());
	}

	/**
	 * Computes the MD5 with email for this password. Use when authenticating a
	 * user via password.
	 *
	 * @param password
	 * @return
	 */
	public static String computeMD5(String email, String password) {
		String salted = email + " - " + password;
		try {
			MessageDigest d = MessageDigest.getInstance("MD5");
			String res = Hex.encodeHexString(d.digest(salted.getBytes("UTF8")));
			return res;
		} catch (Exception e) {
			log.error("MD5 problem.", e);
		}
		return "";
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@JsonIgnore
	public String getMd5Password() {
		return md5Password;
	}

	@JsonIgnore
	public void setMd5Password(String md5Password) {
		this.md5Password = md5Password;
	}

	/*
	 * public List<Search> getSearchHistory() { return searchHistory; }
	 * 
	 * public void setSearchHistory(List<Search> searcHistory) {
	 * this.searchHistory = searcHistory; }
	 */

	public Page getPage() {
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
	}

	public String getFacebookId() {
		return facebookId;
	}

	public void setFacebookId(String facebookId) {
		this.facebookId = facebookId;
	}

	public String getGender() {
		if (gender == null)
			return null;
		return gender.toString();
	}

	public void setGender(String gender) {
		this.gender = Gender.getGender(gender);
	}

	public String getGoogleId() {
		return googleId;
	}

	public void setGoogleId(String googleId) {
		this.googleId = googleId;
	}

	public void addUserGroups(Set<ObjectId> groups) {
		groups.addAll(userGroupsIds);
		this.userGroupsIds.clear();
		this.userGroupsIds.addAll(groups);
	}

	public void removeUserGroups(Set<ObjectId> groups) {
		this.userGroupsIds.removeAll(groups);
	}

	public void addUserGroup(ObjectId group) {
		this.userGroupsIds.add(group);
	}

	public void removeUserGroup(ObjectId group) {
		this.userGroupsIds.remove(group);
	}

	public Set<ObjectId> getUserGroupsIds() {
		return userGroupsIds;
	}

	public void addGroupForAdministration(ObjectId group) {
		this.adminInGroups.add(group);
	}

	public void removeGroupForAdministration(ObjectId group) {
		this.adminInGroups.remove(group);
	}

	public Set<ObjectId> getAdminInGroups() {
		return this.adminInGroups;
	}

	public boolean isSuperUser() {
		return superUser;
	}

	public void setSuperUser(boolean isSuperUser) {
		this.superUser = isSuperUser;
	}

	/*
	 * public int getExhibitionsCreated() { return exhibitionsCreated; }
	 * 
	 * public void setExhibitionsCreated(int exhibitionsCreated) {
	 * this.exhibitionsCreated = exhibitionsCreated; }
	 * 
	 * public void addExhibitionsCreated() { this.exhibitionsCreated++; }
	 */

	public ArrayNode getOrganizations() {
		ArrayNode groups = Json.newObject().arrayNode();
		try {
			for (ObjectId groupId : userGroupsIds) {
				UserGroup group = DB.getUserGroupDAO().get(groupId);
				if (group instanceof Organization) {
					ObjectNode groupInfo = Json.newObject();
					groupInfo.put("id", groupId.toString());
					groupInfo.put("username", group.getUsername());
					if (group.getFriendlyName() == null)
						groupInfo.put("friendlyName", group.getUsername());
					else
						groupInfo.put("friendlyName", group.getFriendlyName());
					groups.add(groupInfo);
				}
			}
			return groups;
		} catch (Exception e) {
			return groups;
		}

	}

	public ArrayNode getProjects() {
		ArrayNode groups = Json.newObject().arrayNode();
		try {
			for (ObjectId groupId : userGroupsIds) {
				UserGroup group = DB.getUserGroupDAO().get(groupId);
				if (group instanceof Project) {
					ObjectNode groupInfo = Json.newObject();
					groupInfo.put("id", groupId.toString());
					groupInfo.put("username", group.getUsername());
					if (group.getFriendlyName() == null)
						groupInfo.put("friendlyName", group.getUsername());
					else
						groupInfo.put("friendlyName", group.getFriendlyName());
					groups.add(groupInfo);
				}
			}
			return groups;
		} catch (Exception e) {
			return groups;
		}
	}

	public ArrayNode getUsergroups() {
		ArrayNode groups = Json.newObject().arrayNode();
		try {
			for (ObjectId groupId : userGroupsIds) {
				UserGroup group = DB.getUserGroupDAO().get(groupId);
				if (group.getClass().equals(UserGroup.class)) {
					ObjectNode groupInfo = Json.newObject();
					groupInfo.put("id", groupId.toString());
					groupInfo.put("username", group.getUsername());
					if (group.getFriendlyName() == null)
						groupInfo.put("friendlyName", group.getUsername());
					else
						groupInfo.put("friendlyName", group.getFriendlyName());
					groups.add(groupInfo);
				}
			}
			return groups;
		} catch (Exception e) {
			return groups;
		}
	}

	public Set<Notification> getNotifications() {
		try {
			Set<ObjectId> userOrGroupIds = new HashSet<ObjectId>();
			userOrGroupIds.add(this.getDbId());
			userOrGroupIds.addAll(this.adminInGroups);
			Set<Notification> unreadNotifications = new HashSet<Notification>(
					DB.getNotificationDAO().getUnreadByReceivers(
							userOrGroupIds, 0));
			Set<Notification> notifications = new HashSet<Notification>();
			if (unreadNotifications.size() < 20) {
				notifications = new HashSet<Notification>(DB
						.getNotificationDAO().getAllByReceivers(userOrGroupIds,
								20 - unreadNotifications.size()));
				notifications.addAll(unreadNotifications);
			} else {
				notifications = unreadNotifications;
			}
			return notifications;
		} catch (Exception e) {
			return new HashSet<Notification>();
		}
	}

	public ObjectId getFavorites() {
		if (favorites != null)
			return favorites;
		CollectionObject favoriteCol = DB.getCollectionObjectDAO()
				.getByOwnerAndLabel(this.getDbId(), null, "_favorites");
		if (favoriteCol != null)
			return favoriteCol.getDbId();
		return CollectionObjectController.createFavorites(this.getDbId());
	}

	public void setFavorites(ObjectId favorites) {
		this.favorites = favorites;
	}
}
