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


package model.resources;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Version;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import db.DB;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import model.basicDataTypes.WithAccess.AccessEntry;
import model.usersAndGroups.User;
import utils.Deserializer;
import utils.Serializer;

public class WithAdmin {

	// index
	@JsonSerialize(using = Serializer.WithAccessSerializer.class)
	@JsonDeserialize(using = Deserializer.WithAccessDeserializer.class)
	private WithAccess access = new WithAccess();
	
	private List<AccessEntry> collectedBy = new ArrayList<AccessEntry>();
	
	private Map<String, Integer> annotators = new HashMap<String, Integer>();

	/*
	 * withCreator is empty in cases of records imported from external
	 * resources. For resources uploaded by a user, it links to the userId
	 * who uploaded that resource. For collections, it links to the userId
	 * who created the collection.
	 */
	// index
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId withCreator = null;

	// uri that this resource has in the rdf repository
	private String withURI;

	@JsonSerialize(using = Serializer.DateSerializer.class)
	@JsonDeserialize(using = Deserializer.DateDeserializer.class)
	private Date created;

	@JsonSerialize(using = Serializer.DateSerializer.class)
	@JsonDeserialize(using = Deserializer.DateDeserializer.class)
	@Version
	private Date lastModified;

	@Embedded
	// @JsonSerialize(using = Serializer.AccessMapSerializer.class)
	// @JsonDeserialize(using = Deserializer.AccessMapDeserializer.class)
	// private final Map<ObjectId, Access> underModeration = new
	// HashMap<ObjectId, Access>();
	// recordId of last entry of provenance chain id the resource has been
	// imported from external resource
	// dbId if uploaded by user
	protected String externalId;

	public WithAccess getAccess() {
		return access;
	}

	@JsonIgnore
	public void setAccess(WithAccess access) {
		this.access = access;
	}

	public String getWithURI() {
		return withURI;
	}

	public void setWithURI(String withURI) {
		this.withURI = withURI;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
		if (this.lastModified == null) {
			this.lastModified = created;
		}
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public ObjectId getWithCreator() {
		return withCreator;
	}

	public void setWithCreator(ObjectId creatorId) {
		// OWN rights from old creator are not withdrawn (ownership is not
		// identical to creation role)
		this.withCreator = creatorId;
		if (withCreator != null)
			this.getAccess().addToAcl(creatorId, Access.OWN);
	}

	public User retrieveWithCreator() {
		ObjectId userId = getWithCreator();
		if (userId != null)
			return DB.getUserDAO().getById(userId);
		else
			return null;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public List<AccessEntry> getCollectedBy() {
		return collectedBy;
	}

	public void setCollectedBy(List<AccessEntry> collectedBy) {
		this.collectedBy = collectedBy;
	}

	public Map<String, Integer> getAnnotators() {
		return annotators;
	}

	public void setAnnotators(Map<String, Integer> annotators) {
		this.annotators = annotators;
	}

}