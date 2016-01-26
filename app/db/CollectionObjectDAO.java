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
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import model.Collection;
import model.basicDataTypes.WithAccess.Access;
import model.resources.CollectionObject;
import model.resources.CollectionObject.CollectionAdmin.CollectionType;
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
	
	public boolean isFavorites(ObjectId dbId) {
		Query<CollectionObject> q = this.createQuery().field("_id").equal(dbId);
		q.field("descriptiveData.label.def").equal("favorites");
		return (this.find(q).asList().size()==0? false: true);
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
	
	public List<CollectionObject> getBySpecificAccess(
			List<ObjectId> effectiveIds, Access access, Boolean isExhibition,
			int offset, int count) {
		CollectionType collectionType = isExhibition ? CollectionType.Exhibition : CollectionType.SimpleCollection;
		Query<CollectionObject> q = this.createQuery()
				.field("administrative.collectionType").equal(collectionType)
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

	/*
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
	}*/

	/*public List<CollectionObject> getPublic(Boolean isExhibition, int offset,
			int count) {
		Query<CollectionObject> q = this.createQuery()
				.field("administrative.access.isPublic").equal(true)
				.order("-administrative.lastModified").offset(offset)
				.limit(count);
		q.field("administrative.isExhibition").equal(isExhibition);
		return this.find(q).asList();
	}*/
	
	
	public Tuple<List<CollectionObject>, Tuple<Integer, Integer>> getCollectionsWithCount(Query<CollectionObject> q,
			Boolean isExhibition) {
		Tuple<Integer, Integer> hits = new Tuple<Integer, Integer>(0, 0);
		QueryResults<CollectionObject> result;
		List<CollectionObject> collections = new ArrayList<CollectionObject>();
		if (isExhibition == null) {
			result = this.find(q);
			collections = result.asList();
			Query<CollectionObject> q2 = q.cloneQuery();
			q2.field("administrative.collectionType").equal(CollectionType.Exhibition);
			q.field("administrative.collectionType").equal(CollectionType.SimpleCollection);
			hits.x = (int) this.find(q).countAll();
			hits.y = (int) this.find(q2).countAll();
		}
		else {
			CollectionType collectionType = isExhibition ? CollectionType.Exhibition : CollectionType.SimpleCollection;
			q.field("administrative.collectionType").equal(collectionType);
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
			q2.field("administrative.collectionType").equal(CollectionType.Exhibition);
			q.field("administrative.collectionType").equal(CollectionType.SimpleCollection);
			hits.x = (int) this.find(q).countAll();
			hits.y = (int) this.find(q2).countAll();
		}
		else {
			CollectionType collectionType = isExhibition ? CollectionType.Exhibition : CollectionType.SimpleCollection;
			q.field("administrative.collectionType").equal(collectionType);
			if (isExhibition)
				hits.y = (int) this.find(q).countAll();
			else
				hits.x = (int)  this.find(q).countAll();
		}
		return hits;
	}
	

	/**
	 * Return CollectionObjects (bounded by a limit) that satisfy the logged in user's access
	 * criteria and optionally some other user access criteria.
	 * @param loggeInEffIds
	 * @param accessedByUserOrGroup
	 * @param creator
	 * @param isExhibition
	 * @param totalHits
	 * @param offset
	 * @param count
	 * @return
	 */
	public Tuple<List<CollectionObject>, Tuple<Integer, Integer>>  getByLoggedInUsersAndAcl(List<ObjectId> loggeInEffIds,
			List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup, ObjectId creator,
			Boolean isExhibition, boolean totalHits, int offset, int count) {
		List<Criteria> criteria =  new ArrayList<Criteria>(Arrays.asList(loggedInUserWithAtLeastAccessQuery(loggeInEffIds, Access.READ)));
		return getByAcl(criteria, accessedByUserOrGroup, creator, isExhibition, totalHits, offset, count);
	}

	
	/**
	 * Return public CollectionObjects (bounded by a limit) that also satisfy some user access criteria.
	 * @param accessedByUserOrGroup
	 * @param creator
	 * @param isExhibition
	 * @param totalHits
	 * @param offset
	 * @param count
	 * @return
	 */
	public Tuple<List<CollectionObject>, Tuple<Integer, Integer>> getByPublicAndAcl(List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup, ObjectId creator,
			Boolean isExhibition,  boolean totalHits, int offset, int count) {
		List<Criteria> criteria = new ArrayList<Criteria>(Arrays.asList(this.createQuery().criteria("administrative.access.isPublic").equal(true)));
		return getByAcl(criteria, accessedByUserOrGroup, creator, isExhibition, totalHits, offset, count);
	}
	
	
	public Tuple<List<CollectionObject>, Tuple<Integer, Integer>> getByAcl(List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup, ObjectId creator,
			Boolean isExhibition,  boolean totalHits, int offset, int count) {
		return getByAcl(new ArrayList<Criteria>(), accessedByUserOrGroup, creator, isExhibition, totalHits, offset, count);
	}
	
	public Tuple<List<CollectionObject>, Tuple<Integer, Integer>> getByAcl(List<Criteria> andCriteria, List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup, ObjectId creator,
			Boolean isExhibition,  boolean totalHits, int offset, int count) {
		Query<CollectionObject> q = this.createQuery().offset(offset).limit(count+1);
		if (creator != null)
			q.field("administrative.withCreator").equal(creator);
		for (List<Tuple<ObjectId, Access>> orAccessed: accessedByUserOrGroup) {
			andCriteria.add(atLeastAccessCriteria(orAccessed));
		}
		if (andCriteria.size() > 0)
			q.and(andCriteria.toArray(new Criteria[andCriteria.size()]));
		if (totalHits) {
			return getCollectionsWithCount(q, isExhibition);
		}
		else {
			if (isExhibition != null) {
				CollectionType collectionType = isExhibition ? CollectionType.Exhibition : CollectionType.SimpleCollection;
				q.field("administrative.collectionType").equal(collectionType);
			}
			return new Tuple<List<CollectionObject>, Tuple<Integer, Integer>>(this.find(q).asList(), null);
		}
	}
	
}
