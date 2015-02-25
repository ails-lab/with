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

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.print.attribute.standard.Media;

import model.Collection;
import model.User;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import play.Logger;

import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.gridfs.GridFS;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

// get the DAOs from here
// the EntityManagerFactory is here
public class DB {
	private static Map<String, DAO<?>> daos = new HashMap<String, DAO<?>>();
	private static MongoClient mongo;
	private static Morphia morphia;
	private static Datastore ds;
	private static GridFS gridfs;
	private static String dbName = "with-db";

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
			ds = morphia.createDatastore(mongo, dbName);
			morphia.mapPackage("model");
			gridfs = new GridFS(mongo.getDB(dbName));
		} catch(UnknownHostException | MongoException e) {
			log.error("Database Connection aborted!", e);
		}
	}

	public static GridFS getGridFs() {
		if(gridfs != null)
			return gridfs;
		else {
			try {
				gridfs = new GridFS(mongo.getDB(dbName));
			} catch(Exception e) {
				log.error("Cannot create GridFS!", e);
			}
			return gridfs;
		}

	}

	public static Datastore getDs() {
		if(ds != null)
			return ds;
		else {
			try {
				ds = morphia.createDatastore(mongo, dbName);
			} catch(Exception e) {
				log.error("Cannot create Datastore!", e);
			}
			return ds;
		}
	}

	public static UserDAO getUserDAO() {
		return (UserDAO) getDAO(User.class);
	}

	public static CollectionDAO getCollectionDAO() {
		return (CollectionDAO) getDAO(Collection.class);
	}

	public static MediaDAO getMediaDAO() {
		return (MediaDAO) getDAO(Media.class);
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
