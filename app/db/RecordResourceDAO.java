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
import model.annotations.ContextData.ContextDataTarget;
import model.annotations.ExhibitionData;
import model.basicDataTypes.CollectionInfo;
import model.basicDataTypes.Language;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import model.basicDataTypes.WithAccess.AccessEntry;
import model.resources.CollectionObject;
import model.resources.RecordResource;

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
public class RecordResourceDAO extends WithResourceDAO<RecordResource> {

	public RecordResourceDAO() {
		super(RecordResource.class);
	}

	public int deleteByCollection(ObjectId colId) {
		Query<RecordResource> q = this.createQuery()
				.field("collectedIn.coldId").exists();
		return this.deleteByQuery(q).getN();
	}

	public List<RecordResource> getByCollectionBetweenPositions(ObjectId colId,
			int lowerBound, int upperBound, List<String> retrievedFields) {
		Query<RecordResource> q = this.createQuery().retrievedFields(true,
				retrievedFields.toArray(new String[retrievedFields.size()]));
		return getByCollectionBetweenPositions(colId, lowerBound, upperBound, q);
	}

	public List<RecordResource> getByCollectionBetweenPositions(ObjectId colId,
			int lowerBound, int upperBound) {
		Query<RecordResource> q = this.createQuery();
		return getByCollectionBetweenPositions(colId, lowerBound, upperBound, q);
	}

	/**
	 * Retrieve records from specific collection whose position is between
	 * lowerBound and upperBound. If a record appears n times in a collection
	 * (in different positions), n copies will appear in the returned list, in
	 * the respective positions.
	 *
	 * @param colId
	 *            , lowerBound, upperBound
	 * @return
	 */
	public List<RecordResource> getByCollectionBetweenPositions(ObjectId colId,
			int lowerBound, int upperBound, Query<RecordResource> q) {
		BasicDBObject colIdQuery = new BasicDBObject();
		colIdQuery.put("collectionId", colId);
		BasicDBObject geq = new BasicDBObject();
		geq.put("$gte", lowerBound);
		geq.append("$lt", upperBound);
		colIdQuery.append("position", geq);
		BasicDBObject elemMatch1 = new BasicDBObject();
		elemMatch1.put("$elemMatch", colIdQuery);
		q.filter("collectedIn", elemMatch1);
		List<RecordResource> resources = this.find(q).asList();
		List<RecordResource> repeatedResources = new ArrayList<RecordResource>(
				upperBound - lowerBound);
		for (int i = 0; i < (upperBound - lowerBound); i++) {
			repeatedResources.add(new RecordResource());
		}
		int maxPosition = -1;
		for (RecordResource d : resources) {
			ArrayList<CollectionInfo> collectionInfos = (ArrayList<CollectionInfo>) d
					.getCollectedIn();
			// May be a long iteration, if a record belongs to many collections
			for (CollectionInfo ci : collectionInfos) {
				ObjectId collectionId = ci.getCollectionId();
				if (collectionId.equals(colId)) {
					int pos = ci.getPosition();
					if ((lowerBound <= pos) && (pos < upperBound)) {
						int arrayPosition = pos - lowerBound;
						if (arrayPosition > maxPosition)
							maxPosition = arrayPosition;
						repeatedResources.set(arrayPosition, d);
					}
				}
			}
		}
		if (maxPosition > -1)
			return repeatedResources.subList(0, maxPosition + 1);
		else
			return new ArrayList<RecordResource>();
	}

	public List<RecordResource> getByCollection(ObjectId colId) {
		Query<RecordResource> q = this.createQuery();
		return getByCollection(colId, q);
	}

	public List<RecordResource> getByCollection(ObjectId colId,
			List<String> retrievedFields) {
		Query<RecordResource> q = this.createQuery().retrievedFields(true,
				retrievedFields.toArray(new String[retrievedFields.size()]));
		return getByCollection(colId, q);
	}

