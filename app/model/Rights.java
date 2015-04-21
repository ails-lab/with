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

import java.util.Date;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;

import utils.Serializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Semantics is: The owner can create or remove a rights object
 * for Collection, Media, Search, ?? objects.
 *
 * @author stabenau
 *
 */
public class Rights {

	@Id
	@JsonSerialize(using=Serializer.ObjectIdSerializer.class)
	public ObjectId dbId;

	public static enum Access {
		NONE, READ, WRITE, OWN
	}

	@JsonSerialize(using=Serializer.ObjectIdSerializer.class)
	public ObjectId receiverId;
	@JsonSerialize(using=Serializer.ObjectIdSerializer.class)
	public ObjectId ownerId;

	@JsonSerialize(using=Serializer.ObjectIdSerializer.class)
	public ObjectId objectId;
	public String collectionName;

	public Access access;

	public Date created;



	// getter setter section
	public ObjectId getDbId() {
		return dbId;
	}

	public void setDbId(ObjectId dbId) {
		this.dbId = dbId;
	}

	public ObjectId getObjectId() {
		return objectId;
	}

	public void setObjectId(ObjectId objectId) {
		this.objectId = objectId;
	}

	public ObjectId getReceiverId() {
		return receiverId;
	}

	public void setReceiverId(ObjectId receiverId) {
		this.receiverId = receiverId;
	}

	public ObjectId getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(ObjectId ownerId) {
		this.ownerId = ownerId;
	}


	public Access getAccess() {
		return access;
	}

	public void setAccess(Access access) {
		this.access = access;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public String getCollectionName() {
		return collectionName;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}


}

