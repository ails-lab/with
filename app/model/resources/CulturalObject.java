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

import java.util.List;

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
		this.administrative = new RecordAdmin();
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
		private List<Literal> dclanguage;
		
		// Painting, Sculpture, Building, Book .... 
		private List<LiteralOrResource> dctype;
		
		// places or times
		private List<LiteralOrResource> dccoverage;
		
		// places are here
		private List<LiteralOrResource> dcspatial;
		
		private List<LiteralOrResource> dccreator;

		private List<WithDate> dccreated;
		private List<WithDate> dcdate;
		
		//TODO: do we want multilinguality for dcformat and dctermsmedium?
		private List<LiteralOrResource> dcformat;
		private List<LiteralOrResource> dctermsmedium;
		
		private List<LiteralOrResource> isRelatedTo;
		
		private List<CidocEvent> events;

		public String getDcidentifier() {
			return dcidentifier;
		}

		public void setDcidentifier(String dcidentifier) {
			this.dcidentifier = dcidentifier;
		}

		public List<Literal> getDclanguage() {
			return dclanguage;
		}

		public void setDclanguage(List<Literal> dclanguage) {
			this.dclanguage = dclanguage;
		}

		public List<LiteralOrResource> getDctype() {
			return dctype;
		}

		public void setDctype(List<LiteralOrResource> dctype) {
			this.dctype = dctype;
		}

		public List<LiteralOrResource> getDccoverage() {
			return dccoverage;
		}

		public void setDccoverage(List<LiteralOrResource> dccoverage) {
			this.dccoverage = dccoverage;
		}

		public List<LiteralOrResource> getDcspatial() {
			return dcspatial;
		}

		public void setDcspatial(List<LiteralOrResource> dcspatial) {
			this.dcspatial = dcspatial;
		}

		public List<LiteralOrResource> getDccreator() {
			return dccreator;
		}

		public void setDccreator(List<LiteralOrResource> dccreator) {
			this.dccreator = dccreator;
		}

		public List<WithDate> getDccreated() {
			return dccreated;
		}

		public void setDccreated(List<WithDate> dccreated) {
			this.dccreated = dccreated;
		}

		public List<WithDate> getDcdate() {
			return dcdate;
		}

		public void setDcdate(List<WithDate> dcdate) {
			this.dcdate = dcdate;
		}

		public List<LiteralOrResource> getDcformat() {
			return dcformat;
		}

		public void setDcformat(List<LiteralOrResource> dcformat) {
			this.dcformat = dcformat;
		}

		public List<LiteralOrResource> getDctermsmedium() {
			return dctermsmedium;
		}

		public void setDctermsmedium(List<LiteralOrResource> dctermsmedium) {
			this.dctermsmedium = dctermsmedium;
		}

		public List<LiteralOrResource> getIsRelatedTo() {
			return isRelatedTo;
		}

		public void setIsRelatedTo(List<LiteralOrResource> isRelatedTo) {
			this.isRelatedTo = isRelatedTo;
		}

		public List<CidocEvent> getEvents() {
			return events;
		}

		public void setEvents(List<CidocEvent> events) {
			this.events = events;
		}
		
	}
	
}
