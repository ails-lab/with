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

import model.Collection;
import model.CollectionMetadata;
import model.User;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.Query;

import play.Logger;
import play.Logger.ALogger;

public class CollectionDAO extends DAO<Collection> {
	public static final ALogger log = Logger.of(CollectionDAO.class);

	public CollectionDAO() {
		super(Collection.class);
	}

	public List<Collection> getCollectionsByIds(List<ObjectId> ids) {
		Query<Collection> colQuery = this.createQuery().field("_id")
				.hasAnyOf(ids);
		return find(colQuery).asList();
	}

	public Collection getByTitle(String title) {
		return this.findOne("title", title);
	}

	public Collection getByOwnerAndTitle(ObjectId ownerId, String title) {
		Query<Collection> q = this.createQuery().field("ownerId")
				.equal(ownerId).field("title").equal(title);
		return this.findOne(q);
	}

	public Collection getById(ObjectId id) {
		Query<Collection> q = this.createQuery().field("_id").equal(id);
		return findOne(q);
	}

	public List<Collection> getByOwner(ObjectId id) {
		return getByOwner(id, 0, 1);
	}

	public List<Collection> getByOwner(ObjectId ownerId, int offset, int count) {
		Query<Collection> q = this.createQuery().field("ownerId")
				.equal(ownerId).offset(offset).limit(count);
		return this.find(q).asList();
	}

	public List<Collection> getByReadAccess(ObjectId userId, int offset,
			int count) {
		Query<Collection> q = this.createQuery().offset(offset).limit(count);
		Criteria[] critiria = {
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("OWN"),
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("WRITE"),
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("READ"),
				this.createQuery().criteria("isPublic").equal(true) };
		q.or(critiria);
		return this.find(q).asList();
	}

	public List<Collection> getByWriteAccess(ObjectId userId, int offset,
			int count) {
		Query<Collection> q = this.createQuery().offset(offset).limit(count);
		Criteria[] critiria = {
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("OWN"),
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("WRITE") };
		q.or(critiria);
		return this.find(q).asList();
	}

	public List<Collection> getByReadAccessFiltered(ObjectId userId,
			ObjectId ownerId, int offset, int count) {
		Query<Collection> q = this.createQuery().field("ownerId")
				.equal(ownerId).offset(offset).limit(count);
		Criteria[] critiria = {
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("OWN"),
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("WRITE"),
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("READ"),
				this.createQuery().criteria("isPublic").equal(true) };
		q.or(critiria);
		return this.find(q).asList();
	}

	public List<Collection> getByWriteAccessFiltered(ObjectId userId,
			ObjectId ownerId, int offset, int count) {
		Query<Collection> q = this.createQuery().field("ownerId")
				.equal(ownerId).offset(offset).limit(count);
		Criteria[] critiria = {
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("OWN"),
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("WRITE") };
		q.or(critiria);
		return this.find(q).asList();
	}

	public List<Collection> getSharedFiltered(ObjectId userId,
			ObjectId ownerId, int offset, int count) {
		Query<Collection> q = this.createQuery().field("ownerId")
				.equal(ownerId).offset(offset).limit(count);
		Criteria[] critiria = {
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("WRITE"),
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("READ").criteria("isPublic").equal(false) };
		q.or(critiria);
		return this.find(q).asList();
	}

	public List<Collection> getShared(ObjectId userId, int offset, int count) {
		Query<Collection> q = this.createQuery().field("ownerId")
				.notEqual(userId).offset(offset).limit(count);
		Criteria[] critiria = {
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("WRITE"),
				this.createQuery().criteria("rights." + userId.toHexString())
						.equal("READ").criteria("isPublic").equal(false) };
		q.or(critiria);
		return this.find(q).asList();
	}

	public List<Collection> getPublicFiltered(ObjectId ownerId, int offset,
			int count) {
		Query<Collection> q = this.createQuery().field("isPublic").equal(true)
				.field("ownerId").equal(ownerId).offset(offset).limit(count);
		return this.find(q).asList();
	}

	public List<Collection> getPublic(int offset, int count) {
		Query<Collection> q = this.createQuery().field("isPublic").equal(true);
		return this.find(q).asList();
	}

	public User getCollectionOwner(ObjectId id) {
		Query<Collection> q = this.createQuery().field("_id").equal(id)
				.retrievedFields(true, "ownerId");
		return findOne(q).retrieveOwner();
	}

	public int removeById(ObjectId id) {

		Collection c = get(id);

		User owner = c.retrieveOwner();
		for (CollectionMetadata colMeta : owner.getCollectionMetadata()) {
			if ((colMeta.getCollectionId() != null)
					&& colMeta.getCollectionId().equals(id)) {
				owner.getCollectionMetadata().remove(colMeta);
				DB.getUserDAO().makePermanent(owner);
				break;
			}
		}

		DB.getCollectionRecordDAO().deleteByCollection(id);
		return makeTransient(c);
	}
}
