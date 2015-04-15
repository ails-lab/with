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

import play.Logger;
import play.Logger.ALogger;
import controllers.CollectionController;
import akka.actor.UntypedActor;
/**
 * Keeps the API keys in memory, writes them to db occasionally and answers to 
 * request. There is to be one ApiKeyManager actor in the system!
 * @author stabenau
 *
 */
public class ApiKeyManager extends UntypedActor  {
	public static final ALogger log = Logger.of( ApiKeyManager.class);

	Hashtable<String, ApiKey> apiKeys = new Hashtable<String, ApiKey>();
	List<ApiKey> ipPatterns = new ArrayList<ApiKey>();
	
	public static class ApiAccess implements Serializable {
		public String call;
		public String ip;
		public String apikey;
		public long volume;
		public boolean update = false;
		
		public String toString( ) {
			return ("Call: " + call + " ip: " + ip 
					+ " apikey: " + apikey + " volume: " + volume 
					+ "update: " + (update?"true":"false")); 
		}
	}
	
	public static class ApiCreate implements Serializable {
		public String dbId;
		
		public String ip;
		
		// if call is empty, it will create a new apikey
		// either with ip or if that is empty with a new secret
		
		public String call;
		
		// set this to -1l if you dont want limits
		public long volumeLimit;
		public long counterLimit;
		public int position;
	}
	
	
	private void readKeysFromDb() {
		// init from db
	}
	
	private void writeKeysToDb() {
		// sync to db
	}
	
	private ApiKey getByIp( String ip ) {
		for( ApiKey k: ipPatterns ) {
			if( ip.matches( k.getIpPattern()))
				return k;
		}
		return null;
	}
	
	private ApiKey getByDbId( String id ) {
		for( ApiKey k: ipPatterns ) {
			if( k.getDbId().toString().equals(id))
				return k;
		}
		for( ApiKey k: apiKeys.values() ) {
			if( k.getDbId().toString().equals(id))
				return k;
		}
		
		return null;
	}
	
	private void onApiAccess( ApiAccess access ) {
		log.info( access.toString() );
		ApiKey key = null;
		if( !StringUtils.isEmpty(access.ip)) { 
			key = getByIp(access.ip);
			if( key == null ) { 
				if( !access.update )
					sender().tell( ApiKey.AccessResponse.INVALID_IP, self());
				return;
			}
		} else {
			key = apiKeys.get( access.apikey);
			if( key == null ) {
				if( !access.update )
					sender().tell( ApiKey.AccessResponse.INVALID_APIKEY, self());
				return;
			}
		}
		
		if( !access.update ) 
			sender().tell( key.check( access.call, access.volume ), self());
		else
			key.updateVolume(access.call, access.volume);
		
	}
	
	private void answer( Object msg ) {
		sender().tell( msg, self());		
	}
	
	private void onApiCreate( ApiCreate create ) {
		// create apikey and return the secret
		// create apikey for ip-pattern
		
		// create calls on apikey structures
		if( StringUtils.isEmpty(create.dbId )) {
			ApiKey newKey = new ApiKey();
			if( StringUtils.isEmpty(create.ip)) {
				newKey.resetKey();
				apiKeys.put( newKey.getKeyString(), newKey);
			} else {
				newKey.setIpPattern(create.ip);
				ipPatterns.add( newKey );
			}
			answer( newKey.getDbId().toString());
		} else {			
			// find by dbId
			ApiKey key =getByDbId(create.dbId);
			
			key.addCall(0, create.call, create.counterLimit, create.volumeLimit);
		}
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		// TODO Auto-generated method stub
		if( msg instanceof ApiAccess ) {
			ApiAccess access = (ApiAccess) msg;
			onApiAccess( access );
		} else if( msg instanceof ApiCreate ) {
			ApiCreate ac = (ApiCreate) msg;
			onApiCreate( ac );
		}
	}
}
