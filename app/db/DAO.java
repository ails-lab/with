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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import model.basicDataTypes.WithAccess.Access;
import model.resources.RecordResource;
import model.resources.WithResource;
import model.usersAndGroups.User;

import org.bson.types.ObjectId;
import org.elasticsearch.common.lang3.ArrayUtils;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.dao.BasicDAO;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.CriteriaContainer;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryResults;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

import play.Logger;
import play.libs.F.Callback;
import utils.Tuple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.util.JSON;

public class DAO<E> extends BasicDAO<E, ObjectId> {
	
	public enum QueryOperator {GT("$gt"), EQ("$eq"), GTE( "$gte");
	
		private final String text;
	
		private QueryOperator(final String text) {
			this.text = text;
		}
	
		@Override
		public String toString() {
			return text;
		}
		
	}
	
	static private final Logger.ALogger log = Logger.of(DAO.class);

	protected final Class<E> entityClass;

	public DAO(Class<E> entityClass) {
		super(entityClass, DB.getDs());
		this.entityClass = entityClass;
	}

	public static DBObject asDBObject(String json) {
		DBObject res = (DBObject) JSON.parse(json);
		return res;
	}

	/**
	 * Convenience method for retrieving all values for this query on certain
	 * field. Use if you don't want the morphia treatment (you want values, not
	 * objects)
	 *
	 * @param res
	 * @param query
	 * @param field
	 */
	public void withCollection(Collection<String> res, String query,
			String field) {
		DBCursor cursor = null;
		try {
			BasicDBObject fieldProjector = new BasicDBObject();
			fieldProjector.put(field, 1);
			cursor = getCollection().find(asDBObject(query), fieldProjector);
			while (cursor.hasNext())
				res.add(cursor.next().get(field).toString());
		} finally {
			cursor.close();
		}
	}

	/**
	 * Return collection stats
	 */
	public String getCollectionStatistics() {
		String stats = this.getCollection().getStats().toString();
		if (stats != null)
			return stats;
		else {
			log.debug("No statistics returned for the "
					+ entityClass.getSimpleName() + "collection");
			return null;
		}

	}

	/**
	 * Execute on all matching entities and optionally write changes back to db.
	 *
	 * @param callback
	 * @param withWriteback
	 * @throws Exception
	 */
	public void onAll(Callback<E> callback, boolean withWriteback)
			throws Exception {
		Query<E> q = (Query<E>) this.getDs().createQuery(entityClass);
		QueryResults<E> qr = this.find(q);
		Iterator<E> i = qr.iterator();
		while (i.hasNext()) {
			try {
				E obj = i.next();
				callback.invoke(obj);
				if (withWriteback) {
					save(obj, WriteConcern.JOURNALED);
				}
			} catch (Throwable thr) {
				log.error("Iterate over " + entityClass.getSimpleName(), thr);
			}
		}
	}

	/**
	 * Drop a collection and all it's documents
	 */
	public void dropCollection() {
		try {
			DB.getDs().getCollection(entityClass).drop();
		} catch (MongoException me) {
			log.error("Cannot drop collection " + entityClass.getSimpleName(),
					me);
		}
	}

	/**
	 * Drop an index from a collection
	 */
	public void dropIndexFromCollection(String index) {
		try {
			DB.getDs().getCollection(entityClass).dropIndex(index);
		} catch (MongoException me) {
			log.error(
					"Cannot drop index from collection "
							+ entityClass.getSimpleName(), me);
		}
	}

	/**
	 * Drop all indexes from collection
	 */
	public void dropAllIndexesFromCollection() {
		try {
			DB.getDs().getCollection(entityClass).dropIndexes();
		} catch (MongoException me) {
			log.error("Cannot drop indexes from collection", me);
		}
	}

	/**
	 * Use this method to save and Object to the database
	 *
	 * @param record
	 */
	public Key<E> makePermanent(E doc) {
		try {
			return this.save(doc, WriteConcern.ACKNOWLEDGED);
		} catch (Exception e) {
			log.error("Cannot save " + doc.getClass().getSimpleName(), e);
		}
		return null;
	}

	/**
	 * Use this method to delete and Object to the database
	 *
	 * @param record
	 */
	public int makeTransient(E doc) {
		try {
			return this.delete(doc).getN();
		} catch (Exception e) {
			log.error("Cannot delete " + doc.getClass().getSimpleName(), e);
		}
		return -1;
	}

