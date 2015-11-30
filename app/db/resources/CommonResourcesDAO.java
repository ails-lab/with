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


package db.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import model.basicDataTypes.WithAccess.Access;
import model.resources.CollectionObject;
import model.resources.WithResource;
import model.usersAndGroups.User;

import org.bson.types.ObjectId;
import org.elasticsearch.common.lang3.ArrayUtils;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.CriteriaContainer;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryResults;
import org.mongodb.morphia.query.UpdateOperations;

import utils.Tuple;

import com.mongodb.BasicDBObject;

import db.DAO;
import db.DB;

/*
 * The class consists of methods that can be both query 
 * a CollectionObject or a RecordResource_ (CollectionObject, 
 * CulturalObject, WithResource etc).
 * 
 * Special methods referring to one of these entities go to the 
 * specific DAO class.
 */
public abstract class CommonResourcesDAO<T extends WithResource> extends DAO<T>{
		
	/*
	 * The value of the entity class is either 
	 * CollectionObject.class or RecordResource.class
	 */
	public CommonResourcesDAO(Class<?> entityClass) {
		super(entityClass);
	}

	
	
	/**
	 * Retrieve an Object from DB using its dbId 
	 * @param id
	 * @return
	 */
	public T getById(ObjectId id) {
		Query<T> q = this.createQuery().field("_id").equal(id);
		return this.findOne(q);
	}
	
	/**
	 * Get a CollectionObject by the dbId and retrieve
	 * only a bunch of fields from the whole document
	 * @param id
	 * @param retrievedFields
	 * @return
	 */
	public T getById(ObjectId id, List<String> retrievedFields) {
		Query<T> q = this.createQuery().field("_id").equal(id);
		if (retrievedFields != null)
			for (int i = 0; i < retrievedFields.size(); i++)
				q.retrievedFields(true, retrievedFields.get(i));
		return this.findOne(q);

	}
	
	/**
	 * Remove a CollectionObject and all collected resources using the dbId
	 * @param id
	 * @return
	 */
	public int removeById(ObjectId id) {
		/*
		 * 0 - no documents returned
		 * * - number of documents returned 
		 */
		return this.deleteById(id).getN();
	}
	
	/**
	 * Return all resources that belong to a 'collection' throwing
	 * away duplicate entries in a 'collection'
	 * This methods is here cause in the future may a collection
	 * belong to a another collection.
	 * 
	 * 
	 * TODO: Return only some fields for these resources. 
	 * 
	 * @param colId
	 * @param offset
	 * @param count
	 * @return
	 */
	public List<T> getSingletonCollectedResources(ObjectId colId, int offset, int count) {
		Query<T> q = this.createQuery().field("collectedIn."+colId.toString()).exists()
				.offset(offset).limit(count);
		return this.find(q).asList();
	}

	/**
	 * Retrieve all records from specific collection checking
	 * out for duplicates and restore them.
	 *
	 * @param colId
	 * @return
	 */
	public List<T> getCollectedResources(ObjectId colId) {
		return getByCollectionOffsetCount(colId, 0, -1);
	}

	/**
	 * Retrieve records from specific collection by offset and count
	 * while restoring duplicate entries.
	 *
	 * @param colId, offset, count
	 * @return
	 */
	public List<T> getByCollectionOffsetCount(ObjectId colId,
			int offset, int count) {
		List<T> Ts = getSingletonCollectedResources(colId, offset, count);
		List<T> repeatedResources = new ArrayList<T>();
		for (T T: Ts) {
			ArrayList<Integer> positions = (ArrayList<Integer>) T.getCollectedIn().get(colId);
			if (positions.size() > 1)
				for (int pos: positions.subList(1, positions.size()-1)) {
					repeatedResources.add(T);
					//Remove last entry from original resources, since add one copy. Have to return (max) count resources.
					Ts.remove(Ts.size()-1);
				}		
		}
		Ts.addAll(repeatedResources);
		return Ts;
	}
	
