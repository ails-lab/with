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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import model.resources.collection.CollectionObject;
import model.usersAndGroups.Organization;
import model.usersAndGroups.Page;
import model.usersAndGroups.Project;
import model.usersAndGroups.User;
import model.usersAndGroups.UserGroup;

import org.bson.types.ObjectId;
import org.mongodb.morphia.geo.Point;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

import play.Logger;
import play.Logger.ALogger;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.GroupManager.GroupType;

public class UserGroupDAO extends DAO<UserGroup> {
	public static final ALogger log = Logger.of(UserGroupDAO.class);

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
				.field("privateGroup").equal(false).offset(offset).limit(count)
				.order("-created");
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
				.in(groupIds).offset(offset).limit(count).order("-created");
		if (!groupType.equals(GroupType.All)) {
			q.field("className").equal(
					"model.usersAndGroups." + groupType.toString());
		}
		List<UserGroup> groups = null;
		try {
			groups = find(q).asList();
		} catch (Exception e) {
			groups = new ArrayList<UserGroup>();
		}

		return groups;
	}

	public int getGroupCount(Set<ObjectId> groupIds, GroupType groupType) {
		if (groupIds.isEmpty())
			return 0;
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
				.field("privateGroup").equal(false).offset(offset).limit(count)
				.order("-created");

		if (!excludedIds.isEmpty())
			q.field("_id").notIn(excludedIds);

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

	//Is this fast or should we use ElasticSearch
	public List<UserGroup> getByGroupNamePrefix(String prefix) {
		Query<UserGroup> q = this.createQuery().field("username")
				.startsWithIgnoreCase(prefix);
		return find(q).asList();

	}

	//Is this fast or should we use ElasticSearch
	public List<UserGroup> getByFriendlyNamePrefix(String prefix) {
		Query<UserGroup> q = this.createQuery().field("friendlyName")
				.startsWithIgnoreCase(prefix);
		return find(q).asList();
	}

	public void setCreated() {
		Query<UserGroup> q = this.createQuery().field("created").doesNotExist();
		UpdateOperations<UserGroup> updateOps = this.createUpdateOperations();
		updateOps.set("created", new Date());
		List<UserGroup> a = find(q).asList();
		for (UserGroup g : a) {
			q = this.createQuery().field("_id").equal(g.getDbId());
			this.update(q, updateOps);
		}
	}

	public void editGroup(ObjectId groupId, JsonNode json) {
		Query<UserGroup> q = this.createQuery().field("_id").equal(groupId);
		UpdateOperations<UserGroup> updateOps = this.createUpdateOperations();
		updateFields("", json, updateOps);
		this.update(q, updateOps);
	}

	public void updatePageCoordinates(ObjectId groupId, Point point) {
		Query<UserGroup> q = this.createQuery().field("_id").equal(groupId);
		UpdateOperations<UserGroup> updateOps = this.createUpdateOperations()
				.disableValidation();
		if (point != null)
			updateOps.set("page.coordinates", point);
		else
			updateOps.unset("page.coordinates");
		this.update(q, updateOps);
	}

	public int updateFeatured(ObjectId groupId, List<ObjectId> fCols, List<ObjectId> fExhs, String op) {
		Query<UserGroup> q = this.createQuery().field("_id").equal(groupId);
		UpdateOperations<UserGroup> updateOps = this.createUpdateOperations()
				.disableValidation();
		if(op.equals("+")) {
			if(fCols.size() != 0)
				updateOps.addAll("page.featuredCollections", fCols, false);
			if(fExhs.size() != 0)
				updateOps.addAll("page.featuredExhibitions", fExhs, false);
		} else if(op.equals("-")) {
			if(fCols.size() != 0)
				updateOps.removeAll("page.featuredCollections", fCols);
			if(fExhs.size() != 0)
				updateOps.removeAll("page.featuredExhibitions", fExhs);
		} else {
			log.error("This operations is not supported when updating featured");
		}

		return this.update(q, updateOps).getUpdatedCount();
	}

	public void findUrlsFromAvatars(Set<String> urls) {
		log.info("Retrieving urls from user avatars");
		Iterator<User> userIterator = DB.getUserDAO().createQuery().iterator();
		int i = 1;
		while (userIterator.hasNext()) {
			User user = userIterator.next();
			log.info("Getting the urls for #" + i++ + " user");
			if ((user.getAvatar() != null) && !user.getAvatar().isEmpty())
				urls.addAll(user.getAvatar().values());
		}
		log.info("Retrieving urls from group avatars");
		Iterator<UserGroup> groupIterator = DB.getUserGroupDAO().createQuery()
				.iterator();
		i = 1;
		while (groupIterator.hasNext()) {
			UserGroup group = groupIterator.next();
			log.info("Getting the urls for #" + i++ + " group");
			if ((group.getAvatar() != null) && !group.getAvatar().isEmpty())
				urls.addAll(group.getAvatar().values());
		}
	}

	public void findUrlsFromCovers(Set<String> urls) {
		log.info("Retrieving urls from organizations/projects covers");
		Iterator<UserGroup> groupIterator = DB.getUserGroupDAO().createQuery()
				.iterator();
		int i = 1;
		while (groupIterator.hasNext()) {
			log.info("Getting the urls for #" + i++ + " group");
			UserGroup group = groupIterator.next();
			Page page = null;
			if (group instanceof Organization)
				page = ((Organization) group).getPage();
			if (group instanceof Project)
				page = ((Project) group).getPage();
			if ((page == null) || (page.getCover() == null)
					|| page.getCover().isEmpty())
				continue;
			urls.addAll(page.getCover().values());
		}
	}
	
	public void addFeatured(ObjectId groupId, List<ObjectId> featured) {
		Query<UserGroup> q = this.createQuery().field("_id").equal(groupId);
		UpdateOperations<UserGroup> updateOps = this
				.createUpdateOperations().disableValidation();

	}
	
	
	public void deleteFeatured(ObjectId groupId, List<ObjectId> featured) {
		
	}
}
