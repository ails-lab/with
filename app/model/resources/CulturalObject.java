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
import java.util.List;
import java.util.Map;

import org.mongodb.morphia.annotations.Entity;

import model.basicDataTypes.CidocEvent;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.MultiLiteralOrResource;
import model.basicDataTypes.WithDate;
import model.resources.RecordResource.RecordDescriptiveData;

@Entity("RecordResource")
public class CulturalObject extends RecordResource<CulturalObject.CulturalObjectData>{

	public CulturalObject() {
		super();
		this.administrative = new RecordAdmin();
		this.resourceType = WithResourceType.valueOf(this.getClass().getSimpleName());
	}

	public static class CulturalObjectData extends RecordDescriptiveData {

		// provenance[0].recordId
		private MultiLiteralOrResource dcidentifier;

		// language of object, if it has one. Not related to the metadata.
		// eg. If the object is a book, its the language it is written in
		// no language for most paintings, vases, sculptures ... etc
		private MultiLiteral dclanguage;

		// Painting, Sculpture, Building, Book ....
		private MultiLiteralOrResource dctype;

		// places or times
		private MultiLiteralOrResource dccoverage;

		private MultiLiteralOrResource dcrights;

		// places are here
		private MultiLiteralOrResource dctermsspatial;

		private MultiLiteralOrResource dccreator;

		private MultiLiteralOrResource dccontributor;

		private List<WithDate> dccreated;
		private List<WithDate> dcdate;

		//TODO: do we want multilinguality for dcformat and dctermsmedium?
		private MultiLiteralOrResource dcformat;
		private MultiLiteralOrResource dctermsmedium;


		private MultiLiteralOrResource isRelatedTo;

		private List<CidocEvent> events;

		public MultiLiteralOrResource getDcidentifier() {
			return dcidentifier;
		}

		public void setDcidentifier(MultiLiteralOrResource dcidentifier) {
			this.dcidentifier = dcidentifier;
		}

		public MultiLiteral getDclanguage() {
			return dclanguage;
		}

		public void setDclanguage(MultiLiteral dclanguage) {
			this.dclanguage = dclanguage;
		}

		public MultiLiteralOrResource getDctype() {
			return dctype;
		}

		public void setDctype(MultiLiteralOrResource dctype) {
			this.dctype = dctype;
		}

		public MultiLiteralOrResource getDcrights() {
			return dcrights;
		}

		public void setDcrights(MultiLiteralOrResource dcrights) {
			this.dcrights = dcrights;
		}

		public MultiLiteralOrResource getDccoverage() {
			return dccoverage;
		}

		public void setDccoverage(MultiLiteralOrResource dccoverage) {
			this.dccoverage = dccoverage;
		}

		public MultiLiteralOrResource getDctermsspatial() {
			return dctermsspatial;
		}

		public void setDctermsspatial(MultiLiteralOrResource dcspatial) {
			this.dctermsspatial = dcspatial;
		}

		public MultiLiteralOrResource getDccreator() {
			return dccreator;
		}

		public void setDccreator(MultiLiteralOrResource dccreator) {
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

		public MultiLiteralOrResource getDcformat() {
			return dcformat;
		}

		public void setDcformat(MultiLiteralOrResource dcformat) {
			this.dcformat = dcformat;
		}

		public MultiLiteralOrResource getDctermsmedium() {
			return dctermsmedium;
		}

		public void setDctermsmedium(MultiLiteralOrResource dctermsmedium) {
			this.dctermsmedium = dctermsmedium;
		}

		public MultiLiteralOrResource getIsRelatedTo() {
			return isRelatedTo;
		}

		public void setIsRelatedTo(MultiLiteralOrResource isRelatedTo) {
			this.isRelatedTo = isRelatedTo;
		}

		public List<CidocEvent> getEvents() {
			return events;
		}

		public void setEvents(List<CidocEvent> events) {
			this.events = events;
		}

		public MultiLiteralOrResource getDccontributor() {
			return dccontributor;
		}

		public void setDccontributor(MultiLiteralOrResource dccontributor) {
			this.dccontributor = dccontributor;
		}

	}

	/* Elastic Transformations */

	/*
	 * Currently we are indexing only Resources that represent
	 * collected records
	 */
	@Override
	public Map<String, Object> transform() {

		Map<String, Object> idx_map = super.transform();

		idx_map.put("dccreator", ((CulturalObjectData)getDescriptiveData()).getDccreator());
		idx_map.put("dcidentifier", ((CulturalObjectData)getDescriptiveData()).getDcidentifier());
		idx_map.put("dclanguage", ((CulturalObjectData)getDescriptiveData()).getDclanguage());
		idx_map.put("dctype", ((CulturalObjectData)getDescriptiveData()).getDctype());
		idx_map.put("dccoverage", ((CulturalObjectData)getDescriptiveData()).getDccoverage());
		idx_map.put("dcrights", ((CulturalObjectData)getDescriptiveData()).getDcrights());
		idx_map.put("dctermsspatial", ((CulturalObjectData)getDescriptiveData()).getDctermsspatial());
		idx_map.put("dccontributor", ((CulturalObjectData)getDescriptiveData()).getDccontributor());
		idx_map.put("dcformat", ((CulturalObjectData)getDescriptiveData()).getDcformat());
		idx_map.put("dctermsmedium", ((CulturalObjectData)getDescriptiveData()).getDctermsmedium());
		idx_map.put("isRelatedTo", ((CulturalObjectData)getDescriptiveData()).getIsRelatedTo());
		idx_map.put("events", ((CulturalObjectData)getDescriptiveData()).getEvents());

		if(((CulturalObjectData)getDescriptiveData()).getDccreated() != null) {
			List<Integer> dccreated = new ArrayList<Integer>();
			((CulturalObjectData)getDescriptiveData()).getDccreated().forEach( (d) -> dccreated.add(d.getYear()));
			idx_map.put("dccreated", dccreated);
		}

		if(((CulturalObjectData)getDescriptiveData()).getDcdate() != null) {
			List<Integer> dcdate = new ArrayList<Integer>();
			((CulturalObjectData)getDescriptiveData()).getDcdate().forEach( (d) -> dcdate.add(d.getYear()));
			idx_map.put("dcdate", dcdate);
		}


		return idx_map;
	}

}
