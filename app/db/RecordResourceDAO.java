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

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import com.mongodb.BasicDBObject;

import model.CollectionRecord;
import model.basicDataTypes.CollectionInfo;
import model.resources.AgentObject;
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
public class RecordResourceDAO extends CommonResourceDAO<RecordResource> {

	public RecordResourceDAO() {super(RecordResource.class);}

	public RecordResourceDAO(Class<?> entityClass) {
		super(RecordResource.class);
	}

	/*
	 * DAO Methods
	 */
	public int deleteByCollection(ObjectId colId) {
		Query<RecordResource> q = this.createQuery().field("collectedIn.coldId").exists();
		return this.deleteByQuery(q).getN();
	}

	/*
	 * ********************************************************
	 * These are embedded classes for very specific queries   *
	 * in the far future.                                     *
	 * ********************************************************
	 */
	/*
	public class AgentObjectDAO extends CommonResourceDAO<AgentObject> {

		public AgentObjectDAO(Class<?> entityClass) {
			super(entityClass);
		}

	}

	public class CulturalObjectDAO extends CommonResourceDAO<CulturalObject> {

		public CulturalObjectDAO(Class<?> entityClass) {
			super(entityClass);
		}

	}

	public class EuscreenObjectDAO extends CommonResourceDAO<EUscreenObject> {

		public EuscreenObjectDAO(Class<?> entityClass) {
			super(entityClass);
		}

	}

	public class EventObjectDAO extends CommonResourceDAO<EventObject> {

		public EventObjectDAO(Class<?> entityClass) {
			super(entityClass);
		}

	}

	public class PlaceObjectDAO extends CommonResourceDAO<PlaceObject> {

		public PlaceObjectDAO(Class<?> entityClass) {
			super(entityClass);
		}

	}

	public class TimespanObjectDAO extends CommonResourceDAO<TimespanObject> {

		public TimespanObjectDAO(Class<?> entityClass) {
			super(entityClass);
		}

	}
	*/
	
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
		List<RecordResource> resources  = (List<RecordResource>) this.find(q).asList();
		for (RecordResource resource: resources) {
			UpdateOperations updateOps = this.createUpdateOperations().disableValidation();
			ArrayList<CollectionInfo> collectedIn = resource.getCollectedIn();
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
	
	/**
	 * Shift one position right all resources in colId with position equal or greater than position.
	 * @param colId
	 * @param position
	 */
	public void shiftRecordsToRight(ObjectId colId, int position) {
		BiConsumer<String, UpdateOperations> update = (String field, UpdateOperations updateOpsPar) -> updateOpsPar.inc(field);
		shift(colId, position, update);
	}
	
	public void addToCollection(ObjectId resourceId, ObjectId colId, int position) {
		UpdateOperations<RecordResource> updateOps = this.createUpdateOperations();
		Query<RecordResource> q = this.createQuery().field("_id").equal(resourceId);
		updateOps.add("collectedIn", new CollectionInfo(colId, position));
		this.update(q, updateOps);
		shiftRecordsToRight(colId, position+1);
	}
	
	public void removeFromCollection(ObjectId resourceId, ObjectId colId, int position) {
		UpdateOperations<RecordResource> updateOps = this.createUpdateOperations();
		Query<RecordResource> q = this.createQuery().field("_id").equal(resourceId);
		updateOps.removeAll("collectedIn", new CollectionInfo(colId, position));
		this.update(q, updateOps);
		shiftRecordsToLeft(colId, position+1);
	}
}
