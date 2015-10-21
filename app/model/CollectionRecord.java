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

import java.time.Year;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotNull;

import model.Rights.Access;

import org.bson.types.ObjectId;
import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import utils.Deserializer;
import utils.Serializer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import db.DB;

// there is an option Record link if the link is already materialized
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class CollectionRecord {
	
	public static enum RecordType {
		IMAGE, TEXT, VIDEO, AUDIO, UNKNOWN
	}
	
	public class Provider {
		public String provider;
		public String recordId;
		public String recordUrl;
	}

	@Id
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId dbId;

	private String externalId;

	@NotNull
	@NotBlank
	private String title;
	private String description;

	private String creator;
	
	// an optional URL for the thumbnail
	private String thumbnailUrl;

	// url to the provider web page for that record
	private String isShownAt;

	// url to the (full resoultion) content - external on in the WITH db
	private String isShownBy;

	//media type
	private  RecordType type;

	private int totalLikes;
	
	private String itemRights;

	private ExhibitionRecord exhibitionRecord;

	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId collectionId;

	@JsonSerialize(using = Serializer.DateSerializer.class)
	@JsonDeserialize(using = Deserializer.DateDeserializer.class)
	private Date created;
	
	private List<String> contributors;
	
	private List<Year> year;
	
	private List<Provider> provenanceChain = new ArrayList<Provider>();

	// the place in the collection of this record,
	// mostly irrelevant I would think ..
	private int position;

	// there will be different serializations of the record available in here
	// like "EDM" -> xml for the EDM
	// "json EDM" -> json format of the EDM?
	// "json UI" -> ...
	// "source format" -> ...
	private final Map<String, String> content = new HashMap<String, String>();

	
	private String subject;
	
	// fixed-size, denormalization of Tags on this record
	// When somebody adds a tag to a record, and the cap is not reached, it will
	// go here.
	// This might get out of sync on tag deletes, since a deleted tag from one
	// user doesn't necessarily delete
	// the tag from here. Tag cleaning has to be performed regularly.
	private final Set<String> tags = new HashSet<String>();
	
	private final HashMap<String, Object> extraFields = new HashMap<String, Object>();
	
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId annotation;

	public ObjectId getDbId() {
		return dbId;
	}

	public void setDbId(ObjectId dbId) {
		this.dbId = dbId;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

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

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public String getIsShownAt() {
		return isShownAt;
	}

	public void setIsShownAt(String isShownAt) {
		this.isShownAt = isShownAt;
	}

	public String getIsShownBy() {
		return isShownBy;
	}

	public void setIsShownBy(String isShownBy) {
		this.isShownBy = isShownBy;
	}

	public RecordType getType() {
		return type;
	}

	public void setType(RecordType type) {
		this.type = type;
	}

	public int getTotalLikes() {
		return totalLikes;
	}

	public void setTotalLikes(int totalLikes) {
		this.totalLikes = totalLikes;
	}

	public String getItemRights() {
		return itemRights;
	}

	public void setItemRights(String itemRights) {
		this.itemRights = itemRights;
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

	public Map<String, String> getContent() {
		return content;
	}

	public Set<String> getTags() {
		return tags;
	}

	public ExhibitionRecord getExhibitionRecord() {
		return exhibitionRecord;
	}

	public void setExhibitionReord(ExhibitionRecord exhibitionRecord) {
		this.exhibitionRecord = exhibitionRecord;
	}

	@Override
	public boolean equals(Object record) {
		if ((((CollectionRecord) record).getDbId() != null)
				&& (this.dbId != null))
			return ((CollectionRecord) record).getDbId().equals(this.dbId);
		else
			return false;
	}
	
	public List<Provider> getProvenanceChain() {
		return provenanceChain;
	}
	
	public void setProvenanceChain() {
	}
	
	
	public void addProvider(Provider provider, int position) {
		provenanceChain.add(position, provider);
	}
	
	public String getSource() {
		if (!provenanceChain.isEmpty())
			return provenanceChain.get(provenanceChain.size()-1).provider;
		else 
			return "";
	}
	
	public String getRecordIdInSource() {
		if (!provenanceChain.isEmpty())
			return provenanceChain.get(provenanceChain.size()-1).recordId;
		else 
			return "";
	}
	
	public String getRecordUrlInSource() {
		if (!provenanceChain.isEmpty())
			return provenanceChain.get(provenanceChain.size()-1).recordUrl;
		else 
			return "";
	}

	public List<String> getContributors() {
		return contributors;
	}

	public void setContributors(List<String> contributors) {
		this.contributors = contributors;
	}

	public List<Year> getYear() {
		return year;
	}

	public void setYear(List<Year> year) {
		this.year = year;
	}

	public HashMap<String, Object> getExtraFields() {
		return extraFields;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

}
