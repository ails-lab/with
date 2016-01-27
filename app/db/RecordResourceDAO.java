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
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;

import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.BasicDBObject;

import model.CollectionRecord;
import model.resources.CollectionObject.CollectionAdmin;
import model.basicDataTypes.CollectionInfo;
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


	/**
	 * Retrieve records from specific collection whose position
	 * is is between lowerBound and upperBound
	 *
	 * @param colId, lowrBound, upperBound
	 * @return
	 */
	public List<RecordResource> getByCollectionBetweenPositions(ObjectId colId, int lowerBound, int upperBound) {
		Query<RecordResource> q = this.createQuery();
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

	/**
	 * Retrieve all records from specific collection checking
	 * out for duplicates and restore them.
	 *
	 * @param colId
	 * @return
	 */
	public List<RecordResource> getByCollection(ObjectId colId) {
		int MAX = 10000;
		return getByCollectionBetweenPositions(colId, 0, MAX);
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
			UpdateOperations updateOps = this.createUpdateOperations().disableValidation();
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
		List<RecordResource> resources  = this.find(q).asList();
		for (RecordResource resource: resources) {
			UpdateOperations updateOps = this.createUpdateOperations().disableValidation();
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

	public CollectionObject updateCollectionAdmin(ObjectId colId) {
		UpdateOperations<CollectionObject> colUpdate = DB.getCollectionObjectDAO().createUpdateOperations().disableValidation();
		Query<CollectionObject> cq = DB.getCollectionObjectDAO().createQuery().field("_id").equal(colId);
		colUpdate.set("administrative.lastModified", new Date());
		colUpdate.inc("administrative.entryCount");
		return DB.getDs().findAndModify(cq, colUpdate, true);//true returns the oldVersion\
	}
	
	public void updateRecordUsageAndCollected(CollectionInfo colInfo,  ObjectId recordId, ObjectId colId) {
		Query<RecordResource> q = this.createQuery().field("_id").equal(recordId);
		UpdateOperations<RecordResource> recordUpdate = this.createUpdateOperations();
		recordUpdate.add("collectedIn", colInfo);
		if (DB.getCollectionObjectDAO().isFavorites(colId))
			recordUpdate.inc("usage.likes");
		else
			recordUpdate.inc("usage.collected");
		//shiftRecordsToRight(colId, position+1);
		this.update(q, recordUpdate);

	}
	
	//TODO: has to be atomic as a whole
	public void addToCollection(ObjectId resourceId, ObjectId colId, int position) {
		CollectionObject co = updateCollectionAdmin(colId);
		updateRecordUsageAndCollected( new CollectionInfo(colId, ((CollectionAdmin) co.getAdministrative()).getEntryCount()), resourceId, colId);
		shiftRecordsToRight(colId, position+1);
	}

	//TODO: have to test
	public void updatePosition(ObjectId resourceId, ObjectId colId, int oldPosition, int newPosition) {
		UpdateOperations<RecordResource> updateOps = this.createUpdateOperations();
		Query<RecordResource> q = this.createQuery().field("_id").equal(resourceId);
		updateOps.add("collectedIn", new CollectionInfo(colId, newPosition));
		updateOps.removeAll("collectedIn", new CollectionInfo(colId, oldPosition));
		this.update(q, updateOps);
	}
	

	//TODO: use findAndModify for entryCount of respective collection
	//what if the append fails (for some strange reason, the record cannot be edited correctly)
	//and the entry count has been increased already?
	public void appendToCollection(ObjectId resourceId, ObjectId colId) {
		//increase entry count
		CollectionObject co = updateCollectionAdmin(colId);
		updateRecordUsageAndCollected( new CollectionInfo(colId, ((CollectionAdmin) co.getAdministrative()).getEntryCount()), resourceId, colId);
	}


	public void removeFromCollection(ObjectId resourceId, ObjectId colId, int position) {
		UpdateOperations<RecordResource> updateOps = this.createUpdateOperations();
		Query<RecordResource> q = this.createQuery().field("_id").equal(resourceId);
		updateOps.removeAll("collectedIn", new CollectionInfo(colId, position));
		this.update(q, updateOps);
		shiftRecordsToLeft(colId, position+1);
	}

	public RecordResource getByCollectionAndPosition(ObjectId colId, int position) {
		Query<RecordResource> q = this.createQuery().field("collectedIn").hasThisElement(new CollectionInfo(colId, position));
		return this.findOne(q);
	}
	
	public void editRecord(String root, ObjectId dbId, JsonNode json) {
		Query<RecordResource> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<RecordResource> updateOps = this
				.createUpdateOperations();
		updateFields(root, json, updateOps);
		updateOps.set("administrative.lastModified", new Date());
		this.update(q, updateOps);
	}
}
