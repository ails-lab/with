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


package db;

import java.util.Date;
import java.util.List;
import java.util.Set;

import model.resources.CollectionObject;
import model.usersAndGroups.UserGroup;
import controllers.GroupManager.GroupType;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import play.Logger;

public class UserGroupDAO extends DAO<UserGroup> {
	static private final Logger.ALogger log = Logger.of(UserGroup.class);

	public UserGroupDAO() {
		super(UserGroup.class);
	}

	public UserGroup getByName(String name) {
		return this.findOne("username", name);
	}

	public List<UserGroup> findByUserId(ObjectId userId, GroupType groupType,
			Boolean privateGroup) {
		Query<UserGroup> q = createQuery().disableValidation().field("users")
				.hasThisOne(userId);
		if (privateGroup != null)
			q.and(q.criteria("privateGroup").equal(privateGroup));
		if (groupType.equals(GroupType.All)) {
			return find(q).asList();
		}
		q.and(q.criteria("className").equal(
				"model.usersAndGroups." + groupType.toString()));
		return find(q).asList();
	}

	public List<UserGroup> findPublic(GroupType groupType, int offset, int count) {
		Query<UserGroup> q = createQuery().disableValidation()
				.field("privateGroup").equal(false).offset(offset).limit(count).order("");
		if (groupType.equals(GroupType.All)) {
			return find(q).asList();
		}
		q.and(q.criteria("className").equal(
				"model.usersAndGroups." + groupType.toString()));
		return find(q).asList();
	}

	public List<UserGroup> findByIds(Set<ObjectId> groupIds,
			GroupType groupType, int offset, int count) {
		Query<UserGroup> q = createQuery().disableValidation().field("_id")
				.in(groupIds).offset(offset).limit(count);
		if (!groupType.equals(GroupType.All)) {
			q.field("className").equal(
					"model.usersAndGroups." + groupType.toString());
		}
		return find(q).asList();
	}
	
	public int getGroupCount(Set<ObjectId> groupIds,
			GroupType groupType) {
		Query<UserGroup> q = createQuery().disableValidation().field("_id")
				.in(groupIds);
		if (!groupType.equals(GroupType.All)) {
			q.field("className").equal(
					"model.usersAndGroups." + groupType.toString());
		}
		return (int) count(q);		
	}

	public List<UserGroup> findPublicWithRestrictions(GroupType groupType,
			int offset, int count, Set<ObjectId> excludedIds) {
		Query<UserGroup> q = createQuery().disableValidation()
				.field("privateGroup").equal(false).field("_id")
				.notIn(excludedIds).offset(offset).limit(count);
		if (groupType.equals(GroupType.All)) {
			return find(q).asList();
		}
		q.and(q.criteria("className").equal(
				"model.usersAndGroups." + groupType.toString()));
		return find(q).asList();
	}

	public List<UserGroup> findByParent(ObjectId parentId, GroupType groupType) {
		Query<UserGroup> q = createQuery().disableValidation()
				.field("parentGroups").hasThisOne(parentId);
		if (groupType.equals(GroupType.All)) {
			return find(q).asList();
		}
		q.and(q.criteria("className").equal(
				"model.usersAndGroups." + groupType.toString()));
		return find(q).asList();
	}

	public List<UserGroup> getByGroupNamePrefix(String prefix) {
		Query<UserGroup> q = this.createQuery().field("username")
				.startsWith(prefix);
		return find(q).asList();

	}
	
	public void setCreated() {
		Query<UserGroup> q = this.createQuery().field("created").doesNotExist();
		UpdateOperations<UserGroup> updateOps = this
				.createUpdateOperations();
		updateOps.set("created", new Date());
		List<UserGroup> a = find(q).asList();
		for (UserGroup g :a) {
			q = this.createQuery().field("_id").equal(g.getDbId());
			this.update(q, updateOps);
		}
	}

}
