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

import javax.validation.constraints.NotNull;

import org.bson.types.ObjectId;
import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import utils.Serializer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import db.DB;

// there is an option Record link if the link is already materialized
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class CollectionRecord {

	@Id
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId dbId;

	// which backend provided this entry
	// Europeana, DPLA ....
	private String source;

	// an optional URL for the thumbnail
	private String thumbnailUrl;

	// an optional cached version of a thumbnail for this record'
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId thumbnail;

	@NotNull
	@NotBlank
	private String title;
	private String description;

	// the id in the source system
	private String sourceId;
	// a link to the record on its source
	private String sourceUrl;

	private String type;

	private String rights;

	// collection specific stuff...

	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId collectionId;

	private Date created;

	// the place in the collection of this record,
	// mostly irrelevant I would think ..
	private int position;

	// there will be different serializations of the record available in here
	// like "EDM" -> xml for the EDM
	// "json EDM" -> json format of the EDM?
	// "json UI" -> ...
	// "source format" -> ...
	private Map<String, String> content = new HashMap<String, String>();

	// fixed-size, denormalization of Tags on this record
	// When somebody adds a tag to a record, and the cap is not reached, it will
	// go here
	// This might get out of sync on tag deletes, since a deleted tag from one
	// user doesn't necessarily delete
	// the tag from here. Tag cleaning has to be performed regularly.
	private final Set<String> tags = new HashSet<String>();

	public ObjectId getDbId() {
		return dbId;
	}

	public void setDbId(ObjectId dbId) {
		this.dbId = dbId;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		if (source.toLowerCase().contains("europeana")) {
			this.source = "Europeana";
		} else if (source.toLowerCase().contains("dpla")) {
			this.source = "DPLA";
		}
		this.source = source;
	}

	public Media retrieveThumbnail() {
		Media thumbnail = DB.getMediaDAO().findById(this.thumbnail);
		return thumbnail;
	}

	public ObjectId getThumbnail() {
		return this.thumbnail;
	}

	@JsonIgnore
	public void setThumbnail(ObjectId thumbnail) {
		this.thumbnail = thumbnail;
	}

	/*
	 * public String getThumbnailUrl() { return "/recordlink/" +
	 * this.getDbId().toString() + "/thumbnail"; }
	 */

	public String getThumbnailUrl() {
		return this.thumbnailUrl;

	}

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

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

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public void setSourceUrl(String sourceUrl) {
		this.sourceUrl = sourceUrl;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getRights() {
		return rights;
	}

	public void setRights(String rights) {
		this.rights = rights;
	}

	@JsonIgnore
	public Collection getCollection() {
		return DB.getCollectionDAO().getById(this.collectionId);
	}

	public ObjectId getCollectionId() {
		return collectionId;
	}

	@JsonProperty
	public void setCollectionId(ObjectId collectionId) {
		this.collectionId = collectionId;
	}

	public void setCollectionId(Collection collection) {
		this.collectionId = collection.getDbId();
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}
	
	public void setContent(Map<String, String> content) {
		this.content = content;
	}

	public Map<String, String> getContent() {
		return content;
	}

	public Set<String> getTags() {
		return tags;
	}

}
