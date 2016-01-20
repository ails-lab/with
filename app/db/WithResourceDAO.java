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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import model.basicDataTypes.Language;
import model.basicDataTypes.WithAccess.Access;
import model.resources.RecordResource;
import model.resources.WithResource;
import model.usersAndGroups.User;

import org.bson.types.ObjectId;
import org.elasticsearch.common.lang3.ArrayUtils;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.CriteriaContainer;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryResults;
import org.mongodb.morphia.query.UpdateOperations;

import com.mongodb.BasicDBObject;

import utils.Tuple;
import utils.AccessManager.Action;

/*
 * The class consists of methods that can be both query
 * a CollectionObject or a RecordResource_ (CollectionObject,
 * CulturalObject, WithResource etc).
 *
 * Special methods referring to one of these entities go to the
 * specific DAO class.
 */
public class WithResourceDAO<T extends WithResource> extends DAO<T>{

	/*
	 * The value of the entity class is either
	 * CollectionObject.class or RecordResource.class
	 */
	public WithResourceDAO(Class<T> entityClass) {
		super(entityClass);
	}

	/**
	 * Retrieve a resource from DB using its dbId
	 * @param id
	 * @return
	 */
	public T getById(ObjectId id) {
		Query<T> q = this.createQuery().field("_id").equal(id);
		return this.findOne(q);
	}

