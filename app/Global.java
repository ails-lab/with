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


import com.mongodb.WriteConcern;

import actors.ApiKeyManager;
import actors.LockActor;
import actors.MediaCheckerActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import controllers.AccessFilter;
import controllers.EffektiveUserFilter;
import controllers.SessionFilter;
import db.DB;
import elastic.Elastic;
import model.ApiKey;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.api.mvc.EssentialFilter;
import play.libs.Akka;
import utils.Locks;


public class Global extends GlobalSettings {
	static private final Logger.ALogger log = Logger.of(Global.class);

	@Override
	public void onStart( Application app ) {
		
		// this needs to change for multi webhosts app
		// some global 
		ActorRef apiKeyManager = Akka.system().actorOf( Props.create( ApiKeyManager.class ), "apiKeyManager");
		ActorRef lockManager = Akka.system().actorOf( Props.create( LockActor.class), "lockManager");
		ActorRef mediaChecker = Akka.system().actorOf( Props.create( MediaCheckerActor.class), "mediaChecker");

		
		Locks.setLockManagerActorRef( lockManager );
		
		if(DB.getConf().getBoolean("elasticsearch.enable"))
			Elastic.initializeIndex();
		setupWithKey();

		// read keys into the Manager
		ActorSelection api = Akka.system().actorSelection("user/apiKeyManager");
		api.tell( new ApiKeyManager.Reset(), ActorRef.noSender());
	}

	//@Override
	public <T extends EssentialFilter> Class<T>[] filters() {
	    return new Class[] {AccessFilter.class, SessionFilter.class, EffektiveUserFilter.class };
	}

	private void setupWithKey() {
		ApiKey withKey = DB.getApiKeyDAO().getByName("WITH");
		if( withKey == null ) {
			ApiKey k = new ApiKey();
			k.setName("WITH");
			k.addCall(0, ".*" );
			k.resetKey();
			k.setOrigin(DB.getConf().getString("with.origin"));
			// store it
			DB.getApiKeyDAO().save(k, WriteConcern.SAFE);
		}

		ApiKey mintKey = DB.getApiKeyDAO().getByName( "Mint" );
		if( mintKey == null ) {
			ApiKey k = new ApiKey();
			k.setName("Mint");
			k.addCall(0, ".*" );

			// guinness ... so that mint can contact this server
//			k.setIpPattern("147\\.102\\.11\\.71");
			// store it
			DB.getApiKeyDAO().save(k, WriteConcern.SAFE);
		}
	}
	
}
