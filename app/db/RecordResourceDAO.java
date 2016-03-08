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

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

import play.libs.Json;
import scala.util.control.Exception;
import sources.core.ParallelAPICall;
import utils.AccessManager;
import utils.AccessManager.Action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBObject;

import elastic.ElasticEraser;
import model.annotations.ContextData;
import model.annotations.ContextData.ContextDataBody;
import model.annotations.ContextData.ContextDataTarget;
import model.annotations.ExhibitionData;
import model.basicDataTypes.CollectionInfo;
import model.basicDataTypes.Language;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import model.basicDataTypes.WithAccess.AccessEntry;
import model.resources.CollectionObject;
import model.resources.RecordResource;

import org.apache.commons.beanutils.BeanToPropertyValueTransformer;
import org.apache.commons.collections.CollectionUtils;

/*
 * This class is the aggregator of methods
 * generically referring to *Object entities. We may assume
 * that these entities represent a Record of a Collection more or less.
 *
 * Type T is used in order for Morphia to know in which type is going
 * deserialize the object retrieved fro Mongo. So we have to options
 *
 * 1. Either pass WithResource when instansiating so that all entities
 * handled as WithResources.
 *
 * 2. Every time create a new DAO class associated with the explicit class
 * that I want to retieve.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class RecordResourceDAO extends WithResourceDAO<RecordResource> {

	public RecordResourceDAO() {
		super(RecordResource.class);
	}

	public List<RecordResource> getByCollectionBetweenPositions(
			CollectionObject collection, int lowerBound, int upperBound,
			List<String> retrievedFields) {
		Query<RecordResource> q = this.createQuery().retrievedFields(true,
				retrievedFields.toArray(new String[retrievedFields.size()]));
		return getByCollectionBetweenPositions(collection, lowerBound,
				upperBound, q);
	}

	public List<RecordResource> getByCollectionBetweenPositions(
			CollectionObject collection, int lowerBound, int upperBound) {
		Query<RecordResource> q = this.createQuery();
		return getByCollectionBetweenPositions(collection, lowerBound,
				upperBound, q);
	}

	/**
	 * Retrieve records from specific collection whose position is between
	 * lowerBound and upperBound. If a record appears n times in a collection
	 * (in different positions), n copies will appear in the returned list, in
	 * the respective positions.
	 *
	 * @param collection
	 *            , lowerBound, upperBound
	 * @return
	 */
	public List<RecordResource> getByCollectionBetweenPositions(
			CollectionObject collection, int lowerBound, int upperBound,
			Query<RecordResource> q) {
		if (lowerBound >= collection.getCollectedResources().size())
			return new ArrayList<RecordResource>();
		if (upperBound > collection.getCollectedResources().size())
			upperBound = collection.getCollectedResources().size();
		List<ContextData<ContextDataBody>> contextData = collection
				.getCollectedResources().subList(lowerBound, upperBound);
		List<ObjectId> recordIds = (List<ObjectId>) CollectionUtils.collect(
				contextData, new BeanToPropertyValueTransformer(
						"target.recordId"));
		q.field("_id").in(recordIds);
		List<RecordResource> records = this.find(q).asList();
		List<RecordResource> orderedRecords = new ArrayList<RecordResource>();
		for (ObjectId recordId : recordIds) {
			for (RecordResource record : records) {
				if (record.getDbId().equals(recordId)) {
					orderedRecords.add(record);
					break;
				}
			}
		}
		for (RecordResource record : orderedRecords) {
			for (ContextData<ContextDataBody> data : contextData) {
				if (data.getTarget().getRecordId().equals(record.getDbId())) {
					record.addContextData(data);
					break;
				}
			}
		}
		return orderedRecords;
	}

	public List<RecordResource> getByCollection(CollectionObject collection) {
		Query<RecordResource> q = this.createQuery();
		return getByCollection(collection, q);
	}

	public List<RecordResource> getByCollection(CollectionObject collection,
			List<String> retrievedFields) {
		Query<RecordResource> q = this.createQuery().retrievedFields(true,
				retrievedFields.toArray(new String[retrievedFields.size()]));
		return getByCollection(collection, q);
	}

	/**
	 * Retrieve all records that belong to that collection. If a record is
	 * included several times in a collection, it will only appear a single time
	 * in the returned list
	 *
	 * @param collection
	 * @return
	 */
	public List<RecordResource> getByCollection(CollectionObject collection,
			Query<RecordResource> q) {
		return getByCollectionBetweenPositions(collection, 0, collection
				.getCollectedResources().size(), q);
	}

	public void updateRecordUsageCollectedAndRights(ObjectId collectionId,
			WithAccess access, ObjectId recordId) {
		Query<RecordResource> q = this.createQuery().field("_id")
				.equal(recordId);
		UpdateOperations<RecordResource> recordUpdate = this
				.createUpdateOperations();
		// TODO: what if already entry with the same colId-position? have to
		// remove first to avoid duplicates.
		recordUpdate.add("collectedIn", collectionId);
		if (access != null)
			recordUpdate.set("administrative.access", access);
		if (DB.getCollectionObjectDAO().isFavorites(collectionId))
			recordUpdate.inc("usage.likes");
		else
			recordUpdate.inc("usage.collected");
		this.update(q, recordUpdate);
	}

	// TODO: has to be atomic as a whole
	// uses findAndModify for entryCount of respective collection
	public void addToCollection(ObjectId recordId, ObjectId collectionId,
			int position, boolean changeRecRights) {
		CollectionObject collection = DB.getCollectionObjectDAO()
				.addToCollection(collectionId, recordId, position);
		WithAccess newAccess = null;
		if (changeRecRights)
			newAccess = mergeParentCollectionRights(recordId, collectionId,
					collection.getAdministrative().getAccess());
		updateRecordUsageCollectedAndRights(collectionId, newAccess, recordId);
		DB.getCollectionObjectDAO().addCollectionMedia(collectionId, recordId);
	}

	public void appendToCollection(ObjectId recordId, ObjectId collectionId,
			boolean changeRecRights) {
		CollectionObject collection = DB.getCollectionObjectDAO()
				.addToCollection(collectionId, recordId, -1);
		WithAccess newAccess = null;
		if (changeRecRights)
			newAccess = mergeParentCollectionRights(recordId, collectionId,
					collection.getAdministrative().getAccess());
		updateRecordUsageCollectedAndRights(collectionId, newAccess, recordId);
		DB.getCollectionObjectDAO().addCollectionMedia(collectionId, recordId);
	}

	// TODO Refactor
	public WithAccess mergeParentCollectionRights(ObjectId recordId,
			ObjectId newCollectionId, WithAccess newCollectionAccess) {
		RecordResource record = this.getById(
				recordId,
				new ArrayList<String>(Arrays.asList(
						"administrative.access.isPublic",
						"administrative.withCreator")));
		List<ObjectId> parentCollections = getParentCollections(recordId);
		List<WithAccess> parentColAccess = new ArrayList<WithAccess>();
		for (ObjectId colId : parentCollections) {
			if (colId.equals(newCollectionId))
				parentColAccess.add(newCollectionAccess);
			else {
				CollectionObject parentCollection = DB.getCollectionObjectDAO()
						.getById(
								colId,
								new ArrayList<String>(Arrays
										.asList("administrative.access")));
				parentColAccess.add(parentCollection.getAdministrative()
						.getAccess());
			}
		}
		// hope there aren't too many collections containing the resource
		// if the record is public, it should remain public. Acl rights are
		// determined by the collections the record belongs to
		return mergeRights(parentColAccess, record.getAdministrative()
				.getWithCreator(), record.getAdministrative().getAccess()
				.getIsPublic());
	}

	public boolean mergeParentCollectionPublicity(ObjectId recordId,
			boolean isPublic, ObjectId newColId) {
		boolean mergedIsPublic = isPublic;
		List<ObjectId> parentCollections = getParentCollections(recordId);
		for (ObjectId colId : parentCollections) {
			CollectionObject parentCollection = DB.getCollectionObjectDAO()
					.getById(
							colId,
							new ArrayList<String>(Arrays
									.asList("administrative.access")));
			if (parentCollection.getAdministrative().getAccess().getIsPublic())
				return true;
		}
		return mergedIsPublic;
	}

	public void updateMembersToMergedRights(ObjectId collectionId,
			AccessEntry newAccess, List<ObjectId> effectiveIds) {
		ArrayList<String> retrievedFields = new ArrayList<String>(
				Arrays.asList("_id", "administrative.access"));
		WithAccess colAccess = DB.getCollectionObjectDAO()
				.getById(collectionId, retrievedFields).getAdministrative()
				.getAccess();
		colAccess.addToAcl(newAccess);
		List<RecordResource> memberRecords = getByCollection(collectionId,
				retrievedFields);
		for (RecordResource r : memberRecords) {
			if (DB.getRecordResourceDAO().hasAccess(effectiveIds,
					Action.DELETE, r.getDbId())) {
				WithAccess mergedAccess = mergeParentCollectionRights(
						r.getDbId(), collectionId, colAccess);
				updateField(r.getDbId(), "administrative.access", mergedAccess);
			}
		}
	}

	public void updateMembersToMergedPublicity(ObjectId colId,
			boolean isPublic, List<ObjectId> effectiveIds) {
		ArrayList<String> retrievedFields = new ArrayList<String>(
				Arrays.asList("_id", "administrative.access"));
		List<RecordResource> memberRecords = getByCollection(colId,
				retrievedFields);
		for (RecordResource r : memberRecords) {
			if (DB.getRecordResourceDAO().hasAccess(effectiveIds,
					Action.DELETE, r.getDbId())) {
				boolean mergedPublicity = mergeParentCollectionPublicity(
						r.getDbId(), isPublic, colId);
				updateField(r.getDbId(), "administrative.access",
						mergedPublicity);
			}
		}
	}

	public void updateRecordRightsUponRemovalFromCollection(ObjectId recordId,
			ObjectId colId) {
		RecordResource record = this.getById(
				recordId,
				new ArrayList<String>(Arrays.asList("collectedIn",
						"administrative.access.isPublic",
						"administrative.withCreator")));
		List<ObjectId> parentCollections = getParentCollections(recordId);
		parentCollections.remove(colId);
		List<WithAccess> parentColAccess = new ArrayList<WithAccess>();
		for (ObjectId parentId : parentCollections) {
			CollectionObject parentCollection = DB.getCollectionObjectDAO()
					.getById(
							parentId,
							new ArrayList<String>(Arrays
									.asList("administrative.access")));
			parentColAccess.add(parentCollection.getAdministrative()
					.getAccess());
		}
		WithAccess mergedAccess = mergeRights(parentColAccess, record
				.getAdministrative().getWithCreator(), record
				.getAdministrative().getAccess().getIsPublic());
		updateField(recordId, "administrative.access", mergedAccess);
	}

	public void updateMembersToNewAccess(ObjectId colId, ObjectId userId,
			Access newAccess, List<ObjectId> effectiveIds) {
		ArrayList<String> retrievedFields = new ArrayList<String>(
				Arrays.asList("_id"));
		List<RecordResource> memberRecords = getByCollection(colId,
				retrievedFields);
		for (RecordResource r : memberRecords) {
			if (DB.getRecordResourceDAO().hasAccess(effectiveIds,
					Action.DELETE, r.getDbId()))
				changeAccess(r.getDbId(), userId, newAccess);
		}
	}

	public void updateMembersToNewPublicity(ObjectId colId, boolean isPublic,
			List<ObjectId> effectiveIds) {
		ArrayList<String> retrievedFields = new ArrayList<String>(
				Arrays.asList("_id"));
		List<RecordResource> memberRecords = getByCollection(colId,
				retrievedFields);
		for (RecordResource r : memberRecords) {
			if (DB.getRecordResourceDAO().hasAccess(effectiveIds,
					Action.DELETE, r.getDbId()))
				DB.getRecordResourceDAO().updateField(r.getDbId(),
						"administrative.access.isPublic", isPublic);
		}
	}

	public void updatePosition(ObjectId resourceId, ObjectId colId,
			int oldPosition, int newPosition) {
		Query<RecordResource> q = this.createQuery().field("_id")
				.equal(resourceId);
		UpdateOperations<RecordResource> updateOps1 = this
				.createUpdateOperations();
		updateOps1.removeAll("collectedIn", new CollectionInfo(colId,
				oldPosition));
		UpdateOperations<RecordResource> updateOps2 = this
				.createUpdateOperations();
		updateOps2.add("collectedIn", new CollectionInfo(colId, newPosition));
		RecordResource record = DB.getRecordResourceDAO().getById(resourceId,
				Arrays.asList("contextData"));
		if (record != null) {
			List<ContextData> contextData = record.getContextData();
			for (ContextData c : contextData) {
				ContextDataTarget target = c.getTarget();
				if (target.getCollectionId().equals(colId)
						&& target.getPosition() == oldPosition) {
					updateOps1.removeAll("contextData", c);
					c.getTarget().setPosition(newPosition);
					updateOps2.add("contextData", c);
					break;
				}
			}
		}
		this.update(q, updateOps1);
		this.update(q, updateOps2);
	}

	public void removeFromCollection(ObjectId recordId, ObjectId colId,
			int position) throws FileNotFoundException {
		UpdateOperations<RecordResource> updateOps = this
				.createUpdateOperations();
		Query<RecordResource> q = this.createQuery().field("_id")
				.equal(recordId);
		BasicDBObject colIdQuery = new BasicDBObject();
		colIdQuery.put("collectionId", colId);
		colIdQuery.put("position", position);
		BasicDBObject elemMatch = new BasicDBObject();
		elemMatch.put("$elemMatch", colIdQuery);
		q.filter("collectedIn", elemMatch);
		updateOps.removeAll("collectedIn", new CollectionInfo(colId, position));
		updateOps.removeAll("contextData", new ContextData(colId, position));
		UpdateResults result = this.update(q, updateOps);
		if (result.getWriteResult().getN() == 0)
			throw new FileNotFoundException();
		else
			shiftRecordsToLeft(colId, position + 1);
	}

	public RecordResource getByCollectionAndPosition(ObjectId colId,
			int position) {
		Query<RecordResource> q = this.createQuery().field("collectedIn")
				.hasThisElement(new CollectionInfo(colId, position));
		return this.findOne(q);
	}

	public RecordResource getByCollectionAndPosition(ObjectId colId,
			int position, List<String> retrievedFields) {
		Query<RecordResource> q = this.createQuery().retrievedFields(true,
				retrievedFields.toArray(new String[retrievedFields.size()]));
		q.field("collectedIn").hasThisElement(
				new CollectionInfo(colId, position));
		return this.findOne(q);
	}

	public boolean existsInCollectionAndPosition(ObjectId colId,
			Integer position) {
		Query<RecordResource> q = this.createQuery().field("collectedIn")
				.hasThisElement(new CollectionInfo(colId, position));
		return this.find(q.limit(1)).asList().size() == 0 ? false : true;
	}

	public boolean existsSameExternaIdInCollection(String externalId,
			ObjectId colId) {
		Query<RecordResource> q = this.createQuery().disableValidation()
				.field("collectedIn").hasThisElement(colId);
		q.field("administrativeData.externalId").equal(externalId);
		return this.find(q.limit(1)).asList().size() == 0 ? false : true;
	}

	public void editRecord(String root, ObjectId dbId, JsonNode json) {
		Query<RecordResource> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<RecordResource> updateOps = this
				.createUpdateOperations();
		updateFields(root, json, updateOps);
		updateOps.set("administrative.lastModified", new Date());
		this.update(q, updateOps);
	}

	public void removeAllRecordsFromCollection(ObjectId colId) {
		ArrayList<String> retrievedFields = new ArrayList<String>(
				Arrays.asList("_id", "collectedIn"));
		List<RecordResource> memberRecords = getByCollection(colId,
				retrievedFields);
		for (RecordResource record : memberRecords) {
			ObjectId recordId = record.getDbId();
			List<CollectionInfo> collectedIn = record.getCollectedIn();
			if (collectedIn.size() > 1) {
				UpdateOperations<RecordResource> updateOps = this
						.createUpdateOperations();
				Query<RecordResource> q = this.createQuery().field("_id")
						.equal(recordId);
				updateOps.removeAll("collectedIn", new CollectionInfo(colId,
						null));
				// TODO See if it is collected in the same collection more than
				// once
				updateOps.dec("usage.collected");
				this.update(q, updateOps);
			} else
				deleteById(recordId);
		}

		/*
		 * Delete resources of these collection. Practically update field
		 * 'collectedIn' of the resource on the index.
		 */
		List<ObjectId> resourceIds = new ArrayList<ObjectId>();
		memberRecords.forEach((r) -> {
			resourceIds.add(r.getDbId());
		});
		Function<List<ObjectId>, Boolean> deleteResources = (List<ObjectId> ids) -> (ElasticEraser
				.deleteManyResources(ids));
		ParallelAPICall.createPromise(deleteResources, resourceIds);
	}

}
