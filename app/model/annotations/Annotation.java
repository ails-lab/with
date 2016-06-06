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

import model.annotations.selectors.SelectorType;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import utils.Deserializer;
import utils.Serializer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import model.annotations.bodies.AnnotationBodyTagging;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity("Annotation")
public class Annotation<T1 extends AnnotationBodyTagging> {
	
	
	@Id
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	ObjectId dbId;	
	
	//This is the annotationWithURI
	String annotationWithURI;

	@JsonSerialize(using = Serializer.DateSerializer.class)
	@JsonDeserialize(using = Deserializer.DateDeserializer.class)
	Date lastModified;

	ArrayList<AnnotationAdmin> annotators;
	
	public static enum MotivationType {
		Tagging, Linking, Commenting, Editing
	}
	
	MotivationType motivation;

	public static class AnnotationAdmin{
		@JsonSerialize(using = Serializer.DateSerializer.class)
		@JsonDeserialize(using = Deserializer.DateDeserializer.class)
		Date created;
		
		@JsonSerialize(using = Serializer.DateSerializer.class)
		@JsonDeserialize(using = Deserializer.DateDeserializer.class)
		Date generated;
		@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
		ObjectId withCreator; // a with user
		String generator;
		float confidence;
		
		public float getConfidence() {
			return confidence;
		}

		public void setConfidence(float confidence) {
			this.confidence = confidence;
		}

		public String getGenerator() {
			return generator;
		}

		public void setGenerator(String generator) {
			this.generator = generator;
		}
		
		public Date getGenerated() {
			return generated;
		}

		public void setGenerated(Date generated) {
			this.generated = generated;
		}
		
		
		public Date getCreated() {
			return created;
		}

		public void setCreated(Date created) {
			this.created = created;
		}
		
		
		public ObjectId getWithCreator() {
			return withCreator;
		}

		public void setWithCreator(ObjectId withCreator) {
			this.withCreator = withCreator;
		}

	}
	
		
	// base class what the annotation references
	public static class AnnotationTarget {

		String withURI;
		String externalId;
		
		@Id
		@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
		ObjectId dbId;	
		
		SelectorType selector;
		
		public ObjectId getDbId() {
			return dbId;
		}

		public void setDbId(ObjectId dbId) {
			this.dbId = dbId;
		}

		public String getWithURI() {
			return withURI;
		}

		public void setWithURI(String withURI) {
			this.withURI = withURI;
		}

		public String getExternalId() {
			return externalId;
		}

		public void setExternalId(String externalId) {
			this.externalId = externalId;
		}

		
	}
	
	@Embedded
	T1 body;
	@Embedded
	AnnotationTarget target;
	
	public ObjectId getDbId() {
		return dbId;
	}

	public void setDbId(ObjectId dbId) {
		this.dbId = dbId;
	}
	
	public String getAnnotationWithURI() {
		return annotationWithURI;
	}

	public void setAnnotationWithURI(String annotationWithURI) {
		this.annotationWithURI = annotationWithURI;
	}
	
	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
	
	public MotivationType getMotivation() {
		return motivation;
	}

	public void setMotivation(MotivationType motivation) {
		this.motivation = motivation;
	}
	
	public T1 getBody() {
		return body;
	}

	public void setBody(T1 body) {
		this.body = body;
	}

	public AnnotationTarget getTarget() {
		return target;
	}

	public void setTarget(AnnotationTarget target) {
		this.target = target;
	}
	
	public Annotation() {
		this.target = new AnnotationTarget();
		this.body = (T1) new AnnotationBodyTagging();
	}
	
}
