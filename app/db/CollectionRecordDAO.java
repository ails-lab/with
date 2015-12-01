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

import java.util.List;

import model.CollectionRecord;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import play.Logger;
import play.Logger.ALogger;

public class CollectionRecordDAO extends DAO<CollectionRecord> {
	public static final ALogger log = Logger.of(CollectionRecord.class);

	public CollectionRecordDAO() {
		super(CollectionRecord.class);
	}

	//added
	public CollectionRecord getById(ObjectId id) {
		Query<CollectionRecord> q = this.createQuery().field("_id").equal(id);
		return this.findOne(q);
	}

	//added
	/**
	 * Retrieve records from specific collection
	 *
	 * @param colId
	 * @return
	 */
	public List<CollectionRecord> getByCollection(ObjectId colId) {
		return getByCollectionOffsetCount(colId, 0, -1);
	}
	

	//added
	/**
	 * Retrieve records from specific collection by offset and count
	 *
	 * @param colId, offset, count
	 * @return
	 */
	public List<CollectionRecord> getByCollectionOffsetCount(ObjectId colId,
			int offset, int count) {
		Query<CollectionRecord> q = this.createQuery().field("collectionId")
				.equal(colId).offset(offset).limit(count);
		return this.find(q).asList();
	}

	
	//added - not used like this
	/**
	 * Retrieve the total amount of records within a collection
	 */
	public long getItemCount(ObjectId colId) {
		Query<CollectionRecord> q = this.createQuery().field("collectionId")
				.equal(colId);
		return this.find(q).countAll();
	}

	//added
	public List<CollectionRecord> getBySource(String sourceId) {
		Query<CollectionRecord> q = this.createQuery()
		// .field("source").equal(source)
				.field("sourceId").equal(sourceId);
		return this.find(q).asList();
	}

	//added
	public List<CollectionRecord> getByExternalId(ObjectId colId, String extId) {
		Query<CollectionRecord> q = this.createQuery().field("collectionId")
				.equal(colId).field("externalId").equal(extId);
		return this.find(q).asList();
	}

	//added
	public int getTotalLikes(String extId) {
		Query<CollectionRecord> q = this.createQuery().field("externalId")
				.equal(extId).limit(1);
		List<CollectionRecord> list = this.find(q).asList();
		if (list.size() > 0)
			return 0;//list.get(0).getTotalLikes();
		else
			return 0;
	}

	//added
	public long countBySource(String sourceId) {
		Query<CollectionRecord> q = this.createQuery()
		// .field("source").equal(source)
				.field("sourceId").equal(sourceId);
		return this.find(q).countAll();
	}

	//added
	public List<CollectionRecord> getByExternalId(String extId) {
		Query<CollectionRecord> q = this.createQuery().field("externalId")
				.equal(extId);
		return this.find(q).asList();
	}

	//added
	public long countByExternalId(String extId) {
		Query<CollectionRecord> q = this.createQuery()
		// .field("source").equal(source)
				.field("externalId").equal(extId);
		return this.find(q).countAll();
	}

	//added
	public void shiftRecordsToRight(ObjectId colId, int position) {
		Query<CollectionRecord> q = this.createQuery().field("collectionId")
				.equal(colId).field("position").greaterThanOrEq(position);
		UpdateOperations<CollectionRecord> updateOps = this
				.createUpdateOperations().inc("position");
		this.update(q, updateOps);
	}

	//added
	public void shiftRecordsToLeft(ObjectId colId, int position) {
		Query<CollectionRecord> q = this.createQuery().field("collectionId")
				.equal(colId).field("position").greaterThan(position);
		UpdateOperations<CollectionRecord> updateOps = this
				.createUpdateOperations().dec("position");
		this.update(q, updateOps);
	}

	//added
	/**
	 * This method is to update the 'public' field on all the records of a
	 * collection. By default update method is invoked to all documents of a
	 * collection.
	 *
	 **/
	public void setSpecificRecordField(ObjectId colId, String fieldName,
			String value) {
		Query<CollectionRecord> q = this.createQuery().field("collectionId")
				.equal(colId);
		UpdateOperations<CollectionRecord> updateOps = this
				.createUpdateOperations();
		updateOps.set(fieldName, value);
		this.update(q, updateOps);
	}

	//added
	public void updateContent(ObjectId recId, String format, String content) {
		Query<CollectionRecord> q = this.createQuery().field("_id")
				.equal(recId);
		UpdateOperations<CollectionRecord> updateOps = this
				.createUpdateOperations();
		updateOps.set("content."+format, content);
		this.update(q, updateOps);

	}

	//added
	public boolean checkMergedRecordVisibility(String extId, ObjectId dbId) {
		List<CollectionRecord> mergedRecord = getByExternalId(extId);
		for (CollectionRecord mr : mergedRecord) {
			if (mr.getCollection().getIsPublic() && !mr.getDbId().equals(dbId))
				return true;
		}
		return false;
	}

	//added
	public void incrementLikes(String externalId) {
		Query<CollectionRecord> q = this.createQuery().field("externalId")
				.equal(externalId);
		UpdateOperations<CollectionRecord> updateOps = this
				.createUpdateOperations();
		updateOps.inc("totalLikes");
		this.update(q, updateOps);
	}

	//added
	public void decrementLikes(String externalId) {
		Query<CollectionRecord> q = this.createQuery().field("externalId")
				.equal(externalId);
		UpdateOperations<CollectionRecord> updateOps = this
				.createUpdateOperations();
		updateOps.dec("totalLikes");
		this.update(q, updateOps);
	}
}