	/**
	 * Retrieve all records that belong to that collection. If a record is
	 * included several times in a collection, it will only appear a single time
	 * in the returned list
	 *
	 * @param colId
	 * @return
	 */
	public List<RecordResource> getByCollection(ObjectId colId,
			Query<RecordResource> q) {
		BasicDBObject colIdQuery = new BasicDBObject();
		colIdQuery.put("collectionId", colId);
		BasicDBObject elemMatch1 = new BasicDBObject();
		elemMatch1.put("$elemMatch", colIdQuery);
		return q.filter("collectedIn", elemMatch1).asList();
		// int MAX = 10000;
		// return getByCollectionBetweenPositions(colId, 0, MAX);
	}

	public void shift(ObjectId colId, int position,
			BiConsumer<String, UpdateOperations> update) {
		Query<RecordResource> q = this.createQuery();
		BasicDBObject colIdQuery = new BasicDBObject();
		colIdQuery.put("collectionId", colId);
		BasicDBObject elemMatch2 = new BasicDBObject();
		BasicDBObject geq = new BasicDBObject();
		geq.put("$gte", position);
		colIdQuery.append("position", geq);
		BasicDBObject elemMatch1 = new BasicDBObject();
		elemMatch1.put("$elemMatch", colIdQuery);
		q.filter("collectedIn", elemMatch1);
		List<RecordResource> resources = this.find(q).asList();
		for (RecordResource resource : resources) {
			UpdateOperations<RecordResource> updateOps = this
					.createUpdateOperations().disableValidation();
			List<CollectionInfo> collectedIn = resource.getCollectedIn();
			int index = 0;
			for (CollectionInfo ci : collectedIn) {
				if (ci.getCollectionId().equals(colId)) {
					int pos = ci.getPosition();
					if (pos >= position)
						update.accept("collectedIn." + index + ".position",
								updateOps);
				}
				index += 1;
			}
			List<ContextData> contextData = resource.getContextData();
			index = 0;
			for (ContextData c : contextData) {
				ContextDataTarget target = c.getTarget();
				if (target.getCollectionId().equals(colId)) {
					int pos = target.getPosition();
					if (pos >= position) {
						update.accept("contextData." + index + ".target.position",
								updateOps);
					}
				}
				index += 1;
			}
			this.update(
					this.createQuery().field("_id").equal(resource.getDbId()),
					updateOps);
		}
	}

	public void shift(ObjectId colId, int startPosition, int stopPosition,
			BiConsumer<String, UpdateOperations> update) {
		Query<RecordResource> q = this.createQuery();
		BasicDBObject colIdQuery = new BasicDBObject();
		colIdQuery.put("collectionId", colId);
		BasicDBObject elemMatch2 = new BasicDBObject();
		BasicDBObject geq = new BasicDBObject();
		geq.put("$gte", startPosition);
		geq.put("$lte", stopPosition);
		colIdQuery.append("position", geq);
		BasicDBObject elemMatch1 = new BasicDBObject();
		elemMatch1.put("$elemMatch", colIdQuery);
		q.filter("collectedIn", elemMatch1);
		String[] retrievedFields = {"collectedIn", "contextData"};
		List<RecordResource> resources = this.find(
				q.retrievedFields(true, retrievedFields)).asList();
		for (RecordResource resource : resources) {
			UpdateOperations<RecordResource> updateOps = this
					.createUpdateOperations().disableValidation();
			List<CollectionInfo> collectedIn = resource.getCollectedIn();
			int index = 0;
			for (CollectionInfo ci : collectedIn) {
				if (ci.getCollectionId().equals(colId)) {
					int pos = ci.getPosition();
					if ((pos >= startPosition) && (pos <= stopPosition))
						update.accept("collectedIn." + index + ".position",
								updateOps);
				}
				index += 1;
			}
			index = 0;
			List<ContextData> contextData = resource.getContextData();
			for (ContextData c : contextData) {
				ContextDataTarget target = c.getTarget();
				if (target.getCollectionId().equals(colId)) {
					int pos = target.getPosition();
					if ((pos >= startPosition) && (pos <= stopPosition)) {
						update.accept("contextData." + index + ".target.position",
								updateOps);
					}
				}
				index += 1;
			}

			this.update(
					this.createQuery().field("_id").equal(resource.getDbId()),
					updateOps);
		}
	}

	
	//TODO: context data have to be updated!!!!
	/**
	 * Shift one position left all resources in colId with position equal or
	 * greater than position.
	 * 
	 * @param colId
	 * @param position
	 */
	public void shiftRecordsToLeft(ObjectId colId, int position) {
		// UpdateOperations updateOps = this.createUpdateOperations();
		BiConsumer<String, UpdateOperations> update = (String field,
				UpdateOperations updateOpsPar) -> updateOpsPar.dec(field);
		shift(colId, position, update);
	}

