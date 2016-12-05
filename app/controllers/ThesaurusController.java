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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.basicDataTypes.Language;
import model.resources.ThesaurusObject;
import model.resources.WithResourceType;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import vocabularies.Vocabulary;
import vocabularies.Vocabulary.VocabularyType;

import com.aliasi.spell.JaccardDistance;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;
import elastic.Elastic;
import elastic.ElasticSearcher;
import elastic.ElasticSearcher.SearchOptions;
import actors.annotation.CultIVMLAnnotatorActor;
import annotators.CultIVMLAnnotator;
import annotators.DBPediaAnnotator;
import annotators.LookupAnnotator;
import annotators.NLPAnnotator;

/**
 * @author achort
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
		
		if (DB.getThesaurusDAO().existsWithExternalId(uri)) {
			ThesaurusObject resource = DB.getThesaurusDAO()
					.getUniqueByFieldAndValue("administrative.externalId",
							uri,
							new ArrayList<String>(Arrays.asList("_id")));
			DB.getThesaurusDAO().editRecord("semantic", resource.getDbId(), json.get("semantic"));
			// should be reindexed

		} else {
			record.getAdministrative().setCreated(new Date());
			record.getAdministrative().setExternalId(uri);
			DB.getThesaurusDAO().makePermanent(record);
			ObjectId recordId = record.getDbId();
			DB.getThesaurusDAO().updateField(recordId, "administrative.externalId", uri);
		}
		
		return true;
	}
	
	public static boolean addThesaurusTerms(List<JsonNode> jsons) throws ClassNotFoundException {
		Class<?> clazz = Class.forName("model.resources.ThesaurusObject");

		ArrayList<ThesaurusObject> records = new ArrayList<>();

		for (JsonNode json : jsons) {
			ThesaurusObject record = (ThesaurusObject) Json.fromJson(json, clazz);
			String uri = record.getSemantic().getUri();
			
			if (uri == null) {
				continue;
			}
			
			if (DB.getThesaurusDAO().existsWithExternalId(uri)) {
				ThesaurusObject resource = DB.getThesaurusDAO()
						.getUniqueByFieldAndValue("administrative.externalId",
								uri,
								new ArrayList<String>(Arrays.asList("_id")));
				DB.getThesaurusDAO().editRecord("semantic", resource.getDbId(), json.get("semantic"));
				//should reindex
			} else {
				record.getAdministrative().setCreated(new Date());
				record.getAdministrative().setExternalId(uri);
				records.add(record);
			}
		}
		
		DB.getThesaurusDAO().storeMany(records);

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

	public static Result listVocabularies() {
		ObjectNode result = Json.newObject();

		try {
			ArrayNode aresult = Json.newObject().arrayNode();
			for (Vocabulary voc : Vocabulary.getVocabularies()) {
				
				ObjectNode json = Json.newObject();
				json.put("name", voc.getName());
				json.put("label", voc.getLabel());
				json.put("type", voc.getType().toString().toLowerCase());
				
				aresult.add(json);
			}
			
			return ok(Json.toJson(aresult));

		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}
	
	public static Result listAnnotators() {
		ArrayNode result = Json.newObject().arrayNode();
		
		ObjectNode ann, option;
		ArrayNode options;
		
		ann = Json.newObject();
		ann.put("group", "Term Detection");
		ann.put("hint", "Select the vocabularies that will be used for term detection");

		options = Json.newObject().arrayNode();
		for (Vocabulary voc : Vocabulary.getVocabularies()) {
			if (voc.getType() == VocabularyType.THESAURUS) {
				option = Json.newObject();
				option.put("name", LookupAnnotator.class.getSimpleName() + "/" + voc.getName());
				option.put("label", voc.getLabel());
			
				options.add(option);
			}
		}
		ann.put("options", options);
		result.add(ann);
		
		ann = Json.newObject();
		ann.put("group", "Named Entity Recognition");
		ann.put("hint", "Select the named entity recognition engines that will be used");
		
		options = Json.newObject().arrayNode();
		option = Json.newObject();
		option.put("name", DBPediaAnnotator.class.getSimpleName());
		option.put("label", DBPediaAnnotator.getName());
		options.add(option);

		option = Json.newObject();
		option.put("name", NLPAnnotator.class.getSimpleName());
		option.put("label", NLPAnnotator.getName());
		options.add(option);

		ann.put("options", options);
		result.add(ann);
		
		ann = Json.newObject();
		ann.put("group", "Image Analysis");
		ann.put("hint", "Select the image analysis services that will be used");
		
		options = Json.newObject().arrayNode();
		option = Json.newObject();
		option.put("name", CultIVMLAnnotator.class.getSimpleName());
		option.put("label", CultIVMLAnnotator.getName());
		options.add(option);

		ann.put("options", options);
		result.add(ann);
		
		return ok(Json.toJson(result));
	}
	
	private static String useLanguages = DB.getConf().getString("annotators.autocomplete_languages");
	private static String[] searchLangCodes;
		
	static {
		List<String> langs = new ArrayList<>();
		for (String s : useLanguages.split(",")) {
			langs.add(s);
		}
		
		searchLangCodes = langs.toArray(new String[langs.size()]);
	}
	
	private	static String[] retrievedFields;
	private static Language[] searchLanguages;
	public static Pattern p = Pattern.compile("^([a-z0-9]+): (.*)$");
	
	static {
		searchLanguages = new Language[searchLangCodes.length];
		retrievedFields = new String[searchLangCodes.length + 3];
		retrievedFields[0] = "uri";
		retrievedFields[1] = "vocabulary.name";
		retrievedFields[2] = "broader.prefLabel.en";
		for (int i = 0; i < searchLangCodes.length; i++) {
			retrievedFields[i + 3] = "prefLabel." + searchLangCodes[i];
			searchLanguages[i] = Language.getLanguage(searchLangCodes[i]);
		}
	};
	
	
	public static Result getSuggestions(String word, String namespaces) {
		
		ObjectNode response = Json.newObject();
		response.put("request", word);

		try {
			ArrayNode terms = Json.newObject().arrayNode();
			
			Matcher m = p.matcher(word);

			String prefix = null;
			if (m.find()) {
				prefix = m.group(1);
				word = m.group(2);
			}
			
			boolean ok =  false;
			
			String[] words = word.split("[ ,\\.\\-:]");
			for (String s : words) {
				if (s.length() > 2) {
					ok = true;
				}
			}
			String[] namespaceArray = new String[0];
			if( StringUtils.isNotBlank(namespaces)) {
				namespaceArray = namespaces.split(",");
			}
			
			if (ok) {
				for (int i = 0; i < words.length; i++) {
					StringBuffer trWord = new StringBuffer();
				
					for (char c : words[i].toCharArray()) {
						if (Character.isLetter(c)) {
							trWord.append('[');
							trWord.append(Character.toLowerCase(c));
							trWord.append(Character.toUpperCase(c));
							trWord.append(']');
						} else {
							trWord.append(c);
						}
					}
					trWord.append(".*");
					words[i] = trWord.toString();
				}

				BoolQueryBuilder query = QueryBuilders.boolQuery();

				BoolQueryBuilder langQuery = QueryBuilders.boolQuery();
				for (Language lang : searchLanguages) {
					BoolQueryBuilder ilangQuery = QueryBuilders.boolQuery();
					
					for (String s : words) {
//						ilangQuery.must(QueryBuilders.prefixQuery("prefLabel." + lang.getDefaultCode(), s.toLowerCase()));
						ilangQuery.must(QueryBuilders.regexpQuery("prefLabel." + lang.getDefaultCode(), s.toString()));
					}
					langQuery.should(ilangQuery);
				}
				query.must(langQuery);
				
				if (prefix != null) {
					query.must(QueryBuilders.termQuery("vocabulary.name", prefix));
				}
			
				if( namespaceArray.length > 0 ) {
					if( namespaceArray.length == 1 ) {
						query.must(QueryBuilders.termQuery("vocabulary.name", namespaceArray[0]));
					} else {
						BoolQueryBuilder vocabNameQuery = QueryBuilders.boolQuery();
						for( String voc: namespaceArray ) {
							vocabNameQuery.should(QueryBuilders.termQuery("vocabulary.name", voc));
						}
						query.must( vocabNameQuery);
					}
				}
				
//				System.out.println("QUERY" + query);
				SearchOptions so = new SearchOptions(0, 1000);
				so.isPublic = false;
				so.scroll = true;
				so.searchFields = retrievedFields;
				
				ArrayList<SearchSuggestion> suggestions = new ArrayList<SearchSuggestion>();

				ElasticSearcher searcher = new ElasticSearcher();
				searcher.setTypes(new ArrayList<String>() {
					{
						add(WithResourceType.ThesaurusObject.toString().toLowerCase()); 
					}
				});
				SearchRequestBuilder srb = searcher.getSearchRequestBuilder(query, so);
				
				SearchResponse sr = srb.execute().actionGet();
				while (true) {
					for (SearchHit hit : sr.getHits().getHits()) {
						SearchHitField categories = hit.field("broader.prefLabel.en");
						
						List<String> labels = new ArrayList<>();
						for (int i = 0; i < searchLangCodes.length; i++) {
							SearchHitField label = hit.field("prefLabel." + searchLangCodes[i]);
							if (label != null) {
								labels.add((String)label.getValues().get(0));
							}
						}
						
						suggestions.add(new SearchSuggestion(word, hit.getId(),
								(String)hit.field("prefLabel.en").getValues().get(0),
                                labels.toArray(new String[labels.size()]), 
                                (String)hit.field("uri").getValues().get(0),
                                (String)hit.field("vocabulary.name").getValues().get(0),
                                categories != null ? categories.getValues().toArray(new String[] {}) : null));

				    }
				    sr = Elastic.getTransportClient().prepareSearchScroll(sr.getScrollId()).setScroll(new TimeValue(60000)).execute().actionGet();
				    
				    if (sr.getHits().getHits().length == 0) {
				        break;
				    }
				}
				
				Collections.sort(suggestions);
				
				int limit = Math.min(100, suggestions.size());
				for (int i = 0; i < limit; i++) {
					SearchSuggestion ss = suggestions.get(i);
					
					ObjectNode element = Json.newObject();
					element.put("id", ss.id);

					element.put("label", ss.enLabel);
					element.put("matchedLabel", ss.getSelectedLabel());
					element.put("uri", ss.uri);
					element.put("vocabulary", ss.vocabulary);
					
					if (ss.categories != null) {
						ArrayNode array = Json.newObject().arrayNode();
						for (String c : ss.categories) {
							array.add(c);
						}
						element.put("categories", array);
					}
					
					element.put("exact", ss.getSelectedLabel().equals(word) && prefix != null && prefix.equals(ss.vocabulary));
					
					terms.add(element);
				}
			}
			
			response.put("results", terms);
			
			return ok(response);

		} catch (Exception e) {
			e.printStackTrace();
			return internalServerError(e.getMessage());
		}
	}
	
	private static class SearchSuggestion implements Comparable<SearchSuggestion> {
		public String id;
		public String enLabel;
		public String[] labels;
		public String uri;
		public String vocabulary;
		public String[] categories;
		
		public double distance;
		private int selectedLabel;
		
		private static JaccardDistance jaccard = new JaccardDistance(IndoEuropeanTokenizerFactory.INSTANCE);
		
		public SearchSuggestion (String reference, String id, String enLabel, String[] labels, String uri, String vocabulary, String[] categories) {
			this.id = id;
			this.enLabel = enLabel;
			this.labels = labels;
			this.uri = uri;
			this.vocabulary = vocabulary;
			this.categories = categories;
			
			distance = Double.MAX_VALUE;
			selectedLabel = 0;
			for (int i = 0; i < labels.length; i++) {
//				double d = jaccardDistance(2, reference, labels[i]);
				double d = jaccard.distance(reference.toLowerCase(), labels[i].toLowerCase());
//				 if (!reference.equals(labels[i])) {
//					 d += 0.1;
//				 }

				 if (d < distance) {
					distance = d;
					selectedLabel = i;
				}
			}
		}

		public String getSelectedLabel() {
			return labels[selectedLabel];
		}
		
		@Override
		public int compareTo(SearchSuggestion arg0) {
			if (this.distance < arg0.distance) {
				return -1;
			} else if (this.distance > arg0.distance) {
				return 1;
			} else {
				return 0;
			}
		}
		
//		 public static double jaccardDistance(int n, String s, String t) {
//			 if (s == null || t == null) {
//				 return 1;
//			 }
//			 
//			 int l1 = s.length() - n + 1;
//			 int l2 = t.length() - n + 1;
//			 
//			 int found = 0;
//			 for (int i = 0; i < l1 ; i++  ){
//				 for (int j = 0; j < l2; j++) {
//					 int k = 0;
//					 for( ; ( k < n ) && ( Character.toLowerCase(s.charAt(i+k)) == Character.toLowerCase(t.charAt(j+k)) ); k++);
//					 if (k == n) {
//						 found++;
//					 }
//				 }
//			 }
//
//			 double dist = 1-(2*((double)found)/((double)(l1+l2)));
//			 if (!s.equals(t)) {
//				 dist += 0.1;
//			 }
//			 
//			 return dist;
//		}
	}

}
