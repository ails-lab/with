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
import org.mongodb.morphia.annotations.Embedded;

import db.DB;

@Embedded
public class CollectionMetadata {

	private String title;
	private String description;

	//ref to collection
	private ObjectId collection;
	//ref to thumbnail
	private ObjectId thumbnail;

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

	public Collection getCollection() {
		Collection collection =
				DB.getCollectionDAO().getById(this.collection);
		return collection;
	}

	public void setCollection(Collection collection) {
		this.collection = collection.getDbId();
	}

	public Media getThumbnail() {
		Media thumbnail =
				DB.getMediaDAO().findById(this.thumbnail);
		return thumbnail;
	}

	public void setThumbnail(Media thumbnail) {
		this.thumbnail = thumbnail.getDbId();
	}

	public void setThumbnail(ObjectId thumbnail) {
		this.thumbnail = thumbnail;
	}
}
