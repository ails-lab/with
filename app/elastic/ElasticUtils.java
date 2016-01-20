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


package elastic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import model.Collection;
import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.basicDataTypes.Language;
import model.basicDataTypes.WithDate;
import model.resources.RecordResource;
import model.resources.RecordResource.RecordDescriptiveData;

import org.bson.types.ObjectId;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import play.Logger;
import play.libs.Json;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ElasticUtils {
	static private final Logger.ALogger log = Logger.of(ElasticUtils.class);

	public static Map<String, List<String>> lang_accumulators;

	/*
	 * Currently we are indexing only Resources that represent
	 * collected records
	 */
	public static Map<String, Object> transformRR(RecordResource<RecordDescriptiveData> rr) {

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
		 * CollectedIn field
		 */
		idx_doc.put("collectedIn", Json.toJson(rr.getCollectedIn()));

		/*
		 * Add more fields to the Json
		 */
		idx_doc.put("metadataRights", Json.toJson(rr.getDescriptiveData().getMetadataRights()));
		idx_doc.put("withCreator", rr_json.get("administrative.withCreator"));
		if((rr.getProvenance() != null) && (rr.getProvenance().size() == 1)) {
			idx_doc.put("dataProvider", Json.toJson(rr.getProvenance().get(0).getProvider()));
		}
		if((rr.getProvenance() != null) && (rr.getProvenance().size() > 1)) {
			idx_doc.put("dataProvider", Json.toJson(rr.getProvenance().get(0).getProvider()));
			idx_doc.put("provider", Json.toJson(rr.getProvenance().get(1).getProvider()));
		}
		idx_doc.put("resourceType", rr_json.get("resourceType"));



		/*
		 * Format and add Media structure
		 */
		ArrayNode media_objects = Json.newObject().arrayNode();
		if(rr.getMedia() != null) {
			// take care about all EmbeddedMediaObjects
			EmbeddedMediaObject emo = rr.getMedia().get(0).get(MediaVersion.Original);
				ObjectNode media = Json.newObject();
				media.put("withRights", Json.toJson(emo.getWithRights()));
				media.put("withMediaType", Json.toJson(emo.getType()));
				media.put("originalRights", Json.toJson(emo.getOriginalRights()));
				media.put("mimeType", Json.toJson(emo.getMimeType()));
				media.put("quality", Json.toJson(emo.getQuality()));
				media.put("width", emo.getWidth());
				media.put("height", emo.getHeight());
				media_objects.add(media);

				/*
				 * Eliminate null values from Media json structures
				 */
				ObjectNode media_copy = media.deepCopy();
				Iterator<String> fieldNames = media_copy.fieldNames();
				while(fieldNames.hasNext()) {
					String fName = fieldNames.next();
					if(media_copy.get(fName).isNull() ) {
						media.remove(fName);
					}
				}
			idx_doc.put("media", media_objects);
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
	private static void addToLangAll(String lang, String value) {
		if(lang_accumulators.get(lang) != null) {
			lang_accumulators.get(lang).add(value);
		} else {
			List<String> lang_values = new ArrayList<String>();
			lang_values.add(value);
			lang_accumulators.put(lang, lang_values);
		}
	}





	/* *********************** OLD CODE ********************************** */

	public static RecordResource hitToRecord(SearchHit hit) {
		JsonNode json = Json.parse(hit.getSourceAsString());
		RecordResource record = Json.fromJson(json, RecordResource.class);
		/*if (hit.type().equals(Elastic.type_general)) {
			List<CollectionInfo> colIds = new ArrayList<CollectionInfo>();
			List<String> tags = new ArrayList<String>();
			ArrayNode ids = (ArrayNode) json.get("descriptiveData.collectedIn");
			ArrayNode allTags = (ArrayNode) json.get("tags");

			for (JsonNode id : ids)
				if (!colIds.contains(id.asText()))
					colIds.add(id.asText());
			for (JsonNode t : allTags)
				if (!tags.contains(t.asText()))
					tags.add(t.asText());

			record.setCollectedIn(colIds);
			record.setAllTags(tags);
		}*/

		return record;
	}

	public static Collection hitToCollection(SearchHit hit) {
		JsonNode json         = Json.parse(hit.getSourceAsString());
		JsonNode accessRights = json.get("rights");
		if(!accessRights.isMissingNode()) {
			ObjectNode ar = Json.newObject();
			for(JsonNode r: accessRights) {
				String user   = r.get("user").asText();
				String access = r.get("access").asText();
				ar.put(user, access);
			}
			((ObjectNode)json).remove("rights");
			((ObjectNode)json).put("rights", ar);
		}
		Collection collection = Json.fromJson(json, Collection.class);
		return collection;
	}

	public static List<Collection> getCollectionMetadaFromHit(
			SearchHits hits) {


		List<Collection> colFields = new ArrayList<Collection>();
		for(SearchHit hit: hits.getHits()) {
			Collection collection = hitToCollection(hit);
			collection.setDbId(new ObjectId(hit.getId()));
			colFields.add(collection);
		}
		return colFields;
	}
}
