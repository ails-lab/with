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

import akka.actor.ActorRef;
import model.Notification;

/**
 * Mostly static, keeping all actors for websockets together, so they can
 * receive messages
 * 
 * @author Arne Stabenau
 *
 */
public class NotificationCenter {

	// Messages are send explicitly to people logged in,
	// Do we want to store them? For people that are not logged in?
	// Primary use for now is testing the websockets.

	public static Set<ActorRef> notificationActors = new HashSet<ActorRef>();

	public static void addActor(ActorRef ref) {
		notificationActors.add(ref);
	}

	public static void removeActor(ActorRef ref) {
		notificationActors.remove(ref);
	}

	// The actor refs wil decide themselves if this is ok for them

	public static void sendNotification(Notification notification) {
		for (ActorRef ref : notificationActors) {
			ref.tell(notification, null);
		}
	}
}
