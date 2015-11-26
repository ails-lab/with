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

import org.bson.types.ObjectId;
import org.elasticsearch.common.lang3.ArrayUtils;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.CriteriaContainer;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryResults;
import org.mongodb.morphia.query.UpdateOperations;

import utils.Tuple;
import model.Collection;
import model.basicDataTypes.WithAccess.Access;
import model.resources.CollectionObject;
import model.usersAndGroups.User;

public class CollectionObjectDAO extends DAO<CollectionObject> {

	public CollectionObjectDAO() {
		super(CollectionObject.class);
	}

	/**
	 * Given a list of ObjectId's (dbId's)
	 * return the specified  CollectionObject's
	 * @param ids
	 * @return
	 */
	public List<CollectionObject> getCollectionsByIds(List<ObjectId> ids) {
		Query<CollectionObject> colQuery = this.createQuery().field("administative._id")
				.hasAnyOf(ids);
		return find(colQuery).asList();
	}

	/**
	 * List all CollectionObjects with the title provided for the language specified
	 * @param title
	 * @return
	 */
	public List<CollectionObject> getByTitle(String title, String lang) {
		if(lang == null) lang = "en";
		Query<CollectionObject> q = this.createQuery().field("descriptivedata.title." + lang)
				.equal(title);
		return this.find(q).asList();
	}

	/**
	 * Return a specific page of CollectionObject
	 * according to the offset and count provided
	 * MongoDB's paging infrastructure is used.
	 * @param offset
	 * @param count
	 * @return
	 */
	public List<CollectionObject> getAll(int offset, int count) {
		Query<CollectionObject> q = this.createQuery().offset(offset).limit(count);
		return this.find(q).asList();
	}

	/**
	 * Get a user's CollectionObject according to the title given
	 * @param creatorId
	 * @param title
	 * @return
	 */
	public CollectionObject getByOwnerAndTitle(ObjectId creatorId, String title, String lang) {
		if(lang == null) lang = "en";
		Query<CollectionObject> q = this.createQuery().field("administative.withCreator")
				.equal(creatorId).field("descriptivedata.title." + lang).equal(title);
		return this.findOne(q);
	}

	/**
	 * Get a CollectionObject by the dbId
	 * @param id
	 * @return
	 */
	public CollectionObject getById(ObjectId id) {
		Query<CollectionObject> q = this.createQuery().field("administative._id").equal(id);
		return findOne(q);
	}

	/**
	 * Get a CollectionObject by the dbId and retrieve
	 * only a bounch of fields from the whole document
	 * @param id
	 * @param retrievedFields
	 * @return
	 */
	public CollectionObject getById(ObjectId id, List<String> retrievedFields) {
		Query<CollectionObject> q = this.createQuery().field("administative._id").equal(id);
		if (retrievedFields != null)
			for (int i = 0; i < retrievedFields.size(); i++)
				q.retrievedFields(true, retrievedFields.get(i));
		return this.findOne(q);

	}

	/**
	 * Get the first CollectionObject that a user has created
	 * using the creator's/owner's id.
	 * We are using MongoDB's paging.
	 * @param id
	 * @return
	 */
	public List<CollectionObject> getByOwner(ObjectId id) {
		return getByOwner(id, 0, 1);
	}

	/**
	 * Get all CollectionObject using the creator's/owner's id.
	 * @param creatorId
	 * @param offset
	 * @param count
	 * @return
	 */
	public List<CollectionObject> getByOwner(ObjectId creatorId, int offset, int count) {
		Query<CollectionObject> q = this.createQuery().field("administative.withCreator")
				.equal(creatorId).field("isExhibition").equal(false)
				.offset(offset).limit(count);
		return this.find(q).asList();
	}

