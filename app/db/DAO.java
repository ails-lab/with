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

import java.util.Iterator;
import java.util.List;

import org.mongodb.morphia.dao.BasicDAO;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryResults;

import play.Logger;
import play.libs.F.Callback;

import com.mongodb.MongoException;
import com.mongodb.WriteConcern;

public class DAO<E> extends BasicDAO<E, String> {
	static private final Logger.ALogger log = Logger.of(DAO.class);

	private final Class<?> entityClass;
	public DAO( Class<?> entityClass )  {
		super(DB.getDs());
		this.entityClass = entityClass;
	}


	/**
	 * use attributes on the condition with the obj. prefix
	 * @param condition
	 * @return
	 */
	public List<E> list( String property, String value)  {
		List<E> res = (List<E>) this.getDs().find(entityClass, property, value).asList();
		return res;

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
	 * Return index info for a collection
	 */
	public String getIndexindexInfo() {
		return null;
	}

	/**
	 * Execute on all matching entities and optionally write changes back to db.
	 * Use the "obj." prefix on parameters of the query.
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
	public void makePermanent(Object obj) {
		try {
			this.save(getEntityClass().cast(obj));
		} catch(Exception e) {
			log.error("Cannot save " + getEntityClass().getSimpleName(), e);
		}
	}
	/**
	 * Use this method to delete and Object to the database
	 * @param record
	 */
	public void makeTransient(Object obj) {
		try {
			this.delete(getEntityClass().cast(obj));
		} catch (Exception e) {
			log.error("Cannot delete " + getEntityClass().getSimpleName(), e);
		}
	}

	/**
	 * Condition on the object needs to include the prefix 'obj.'!
	 * There is a separate entity manager doing the work here.
	 * @param condition
	 * @return Number of entries deleted
	 */
	public boolean removeAll( String condition ) {
		Query<E> q = this.createQuery();
		q.criteria(condition);
		boolean res = this.deleteByQuery(q).getLastError().ok();

		return res;
	}
}
