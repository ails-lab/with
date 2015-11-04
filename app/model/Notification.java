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


package model;

import java.sql.Timestamp;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import utils.Serializer;

public class Notification {

	public enum Activity {
		// group related
		GROUP_INVITE, GROUP_INVITE_ACCEPT, GROUP_INVITE_DECLINED, 
		GROUP_REQUEST, GROUP_REQUEST_ACCEPT, GROUP_REQUEST_DENIED,

		// collection related
		COLLECTION_ITEM_ADDED, COLLECTION_ITEM_REMOVED, COLLECTION_SHARED,

		// messages
		MESSAGE
	}

	@Id
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId dbId;
	private Activity activity;

	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId receiver;

	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId sender;

	// The collection related with the action (if collection related)
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId collection;

	// The group that is involved with the action (if group related)
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId group;
	private String message;
	// While the notification is pending for an answer, it remains open 
	private boolean open;
	private Timestamp sentAt;
	private Timestamp readAt;

	public ObjectId getReceiver() {
		return receiver;
	}

	public void setReceiver(ObjectId receiver) {
		this.receiver = receiver;
	}

	public ObjectId getSender() {
		return sender;
	}

	public void setSender(ObjectId sender) {
		this.sender = sender;
	}

	public ObjectId getCollection() {
		return collection;
	}

	public void setCollection(ObjectId collection) {
		this.collection = collection;
	}

	public ObjectId getGroup() {
		return group;
	}

	public void setGroup(ObjectId group) {
		this.group = group;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public boolean isOpen() {
		return open;
	}

	public void setOpen(boolean open) {
		this.open = open;
	}

	public Timestamp getSentAt() {
		return sentAt;
	}

	public void setSentAt(Timestamp sentAt) {
		this.sentAt = sentAt;
	}

	public Timestamp getReadAt() {
		return readAt;
	}

	public void setReadAt(Timestamp readAt) {
		this.readAt = readAt;
	}

	public Activity getActivity() {
		return activity;
	}

	public void setActivity(Activity activity) {
		this.activity = activity;
	}
}