	/**
	 * Create a Mongo access query criteria
	 * @param userAccess
	 * @return
	 */
	public Criteria formAccessLevelQuery(Tuple<ObjectId, Access> userAccess) {
		int ordinal = userAccess.y.ordinal();
		/*Criteria[] criteria = new Criteria[Access.values().length-ordinal];
		for (int i=0; i<Access.values().length-ordinal; i++)
			criteria[i] = this.createQuery().criteria("rights." + userAccess.x.toHexString())
			.equal(Access.values()[i+ordinal].toString());*/
		return this.createQuery().criteria("administrative.access." + userAccess.x.toHexString()).greaterThanOrEq(ordinal);
	}

	/**
	 * Create Mongo access criteria for the current logged in user
	 * @param loggedInUserEffIds
	 * @return
	 */
	public CriteriaContainer formLoggedInUserQuery(List<ObjectId> loggedInUserEffIds) {
		int ordinal = Access.READ.ordinal();
		Criteria[] criteria = new Criteria[loggedInUserEffIds.size()+1];
		for (int i=0; i<loggedInUserEffIds.size(); i++) {
			criteria[i] = this.createQuery().criteria("rights." + loggedInUserEffIds.get(i)).greaterThanOrEq(ordinal);
		}
		criteria[loggedInUserEffIds.size()] = this.createQuery().criteria("rights.isPublic").equal(true);
		return this.createQuery().or(criteria);
	}

	/**
	 * Create general Mongo access criteria for users-access level specified
	 * @param filterByUserAccess
	 * @return
	 */
	public CriteriaContainer formQueryAccessCriteria(List<Tuple<ObjectId, Access>> filterByUserAccess) {
		Criteria[] criteria = new Criteria[0];
		for (Tuple<ObjectId, Access> userAccess: filterByUserAccess) {
			criteria = ArrayUtils.addAll(criteria, formAccessLevelQuery(userAccess));
		}
		return this.createQuery().or(criteria);
	}

	/**
	 * Create a basic Mongo query with withCreator field matching, offset, limit and criteria.
	 * @param criteria
	 * @param creator
	 * @param isExhibition
	 * @param offset
	 * @param count
	 * @return
	 */
	public Query<CollectionObject> formBasicQuery(CriteriaContainer[] criteria, ObjectId creator, Boolean isExhibition,  int offset, int count) {
		Query<CollectionObject> q = this.createQuery().offset(offset).limit(count+1);
		if (creator != null)
			q.field("administrative.withCreator").equal(creator);
		if (criteria.length > 0)
			q.and(criteria);
		return q;
	}

	/**
	 * Return a tuple containing a list of CollectionObjects (usually bounded from a limit)
	 * together with the total number of entities corresponded to the query.
	 * @param q
	 * @param isExhibition
	 * @return
	 */
	public Tuple<List<CollectionObject>, Tuple<Integer, Integer>> getCollectionsAndHits(Query<CollectionObject> q,
			Boolean isExhibition) {

		Tuple<Integer, Integer> hits = new Tuple<Integer, Integer>(0, 0);
		QueryResults<CollectionObject> result;
		List<CollectionObject> collections = new ArrayList<CollectionObject>();
		if (isExhibition == null) {
			result = this.find(q);
			collections = result.asList();
			Query<CollectionObject> q2 = q.cloneQuery();
			q2.field("isExhibition").equal(true);
			q.field("isExhibition").equal(false);
			hits.x = (int) this.find(q).countAll();
			hits.y = (int) this.find(q2).countAll();
		}
		else {
			q.field("isExhibition").equal(isExhibition);
			result = this.find(q);
			collections = result.asList();
			if (isExhibition)
				hits.y = (int) result.countAll();
			else
				hits.x = (int) result.countAll();
		}
		return new Tuple<List<CollectionObject>, Tuple<Integer, Integer>>(collections, hits);
	}

