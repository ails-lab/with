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

import model.resources.WithResource;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

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
	
	//TODO: limit to return only some fields (?)
	public List<T> getByCollectionSingles(ObjectId colId, int offset, int count) {
		Query<T> q = this.createQuery().field("collectedIn."+colId.toString()).exists()
				.offset(offset).limit(count);
		/*Query<T> q = this.createQuery().field("collectedIn").hasThisElement(new CollectionInfo(colId, null))
				.offset(offset).limit(count);*/
		return this.find(q).asList();
	}

	/**
	 * Retrieve all records from specific collection
	 *
	 * @param colId
	 * @return
	 */
	public List<T> getByCollectionAllInstances(ObjectId colId) {
		return getByCollectionOffsetCount(colId, 0, -1);
	}

	/**
	 * Retrieve records from specific collection by offset and count
	 *
	 * @param colId, offset, count
	 * @return
	 */
	public List<T> getByCollectionOffsetCount(ObjectId colId,
			int offset, int count) {
		List<T> Ts = getByCollectionSingles(colId, offset, count);
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
	 * Retrieve records from specific collection with position between lowerBound and upperBound
	 *
	 * @param colId, lowrBound, upperBound
	 * @return
	 */
	public List<T> getByCollectionPosition(ObjectId colId,
			int lowerBound, int upperBound) {
		Query<T> q = this.createQuery();
		String colField = "collectedIn."+colId;
		q.and(q.criteria(colField).exists(), q.criteria(colField).greaterThanOrEq(lowerBound), q.criteria(colField).lessThan(upperBound));
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

	public List<T> getBySource(String sourceName) {
		//TODO: faster if could query on last entry of provenance array. Mongo query!
		Query<T> q = this.createQuery()
				.field("provenance.provider").equal(sourceName);
		return this.find(q).asList();
	}

	public int getTotalLikes(ObjectId id) {
		T resource = getById(id);
		return resource.getUsage().getLikes();
	}

	/*
	public long countBySource(String sourceId) {
		Query<T> q = this.createQuery()
		// .field("source").equal(source)
				.field("sourceId").equal(sourceId);
		return this.find(q).countAll();
	}*/

	public List<T> getByExternalId(String extId) {
		Query<T> q = this.createQuery().field("externalId")
				.equal(extId);
		return this.find(q).asList();
	}

	public long countByExternalId(String extId) {
		Query<T> q = this.createQuery()
				.field("externalId").equal(extId);
		return this.find(q).countAll();
	}

	public void removeFromCollection(ObjectId resourceId, ObjectId colId, int position) {
		Query<T> q = this.createQuery().field("_id").equal(resourceId);
		UpdateOperations<T> updateOps = this.createUpdateOperations();
		//updateOps.removeAll("collectedIn", new CollectionInfo(colId, position));
		updateOps.removeAll("collectedIn."+colId, position);
		this.update(q, updateOps);
	}

	//TODO:Mongo query!
	public void shiftRecordsToLeft(ObjectId colId, int position) {
		Query<T> q = this.createQuery();
		String colField = "collectedIn."+colId;
		q.and(q.criteria(colField).exists(), q.criteria(colField).greaterThanOrEq(position));
		UpdateOperations<T> updateOps = this.createUpdateOperations();
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
		BasicDBObject colIdQuery = new BasicDBObject();
		BasicDBObject existsField = new BasicDBObject();
		existsField.put("$exists", true);
		colIdQuery.put("collectedIn.collId", existsField);
		BasicDBObject geq = new BasicDBObject();
		geq.put("$geq", position);
		colIdQuery.append("$elemMatch", geq);
		BasicDBObject update = new BasicDBObject();
		BasicDBObject entrySpec = new BasicDBObject();
		entrySpec.put("collectedIn."+colId+".$", 1);
		update.put("$dec", entrySpec);
		this.getDs().getCollection(entityClass.getSimpleName()).find(colIdQuery, update);
	}

	/**
	 * This method is to update the 'public' field on all the records of a
	 * collection. By default update method is invoked to all documents of a
	 * collection.
	 *
	 **/
	public void setSpecificRecordField(ObjectId colId, String fieldName,
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

	public boolean checkMergedRecordVisibility(String extId, ObjectId dbId) {
		List<T> mergedRecord = getByExternalId(extId);
		for (T mr : mergedRecord) {
			//if (mr.getCollection().getIsPublic() && !mr.getDbId().equals(dbId))
				return true;
		}
		return false;
	}

	public void incrementLikes(String externalId) {
		Query<T> q = this.createQuery().field("externalId")
				.equal(externalId);
		UpdateOperations<T> updateOps = this
				.createUpdateOperations();
		updateOps.inc("usage.likes");
		this.update(q, updateOps);
	}

	public void decrementLikes(String externalId) {
		Query<T> q = this.createQuery().field("externalId")
				.equal(externalId);
		UpdateOperations<T> updateOps = this
				.createUpdateOperations();
		updateOps.dec("usage.likes");
		this.update(q, updateOps);
	}

}
