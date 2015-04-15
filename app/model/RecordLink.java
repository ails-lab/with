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

import java.util.List;

import javax.validation.constraints.NotNull;

import org.bson.types.ObjectId;
import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import utils.Serializer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import db.DB;


// there is an option Record link if the link is already materialized
@Entity
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(value=JsonInclude.Include.NON_NULL)
public class RecordLink {

	@Id
	@JsonSerialize(using=Serializer.ObjectIdSerializer.class)
	private ObjectId dbId;

	// which backend provided this entry
	// Europeana, DPLA ....
	private String source;

	// an optional URL for the thumbnail
	private String thumbnailUrl;

	// an optional cached version of a thumbnail for this record'
	@NotNull
	@JsonSerialize(using=Serializer.ObjectIdSerializer.class)
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


	public ObjectId getDbId() {
		return dbId;
	}

	public void setDbId(ObjectId dbId) {
		this.dbId = dbId;
	}

	/**
	 * Retrive all personalized records referred to this
	 * (outside) record link
	 * @return
	 */
	public List<CollectionEntry> retrievePersonalizedRecords() {
		return DB.getCollectionEntryDAO().getByRecLinkId(this.dbId);
	}


	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public Media retrieveThumbnail() {
		Media thumbnail =
				DB.getMediaDAO().findById(this.thumbnail);
		return thumbnail;
	}

	public ObjectId getThumbnail() {
		return this.thumbnail;
	}

	public void setThumbnail(Media thumbnail) {
		this.thumbnail = thumbnail.getDbId();
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

	public String getThumbnailUrl() {
		return "/recordlink/" +
				this.getDbId().toString() +
				"/thumbnail";
	}

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

}
