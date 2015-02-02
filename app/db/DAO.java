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

import javax.persistence.EntityManager;
import javax.persistence.Query;

import play.Logger;
import play.libs.F.Callback;

public class DAO<E> {
	static private final Logger.ALogger log = Logger.of(DAO.class); 

	private Class<?> entityClass;
	public DAO( Class<?> entityClass ) {
		this.entityClass = entityClass;
	}
	
	/**
	 * Primitive getter for objects. 
	 * @param condition
	 * @return
	 */
	public E find( String condition ) {
		E res =(E) DB.getEm().find( entityClass, condition );
		return res;
	}
	
	/**
	 * use attributes on the condition with the obj. prefix
	 * @param condition
	 * @return
	 */
	public List<E> list( String condition) {
		Query q = DB.getEm().createQuery( "select obj from " + entityClass.getSimpleName() + " obj " +
				(condition != null ? " where " + condition : "" ));
		List<E> res = (List<E>) q.getResultList();
		return res;
		
	}

	public void persist( E obj) {
		DB.getEm().persist(obj);
	}
	
	
	public void remove( E obj ) {
		DB.getEm().remove(obj);
	}
	
	/**
	 * Execute on all matching entities and optionally write changes back to db.
	 * Use the "obj." prefix on parameters of the query.
	 * @param callback
	 * @param withWriteback
	 * @throws Exception
	 */
	public void onAll( String condition, Callback<E> callback, boolean withWriteback ) throws Exception {
		EntityManager em = DB.newEm();
    	Query q = em.createQuery("select obj from " + entityClass.getSimpleName() + "obj " +
    			(( condition !=  null ) ? " where " + condition : "" ));
    	com.impetus.kundera.query.Query kq = (com.impetus.kundera.query.Query) q;
    	Iterator<E> i = kq.iterate();
    	while( i.hasNext()) {
    		try {
    			E obj = i.next();
    			callback.invoke(obj);
    			if( withWriteback ) {
    				em.flush();
    			}
    			em.clear();
    		} catch( Throwable thr) {
    			log.error( "Iterate over " + entityClass.getSimpleName() + " with condition " +
    					condition, thr  );
    		} finally {
    			em.close();
    		}
    	}
	}	
	
	/**
	 * Condition on the object needs to include the prefix 'obj.'!
	 * There is a separate entity manager doing the work here.
	 * @param condition
	 * @return Number of entries deleted
	 */
	public int removeAll( String condition ) {
		EntityManager em = DB.newEm();
		int res = -1;
		Query q = em.createQuery("delete obj from " + entityClass.getSimpleName() + " obj " +
    			(( condition !=  null ) ? " where " + condition : "" ));
		try {
			res = q.executeUpdate();
		} catch( Exception e ) {
			log.error( "Batch delete on " + entityClass.getSimpleName() + "'" + condition +"' failed.", e);
		} finally {
			em.close();
		}
		return res;
	}
}
