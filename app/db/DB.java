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

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import model.User;
import play.Logger;

// get the DAOs from here
// the EntityManagerFactory is here
public class DB {
   	static EntityManagerFactory emf = Persistence.createEntityManagerFactory("with_persist");
	static ThreadLocal<EntityManager> managers = new ThreadLocal<EntityManager>();
	static Map<String, DAO<?>> daos = new HashMap<String, DAO<?>>();
	
	static private final Logger.ALogger log = Logger.of(DB.class); 
	/**
	 * A Thread specific EntityManager
	 * System will use those during requests.
	 * @return
	 */
	public static EntityManager getEm() {
		EntityManager em = managers.get();
		if( em == null ) {
			em = emf.createEntityManager();
			managers.set( em );
		}
		return em;
	}
	
	
	public static void flush() {
		getEm().flush();
	}
	
	/**
	 * A new EntityManager. Do whatever you will and clean up afterwards 
	 * @return
	 */
	public static EntityManager newEm() {
		return emf.createEntityManager();
	}
	
	public static UserDAO getUserDAO() {
		return (UserDAO) getDAO(User.class);
	}
	
	/**
	 * Singleton DAO class for all the models
	 * @param clazz
	 * @return
	 */
	private static DAO<?> getDAO( Class<?> clazz ) {
		DAO<?> dao = daos.get( clazz.getSimpleName());
		if( dao == null ) {
			try {
				String daoClassName = "db."+clazz.getSimpleName()+"DAO";
				Class<?> daoClass = Class.forName(daoClassName );
				dao = (DAO<?>) daoClass.newInstance();
				daos.put( clazz.getSimpleName(), dao);
			} catch( Exception e ) {
				log.error( "Can't instantiate DAO for "+ clazz.getName(), e );
			}
		}
		return dao;
	}
	
	public static void close() {
		EntityManager em = managers.get();
		if( em != null ) em.close();
		managers.remove();
	}
}
