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

	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId collection;

	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId group;
	private String message;
	private boolean open;
	private Timestamp sentAt;
	private Timestamp readAt;
}