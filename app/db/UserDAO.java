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
import java.util.List;

import model.usersAndGroups.User;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import play.Logger;
import play.Logger.ALogger;
import utils.Tuple;

public class UserDAO extends DAO<User> {
	public static final ALogger log = Logger.of(UserDAO.class);

	public UserDAO() {
		super(User.class);
	}

	public User getByEmail(String email) {
		return this.findOne("email", email);
	}

	public User getByUsername(String username) {
		return this.findOne("username", username);
	}

	public User getByFacebookId(String facebookId) {
		return this.findOne("facebookId", facebookId);
	}

	public User getByGoogleId(String googleId) {
		return this.findOne("googleId", googleId);
	}

	/**
	 * This method is updating one specific User. By default update method is
	 * invoked to all documents of a collection.
	 **/
	private void setSpecificUserField(ObjectId dbId, String fieldName,
			String value) {
		Query<User> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<User> updateOps = this.createUpdateOperations();
		updateOps.set(fieldName, value);
		this.update(q, updateOps);
	}

	/**
	 * Retrieve a user from his credentials
	 *
	 * @param email
	 * @param pass
	 * @return
	 */
	public User getByEmailPassword(String email, String pass) {
		Query<User> q = this.createQuery();
		String md5Pass = User.computeMD5(email, pass);
		q.and(q.criteria("email").equal(email), q.criteria("md5Password")
				.equal(md5Pass));
		return find(q).get();
	}

	//Is this fast or should we use ElasticSearch
	public List<User> getByUsernamePrefix(String prefix) {
		Query<User> q = this.createQuery().field("username").startsWithIgnoreCase(prefix);
		return find(q).asList();

	}

	public List<String> getAllUsernames() {
		ArrayList<String> res = new ArrayList<String>();
		withCollection(res, "", "username");
		return res;
	}

	public int removeById(ObjectId id) {
		User user = getById(id, null);

		// delete user realted searches
		/*List<Search> userSearches = user.getSearchHistory();
		for (Search s : userSearches)
			DB.getSearchDAO().makeTransient(s);*/

		// delete user related collections
		/*
		 * List<ObjectId> collectionIds = user.getCollectionIds(); for (ObjectId
		 * cid: collectionIds) DB.getCollectionDAO().removeById(cid);
		 */

		return this.makeTransient(user);
	}

	public List<User> getByGroupId(ObjectId groupId) {
		Query<User> q = this.createQuery().field("userGroupsIds")
				.hasThisOne(groupId);
		return find(q).asList();
	}

	public boolean isSuperUser(ObjectId id) {
		ArrayList<Tuple<String, Object>> superUser = new ArrayList<Tuple<String, Object>>();
		superUser.add(new Tuple("_id", id));
		superUser.add(new Tuple("superUser", true));
		return existsFieldsWithValues(superUser);
	}
}
