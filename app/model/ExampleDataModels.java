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
	public static class WithAccess extends HashMap<ObjectId, Access> {
		
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
	
	// just to have a separate class
	public static class Collection extends Resource {
	}
	
	public static class CollectionData extends DescriptiveData {
		
	}
	
	public static class RecordAdmin extends WithAdmin {
		// last entry of provenance chain hash of provider and recordId
		String externalId;
				
		// if this resource / record is derived (modified) from a different Record.
		ObjectId parentResourceId;
	}
	
	public static class CollectionAdmin extends WithAdmin {
		WithAccess access;
		int entryCount;
		boolean isExhibition;
	}
	
	
	public static class WithAdmin {
		
		ObjectId dbId;
		
		// uri that this resource has in the rdf repository
		String withURI;
		
		Date created;
		Date lastModified;
	}

	/**
	 * 
	 */
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
	
	/**
	 * This is how we link records into one or more collections
	 * @author Arne Stabenau
	 *
	 */
	public static class CollectionInfo {
		ObjectId collectionId;
		int position;
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
	
	/**
	 * Capture accurate and inaccurate dates in a visualisable way. Enable search for year.
	 * This is a point in time. If you mean a timespan, use different class.
	 */
	public static class WithTime {
		Date isoDate;
		int year;
		
		// controlled expression of an epoch "stone age", "renaissance", "16th century"
		String epoch;
		
		// if the year is not accurate, give the inaccuracy here( 0- accurate)
		int approximation;
		
		// ontology based time 
		String uri;
		String uriType;
		
		// any expression that cannot fit into above
		String free;
	}
	
	/**
	 * 
	 * The WithTime might already cover the timespan you mean, but if you need more fields, its meant to be the 
	 * start of the timespan.
	 */
	public static class WithTimespan extends WithTime  {
		Date isoEndDate;
		int endYear;
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
		
		
		// The whole legal bla, unedited, from the source, mostly cc0
		LiteralOrResource metadataRights;
		
		// rdf  .. Agent, Artist, Painter, Painting, Series
		String rdfType;
		
		// URIs how this Resource is known elsewhere
		ArrayList<String> sameAs;
		
		// in a timeline where would this resource appear
		int year;
		
		
	}
	
	
	public static class CulturalSimpifiedData extends DescriptiveData {
		
		// provenance[0].recordId
		String dcidentifier;
		
		// language of object, if it has one. Not related to the metadata.
		// eg. If the object is a book, its the language it is written in
		// no language for most paintings, vases, sculptures ... etc
		ArrayList<Literal> dclanguage;
		
		// Painting, Sculpture, Building, Book .... 
		ArrayList<LiteralOrResource> dctype;
		
		// places or times
		ArrayList<LiteralOrResource> dccoverage;
		
		// places are here
		ArrayList<LiteralOrResource> dcspatial;
		
		ArrayList<LiteralOrResource> dccreator;

		ArrayList<WithTime> dccreated;
		ArrayList<WithTime> dcdate;

		ArrayList<LiteralOrResource> dcformat;
		ArrayList<LiteralOrResource> dctermsmedium;
		
		ArrayList<LiteralOrResource> isRelatedTo;
		
		Event create;
	}

	
	
	public static class AgentData extends DescriptiveData {
		
	}
	
	public static class PlaceData extends DescriptiveData {
		// city, archeological site, area, nature reserve, historical site
	}
	
	public static class Event {
		public static enum EventType {
			CREATED, OTHER 
		}
		
		EventType eventType;
		WithTime time;
		ArrayList<LiteralOrResource> agent;
		ArrayList<LiteralOrResource> place;
	}
	
	public static class Resource {
		WithAdmin administrative;
		ArrayList<CollectionInfo> collectedIn;
		
		Usage usage;
		
		ArrayList<ExternalCollection> externalCollections;		
		ArrayList<ProvenanceInfo> provenance;
	
		// enum of classes that are derived from DescriptiveData
		String resourceType;
		
		// different model descriptive data. The keys need to be interoperable in the system
		// maybe not all descriptive datas should be subclasses of descriptive data?
		HashMap<String, DescriptiveData> model;
		
		// All the available content serializations 
		// all keys in here should be understood by the WITH system
		HashMap<String, String> content;
		
		// all attached media Objects (their embedded part)
		ArrayList<EmbeddedMediaObject> media;
	}
	
	
	public static class EmbeddedMediaObject {
		ObjectId dbId;

		public static enum Type {
			VIDEO, IMAGE, TEXT, AUDIO
		}

		Type type;
		Set<Rights> withRights;

		// if the thumbnail is externally provided
		String thumbnailUrl;

		// the media objects URL
		String url;
		
		// with urls for embedded or cached objects
		String withUrl;
		String withThumbnailUrl;
		
		public static enum MimeType {
			
		}
		
		LiteralOrResource originalRights;
		MimeType mimeType;
		
		public static enum Quality {
			UNKNOWN, IMAGE_SMALL, IMAGE_500k, IMAGE_1, IMAGE_4, VIDEO_SD, VIDEO_HD,
			AUDIO_8k, AUDIO_32k, AUDIO_256k, TEXT_IMAGE, TEXT_TEXT			
		}		
	}
	
	/**
	 * Goes into its own mongo collection. Extended characteristics on the media.
	 * @author Arne Stabenau
	 *
	 */
	public static class MediaObject extends EmbeddedMediaObject {		
		// which collection is this Media part of, this is the access rights restriction
		// if there is none, the media object is publicly available
		ArrayList<ObjectId> collection;
		
		int width, height;
		// external Url
		String url;
		
		double durationSeconds;
				
		byte[] thumbnailBytes;
		byte[] mediaBytes;
	}
	
	
	
}
