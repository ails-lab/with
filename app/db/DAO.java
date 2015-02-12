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
    				this.delete(obj);
    			}
    		} catch( Throwable thr) {
    			log.error( "Iterate over " + entityClass.getSimpleName() + " with condition " +
    					condition, thr  );
    		}
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
