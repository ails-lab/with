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

import model.User;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import play.Logger;
import play.Play;

import com.mongodb.MongoClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

// get the DAOs from here
// the EntityManagerFactory is here
public class DB {
	private static Map<String, DAO<?>> daos = new HashMap<String, DAO<?>>();
	private static MongoClient mongo;
	private static Morphia morphia;
	private static Datastore ds;

	static private final Logger.ALogger log = Logger.of(DB.class);

	/* Init session method.
	 * I can obtain host, port, dbName from the constructor
	 */
	public static void initialize() {
		// ConfigFactory.invalidateCaches();
		Config conf = ConfigFactory.load();
		
		String host = conf.getString("mongo.host");
		int port = conf.getInt("mongo.port");

		try {
			mongo = new MongoClient(host, port);
			morphia = new Morphia();
		} catch(Exception e) {
			log.error("Database Conection aborted!", e);
		}
	}

	public static Datastore getDs() {
		String dbName = "with-db";
		try {
			ds = morphia.createDatastore(mongo, dbName);
			morphia.mapPackage("model");
		} catch(Exception e) {
			log.error("Database Conection aborted!", e);
		}
			return ds;
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
}
