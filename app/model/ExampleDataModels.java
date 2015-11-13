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
import java.util.Set;

import org.bson.types.ObjectId;

import model.Rights.Access;

public class ExampleDataModels {
	
	/**
	 * userids or group ids and how much access they have on the object
	 * @author Arne Stabenau
	 *
	 */
	public static class WithRights extends HashMap<ObjectId, Access> {
		
		public static enum Access {
			NONE, READ, WRITE, OWN
		}
		private boolean isPublic;
		
		public boolean isPublic() {
			return isPublic;
		}
		public void setPublic(boolean isPublic) {
			this.isPublic = isPublic;
		}
	}

	public static class Literal extends  HashMap<String, String> {
		// keys are language 2 letter codes, 
		// "unknown" for unknown language
		// special request for any is "any"
		public void setLiteral( String val, String lang ) {
			
		}
		
		/**
		 * Dont request the "unknown" language, request "any" if you don't care
 		 * @param lang
		 * @return
		 */
		public String getLiteral(String lang ) {
			if( "any".equals( lang )) {
				
			}
			return get( lang );
		}		
	}
	
	public static class LiteralOrResource extends Literal {
		// resources we do understand about and can process further (or not)
		// uri being general and difficult to process
		public static enum ResourceType {
			uri, skos, dbpedia, getty, wikidata, geodata, gemet, withRepository
		}
				
		// etc etc		
		public String getResource( ResourceType resourceType ) {
			if( get( "uriType").equals( resourceType.toString())) 
				return get( "uri");
			else 
				return "";
		}
		
		public void setResource( ResourceType resourceType, String uri ) {
			put( "uriType", resourceType.toString());
			put( "uri", uri );
		}
	}
	
	public static class Collection {
		WithRights rights;
		ObjectId dbID;
		LiteralOrResource title;
		LiteralOrResource description;
		
		ExternalCollection externalCollection;
	}
	
	public static class WithAdmin {
		
		ObjectId dbId;
		
		// uri that this resource has in the rdf repository
		String withURI;
		
		Date created;
		Date lastModified;
		
		ObjectId parentResourceId;
	}

	public static class Usage {
		// in hoe many favorites is it
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
	
	public static class CollectionInfo {
		ObjectId collectionId;
		int position;
	}
	
	public static class WithCollection extends ArrayList<CollectionInfo>{
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
	
	public static class EdmCollection extends ExternalCollection {}
	
	public static class ProvenanceInfo {
		String provider;
		String uri;
		String recordId;
		
		// you can have entries for WITH records with provider "WITH" and
		// recordId the ObjectId of the annotated Record
	}
	
	
	// this needs work
	public static enum Rights {
		ALLFREE, PAY, COMMERCIAL_REUSE, ATTRIBUTION, REMIX   
	}
	
	public static class WithTime {
		Date isoDate;
		int year;
		
		// controlled expression of an epoch "stone age", "renaissaunce"
		String epoch;
		
		// if the year is nmot there but you want to have timeline function,
		// give an approximate year where to put the record
		int approximateYear;
		
		// any expression that cannot fit into above
		String free;
	}
	
	public static class DescriptiveData {
		// one line content description with identifiable characteristic
		Literal title;
		
		// arbitrary length content description
		Literal description;
		
		// an indexers dream !! They can be literal concepts and enriched easily
		ArrayList<LiteralOrResource> keywords;
		
		// This are reachable URLs
		String isShownAt, isShownBy;
		
		// simple rights, not the original, the layman excerpt
		Set<Rights> rightsCategories;
		
		// The whole legal bla, unedited
		LiteralOrResource originalRights;
		
		// rdf  .. Agent, Artist, Painter, Painting, Series
		String rdfType;
		
		// URIs how this Resource is known elsewhere
		ArrayList<String> sameAs;		
	}
	
	public static class RecordData extends DescriptiveData {
		// about the creation of the thing
		Event created;		
	}
	
	public static class CulturalSimpifiedData extends RecordData {
		
	}
	
	public static class AgentData extends DescriptiveData {
		
	}
	
	public static class PlaceData extends DescriptiveData {
		// city, archeological site, area, nature reserve, historical site
	}
	
	public static class Event {
		WithTime time;
		ArrayList<LiteralOrResource> agent;
		ArrayList<LiteralOrResource> place;
	}
	
	public static class Resource {
		WithAdmin administrative;
		WithCollection collectedIn;
		Usage usage;
		
		ArrayList<ExternalCollection> externalCollections;		
		ArrayList<ProvenanceInfo> provenance;
	
		// enum of classes that are derived from DescriptiveData
		String resourceType;
		
		HashMap<String, DescriptiveData> model;
	}
	
	/**
	 * Goes into its own mongo collection
	 * @author Arne Stabenau
	 *
	 */
	public static class MediaObject {
		// image, sound, video, text
		// technical metadata
		// external or internal
		// access based on collection or public
		// filedata pointer
		
		// maybe rights object
	}
	
	
	
}
