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
import model.annotations.targets.AnnotationTarget;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity("Annotation")
public class Annotation<T1 extends AnnotationBodyTagging> {
	
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

	public ArrayList<T1> getBodies() {
		return bodies;
	}

	public void setBodies(ArrayList<T1> bodies) {
		this.bodies = bodies;
	}

	public ArrayList<AnnotationTarget> getTargets() {
		return targets;
	}

	public void setTargets(ArrayList<AnnotationTarget> targets) {
		this.targets = targets;
	}

	/**
	 * The dbIdentfier for retrieving this annotation from Mongo.
	 */
	@Id
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	ObjectId dbId;	
	
	/**
	 * The URI of the annotation - this should normally result to the JSON representation of the annotation.
	 */
	String annotationWithURI;
	
	/**
	 * The with user who created this annotation.
	 */
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	ObjectId withCreator; // a with user
	
	/**
	 * The tool used for generating this annotation 
	 */
	String generator;
	
	/**
	 * The date this annotation has been created.
	 */
	@JsonSerialize(using = Serializer.DateSerializer.class)
	@JsonDeserialize(using = Deserializer.DateDeserializer.class)
	Date created;
	
	/**
	 * The date this annotation has been created.
	 */
	@JsonSerialize(using = Serializer.DateSerializer.class)
	@JsonDeserialize(using = Deserializer.DateDeserializer.class)
	Date generated;
	
	/**
	 * The date this annotation has been last modified.
	 */
	@JsonSerialize(using = Serializer.DateSerializer.class)
	@JsonDeserialize(using = Deserializer.DateDeserializer.class)
	Date lastModified;
	
	/**
	 * The motivation why this annotation has been created. This takes values from an enumerated
	 * list that currently includes Tagging, Linking, Commenting, Editing
	 */
	MotivationType motivation;
	
	/**
	 * The list of bodies that include the annotation details.
	 */
	@Embedded
	ArrayList<T1> bodies;
	
	/**
	 * The list of targets to which the bodies refer to.
	 */
	@Embedded
	ArrayList<AnnotationTarget> targets;
	
	/**
	 * @author nsimou
	 * This enumeration included the motivation types for an annotation. It currently includes
	 * includes Tagging, Linking, Commenting, Editing
	 */
	public static enum MotivationType {
		Tagging, Linking, Commenting, Editing
	}
	
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
	
}
