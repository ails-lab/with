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


package notifications;

import java.sql.Timestamp;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import db.DB;
import model.usersAndGroups.User;
import model.usersAndGroups.UserGroup;
import utils.Serializer;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity("Notification")
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@Indexes({
	@Index(fields = @Field(value = "receiver", type = IndexType.ASC), options = @IndexOptions()),
	@Index(fields = @Field(value = "readAt", type = IndexType.ASC), options = @IndexOptions()),
	@Index(fields = @Field(value = "pendingResponse", type = IndexType.ASC), options = @IndexOptions()),
	@Index(fields = @Field(value = "activity", type = IndexType.ASC), options = @IndexOptions())})
public class Notification {

	public enum Activity {
		// group related
		GROUP_INVITE, GROUP_INVITE_ACCEPT, GROUP_INVITE_DECLINED, GROUP_REMOVAL,
		GROUP_REQUEST, GROUP_REQUEST_ACCEPT, GROUP_REQUEST_DENIED,
		//resource related
		RECORD_ADDED_TO_COLLECTION, RECORD_REMOVED_FROM_COLLECTION,
		COLLECTION_SHARE, COLLECTION_SHARED, COLLECTION_UNSHARED, COLLECTION_REJECTED,
		EXHIBITION_SHARE, EXHIBITION_SHARED, EXHIBITION_UNSHARED, EXHIBITION_REJECTED,
		// annotation
		COLLECTION_ANNOTATING_COMPLETED, RECORD_ANNOTATING_COMPLETED,
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
	private String senderLogoUrl;

	private String message;
	// While the notification is pending for an answer, it remains open
	private boolean pendingResponse;
	private Timestamp openedAt;
	private Timestamp readAt;

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
		if (user != null) {
			return user.getFirstName() + " " + user.getLastName();
		}
		UserGroup group = DB.getUserGroupDAO().get(this.sender);
		if (group != null) {
			return group.getFriendlyName();
		}
		return "DELETED";
	}

	public void setSender(ObjectId sender) {
		this.sender = sender;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public boolean isPendingResponse() {
		return pendingResponse;
	}

	public void setPendingResponse(boolean pendingResponse) {
		this.pendingResponse = pendingResponse;
	}

	public Timestamp getOpenedAt() {
		return openedAt;
	}

	public void setOpenedAt(Timestamp openedAt) {
		this.openedAt = openedAt;
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

	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (other == this)
			return true;
		if (!(other instanceof Notification))
			return false;
		Notification otherNotification = (Notification) other;
		return (otherNotification.hashCode() == this.hashCode());
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31).append(this.dbId).toHashCode();
	}

	public String getSenderLogoUrl() {
		return senderLogoUrl;
	}

	public void setSenderLogoUrl(String senderLogoUrl) {
		this.senderLogoUrl = senderLogoUrl;
	}

}