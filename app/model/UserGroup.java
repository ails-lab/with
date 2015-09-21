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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;

import utils.Serializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import db.DB;

public class UserGroup {

	@Id
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId dbId;
	private final Set<ObjectId> adminIds = new HashSet<ObjectId>();
	private String name;
	private String description;
	private boolean privateGroup;
	private ObjectId thumbnail;

	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private final List<ObjectId> users = new ArrayList<ObjectId>();

	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private final List<ObjectId> ancestorGroups = new ArrayList<ObjectId>();

	public ObjectId getDbId() {
		return dbId;
	}

	public void setDbId(ObjectId dbId) {
		this.dbId = dbId;
	}

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

	public List<ObjectId> getUsers() {
		return users;
	}

	// id of parent user groups
	public List<ObjectId> getAncestorGroups() {
		return ancestorGroups;
	}

	public Set<ObjectId> retrieveParents() {
		Set<ObjectId> ancestors = new HashSet<ObjectId>();
		ancestors.addAll(ancestorGroups);
		for (ObjectId gid : ancestorGroups) {
			UserGroup g = DB.getUserGroupDAO().get(gid);
			if ((g != null) && !g.getAncestorGroups().isEmpty())
				ancestors.addAll(g.retrieveParents());
		}

		return ancestors;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void accumulateGroups(Set<ObjectId> groupAcc) {
		groupAcc.addAll(getAncestorGroups());
		for (ObjectId grId : getAncestorGroups()) {
			UserGroup ug = DB.getUserGroupDAO().get(grId);
			if (ug != null)
				ug.accumulateGroups(groupAcc);
		}
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String desc) {
		this.description = desc;
	}

	public ObjectId getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(ObjectId thumbnail) {
		this.thumbnail = thumbnail;
	}

	public boolean isPrivateGroup() {
		return privateGroup;
	}

	public void setPrivateGroup(boolean privateGroup) {
		this.privateGroup = privateGroup;
	}

}
