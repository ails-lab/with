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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.bson.types.ObjectId;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.search.fetch.source.FetchSourceContext;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.dao.BasicDAO;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryResults;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.mongodb.util.JSON;

import elastic.Elastic;
import elastic.ElasticEraser;
import elastic.ElasticIndexer;
import elastic.Indexable;
import model.quality.RecordQuality;
import model.resources.WithResourceType;
import play.Logger;
import play.libs.F.Callback;
import play.libs.F.Promise;
import play.libs.Json;
import sources.core.ParallelAPICall;
import sources.utils.JsonContextRecord;
import utils.Tuple;

/**
 * Data Access Object.
 *
 * @param <E>
 */
public class DAO<E> extends BasicDAO<E, ObjectId> {

	public enum QueryOperator {
		GT("$gt"), EQ("$eq"), GTE("$gte");

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
		Query<E> q = this.getDs().createQuery(entityClass);
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
	 * Get a stream of ids for this DAOs object class.
	 * @return
	 */
	public Stream<String> listIds() {
		DBCollection coll = DB.getDs().getCollection(entityClass);
		DBCursor curs = coll.find( toDBObj( "{}"), toDBObj( "{'_id':1}"));
		return StreamSupport
			.stream( curs.spliterator(), false )
			.map(  (DBObject dbobj )-> {
				return ((ObjectId) dbobj.get( "_id" )).toHexString();
			});
	}
	
	/**
	 * Get a Stream of objects that are partly filled, not including all of them into memory.
	 * @param q
	 * @param fieldlist
	 * @return
	 */
	public Stream<E> find( Query<E> q, String... includedFields ) {
		return StreamSupport.stream( super.find(
				q.retrievedFields(true, includedFields)).spliterator(), false );
	}
	
	
	/**
	 * Get a Stream of objects that are partly filled, not including all of them into memory.
	 * @param fieldlist
	 * @return
	 */
	public Stream<E> findAll( String... includedFields ) {
		Query<E> q = createQuery();
		return StreamSupport.stream( super.find( 
				q .retrievedFields(true, includedFields)).spliterator(), false );
	}
	
	public DBObject toDBObj( String json ) {
		return (DBObject) JSON.parse( json );
	}
	
	
	/**
	 * Use this method to save and Object to the database
	 *
	 * @param record
	 */
	public Key<E> makePermanent(E doc) {
		try {
			Key<E> dbKey = this.save(doc, WriteConcern.ACKNOWLEDGED);
			String type = defineInstanceOf(doc);
			if (type != null) {

				/* Index Resource */
				BiFunction<ObjectId, Map<String, Object>, IndexResponse> indexResource = (
						ObjectId colId, Map<String, Object> map) -> {
					return ElasticIndexer.index(type, colId, map);
				};
				ParallelAPICall.createPromise(indexResource, 
						                      (ObjectId) doc.getClass().getMethod("getDbId", new Class<?>[0]).invoke(doc), 
						                      (Map<String, Object>) doc.getClass().getMethod("transform", new Class<?>[0]).invoke(doc));
			}
			return dbKey;
		} catch (Exception e) {
			log.error("Cannot save " + doc.getClass().getSimpleName(), e);
		}
		return null;
	}

