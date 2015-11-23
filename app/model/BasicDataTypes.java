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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import model.ExampleDataModels.LiteralOrResource;
import model.ExampleDataModels.WithPeriod;
import model.ExampleDataModels.CidocEvent.EventType;
import model.ExampleDataModels.LiteralOrResource.ResourceType;

public class BasicDataTypes {
	//TODO: why is the key a string and not an enum?
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
		
		/**
		 * Capture accurate and inaccurate dates in a visualisable way. Enable search for year.
		 * This is a point in time. If you mean a timespan, use different class.
		 */
		public static class WithDate {
			Date isoDate;
			int year;
			
			// controlled expression of an epoch "stone age", "renaissance", "16th century"
			LiteralOrResource epoch;
			
			// if the year is not accurate, give the inaccuracy here( 0- accurate)
			int approximation;
			
			// ontology based time 
			String uri;
			ResourceType uriType;
			
			// any expression that cannot fit into above
			String free;
		}
		
		/**
		 * 
		 * The WithTime might already cover the timespan you mean, but if you need more fields, its meant to be the 
		 * start of the timespan.
		 */
		public static class WithPeriod extends WithDate  {
			Date isoEndDate;
			int endYear;
		}
		
		public static class CidocEvent {
			public static enum EventType {
				CREATED, OTHER 
			}
			
			EventType eventType;
			WithPeriod timespan;
			ArrayList<LiteralOrResource> agent;
			ArrayList<LiteralOrResource> place;
		}
}
