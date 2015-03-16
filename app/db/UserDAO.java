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

import model.Collection;
import model.User;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import play.Logger;
import play.Logger.ALogger;

public class UserDAO extends DAO<User> {
	public static final ALogger log = Logger.of( UserDAO.class);

	public UserDAO() {
		super( User.class );
	}

	public User getById(String id) {
		Query<User> q = this.createQuery()
				.field("_id").equal(new ObjectId(id));
		return this.findOne(q);

	}

	public User getByEmail(String email) {
		return this.findOne("email", email);
	}

	public User getByDisplayName(String displayName) {
		return this.findOne("displayName", displayName);
	}

	/**
	 * This method is updating one specific User.
	 * By default update method is invoked to all documents of a collection.
	 **/
	private void setSpecificUserField(String dbId, String fieldName, String value) {
		Query<User> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<User> updateOps = this.createUpdateOperations();
		updateOps.set(fieldName, value);
		this.update(q, updateOps);
	}

	/**
	 * Retrieve a user from his credentials
	 * @param email
	 * @param pass
	 * @return
	 */
	public User getByEmailPassword(String email, String pass) {
		Query<User> q = this.createQuery();
		String md5Pass = User.computeMD5(email, pass);
		q.and(
			q.criteria("email").equal(email),
			q.criteria("md5Password").equal(md5Pass)
		);
		return find(q).get();
	}

	/**
	 * Return user collections
	 * @param email
	 * @return
	 */
	public List<Collection> getUserCollectionsByEmail(String email) {
		Query<User> q = this.createQuery()
				.field("email").equal(email)
				.retrievedFields(true, "collections.collection");

		return this.findOne(q).getUserCollections();
	}

	public List<String> getAllDisplayNames() {
		ArrayList<String> res = new ArrayList<String>();
		withCollection( res, "", "displayName");
		return res;
	}

	public int deleteById(String id) {
		Query<User> q = this.createQuery()
				.field("_id").equal(new ObjectId(id));
		return this.deleteByQuery(q).getN();
	}
}
