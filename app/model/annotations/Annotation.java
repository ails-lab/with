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

import java.util.Date;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import utils.Deserializer;
import utils.Serializer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity("Annotation")
public class Annotation<T1 extends Annotation.AnnotationBody, T2 extends Annotation.AnnotationTarget> {
	
	// marker base class for possible annotations
	public static class AnnotationBody {
	}
	
	// base class what the annotation references
	public static class AnnotationTarget {
	}
	
	@Id
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	ObjectId dbId;
	
	//what is this, smth like annotation/id?
	String withUrl;
	
	@JsonSerialize(using = Serializer.DateSerializer.class)
	@JsonDeserialize(using = Deserializer.DateDeserializer.class)
	Date created;
	
	@JsonSerialize(using = Serializer.DateSerializer.class)
	@JsonDeserialize(using = Deserializer.DateDeserializer.class)
	Date lastModified;

	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	ObjectId withCreator; // a with user
	
	public static enum AnnotationType {
		ExhibitionAnnotation, TextAnnotation
	}
	
	AnnotationType annotationType;
	// body and target depend on the annotation type
	
	@Embedded
	T1 body;
	@Embedded
	T2 target;
	
	public ObjectId getDbId() {
		return dbId;
	}

	public void setDbId(ObjectId dbId) {
		this.dbId = dbId;
	}
	
	public String getWithUrl() {
		return withUrl;
	}

	public void setWithUrl(String withUrl) {
		this.withUrl = withUrl;
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

	public AnnotationType getAnnotationType() {
		return annotationType;
	}

	public void setAnnotationType(AnnotationType annotationType) {
		this.annotationType = annotationType;
	}
	
	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
	
	public T1 getBody() {
		return body;
	}

	public void setBody(T1 body) {
		this.body = body;
	}

	public T2 getTarget() {
		return target;
	}

	public void setTarget(T2 target) {
		this.target = target;
	}

	
	public Annotation() {
		this.target = (T2) new AnnotationTarget();
		this.body = (T1) new AnnotationBody();
	}
	
	public Annotation(Class<?> clazz) {
		annotationType = AnnotationType.valueOf(clazz.getSimpleName());
	}
}
