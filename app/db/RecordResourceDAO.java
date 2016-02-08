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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.BasicDBObject;

import model.CollectionRecord;
import model.resources.CollectionObject.CollectionAdmin;
import model.annotations.ContextData;
import model.basicDataTypes.CollectionInfo;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import model.basicDataTypes.WithAccess.AccessEntry;
import model.resources.AgentObject;
import model.resources.CollectionObject;
import model.resources.CulturalObject;
import model.resources.EUscreenObject;
import model.resources.EventObject;
import model.resources.PlaceObject;
import model.resources.RecordResource;
import model.resources.TimespanObject;
import model.resources.WithResource;


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
		Query<RecordResource> q = this.createQuery().field("collectedIn.coldId").exists();
		return this.deleteByQuery(q).getN();
	}

	public List<RecordResource> getByCollectionBetweenPositions(ObjectId colId, int lowerBound, int upperBound, List<String> retrievedFields) {
		Query<RecordResource> q = this.createQuery().retrievedFields(true,  retrievedFields.toArray(new String[retrievedFields.size()]));
		return getByCollectionBetweenPositions(colId, lowerBound, upperBound, q);
	}
	
	public List<RecordResource> getByCollectionBetweenPositions(ObjectId colId, int lowerBound, int upperBound) {
		Query<RecordResource> q = this.createQuery();
		return getByCollectionBetweenPositions(colId, lowerBound, upperBound, q);
	}
	
	/**
	 * Retrieve records from specific collection whose position
	 * is between lowerBound and upperBound. If a record appears n times in a collection (in different positions), n copies will appear
	 * in the returned list, in the respective positions.
	 *
	 * @param colId, lowerBound, upperBound
	 * @return
	 */
	public List<RecordResource> getByCollectionBetweenPositions(ObjectId colId, int lowerBound, int upperBound, Query<RecordResource> q) {
		BasicDBObject colIdQuery = new BasicDBObject();
		colIdQuery.put("collectionId", colId);
		BasicDBObject geq = new BasicDBObject();
		geq.put("$gte", lowerBound);
		geq.append("$lt", upperBound);
		colIdQuery.append("position", geq);
		BasicDBObject elemMatch1 = new BasicDBObject();
		elemMatch1.put("$elemMatch", colIdQuery);
		q.filter("collectedIn", elemMatch1);
		List<RecordResource> resources  = this.find(q).asList();
		/*DBCursor cursor = this.getDs().getCollection(entityClass).find(query);
		List<T> ds = new ArrayList<T>();
		while (cursor.hasNext()) {
		   DBObject o = cursor.next();
		   T d = (T) DB.getMorphia().fromDBObject(entityClass, o);
		   ds.add(d);
		}*/
		List<RecordResource> repeatedResources = new ArrayList<RecordResource>(upperBound-lowerBound);
		for (int i=0; i<(upperBound - lowerBound); i++) {
			repeatedResources.add(new RecordResource());
		}
		int maxPosition = -1;
		for (RecordResource d: resources) {
			ArrayList<CollectionInfo> collectionInfos = (ArrayList<CollectionInfo>) d.getCollectedIn();
			//May be a long iteration, if a record belongs to many collections
			for (CollectionInfo ci: collectionInfos) {
				ObjectId collectionId = ci.getCollectionId();
				if (collectionId.equals(colId)) {
					int pos = ci.getPosition();
					if ((lowerBound <= pos) && (pos < upperBound)) {
						int arrayPosition = pos - lowerBound;
						if (arrayPosition > maxPosition)
							maxPosition = arrayPosition;
						repeatedResources.add(arrayPosition, d);
					}
				}
			}
		}
		if (maxPosition > -1)
			return repeatedResources.subList(0, maxPosition+1);
		else
			return new ArrayList<RecordResource>();
	}
	
	
	public List<RecordResource> getByCollection(ObjectId colId) {
		Query<RecordResource> q = this.createQuery();
		return getByCollection(colId, q);
	}
	
	public List<RecordResource> getByCollection(ObjectId colId, List<String> retrievedFields) {
		Query<RecordResource> q = this.createQuery().retrievedFields(true,  retrievedFields.toArray(new String[retrievedFields.size()]));
		return getByCollection(colId, q);
	}

	/**
	 * Retrieve all records that belong to that collection. 
	 * If a record is included several times in a collection, it will only appear a single time in the returned list
	 *
	 * @param colId
	 * @return
	 */
	public List<RecordResource> getByCollection(ObjectId colId, Query<RecordResource> q) {
		BasicDBObject colIdQuery = new BasicDBObject();
		colIdQuery.put("collectionId", colId);
		BasicDBObject elemMatch1 = new BasicDBObject();
		elemMatch1.put("$elemMatch", colIdQuery);
		return q.filter("collectedIn", elemMatch1).asList();
		//int MAX = 10000;
		//return getByCollectionBetweenPositions(colId, 0, MAX);
	}
	

	public void shift(ObjectId colId, int position, BiConsumer<String, UpdateOperations> update) {
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
		List<RecordResource> resources  = this.find(q).asList();
		for (RecordResource resource: resources) {
			UpdateOperations<RecordResource> updateOps = this.createUpdateOperations().disableValidation();
			List<CollectionInfo> collectedIn = resource.getCollectedIn();
			int index = 0;
			for (CollectionInfo ci: collectedIn) {
				if (ci.getCollectionId().equals(colId)) {
					int pos = ci.getPosition();
					if (pos >= position)
						update.accept("collectedIn."+index+".position", updateOps);
				}
				index+=1;
			}
			this.update(this.createQuery().field("_id").equal(resource.getDbId()), updateOps);
		}
	}

	public void shift(ObjectId colId, int startPosition, int stopPosition, BiConsumer<String, UpdateOperations> update) {
		Query<RecordResource> q = this.createQuery();
		BasicDBObject colIdQuery = new BasicDBObject();
		colIdQuery.put("collectionId", colId);
		BasicDBObject elemMatch2 = new BasicDBObject();
		BasicDBObject geq = new BasicDBObject();
		geq.put("$gte", startPosition);
		geq.put("$le", stopPosition);
		colIdQuery.append("position", geq);
		BasicDBObject elemMatch1 = new BasicDBObject();
		elemMatch1.put("$elemMatch", colIdQuery);
		q.filter("collectedIn", elemMatch1);
		ArrayList<String> retrievedFields = new ArrayList<String>();
		retrievedFields.add("collectedIn");
		List<RecordResource> resources  = this.find(q.retrievedFields(true, retrievedFields.toArray(new String[retrievedFields.size()]))).asList();
		for (RecordResource resource: resources) {
			UpdateOperations<RecordResource> updateOps = this.createUpdateOperations().disableValidation();
			List<CollectionInfo> collectedIn = resource.getCollectedIn();
			int index = 0;
			for (CollectionInfo ci: collectedIn) {
				if (ci.getCollectionId().equals(colId)) {
					int pos = ci.getPosition();
					if ((pos >= startPosition) && (pos <= stopPosition))
						update.accept("collectedIn."+index+".position", updateOps);
				}
				index+=1;
			}
			this.update(this.createQuery().field("_id").equal(resource.getDbId()), updateOps);
		}
	}

	/**
	 * Shift one position left all resources in colId with position equal or greater than position.
	 * @param colId
	 * @param position
	 */
	public void shiftRecordsToLeft(ObjectId colId, int position) {
		//UpdateOperations updateOps = this.createUpdateOperations();
		BiConsumer<String, UpdateOperations> update = (String field, UpdateOperations updateOpsPar) -> updateOpsPar.dec(field);
		shift(colId, position, update);
	}

	public void shiftRecordsToLeft(ObjectId colId, int startPosition, int stopPosition) {
		//UpdateOperations updateOps = this.createUpdateOperations();
		BiConsumer<String, UpdateOperations> update = (String field, UpdateOperations updateOpsPar) -> updateOpsPar.dec(field);
		shift(colId, startPosition, stopPosition, update);
	}

	/**
	 * Shift one position right all resources in colId with position equal or greater than position.
	 * @param colId
	 * @param position
	 */
	public void shiftRecordsToRight(ObjectId colId, int position) {
		BiConsumer<String, UpdateOperations> update = (String field, UpdateOperations updateOpsPar) -> updateOpsPar.inc(field);
		shift(colId, position, update);
	}

	public void shiftRecordsToRight(ObjectId colId, int startPosition, int stopPosition) {
		BiConsumer<String, UpdateOperations> update = (String field, UpdateOperations updateOpsPar) -> updateOpsPar.inc(field);
		shift(colId, startPosition, stopPosition, update);
	}	
	public void updateRecordUsageCollectedAndRights(CollectionInfo colInfo, WithAccess access, ObjectId recordId, ObjectId colId) {
		Query<RecordResource> q = this.createQuery().field("_id").equal(recordId);
		UpdateOperations<RecordResource> recordUpdate = this.createUpdateOperations();
		recordUpdate.add("collectedIn", colInfo);
		if (access!= null)
			recordUpdate.set("administrative.access", access);
		//recordUpdate.set("administrative.lastModified", new Date());//do we want to update lastModified?
		//the rights of the collection are copied to the resource
		// if the resource is added 
		// to a collection whose owner is the owner of the resource
		//CollectionObject co = DB.getCollectionObjectDAO().getById(colId, new ArrayList<String>(Arrays.asList("descriptiveData.label.default", "administrative.withCreator")));
		if (DB.getCollectionObjectDAO().isFavorites(colId))
			recordUpdate.inc("usage.likes");
		else
			recordUpdate.inc("usage.collected");
		//shiftRecordsToRight(colId, position+1);
		this.update(q, recordUpdate);
	}
	
	//TODO: has to be atomic as a whole
	//uses findAndModify for entryCount of respective collection
	public void addToCollection(ObjectId recordId, ObjectId colId, int position, boolean changeRecRights) {
		CollectionObject co = DB.getCollectionObjectDAO().updateCollectionAdmin(colId);
		int entryCount = co.getAdministrative().getEntryCount();//old entry count (before addition)
		if (position > entryCount)
			position = entryCount;
		DB.getCollectionObjectDAO().addCollectionMedia(colId, recordId, position);
		WithAccess newAccess = null;
		if (changeRecRights)
			newAccess = mergeParentCollectionRights(recordId, colId, co.getAdministrative().getAccess());
		updateRecordUsageCollectedAndRights(new CollectionInfo(colId, position), newAccess, recordId, colId);
		shiftRecordsToRight(colId, position);
	}
	
	
	public void appendToCollection(ObjectId recordId, ObjectId colId, boolean changeRecRights) {
		//increase entry count
		CollectionObject co = DB.getCollectionObjectDAO().updateCollectionAdmin(colId);
		int entryCount = ((CollectionAdmin) co.getAdministrative()).getEntryCount();//old entry count (before addition)
		DB.getCollectionObjectDAO().addCollectionMedia(colId, recordId, entryCount);
		WithAccess newAccess = null;
		if (changeRecRights)
			newAccess = mergeParentCollectionRights(recordId, colId, co.getAdministrative().getAccess());
		updateRecordUsageCollectedAndRights(new CollectionInfo(colId, entryCount), newAccess, recordId, colId);
	}
	
	public WithAccess mergeParentCollectionRights(ObjectId recordId, ObjectId newColId, WithAccess newColAccess) {
		RecordResource record = this.getById(recordId, new ArrayList<String>(Arrays.asList("collectedIn", "administrative.access.isPublic", "administrative.withCreator")));
		List<ObjectId> parentCollections = getParentCollections(recordId);
		List<WithAccess> parentColAccess = new ArrayList<WithAccess>();
		for (ObjectId colId: parentCollections) {
			if (colId.equals(newColId)) 
				parentColAccess.add(newColAccess);
			else {
				CollectionObject parentCollection = DB.getCollectionObjectDAO().getById(colId, new ArrayList<String>(Arrays.asList("administrative.access")));
				parentColAccess.add(parentCollection.getAdministrative().getAccess());
			}
		}
		//hope there aren't too many collections containing the resource
		//if the record is public, it should remain public. Acl rights are determined by the collections the record belongs to
		return mergeRights(parentColAccess, record.getAdministrative().getWithCreator(), record.getAdministrative().getAccess().getIsPublic());
	}
	
	public void updateMembersToMergedRights(ObjectId colId, AccessEntry newAccess) {
		ArrayList<String> retrievedFields = new ArrayList<String>(Arrays.asList("_id", "administrative.access"));
		WithAccess colAccess = DB.getCollectionObjectDAO().getById(colId, retrievedFields).getAdministrative().getAccess();
		colAccess.addToAcl(newAccess);
		List<RecordResource> memberRecords = getByCollection(colId, retrievedFields);
		for (RecordResource r: memberRecords) {
			WithAccess mergedAccess = mergeParentCollectionRights(r.getDbId(), colId, colAccess);
			updateField(r.getDbId(), "administrative.access", mergedAccess);
		}
	}
	
	public void updateRecordRightsUponRemovalFromCollection(ObjectId recordId, ObjectId colId) {
		RecordResource record = this.getById(recordId, new ArrayList<String>(Arrays.asList("collectedIn", "administrative.access.isPublic", "administrative.withCreator")));
		List<ObjectId> parentCollections = getParentCollections(recordId);
		parentCollections.remove(colId);
		List<WithAccess> parentColAccess = new ArrayList<WithAccess>();
		for (ObjectId parentId: parentCollections) {
			CollectionObject parentCollection = DB.getCollectionObjectDAO().getById(parentId, new ArrayList<String>(Arrays.asList("administrative.access")));
			parentColAccess.add(parentCollection.getAdministrative().getAccess());
		}
		WithAccess mergedAccess =  mergeRights(parentColAccess, record.getAdministrative().getWithCreator(), record.getAdministrative().getAccess().getIsPublic());
		updateField(recordId, "administrative.access", mergedAccess);
	}
	
	public void updateMembersToNewAccess(ObjectId colId, ObjectId userId, Access newAccess) {
		ArrayList<String> retrievedFields = new ArrayList<String>(Arrays.asList("_id"));
		List<RecordResource> memberRecords = getByCollection(colId, retrievedFields);
		for (RecordResource r: memberRecords) {
			changeAccess(r.getDbId(), userId, newAccess);
		}
	}

	//TODO: have to test
	public void updatePosition(ObjectId resourceId, ObjectId colId, int oldPosition, int newPosition) {
		UpdateOperations<RecordResource> updateOps = this.createUpdateOperations();
		Query<RecordResource> q = this.createQuery().field("_id").equal(resourceId);
		updateOps.add("collectedIn", new CollectionInfo(colId, newPosition));
		updateOps.removeAll("collectedIn", new CollectionInfo(colId, oldPosition));
		this.update(q, updateOps);
	}

	public void removeFromCollection(ObjectId recordId, ObjectId colId, int position) {
		UpdateOperations<RecordResource> updateOps = this.createUpdateOperations();
		Query<RecordResource> q = this.createQuery().field("_id").equal(recordId);
		updateOps.removeAll("collectedIn", new CollectionInfo(colId, position));
		updateOps.removeAll("ContextDatas", new ContextData(colId, position));
		this.update(q, updateOps);
		shiftRecordsToLeft(colId, position+1);
	}

	public RecordResource getByCollectionAndPosition(ObjectId colId, int position) {
		Query<RecordResource> q = this.createQuery().field("collectedIn").hasThisElement(new CollectionInfo(colId, position));
		return this.findOne(q);
	}
	
	public void editRecord(String root, ObjectId dbId, JsonNode json) {
		Query<RecordResource> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<RecordResource> updateOps = this.createUpdateOperations();
		updateFields(root, json, updateOps);
		updateOps.set("administrative.lastModified", new Date());
		this.update(q, updateOps);
	}
	
	public void removeAllRecordsFromCollection(ObjectId colId) {
		ArrayList<String> retrievedFields = new ArrayList<String>(Arrays.asList("_id", "collectedIn"));
		List<RecordResource> memberRecords = getByCollection(colId, retrievedFields);
		for (RecordResource record: memberRecords) {
			ObjectId recordId = record.getDbId();
			List<CollectionInfo> collectedIn = record.getCollectedIn();
			if (collectedIn.size() > 1) {
				UpdateOperations<RecordResource> updateOps = this.createUpdateOperations();
				Query<RecordResource> q = this.createQuery().field("_id").equal(recordId);
				//TODO: check that the null value indeed works for ignoring position
				updateOps.removeAll("collectedIn", new CollectionInfo(colId, null));
				//TODO See if it is collected in the same collection more than once
				updateOps.dec("usage.collected");
				this.update(q, updateOps);
			}
			else
				deleteById(recordId);
		}
	}
	
}