	/*
	 * Bulk inserts on MongoDB and Elastic
	 */
	public void storeMany(List<? extends Indexable> docs) {
		if (docs.size() == 0) {
			return;
		}
		
		try {
			this.getDatastore().save(docs,WriteConcern.ACKNOWLEDGED);
			String type = defineInstanceOf(docs.get(0));
			
			if (type != null) {
	
				BiFunction<List<ObjectId>, List<Map<String, Object>>, String> indexResources = (List<ObjectId> colIds, List<Map<String, Object>> maps) -> {
					return ElasticIndexer.indexMany(type, colIds, maps);
				};
	
				ParallelAPICall.createPromise(indexResources, 
						                      docs.stream().map((d) -> { 
						                    	  return (ObjectId)d.getDbId();
						                      }).collect(Collectors.toList()), 
											  docs.stream().map((d) -> { 
												  return (Map<String, Object>)d.transform();
										      }).collect(Collectors.toList()));
	
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Cannot save bulky documents");
		}
	}
	
	
	/**
	 * Use this method to delete and Object to the database
	 *
	 * @param record
	 */
	public int makeTransient(E doc) {
		try {
			String type = defineInstanceOf(doc);
			if (type != null) {

				/* Erase CollectionObject from index */
				Function<ObjectId, Boolean> deleteCollection = (ObjectId colId) -> {
					return ElasticEraser.deleteResource(type, colId.toString());
				};
				ParallelAPICall.createPromise(deleteCollection, (ObjectId) doc
						.getClass().getMethod("getDbId", new Class<?>[0])
						.invoke(doc));

			}
			return this.delete(doc).getN();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return -1;
		}

	}

	/*
	 *
	 */
	@Override
	public UpdateResults update(final Query<E> q, final UpdateOperations<E> ops) {
		E doc = DB.getDs().findAndModify(q, ops, false);
		// ObjectId id = (ObjectId) results.getWriteResult().getUpsertedId();
		// E doc = findOne(q);
		if (doc != null) {
			String type = defineInstanceOf(doc);
//			try {
//				if (type != null) {
//					/* Index Resource */
//					BiFunction<ObjectId, Map<String, Object>, IndexResponse> indexResource = (
//							ObjectId colId, Map<String, Object> map) -> {
//						return ElasticIndexer.index(type, colId, map);
//					};
//					ParallelAPICall.createPromise(indexResource, (ObjectId) doc
//							.getClass().getMethod("getDbId", new Class<?>[0])
//							.invoke(doc), (Map<String, Object>) doc.getClass()
//							.getMethod("transform", new Class<?>[0])
//							.invoke(doc));
//				}
//			} catch (Exception e) {
//				System.out.println(e.getMessage());
//				log.error(e.getMessage(), e);
//				return null;
//			}
		}
		int n = doc == null ? 0 : 1;
		return new UpdateResults(new WriteResult(n, true, null));
	}

	@Override
	public WriteResult deleteById(ObjectId id) {

		WriteResult wr = super.deleteById(id);

		if (wr.getN() == 1) {

			GetResponse resp = Elastic.getTransportClient().get(new GetRequest(Elastic.index, "_all", id.toString())
			.fetchSourceContext(FetchSourceContext.DO_NOT_FETCH_SOURCE))
			.actionGet();

			List<String> enumNames = new ArrayList<String>();
			Arrays.asList(WithResourceType.values()).forEach((t) -> {
				enumNames.add(t.toString());
				return;
			});
			if(enumNames.contains(resp.getType())) {
				Function<String, Boolean> deleteResource = (indexId) -> (ElasticEraser
						.deleteResourceByQuery(indexId));
				Promise<Boolean> deleteResp = ParallelAPICall.createPromise(
						deleteResource, id.toString());
			}
		}

		return wr;
	}

	private String defineInstanceOf(E doc) {

		String instanceName = doc.getClass().getSimpleName();
		List<String> enumNames = new ArrayList<String>();
		Arrays.asList(WithResourceType.values()).forEach((t) -> {
			enumNames.add(t.toString());
			return;
		});
		if (enumNames.contains(instanceName)) {
			if (!instanceName.equalsIgnoreCase(WithResourceType.WithResource
					.toString()))
				return instanceName.toLowerCase();
			else
				return WithResourceType.RecordResource.toString().toLowerCase();
		} else
			return null;

	}
	
	private String defineInstanceOf(Indexable doc) {

		String instanceName = doc.getClass().getSimpleName();
		List<String> enumNames = new ArrayList<String>();
		Arrays.asList(WithResourceType.values()).forEach((t) -> {
			enumNames.add(t.toString());
			return;
		});
		if (enumNames.contains(instanceName)) {
			if (!instanceName.equalsIgnoreCase(WithResourceType.WithResource
					.toString()))
				return instanceName.toLowerCase();
			else
				return WithResourceType.RecordResource.toString().toLowerCase();
		} else
			return null;

	}

	private String defineInstanceOf(Class cz) {

		String instanceName = cz.getSimpleName();
		List<String> enumNames = new ArrayList<String>();
		Arrays.asList(WithResourceType.values()).forEach((t) -> {
			enumNames.add(t.toString());
			return;
		});
		if (enumNames.contains(instanceName)) {
			if (!instanceName.equalsIgnoreCase(WithResourceType.WithResource
					.toString()))
				return instanceName.toLowerCase();
			else
				return WithResourceType.RecordResource.toString().toLowerCase();
		} else
			return null;

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
	 *
	 * @param id
	 * @return
	 */
	public E getById(ObjectId id) {
		Query<E> q = this.createQuery().field("_id").equal(id);
		return this.findOne(q);
	}

	/**
	 * Get a resource by the dbId and retrieve only a bunch of fields from the
	 * whole document
	 *
	 * @param id
	 * @param retrievedFields
	 * @return
	 */
	public E getById(ObjectId id, List<String> retrievedFields) {
		Query<E> q = this.createQuery().field("_id").equal(id).disableValidation();
		if (retrievedFields != null)
			q.retrievedFields(true,
					retrievedFields.toArray(new String[retrievedFields.size()]));
		return this.findOne(q);
	}

	public boolean existsWithExternalId(String externalId) {
		return existsFieldWithValue("administrative.externalId", externalId);
	}

	/**
	 * Get a resource by the dbId and exclude a bunch fields from the whole document
	 *
	 */
	public E getByIdAndExclude(ObjectId id, List<String> excludedFields) {
		Query<E> q = this.createQuery().field("_id").equal(id);
		if (excludedFields != null)
			q.retrievedFields(false,
					excludedFields.toArray(new String[excludedFields.size()]));
		return this.findOne(q);
	}

	/**
	 * Remove an entiry by dbId
	 *
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

	public E getUniqueByFieldAndValue(String field, Object value,
			List<String> retrievedFields) {
		Query<E> q = this.createQuery().field(field).equal(value);
		q.retrievedFields(true,
				retrievedFields.toArray(new String[retrievedFields.size()]));
		return this.findOne(q);
	}

	public List<E> getByFieldAndValue(String field, Object value,
			List<String> retrievedFields) {
		Query<E> q = this.createQuery().field(field).equal(value);
		q.retrievedFields(true,
				retrievedFields.toArray(new String[retrievedFields.size()]));
		return this.find(q).asList();
	}

	public boolean existsFieldWithValue(String field, Object value) {
		Query<E> q = this.createQuery().disableValidation().field(field)
				.equal(value).retrievedFields(true, "_id").limit(1);

		return (this.find(q).asKeyList().size() == 0 ? false : true);
	}

	public boolean existsFieldsWithValues(
			List<Tuple<String, Object>> fieldValues) {
		Query<E> q = this.createQuery().limit(1);
		for (Tuple<String, Object> tuple : fieldValues) {
			q.field(tuple.x).equal(tuple.y);
		}
		return (this.findIds(q).size() == 0 ? false : true);
	}

	public boolean existsEntity(ObjectId id) {
		return existsFieldWithValue("_id", id);
	}




	public void computeAndUpdateQuality(ObjectId id) {
		RecordQuality q = new RecordQuality();
		E obj = getById(id, Arrays.asList("descriptiveData","provenance", "media"));
		double dq= q.compute(new JsonContextRecord(Json.toJson(obj)));
		log.debug("Quality "+dq);
		updateField(id, "qualityMeasure", dq);
	}
	

	public void updateField(ObjectId id, String field, Object value) {
		Query<E> q = this.createQuery().field("_id").equal(id);
		UpdateOperations<E> updateOps = this.createUpdateOperations()
				.disableValidation();
		updateOps.set(field, value);
		this.updateFirst(q, updateOps);
	}

	public void deleteField(ObjectId id, String field) {
		Query<E> q = this.createQuery().field("_id").equal(id);
		UpdateOperations<E> updateOps = this.createUpdateOperations()
				.disableValidation();
		updateOps.unset(field);
		this.updateFirst(q, updateOps);
	}

	/**
	 * Increment the specified field in a CollectionObject
	 *
	 * @param dbId
	 * @param fieldName
	 */
	public void incField(String fieldName, ObjectId dbId) {
		Query<E> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<E> updateOps = this.createUpdateOperations()
				.disableValidation();
		updateOps.inc(fieldName);
		this.updateFirst(q, updateOps);
	}

	/**
	 * Decrement the specified field in a CollectionObject
	 *
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
			String newFieldName = parentField.isEmpty() ? fieldName
					: parentField + "." + fieldName;
			if (fieldValue.isNull())
				continue;
			if (fieldName.equals("dates"))
				continue;
			if (fieldValue.isObject()) {
				updateFields(newFieldName, fieldValue, updateOps);
			} else {
				if (fieldValue.isArray()) {
					String[] values = new String[fieldValue.size()];
					for (int i = 0; i < fieldValue.size(); i++) {
						if (fieldValue.get(i).isObject()) {
							updateFields(newFieldName+"."+i, fieldValue.get(i), updateOps);
						}
						else
							values[i] = fieldValue.get(i).asText();
					}
					if ((values.length > 0) && (values[0] != null)) {
						updateOps.disableValidation().set(newFieldName, values);
					}
				} else {
					if(fieldValue.isBoolean())
						updateOps.disableValidation().set(newFieldName,
							fieldValue.asBoolean());
					else
						updateOps.disableValidation().set(newFieldName,
								fieldValue.asText());
				}
			}
		}
	}

}
