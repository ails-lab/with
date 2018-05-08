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

import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;

import utils.Serializer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import db.DB;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Indexes({
	@Index(fields = @Field(value = "username", type = IndexType.ASC), options = @IndexOptions()),
	@Index(fields = @Field(value = "username", type = IndexType.TEXT), options = @IndexOptions(background=true, name="username_text")),
	@Index(fields = @Field(value = "users", type = IndexType.ASC), options = @IndexOptions()),
	@Index(fields = @Field(value = "parentGroups", type = IndexType.ASC), options = @IndexOptions())
	})
public class UserGroup extends UserOrGroup {

	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId creator;

	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private final Set<ObjectId> adminIds = new HashSet<ObjectId>();
	private boolean privateGroup;

	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private final Set<ObjectId> users = new HashSet<ObjectId>();

	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private final Set<ObjectId> parentGroups = new HashSet<ObjectId>();

	private String friendlyName;

	public Set<ObjectId> getAdminIds() {
		return adminIds;
	}

	public void addAdministrators(Set<ObjectId> administrators) {
		this.adminIds.addAll(administrators);
	}

	public void addAdministrator(ObjectId administrator) {
		this.adminIds.add(administrator);
	}

	public void removeAdministrator(ObjectId administrator) {
		this.adminIds.remove(administrator);
	}

	public Set<ObjectId> getUsers() {
		return users;
	}

	public void addUser(ObjectId user) {
		this.getUsers().add(user);
	}

	public void removeUser(ObjectId user) {
		this.getUsers().remove(user);
	}

	public Set<ObjectId> getParentGroups() {
		return parentGroups;
	}

	@JsonIgnore
	public Set<ObjectId> getAncestorGroups() {
		Set<ObjectId> ancestors = new HashSet<ObjectId>();
		ancestors.addAll(parentGroups);
		for (ObjectId gid : parentGroups) {
			UserGroup g = DB.getUserGroupDAO().get(gid);
			if ((g != null) && !g.getAncestorGroups().isEmpty())
				ancestors.addAll(g.getParentGroups());
		}

		return ancestors;
	}

	public void accumulateGroups(Set<ObjectId> groupAcc) {
		groupAcc.addAll(getAncestorGroups());
		for (ObjectId grId : getAncestorGroups()) {
			UserGroup ug = DB.getUserGroupDAO().get(grId);
			if (ug != null)
				ug.accumulateGroups(groupAcc);
		}
	}

	public boolean isPrivateGroup() {
		return privateGroup;
	}

	public void setPrivateGroup(boolean privateGroup) {
		this.privateGroup = privateGroup;
	}

	public String getFriendlyName() {
		return friendlyName;
	}

	public void setFriendlyName(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	public ObjectId getCreator() {
		return creator;
	}

	public void setCreator(ObjectId creator) {
		this.creator = creator;
	}
	
	public String getType() {
		return "UserGroup";
	}
}
