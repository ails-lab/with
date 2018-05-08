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

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteral;
import model.resources.ThesaurusObject;
import model.resources.ThesaurusObject.SKOSSemantic;
import model.resources.ThesaurusObject.SKOSTerm;
import model.EmbeddedMediaObject.WithMediaRights;
import model.EmbeddedMediaObject.WithMediaType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import play.Logger;
import play.libs.Json;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.net.MediaType;

import db.DB;
import elastic.ElasticUtils;

public class SKOSObjectTest {

	public static void main(String[] args) {
		new SKOSObjectTest().genericTest();
	}
	
	@Test
	public void genericTest() {

		JsonNode json = Json.parse("{ \"administrative\": { \"externalId\": \"http://vocab.getty.edu/aat/300073692\", \"created\": { \"$date\": 1450700821484 } , \"lastModified\": { \"$date\": 1450700821484 } }, \"semantic\": { \"uri\": \"http://vocab.getty.edu/aat/300073692\", \"type\": \"http://www.w3.org/2004/02/skos/core#Collection\", \"prefLabel\": { \"en\": \"<balustrades, railings and their components>\", \"es\": \"<balaustradas, barandas y sus componentes>\", \"nl\": \"<balustrades, leuningen en hun onderdelen>\" }, \"members\": [ { \"uri\": \"http://vocab.getty.edu/aat/300001988\", \"type\": \"http://www.w3.org/2004/02/skos/core#Concept\", \"prefLabel\": { \"en\": \"barrier elements\", \"es\": \"elementos de barrera\", \"nl\": \"hekwerkelementen\" }, \"altLabel\": { \"en\": [ \"barrier element\", \"elements, barrier\" ] } }, { \"uri\": \"http://vocab.getty.edu/aat/300163880\", \"type\": \"http://www.w3.org/2004/02/skos/core#Concept\", \"prefLabel\": { \"en\": \"barriers and barrier elements\", \"es\": \"barreras y elementos de barrera\", \"nl\": \"hekwerken en hekwerkonderdelen\" } }, { \"uri\": \"http://vocab.getty.edu/aat/300000885\", \"type\": \"http://www.w3.org/2004/02/skos/core#Concept\", \"prefLabel\": { \"en\": \"architectural elements\", \"es\": \"elementos de arquitectura\", \"nl\": \"architecturale elementen\", \"zh\": \"建築元素\" }, \"altLabel\": { \"en\": [ \"architectural element\", \"elements, architectural\" ], \"es\": [ \"elemento de arquitectura\" ], \"nl\": [ \"architecturaal element\", \"bouwfragment\", \"bouwfragmenten\" ], \"zh\": [ \"jiàn zhú yuán sù\", \"jian zhu yuan su\", \"chien chu yüan su\" ] } }, { \"uri\": \"http://vocab.getty.edu/aat/300241584\", \"type\": \"http://www.w3.org/2004/02/skos/core#Collection\", \"prefLabel\": { \"en\": \"<components by specific context>\", \"es\": \"<componentes según contexto específico>\", \"nl\": \"<onderdelen naar specifieke context>\" }, \"altLabel\": { \"nl\": [ \"<onderdeel naar specifieke context>\" ] } }, { \"uri\": \"http://vocab.getty.edu/aat/300241583\", \"type\": \"http://www.w3.org/2004/02/skos/core#Concept\", \"prefLabel\": { \"en\": \"components (objects parts)\", \"es\": \"componentes\", \"nl\": \"onderdelen\" }, \"altLabel\": { \"en\": [ \"component (component object)\", \"parts (component objects)\" ], \"es\": [ \"componente\" ], \"nl\": [ \"onderdeel\" ] } }, { \"uri\": \"http://vocab.getty.edu/aat/300241490\", \"type\": \"http://www.w3.org/2004/02/skos/core#Collection\", \"prefLabel\": { \"en\": \"Components (hierarchy name)\", \"es\": \"componentes\", \"fr\": \"Composantes\", \"nl\": \"Onderdelen\" } }, { \"uri\": \"http://vocab.getty.edu/aat/300264092\", \"type\": \"http://www.w3.org/2004/02/skos/core#Collection\", \"prefLabel\": { \"en\": \"Objects Facet\", \"es\": \"faceta objetos\", \"nl\": \"Facet Objecten\" } }, { \"uri\": \"http://vocab.getty.edu/aat/300241508\", \"type\": \"http://www.w3.org/2004/02/skos/core#Collection\", \"prefLabel\": { \"en\": \"<object groupings by general context>\", \"es\": \"<grupos de objetos por contexto general>\", \"nl\": \"<objectgroepen naar algemene context>\" }, \"altLabel\": { \"nl\": [ \"<objectgroep naar algemene context>\" ] } }, { \"uri\": \"http://vocab.getty.edu/aat/300241507\", \"type\": \"http://www.w3.org/2004/02/skos/core#Concept\", \"prefLabel\": { \"en\": \"object groupings\", \"es\": \"grupos de objetos\", \"fr\": \"groupements d'objets\", \"nl\": \"objectgroepen\" }, \"altLabel\": { \"en\": [ \"object grouping\", \"groupings, object\" ], \"nl\": [ \"objectgroep\" ] } }, { \"uri\": \"http://vocab.getty.edu/aat/300241489\", \"type\": \"http://www.w3.org/2004/02/skos/core#Collection\", \"prefLabel\": { \"en\": \"Object Groupings and Systems (hierarchy name)\", \"es\": \"sistemas y grupos de objetos\", \"fr\": \"Ensembles d'objets et systèmes\", \"nl\": \"Objectgroepen en systemen\" } } ] } }");
		
//		JsonNode json = Json.parse("{ \"index\": 123 }");

//		System.out.println(json);

//		DB.getThesaurusDAO();
//		DB.getCollectionObjectDAO();
		
		ThesaurusObject to = DB.getThesaurusDAO().getByUri("http://vocab.getty.edu/aat/300179417");
		
		System.out.println(to.getSemantic().getPrefLabel());
		System.out.println(to.getSemantic().getAltLabel());
		
		transformTO(to);
		
		
//		ThesaurusObject so = new ThesaurusObject();
//		so.setAdministrative(new ThesaurusObject.SKOSAdmin(new Date(), new Date(), "test"));
//		Literal lit = new Literal();
//		lit.put("en", "english");
//		lit.put("de", "german");
//
//		MultiLiteral mlit = new MultiLiteral();
//		ArrayList<String> text = new ArrayList<>();
//		text.add("enlgish1");
//		text.add("enlgish2");
//		mlit.put("en", text);
//
//		SKOSSemantic sem = new SKOSSemantic();
//		sem.setUri("http://vocab.getty.edu/aat/300073692");
//		sem.setType("http://www.w3.org/2004/02/skos/core#Collection");
//		
//		Literal sempref = new Literal();
//		sempref.put("en", "<balustrades, railings and their components>");
//		sempref.put("es", "<balaustradas, barandas y sus componentes>");
//		
//		sem.setPrefLabel(sempref);
//
//		Literal narpref = new Literal();
//		narpref.put("en", "nar1");
//		narpref.put("es", "nar2");
//
//		List<SKOSTerm> semnar = new ArrayList<>();
//		semnar.add(new SKOSTerm("a1","b1",narpref,null));
//		semnar.add(new SKOSTerm("a2","b2",narpref,null));
//		
//		sem.setNarrower(semnar);
//		so.setSemantic(sem);
//		
//		
////		so.term = new SKOSTerm("uri1", "type1", lit, mlit);
//		
////		DB.getMorphia().getMapper().
//		
//		
//		
//		ObjectMapper mapper = new ObjectMapper();
////		mapper.
//		
//		try {
//			String s = mapper.writeValueAsString(so);
//			System.out.println(s);
//		} catch (JsonProcessingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
//		MediaObject mo = new MediaObject();
//		byte[] rawbytes = null;
//		URL url = null;
//		try {
//			url = new URL("http://www.ntua.gr/schools/ece.jpg");
//			File file = new File("test_java.txt");
//			ImageInputStream iis = ImageIO.createImageInputStream(file);
//			Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
//
//			if (readers.hasNext()) {
//
//                // pick the first available ImageReader
//                ImageReader reader = readers.next();
//
//                // attach source to the reader
//                reader.setInput(iis, true);
//
//                // read metadata of first image
//                IIOMetadata metadata = reader.getImageMetadata(0);
//
//                String[] names = metadata.getMetadataFormatNames();
//                int length = names.length;
//                for (int i = 0; i < length; i++) {
//                    System.out.println( "Format name: " + names[ i ] );
//                }
//            }
//
//			FileUtils.copyURLToFile(url, file);
//			FileInputStream fileStream = new FileInputStream(
//					file);
//
//			rawbytes = IOUtils.toByteArray(fileStream);
//		} catch(Exception e) {
//			System.out.println(e);
//			System.exit(-1);
//		}
//
//		mo.setMediaBytes(rawbytes);
//		mo.setMimeType(MediaType.ANY_IMAGE_TYPE);
//		mo.setHeight(875);
//		mo.setWidth(1230);
//		LiteralOrResource lor = new LiteralOrResource(ResourceType.uri, url.toString());
//		mo.setOriginalRights(lor);
//		HashSet<WithMediaRights> set = new HashSet<EmbeddedMediaObject.WithMediaRights>();
//		set.add(WithMediaRights.Creative);
//		mo.setWithRights(set);
//		mo.setType(WithMediaType.IMAGE);
//		mo.setUrl(url.toString());
//
//		try {
//			DB.getMediaObjectDAO().makePermanent(mo);
//			System.out.println("Media succesfully saved!");
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		MediaObject nmo = DB.getMediaObjectDAO().findById(mo.getDbId());
//		if(nmo!=null)
//			System.out.println("Media succesfuly retieved!");
//
//
//		DB.getMediaObjectDAO().deleteById(nmo.getDbId());
//		System.out.println("Succesfully deleted!");


	}
	
