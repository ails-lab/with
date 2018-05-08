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


package model.annotations;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import model.annotations.bodies.AnnotationBody;
import model.annotations.targets.AnnotationTarget;
import model.basicDataTypes.Literal;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.MultiLiteralOrResource;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;

import utils.Deserializer;
import utils.Serializer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SuppressWarnings("unchecked")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Indexes({
	@Index(fields = @Field(value = "motivation", type = IndexType.ASC), options = @IndexOptions()),
//	@Index(fields = @Field(value = "body.uri", type = IndexType.ASC), options = @IndexOptions(disableValidation = true)),
	@Index(fields = @Field(value = "target.recordId", type = IndexType.ASC), options = @IndexOptions()),
	@Index(fields = @Field(value = "score.approvedBy", type = IndexType.ASC), options = @IndexOptions()),
})

@Entity("Annotation")
public class Annotation<T extends AnnotationBody> {

	public Annotation() {
		this.target = new AnnotationTarget();
		this.body = (T) new AnnotationBody();
	}

	/**
	 * The dbIdentfier for retrieving this annotation from Mongo.
	 */
	@Id
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId dbId;

	/**
	 * The URI of the annotation - this should normally result to the JSON
	 * representation of the annotation.
	 */
	private String annotationWithURI;

	/**
	 * Administrative data about the annotation creation
	 */
	private ArrayList<AnnotationAdmin> annotators;

	/**
	 * This enumeration included the motivation types for an annotation. It
	 * currently includes includes Tagging, Linking, Commenting, Editing
	 */
	public static enum MotivationType {
		Tagging, GeoTagging, Linking, Commenting, Editing
	}

	/**
	 * The motivation why this annotation has been created. This takes values
	 * from an enumerated list that currently includes Tagging, Linking,
	 * Commenting, Editing
	 */
	private MotivationType motivation;
	
	@Embedded
	private AnnotationScore score;

	/**
	 * The body that includes the annotation details.
	 */
	@Embedded
	private T body;

	/**
	 * The target to which the body refer to.
	 */
	@Embedded
	private AnnotationTarget target;

	public ObjectId getDbId() {
		return dbId;
	}

	public void setDbId(ObjectId dbId) {
		this.dbId = dbId;
	}

	public T getBody() {
		return body;
	}

	public void setBody(T body) {
		this.body = body;
	}

	public AnnotationTarget getTarget() {
		return target;
	}

	public void setTarget(AnnotationTarget target) {
		this.target = target;
	}

	public String getAnnotationWithURI() {
		return annotationWithURI;
	}

	public void setAnnotationWithURI(String annotationWithURI) {
		this.annotationWithURI = annotationWithURI;
	}

	public MotivationType getMotivation() {
		return motivation;
	}

	public void setMotivation(MotivationType motivation) {
		this.motivation = motivation;
	}

	public AnnotationScore getScore() {
		return score;
	}

	public void setScore(AnnotationScore score) {
		this.score = score;
	}

	public ArrayList<AnnotationAdmin> getAnnotators() {
		return annotators;
	}

	public void setAnnotators(ArrayList<AnnotationAdmin> annotators) {
		this.annotators = annotators;
	}

	@JsonInclude(value = JsonInclude.Include.NON_NULL)
	public static class AnnotationAdmin {
		/**
		 * The with user who created this annotation.
		 */
		@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
		private ObjectId withCreator; // a with user

		/**
		 * The tool used for generating this annotation
		 */
		private String generator;

		/**
		 * The date this annotation has been created.
		 */
		@JsonSerialize(using = Serializer.DateSerializer.class)
		@JsonDeserialize(using = Deserializer.DateDeserializer.class)
		private Date created;

		/**
		 * The date this annotation has been created.
		 */
		@JsonSerialize(using = Serializer.DateSerializer.class)
		@JsonDeserialize(using = Deserializer.DateDeserializer.class)
		private Date generated;

		/**
		 * The date this annotation has been last modified.
		 */
		@JsonSerialize(using = Serializer.DateSerializer.class)
		@JsonDeserialize(using = Deserializer.DateDeserializer.class)
		private Date lastModified;

		private double confidence;

		public ObjectId getWithCreator() {
			return withCreator;
		}

		public void setWithCreator(ObjectId withCreator) {
			this.withCreator = withCreator;
		}

		public String getGenerator() {
			return generator;
		}

		public void setGenerator(String generator) {
			this.generator = generator;
		}

		public Date getCreated() {
			return created;
		}

		public void setCreated(Date created) {
			this.created = created;
		}

		public Date getGenerated() {
			return generated;
		}

		public void setGenerated(Date generated) {
			this.generated = generated;
		}

		public Date getLastModified() {
			return lastModified;
		}

		public void setLastModified(Date lastModified) {
			this.lastModified = lastModified;
		}

		public double getConfidence() {
			return confidence;
		}

		public void setConfidence(double confidence) {
			this.confidence = confidence;
		}

	}

	@JsonInclude(value = JsonInclude.Include.NON_NULL)
	@Embedded
	public static class AnnotationScore {

		/**
		 * An arrayList with the user ids who approved this annotation body.
		 */
		@JsonSerialize(using = Serializer.ObjectIdArraySerializer.class)
		private ArrayList<ObjectId> approvedBy;

		/**
		 * An arrayList with the user ids who rejected this annotation body.
		 */
		@JsonSerialize(using = Serializer.ObjectIdArraySerializer.class)
		private ArrayList<ObjectId> rejectedBy;

		/**
		 * An arrayList with the user ids who didn't comment on this annotation
		 * body.
		 */
		@JsonSerialize(using = Serializer.ObjectIdArraySerializer.class)
		private ArrayList<ObjectId> dontKnowBy;

		public ArrayList<ObjectId> getApprovedBy() {
			return approvedBy;
		}

		public void setApprovedBy(ArrayList<ObjectId> approvedBy) {
			this.approvedBy = approvedBy;
		}

		public ArrayList<ObjectId> getRejectedBy() {
			return rejectedBy;
		}

		public void setRejectedBy(ArrayList<ObjectId> rejectedBy) {
			this.rejectedBy = rejectedBy;
		}

		public ArrayList<ObjectId> getDontKnowByBy() {
			return dontKnowBy;
		}

		public void setDontKnowBy(ArrayList<ObjectId> dontKnowBy) {
			this.dontKnowBy = dontKnowBy;
		}
	}

	public Map<String, Object> transform() {

		ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addSerializer(MultiLiteral.class, new Serializer.MUltiliteralSerializerForElastic());
		module.addSerializer(MultiLiteralOrResource.class, new Serializer.MUltiliteralSerializerForElastic());
		module.addSerializer(Literal.class, new Serializer.LiteralSerializerForElastic());
		mapper.registerModule(module);
		mapper.setSerializationInclusion(Include.NON_NULL);

		JsonNode json = mapper.valueToTree(this);

		((ObjectNode)json).remove("annotationWithURI");
		((ObjectNode)json.get("target")).remove("selector");

//		((ObjectNode)json).remove("narrower");
//		((ObjectNode)json).remove("topConcepts");
//		((ObjectNode)json).remove("members");
//		
		
		return mapper.convertValue(json, Map.class);
	}

}
