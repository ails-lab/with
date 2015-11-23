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
import model.resources.CulturalObject;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import play.Logger;
import play.Logger.ALogger;

public class CulturalObjectDAO extends DAO<CulturalObject> {
	public static final ALogger log = Logger.of(CulturalObject.class);

	public CulturalObjectDAO() {
		super(CollectionRecord.class);
	}

	public CulturalObject getById(ObjectId id) {
		Query<CulturalObject> q = this.createQuery().field("_id").equal(id);
		return this.findOne(q);
	}

	/**
	 * Retrieve records from specific collection
	 *
	 * @param colId
	 * @return
	 */
	public List<CulturalObject> getByCollection(ObjectId colId) {
		return getByCollectionOffsetCount(colId, 0, -1);
	}

	/**
	 * Retrieve records from specific collection by offset and count
	 *
	 * @param colId
	 *            , offset, count
	 * @return
	 */
	public List<CulturalObject> getByCollectionOffsetCount(ObjectId colId,
			int offset, int count) {
		Query<CulturalObject> q = this.createQuery().field("")
				.equal(colId).offset(offset).limit(count);
		return this.find(q).asList();
	}

	public List<CulturalObject> getBySource(String sourceId) {
		Query<CulturalObject> q = this.createQuery()
		// .field("source").equal(source)
				.field("sourceId").equal(sourceId);
		return this.find(q).asList();
	}

	public List<CulturalObject> getByExternalId(ObjectId colId, String extId) {
		Query<CulturalObject> q = this.createQuery().field("collectionId")
				.equal(colId).field("externalId").equal(extId);
		return this.find(q).asList();
	}

	public int getTotalLikes(String extId) {
		Query<CulturalObject> q = this.createQuery().field("externalId")
				.equal(extId).limit(1);
		List<CulturalObject> list = this.find(q).asList();
		if (list.size() > 0)
			return list.get(0).getTotalLikes();
		else
			return 0;
	}

	public long countBySource(String sourceId) {
		Query<CulturalObject> q = this.createQuery()
		// .field("source").equal(source)
				.field("sourceId").equal(sourceId);
		return this.find(q).countAll();
	}

	public List<CulturalObject> getByExternalId(String extId) {
		Query<CulturalObject> q = this.createQuery().field("externalId")
				.equal(extId);
		return this.find(q).asList();
	}

	public long countByExternalId(String extId) {
		Query<CulturalObject> q = this.createQuery()
		// .field("source").equal(source)
				.field("externalId").equal(extId);
		return this.find(q).countAll();
	}

	public int deleteByCollection(ObjectId colId) {
		Query<CulturalObject> q = this.createQuery().field("collectionId")
				.equal(colId);
		return this.deleteByQuery(q).getN();
	}

	public void CulturalObject(ObjectId colId, int position) {
		Query<CulturalObject> q = this.createQuery().field("collectionId")
				.equal(colId).field("position").greaterThanOrEq(position);
		UpdateOperations<CulturalObject> updateOps = this
				.createUpdateOperations().inc("position");
		this.update(q, updateOps);
	}

	public void shiftRecordsToLeft(ObjectId colId, int position) {
		Query<CulturalObject> q = this.createQuery().field("collectionId")
				.equal(colId).field("position").greaterThan(position);
		UpdateOperations<CulturalObject> updateOps = this
				.createUpdateOperations().dec("position");
		this.update(q, updateOps);
	}

	/**
	 * This method is to update the 'public' field on all the records of a
	 * collection. By default update method is invoked to all documents of a
	 * collection.
	 *
	 **/
	public void setSpecificRecordField(ObjectId colId, String fieldName,
			String value) {
		Query<CulturalObject> q = this.createQuery().field("collectionId")
				.equal(colId);
		UpdateOperations<CulturalObject> updateOps = this
				.createUpdateOperations();
		updateOps.set(fieldName, value);
		this.update(q, updateOps);
	}

	public void updateContent(ObjectId recId, String format, String content) {
		Query<CulturalObject> q = this.createQuery().field("_id")
				.equal(recId);
		UpdateOperations<CulturalObject> updateOps = this
				.createUpdateOperations();
		updateOps.set("content."+format, content);
		this.update(q, updateOps);

	}

	public boolean checkMergedRecordVisibility(String extId, ObjectId dbId) {
		List<CulturalObject> mergedRecord = getByExternalId(extId);
		for (CulturalObject mr : mergedRecord) {
			//if (mr.getCollection().getIsPublic() && !mr.getDbId().equals(dbId))
				return true;
		}
		return false;
	}

	public void incrementLikes(String externalId) {
		Query<CulturalObject> q = this.createQuery().field("externalId")
				.equal(externalId);
		UpdateOperations<CulturalObject> updateOps = this
				.createUpdateOperations();
		updateOps.inc("totalLikes");
		this.update(q, updateOps);
	}

	public void decrementLikes(String externalId) {
		Query<CulturalObject> q = this.createQuery().field("externalId")
				.equal(externalId);
		UpdateOperations<CulturalObject> updateOps = this
				.createUpdateOperations();
		updateOps.dec("totalLikes");
		this.update(q, updateOps);
	}
}
