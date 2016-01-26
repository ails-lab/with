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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;

import play.libs.Json;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import model.DescriptiveData;
import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.basicDataTypes.Language;
import model.basicDataTypes.MultiLiteralOrResource;
import model.basicDataTypes.WithDate;
import model.resources.RecordResource.RecordDescriptiveData;

@Entity("CollectionObject")
public class CollectionObject extends WithResource<CollectionObject.CollectionDescriptiveData> {

	public CollectionObject() {
		super();
		this.administrative = new CollectionAdmin();
		this.resourceType = WithResourceType.valueOf(this.getClass()
				.getSimpleName());
	}

	public static class CollectionAdmin extends WithAdmin {

		private int entryCount = 0;
		private boolean isExhibition = false;

		public int getEntryCount() {
			return entryCount;
		}

		public void setEntryCount(int entryCount) {
			this.entryCount = entryCount;
		}

		public void incEntryCount() {
			this.entryCount++;
		}

		public boolean isExhibition() {
			return isExhibition;
		}

		public void setExhibition(boolean isExhibition) {
			this.isExhibition = isExhibition;
		}

	}

	@Embedded
	public static class CollectionDescriptiveData extends DescriptiveData {
		//TODO: change these to camelCase!
		// start day or possible start days
		private MultiLiteralOrResource dccreator;
		// for whom the resource is intended or useful
		private MultiLiteralOrResource dctermsaudience;
		// additional views of the timespan?
		private MultiLiteralOrResource dclanguage;

		// TODO: add link to external collection
		public MultiLiteralOrResource getDccreator() {
			return dccreator;
		}

		public void setDccreator(MultiLiteralOrResource dccreator) {
			this.dccreator = dccreator;
		}

		public MultiLiteralOrResource getDctermsaudience() {
			return dctermsaudience;
		}

		public void setDctermsaudience(
				MultiLiteralOrResource dctermsaudience) {
			this.dctermsaudience = dctermsaudience;
		}

		public MultiLiteralOrResource getDclanguage() {
			return dclanguage;
		}

		public void setDclanguage(MultiLiteralOrResource dclanguage) {
			this.dclanguage = dclanguage;
		}

	}

	
	/*
	 * Elastic transformations
	 */
	
	public Map<String, List<String>> lang_accumulators;