	/**
	 * Retrieve records from specific collection using position 
	 * which is between lowerBound and upperBound
	 *
	 * @param colId, lowrBound, upperBound
	 * @return
	 */
	public List<T> getByCollectionPosition(ObjectId colId, int lowerBound, int upperBound) {
		Query<T> q = this.createQuery();
		String colField = "collectedIn."+colId;
		q.filter(colField + " >", lowerBound).filter(colField + " <", upperBound);
		/*
		q.field(colField).exists();
		q.and(q.criteria(colField).exists(), 
			q.filter("colField >", lowerBound)., 
			q.filter("colField <", upperBound));
		*/
		List<T> Ts = this.find(q).asList();
		List<T> repeatedResources = new ArrayList<T>();
		for (T T: Ts) {
			ArrayList<Integer> positions = (ArrayList<Integer>) T.getCollectedIn().get(colId);
			int firstPosition = -1;
			for (int pos: positions) {
				if (lowerBound <= pos && pos < upperBound) {
					firstPosition = pos;
				}
				if (firstPosition > -1 && lowerBound <= pos && pos < upperBound) {
					repeatedResources.add(T);
					//Remove last entry from original resources, since add one copy. Have to return (max) upperBound resources.
					Ts.remove(Ts.size()-1);
				}
			}
		}
		Ts.addAll(repeatedResources);
		return Ts;
	}

	/**
	 * Given a list of ObjectId's (dbId's)
	 * return the specified  CollectionObject's
	 * @param ids
	 * @return
	 */
	public List<T> getCollectionsByIds(List<ObjectId> ids) {
		Query<T> colQuery = this.createQuery().field("_id")
				.hasAnyOf(ids);
		return find(colQuery).asList();
	}

