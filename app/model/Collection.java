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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.bson.types.ObjectId;
import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import utils.Serializer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import db.DB;
@Entity
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(value=JsonInclude.Include.NON_NULL)
public class Collection {
	private static final int EMBEDDED_CAP = 20;


	@Id
	@JsonSerialize(using=Serializer.ObjectIdSerializer.class)
	private ObjectId dbId;

	@NotNull
	@JsonSerialize(using=Serializer.ObjectIdSerializer.class)
	private ObjectId owner;

	@NotNull
	@NotBlank
	private String title;
	private String description;

	@JsonSerialize(using=Serializer.ObjectIdSerializer.class)
	private ObjectId thumbnail;

	@Embedded
	private Record exampleRecord;
	private boolean isPublic;
	private Date created;
	private Date lastModified;
	private String category;

	// fixed-size list of entries
	// those will be as well in the CollectionEntry table
	@Embedded
	private List<RecordLink> firstEntries = new ArrayList<RecordLink>();


	public ObjectId getDbId() {
		return this.dbId;
	}

	@JsonProperty
	public void setDbId(ObjectId id) {
		this.dbId = id;
	}


	public void addEntry( CollectionEntry ce ) {

	}
	/**
	 * Get the embeddable Metadata part
	 * @return
	 */
	public CollectionMetadata collectMetadata() {
		CollectionMetadata cm = new CollectionMetadata();
		cm.setCollection(this.dbId);
		cm.setDescription(description);
		cm.setThumbnail(thumbnail);
		cm.setTitle(title);

		return cm;
	}

	// Getter setters
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public boolean isPublic() {
		return isPublic;
	}
	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}
	public User retrieveOwner() {
		return	DB.getUserDAO().getById(this.owner);
	}

	public ObjectId getOwner() {
		return this.owner;
	}

	@JsonProperty
	public void setOwner(ObjectId ownerId) {
		this.owner = ownerId;
	}

	public void setOwner(User owner) {
		//set owner to collection
		this.owner = owner.getDbId();

		//create a new collection metadata for owner
		owner.getCollectionMetadata().add(collectMetadata());

		//save the new owner
		DB.getUserDAO().makePermanent(owner);
	}

	public List<RecordLink> getFirstEntries() {
		return firstEntries;
	}

	public void setFirstEntries(List<RecordLink> firstEntries) {
		this.firstEntries = firstEntries;
	}

	public String getThumbnailUrl() {

		if(firstEntries.size() > 0)
			return 	firstEntries.get(0).getThumbnailUrl();
		return null;

	}

	public Media retrieveThumbnail() {
		Media media =
				DB.getMediaDAO().findById(this.thumbnail);
		return media;
	}

	public ObjectId getThumbnail() {
		return this.thumbnail;
	}

	@JsonProperty
	public void setThumbnail(ObjectId thumbId) {
		this.thumbnail = thumbId;
	}

	public void setThumbnail(Media thumbnail) {
		this.thumbnail = thumbnail.getDbId();
	}

	public Record getExampleRecord() {
		return exampleRecord;
	}

	public void setExampleRecord(Record exampleRecord) {
		this.exampleRecord = exampleRecord;
	}


	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

}