	/**
	 * Condition on the object needs to include the prefix 'obj.'! There is a
	 * separate entity manager doing the work here.
	 *
	 * @param condition
	 * @return Number of entries deleted
	 */
	public int removeAll(String field, String operator, Object value) {
		Query<E> q = this.createQuery();
		q.filter(field + " " + operator, value);
		int n = this.deleteByQuery(q).getN();
		return n;
	}

	/**
	 * Careful with this one! Too many results may come back.
	 *
	 * @return
	 */
	public List<E> getAll() {
		return find().asList();
	}
	
	/**
	 * Retrieve a resource from DB using its dbId
	 * @param id
	 * @return
	 */
	public E getById(ObjectId id) {
		Query<E> q = this.createQuery().field("_id").equal(id);
		return this.findOne(q);
	}

	/**
	 * Get a resource by the dbId and retrieve
	 * only a bunch of fields from the whole document
	 * @param id
	 * @param retrievedFields
	 * @return
	 */
	public E getById(ObjectId id, List<String> retrievedFields) {
		Query<E> q = this.createQuery().field("_id").equal(id);
		if (retrievedFields != null)
			q.retrievedFields(true, retrievedFields.toArray(new String[retrievedFields.size()]));
		return this.findOne(q);
	}
	
	public boolean existsWithExternalId(String externalId) {
		return existsFieldWithValue("administrative.externalId", externalId);
	}

	/**
	 * Remove an entiry by dbId
	 * @param id
	 * @return
	 */
	public int removeById(ObjectId id) {
		return this.deleteById(id).getN();
	}
	
	public E getUniqueByFieldAndValue(String field, Object value) {
		Query<E> q = this.createQuery().field(field).equal(value);
		return this.findOne(q);
	}
	
	public E getUniqueByFieldAndValue(String field, Object value, List<String> retrievedFields) {
		Query<E> q = this.createQuery().field(field).equal(value);
		q.retrievedFields(true, retrievedFields.toArray(new String[retrievedFields.size()]));
		return this.findOne(q);
	}
	
	public List<E> getByFieldAndValue(String field, Object value, List<String> retrievedFields) {
		Query<E> q = this.createQuery().field(field).equal(value);
		q.retrievedFields(true, retrievedFields.toArray(new String[retrievedFields.size()]));
		return this.find(q).asList();
	}


	public boolean existsFieldWithValue(String field, Object value) {
		Query<E> q = this.createQuery().field(field).equal(value).limit(1);
		return (this.find(q).asList().size()==0? false: true);
	}
	
	public boolean existsFieldsWithValues(List<Tuple<String, Object>> fieldValues) {
		Query<E> q = this.createQuery().limit(1);
		for (Tuple<String, Object> tuple: fieldValues) {
			q.field(tuple.x).equal(tuple.y);
		}
		return (this.find(q).asList().size()==0? false: true);
	}

	
	public boolean existsEntity(ObjectId id) {
		return existsFieldWithValue("_id", id);
	}
	
	public void updateField(ObjectId id, String field, Object value) {
		Query<E> q = this.createQuery().field("_id").equal(id);
		UpdateOperations<E> updateOps = this.createUpdateOperations().disableValidation();
		updateOps.set(field, value);
		this.updateFirst(q, updateOps);
	}
	
	/**
	 * Increment the specified field in a CollectionObject
	 * @param dbId
	 * @param fieldName
	 */
	public void incField( String fieldName, ObjectId dbId) {
		Query<E> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<E> updateOps = this.createUpdateOperations().disableValidation();
		updateOps.inc(fieldName);
		this.updateFirst(q, updateOps);
	}

	/**
	 * Decrement the specified field in a CollectionObject
	 * @param dbId
	 * @param fieldName
	 */
	public void decField(String fieldName, ObjectId dbId) {
		Query<E> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<E> updateOps = this.createUpdateOperations();
		updateOps.dec(fieldName);
		this.updateFirst(q, updateOps);
	}
	
	public void updateFields(String parentField, JsonNode node,
			UpdateOperations<E> updateOps) {
		Iterator<String> fieldNames = node.fieldNames();
		  while (fieldNames.hasNext()) {
	         String fieldName = fieldNames.next();
	         JsonNode fieldValue = node.get(fieldName);
        	 String newFieldName = parentField.isEmpty() ? fieldName : parentField + "." + fieldName;
	         if (fieldValue.isObject()) {
	        	 updateFields(newFieldName, fieldValue, updateOps);
	         }
	         else {//value
				try {
					ObjectMapper mapper = new ObjectMapper();
					Object value = mapper.treeToValue(fieldValue, newFieldName.getClass());
					updateOps.disableValidation().set(newFieldName, value);
				} catch (IOException e) {
					e.printStackTrace();
				}	 
	         }
	     }
	}

}
