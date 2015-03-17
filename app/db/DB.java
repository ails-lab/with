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

import model.Collection;
import model.CollectionEntry;
import model.Record;
import model.RecordLink;
import model.Search;
import model.SearchResult;
import model.User;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import play.Logger;

import com.mongodb.MongoClient;
import com.mongodb.gridfs.GridFS;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

// get the DAOs from here
// the EntityManagerFactory is here
public class DB {
	private static Map<String, DAO<?>> daos = new HashMap<String, DAO<?>>();
	private static MediaDAO mediaDAO;
	private static MongoClient mongo;
	private static Datastore ds;
	private static Morphia morphia;
	private static GridFS gridfs;
	private static Config conf;

	static private final Logger.ALogger log = Logger.of(DB.class);

	public static GridFS getGridFs() {
		if(gridfs == null) {
			try {
				String dbname = getConf().getString( "mongo.dbname");
				gridfs = new GridFS(getMongo().getDB(dbname));
			} catch(Exception e) {
				log.error("Cannot create GridFS!", e);
			}
		}
		return gridfs;
	}

	public static Config getConf() {
		if( conf == null ) {
			conf = ConfigFactory.load();
		}
		return conf;
	}

	public static MongoClient getMongo() {
		if( mongo == null ) {
			try {
				String host = getConf().getString("mongo.host");
				int port = getConf().getInt("mongo.port");
				mongo = new MongoClient(host, port);
				if( getConf().hasPath("mongo.erase") && getConf().getBoolean("mongo.erase")) {
					mongo.dropDatabase(getConf().getString( "mongo.dbname"));
				}
			} catch( Exception e ) {
				log.error( "Cannot create Mongo client", e );
			}
		}
		return mongo;
	}

	public static Morphia getMorphia() {
		if( morphia == null ) {
			 morphia = new Morphia();
			//this method is not working, have to find why!!
			//morphia.mapPackage("model");
		}
		return morphia;
	}

	public static Datastore getDs() {
		if(ds == null) {
			try {
				ds = getMorphia().createDatastore(getMongo(), getConf().getString("mongo.dbname"));
				ds.ensureIndexes();

			} catch(Exception e) {
				log.error("Cannot create Datastore!", e);
			}
		}
		return ds;
	}

	public static String getJson( Object o ) {
		return getMorphia().getMapper().toDBObject(o).toString();
	}

	public static UserDAO getUserDAO() {
		return (UserDAO) getDAO(User.class);
	}

	public static CollectionDAO getCollectionDAO() {
		return (CollectionDAO) getDAO(Collection.class);
	}

	public static MediaDAO getMediaDAO() {

		if( mediaDAO == null )
			mediaDAO = new MediaDAO();
		return mediaDAO;
	}

	public static RecordDAO getRecordDAO() {
		return (RecordDAO) getDAO(Record.class);
	}

	public static RecordLinkDAO getRecordLinkDAO() {
		return (RecordLinkDAO) getDAO(RecordLink.class);
	}

	public static SearchDAO getSearchDAO() {
		return (SearchDAO) getDAO(Search.class);
	}

	public static SearchResultDAO getSearchResultDAO() {
		return (SearchResultDAO) getDAO(SearchResult.class);
	}

	public static CollectionEntryDAO getCollectionEntryDAO() {
		return (CollectionEntryDAO) getDAO(CollectionEntry.class);
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
