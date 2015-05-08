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


import java.util.List;

import com.mongodb.WriteConcern;

import model.ApiKey;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.api.mvc.EssentialFilter;
import play.libs.Akka;
import actors.ApiKeyManager;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import controllers.AccessFilter;
import controllers.SessionFilter;
import db.DB;


public class Global extends GlobalSettings {
	static private final Logger.ALogger log = Logger.of(Global.class);
	
	@Override
	public void onStart( Application app ) {
		Akka.system().actorOf( Props.create( ApiKeyManager.class ), "apiKeyManager");

		setTestApikey();
		// read keys into the Manager 
		ActorSelection api = Akka.system().actorSelection("user/apiKeyManager"); 
		api.tell( new ApiKeyManager.Reset(), ActorRef.noSender());
	}

	@Override
	public <T extends EssentialFilter> Class<T>[] filters() {
	    return new Class[] {AccessFilter.class, SessionFilter.class };
	}
	
	private void setTestApikey() {
		if( DB.getConf().hasPath( "apikey.testAccessPattern")) {
			String pattern = DB.getConf().getString( "apikey.testAccessPattern");
			List<ApiKey> lk = DB.getApiKeyDAO().getByIpPattern(pattern);
			if( lk.isEmpty() ) { 
				ApiKey k = new ApiKey();
				// should cover localhost
				k.setIpPattern(pattern);
				k.addCall(0, ".*");	
				// store it
				DB.getApiKeyDAO().save(k, WriteConcern.SAFE);
			}
		}
	}
}