	/**
	 * Get a resource by the dbId and retrieve
	 * only a bunch of fields from the whole document
	 * @param id
	 * @param retrievedFields
	 * @return
	 */
	public T getById(ObjectId id, List<String> retrievedFields) {
		Query<T> q = this.createQuery().field("_id").equal(id);
		q.retrievedFields(true, retrievedFields.toArray(new String[retrievedFields.size()]));
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

	public Query<T> createColIdElemMatchQuery(ObjectId colId) {
		Query<T> q = this.createQuery();
		BasicDBObject colIdQuery = new BasicDBObject();
		colIdQuery.put("collectionId", colId);
		BasicDBObject elemMatch1 = new BasicDBObject();
		elemMatch1.put("$elemMatch", colIdQuery);
		q.filter("collectedIn", elemMatch1);
		return q;
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
		//Query<T> q = this.createQuery().field("collectedIn.collectionId").equal(colId).offset(offset).limit(count);
		return this.find(createColIdElemMatchQuery(colId).offset(offset).limit(count)).asList();
	}

	/**
	 * Given a list of ObjectId's (dbId's)
	 * return the specified  resources
	 * @param ids
	 * @return
	 */
	public List<T> getByIds(List<ObjectId> ids) {
		Query<T> colQuery = this.createQuery().field("_id")
				.hasAnyOf(ids);
		return find(colQuery).asList();
	}

	/**
	 * List all CollectionObjects with the title provided for the language specified
	 * @param title
	 * @return
	 */
	public List<T> getByLabel(String lang, String title) {
		if (lang == null) lang = "default";
		Query<T> q = this.createQuery().disableValidation().field("descriptiveData.label" + lang)
				.contains(title);
		return this.find(q).asList();
	}

	public List<T> getByLabel(Language lang, String title) {
		Query<T> q = this.createQuery().disableValidation().field("descriptiveData.label." + lang.toString()).equal(title);//.contains(title);
		return this.find(q).asList();
	}

	/**
	 * Get a user's CollectionObject according to the title given
	 * @param creatorId
	 * @param title
	 * @return
	 */
	public T getByOwnerAndLabel(ObjectId creatorId, String lang, String title) {
		if(lang == null) lang = "en";
		Query<T> q = this.createQuery().field("administrative.withCreator")
				.equal(creatorId).field("descriptiveData.label." + lang).equal(title);
		return this.findOne(q);
	}


	/**
	 * Get all CollectionObject using the creator's/owner's id.
	 * @param creatorId
	 * @param offset
	 * @param count
	 * @return
	 */
	public List<T> getByCreator(ObjectId creatorId, int offset, int count) {
		Query<T> q = this.createQuery().field("administrative.withCreator")
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
	public List<T> getFirstResourceByCreator(ObjectId id) {
		return getByCreator(id, 0, 1);
	}

	/**
	 * Retrieve the owner/creator of a Resource
	 * using collection's dbId
	 * @param id
	 * @return
	 */
	public User getOwner(ObjectId id) {
		Query<T> q = this.createQuery().field("_id").equal(id)
				.retrievedFields(true, "administrative.withCreator");
		return ((WithResource) findOne(q)).retrieveCreator();
	}

	/**
	 * Retrieve a resource using the source that provided it
	 * @param sourceName
	 * @return
	 */
	public List<T> getByProvider(String sourceName) {

		//TODO: faster if could query on last entry of provenance array. Mongo query!
		/*
		 * We can sort the array in inverted order so to query
		 * only the first element of this array directly!
		 */
		Query<T> q = this.createQuery();
		BasicDBObject provQuery = new BasicDBObject();
		provQuery.put("provider", sourceName);
		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("$elemMatch", provQuery);
		q.filter("provenance", elemMatch);
		return this.find(q).asList();
	}

	/**
	 * Return the number of resources that belong to a source
	 * @param sourceId
	 * @return
	 */
	public long countBySource(String sourceName) {
		Query<T> q = this.createQuery();
		BasicDBObject provQuery = new BasicDBObject();
		provQuery.put("provider", sourceName);
		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("$elemMatch", provQuery);
		q.filter("provenance", elemMatch);
		return this.find(q).countAll();
	}

	/**
	 * Create a Mongo access query criteria
	 * @param userAccess
	 * @return
	 */
	protected Criteria formAccessLevelQuery(Tuple<ObjectId, Access> userAccess, QueryOperator operator) {
		int ordinal = userAccess.y.ordinal();
		/*Criteria[] criteria = new Criteria[Access.values().length-ordinal];
		for (int i=0; i<Access.values().length-ordinal; i++)
			criteria[i] = this.createQuery().criteria("rights." + userAccess.x.toHexString())
			.equal(Access.values()[i+ordinal].toString());*/
		//return this.createQuery().criteria("administrative.access." + userAccess.x.toHexString()).greaterThanOrEq(ordinal);
		BasicDBObject accessQuery = new BasicDBObject();
		accessQuery.put("user", userAccess.x);
		BasicDBObject geq = new BasicDBObject();
		geq.put(operator.toString(), ordinal);
		accessQuery.append("level", geq);
		//BasicDBObject elemMatch1 = new BasicDBObject();
		//elemMatch1.put("$elemMatch", accessQuery);
		return this.createQuery().criteria("administrative.access").hasThisElement(accessQuery);
	}

	/**
	 * Create Mongo access criteria for the current logged in user
	 * @param loggedInUserEffIds
	 * @return
	 */
	protected CriteriaContainer formLoggedInUserQuery(List<ObjectId> loggedInUserEffIds) {
		Criteria[] criteria = new Criteria[loggedInUserEffIds.size()+1];
		for (int i=0; i<loggedInUserEffIds.size(); i++) {
			criteria[i] = formAccessLevelQuery(new Tuple(loggedInUserEffIds.get(i), Access.READ), QueryOperator.GTE);//this.createQuery().criteria("rights." + loggedInUserEffIds.get(i)).greaterThanOrEq(ordinal);
		}
		criteria[loggedInUserEffIds.size()] = this.createQuery().criteria("administrative.access.isPublic").equal(true);
		return this.createQuery().or(criteria);
	}

	/**
	 * Create general Mongo access criteria for users-access level specified
	 * @param filterByUserAccess
	 * @return
	 */
	protected CriteriaContainer formQueryAccessCriteria(List<Tuple<ObjectId, Access>> filterByUserAccess) {
		Criteria[] criteria = new Criteria[0];
		for (Tuple<ObjectId, Access> userAccess: filterByUserAccess) {
			criteria = ArrayUtils.addAll(criteria, formAccessLevelQuery(userAccess, QueryOperator.GTE));
		}
		return this.createQuery().or(criteria);
	}

	/**
	 * Create a basic Mongo query with withCreator field matching, offset, limit and criteria.
	 * @param criteria
	 * @param creator
	 * @param offset
	 * @param count
	 * @return
	 */
	protected Query<T> formCreatorQuery(CriteriaContainer[] criteria, ObjectId creator,  int offset, int count) {
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
	public Tuple<List<T>, Tuple<Integer, Integer>>  getByACL(List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup, ObjectId creator,
			Boolean isExhibition, boolean totalHits, int offset, int count) {
		CriteriaContainer[] criteria =  new CriteriaContainer[0];
		for (List<Tuple<ObjectId, Access>> orAccessed: accessedByUserOrGroup) {
			criteria = ArrayUtils.addAll(criteria, formQueryAccessCriteria(orAccessed));
		}
		Query<T> q = formCreatorQuery(criteria, creator, offset, count);
		if (totalHits) {
			return getResourcesWithCount(q, isExhibition);
		}
		else {
			if (isExhibition != null)
				q.field("administrative.isExhibition").equal(isExhibition);
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
		Query<T> q = formCreatorQuery(criteria, creator, offset, count);
		if (totalHits) {
			return getResourcesWithCount(q, isExhibition);
		}
		else {
			if (isExhibition != null)
				q.field("administrative.isExhibition").equal(isExhibition);
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
				q.field("administrative.isExhibition").equal(isExhibition);
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
		return ((WithResource) this.findOne(q)).getUsage().getLikes();
	}

	/**
	 * @param extId
	 * @return
	 */
	public List<T> getByExternalId(String extId) {
		Query<T> q = this.createQuery().field("administrative.externalId")
				.equal(extId);
		return this.find(q).asList();
	}

	/**
	 * This method is to update the 'public' field on all the records of a
	 * collection. By default update method is invoked to all documents of a
	 * collection.
	 *
	 **/
	public void setFieldValueOfCollectedResource(ObjectId colId, String fieldName,
			String value) {
		Query<T> q = createColIdElemMatchQuery(colId);
		UpdateOperations<T> updateOps = this.createUpdateOperations();
		updateOps.set(fieldName, value);
		this.update(q, updateOps);
	}

	public void updateContent(ObjectId recId, String format, String content) {
		Query<T> q = this.createQuery().field("_id").equal(recId);
		UpdateOperations<T> updateOps = this
				.createUpdateOperations();
		updateOps.set("content."+format, content);
		this.update(q, updateOps);
	}

	/**
	 * Increment likes for this specific resource
	 * @param externalId
	 */
	public void incrementLikes(ObjectId dbId) {
		incField("usage.likes", dbId);
	}

	/**
	 * Decrement likes for this specific resource
	 * @param dbId
	 */
	public void decrementLikes(ObjectId dbId) {
		decField("usage.likes", dbId);
	}

	/**
	 * Increment the specified field in a CollectionObject
	 * @param dbId
	 * @param fieldName
	 */
	public void incField( String fieldName, ObjectId dbId) {
		Query<T> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<T> updateOps = this.createUpdateOperations().disableValidation();
		updateOps.inc(fieldName);
		this.update(q, updateOps);
	}

	/**
	 * Decrement the specified field in a CollectionObject
	 * @param dbId
	 * @param fieldName
	 */
	public void decField(String fieldName, ObjectId dbId) {
		Query<T> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<T> updateOps = this.createUpdateOperations();
		updateOps.dec(fieldName);
		this.update(q, updateOps);
	}
	
	public boolean hasAccess(List<ObjectId> effectiveIds, Action access, ObjectId resourceId) {
		return true;
	}
	
}
