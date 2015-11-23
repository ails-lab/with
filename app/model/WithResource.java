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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.bson.types.ObjectId;

import model.BasicDataTypes.Literal;
import model.BasicDataTypes.LiteralOrResource;
import model.ExampleDataModels.WithAccess;
import model.annotations.Annotation;

public class WithResource<T extends DescriptiveData> {
	
	public static class CollectionInfo {
		ObjectId collectionId;
		int position;
	}
	
	public static class WithAdmin {
		
		ObjectId dbId;
		WithAccess access;
		
		// uri that this resource has in the rdf repository
		String withURI;
		
		Date created;
		Date lastModified;
		private final Map<ObjectId, WithAccess> underModeration = new HashMap<ObjectId, WithAccess>();
	}
	
	public static class Usage {
		// in how many favorites is it
		int likes;
		
		// in how many user collections is it
		int collected;
		
		// how many modified versions exist
		int annotated;
		
		// how often is it viewed, don't count count api calls,
		// count UI messages for viewing
		int viewCount;

		// implementation detail, put any tag on the record twice, 
		// with userID prepended and without. This will allow for people to look
		// for their own tags.
		ArrayList<String> tags;
	}
	
	/**
	 * If we know about collections from our sources, the info goes here
	 * For single records, fill in the position or next in sequence, for 
	 * general collection linking, omit it. 
	 *
	 */
	public static class ExternalCollection {
		// known sources only
		String source;
		
		String collectionUri;
		String nextInSequenceUri;
		int position;
		String title;
		String description;
	}
	
	
	public static class ProvenanceInfo {
		String provider;
		String uri;
		String recordId;
		
		// you can have entries for WITH records with provider "WITH" and
		// recordId the ObjectId of the annotated Record
	}

	
	WithAdmin administrative;
	ArrayList<CollectionInfo> collectedIn;
	
	Usage usage;
	
	//What is the 
	ArrayList<ExternalCollection> externalCollections;		
	ArrayList<ProvenanceInfo> provenance;

	// enum of classes that are derived from DescriptiveData
	String resourceType;
	
	T model;
	
	// All the available content serializations 
	// all keys in here should be understood by the WITH system
	HashMap<String, String> content;
	
	// all attached media Objects (their embedded part)
	ArrayList<EmbeddedMediaObject> media;
	
	// embedded for some or all, not sure
	// key is CollectionInfo.toString()
	HashMap<String, ContextAnnotation> contextAnnotation;
	
	ArrayList<Annotation> annotations;
	

}
