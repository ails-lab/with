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
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import model.DescriptiveData;
import model.EmbeddedMediaObject;
import model.basicDataTypes.Language;
import model.basicDataTypes.WithAccess;
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
	
	public boolean existsResource(ObjectId id) {
		Query<T> q = this.createQuery().field("_id").equal(id).limit(1);
		return (this.find(q).asList().size()==0? false: true);
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
	 * Return all resources that belong to a collection throwing
	 * away duplicate entries in a collection
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
	 * Retrieve a resource if the provenanceChain contains the providerName
	 * @param sourceName
	 * @return
	 */
	public List<T> getByProvider(String providerName) {
		Query<T> q = this.createQuery();
		BasicDBObject provQuery = new BasicDBObject();
		provQuery.put("provider", providerName);
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

	public boolean isPublic(ObjectId id) {
		Query<T> q = this.createQuery().field("_id").equal(id).limit(1);
		q.field("administrative.isPublic").equal(true);
		return (find(q).asList().size()==0? false: true);
	}
	/**
	 * Create a Mongo access query criteria
	 * @param userAccess
	 * @return
	 */
	protected Criteria formAccessLevelQuery(Tuple<ObjectId, Access> userAccess, QueryOperator operator) {
		int ordinal = userAccess.y.ordinal();
		BasicDBObject accessQuery = new BasicDBObject();
		accessQuery.put("user", userAccess.x);
		BasicDBObject oper = new BasicDBObject();
		oper.put(operator.toString(), ordinal);
		accessQuery.append("level", oper);
		return this.createQuery().criteria("administrative.access.acl").hasThisElement(accessQuery);
	}


	protected CriteriaContainer loggedInUserWithAtLeastAccessQuery(List<ObjectId> loggedInUserEffIds, Access access) {
		List<Criteria> criteria = new ArrayList<Criteria>();//new Criteria[loggedInUserEffIds.size()+1];
		for (int i=0; i<loggedInUserEffIds.size(); i++) {
			criteria.add(formAccessLevelQuery(new Tuple(loggedInUserEffIds.get(i), access), QueryOperator.GTE));
		}
		if (access.ordinal()<2)
			criteria.add(this.createQuery().criteria("administrative.access.isPublic").equal(true));
		return this.createQuery().or(criteria.toArray(new Criteria[criteria.size()]));
	}


	protected CriteriaContainer atLeastAccessCriteria(List<Tuple<ObjectId, Access>> filterByUserAccess) {
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
	
	public boolean hasAccess(List<ObjectId> effectiveIds,  Action action, ObjectId resourceId) {
		CriteriaContainer criteria = loggedInUserWithAtLeastAccessQuery(effectiveIds, actionToAccess(action));
		Query<T> q = this.createQuery().disableValidation().limit(1);
		q.field("_id").equal(resourceId);
		q.or(criteria);
		return (this.find(q).asList().size()==0? false: true);
	}
	
	public Access actionToAccess(Action action) {
		return Access.values()[action.ordinal()+1];
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
	public T getByExternalId(String extId) {
		Query<T> q = this.createQuery().field("administrative.externalId")
				.equal(extId);
		return this.findOne(q);
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
	
	public void updateDescriptiveData(ObjectId recId, DescriptiveData data) {
		Query<T> q = this.createQuery().field("_id").equal(recId);
		UpdateOperations<T> updateOps = this
				.createUpdateOperations();
		updateOps.set("descriptiveData", data);
		this.update(q, updateOps);
	}
	
	public void updateEmbeddedMedia(ObjectId recId, EmbeddedMediaObject media) {
		Query<T> q = this.createQuery().field("_id").equal(recId);
		UpdateOperations<T> updateOps = this
				.createUpdateOperations();
		updateOps.set("media", media);
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
	

}
