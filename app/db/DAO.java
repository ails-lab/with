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

import java.util.Collection;
import java.util.Iterator;

import org.bson.types.ObjectId;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.dao.BasicDAO;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryResults;

import play.Logger;
import play.libs.F.Callback;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.mongodb.util.JSON;


public class DAO<E> extends BasicDAO<E, ObjectId> {
	static private final Logger.ALogger log = Logger.of(DAO.class);

	private final Class<?> entityClass;
	public DAO( Class<?> entityClass )  {
		super(DB.getDs());
		this.entityClass = entityClass;
	}

	public static DBObject asDBObject( String json ) {
		DBObject res = (DBObject) JSON.parse(json);
		return res;
	}

	/**
	 * Convenience method for retrieving all values for this query on certain field.
	 * Use if you don't want the morphia treatment (you want values, not objects)
	 * @param res
	 * @param query
	 * @param field
	 */
	public void withCollection( Collection<String> res, String query, String field ) {
		DBCursor cursor = null;
		try {
			BasicDBObject fieldProjector = new BasicDBObject();
			fieldProjector.put( field, 1 );
			cursor = getCollection().find( asDBObject(query), fieldProjector);
			while( cursor.hasNext()) res.add( cursor.next().get(field).toString());
		} finally {
			cursor.close();
		}
	}

	/**
	 * Return collection stats
	 */
	public String getCollectionStatistics() {
		String stats = this.getCollection().getStats().toString();
		if(stats != null)
			return stats;
		else {
			log.debug("No statistics returned for the " + entityClass.getSimpleName() + "collection");
			return null;
		}

	}


	/**
	 * Execute on all matching entities and optionally write changes back to db.
	 * @param callback
	 * @param withWriteback
	 * @throws Exception
	 */
	public void onAll( String condition, Callback<E> callback, boolean withWriteback ) throws Exception {
    	Query<E> q = (Query<E>) this.getDs().createQuery(entityClass);
    	q.criteria(condition);
    	QueryResults<E> qr = this.find(q);
    	Iterator<E> i = qr.iterator();
    	while( i.hasNext()) {
    		try {
    			E obj = i.next();
    			callback.invoke(obj);
    			if( withWriteback ) {
    				save(obj, WriteConcern.JOURNALED);
    			}
    		} catch( Throwable thr) {
    			log.error( "Iterate over " + entityClass.getSimpleName() + " with condition " +
    					condition, thr  );
    		}
    	}
	}

	/**
	 * Drop a collection and all it's documents
	 */
	public void dropCollection() {
		try {
			DB.getDs().getCollection(entityClass).drop();
		} catch(MongoException me) {
			log.error("Cannot drop collection " + entityClass.getSimpleName(), me);
		}
	}

	/**
	 * Drop an index from a collection
	 */
	public void dropIndexFromCollection(String index) {
		try {
			DB.getDs().getCollection(entityClass).dropIndex(index);
		} catch(MongoException me) {
			log.error("Cannot drop index from collection " + entityClass.getSimpleName(), me);
		}
	}

	/**
	 * Drop all indexes from collection
	 */
	public void dropAllIndexesFromCollection() {
		try {
			DB.getDs().getCollection(entityClass).dropIndexes();
		} catch(MongoException me) {
			log.error("Cannot drop indexes from collection", me);
		}
	}

	/**
	 * Use this method to save and Object to the database
	 * @param record
	 */
	public Key<E> makePermanent(E doc) {
		try {
			return this.save(doc);
		} catch(Exception e) {
			log.error("Cannot save " + doc.getClass().getSimpleName(), e);
		}
		return null;
	}
	/**
	 * Use this method to delete and Object to the database
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
	 * Condition on the object needs to include the prefix 'obj.'!
	 * There is a separate entity manager doing the work here.
	 * @param condition
	 * @return Number of entries deleted
	 */
	public int removeAll( String condition ) {
		Query<E> q = this.createQuery();
		q.criteria(condition);
		int n = this
				.deleteByQuery(q)
				.getN();

		return n;
	}
}
