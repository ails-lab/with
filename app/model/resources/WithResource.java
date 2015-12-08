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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import utils.Deserializer;
import utils.Serializer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import db.DB;
import db.converters.RightsConverter;
import model.DescriptiveData;
import model.EmbeddedMediaObject;
import model.ExampleDataModels.LiteralOrResource.ResourceType;
import model.annotations.Annotation;
import model.annotations.ContextAnnotation;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import model.usersAndGroups.User;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Entity("RecordResource")
public class WithResource<T extends DescriptiveData> {

	public static class WithAdmin {

		@JsonSerialize(using = Serializer.WithAccessSerializer.class)
		@JsonDeserialize(using = Deserializer.WithAccessDeserializer.class)
		@Embedded
		private WithAccess access = new WithAccess();

		/*
		 * withCreator is empty in cases of records imported from external
		 * resources. For resources uploaded by a user, it links to the userId
		 * who uploaded that resource. For collections, it links to the userId
		 * who created the collection.
		 */
		@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
		private ObjectId withCreator;

		// uri that this resource has in the rdf repository
		private String withURI;

		@JsonSerialize(using = Serializer.DateSerializer.class)
		@JsonDeserialize(using = Deserializer.DateDeserializer.class)
		private Date created;

		@JsonSerialize(using = Serializer.DateSerializer.class)
		@JsonDeserialize(using = Deserializer.DateDeserializer.class)
		private Date lastModified;

		@Embedded
		@JsonSerialize(using = Serializer.AccessMapSerializer.class)
		@JsonDeserialize(using = Deserializer.AccessMapDeserializer.class)
		private final Map<ObjectId, Access> underModeration = new HashMap<ObjectId, Access>();

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
		}

		public Date getLastModified() {
			return lastModified;
		}

		public void setLastModified(Date lastModified) {
			this.lastModified = lastModified;
		}

		public void addForModeration(ObjectId groupId, Access access) {
			this.underModeration.put(groupId, access);
		}

		public Access removeFromModeration(ObjectId groupId) {
			return this.underModeration.remove(groupId);
		}

		public ObjectId getWithCreator() {
			return withCreator;
		}

		public void setWithCreator(ObjectId creatorId) {
			// Set owner for first time
			if (this.withCreator == null) {
				this.withCreator = creatorId;
				this.access.put(creatorId, Access.OWN);
				// Owner has changed
			} else if (!this.withCreator.equals(creatorId)) {
				// Remove rights for old owner
				access.remove(this.withCreator, Access.OWN);
				withCreator = null;
				setWithCreator(creatorId);
			}
		}

