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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import model.basicDataTypes.WithAccess.Access;
import model.resources.CollectionObject;
import model.resources.RecordResource;

import org.bson.types.ObjectId;
import org.elasticsearch.common.lang3.ArrayUtils;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.CriteriaContainer;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryResults;
import org.mongodb.morphia.query.UpdateOperations;

import utils.Tuple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CollectionObjectDAO extends WithResourceDAO<CollectionObject> {

	/*
	 * The constructor is optional because the explicit type is passed through
	 * generics.
	 */
	public CollectionObjectDAO() {
		super(CollectionObject.class);
	}

	/**
	 * Increment entryCount (number of entries collected) in a CollectionObject
	 * 
	 * @param dbId
	 */
	public void incEntryCount(ObjectId dbId) {
		Query<CollectionObject> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<CollectionObject> updateOps = this
				.createUpdateOperations();
		updateOps.set("administrative.lastModified", new Date());
		updateOps.inc("administrative.entryCount");
		this.update(q, updateOps);
	}

	/**
	 * Decrement entryCount (number of entries collected) in a CollectionObject
	 * 
	 * @param dbId
	 */
	public void decEntryCount(ObjectId dbId) {
		Query<CollectionObject> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<CollectionObject> updateOps = this
				.createUpdateOperations();
		updateOps.set("administrative.lastModified", new Date());
		updateOps.dec("administrative.entryCount");
		this.update(q, updateOps);
	}
	
	public List<RecordResource> getFirstEntries(ObjectId dbId, int upperBound) {
		return DB.getRecordResourceDAO().getByCollectionBetweenPositions(dbId, 0, upperBound);
	}


	public void editCollection(ObjectId dbId, JsonNode json) {
		Query<CollectionObject> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<CollectionObject> updateOps = this
				.createUpdateOperations();
		updateFields("", json, updateOps);
		updateOps.set("administrative.lastModified", new Date());
		this.update(q, updateOps);
	}

	public void updateFields(String parentField, JsonNode node,
			UpdateOperations<CollectionObject> updateOps) {
		Iterator<String> fieldNames = node.fieldNames();
		  while (fieldNames.hasNext()) {
	         String fieldName = fieldNames.next();
	         JsonNode fieldValue = node.get(fieldName);
        	 String newFieldName = parentField.isEmpty() ? fieldName : parentField + "." + fieldName;
	         if (fieldValue.isObject()) {
	        	 updateFields(newFieldName, fieldValue, updateOps);
	         }
	         else {//value
				try {
					ObjectMapper mapper = new ObjectMapper();
					Object value = mapper.treeToValue(fieldValue, newFieldName.getClass());
					updateOps.disableValidation().set(newFieldName, value);
				} catch (IOException e) {
					e.printStackTrace();
				}	 
	         }
	     }
	}
	
	/**
	 * Gets the union of the collections/exhibitions for a list of users and
	 * groups for a specific right
	 * 
	 * @param effectiveIds
	 * @param access
	 * @param isExhibition
	 * @param offset
	 * @param count
	 * @return the list of these collections
	 */
	public List<CollectionObject> getBySpecificAccess(
			List<ObjectId> effectiveIds, Access access, Boolean isExhibition,
			int offset, int count) {
		Query<CollectionObject> q = this.createQuery()
				.field("administrative.isExhibition").equal(isExhibition)
				.order("-administrative.lastModified").offset(offset)
				.limit(count);
		Criteria[] criteria = new Criteria[effectiveIds
				.size()];
		for (int i = 0; i < effectiveIds.size(); i++) {
			criteria[i] = formAccessLevelQuery(new Tuple(effectiveIds.get(i), Access.READ), QueryOperator.EQ);
		}
		q.or(criteria);
		return this.find(q).asList();
	}

	public List<CollectionObject> getByMaxAccess(List<ObjectId> effectiveIds,
			Access access, Boolean isExhibition, int offset, int count) {
		Query<CollectionObject> q = this.createQuery()
				.order("-administrative.lastModified").offset(offset)
				.limit(count);
		Criteria[] criteria = new Criteria[effectiveIds
				.size()];
		for (int i = 0; i < effectiveIds.size(); i++) {
			criteria[i] = formAccessLevelQuery(new Tuple(effectiveIds.get(i), access), QueryOperator.GTE);
		}
		q.field("administrative.isExhibition").equal(isExhibition);
		q.or(criteria);
		return this.find(q).asList();
	}

	public List<CollectionObject> getPublic(Boolean isExhibition, int offset,
			int count) {
		Query<CollectionObject> q = this.createQuery()
				.field("administrative.access.isPublic").equal(true)
				.order("-administrative.lastModified").offset(offset)
				.limit(count);
		q.field("administrative.isExhibition").equal(isExhibition);
		return this.find(q).asList();
	}

	public List<CollectionObject> getByAccessWithRestrictions(
			List<ObjectId> effectiveIds, QueryOperator op1,  Access access,
			Map<ObjectId, Access> restrictions, QueryOperator op2, Boolean isExhibition,
			int offset, int count) {
		Query<CollectionObject> q = this.createQuery()
				.field("administrative.isExhibition").equal(isExhibition)
				.order("-administrative.lastModified").offset(offset)
				.limit(count);
		Criteria[] criteriaOr = new Criteria[effectiveIds.size()];
		for (int i = 0; i < effectiveIds.size(); i++) {
			criteriaOr[i] = formAccessLevelQuery(new Tuple(effectiveIds.get(i), access), op1);
		}
		Criteria[] criteriaAnd = new Criteria[restrictions.size()];
		int i = 0;
		for (ObjectId id : restrictions.keySet()) {
			criteriaOr[i++] = formAccessLevelQuery(new Tuple(effectiveIds.get(i), restrictions.get(id)), op2);
		}
		q.or(criteriaOr);
		q.and(criteriaAnd);
		return this.find(q).asList();
		
	}
	
	public List<CollectionObject> getBySpecificAccessWithRestrictions(
			List<ObjectId> effectiveIds, Access access,
			Map<ObjectId, Access> restrictions, Boolean isExhibition,
			int offset, int count) {
		return getByAccessWithRestrictions(effectiveIds, QueryOperator.EQ, access, restrictions, QueryOperator.EQ, isExhibition, offset, count);
	}
	
	public List<CollectionObject> getByMaxAccessWithRestrictions(
			List<ObjectId> effectiveIds, Access access,
			Map<ObjectId, Access> restrictions, Boolean isExhibition,
			int offset, int count) {
		return getByAccessWithRestrictions(effectiveIds, QueryOperator.GTE, access, restrictions, QueryOperator.EQ, isExhibition, offset, count);
	}
	
	/**
	 * Return a tuple containing a list of CollectionObjects (usually bounded from a limit)
	 * together with the total number of entities corresponded to the query.
	 * @param q
	 * @param isExhibition
	 * @return
	 */
	public Tuple<List<CollectionObject>, Tuple<Integer, Integer>> getCollectionsWithCount(Query<CollectionObject> q,
			Boolean isExhibition) {
		Tuple<Integer, Integer> hits = new Tuple<Integer, Integer>(0, 0);
		QueryResults<CollectionObject> result;
		List<CollectionObject> collections = new ArrayList<CollectionObject>();
		if (isExhibition == null) {
			result = this.find(q);
			collections = result.asList();
			Query<CollectionObject> q2 = q.cloneQuery();
			q2.field("administrative.isExhibition").equal(true);
			q.field("administrative.isExhibition").equal(false);
			hits.x = (int) this.find(q).countAll();
			hits.y = (int) this.find(q2).countAll();
		}
		else {
			q.field("administrative.isExhibition").equal(isExhibition);
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
	public Tuple<Integer, Integer> getCollectionsCount(Query<CollectionObject> q, Boolean isExhibition) {
		Tuple<Integer, Integer> hits = new Tuple<Integer, Integer>(0, 0);
		if (isExhibition == null) {
			Query<CollectionObject> q2 = q.cloneQuery();
			q2.field("administrative.isExhibition").equal(true);
			q.field("administrative.isExhibition").equal(false);
			hits.x = (int) this.find(q).countAll();
			hits.y = (int) this.find(q2).countAll();
		}
		else {
			q.field("administrative.isExhibition").equal(isExhibition);
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
	public Tuple<List<CollectionObject>, Tuple<Integer, Integer>>  getByACL(List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup, ObjectId creator,
			Boolean isExhibition, boolean totalHits, int offset, int count) {
		CriteriaContainer[] criteria =  new CriteriaContainer[0];
		for (List<Tuple<ObjectId, Access>> orAccessed: accessedByUserOrGroup) {
			criteria = ArrayUtils.addAll(criteria, atLeastAccessCriteria(orAccessed));
		}
		Query<CollectionObject> q = formCreatorQuery(criteria, creator, offset, count);
		if (totalHits) {
			return getCollectionsWithCount(q, isExhibition);
		}
		else {
			if (isExhibition != null)
				q.field("administrative.isExhibition").equal(isExhibition);
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
	public Tuple<List<CollectionObject>, Tuple<Integer, Integer>>  getUsersAccessibleWithACL(List<ObjectId> loggeInEffIds,
			List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup, ObjectId creator,
			Boolean isExhibition, boolean totalHits, int offset, int count) {

		CriteriaContainer[] criteria =  new CriteriaContainer[0];
		criteria = ArrayUtils.addAll(criteria, loggedInUserWithAtLeastAccessQuery(loggeInEffIds, Access.READ));
		for (List<Tuple<ObjectId, Access>> orAccessed: accessedByUserOrGroup) {
			criteria = ArrayUtils.addAll(criteria, atLeastAccessCriteria(orAccessed));
		}
		Query<CollectionObject> q = formCreatorQuery(criteria, creator, offset, count);
		if (totalHits) {
			return getCollectionsWithCount(q, isExhibition);
		}
		else {
			if (isExhibition != null)
				q.field("administrative.isExhibition").equal(isExhibition);
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
	public Tuple<List<CollectionObject>, Tuple<Integer, Integer>> getSharedWithACL(ObjectId userId, List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup,
			Boolean isExhibition,  boolean totalHits, int offset, int count) {

		Query<CollectionObject> q = this.createQuery().offset(offset).limit(count+1);
		q.field("administrative.withCreator").notEqual(userId);
		CriteriaContainer[] criteria =  new CriteriaContainer[0];
		for (List<Tuple<ObjectId, Access>> orAccessed: accessedByUserOrGroup) {
			criteria = ArrayUtils.addAll(criteria ,atLeastAccessCriteria(orAccessed));
		}
		if (criteria.length > 0)
			q.and(criteria);
		if (totalHits) {
			return getCollectionsWithCount(q, isExhibition);
		}
		else {
			if (isExhibition != null)
				q.field("administrative.isExhibition").equal(isExhibition);
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
	public Tuple<List<CollectionObject>, Tuple<Integer, Integer>> getPublicWithACL(List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup, ObjectId creator,
			Boolean isExhibition,  boolean totalHits, int offset, int count) {

		Query<CollectionObject> q = this.createQuery().offset(offset).limit(count+1);
		if (creator != null)
			q.field("administrative.withCreator").equal(creator);
		Criteria[] criteria = {this.createQuery().criteria("administrative.access.isPublic").equal(true)};
		for (List<Tuple<ObjectId, Access>> orAccessed: accessedByUserOrGroup) {
			criteria = ArrayUtils.addAll(criteria ,atLeastAccessCriteria(orAccessed));
		}
		if (criteria.length > 0)
			q.and(criteria);
		if (totalHits) {
			return getCollectionsWithCount(q, isExhibition);
		}
		else {
			if (isExhibition != null)
				q.field("isExhibition").equal(isExhibition);
			return new Tuple<List<CollectionObject>, Tuple<Integer, Integer>>(this.find(q).asList(), null);
		}
	}
	
}
