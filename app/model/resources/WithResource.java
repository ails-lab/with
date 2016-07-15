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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import model.DescriptiveData;
import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.annotations.Annotation;
import model.annotations.ContextData;
import model.basicDataTypes.CollectionInfo;
import model.basicDataTypes.ProvenanceInfo;
import model.usersAndGroups.User;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import sources.formatreaders.CulturalRecordFormatter;
import utils.Serializer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;
import elastic.ElasticUtils;

@SuppressWarnings("rawtypes")
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity("RecordResource")
@Indexes({
		@Index(fields = @Field(value = "resourceType", type = IndexType.ASC), options = @IndexOptions()),
		@Index(fields = @Field(value = "administrative.withCreator", type = IndexType.ASC), options = @IndexOptions()),
		@Index(fields = @Field(value = "administrative.externalId", type = IndexType.ASC), options = @IndexOptions()),
		@Index(fields = @Field(value = "provenance.provider", type = IndexType.ASC), options = @IndexOptions()),
		@Index(fields = @Field(value = "provenance.resourceId", type = IndexType.ASC), options = @IndexOptions()),
		@Index(fields = @Field(value = "descriptiveData.label", type = IndexType.ASC), options = @IndexOptions()),
		@Index(fields = @Field(value = "collectedIn", type = IndexType.ASC), options = @IndexOptions()) })
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class WithResource<T extends DescriptiveData, U extends WithAdmin> {

	public static final ALogger log = Logger.of( WithResource.class );
	
	
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
	protected U administrative;

	@Embedded
	@JsonSerialize(using = Serializer.ObjectIdArraySerializer.class)
	private List<ObjectId> collectedIn;

	@Embedded
	private Usage usage;

	@Embedded
	// external collections to which the resource may belong to
	private List<ExternalCollection> externalCollections;

	@Embedded
	// depending on the source, we know which entry is the dataProvider and
	// which the provider
	private List<ProvenanceInfo> provenance;

	// enum of classes that are derived from DescriptiveData
	protected WithResourceType resourceType;

	// metadata
	@Embedded
	protected T descriptiveData;

	// All the available content serializations
	// all keys in here should be understood by the WITH system
	private HashMap<String, String> content;

	// all attached media Objects (their embedded part)
	@Embedded
	private List<HashMap<MediaVersion, EmbeddedMediaObject>> media;

	@Embedded
	//@JsonDeserialize(using = Deserializer.ContextDataDeserializer.class)
	private ContextData contextData;
	
	@JsonInclude(Include.ALWAYS)
	private double qualityMeasure;

	public double getQualityMeasure() {
		return qualityMeasure;
	}

	public void setQualityMeasure(double qualityMeasure) {
		this.qualityMeasure = qualityMeasure;
	}
	
	@JsonSerialize(using = Serializer.ObjectIdArraySerializer.class)
	private Set<ObjectId> annotationIds;
	private List<Annotation> annotations;

	public WithResource() {
		this.usage = new Usage();
		this.provenance = new ArrayList<ProvenanceInfo>();
		this.collectedIn = new ArrayList<ObjectId>();
		this.contextData = new ContextData();
		this.media = new ArrayList<>();
		HashMap<MediaVersion, EmbeddedMediaObject> embedded = new HashMap<MediaVersion, EmbeddedMediaObject>();
		embedded.put(MediaVersion.Thumbnail, new EmbeddedMediaObject());
		this.media.add(embedded);
	}

	public WithResource(Class<?> clazz) {
		this.usage = new Usage();
		this.provenance = new ArrayList<ProvenanceInfo>();
		this.collectedIn = new ArrayList<ObjectId>();
		this.media = new ArrayList<>();
		this.contextData = new ContextData();
		HashMap<MediaVersion, EmbeddedMediaObject> embedded = new HashMap<MediaVersion, EmbeddedMediaObject>();
		embedded.put(MediaVersion.Thumbnail, new EmbeddedMediaObject());
		this.media.add(embedded);
		this.resourceType = WithResourceType.valueOf(clazz.getSimpleName());
	}

	/*
	 * Getters/Setters
	 */
	public U getAdministrative() {
		return administrative;
	}

	public void setAdministrative(U administrative) {
		this.administrative = administrative;
	}

	public List<ObjectId> getCollectedIn() {
		return collectedIn;
	}

	public void setCollectedIn(List<ObjectId> collectedIn) {
		this.collectedIn = collectedIn;
	}

	public void addPositionToCollectedIn(ObjectId colId) {
		if (collectedIn == null)
			collectedIn = new ArrayList<ObjectId>();
		collectedIn.add(colId);

	}

	public void removePositionFromCollectedIn(ObjectId colId, Integer position) {
		collectedIn.remove(new CollectionInfo(colId, position));
	}

	public Usage getUsage() {
		return usage;
	}

	public void setUsage(Usage usage) {
		this.usage = usage;
	}

	public List<ExternalCollection> getExternalCollections() {
		return externalCollections;
	}

	public void setExternalCollections(
			ArrayList<ExternalCollection> externalCollections) {
		this.externalCollections = externalCollections;
	}

	public List<ProvenanceInfo> getProvenance() {
		return provenance;
	}

	public void setProvenance(List<ProvenanceInfo> provenance) {
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

	public List<HashMap<MediaVersion, EmbeddedMediaObject>> getMedia() {
		return media;
	}

	public void setMedia(List<HashMap<MediaVersion, EmbeddedMediaObject>> media) {
		this.media = media;
	}

	public void addMediaView(MediaVersion mediaVersion,
			EmbeddedMediaObject media, int viewIndex) {
		media.setMediaVersion(mediaVersion);
		this.media.get(viewIndex).put(mediaVersion, media);
	}

	/**
	 * adds additional media objects to the resource.
	 * @param mediaVersion
	 * @param media
	 */
	public void addMediaView(MediaVersion mediaVersion,
			EmbeddedMediaObject media) {
		media.setMediaVersion(mediaVersion);
		HashMap<MediaVersion, EmbeddedMediaObject> e = new HashMap<>();
		HashMap<MediaVersion, EmbeddedMediaObject> map = findMedia(media);
		if (map!=null){
			log.debug("Media updated!!");
			map.put(mediaVersion, media);
		} else {
			e.put(mediaVersion, media);
			this.media.add(e);
		}
	}

	private HashMap<MediaVersion, EmbeddedMediaObject> findMedia(EmbeddedMediaObject media2) {
		for (HashMap<MediaVersion, EmbeddedMediaObject> med : media) {
			if (media2.equals(med.get(media2.getMediaVersion())))
				return med;
		}
		return null;
	}

	/**
	 * sets the main media object to the resource without overriding the possibly existing media.
	 * @param mediaVersion version of the media
	 * @param media the media object
	 */
	public void addMedia(MediaVersion mediaVersion, EmbeddedMediaObject media) {
		addMedia(mediaVersion, media, false);
	}
	
	/**
	 * sets the main media object to the resource
	 * @param mediaVersion version of the media
	 * @param media the media object
	 * @param override if true overrides the existing media, otherwise it is added as a view (additional media information)
	 */
	public void addMedia(MediaVersion mediaVersion, EmbeddedMediaObject media, boolean override) {
		if (override || !this.media.get(0).containsKey(mediaVersion) || !this.media.get(0).get(mediaVersion).isEmpty()){
			media.setMediaVersion(mediaVersion);
			this.media.get(0).put(mediaVersion, media);
//			log.debug(mediaVersion+" overriten");
		} else {
			addMediaView(mediaVersion, media);
//			log.debug(this.media.get(0).toString());
//			log.debug(mediaVersion+" added as additional view");
		}
	}

	public ContextData getContextData() {
		return contextData;
	}

	public void setContextData(ContextData contextData) {
		this.contextData = contextData;
	}

	public Set<ObjectId> getAnnotationIds() {
		return annotationIds;
	}

	public void setAnnotationIds(Set<ObjectId> annotationIds) {
		this.annotationIds = annotationIds;
	}

	public List<Annotation> getAnnotations() {
		return annotations;
	}

	public void fillAnnotations() {
		//TODO: group further the annotations returned if needed
		this.annotations = DB.getAnnotationDAO().getByIds(this.annotationIds);
	}

	public ObjectNode getWithCreatorInfo() {
		if (administrative.getWithCreator() != null) {
			User u = DB.getUserDAO().getById(
					this.administrative.getWithCreator(),
					new ArrayList<String>(Arrays.asList("username",
							"firstName", "lastName")));
			ObjectNode withCreator = Json.newObject();
			withCreator.put("username", u.getUsername());
			withCreator.put("firstName", u.getFirstName());
			withCreator.put("lastName", u.getLastName());
			return withCreator;
		}
		else
			return null;
	}

	@JsonIgnore
	public User getWithCreator() {
		if (administrative.getWithCreator() != null)
			return DB.getUserDAO().getById(
					this.administrative.getWithCreator(),
					new ArrayList<String>(Arrays.asList("username",
							"firstName", "lastName")));
		else
			return null;
	}


	/* Elastic Transformations */

	/*
	 * Currently we are indexing only Resources that represent collected records
	 */
	public Map<String, Object> transformWR() {
		return ElasticUtils.basicTransformation(this);
	}


}
