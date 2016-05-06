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


package actors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import model.ApiKey;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import akka.actor.ActorPath;
import akka.actor.UntypedActor;
import db.DB;
/**
 * Keeps the API keys in memory, writes them to db occasionally and answers to 
 * request. There is to be one ApiKeyManager actor in the system!
 * @author stabenau
 *
 */
public class ApiKeyManager extends UntypedActor  {
	public static final ALogger log = Logger.of( ApiKeyManager.class);
	public static final ALogger allAccessLog = Logger.of( "AllAccess" );
	
	Hashtable<String, ApiKey> apiKeys = new Hashtable<String, ApiKey>();
	
	public static class Access implements Serializable {
		public String call;
		public String apikey;
		public long volume;
		public boolean update = false;
		
		public String toString( ) {			
			return ("Call: " + call 
					+ " apikey: " + apikey + " volume: " + volume 
					+ " update: " + (update?"true":"false"));
		}
	}
	
	public static class Create implements Serializable {
		public String dbId;
			
		// the following two are optional
		// an email doesn't need to be a registered user's email
		
		// ObjectId contains the associated user's id
		public ObjectId proxyUserId;
		
		// email contains the associated email
		public String email;
		
		// if call is empty, it will create a new apikey
		// either with ip or if that is empty with a new secret
		
		public String call;
		
		// set this to -1l if you dont want limits
		public long volumeLimit;
		public long counterLimit;
		public int position;
	}
	
	/**
	 * Send this to store current state
	 * @author stabenau
	 *
	 */
	public static class Store implements Serializable {
		// just a marker
	}
	
	/**
	 * Send this to reread the state from database
	 * @author stabenau
	 *
	 */
	public static class Reset implements Serializable {
		// just a marker
	}
	
	
	public ApiKeyManager() {
		super();
		readKeysFromDb();
	}
	
	/**
	 * Read everything from DB and overwrite whats in here.
	 */
	private void readKeysFromDb() {
		apiKeys.clear();
		log.info("Reading " + DB.getApiKeyDAO().count() + " API-keys");
		for( ApiKey k: DB.getApiKeyDAO().find()) {
			if( !StringUtils.isEmpty(k.getKeyString())) {
				apiKeys.put( k.getKeyString(), k);
				log.info(k.getKeyString());
			} 
		}
	}
	
	private void writeKeysToDb() {
		for( ApiKey k: apiKeys.values()) {
			// safe or overwrite
			DB.getApiKeyDAO().save(k);
		}
	}
	
	
	private ApiKey getByDbId( String id ) {
		for( ApiKey k: apiKeys.values() ) {
			if( k.getDbId().toString().equals(id))
				return k;
		}
		
		return null;
	}
	
	private void onApiAccess( Access access ) {

		// conf disabled api keys
		if( DB.getConf().getBoolean("apikey.disabled") ) {
			sender().tell( ApiKey.Response.ALLOWED, self());
			return;
		}
		
		allAccessLog.info( access.toString() );
		ApiKey key = null;
		
			key = apiKeys.get( access.apikey);
			if( key == null ) {
				if( !access.update )
					sender().tell( ApiKey.Response.INVALID_APIKEY, self());
				return;
			} else if (key.getKeyString() == "empty"){
				sender().tell( ApiKey.Response.EMPTY_APIKEY, self());
			}
		
		if( !access.update ) {
			ApiKey.Response res = key.check( access.call, access.volume );
			if(( res == ApiKey.Response.ALLOWED) &&
					key.getProxyUserId() != null ) {
				sender().tell( key.getProxyUserId(), self());
			} else 
				sender().tell( res, self());

		} else
			key.updateVolume(access.call, access.volume);
		
	}
	
	private void answer( Object msg ) {
		sender().tell( msg, self());		
	}
	
	private void onApiCreate( Create create ) {
		// create apikey and return the secret
		
		// create calls on apikey structures
		if( StringUtils.isEmpty(create.dbId )) {
			ApiKey newKey = new ApiKey();
			
			if(!StringUtils.isEmpty(create.email)){
				newKey.setEmail(create.email);
			}
			
			if(create.proxyUserId!=null){
				newKey.setProxyUserId(create.proxyUserId);
			}

			newKey.resetKey();
			apiKeys.put( newKey.getKeyString(), newKey);

			writeKeysToDb();
			
			answer( newKey.getKeyString());
		} else {			
			// find by dbId
			ApiKey key =getByDbId(create.dbId);
			
			key.addCall(0, create.call, create.counterLimit, create.volumeLimit);
		}
		
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		// TODO Auto-generated method stub
		if( msg instanceof Access ) {
			Access access = (Access) msg;
			onApiAccess( access );
		} else if( msg instanceof Create ) {
			Create ac = (Create) msg;
			onApiCreate( ac );
		} else if( msg instanceof Store ) {
			writeKeysToDb();
			// wait for the answer to be sure the store has happened
			answer( new Store());
		} else if( msg instanceof Reset ) {
			readKeysFromDb();
			// wait for the answer to be sure the read has happened
			answer( new Reset());
		}
	}
}
