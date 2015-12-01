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

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;

import model.basicDataTypes.CidocEvent;
import model.basicDataTypes.Literal;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.WithDate;
import model.resources.RecordResource.RecordDescriptiveData;
import model.resources.WithResource.WithResourceType;

@Entity("RecordResource")
public class CulturalObject extends RecordResource<CulturalObject.CulturalObjectData>{
	
	public CulturalObject() {
		super();
		this.resourceType = WithResourceType.valueOf(this.getClass().getSimpleName());
	}
	
	public static class RecordAdmin extends WithAdmin {
		
		// last entry of provenance chain hash of provider and recordId
		private String externalId;
				
		// if this resource / record is derived (modified) from a different Record.
		private ObjectId parentResourceId;

		public String getExternalId() {
			return externalId;
		}

		public void setExternalId(String externalId) {
			this.externalId = externalId;
		}

		public ObjectId getParentResourceId() {
			return parentResourceId;
		}

		public void setParentResourceId(ObjectId parentResourceId) {
			this.parentResourceId = parentResourceId;
		}
		
	}
	
	
	public static class CulturalObjectData extends RecordDescriptiveData {

		// provenance[0].recordId
		private String dcidentifier;
		
		// language of object, if it has one. Not related to the metadata.
		// eg. If the object is a book, its the language it is written in
		// no language for most paintings, vases, sculptures ... etc
		private ArrayList<Literal> dclanguage;
		
		// Painting, Sculpture, Building, Book .... 
		private ArrayList<LiteralOrResource> dctype;
		
		// places or times
		private ArrayList<LiteralOrResource> dccoverage;
		
		// places are here
		private ArrayList<LiteralOrResource> dcspatial;
		
		private ArrayList<LiteralOrResource> dccreator;

		private ArrayList<WithDate> dccreated;
		private ArrayList<WithDate> dcdate;
		
		//TODO: do we want multilinguality for dcformat and dctermsmedium?
		private ArrayList<LiteralOrResource> dcformat;
		private ArrayList<LiteralOrResource> dctermsmedium;
		
		private ArrayList<LiteralOrResource> isRelatedTo;
		
		private ArrayList<CidocEvent> events;

		public String getDcidentifier() {
			return dcidentifier;
		}

		public void setDcidentifier(String dcidentifier) {
			this.dcidentifier = dcidentifier;
		}

		public ArrayList<Literal> getDclanguage() {
			return dclanguage;
		}

		public void setDclanguage(ArrayList<Literal> dclanguage) {
			this.dclanguage = dclanguage;
		}

		public ArrayList<LiteralOrResource> getDctype() {
			return dctype;
		}

		public void setDctype(ArrayList<LiteralOrResource> dctype) {
			this.dctype = dctype;
		}

		public ArrayList<LiteralOrResource> getDccoverage() {
			return dccoverage;
		}

		public void setDccoverage(ArrayList<LiteralOrResource> dccoverage) {
			this.dccoverage = dccoverage;
		}

		public ArrayList<LiteralOrResource> getDcspatial() {
			return dcspatial;
		}

		public void setDcspatial(ArrayList<LiteralOrResource> dcspatial) {
			this.dcspatial = dcspatial;
		}

		public ArrayList<LiteralOrResource> getDccreator() {
			return dccreator;
		}

		public void setDccreator(ArrayList<LiteralOrResource> dccreator) {
			this.dccreator = dccreator;
		}

		public ArrayList<WithDate> getDccreated() {
			return dccreated;
		}

		public void setDccreated(ArrayList<WithDate> dccreated) {
			this.dccreated = dccreated;
		}

		public ArrayList<WithDate> getDcdate() {
			return dcdate;
		}

		public void setDcdate(ArrayList<WithDate> dcdate) {
			this.dcdate = dcdate;
		}

		public ArrayList<LiteralOrResource> getDcformat() {
			return dcformat;
		}

		public void setDcformat(ArrayList<LiteralOrResource> dcformat) {
			this.dcformat = dcformat;
		}

		public ArrayList<LiteralOrResource> getDctermsmedium() {
			return dctermsmedium;
		}

		public void setDctermsmedium(ArrayList<LiteralOrResource> dctermsmedium) {
			this.dctermsmedium = dctermsmedium;
		}

		public ArrayList<LiteralOrResource> getIsRelatedTo() {
			return isRelatedTo;
		}

		public void setIsRelatedTo(ArrayList<LiteralOrResource> isRelatedTo) {
			this.isRelatedTo = isRelatedTo;
		}

		public ArrayList<CidocEvent> getEvents() {
			return events;
		}

		public void setEvents(ArrayList<CidocEvent> events) {
			this.events = events;
		}
		
	}
	
}
