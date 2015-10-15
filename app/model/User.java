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


package model;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import controllers.GroupManager.GroupType;
import db.DB;
import play.Logger;
import play.Logger.ALogger;

@Entity
public class User extends UserOrGroup {

	public static final ALogger log = Logger.of(User.class);

	private static final int EMBEDDED_CAP = 20;

	private enum Gender {
		MALE, FEMALE, UNSPECIFIED
	}

	private String email;

	private String firstName;
	private String lastName;

	private Gender gender;
	private String facebookId;
	private String googleId;

	private String md5Password;

	private boolean superUser;
	// we should experiment here with an array of fixed-size
	// We keep a complete search history, but have the first
	// k entries in here as a copy
	@Embedded
	private List<Search> searchHistory = new ArrayList<Search>();
	@Embedded
	private Page page;

	private int exhibitionsCreated;

	private final Set<ObjectId> userGroupsIds = new HashSet<ObjectId>();

	/**
	 * The search should already be stored in the database separately
	 *
	 * @param search
	 */
	public void addToHistory(Search search) {
		if (search.getDbID() == null) {
			log.error("Search is  not saved!");
			return;
		}

		searchHistory.add(search);
		if (searchHistory.size() > EMBEDDED_CAP) {
			searchHistory.remove(0);
		}
	}

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

	public void recalculateGroups() {
		Set<ObjectId> groupAcc = new HashSet<ObjectId>();
		// get all groups I'm in
		List<UserGroup> gr = DB.getUserGroupDAO().findByUserIdAll(this.getDbId(), GroupType.All);
		for (UserGroup ug : gr) {
			groupAcc.add(ug.getDbId());
			ug.accumulateGroups(groupAcc);
		}
		getUserGroupsIds().clear();
		getUserGroupsIds().addAll(groupAcc);
	}

	// getter setter

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

	public List<Search> getSearchHistory() {
		return searchHistory;
	}

	public void setSearchHistory(List<Search> searcHistory) {
		this.searchHistory = searcHistory;
	}

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
		switch (gender) {
		case FEMALE:
			return "Female";
		case MALE:
			return "Male";
		default:
			return "Unspecified";

		}

	}

	public void setGender(String gender) {
		if (gender.equalsIgnoreCase("female")) {
			this.gender = Gender.FEMALE;
		} else if (gender.equalsIgnoreCase("male")) {
			this.gender = Gender.MALE;
		} else {
			this.gender = Gender.UNSPECIFIED;
		}
	}

	public String getGoogleId() {
		return googleId;
	}

	public void setGoogleId(String googleId) {
		this.googleId = googleId;
	}

	public void addUserGroups(Set<ObjectId> groups) {
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
	
	@JsonIgnore
	public boolean isSuperUser() {
		return superUser;
	}

	@JsonIgnore
	public void setSuperUser(boolean isSuperUser) {
		this.superUser = isSuperUser;
	}
	
	@JsonIgnore
	public int getExhibitionsCreated() {
		return exhibitionsCreated;
	}

	@JsonIgnore
	public void setExhibitionsCreated(int exhibitionsCreated) {
		this.exhibitionsCreated = exhibitionsCreated;
	}

	public void addExhibitionsCreated() {
		this.exhibitionsCreated++;
	}

}
