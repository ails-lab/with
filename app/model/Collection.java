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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.bson.types.ObjectId;
import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import db.DB;
import model.WithAccess.Access;
import model.usersAndGroups.User;
import utils.AccessEnumConverter;
import utils.Deserializer;
import utils.Serializer;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Converters(AccessEnumConverter.class)
public class Collection {

	@Id
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId dbId;

	@NotNull
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId creatorId;

	@NotNull
	@NotBlank
	private String title;
	private String description;

	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId thumbnail;

	private int itemCount;
	// private boolean isPublic;
	@JsonSerialize(using = Serializer.DateSerializer.class)
	@JsonDeserialize(using = Deserializer.DateDeserializer.class)
	private Date created;
	@JsonSerialize(using = Serializer.DateSerializer.class)
	@JsonDeserialize(using = Deserializer.DateDeserializer.class)
	private Date lastModified;

	private boolean isExhibition;

	private ExhibitionCollection exhibition;

	// fixed-size list of entries
	// those will be as well in the CollectionEntry table
	// we need to remove some fields from CollectionRecord 
	//otherwise the whole document will be very heavy
	@Embedded
	private final List<CollectionRecord> firstEntries = new ArrayList<CollectionRecord>();

	@JsonSerialize(using = Serializer.WithAccessSerializer.class)
	@JsonDeserialize(using = Deserializer.WithAccessDeserializer.class)
	@Embedded
	private final WithAccess rights = new WithAccess();
	
	@Embedded
	@JsonSerialize(using = Serializer.CustomMapSerializer.class)
	private final Map<ObjectId, Access> underModerationInGroups = new HashMap<ObjectId, Access>();

	public ObjectId getDbId() {
		return this.dbId;
	}

	@JsonProperty
	public void setDbId(ObjectId id) {
		this.dbId = id;
	}

	/**
	 * Get the embeddable Metadata part
	 *
	 * @return
	 */
	/*
	 * public CollectionMetadata collectMetadata() { CollectionMetadata cm = new
	 * CollectionMetadata(); cm.setCollectionId(this.dbId);
	 * cm.setDescription(description); cm.setThumbnail(thumbnail);
	 * cm.setTitle(title); return cm; }
	 */
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

	public boolean getIsPublic() {
		return rights.isPublic();
	}

	public void setIsPublic(boolean isPublic) {
		rights.setPublic(isPublic);
	}

	public User retrieveCreator() {
		return DB.getUserDAO().getById(this.creatorId, null);
	}

	public ObjectId getCreatorId() {
		return this.creatorId;
	}

	@JsonProperty
	public void setCreatorId(ObjectId creatorId) {
		// Set owner for first time
		if (this.creatorId == null) {
			this.creatorId = creatorId;
			rights.put(this.creatorId, Access.OWN);
			// Owner has changed
		} else if (!this.creatorId.equals(creatorId)) {
			// Remove rights for old owner
			rights.remove(this.creatorId, Access.OWN);
			this.creatorId = null;
			setCreatorId(creatorId);
		}
	}

	public void setCreatorId(User creator) {
		setCreatorId(creator.getDbId());
	}

	public List<CollectionRecord> getFirstEntries() {
		return firstEntries;
	}

	public String getThumbnailUrl() {

		if (firstEntries.size() > 0)
			return firstEntries.get(0).getThumbnailUrl();
		return null;

	}

	public Media retrieveThumbnail() {
		Media media = DB.getMediaDAO().findById(this.thumbnail);
		return media;
	}

	public ObjectId getThumbnail() {
		return this.thumbnail;
	}

	// @JsonProperty
	@JsonIgnore
	public void setThumbnail(ObjectId thumbId) {
		this.thumbnail = thumbId;
	}

	@JsonIgnore
	public void setThumbnail(Media thumbnail) {
		this.thumbnail = thumbnail.getDbId();
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

	public int getItemCount() {
		return itemCount;
	}

	public void setItemCount(int itemCount) {
		this.itemCount = itemCount;
	}

	public void itemCountIncr() {
		this.itemCount++;
	}

	public void itemCountDec() {
		this.itemCount--;
	}

	public WithAccess getRights() {
		return rights;
	}

	@JsonIgnore
	public void setRights(WithAccess r) {
	}

	public boolean getIsExhibition() {
		return isExhibition;
	}

	public void setIsExhibition(boolean isExhibition) {
		this.isExhibition = isExhibition;
	}

	public ExhibitionCollection getExhibition() {
		return exhibition;
	}

	public void setExhibition(ExhibitionCollection exhibition) {
		this.exhibition = exhibition;
	}

	public void addForModeration(ObjectId groupId, Access access) {
		this.underModerationInGroups.put(groupId, access);
	}

	public Access removeFromModeration(ObjectId groupId) {
		return this.underModerationInGroups.remove(groupId);
	}
}