	public static Map<String, Object> transformTO(ThesaurusObject to) {

		JsonNode rr_json =  Json.toJson(to);
		ObjectNode idx_doc = Json.newObject();

		lang_accumulators = new HashMap<String, List<String>>();

		/*
		 *  Label
		 */
		JsonNode label = rr_json.get("semantic").get("prefLabel");
		
		idx_doc.put("prefLabel", label);
		
		if(label != null) {
			Iterator<Entry<String, JsonNode>> labels_it = label.fields();
			ArrayNode all_labels = Json.newObject().arrayNode();
			while(labels_it.hasNext()) {
				Entry<String, JsonNode> e = labels_it.next();
				
				// ignore "def" and "unknown" language
				if(e.getKey().equals(Language.DEFAULT) || e.getKey().equals(Language.UNKNOWN))
					continue;
				all_labels.add(e.getValue().asText());
				addToLangAll(e.getKey(), e.getValue().asText());
//				List<String> un = rr.getDescriptiveData().getLabel().get(Language.UNKNOWN);
				String un = to.getSemantic().getPrefLabel().get(Language.UNKNOWN);
				if(un != null) {
					addToLangAll(e.getKey(), un);
				}
			}
			idx_doc.put("prefLabel_all", all_labels);
			System.out.println(all_labels);
			System.out.println("LA " + lang_accumulators);

		}

		/*
		 * AltLabels
		 */
		JsonNode altLabels = rr_json.get("semantic").get("altLabel");
		idx_doc.put("altLabel", altLabels);

		if(altLabels != null) {
			Iterator<Entry<String, JsonNode>> altLabels_it = altLabels.fields();
			ArrayNode all_altLabels = Json.newObject().arrayNode();
			while(altLabels_it.hasNext()) {
				Entry<String, JsonNode> e = altLabels_it.next();
				
//				System.out.println(">> " + e + " // " + e.getValue().asText());
				// ignore "def" and "unknown" language
				if(e.getKey().equals(Language.DEFAULT) || e.getKey().equals(Language.UNKNOWN))
					continue;
				all_altLabels.add(e.getValue().asText());
				addToLangAll(e.getKey(), e.getValue().asText());
				List<String> un = to.getSemantic().getAltLabel().get(Language.UNKNOWN);
				if(un != null) {
					for(String u: un)
						addToLangAll(e.getKey(), u);
				}
			}
			idx_doc.put("altLabel_all", all_altLabels);
			System.out.println(all_altLabels);
		}

		for(Entry<String, List<String>> e: lang_accumulators.entrySet()) {
			JsonNode langs = Json.toJson(e.getValue());
			idx_doc.put("_all_" + e.getKey(), langs);
			System.out.println("TT_all_" + e.getKey() + " --- " + langs);
		}

//		ArrayNode dates = Json.newObject().arrayNode();
//		if(rr.getDescriptiveData().getDates() != null) {
//			for(WithDate d: rr.getDescriptiveData().getDates()) {
//				dates.add(d.getYear());
//			}
//			idx_doc.put("dates", dates);
//		}
//
//		/*
//		 * CollectedIn field
//		 */
//		idx_doc.put("collectedIn", Json.toJson(rr.getCollectedIn()));
//
//		/*
//		 * Add more fields to the Json
//		 */
//		idx_doc.put("metadataRights", Json.toJson(rr.getDescriptiveData().getMetadataRights()));
//		idx_doc.put("withCreator", rr_json.get("administrative.withCreator"));
//		if((rr.getProvenance() != null) && (rr.getProvenance().size() == 1)) {
//			idx_doc.put("dataProvider", Json.toJson(rr.getProvenance().get(0).getProvider()));
//		}
//		if((rr.getProvenance() != null) && (rr.getProvenance().size() > 1)) {
//			idx_doc.put("dataProvider", Json.toJson(rr.getProvenance().get(0).getProvider()));
//			idx_doc.put("provider", Json.toJson(rr.getProvenance().get(1).getProvider()));
//		}
//		idx_doc.put("resourceType", rr_json.get("resourceType"));
//
//
//
//		/*
//		 * Format and add Media structure
//		 */
//		ArrayNode media_objects = Json.newObject().arrayNode();
//		if(rr.getMedia() != null) {
//			// take care about all EmbeddedMediaObjects
//			EmbeddedMediaObject emo = rr.getMedia().get(0).get(MediaVersion.Original);
//				ObjectNode media = Json.newObject();
//				media.put("withRights", Json.toJson(emo.getWithRights()));
//				media.put("withMediaType", Json.toJson(emo.getType()));
//				media.put("originalRights", Json.toJson(emo.getOriginalRights()));
//				media.put("mimeType", Json.toJson(emo.getMimeType()));
//				media.put("quality", Json.toJson(emo.getQuality()));
//				media.put("width", emo.getWidth());
//				media.put("height", emo.getHeight());
//				media_objects.add(media);
//
//				/*
//				 * Eliminate null values from Media json structures
//				 */
//				ObjectNode media_copy = media.deepCopy();
//				Iterator<String> fieldNames = media_copy.fieldNames();
//				while(fieldNames.hasNext()) {
//					String fName = fieldNames.next();
//					if(media_copy.get(fName).isNull() ) {
//						media.remove(fName);
//					}
//				}
//			idx_doc.put("media", media_objects);
//		}
//
//		/*
//		 * User Rights structure in the document
//		 */
//		idx_doc.put("isPublic", rr.getAdministrative().getAccess().isPublic());
//		idx_doc.put("access", Json.toJson(rr.getAdministrative().getAccess().getAcl()));

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

	static private final Logger.ALogger log = Logger.of(ElasticUtils.class);

	public static Map<String, List<String>> lang_accumulators;

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

}
