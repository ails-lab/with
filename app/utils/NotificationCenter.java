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


package utils;

import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import akka.actor.ActorRef;
import model.Collection;
import model.Notification.Activity;
import model.User;
import model.UserGroup;

/**
 * Mostly static, keeping all actors for websockets together, so they can receive messages
 * @author Arne Stabenau
 *
 */
public class NotificationCenter {

	// Messages are send explicitly to people logged in,
	// Do we want to store them? For people that are not logged in?
	// Primary use for now is testing the websockets.
	public static class Message {
		public Message(String mesg, ObjectId userOrGroup ) {
			message = mesg;
			this.userOrGroup = userOrGroup;			
		}
		public String message;
		@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
		public ObjectId userOrGroup;
	}

	// A Collection update cannot automatically be broadcasted after every addRecord API call,
	// or an import of a dataset would be a disaster. 
	// So if you want a message about this, it has to be done via API call
	public static class CollectionUpdate {
		public  Activity activity; //item_add, item_remove, shared
		public Collection coll;

		public CollectionUpdate( Activity activity, Collection  coll ) {
			this.coll = coll;
			this.activity = activity;
		}
	}

	// User group interaction can be automatical notification. Its few people involved and the risk of
	// massive messages is low.
	public static class UserUpdate {
		public UserUpdate( Activity activity, User user, UserGroup userGroup ) {
			this.user = user;
			this.userGroup = userGroup;
			this.activity = activity; 
		}
		public Activity activity;
		public User user;
		public UserGroup userGroup;
	}
	
	public static Set<ActorRef> notificationActors = new HashSet<ActorRef>();
	
	public static void addActor( ActorRef ref ) {
		notificationActors.add( ref );
	}
	
	public static void removeActor( ActorRef ref ) {
		notificationActors.remove( ref );
	}
	
	// The actor refs wil decide themselves if this is ok for them
	public static void simpleMessage( Message msg ) {
		for( ActorRef ref: notificationActors ) ref.tell( msg, null );
	}
	
	public static void userUpdate( User u, UserGroup userGroup, Activity activity  ) {
		for( ActorRef ref: notificationActors ) ref.tell( new UserUpdate(activity,  u, userGroup ), null );		
	}
	
	public static void collectionUpdate( Activity activity, Collection coll ) {
		for( ActorRef ref: notificationActors ) ref.tell( new CollectionUpdate( activity, coll ), null );				
	}
	
	// some test would be nice, a message
}