	/**
	 * Return the total number of CollectionObject entities for a specific query
	 * @param q
	 * @param isExhibition
	 * @return
	 */
	public Tuple<Integer, Integer> getHits(Query<CollectionObject> q, Boolean isExhibition) {
		Tuple<Integer, Integer> hits = new Tuple<Integer, Integer>(0, 0);
		if (isExhibition == null) {
			Query<CollectionObject> q2 = q.cloneQuery();
			q2.field("isExhibition").equal(true);
			q.field("isExhibition").equal(false);
			hits.x = (int) this.find(q).countAll();
			hits.y = (int) this.find(q2).countAll();
		}
		else {
			q.field("isExhibition").equal(isExhibition);
			if (isExhibition)
				hits.y = (int) this.find(q).countAll();
			else
				hits.x = (int)  this.find(q).countAll();
		}
		return hits;
	}

	/**
	 * Return all CollectionObjects (usually bounded by a limit) some user access criteria.
	 * The method can be parametrised to return also the total number of entities for the specified query.
	 * @param accessedByUserOrGroup
	 * @param creator
	 * @param isExhibition
	 * @param totalHits
	 * @param offset
	 * @param count
	 * @return
	 */
	public Tuple<List<CollectionObject>, Tuple<Integer, Integer>>  getByAccess(List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup, ObjectId creator,
			Boolean isExhibition, boolean totalHits, int offset, int count) {
		CriteriaContainer[] criteria =  new CriteriaContainer[0];
		for (List<Tuple<ObjectId, Access>> orAccessed: accessedByUserOrGroup) {
			criteria = ArrayUtils.addAll(criteria, formQueryAccessCriteria(orAccessed));
		}
		Query<CollectionObject> q = formBasicQuery(criteria, creator, isExhibition, offset, count);
		if (totalHits) {
			return getCollectionsAndHits(q, isExhibition);
		}
		else {
			if (isExhibition != null)
				q.field("isExhibition").equal(isExhibition);
			return new Tuple<List<CollectionObject>, Tuple<Integer, Integer>>(this.find(q).asList(), null);
		}
	}

	/**
	 * Return all CollectionObjects (usually bounded by a limit) that satisfy the loggin user's access
	 * criteria and optionally some other user access criteria. Typically all the CollectionObject that a user has access.
	 * The method can be parametrised to return also the total number of entities for the specified query.
	 * @param loggeInEffIds
	 * @param accessedByUserOrGroup
	 * @param creator
	 * @param isExhibition
	 * @param totalHits
	 * @param offset
	 * @param count
	 * @return
	 */
	public Tuple<List<CollectionObject>, Tuple<Integer, Integer>>  getByAccess(List<ObjectId> loggeInEffIds,
			List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup, ObjectId creator,
			Boolean isExhibition, boolean totalHits, int offset, int count) {

		CriteriaContainer[] criteria =  new CriteriaContainer[0];
		criteria = ArrayUtils.addAll(criteria, formLoggedInUserQuery(loggeInEffIds));
		for (List<Tuple<ObjectId, Access>> orAccessed: accessedByUserOrGroup) {
			criteria = ArrayUtils.addAll(criteria, formQueryAccessCriteria(orAccessed));
		}
		Query<CollectionObject> q = formBasicQuery(criteria, creator, isExhibition, offset, count);
		if (totalHits) {
			return getCollectionsAndHits(q, isExhibition);
		}
		else {
			if (isExhibition != null)
				q.field("isExhibition").equal(isExhibition);
			return new Tuple<List<CollectionObject>, Tuple<Integer, Integer>>(this.find(q).asList(), null);
		}
	}

	/**
	 * Return all CollectionObjects (usually bounded by a limit) of a user that satisfy some user
	 * access criteria (that are shared with some users).
	 * The method can be parametrised to return also the total number of entities for the specified query.
	 * @param userId
	 * @param accessedByUserOrGroup
	 * @param isExhibition
	 * @param totalHits
	 * @param offset
	 * @param count
	 * @return
	 */
	public Tuple<List<CollectionObject>, Tuple<Integer, Integer>> getShared(ObjectId userId, List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup,
			Boolean isExhibition,  boolean totalHits, int offset, int count) {

		Query<CollectionObject> q = this.createQuery().offset(offset).limit(count+1);
		q.field("administrative.withCreator").notEqual(userId);
		CriteriaContainer[] criteria =  new CriteriaContainer[0];
		for (List<Tuple<ObjectId, Access>> orAccessed: accessedByUserOrGroup) {
			criteria = ArrayUtils.addAll(criteria ,formQueryAccessCriteria(orAccessed));
		}
		if (criteria.length > 0)
			q.and(criteria);
		if (totalHits) {
			return getCollectionsAndHits(q, isExhibition);
		}
		else {
			if (isExhibition != null)
				q.field("isExhibition").equal(isExhibition);
			return new Tuple<List<CollectionObject>, Tuple<Integer, Integer>>(this.find(q).asList(), null);
		}
	}

