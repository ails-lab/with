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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.basicDataTypes.WithAccess.AccessEntry;
import model.basicDataTypes.WithDate;
import model.resources.WithResource;
import model.resources.WithResource.WithAdmin;
import model.resources.WithResource.WithResourceType;
import model.DescriptiveData;
import model.resources.RecordResource;
import model.resources.RecordResource.RecordDescriptiveData;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHits;

import play.Logger;
import play.libs.Json;
import utils.Serializer;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class ElasticUtils {
	static private final Logger.ALogger log = Logger.of(ElasticUtils.class);

	/*
	 * Currently we are indexing only Resources that represent
	 * collected records
	 */
	public static <T extends DescriptiveData, A extends WithAdmin> Map<String, Object> basicTransformation(WithResource<T, A> rr) {

		JsonNode rr_json =  Json.toJson(rr);
		ObjectNode idx_doc = Json.newObject();

		Map<String, List<String>>lang_accumulators = new HashMap<String, List<String>>();

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
				for(String v: (List<String>)Json.fromJson(e.getValue(), List.class)) {
					all_labels.add(v);
				}
				for(String v: (List<String>)Json.fromJson(e.getValue(), List.class)) {
					addToLangAll(lang_accumulators, e.getKey(), v);
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
				for(String v: (List<String>)Json.fromJson(e.getValue(), List.class)) {
					all_descs.add(v);
				}
				for(String v: (List<String>)Json.fromJson(e.getValue(), List.class)) {
					addToLangAll(lang_accumulators, e.getKey(), v);
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
				for(String v: (List<String>)Json.fromJson(e.getValue(), List.class)) {
					all_keywords.add(v);
				}
				for(String v: (List<String>)Json.fromJson(e.getValue(), List.class)) {
					addToLangAll(lang_accumulators, e.getKey(), v);
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
				for(String v: (List<String>)Json.fromJson(e.getValue(), List.class)) {
					all_altLabels.add(v);
				}
				for(String v: (List<String>)Json.fromJson(e.getValue(), List.class)) {
					addToLangAll(lang_accumulators, e.getKey(), v);
				}
			}
			idx_doc.put("altLabels_all", all_altLabels);
		}

		List<String> unknownValues = null;
		for(Entry<String, List<String>> e: lang_accumulators.entrySet()) {
			if(e.getKey().toLowerCase().matches("un(.*)")) {
				unknownValues = e.getValue();
			}
		}
		for(Entry<String, List<String>> e: lang_accumulators.entrySet()) {
			if(e.getKey().toLowerCase().matches("un(.*)"))
				continue;

			if(unknownValues != null)
				e.getValue().addAll(unknownValues);
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
		idx_doc.put("isPublic", rr.getAdministrative().getAccess().getIsPublic());
		ObjectMapper aclMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addSerializer(AccessEntry.class, new Serializer.AccessEntrySerializer());
		aclMapper.registerModule(module);
		String aclString;
		try {
			aclString = aclMapper.writeValueAsString(rr.getAdministrative().getAccess().getAcl());
			idx_doc.put("access", aclMapper.readTree(aclString));
		} catch (JsonProcessingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}



		/*
		 * CollectedIn field
		 */
		//idx_doc.put("collectedIn", Json.toJson(rr.getCollectedIn()));

		/*
		 * Add the username and email of the creator
		 * NOT the dbId
		 */
		if(rr.getWithCreatorInfo() != null ) {
			idx_doc.put("creatorUsername", rr.getWithCreator().getUsername());
			idx_doc.put("creatorEmail", rr.getWithCreator().getEmail());
		}


		/*
		 * Add more fields to the Json
		 */
		idx_doc.put("metadataRights", Json.toJson(rr.getDescriptiveData().getMetadataRights()));
		//idx_doc.put("isExhibition", Json.toJson(rr.getAdministrative().));
		idx_doc.put("tags", Json.toJson(rr.getUsage().getTags()));
		idx_doc.put("isShownAt", Json.toJson(rr.getDescriptiveData().getIsShownAt()));
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
		 * TODO Add all Media Objects!!
		 */
		ArrayNode media_objects = Json.newObject().arrayNode();
		if( (rr.getMedia() != null) && !rr.getMedia().isEmpty()
				&& (rr.getMedia().get(0)!=null) && (!rr.getMedia().get(0).isEmpty())  ) {
			// take care about all EmbeddedMediaObjects
			ObjectNode media = Json.newObject();
			for(HashMap<MediaVersion, EmbeddedMediaObject> mediaobj: rr.getMedia()) {
				for(EmbeddedMediaObject emo: mediaobj.values()) {
					media.put("withRights", Json.toJson(emo.getWithRights()));
					//TODO: why withMediaType and not type, like in EmbeddedMedia? media.type
					media.put("type", Json.toJson(emo.getType()));
					media.put("originalRights", Json.toJson(emo.getOriginalRights()));
					//media.put("mimeType", Json.toJson(emo.getMimeType()));
					media.put("url", Json.toJson(emo.getUrl()));
					media.put("quality", Json.toJson(emo.getQuality()));
					media.put("width", emo.getWidth());
					media.put("height", emo.getHeight());
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
					media_objects.add(media);
				}
			}
			idx_doc.put("media", media_objects);
		}

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
	private static void addToLangAll(Map<String, List<String>> lang_acc, String lang, String value) {
		if(lang_acc.get(lang) != null) {
			lang_acc.get(lang).add(value);
		} else {
			List<String> lang_values = new ArrayList<String>();
			lang_values.add(value);
			lang_acc.put(lang, lang_values);
		}
	}

	/*
	 * Define the type of that instance
	 */
	public static <E> String defineInstanceOf(E doc) {

		String instanceName = doc.getClass().getSimpleName();
		List<String> enumNames = new ArrayList<String>();
		Arrays.asList(WithResourceType.values()).forEach( (t) -> {enumNames.add(t.toString()); return;} );
		if(enumNames.contains(instanceName)) {
			if(!instanceName.equalsIgnoreCase(WithResourceType.WithResource.toString()))
				return instanceName.toLowerCase();
			else
				return WithResourceType.RecordResource.toString().toLowerCase();
		} else
			return null;

	}

	/*
	 * Retrieve from DB the resources that where returned
	 * from an elastic query.
	 * Returns a Map of List of Resources per Type.
	 */

	public static Map<String, List<?>> getResourcesPerType(SearchResponse resp) {

		Map<String, List<ObjectId>> idsOfEachType = new HashMap<String, List<ObjectId>>();
		resp.getHits().forEach( (h) -> {
			if(!idsOfEachType.containsKey(h.getType())) {
				idsOfEachType.put(h.getType(), new ArrayList<ObjectId>() {{ add(new ObjectId(h.getId())); }});
			} else {
				idsOfEachType.get(h.getType()).add(new ObjectId(h.getId()));
			}
		});

		Map<String, List<?>> resourcesPerType = new HashMap<String, List<?>>();

		for(Entry<String, List<ObjectId>> e: idsOfEachType.entrySet()) {
			resourcesPerType.put(e.getKey() , DB.getRecordResourceDAO().getByIds(e.getValue()));

		}

		return resourcesPerType;
	}

}