	/**
	 * List all CollectionObjects with the title provided for the language specified
	 * @param title
	 * @return
	 */
	public List<T> getByLabel(String title, String lang) {
		if(lang == null) lang = "en";
		Query<T> q = this.createQuery().field("model.label." + lang)
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
	public List<T> getAll(int offset, int count) {
		Query<T> q = this.createQuery().offset(offset).limit(count);
		return this.find(q).asList();
	}

	/**
	 * Get a user's CollectionObject according to the title given
	 * @param creatorId
	 * @param title
	 * @return
	 */
	public T getByOwnerAndLabel(ObjectId creatorId, String title, String lang) {
		if(lang == null) lang = "en";
		Query<T> q = this.createQuery().field("administative.withCreator")
				.equal(creatorId).field("model.label." + lang).equal(title);
		return this.findOne(q);
	}

	
	/**
	 * Get all CollectionObject using the creator's/owner's id.
	 * @param creatorId
	 * @param offset
	 * @param count
	 * @return
	 */
	public List<T> getByOwner(ObjectId creatorId, int offset, int count) {
		Query<T> q = this.createQuery().field("administative.withCreator")
				.equal(creatorId).offset(offset).limit(count);
		return this.find(q).asList();
	}

	/**
	 * Get the first CollectionObject that a user has created
	 * using the creator's/owner's id.
	 * We are using MongoDB's paging.
	 * @param id
	 * @return
	 */
	public List<T> getFirstResourceByOwner(ObjectId id) {
		return getByOwner(id, 0, 1);
	}
	
	/**
	 * Retrieve the owner/creator of a CollectionObject
	 * using collection's dbId
	 * @param id
	 * @return
	 */
	public User getCollectionOwner(ObjectId id) {
		Query<T> q = this.createQuery().field("_id").equal(id)
				.retrievedFields(true, "administative.withCreator");
		return findOne(q).retrieveCreator();
	}

	/**
	 * Retrieve a resource using the source that provided it
	 * @param sourceName
	 * @return
	 */
	public List<T> getByProvider(String sourceName) {
		//TODO: faster if could query on last entry of provenance array. Mongo query!
		Query<T> q = this.createQuery();
		q.field("provenance").hasThisElement(q.field("provider").equals(sourceName));
		return this.find(q).asList();
	}

	/**
	 * Create a Mongo access query criteria
	 * @param userAccess
	 * @return
	 */
	private Criteria formAccessLevelQuery(Tuple<ObjectId, Access> userAccess) {
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
	private CriteriaContainer formLoggedInUserQuery(List<ObjectId> loggedInUserEffIds) {
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
	private CriteriaContainer formQueryAccessCriteria(List<Tuple<ObjectId, Access>> filterByUserAccess) {
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
	private Query<T> formBasicQuery(CriteriaContainer[] criteria, ObjectId creator, Boolean isExhibition,  int offset, int count) {
		Query<T> q = this.createQuery().offset(offset).limit(count+1);
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
	public Tuple<List<T>, Tuple<Integer, Integer>> getResourcesWithCount(Query<T> q,
			Boolean isExhibition) {

		Tuple<Integer, Integer> hits = new Tuple<Integer, Integer>(0, 0);
		QueryResults<T> result;
		List<T> collections = new ArrayList<T>();
		if (isExhibition == null) {
			result = this.find(q);
			collections = result.asList();
			Query<T> q2 = q.cloneQuery();
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
		return new Tuple<List<T>, Tuple<Integer, Integer>>(collections, hits);
	}

	/**
	 * Return the total number of CollectionObject entities for a specific query
	 * @param q
	 * @param isExhibition
	 * @return
	 */
	public Tuple<Integer, Integer> getResourceCount(Query<T> q, Boolean isExhibition) {
		Tuple<Integer, Integer> hits = new Tuple<Integer, Integer>(0, 0);
		if (isExhibition == null) {
			Query<T> q2 = q.cloneQuery();
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
	public Tuple<List<T>, Tuple<Integer, Integer>>  getByACL(List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup, ObjectId creator,
			Boolean isExhibition, boolean totalHits, int offset, int count) {
		CriteriaContainer[] criteria =  new CriteriaContainer[0];
		for (List<Tuple<ObjectId, Access>> orAccessed: accessedByUserOrGroup) {
			criteria = ArrayUtils.addAll(criteria, formQueryAccessCriteria(orAccessed));
		}
		Query<T> q = formBasicQuery(criteria, creator, isExhibition, offset, count);
		if (totalHits) {
			return getResourcesWithCount(q, isExhibition);
		}
		else {
			if (isExhibition != null)
				q.field("isExhibition").equal(isExhibition);
			return new Tuple<List<T>, Tuple<Integer, Integer>>(this.find(q).asList(), null);
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
	public Tuple<List<T>, Tuple<Integer, Integer>>  getUsersAccessibleWithACL(List<ObjectId> loggeInEffIds,
			List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup, ObjectId creator,
			Boolean isExhibition, boolean totalHits, int offset, int count) {

		CriteriaContainer[] criteria =  new CriteriaContainer[0];
		criteria = ArrayUtils.addAll(criteria, formLoggedInUserQuery(loggeInEffIds));
		for (List<Tuple<ObjectId, Access>> orAccessed: accessedByUserOrGroup) {
			criteria = ArrayUtils.addAll(criteria, formQueryAccessCriteria(orAccessed));
		}
		Query<T> q = formBasicQuery(criteria, creator, isExhibition, offset, count);
		if (totalHits) {
			return getResourcesWithCount(q, isExhibition);
		}
		else {
			if (isExhibition != null)
				q.field("isExhibition").equal(isExhibition);
			return new Tuple<List<T>, Tuple<Integer, Integer>>(this.find(q).asList(), null);
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
	public Tuple<List<T>, Tuple<Integer, Integer>> getSharedWithACL(ObjectId userId, List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup,
			Boolean isExhibition,  boolean totalHits, int offset, int count) {

		Query<T> q = this.createQuery().offset(offset).limit(count+1);
		q.field("administrative.withCreator").notEqual(userId);
		CriteriaContainer[] criteria =  new CriteriaContainer[0];
		for (List<Tuple<ObjectId, Access>> orAccessed: accessedByUserOrGroup) {
			criteria = ArrayUtils.addAll(criteria ,formQueryAccessCriteria(orAccessed));
		}
		if (criteria.length > 0)
			q.and(criteria);
		if (totalHits) {
			return getResourcesWithCount(q, isExhibition);
		}
		else {
			if (isExhibition != null)
				q.field("isExhibition").equal(isExhibition);
			return new Tuple<List<T>, Tuple<Integer, Integer>>(this.find(q).asList(), null);
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
	public Tuple<List<T>, Tuple<Integer, Integer>> getPublicWithACL(List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup, ObjectId creator,
			Boolean isExhibition,  boolean totalHits, int offset, int count) {

		Query<T> q = this.createQuery().offset(offset).limit(count+1);
		if (creator != null)
			q.field("administrative.withCreator").equal(creator);
		Criteria[] criteria = {this.createQuery().criteria("administrative.access.isPublic").equal(true)};
		for (List<Tuple<ObjectId, Access>> orAccessed: accessedByUserOrGroup) {
			criteria = ArrayUtils.addAll(criteria ,formQueryAccessCriteria(orAccessed));
		}
		if (criteria.length > 0)
			q.and(criteria);
		if (totalHits) {
			return getResourcesWithCount(q, isExhibition);
		}
		else {
			if (isExhibition != null)
				q.field("isExhibition").equal(isExhibition);
			return new Tuple<List<T>, Tuple<Integer, Integer>>(this.find(q).asList(), null);
		}
	}
	
	/**
	 * Return the total number of likes for a resource.
	 * @param id
	 * @return
	 */
	public int getTotalLikes(ObjectId id) {
		Query<T> q = this.createQuery().field("_id").equal(id)
				.retrievedFields(true, "usage.likes");
		return this.findOne(q).getUsage().getLikes();
	}

	/**
	 * Return the number of resources that belong to a source
	 * @param sourceId
	 * @return
	 */
	public long countBySource(String sourceId) {
		Query<T> q = this.createQuery()
				.field("provenance.uri").equal(sourceId);
		return this.find(q).countAll();
	}

	
	/**
	 * ??????? do we have external Ids ??????
	 * @param extId
	 * @return
	 */
	public List<T> getByExternalId(String extId) {
		Query<T> q = this.createQuery().field("externalId")
				.equal(extId);
		return this.find(q).asList();
	}

	
	/**
	 * ??????? do we have external Ids ??????
	 * @param extId
	 * @return
	 */
	public long countByExternalId(String extId) {
		Query<T> q = this.createQuery()
				.field("externalId").equal(extId);
		return this.find(q).countAll();
	}

	/**
	 * Not a good Idea problably want work
	 * @param resourceId
	 * @param colId
	 * @param position
	 */
	public void removeFromCollection(ObjectId resourceId, ObjectId colId, int position) {
		Query<T> q = this.createQuery().field("_id").equal(resourceId);
		UpdateOperations<T> updateOps = this.createUpdateOperations();
		updateOps.removeAll("collectedIn."+colId, position);
		this.update(q, updateOps);
	}

	//TODO:Mongo query!
	/** 
	 * Also wrong implementation
	 * @param colId
	 * @param position
	 */
	public void shiftRecordsToLeft(ObjectId colId, int position) {
		String colField = "collectedIn."+colId;
		Query<T> q = this.createQuery().field(colField).exists();
		UpdateOperations<T> ops = this.createUpdateOperations().inc(colField);
		this.update(q, ops);
		/* TODO: check if the query can be expressed in morphia
		Query<T> q = this.createQuery();
		q.and(q.criteria(colField).exists(), q.criteria(colField).hasThisElement(q.field("position").greaterThanOrEq(position)));
		UpdateOperations<T> updateOps = this.createUpdateOperations();
		updateOps.dec(colField+".$");
		this.update(q,  updateOps);
		*/
		/*for (T resource: resources) {
			HashMap<ObjectId, ArrayList<Integer>> collectedIn = resource.getCollectedIn();
			ArrayList<Integer> positions = collectedIn.get(colId);
			for (Integer pos: positions) {
				if (pos > position) {
					updateOps.removeAll("collectedIn."+colId, position);
					updateOps.add("collectedIn."+colId, position-1);
				}
			}
		}
		this.update(q, updateOps);
		 */
		/*{	
		 	collectedIn.colId: {$exists: true},
		     collectedIn.colId: { $elemMatch: { $gte: position} }
		   },
		   { $dec: { "collectedIn."+colId+".$" : 1 } }*/
		/*BasicDBObject colIdQuery = new BasicDBObject();
		BasicDBObject existsField = new BasicDBObject();
		existsField.put("$exists", true);
		colIdQuery.put(colField, existsField);
		BasicDBObject geq = new BasicDBObject();
		geq.put("$geq", position);
		colIdQuery.append("$elemMatch", geq);
		BasicDBObject update = new BasicDBObject();
		BasicDBObject entrySpec = new BasicDBObject();
		entrySpec.put(colField+".$", 1);
		update.put("$dec", entrySpec);
		this.getDs().getCollection(entityClass.getSimpleName()).find(colIdQuery, update);*/
	}

	/**
	 * This method is to update the 'public' field on all the records of a
	 * collection. By default update method is invoked to all documents of a
	 * collection.
	 *
	 **/
	public void setFieldValueOfCollectedResource(ObjectId colId, String fieldName,
			String value) {
		Query<T> q = this.createQuery().field("collectedIn."+colId).exists();
		UpdateOperations<T> updateOps = this
				.createUpdateOperations();
		updateOps.set(fieldName, value);
		this.update(q, updateOps);
	}

	public void updateContent(ObjectId recId, String format, String content) {
		Query<T> q = this.createQuery().field("_id")
				.equal(recId);
		UpdateOperations<T> updateOps = this
				.createUpdateOperations();
		updateOps.set("content."+format, content);
		this.update(q, updateOps);

	}

	/*
	 * What is this ???????????/ ASK EIRINI
	 */
	public boolean checkMergedRecordVisibility(String extId, ObjectId dbId) {
		List<T> mergedRecord = getByExternalId(extId);
		for (T mr : mergedRecord) {
			//if (mr.getCollection().getIsPublic() && !mr.getDbId().equals(dbId))
				return true;
		}
		return false;
	}

	/**
	 * Increment likes for this specific resource 
	 * @param externalId
	 */
	public void incrementLikes(String dbId) {
		Query<T> q = this.createQuery().field("_id")
				.equal(dbId);
		UpdateOperations<T> updateOps = this
				.createUpdateOperations();
		updateOps.inc("usage.likes");
		this.update(q, updateOps);
	}

	/**
	 * Decrement likes for this specific resource
	 * @param dbId
	 */
	public void decrementLikes(String dbId) {
		Query<T> q = this.createQuery().field("dbId")
				.equal(dbId);
		UpdateOperations<T> updateOps = this
				.createUpdateOperations();
		updateOps.dec("usage.likes");
		this.update(q, updateOps);
	}

	/**
	 * Increment the specified field in a CollectionObject
	 * @param dbId
	 * @param fieldName
	 */
	public void incField(ObjectId dbId, String fieldName) {
		Query<T> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<T> updateOps = this.createUpdateOperations();
		updateOps.inc(fieldName);
		this.update(q, updateOps);
	}

	/**
	 * Decrement the specified field in a CollectionObject
	 * @param dbId
	 * @param fieldName
	 */
	public void decField(ObjectId dbId, String fieldName) {
		Query<T> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<T> updateOps = this.createUpdateOperations();
		updateOps.dec(fieldName);
		this.update(q, updateOps);
	}
}
