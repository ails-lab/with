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

import model.WithAccess.Access;

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

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class CollectionRecord extends ExternalBasicRecord {

	@Id
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId dbId;

	private int totalLikes;
	
	private ExhibitionRecord exhibitionRecord;
	
	private Provider importedFrom;

	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId collectionId;

	/**
	 * Mongo uses ISODate type to store dates.
	 * That's why we need these jackson annotation 
	 * to keep the format yyyy//MM/dd at the frontend.
	 * 
	 * Tip: If we want to write queries/criteria for
	 * the create field we have to use ISODate() constructor
	 * for these. 
	 */
	@JsonSerialize(using = Serializer.DateSerializer.class)
	@JsonDeserialize(using = Deserializer.DateDeserializer.class)
	private Date created;
	
	// the place in the collection of this record,
	// mostly irrelevant I would think ..
	private int position;

	// there will be different serializations of the record available in here
	// like "EDM" -> xml for the EDM
	// "json EDM" -> json format of the EDM?
	// "json UI" -> ...
	// "source format" -> ...
	/*
	 *  Some Questions:
	 *  
	 *  1. Do we need to store XML/JSON to BSON in Mongo?
	 *  2. Do we need to query that data?
	 *  3. Finally what is the reason to store them?
	 *  4. Do we need to index them?
	 */
	private final Map<String, String> content = new HashMap<String, String>();
	
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

	public int getTotalLikes() {
		return totalLikes;
	}

	public void setTotalLikes(int totalLikes) {
		this.totalLikes = totalLikes;
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

	public HashMap<String, Object> getExtraFields() {
		return extraFields;
	}

	public Provider getImportedFrom() {
		return importedFrom;
	}

	public void setImportedFrom(Provider importedFrom) {
		this.importedFrom = importedFrom;
	}

}
