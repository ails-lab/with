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


package controllers;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.DescriptiveData;
import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.WithMediaType;
import model.MediaObject;
import model.annotations.ContextData;
import model.annotations.ContextData.ContextDataBody;
import model.annotations.ContextData.ContextDataTarget;
import model.annotations.ContextData.ContextDataType;
import model.annotations.bodies.AnnotationBodyTagging;
import model.basicDataTypes.Language;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.ProvenanceInfo;
import model.resources.CulturalObject.CulturalObjectData;
import model.resources.RecordResource;
import model.resources.ThesaurusObject;
import model.resources.WithResourceType;
import model.resources.collection.CollectionObject;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Option;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import sources.core.ISpaceSource;
import sources.core.ParallelAPICall;
import sources.core.ParallelAPICall.Priority;
import sources.core.RecordJSONMetadata;
//import utils.AccessManager;
//import utils.AccessManager.Action;
import utils.Locks;

import com.aliasi.dict.MapDictionary;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;
import db.WithResourceDAO;
import elastic.Elastic;
import elastic.ElasticSearcher;
import elastic.ElasticSearcher.SearchOptions;

/**
 * @author mariaral
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ThesaurusController extends Controller {

	public static final ALogger log = Logger.of(ThesaurusController.class);

	public static Result addThesaurusTerm() {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();

		try {
			if (json == null) {
				result.put("error", "Invalid JSON");
				return badRequest(result);
			} else {
				if (addThesaurusTerm(json)) {
					result.put("message", "Thesaurus term succesfully added");
				} else {
					result.put("error", "Invalid term json");
				}
			}
			return ok(result);
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}
	
	public static boolean addThesaurusTerm(JsonNode json) throws ClassNotFoundException {
		
		Class<?> clazz = Class.forName("model.resources.ThesaurusObject");
		
		ThesaurusObject record = (ThesaurusObject) Json.fromJson(json, clazz);
		String uri = record.getSemantic().getUri();
		
		if (uri == null) {
			return false;
		}
		
		ObjectId recordId;
		
		if (DB.getThesaurusDAO().existsWithExternalId(uri)) {
			ThesaurusObject resource = DB.getThesaurusDAO()
					.getUniqueByFieldAndValue("administrative.externalId",
							uri,
							new ArrayList<String>(Arrays.asList("_id")));
			DB.getThesaurusDAO().editRecord("semantic", resource.getDbId(), json.get("semantic"));

		} else {
			record.getAdministrative().setCreated(new Date());
			DB.getThesaurusDAO().makePermanent(record);
			recordId = record.getDbId();
			DB.getThesaurusDAO().updateField(recordId, "administrative.externalId", uri);
		}
		
		
		return true;
	}

	public static Result addThesaurusTerms() {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		try {
			if (json == null || !json.isArray()) {
				result.put("error", "Invalid JSON");
				return badRequest(result);
			} else {
				int count = 0;
				int total = 0;
				Iterator<JsonNode> iterator = json.iterator();
				while (iterator.hasNext()) {
					JsonNode recordJson = iterator.next();
					if (addThesaurusTerm(recordJson)) {
						count++;
					}
					total++;
				}
				result.put("message", count + "/" + total + "terms successfully added.");
				return ok(result);
			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}
	
	public static Result deleteThesaurus(String thesaurus) {
		ObjectNode result = Json.newObject();
		try {
			DB.getThesaurusDAO().removeAllTermsFromThesaurus(thesaurus);

			result.put("message", "Thesaurus was deleted successfully");
			return ok(result);
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}
	

	public static Result getThesaurusTerm(String uri) {
		ObjectNode result = Json.newObject();

		try {
			if (uri == null) {
				result.put("error", "Invalid Request");
				return badRequest(result);
			} else {
				ThesaurusObject to = DB.getThesaurusDAO().getByUri(uri);
				if (to == null) {
					result.put("error", "Term not found");
				}
				
				return ok(Json.toJson(to));
			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	public static Result listThesauri() {
		ObjectNode result = Json.newObject();

		try {
			ArrayNode aresult = Json.newObject().arrayNode();

			for (Vocabulary voc : Vocabulary.values()) {
				
				ObjectNode json = Json.newObject();
				json.put("vocabulary", voc.getName());
				json.put("label", voc.getLabel());
				json.put("type", voc.getType().toString().toLowerCase());
				if (voc.getAnnotator() != null) {
					json.put("annotator", (String)voc.getAnnotator().getMethod("getName").invoke(null));
					json.put("annotatortype", ((AnnotatorType)voc.getAnnotator().getMethod("getType").invoke(null)).toString().toLowerCase());
				}
				
				aresult.add(json);
			}
			
			return ok(Json.toJson(aresult));

		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}
	
	public static String[] retrievedFields = new String[] {
		"prefLabel.en",
		"uri",
		"vocabulary"
	};
	
	public static Pattern p = Pattern.compile("^([a-z0-9]+): (.*)$");
	
	private static Language[] searchLanguages = new Language[] {Language.EN, Language.DE, 
		                                                        Language.FR, Language.IT, 
		                                                        Language.ES, Language.NL, 
		                                                        Language.EL, Language.PT};
	
	public static Result getSuggestions(String word) {
		
		try {
			ElasticSearcher es = new ElasticSearcher();
			
			ArrayNode terms = Json.newObject().arrayNode();
			
			Matcher m = p.matcher(word);

			String prefix = null;
			if (m.find()) {
				prefix = m.group(1);
				word = m.group(2);
			}
			
			if (word.trim().length() > 2) {
				BoolQueryBuilder query = QueryBuilders.boolQuery();
				
//				StringBuffer trWord = new StringBuffer();
//				
//				for (char c : word.toCharArray()) {
//					if (Character.isLetter(c)) {
//						trWord.append('[');
//						trWord.append(Character.toLowerCase(c));
//						trWord.append(Character.toUpperCase(c));
//						trWord.append(']');
//					} else {
//						trWord.append(c);
//					}
//				}
//				trWord.append(".*");
//				
//				query.must(QueryBuilders.regexpQuery("prefLabel_all.all", trWord.toString()));

				String[] words = word.split("[ ,\\.\\-:]");
				
				BoolQueryBuilder langQuery = QueryBuilders.boolQuery();
				for (Language lang : searchLanguages) {
					
					for (String s : words) {
						langQuery.should(QueryBuilders.prefixQuery("prefLabel." + lang.getDefaultCode(), s.toLowerCase()));
					}
					
				}
				query.must(langQuery);
				
				if (prefix != null) {
					query.must(QueryBuilders.termQuery("vocabulary", prefix));
				}
				
//				System.out.println(query);
				SearchOptions so = new SearchOptions(0, Integer.MAX_VALUE);
				so.isPublic = false;
				SearchResponse res = es.execute(query, so, retrievedFields);
				SearchHits sh = res.getHits();
				
				ArrayList<SearchSuggestion> suggestions = new ArrayList<SearchSuggestion>();
				
				for (Iterator<SearchHit> iter = sh.iterator(); iter.hasNext();) {
					SearchHit hit = iter.next();
					
					suggestions.add(new SearchSuggestion(word, hit.getId(), 
							                                  (String)hit.field("prefLabel.en").getValues().get(0), 
							                                  (String)hit.field("uri").getValues().get(0),
							                                  (String)hit.field("vocabulary").getValues().get(0)));
				}
				
				Collections.sort(suggestions);
				
				for (SearchSuggestion ss : suggestions) {
					ObjectNode element = Json.newObject();
					element.put("id", ss.id);

					element.put("label", ss.label);
					element.put("uri", ss.uri);
					element.put("vocabulary", ss.vocabulary);
					
					element.put("exact", ss.label.equals(word) && prefix != null && prefix.equals(ss.vocabulary));
					
					terms.add(element);
				}
			}
			
			return ok(terms);

		} catch (Exception e) {
			return internalServerError(e.getMessage());
		}
	}

}
