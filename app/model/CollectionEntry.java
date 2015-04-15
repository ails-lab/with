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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import utils.Serializer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import db.DB;

@Entity
public class CollectionEntry {

	@Id
	@JsonSerialize(using=Serializer.ObjectIdSerializer.class)
	private ObjectId dbID;

	@JsonSerialize(using=Serializer.ObjectIdSerializer.class)
	private ObjectId collectionId;

	private Date created;

	// the place in the collection of this record,
	// mostly irrelevant I would think ..
	private int position;

	@Embedded
	private RecordLink baseLinkData;

	// there will be different serializations of the record available in here
	// like "EDM" -> xml for the EDM
	// "json EDM" -> json format of the EDM?
	// "json UI" -> ...
	// "source format" -> ...
	private Map<String, String> content = new HashMap<String, String>();

	// fixed-size, denormalization of Tags on this record
	// When somebody adds a tag to a record, and the cap is not reached, it will go here
	// This might get out of sync on tag deletes, since a deleted tag from one user doesn't necessarily delete
	// the tag from here. Tag cleaning has to be performed regularly.
	private Set<String> tags = new HashSet<String>();


	// getter setter section

	public ObjectId getDbID() {
		return dbID;
	}

	public void setDbID(ObjectId dbID) {
		this.dbID = dbID;
	}

	@JsonIgnore
	public Collection getCollection() {
		Collection collection =
				DB.getCollectionDAO().getById(this.collectionId);
		return collection;
	}

	public void setCollectionId(Collection collection) {
		this.collectionId = collection.getDbId();
	}

	@JsonProperty
	public void setCollectionId(ObjectId collectionId) {
		this.collectionId = collectionId;
	}

	public RecordLink getBaseLinkData() {
		return baseLinkData;
	}

	public void setBaseLinkData(RecordLink recordLink) {
		this.baseLinkData = recordLink;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Set<String> getTags() {
		return tags;
	}

	public void setTags(Set<String> tags) {
		this.tags = tags;
	}

	public Map<String, String> getContent() {
		return content;
	}

	public void setContent(Map<String, String> content) {
		this.content = content;
	}

}
