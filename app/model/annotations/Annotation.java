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
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import model.annotations.bodies.AnnotationBody;
import model.annotations.bodies.AnnotationBodyCommenting;
import model.annotations.bodies.AnnotationBodyTagging;
import model.annotations.selectors.PropertyTextFragmentSelector;
import model.annotations.targets.AnnotationTarget;
import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.MultiLiteralOrResource;
import model.resources.CulturalObject.CulturalObjectData;
import model.resources.RecordResource;
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
import db.DB;
import org.apache.commons.text.similarity.LevenshteinDistance;

@SuppressWarnings("unchecked")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Indexes({ @Index(fields = @Field(value = "motivation", type = IndexType.ASC), options = @IndexOptions()),
//	@Index(fields = @Field(value = "body.uri", type = IndexType.ASC), options = @IndexOptions(disableValidation = true)),
		@Index(fields = @Field(value = "target.recordId", type = IndexType.ASC), options = @IndexOptions()),
		@Index(fields = @Field(value = "score.approvedBy", type = IndexType.ASC), options = @IndexOptions()), })

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
		Tagging, GeoTagging, Linking, Commenting, Editing, ColorTagging, Polling, ImageTagging, SubTagging
	}

	public static enum CreatorType {
		Person, Software
	}
	/**
	 * The motivation why this annotation has been created. This takes values from
	 * an enumerated list that currently includes Tagging, Linking, Commenting,
	 * Editing
	 */
	private MotivationType motivation;

	@Embedded
	private AnnotationScore score;
	
	private boolean publish ;
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
	private String externalId;

	private String scope;

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

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

	public boolean getPublish() {
		return publish;
	}

	public void setPublish(boolean publish) {
		this.publish = publish;
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

		private String externalCreatorId;
		private CreatorType externalCreatorType;
		private String externalCreatorName;


		private ArrayList<String> validationErrorType;
		private String validationComment;
		private String validationCorrection;

		private double confidence = -1;

		public ArrayList<String> getValidationErrorType() {
			return validationErrorType;
		}

		public void setValidationErrorType(ArrayList<String> validationErrorType) {
			this.validationErrorType = validationErrorType;
		}

		public String getValidationComment() {
			return validationComment;
		}

		public void setValidationComment(String validationComment) {
			this.validationComment = validationComment;
		}

		public String getValidationCorrection() {
			return validationCorrection;
		}

		public void setValidationCorrection(String validationCorrection) {
			this.validationCorrection = validationCorrection;
		}

		public String getExternalCreatorName() {
			return externalCreatorName;
		}

		public void setExternalCreatorName(String externalCreatorName) {
			this.externalCreatorName = externalCreatorName;
		}

		public String getExternalCreatorId() {
			return externalCreatorId;
		}

		public void setExternalCreatorId(String externalCreatorId) {
			this.externalCreatorId = externalCreatorId;
		}

		public Annotation.CreatorType getExternalCreatorType() {
			return externalCreatorType;
		}

		public void setExternalCreatorType(Annotation.CreatorType externalCreatorType) {
			this.externalCreatorType = externalCreatorType;
		}

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

		@Override
		public String toString() {
			return "AnnotationAdmin{" +
					"withCreator=" + withCreator +
					", generator='" + generator + '\'' +
					", created=" + created +
					", generated=" + generated +
					", lastModified=" + lastModified +
					", externalCreatorId='" + externalCreatorId + '\'' +
					", externalCreatorType=" + externalCreatorType +
					", externalCreatorName='" + externalCreatorName + '\'' +
					", confidence=" + confidence +
					'}';
		}
	}

	@JsonInclude(value = JsonInclude.Include.NON_NULL)
	@Embedded
	public static class AnnotationScore {

		/**
		 * An arrayList with the users who approved this annotation body.
		 */
		private ArrayList<AnnotationAdmin> approvedBy;

		/**
		 * An arrayList with the users who rejected this annotation body.
		 */
		private ArrayList<AnnotationAdmin> rejectedBy;

		/**
		 * An arrayList with the users who rated this annotation body.
		 */
		private ArrayList<AnnotationAdmin> ratedBy;

		/**
		 * An arrayList with the users who didn't comment on this annotation body.
		 */
		private ArrayList<AnnotationAdmin> dontKnowBy;

		public ArrayList<AnnotationAdmin> getRatedBy() {
			return ratedBy;
		}

		public void setRatedBy(ArrayList<AnnotationAdmin> ratedBy) {
			this.ratedBy = ratedBy;
		}

		public ArrayList<AnnotationAdmin> getApprovedBy() {
			return approvedBy;
		}

		public void setApprovedBy(ArrayList<AnnotationAdmin> approvedBy) {
			this.approvedBy = approvedBy;
		}

		public ArrayList<AnnotationAdmin> getRejectedBy() {
			return rejectedBy;
		}

		public void setRejectedBy(ArrayList<AnnotationAdmin> rejectedBy) {
			this.rejectedBy = rejectedBy;
		}

		public ArrayList<AnnotationAdmin> getDontKnowByBy() {
			return dontKnowBy;
		}

		public void setDontKnowBy(ArrayList<AnnotationAdmin> dontKnowBy) {
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

		((ObjectNode) json).remove("annotationWithURI");
		((ObjectNode) json.get("target")).remove("selector");
		try {
			JsonNode label = json.get("body").get("label").get("en");
			if (label == null) {
				label = json.get("body").get("label").get("default");
			}
			if (label != null && label.get(0) != null) {
				((ObjectNode) json).put("annlabel", label.get(0).asText());
			}
		} catch (NullPointerException e) {

		}

		return mapper.convertValue(json, Map.class);

	}

	@java.lang.Override
	public java.lang.String toString() {
		return "Annotation{" +
				"dbId=" + dbId +
				", annotationWithURI='" + annotationWithURI + '\'' +
				", annotators=" + annotators +
				", motivation=" + motivation +
				", score=" + score +
				", publish=" + publish +
				", body=" + body +
				", target=" + target +
				", externalId='" + externalId + '\'' +
				", scope='" + scope + '\'' +
				'}';
	}

	public ObjectNode toNtuaModel(String campaignName) throws Exception {
		ObjectMapper om = new ObjectMapper();
		ObjectNode result = om.createObjectNode();
		// result.put("id", this.dbId.toString());
		result.put("type", "Annotation");

		ArrayList<AnnotationAdmin> adminList = this.getAnnotators();
		AnnotationAdmin campaignRelevantAdmin = adminList.stream().filter(adm -> adm.getGenerator().equals("CrowdHeritage "+ campaignName)).findFirst().orElseThrow(()->new Exception());
		result.put("created", campaignRelevantAdmin.getCreated().toString());
		ObjectNode creator = om.createObjectNode();
		if (campaignRelevantAdmin.getExternalCreatorId() != null)
			creator.put("id", campaignRelevantAdmin.getExternalCreatorId());
		if (campaignRelevantAdmin.getExternalCreatorName() != null)
			creator.put("name", campaignRelevantAdmin.getExternalCreatorName());
		if (campaignRelevantAdmin.getExternalCreatorType() != null)
			creator.put("type", campaignRelevantAdmin.getExternalCreatorType().toString());
		result.set("creator", creator);
		if (campaignRelevantAdmin.getConfidence() != -1)
			result.put("confidence", campaignRelevantAdmin.getConfidence());
		
		AnnotationBody annBody = this.getBody();
		if (annBody instanceof AnnotationBodyCommenting) {
			AnnotationBodyCommenting annBodyCommenting = (AnnotationBodyCommenting) annBody;
			ObjectNode body = om.createObjectNode();
			body.put("type", "TextualBody");
			body.put("value", annBodyCommenting.getLabel().get(Language.DEFAULT).get(0));
			body.put("language", annBodyCommenting.getLabel().getLanguages().stream()
																.filter(lang -> lang.getName() != Language.DEFAULT.getName())
																.findFirst()
																.orElse(Language.DEFAULT).toString().toLowerCase());
			result.set("body", body);
		} else if (annBody instanceof AnnotationBodyTagging) {
			String uri = ((AnnotationBodyTagging) annBody).getUri();
			result.put("body", uri);
		}

		ObjectNode target = om.createObjectNode();
		AnnotationTarget tgt = this.getTarget();
		if ( tgt.getExternalId()!= null) {
			target.put("source", tgt.getExternalId());
		}
		else {
			if (tgt.getRecordId() != null) {
				String externalId = DB.getRecordResourceDAO().getById(tgt.getRecordId()).getAdministrative().getExternalId();
				target.put("source", externalId);
			}
			
		}
		if (tgt.getSelector() != null) {
			// right now when ingesting annotations, we use only PropertyTextFragmentSelector
			PropertyTextFragmentSelector slct = (PropertyTextFragmentSelector) tgt.getSelector();
			ObjectNode selector = om.createObjectNode();
			selector.put("type", "RDFPropertySelector");
			selector.put("property", slct.getProperty());
			if (slct.getOrigValue() != null && !slct.getOrigValue().isEmpty()) {
				ObjectNode destination = om.createObjectNode();
				destination.put("type", "Literal");
				destination.put("value", slct.getOrigValue());
				destination.put("language", slct.getOrigLang().toString().toLowerCase());
				selector.set("destination", destination);
			} 
			else {
				Language lang = slct.getOrigLang();
				String property = slct.getProperty();
				RecordResource r = DB.getRecordResourceDAO().getById(tgt.getRecordId());
				LevenshteinDistance distance = new LevenshteinDistance();
				String valueToCheck = slct.getPrefix() + slct.getAnnotatedValue() + slct.getSuffix();
				String closestMatch = null; 
    			int minDistance = Integer.MAX_VALUE; 

				try {

					if (property.equals("dc:title")) {
						for (String val : r.getDescriptiveData().getLabel().get(lang)) {
							int currentDistance = distance.apply(valueToCheck, val);
							if (currentDistance < minDistance) {
								minDistance = currentDistance;
								closestMatch = val;
							}
						}
					}
					else if (property.equals("dc:description")) {
						for (String val : r.getDescriptiveData().getDescription().get(lang)) {
							int currentDistance = distance.apply(valueToCheck, val);
							if (currentDistance < minDistance) {
								minDistance = currentDistance;
								closestMatch = val;
							}
						}				
					}
					else if (property.equals("dc:type")) {
						for (String val : ((CulturalObjectData) r.getDescriptiveData()).getDctype().get(lang)) {
							int currentDistance = distance.apply(valueToCheck, val);
							if (currentDistance < minDistance) {
								minDistance = currentDistance;
								closestMatch = val;
							}
						}                
					}
					else if (property.equals("dc:subject")) {
						for (String val : ((CulturalObjectData) r.getDescriptiveData()).getKeywords().get(lang)) {
							int currentDistance = distance.apply(valueToCheck, val);
							if (currentDistance < minDistance) {
								minDistance = currentDistance;
								closestMatch = val;
							}
						}  
					}

				}
				catch (Exception e) {
					closestMatch = "";
				}
				finally {
					ObjectNode destination = om.createObjectNode();
					destination.put("type", "Literal");
					destination.put("value", closestMatch);
					destination.put("language", slct.getOrigLang().toString().toLowerCase());
					selector.set("destination", destination);
				}	

			}

			if (slct.getStart() != 0 && slct.getEnd() != 0) {
				ObjectNode refinedBy = om.createObjectNode();
				refinedBy.put("type", "TextPositionSelector");
				refinedBy.put("start", slct.getStart());
				refinedBy.put("end", slct.getEnd());
				selector.set("refinedBy", refinedBy);
			}

			target.set("selector", selector);
		}
		result.set("target", target);
		
		if (this.getScope() != null)
			result.put("scope", this.getScope());
		
		if (this.getScore() != null) {
			AnnotationScore score = this.getScore();
			
			if (score.getApprovedBy() != null || score.getRejectedBy() != null) {
				int approvals = 0;
				int rejections = 0;
				if (score.getApprovedBy() != null)
					approvals = score.getApprovedBy().stream()
									.filter(a -> a.getGenerator().equals("CrowdHeritage "+campaignName))
									.collect(Collectors.toList())
									.size();
				if (score.getRejectedBy() != null)
					rejections = score.getRejectedBy().stream()
									.filter(a -> a.getGenerator().equals("CrowdHeritage "+campaignName))
									.collect(Collectors.toList())
									.size();
				int finalScore = approvals - rejections;
				ObjectNode review = om.createObjectNode();
				review.put("type", "Validation");
				review.put("upvotes", approvals);
				review.put("downvotes", rejections);
				if (finalScore > 0) {
					review.put("recommendation", "accept");
					result.set("review", review);
				}
				else if (finalScore < 0) {
					review.put("recommendation", "reject");
					result.set("review", review);
				}
			}
			// if we have ratings, calculate the median
			else if (score.getRatedBy() != null && score.getRatedBy().size() > 0 ) {
				ArrayList<Double> ratings = new ArrayList<>();
				score.getRatedBy().stream().forEach(rating -> {
					if (rating.getGenerator().equals("CrowdHeritage "+campaignName)) {
						ratings.add(rating.getConfidence());
					}
				});
				Collections.sort(ratings);
				double median = 0.0;
				if (ratings.size() % 2 == 1)
					median =  ratings.get((ratings.size() + 1) / 2 - 1);
				else {
					double lower = ratings.get(ratings.size() / 2 - 1);
					double upper = ratings.get(ratings.size() / 2);

					median = (lower + upper) / 2.0;
        		}
				ObjectNode review = om.createObjectNode();
				review.put("type", "Rating");
				review.put("score", median);
				result.set("review", review);
			}
		}	
		return result;
	}
	
}
