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

import java.util.List;

import model.usersAndGroups.UserGroup;
import controllers.GroupManager.GroupType;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;

import play.Logger;

public class UserGroupDAO extends DAO<UserGroup> {
	static private final Logger.ALogger log = Logger.of(UserGroup.class);

	public UserGroupDAO() {
		super(UserGroup.class);
	}

	public UserGroup getByName(String name) {
		return this.findOne("username", name);
	}

	public List<UserGroup> findByUserId(ObjectId userId,
			GroupType groupType, Boolean privateGroup) {
		Query<UserGroup> q = createQuery().disableValidation().field("users")
				.hasThisOne(userId);
		if (privateGroup != null)
			q.and(q.criteria("privateGroup").equal(privateGroup));
		if (groupType.equals(GroupType.All)) {
			return find(q).asList();
		}
		q.and(q.criteria("className").equal("model.usersAndGroups." + groupType.toString()));
		return find(q).asList();
	}


	public List<UserGroup> findByParent(ObjectId parentId, GroupType groupType) {
		Query<UserGroup> q = createQuery().disableValidation()
				.field("parentGroups").hasThisOne(parentId);
		if (groupType.equals(GroupType.All)) {
			return find(q).asList();
		}
		q.and(q.criteria("className").equal("model.usersAndGroups." + groupType.toString()));
		return find(q).asList();
	}

	public List<UserGroup> getByGroupNamePrefix(String prefix) {
		Query<UserGroup> q = this.createQuery().field("username")
				.startsWith(prefix);
		return find(q).asList();

	}

}
