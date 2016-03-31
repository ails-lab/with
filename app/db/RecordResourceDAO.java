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
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import model.annotations.ContextData;
import model.annotations.ContextData.ContextDataBody;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import model.basicDataTypes.WithAccess.AccessEntry;
import model.resources.CollectionObject;
import model.resources.RecordResource;

import org.apache.commons.beanutils.BeanToPropertyValueTransformer;
import org.apache.commons.collections.CollectionUtils;
import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import sources.core.ParallelAPICall;
import utils.AccessManager.Action;

import com.fasterxml.jackson.databind.JsonNode;

import elastic.ElasticEraser;

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
			ObjectId collectionId, int lowerBound, int upperBound,
			List<String> retrievedFields) {
		CollectionObject collection = DB.getCollectionObjectDAO().getById(
				collectionId,
				new ArrayList<String>(Arrays.asList("collectedResources")));
		Query<RecordResource> q = this.createQuery().retrievedFields(true,
				retrievedFields.toArray(new String[retrievedFields.size()]));
		return getByCollectionBetweenPositions(
				collection.getCollectedResources(), lowerBound, upperBound, q);
	}

	public List<RecordResource> getByCollectionBetweenPositions(
			ObjectId collectionId, int lowerBound, int upperBound) {
		CollectionObject collection = DB.getCollectionObjectDAO().getById(
				collectionId,
				new ArrayList<String>(Arrays.asList("collectedResources")));
		Query<RecordResource> q = this.createQuery();
		return getByCollectionBetweenPositions(
				collection.getCollectedResources(), lowerBound, upperBound, q);
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
			List<ContextData<ContextDataBody>> collectedResources,
			int lowerBound, int upperBound, Query<RecordResource> q) {
		if (collectedResources == null)
			return new ArrayList<RecordResource>();
		if (lowerBound >= collectedResources.size())
			return new ArrayList<RecordResource>();
		if (upperBound > collectedResources.size())
			upperBound = collectedResources.size();
		List<ContextData<ContextDataBody>> contextData = collectedResources
				.subList(lowerBound, upperBound);
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
					record.setContextData(data);
					break;
				}
			}
		}
		return orderedRecords;
	}

	public List<RecordResource> getByCollection(ObjectId collectionId) {
		CollectionObject collection = DB.getCollectionObjectDAO().getById(
				collectionId,
				new ArrayList<String>(Arrays.asList("collectedResources")));
		Query<RecordResource> q = this.createQuery();
		return getByCollection(collection.getCollectedResources(), q);
	}

	public List<RecordResource> getByCollection(ObjectId collectionId,
			List<String> retrievedFields) {
		CollectionObject collection = DB.getCollectionObjectDAO().getById(
				collectionId,
				new ArrayList<String>(Arrays.asList("collectedResources")));
		Query<RecordResource> q = this.createQuery().retrievedFields(true,
				retrievedFields.toArray(new String[retrievedFields.size()]));
		return getByCollection(collection.getCollectedResources(), q);
	}

	/**
	 * Retrieve all records that belong to that collection. If a record is
	 * included several times in a collection, it will only appear a single time
	 * in the returned list
	 *
	 * @param collection
	 * @return
	 */
	public List<RecordResource> getByCollection(
			List<ContextData<ContextDataBody>> collectedResources,
			Query<RecordResource> q) {
		try {
			List<ObjectId> recordIds = (List<ObjectId>) CollectionUtils
					.collect(collectedResources,
							new BeanToPropertyValueTransformer(
									"target.recordId"));
			q.field("_id").in(recordIds);
			return this.find(q).asList();
		} catch (Exception e) {
			return new ArrayList<RecordResource>();
		}
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
				.addToCollection(collectionId, recordId, position, false);
		WithAccess newAccess = null;
		if (changeRecRights)
			newAccess = mergeParentCollectionRights(recordId, collectionId,
					collection.getAdministrative().getAccess());
		updateRecordUsageCollectedAndRights(collectionId, newAccess, recordId);
		DB.getCollectionObjectDAO().addCollectionMedia(collectionId, recordId,
				position);
	}

	public void appendToCollection(ObjectId recordId, ObjectId collectionId,
			boolean changeRecRights) {
		CollectionObject collection = DB.getCollectionObjectDAO()
				.addToCollection(collectionId, recordId, -1, true);
		WithAccess newAccess = null;
		if (changeRecRights)
			newAccess = mergeParentCollectionRights(recordId, collectionId,
					collection.getAdministrative().getAccess());
		updateRecordUsageCollectedAndRights(collectionId, newAccess, recordId);
		DB.getCollectionObjectDAO().addCollectionMedia(collectionId, recordId,
				collection.getCollectedResources().size());
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
			ObjectId collectionId) {
		RecordResource record = this.getById(
				recordId,
				new ArrayList<String>(Arrays.asList("collectedIn",
						"administrative.access.isPublic",
						"administrative.withCreator")));
		List<ObjectId> parentCollections = getParentCollections(recordId);
		parentCollections.remove(collectionId);
		List<WithAccess> parentColAccess = new ArrayList<WithAccess>();
		for (ObjectId parentId : parentCollections) {
			CollectionObject parentCollection;
			if ((parentCollection = DB.getCollectionObjectDAO().getById(
					parentId,
					new ArrayList<String>(Arrays
							.asList("administrative.access")))) != null)
				parentColAccess.add(parentCollection.getAdministrative()
						.getAccess());
		}
		WithAccess mergedAccess = mergeRights(parentColAccess, record
				.getAdministrative().getWithCreator(), record
				.getAdministrative().getAccess().getIsPublic());
		updateField(recordId, "administrative.access", mergedAccess);
	}

	public void updateMembersToNewAccess(ObjectId collectionId,
			ObjectId userId, Access newAccess, List<ObjectId> effectiveIds) {
		ArrayList<String> retrievedFields = new ArrayList<String>(
				Arrays.asList("_id"));
		List<RecordResource> memberRecords = getByCollection(collectionId,
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

	public void removeFromCollection(ObjectId recordId, ObjectId collectionId,
			int position, boolean first, boolean all) throws Exception {
		DB.getCollectionObjectDAO().removeFromCollection(collectionId,
				recordId, position, first, all);
		UpdateOperations<RecordResource> recordUpdate = this
				.createUpdateOperations();
		Query<RecordResource> q = this.createQuery().field("_id")
				.equal(recordId);
		recordUpdate.removeAll("collectedIn", Arrays.asList(collectionId));
		if (DB.getCollectionObjectDAO().isFavorites(collectionId))
			recordUpdate.dec("usage.likes");
		else
			recordUpdate.dec("usage.collected");
		this.update(q, recordUpdate);
		updateRecordRightsUponRemovalFromCollection(recordId, collectionId);
	}

	public RecordResource getByCollectionAndPosition(ObjectId collectionId,
			int position) {
		List<ContextData<ContextDataBody>> collectedResources = DB
				.getCollectionObjectDAO()
				.getById(collectionId, Arrays.asList("collectedResources"))
				.getCollectedResources();
		if (collectedResources.size() <= position)
			return null;
		ObjectId recordId = collectedResources.get(position).getTarget()
				.getRecordId();
		return this.get(recordId);
	}

	public List<RecordResource> getByMedia(String mediaUrl) {
		Query<RecordResource> q = this.createQuery().disableValidation()
				.field("media.0.Original.url").equal(mediaUrl);
		System.out.println(q.toString());
		return this.find(q.retrievedFields(true, "_id")).asList();
	}

	public boolean existsSameExternaIdInCollection(String externalId,
			ObjectId collectionId) {
		Query<RecordResource> q = this.createQuery().disableValidation()
				.field("collectedIn").equal(collectionId);
		q.field("administrative.externalId").equal(externalId);
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

	public void removeAllRecordsFromCollection(ObjectId collectionId) {
		ArrayList<String> retrievedFields = new ArrayList<String>(
				Arrays.asList("_id", "collectedIn"));
		List<RecordResource> memberRecords = getByCollection(collectionId,
				retrievedFields);
		for (RecordResource record : memberRecords) {
			ObjectId recordId = record.getDbId();
			List<ObjectId> collectedIn = record.getCollectedIn();
			if (collectedIn.size() > 1) {
				UpdateOperations<RecordResource> updateOps = this
						.createUpdateOperations();
				Query<RecordResource> q = this.createQuery().field("_id")
						.equal(recordId);
				updateOps.removeAll("collectedIn", collectionId);
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