	public void shiftRecordsToLeft(ObjectId colId, int startPosition,
			int stopPosition) {
		// UpdateOperations updateOps = this.createUpdateOperations();
		BiConsumer<String, UpdateOperations> update = (String field,
				UpdateOperations updateOpsPar) -> updateOpsPar.dec(field);
		shift(colId, startPosition, stopPosition, update);
	}

	/**
	 * Shift one position right all resources in colId with position equal or
	 * greater than position.
	 * 
	 * @param colId
	 * @param position
	 */
	public void shiftRecordsToRight(ObjectId colId, int position) {
		BiConsumer<String, UpdateOperations> update = (String field,
				UpdateOperations updateOpsPar) -> updateOpsPar.inc(field);
		shift(colId, position, update);
	}

	public void shiftRecordsToRight(ObjectId colId, int startPosition,
			int stopPosition) {
		BiConsumer<String, UpdateOperations> update = (String field,
				UpdateOperations updateOpsPar) -> updateOpsPar.inc(field);
		shift(colId, startPosition, stopPosition, update);
	}
	
	public void updateContextData(ContextData contextData) {
		ObjectId colId = contextData.getTarget().getCollectionId();
		int position = contextData.getTarget().getPosition();
		Query<RecordResource> q = this.createQuery().field("collectedIn")
				.hasThisElement(new CollectionInfo(colId, position));
		UpdateOperations<RecordResource> recordUpdate1 = this
				.createUpdateOperations();
		recordUpdate1.removeAll("contextData", new ContextData(colId, position));
		this.update(q, recordUpdate1);
		UpdateOperations<RecordResource> recordUpdate2 = this
				.createUpdateOperations();
		recordUpdate2.add("contextData", contextData);
		this.update(q, recordUpdate2);
	}

	public void updateRecordUsageCollectedAndRights(CollectionInfo colInfo,
			WithAccess access, ObjectId recordId, ObjectId colId) {
		Query<RecordResource> q = this.createQuery().field("_id")
				.equal(recordId);
		UpdateOperations<RecordResource> recordUpdate = this
				.createUpdateOperations();
		//TODO: what if already entry with the same colId-position? have to remove first to avoid duplicates.
		recordUpdate.add("collectedIn", colInfo);
		if (access != null)
			recordUpdate.set("administrative.access", access);
		// recordUpdate.set("administrative.lastModified", new Date());//do we
		// want to update lastModified?
		// the rights of the collection are copied to the resource
		// if the resource is added
		// to a collection whose owner is the owner of the resource
		// CollectionObject co = DB.getCollectionObjectDAO().getById(colId, new
		// ArrayList<String>(Arrays.asList("descriptiveData.label.default",
		// "administrative.withCreator")));
		if (DB.getCollectionObjectDAO().isFavorites(colId))
			recordUpdate.inc("usage.likes");
		else
			recordUpdate.inc("usage.collected");
		// shiftRecordsToRight(colId, position+1);
		this.update(q, recordUpdate);
	}

	// TODO: has to be atomic as a whole
	// uses findAndModify for entryCount of respective collection
	public void addToCollection(ObjectId recordId, ObjectId colId,
			int position, boolean changeRecRights) {
		CollectionObject co = DB.getCollectionObjectDAO()
				.updateCollectionAdmin(colId);
		//old entry count (before addition)
		int entryCount = co.getAdministrative().getEntryCount();
		if (position > entryCount)
			position = entryCount;
		WithAccess newAccess = null;
		if (changeRecRights)
			newAccess = mergeParentCollectionRights(recordId, colId, co
					.getAdministrative().getAccess());
		shiftRecordsToRight(colId, position);
		updateRecordUsageCollectedAndRights(
				new CollectionInfo(colId, position), newAccess, recordId, colId);
		DB.getCollectionObjectDAO().addCollectionMediaAsync(colId, recordId);
	}