	/*
	 * Currently we are indexing only Resources that represent
	 * collected records
	 */
	public Map<String, Object> transformRR(CollectionObject rr) {

		JsonNode rr_json =  Json.toJson(rr);
		ObjectNode idx_doc = Json.newObject();

		lang_accumulators = new HashMap<String, List<String>>();

		/*
		 *  Label
		 */
		JsonNode label = rr_json.get("descriptiveData").get("label");
		idx_doc.put("label", label);
		if(label != null) {
			Iterator<Entry<String, JsonNode>> labels_it = label.fields();
			ArrayNode all_labels = Json.newObject().arrayNode();
			while(labels_it.hasNext()) {
				Entry<String, JsonNode> e = labels_it.next();
				// ignore "def" and "unknown" language
				if(e.getKey().equals(Language.DEF) || e.getKey().equals(Language.UNKNOWN))
					continue;
				all_labels.add(e.getValue().asText());
				addToLangAll(e.getKey(), e.getValue().asText());
				List<String> un = rr.getDescriptiveData().getLabel().get(Language.UNKNOWN);
				if(un != null) {
					for(String u: un)
						addToLangAll(e.getKey(), u);
				}
			}
			idx_doc.put("label_all", all_labels);


		}

		/*
		 * Description
		 */
		JsonNode description = rr_json.get("descriptiveData").get("description");
		idx_doc.put("description", description);
		if(description != null) {
			Iterator<Entry<String, JsonNode>> descs_it = description.fields();
			ArrayNode all_descs = Json.newObject().arrayNode();
			while(descs_it.hasNext()) {
				Entry<String, JsonNode> e = descs_it.next();
				// ignore "def" and "unknown" language
				if(e.getKey().equals(Language.DEF) || e.getKey().equals(Language.UNKNOWN))
					continue;
				all_descs.add(e.getValue().asText());
				addToLangAll(e.getKey(), e.getValue().asText());
				List<String> un = rr.getDescriptiveData().getDescription().get(Language.UNKNOWN);
				if(un != null) {
					for(String u: un)
						addToLangAll(e.getKey(), u);
				}
			}
			idx_doc.put("description_all", all_descs);
		}

		/*
		 * Keywords
		 */
		JsonNode keywords = rr_json.get("descriptiveData").get("keywords");
		idx_doc.put("keywords", keywords);
		if(keywords != null) {
			Iterator<Entry<String, JsonNode>> keywords_it = keywords.fields();
			ArrayNode all_keywords = Json.newObject().arrayNode();
			while(keywords_it.hasNext()) {
				Entry<String, JsonNode> e = keywords_it.next();
				// ignore "def" and "unknown" language
				if(e.getKey().equals(Language.DEF) || e.getKey().equals(Language.UNKNOWN))
					continue;
				all_keywords.add(e.getValue().asText());
				addToLangAll(e.getKey(), e.getValue().asText());
				List<String> un = rr.getDescriptiveData().getKeywords().get(Language.UNKNOWN);
				if(un != null) {
					for(String u: un)
						addToLangAll(e.getKey(), u);
				}
			}
			idx_doc.put("keywords_all", all_keywords);
		}

		/*
		 * AltLabels
		 */
		JsonNode altLabels = rr_json.get("descriptiveData").get("altLabels");
		idx_doc.put("altLabels", altLabels);
		if(altLabels != null) {
			Iterator<Entry<String, JsonNode>> altLabels_it = altLabels.fields();
			ArrayNode all_altLabels = Json.newObject().arrayNode();
			while(altLabels_it.hasNext()) {
				Entry<String, JsonNode> e = altLabels_it.next();
				// ignore "def" and "unknown" language
				if(e.getKey().equals(Language.DEF) || e.getKey().equals(Language.UNKNOWN))
					continue;
				all_altLabels.add(e.getValue().asText());
				addToLangAll(e.getKey(), e.getValue().asText());
				List<String> un = rr.getDescriptiveData().getAltLabels().get(Language.UNKNOWN);
				if(un != null) {
					for(String u: un)
						addToLangAll(e.getKey(), u);
				}
			}
			idx_doc.put("altLabels_all", all_altLabels);
		}

		for(Entry<String, List<String>> e: lang_accumulators.entrySet()) {
			JsonNode langs = Json.toJson(e.getValue());
			idx_doc.put("_all_" + e.getKey(), langs);
		}

		ArrayNode dates = Json.newObject().arrayNode();
		if(rr.getDescriptiveData().getDates() != null) {
			for(WithDate d: rr.getDescriptiveData().getDates()) {
				dates.add(d.getYear());
			}
			idx_doc.put("dates", dates);
		}

		/*
		 * User Rights structure in the document
		 */
		idx_doc.put("isPublic", rr.getAdministrative().getAccess().isPublic());
		idx_doc.put("access", Json.toJson(rr.getAdministrative().getAccess().getAcl()));

		
		/*
		 * Eliminate null values from the root document
		 */
		ObjectNode idx_copy = idx_doc.deepCopy();
		Iterator<String> fieldNames = idx_copy.fieldNames();
		while(fieldNames.hasNext()) {
			String fName = fieldNames.next();
			if(idx_copy.get(fName).isNull() ) {
				idx_doc.remove(fName);
			}
		}

		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);
		Map<String, Object> idx_map = mapper.convertValue(idx_doc, Map.class);
		
		return idx_map;
		
	}

	/*
	 * This method helps add a language specific value to
	 * the map containing the custom language specific
	 * _all fields
	 */
	private void addToLangAll(String lang, String value) {
		if(lang_accumulators.get(lang) != null) {
			lang_accumulators.get(lang).add(value);
		} else {
			List<String> lang_values = new ArrayList<String>();
			lang_values.add(value);
			lang_accumulators.put(lang, lang_values);
		}
	}


}
