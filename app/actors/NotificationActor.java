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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import model.User;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import utils.NotificationCenter;

/**
 * 
 * @author Arne Stabenau
 * The actor that talks with the web app.
 */
public class NotificationActor extends UntypedActor  {
	public static final ALogger log = Logger.of( NotificationActor.class);

	private User loggedInUser;
	private final ActorRef out;
	
	public static Props props(ActorRef out) {
		return Props.create(NotificationActor.class, out);
	}

	public NotificationActor(ActorRef out) {
		this.out = out;
	}

	public void onReceive(Object message) throws Exception {
		if (message instanceof JsonNode ) {
			// only login / logout messages are expected
			log.debug( "Received Message from browser: " + message.toString());
		}
		if( message instanceof NotificationCenter.Message ) {
			notifyMessage( (NotificationCenter.Message) message );
		}
		if( message instanceof NotificationCenter.UserUpdate) {
			
		}
	}
	
	/**
	 * Simple message notification, to be printed on the browser if the user matches the given objectId.
	 * @param mesg
	 * @param userOrGroup
	 */
	public void notifyMessage( NotificationCenter.Message mesg ) {
		if( mesg.userOrGroup == null ) {
			// public messsage
			out.tell( Json.toJson(mesg) , self());
		} else {
			if( loggedInUser != null ) {
				if( loggedInUser.getUserGroupsIds().contains(mesg.userOrGroup)) {
					out.tell( Json.toJson( mesg ), self());					
				}
			}
		}
	}
	
	public void userUpdate( NotificationCenter.UserUpdate userUp) {
		
	}
}