	public void appendToCollection(ObjectId recordId, ObjectId colId,
			boolean changeRecRights) {
		// increase entry count
		CollectionObject co = DB.getCollectionObjectDAO()
				.updateCollectionAdmin(colId);
		//old entry count (before addition)
		int entryCount = co.getAdministrative().getEntryCount();
		WithAccess newAccess = null;
		if (changeRecRights)
			newAccess = mergeParentCollectionRights(recordId, colId, co
					.getAdministrative().getAccess());
		updateRecordUsageCollectedAndRights(new CollectionInfo(colId,
				entryCount), newAccess, recordId, colId);
		DB.getCollectionObjectDAO().addCollectionMediaAsync(colId, recordId);
	}

	public WithAccess mergeParentCollectionRights(ObjectId recordId,
			ObjectId newColId, WithAccess newColAccess) {
		RecordResource record = this.getById(
				recordId,
				new ArrayList<String>(Arrays.asList("collectedIn",
						"administrative.access.isPublic",
						"administrative.withCreator")));
		List<ObjectId> parentCollections = getParentCollections(recordId);
		List<WithAccess> parentColAccess = new ArrayList<WithAccess>();
		for (ObjectId colId : parentCollections) {
			if (colId.equals(newColId))
				parentColAccess.add(newColAccess);
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
		RecordResource record = this.getById(
				recordId,
				new ArrayList<String>(Arrays.asList("collectedIn",
						"administrative.access.isPublic",
						"administrative.withCreator")));
		boolean mergedIsPublic = isPublic;
		List<ObjectId> parentCollections = getParentCollections(recordId);
		for (ObjectId colId : parentCollections) {
			CollectionObject parentCollection = DB.getCollectionObjectDAO()
					.getById(
							colId,
							new ArrayList<String>(Arrays
									.asList("administrative.access")));
			if (parentCollection.getAdministrative()
					.getAccess().getIsPublic())
				return true;
		}
		return mergedIsPublic;
	}

	public void updateMembersToMergedRights(ObjectId colId,
			AccessEntry newAccess, List<ObjectId> effectiveIds) {
		ArrayList<String> retrievedFields = new ArrayList<String>(
				Arrays.asList("_id", "administrative.access"));
		WithAccess colAccess = DB.getCollectionObjectDAO()
				.getById(colId, retrievedFields).getAdministrative()
				.getAccess();
		colAccess.addToAcl(newAccess);
		List<RecordResource> memberRecords = getByCollection(colId,
				retrievedFields);
		for (RecordResource r : memberRecords) {
			if (DB.getRecordResourceDAO().hasAccess(effectiveIds,
					Action.DELETE, r.getDbId())) {
				WithAccess mergedAccess = mergeParentCollectionRights(
						r.getDbId(), colId, colAccess);
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
				updateField(r.getDbId(), "administrative.access", mergedPublicity);
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
	
	public void updateMembersToNewPublicity(ObjectId colId, 
			boolean isPublic, List<ObjectId> effectiveIds) {
		ArrayList<String> retrievedFields = new ArrayList<String>(
				Arrays.asList("_id"));
		List<RecordResource> memberRecords = getByCollection(colId,
				retrievedFields);
		for (RecordResource r : memberRecords) {
			if (DB.getRecordResourceDAO().hasAccess(effectiveIds,
					Action.DELETE, r.getDbId()))
				DB.getRecordResourceDAO().updateField(r.getDbId(), "administrative.access.isPublic", isPublic);
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
			for (ContextData c: contextData) {
				ContextDataTarget target = c.getTarget();
				if (target.getCollectionId().equals(colId) &&
						target.getPosition() == oldPosition) {
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
		q.field("collectedIn")
				.hasThisElement(new CollectionInfo(colId, position));
		return this.findOne(q);
	}
	
	public boolean existsInCollectionAndPosition(ObjectId colId,
			Integer position) {
		Query<RecordResource> q = this.createQuery().field("collectedIn")
				.hasThisElement(new CollectionInfo(colId, position));
		return this.find(q.limit(1)).asList().size() == 0? false : true;
	}
	
	public boolean existsInCollection(ObjectId colId) {
		Query<RecordResource> q = this.createQuery().field("collectedIn")
				.hasThisElement(new CollectionInfo(colId, null));
		return this.find(q.limit(1)).asList().size() == 0? false : true;
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
