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

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import db.DB;


// there is an option Record link if the link is already materialized
@Entity
public class RecordLink {

	@Id
	private ObjectId dbId;
	// optional link to the materialized Record
	private ObjectId recordReference;

	// which backend provided this entry
	// Europeana, DPLA ....
	private String source;

	// an optional URL for the thumbnail
	private String thumbnailUrl;

	// an optional cached version of a thumbnail for this record'
	private ObjectId thumbnail;

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

	public Record getRecordReference() {
		Record record =
				DB.getRecordDAO().getById(this.recordReference);
		return record;
	}

	public void setRecordReference(Record recordReference) {
		this.recordReference = recordReference.getDbID();
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public Media getThumbnail() {
		Media thumbnail =
				DB.getMediaDAO().findById(this.thumbnail.toString());
		return thumbnail;
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
		return thumbnailUrl;
	}

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}


}
