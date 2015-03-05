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

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import db.DB;

@Entity
public class Collection {

	@Id
	private ObjectId dbId;

	private ObjectId owner;

	private String title;
	private String description;

	private boolean isPublic;

	// fixed-size list of entries
	// those will be as well in the CollectionEntry table
	@Embedded
	private List<RecordLink> firstEntries;

	public ObjectId getDbId() {
		return this.dbId;
	}

	public void setDbId(ObjectId id) {
		this.dbId = id;
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
	public boolean isPublic() {
		return isPublic;
	}
	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}
	public User getOwner() {
		User user =
				DB.getUserDAO().getById(this.owner);
		return user;
	}
	public void setOwner(User owner) {
		this.owner = owner.getDbId();
	}
	public List<RecordLink> getFirstEntries() {
		return firstEntries;
	}
	public void setFirstEntries(List<RecordLink> firstEntries) {
		this.firstEntries = firstEntries;
	}

}
