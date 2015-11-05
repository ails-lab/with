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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import db.DB;
import utils.Serializer;

public class Notification {

	public enum Activity {
		// group related
		GROUP_INVITE, GROUP_INVITE_ACCEPT, GROUP_INVITE_DECLINED, GROUP_REQUEST, GROUP_REQUEST_ACCEPT, GROUP_REQUEST_DENIED,

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
	private Timestamp openedAt;
	private Timestamp closedAt;

	@JsonIgnore
	public ObjectId getDbId() {
		return dbId;
	}

	public void setDbId(ObjectId dbId) {
		this.dbId = dbId;
	}

	public ObjectId getReceiver() {
		return receiver;
	}

	public void setReceiver(ObjectId receiver) {
		this.receiver = receiver;
	}

	public ObjectId getSender() {
		return sender;
	}

	public String getSenderName() {
		if (this.sender == null) {
			return null;
		}
		User user = DB.getUserDAO().get(this.sender);
		return user.getFirstName() + " " + user.getLastName();
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

	public String getCollectionName() {
		if (this.collection == null) {
			return null;
		}
		return DB.getCollectionDAO().get(this.collection).getTitle();
	}

	public ObjectId getGroup() {
		return group;
	}

	public void setGroup(ObjectId group) {
		this.group = group;
	}

	public String getGroupName() {
		if (this.group == null) {
			return null;
		}
		return DB.getUserGroupDAO().get(this.group).getFriendlyName();
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@JsonIgnore
	public boolean isOpen() {
		return open;
	}

	public void setOpen(boolean open) {
		this.open = open;
	}

	public Timestamp getOpenedAt() {
		return openedAt;
	}

	public void setOpenedAt(Timestamp openedAt) {
		this.openedAt = openedAt;
	}

	@JsonIgnore
	public Timestamp getClosedAt() {
		return closedAt;
	}

	public void setClosedAt(Timestamp closedAt) {
		this.closedAt = closedAt;
	}

	public Activity getActivity() {
		return activity;
	}

	public void setActivity(Activity activity) {
		this.activity = activity;
	}
}