		public User retrieveWithCreator() {
			ObjectId userId = getWithCreator();
			if (userId != null)
				return DB.getUserDAO().getById(userId, null);
			else
				return null;
		}

	}

	public static class Usage {
		// in how many favorites is it
		private int likes;

		// in how many user collections is it
		private int collected;
		// how many modified versions exist
		private int annotated;

		// how often is it viewed, don't count count api calls,
		// count UI messages for viewing
		private int viewCount;

		// implementation detail, put any tag on the record twice,
		// with userID prepended and without. This will allow for people to look
		// for their own tags.
		private ArrayList<String> tags;

		public int getLikes() {
			return likes;
		}

		public void setLikes(int likes) {
			this.likes = likes;
		}
		
		public void incLikes() {
			this.likes++;
		}
		
		public void decLikes() {
			this.likes--;
		}

		public int getCollected() {
			return collected;
		}

		public void setCollected(int collected) {
			this.collected = collected;
		}
		
		public void incCollected() {
			this.collected++;
		}
		
		public void decCollected() {
			this.collected--;
		}

		public int getAnnotated() {
			return annotated;
		}

		public void setAnnotated(int annotated) {
			this.annotated = annotated;
		}

		public int getViewCount() {
			return viewCount;
		}

		public void setViewCount(int viewCount) {
			this.viewCount = viewCount;
		}

		public ArrayList<String> getTags() {
			return tags;
		}

		public void setTags(ArrayList<String> tags) {
			this.tags = tags;
		}
	}

	/**
	 * If we know about collections from our sources, the info goes here For
	 * single records, fill in the position or next in sequence, for general
	 * collection linking, omit it (i.e. if the resource is of type collection,
	 * the colletionUri refers to the external "equivalent" collection).
	 *
	 */
	public static class ExternalCollection {
		// known sources only
		private String source;
		private String collectionUri;
		private String nextInSequenceUri;
		private int position;
		private String title;
		private String description;

		public String getSource() {
			return source;
		}

		public void setSource(String source) {
			this.source = source;
		}

		public String getCollectionUri() {
			return collectionUri;
		}

		public void setCollectionUri(String collectionUri) {
			this.collectionUri = collectionUri;
		}

		public String getNextInSequenceUri() {
			return nextInSequenceUri;
		}

		public void setNextInSequenceUri(String nextInSequenceUri) {
			this.nextInSequenceUri = nextInSequenceUri;
		}

		public int getPosition() {
			return position;
		}

		public void setPosition(int position) {
			this.position = position;
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
	}

	public static enum WithResourceType {
		WithResource, CollectionObject, CulturalObject, EuScreenObject, EventObject, PlaceObject, TimespanObject;
	}

	@Id
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId dbId;

	public ObjectId getDbId() {
		return dbId;
	}

	public void setDbId(ObjectId dbId) {
		this.dbId = dbId;
	}

	@Embedded
	protected WithAdmin administrative;

	@Embedded
	private HashMap<ObjectId, ArrayList<Integer>> collectedIn;

	@Embedded
	private Usage usage;

	@Embedded
	// external collections to which the resource may belong to
	private ArrayList<ExternalCollection> externalCollections;
	@Embedded
	private ArrayList<ProvenanceInfo> provenance;

	// enum of classes that are derived from DescriptiveData
	protected WithResourceType resourceType;

	// metadata
	@Embedded
	private T descriptiveData;

	// All the available content serializations
	// all keys in here should be understood by the WITH system
	private HashMap<String, String> content;

	// all attached media Objects (their embedded part)
	private ArrayList<EmbeddedMediaObject> media;

	// embedded for some or all, not sure
	// key is CollectionInfo.toString()
	private HashMap<String, ContextAnnotation> contextAnnotation;

	private ArrayList<Annotation> annotations;

	public WithResource() {
		this.usage = new Usage();
		this.administrative = new WithAdmin();
		this.provenance = new ArrayList<ProvenanceInfo>();
		this.collectedIn = new HashMap<ObjectId, ArrayList<Integer>>();
	}

	public WithResource(Class<?> clazz) {
		this.usage = new Usage();
		this.administrative = new WithAdmin();
		this.provenance = new ArrayList<ProvenanceInfo>();
		this.collectedIn = new HashMap<ObjectId, ArrayList<Integer>>();
		resourceType = WithResourceType.valueOf(clazz.getSimpleName());
	}

	/*
	 * Getters/Setters
	 */
	public WithAdmin getAdministrative() {
		return administrative;
	}

	public void setAdministrative(WithAdmin administrative) {
		this.administrative = administrative;
	}

	public HashMap<ObjectId, ArrayList<Integer>> getCollectedIn() {
		return collectedIn;
	}

	public void setCollectedIn(HashMap<ObjectId, ArrayList<Integer>> collectedIn) {
		this.collectedIn = collectedIn;
	}

	public void addPositionToCollectedIn(ObjectId colId, Integer position) {
		if (collectedIn.containsKey(colId)) {
			collectedIn.get(colId).add(position);
		} else
			collectedIn.put(colId,
					new ArrayList<Integer>(Arrays.asList(position)));
	}

	public Usage getUsage() {
		return usage;
	}

	public void setUsage(Usage usage) {
		this.usage = usage;
	}

	public ArrayList<ExternalCollection> getExternalCollections() {
		return externalCollections;
	}

	public void setExternalCollections(
			ArrayList<ExternalCollection> externalCollections) {
		this.externalCollections = externalCollections;
	}

	public ArrayList<ProvenanceInfo> getProvenance() {
		return provenance;
	}

	public void setProvenance(ArrayList<ProvenanceInfo> provenance) {
		this.provenance = provenance;
	}

	public void addToProvenance(ProvenanceInfo provInfo) {
		this.provenance.add(provInfo);
	}

	public void addToProvenance(ProvenanceInfo provInfo, int position) {
		provenance.add(position, provInfo);
	}

	public WithResourceType getResourceType() {
		return resourceType;
	}

	@JsonIgnore
	public void setResourceType(WithResourceType resourceType) {
		this.resourceType = resourceType;
	}

	public T getDescriptiveData() {
		return descriptiveData;
	}

	public void setDescriptiveData(T descriptiveData) {
		this.descriptiveData = descriptiveData;
	}

	public HashMap<String, String> getContent() {
		return content;
	}

	public void setContent(HashMap<String, String> content) {
		this.content = content;
	}

	public ArrayList<EmbeddedMediaObject> getMedia() {
		return media;
	}

	public void setMedia(ArrayList<EmbeddedMediaObject> media) {
		this.media = media;
	}

	public HashMap<String, ContextAnnotation> getContextAnnotation() {
		return contextAnnotation;
	}

	public void setContextAnnotation(
			HashMap<String, ContextAnnotation> contextAnnotation) {
		this.contextAnnotation = contextAnnotation;
	}

	public ArrayList<Annotation> getAnnotations() {
		return annotations;
	}

	public void setAnnotations(ArrayList<Annotation> annotations) {
		this.annotations = annotations;
	}

	/*
	 * CollectionObject embedded logic
	 */
	public User retrieveCreator() {
		return DB.getUserDAO().getById(this.administrative.withCreator, null);
	}
}