	/**
	 * Return all public CollectionObjects (usually bounded by a limit) that also satisfy some user access criteria.
	 * The method can be parametrised to return also the total number of entities for the specified query.
	 * @param accessedByUserOrGroup
	 * @param creator
	 * @param isExhibition
	 * @param totalHits
	 * @param offset
	 * @param count
	 * @return
	 */
	public Tuple<List<CollectionObject>, Tuple<Integer, Integer>> getPublic(List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup, ObjectId creator,
			Boolean isExhibition,  boolean totalHits, int offset, int count) {

		Query<CollectionObject> q = this.createQuery().offset(offset).limit(count+1);
		if (creator != null)
			q.field("administrative.withCreator").equal(creator);
		Criteria[] criteria = {this.createQuery().criteria("administrative.access.isPublic").equal(true)};
		for (List<Tuple<ObjectId, Access>> orAccessed: accessedByUserOrGroup) {
			criteria = ArrayUtils.addAll(criteria ,formQueryAccessCriteria(orAccessed));
		}
		if (criteria.length > 0)
			q.and(criteria);
		if (totalHits) {
			return getCollectionsAndHits(q, isExhibition);
		}
		else {
			if (isExhibition != null)
				q.field("isExhibition").equal(isExhibition);
			return new Tuple<List<CollectionObject>, Tuple<Integer, Integer>>(this.find(q).asList(), null);
		}
	}

	/**
	 * Retrieve the owner/creator of a CollectionObject
	 * using collection's dbId
	 * @param id
	 * @return
	 */
	public User getCollectionOwner(ObjectId id) {
		Query<CollectionObject> q = this.createQuery().field("administative._id").equal(id)
				.retrievedFields(true, "administative.withCreator");
		return findOne(q).retrieveCreator();
	}

	/**
	 * Remove a CollectionObject and all collected resources using the dbId
	 * @param id
	 * @return
	 */
	public int removeById(ObjectId id) {

		CollectionObject c = get(id);
		DB.getCollectionRecordDAO().deleteByCollection(id);
		return makeTransient(c);
	}

	/**
	 * Increment the specified field in a CollectionObject
	 * @param dbId
	 * @param fieldName
	 */
	public void incField(ObjectId dbId, String fieldName) {
		Query<CollectionObject> q = this.createQuery().field("administative._id").equal(dbId);
		UpdateOperations<CollectionObject> updateOps = this.createUpdateOperations();
		updateOps.inc(fieldName);
		this.update(q, updateOps);
	}

	/**
	 * Decrement the specified field in a CollectionObject
	 * @param dbId
	 * @param fieldName
	 */
	public void decField(ObjectId dbId, String fieldName) {
		Query<CollectionObject> q = this.createQuery().field("administative._id").equal(dbId);
		UpdateOperations<CollectionObject> updateOps = this.createUpdateOperations();
		updateOps.dec(fieldName);
		this.update(q, updateOps);
	}

	/**
	 * Increment entryCount (number of entries collected) in a CollectionObject
	 * @param dbId
	 */
	public void incEntryCount(ObjectId dbId) {
		incField(dbId, "administrative.entryCount");
	}

	/**
	 * Decrement entryCount (number of entries collected) in a CollectionObject
	 * @param dbId
	 */
	public void decEntryCount(ObjectId dbId) {
		decField(dbId, "administrative.entryCount");
	}
}
