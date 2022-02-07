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

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import actors.MediaCheckerActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import com.github.jsonldjava.utils.Obj;
import model.EmbeddedMediaObject;
import model.basicDataTypes.Literal;
import model.basicDataTypes.WithAccess;
import model.resources.ThesaurusAdmin;
import model.usersAndGroups.User;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;

import com.aliasi.spell.JaccardDistance;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import annotators.CultIVMLAnnotator;
import annotators.DBPediaAnnotator;
import annotators.LookupAnnotator;
import annotators.NLPAnnotator;
import db.DB;
import elastic.Elastic;
import elastic.ElasticSearcher;
import elastic.ElasticSearcher.SearchOptions;
import model.Campaign;
import model.Campaign.CampaignTerm;
import model.Campaign.CampaignTermWithInfo;
import model.basicDataTypes.Language;
import model.basicDataTypes.MultiLiteral;
import model.resources.ThesaurusObject;
import model.resources.WithResourceType;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Akka;
import play.libs.F;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import vocabularies.Vocabulary;
import vocabularies.Vocabulary.VocabularyType;

/**
 * @author achort
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ThesaurusController extends WithController {

	public static final ALogger log = Logger.of(ThesaurusController.class);

	// This call is for creating a doc in the ThesaurusAdmin collection
	// It is also used for creating the WITH supported thesauri (by providing vocType)
	// This is a "hack", since we dont want any user to create WITH supported thesauri
	// Should consider how to properly implement that.
	public static Result createEmptyThesaurus(String name, String version, String label, F.Option<String> vocType) {
		User loggedInUser = effectiveUser();
		if (loggedInUser == null) {
			return badRequest("You should be signed in as a user.");
		}

		if (DB.getThesaurusAdminDAO().findThesaurusAdminByName(name) != null) {
			return badRequest("Thesaurus already exists with given name");
		}
		ThesaurusAdmin thesaurus;
		if (vocType.isDefined()) {
			thesaurus = new ThesaurusAdmin(name, version, label, VocabularyType.valueOf(vocType.get().toUpperCase()), loggedInUser.getDbId());
			if (!vocType.get().equals(VocabularyType.CUSTOM_THESAURUS)) {
				thesaurus.getAccess().setIsPublic(true);
			}
		}
		else {
			thesaurus = new ThesaurusAdmin(name, version, label, VocabularyType.CUSTOM_THESAURUS, loggedInUser.getDbId());
		}
		DB.getThesaurusAdminDAO().makePermanent(thesaurus);
		return ok(Json.toJson(thesaurus));

	}

	public static Result getThesaurusAdmin(String name) {
		ObjectNode result = Json.newObject();

		User loggedInUser = effectiveUser();
		if (loggedInUser == null) {
			result.put("error", "You should be signed in as a user.");
			return badRequest(Json.toJson(result));
		}

		ThesaurusAdmin thesaurusAdm = DB.getThesaurusAdminDAO().findThesaurusAdminByName(name);
		if (thesaurusAdm == null) {
			result.put("error", "No thesaurus found with that name.");
			return badRequest(Json.toJson(result));
		}
		if (!thesaurusAdm.getAccess().canRead(loggedInUser.getDbId())) {
			result.put("error", "You don't have READ access in this thesaurus.");
			return badRequest(Json.toJson(result));
		}

		return ok(Json.toJson(thesaurusAdm));
	}

	public static Result populateCustomThesaurus(String thesaurusName, String thesaurusVersion) {

		String[] langs = new String[] { "en", "it", "fr", "pl", "es", "el", "de", "nl" };

		User loggedInUser = effectiveUser();
		if (loggedInUser == null || !loggedInUser.getCampaignCreationAccess()) {
			return badRequest("You should be signed in as a user.");
		}

		final Http.MultipartFormData multipartBody = request().body().asMultipartFormData();

		if (multipartBody != null) {
			try {
				if (!multipartBody.getFiles().isEmpty()) {
					Http.MultipartFormData.FilePart fp = multipartBody.getFiles().get(0);
					File x = fp.getFile();

					Reader in = new FileReader(x);
					Iterable<CSVRecord> records = CSVFormat.DEFAULT.withDelimiter(',').parse(in);
					ThesaurusObject term;

					for (CSVRecord record : records) {
						String recordUri = record.get(0);
						try {
							URI uri = new URI(recordUri);
						}
						catch (Exception e) {
							return badRequest();
						}
						String recordLabel = record.get(record.size()-1);

						term = new ThesaurusObject(recordUri, recordLabel, "term_description");

						if (term.getSemantic().getUri().contains("wikidata")) {
							term.getSemantic().setUri(term.getSemantic().getUri().replace("/wiki/", "/entity/"));
							term.getSemantic().setUri(term.getSemantic().getUri().replace("https", "http"));
							HttpClient client = HttpClientBuilder.create().build();
							HttpGet request = new HttpGet(term.getSemantic().getUri() + ".json");

							try {
								HttpResponse response = client.execute(request);
								InputStream jsonStream = response.getEntity().getContent();
								JsonNode json = Json.parse(jsonStream);
								for (String lang : langs) {
									JsonNode langNode = json.get("entities").fields().next().getValue().get("labels").get(lang);
									if (langNode != null) {
										String langTerm = langNode.get("value").asText();
										term.getSemantic().getPrefLabel().addLiteral(Language.getLanguage(lang), langTerm);
									}
									else {
										String englishTerm = term.getSemantic().getPrefLabel().getLiteral(Language.EN);
										term.getSemantic().getPrefLabel().addLiteral(Language.getLanguage(lang), englishTerm);
									}
									JsonNode descNode = json.get("entities").fields().next().getValue().get("descriptions").get(lang);
									if (descNode != null) {
										String desc = descNode.get("value").asText();
										term.getSemantic().getDescription().addLiteral(Language.getLanguage(lang), desc);
									}
								}
							}
							catch (Exception e) {
								e.printStackTrace();
								return badRequest();
							}
						}
						else {
							try {
								URL u = new URL(term.getSemantic().getUri() + ".json");
								InputStream jsonStream = u.openStream();
								JsonNode json = Json.parse(jsonStream);

								Iterator<JsonNode> it = json.get("results").get("bindings").iterator();
								// support both us and gb english
								List<String> lang = new ArrayList<>(Arrays.asList(langs));
								lang.add("en-us");
								lang.add("en-gb");
								while (it.hasNext()) {
									JsonNode node = it.next();
									if (node.get("Object").get("xml:lang") != null
											&& node.get("Predicate").get("value").textValue().contains("skos/core#prefLabel")
											&& lang.contains(node.get("Object").get("xml:lang").asText())) {
										String langTerm = node.get("Object").get("value").asText();
										String lan = node.get("Object").get("xml:lang").asText();
										if (lan.equals("en-us") || lan.equals("en-gb")) {
											lan = "en";
										}
										term.getSemantic().getPrefLabel().addLiteral(Language.getLanguage(lan), langTerm);
									}
									else if (node.get("Object").get("xml:lang") != null
											&& node.get("Predicate").get("value").textValue().contains("http://www.w3.org/1999/02/22-rdf-syntax-ns#value")
											&& lang.contains(node.get("Object").get("xml:lang").asText())) {
										String langTerm = node.get("Object").get("value").asText();
										String lan = node.get("Object").get("xml:lang").asText();
										if (lan.equals("en-us") || lan.equals("en-gb")) {
											lan = "en";
										}
										term.getSemantic().getDescription().addLiteral(Language.getLanguage(lan), langTerm);
									}

								}

							} catch (MalformedURLException e) {
								String englishTerm = term.getSemantic().getPrefLabel().getLiteral(Language.EN);
								String[] s = englishTerm.split(",");
								if (s.length > 2) {
									Logger.info("Cannot create name");
								} else if (s.length == 2) {
									englishTerm = s[1].trim() + " " + s[0].trim();
								}
								for (String lang : langs) {
									term.getSemantic().getPrefLabel().addLiteral(Language.getLanguage(lang), englishTerm);
								}
								Logger.error(e.getMessage());
								;
							}
						}

						term.getSemantic().setVocabulary(new ThesaurusObject.SKOSVocabulary(thesaurusName, thesaurusVersion));
						term.getSemantic().setType("CUSTOM_THESAURUS_TERM");
						DB.getThesaurusDAO().makePermanent(term);
					}

					return ok();

				} else {
					return badRequest();
				}
			} catch (Exception e) {
				log.debug("CSV upload error", e);
				e.printStackTrace();
				return badRequest();
			}
		}
		return badRequest();
	}

	public static Result assignAccessToThesaurus(String userId, String thesaurusId, F.Option<String> accessLevel) {
		ObjectNode result = Json.newObject();
		try {
			User loggedInUser = effectiveUser();
			if (loggedInUser == null || !loggedInUser.getCampaignCreationAccess()) {
				result.put("error", "You should be signed in as a user.");
				return badRequest(Json.toJson(result));
			}

			ThesaurusAdmin thesaurusAdmin = DB.getThesaurusAdminDAO().findThesaurusAdminById(new ObjectId(thesaurusId));

			// Check if i have OWN permissions to the thesaurus
			if (thesaurusAdmin == null || (!thesaurusAdmin.getAccess().canDelete(loggedInUser.getDbId()))) {
				result.put("error", "Thesaurus not found or no access");
				return badRequest(Json.toJson(result));
			}

			User targetUser = DB.getUserDAO().getById(new ObjectId(userId));
			if (targetUser == null) {
				result.put("error", "Could not find user to give access to");
				return badRequest(Json.toJson(result));
			}

			if (accessLevel.isDefined()) {
				WithAccess.Access access = WithAccess.Access.valueOf(accessLevel.get().toUpperCase());
				thesaurusAdmin.getAccess().addToAcl(new WithAccess.AccessEntry(targetUser.getDbId(), access));
			}
			// If no access level is provided, assign the lower (read)
			else {
				thesaurusAdmin.getAccess().addToAcl(new WithAccess.AccessEntry(targetUser.getDbId(), WithAccess.Access.READ));
			}
			DB.getThesaurusAdminDAO().makePermanent(thesaurusAdmin);

			result.put("message", "Access assigned successfully");
			return ok(Json.toJson(result));
		}
		catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}

	}

	public static Result getThesaurusTerms(String thesaurusName) {
		ObjectNode result = Json.newObject();

		User loggedInUser = effectiveUser();
		if (loggedInUser == null) {
			result.put("error", "You should be signed in as a user.");
			return badRequest(Json.toJson(result));
		}

		ThesaurusAdmin adm = DB.getThesaurusAdminDAO().findThesaurusAdminByName(thesaurusName);

		if (adm == null) {
			result.put("error", "Thesaurus does not exist");
			return badRequest(Json.toJson(result));
		}

		if (adm.getAccess().getIsPublic() || adm.getAccess().canRead(loggedInUser.getDbId())) {
			List<ThesaurusObject> terms = DB.getThesaurusDAO().getALlByVocabularyName(thesaurusName);
			return ok(Json.toJson(terms));
		}
		else {
			result.put("error", "No access to thesaurus");
			return badRequest(Json.toJson(result));
		}
	}

	public static Result emptyThesaurus(String id) {
		ObjectNode result = Json.newObject();
		try {
			User loggedInUser = effectiveUser();
			if (loggedInUser == null || !loggedInUser.getCampaignCreationAccess()) {
				result.put("error", "You should be signed in as a user.");
				return badRequest(Json.toJson(result));
			}

			ThesaurusAdmin adm = DB.getThesaurusAdminDAO().findThesaurusAdminById(new ObjectId(id));
			if (adm == null || (!adm.getAccess().canDelete(loggedInUser.getDbId()))) {
				result.put("error", "Thesaurus not found or no access");
				return badRequest(Json.toJson(result));
			}

			DB.getThesaurusDAO().removeAllTermsFromThesaurus(adm.getName());
			result.put("message", "Thesaurus was deleted successfully");
			return ok(result);
		}
		catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	public static Result deleteThesaurusAdminObject(String id) {
		ObjectNode result = Json.newObject();

		try {
			User loggedInUser = effectiveUser();

			if (loggedInUser == null || !loggedInUser.getCampaignCreationAccess()) {
				return badRequest("You should be signed in as a user.");
			}

			ThesaurusAdmin adm = DB.getThesaurusAdminDAO().findThesaurusAdminById(new ObjectId(id));
			if (adm == null || (!adm.getAccess().canDelete(loggedInUser.getDbId()))) {
				return badRequest();
			}

			DB.getThesaurusDAO().removeAllTermsFromThesaurus(adm.getName());
			DB.getThesaurusAdminDAO().removeThesaurusAdmin(new ObjectId(id));
			result.put("message", "Thesaurus deleted successfuly");
			return ok(Json.toJson(result));
		}
		catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);		}

	}

	public static Result removeThesaurusTerm(String id) {
		ObjectNode result = Json.newObject();

		try {
			User loggedInUser = effectiveUser();
			if (loggedInUser == null || !loggedInUser.getCampaignCreationAccess()) {
				return badRequest("You should be signed in as a user.");
			}
			ThesaurusObject term = DB.getThesaurusDAO().getById(new ObjectId(id));

			if (term == null) {
				result.put("error", "Term does not exist");
				return badRequest(Json.toJson(result));
			}

			ThesaurusAdmin adm = DB.getThesaurusAdminDAO().findThesaurusAdminByName(term.getSemantic().getVocabulary().getName());

			if (adm == null) {
				result.put("error", "Server Error in DB");
				return internalServerError(Json.toJson(result));
			}

			if (!adm.getAccess().canDelete(loggedInUser.getDbId())) {
				result.put("error", "No DELETE access");
				return badRequest(Json.toJson(result));
			}

			DB.getThesaurusDAO().removeById(new ObjectId(id));
			result.put("message", "Term Deleted Successfully");
			return ok(Json.toJson(ok()));

		}
		catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}
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
			ThesaurusObject resource = DB.getThesaurusDAO().getUniqueByFieldAndValue("administrative.externalId", uri,
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
				ThesaurusObject resource = DB.getThesaurusDAO().getUniqueByFieldAndValue("administrative.externalId",
						uri, new ArrayList<String>(Arrays.asList("_id")));
				DB.getThesaurusDAO().editRecord("semantic", resource.getDbId(), json.get("semantic"));
				// should reindex
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

	public static ThesaurusObject getThesaurusArtStyle(String name) {
		try {
			if (name == null) {
				return null;
			} else {
				ThesaurusObject to = DB.getThesaurusDAO().getByPrefLabel(name);
				if(to.getSemantic().getVocabulary().getName().equals("wikidata")){
					return to;
				}
				else{
					return null;
				}
			}
		} catch (Exception e) {
			return null;
		}
	}

	public static Result listVocabularies() {
		User loggedInUser = effectiveUser();
		if (loggedInUser == null) {
			return badRequest("You should be signed in as a user.");
		}
		List<ThesaurusAdmin> thesaurusList = DB.getThesaurusAdminDAO().getUserAccessibleThesaurusAdminObjects(effectiveUser().getDbId());
		return ok(Json.toJson(thesaurusList));
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

	private static String[] retrievedFields;
	private static Language[] searchLanguages;
	public static Pattern p = Pattern.compile("^([a-z0-9]+): (.*)$");

	static {
		searchLanguages = new Language[searchLangCodes.length];
		retrievedFields = new String[searchLangCodes.length + 5];
		retrievedFields[0] = "uri";
		retrievedFields[1] = "vocabulary.name";
		retrievedFields[2] = "broaderTransitive.uri";
		retrievedFields[3] = "broader.prefLabel.en";
		for (int i = 0; i < searchLangCodes.length; i++) {
			retrievedFields[4 + i] = "prefLabel." + searchLangCodes[i];
			searchLanguages[i] = Language.getLanguageByCode(searchLangCodes[i]);
		}
		retrievedFields[4 + searchLangCodes.length] = "properties.values.prefLabel.en";
	};

	public static ArrayNode getWikidataSuggestions(String word) throws ClientProtocolException, IOException {
		String url = "https://www.wikidata.org/w/api.php?action=wbsearchentities&language=fr&format=json&search="
				+ URLEncoder.encode(word, StandardCharsets.UTF_8.toString());
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(url);
		HttpResponse resp = client.execute(request);
		JsonNode jsonRes = Json.parse(resp.getEntity().getContent()).get("search");
		ArrayNode terms = Json.newObject().arrayNode();
		for (JsonNode res : jsonRes) {
			ObjectNode element = Json.newObject();
			element.put("id", res.get("id").asText());
			element.put("label", res.get("label").asText());
			if (res.hasNonNull("description"))
				element.put("description", res.get("description").asText());
			element.put("matchedLabel", res.get("match").get("text").asText());
			element.put("uri", res.get("concepturi").asText());
			element.put("vocabulary", "wikidata");
			terms.add(element);
		}
		return terms;
	}

	public static ArrayNode searchCampaignTerms(String word, List<CampaignTerm> terms, String language) {
		ArrayNode results = Json.newObject().arrayNode();
		word = word.toLowerCase();
		for (CampaignTerm term : terms) {
			if (term.selectable) {
				for (Entry<String, String> e : term.labelAndUri.entrySet()) {
					Boolean languageCondition = (language.equalsIgnoreCase("all") ? true
							: e.getKey().equalsIgnoreCase(language) || e.getKey().equalsIgnoreCase("default"));
					if (languageCondition && e.getValue().toLowerCase().contains(word)
							&& !e.getKey().equalsIgnoreCase("uri")) {
						ObjectNode resInfo = Json.newObject();
						resInfo.put("label", e.getValue());
						if ( (term instanceof CampaignTermWithInfo) && (((CampaignTermWithInfo) term).description.get(e.getKey()) != null) ) {
							resInfo.put("description", ((CampaignTermWithInfo) term).description.get(e.getKey()));
						}
						resInfo.put("uri", term.labelAndUri.getURI());
						resInfo.put("lang", e.getKey());
						resInfo.put("id", term.labelAndUri.getURI());
						MultiLiteral labels = new MultiLiteral(term.labelAndUri);
						labels.remove("uri");
						resInfo.put("labels", Json.toJson(labels));
						results.add(resInfo);
					}
				}
			}
			if (term.children != null && term.children.size() > 0) {
				results.addAll(searchCampaignTerms(word, term.children, language));
			}
		}
		ArrayNode sortedResults = Json.newObject().arrayNode();
		for (JsonNode res : results) {
			if (res.get("label").asText().toLowerCase().startsWith(word)) {
				sortedResults.insert(0, res);
			} else {
				sortedResults.add(res);
			}
		}
		return sortedResults;
	}

	public static Result getSuggestions(String word, String namespaces, String campaignId, Boolean geotagging,
			String language) throws ClientProtocolException, IOException {
		ObjectNode response = Json.newObject();
		response.put("request", word);

		try {
			ArrayNode terms = Json.newObject().arrayNode();
			ArrayNode campaignResults = Json.newObject().arrayNode();

			// Matcher m = p.matcher(word);

			// String prefix = null;
			// if (m.find()) {
			// prefix = m.group(1);
			// word = m.group(2);
			// }

			boolean searchForSuggestions = false;

			/*
				Split word on . , : - so that you can
				search with multiple words on the same call
			 */
			String[] words = word.split("[ ,\\.\\-:]");
			for (String s : words) {
				if (s.length() > 2) {
					searchForSuggestions = true;
				}
			}
			String[] namespaceArray = new String[0];
			if (StringUtils.isNotBlank(namespaces)) {
				namespaceArray = namespaces.split(",");
			}

			// TODO: To be removed from code. Campaign terms are obsolete.
			if (campaignId != null) {
				Campaign campaign = DB.getCampaignDAO().getById(new ObjectId(campaignId));
				if (!geotagging && campaign.getCampaignTerms() != null && campaign.getCampaignTerms().size() > 0) {
					List<CampaignTerm> campaignTerms = campaign.getCampaignTerms();
					ArrayNode res = searchCampaignTerms(word, campaignTerms, language);
					campaignResults = res;
					ObjectNode result = Json.newObject();
					result.put("request", word);
					result.set("results", res);
//					return ok(result);
				}
				List<String> vocabularyList = campaign.getVocabularies();
				namespaceArray = vocabularyList.toArray(new String[vocabularyList.size()]);
			}

			if (searchForSuggestions) {
				/*
					Normalize word for elastic search
				 */
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
				if (!language.equalsIgnoreCase("all")) {
					Language lang = Language.getLanguage(language);
					searchLanguages = new Language[] { lang };
				}
				for (Language lang : searchLanguages) {
					BoolQueryBuilder ilangQuery = QueryBuilders.boolQuery();

					for (String s : words) {
						// ilangQuery.must(QueryBuilders.prefixQuery("prefLabel." +
						// lang.getDefaultCode(), s.toLowerCase()));
						BoolQueryBuilder labQuery = QueryBuilders.boolQuery();
						labQuery.should(QueryBuilders.regexpQuery("prefLabel." + lang.getDefaultCode(), s.toString()));
						labQuery.should(QueryBuilders.regexpQuery("altLabel." + lang.getDefaultCode(), s.toString()));

						// ilangQuery.must(QueryBuilders.regexpQuery("prefLabel." +
						// lang.getDefaultCode(), s.toString()));
						ilangQuery.must(labQuery);
					}
					langQuery.should(ilangQuery);
				}
				query.must(langQuery);

				// if (prefix != null) {
				// query.must(QueryBuilders.termQuery("vocabulary.name", prefix));
				// }

				if (namespaceArray.length > 0) {

					if (namespaceArray.length == 1) {
						query.must(QueryBuilders.termQuery("vocabulary.name", namespaceArray[0]));
					} else {
						BoolQueryBuilder vocabNameQuery = QueryBuilders.boolQuery();
						for (String voc : namespaceArray) {
							vocabNameQuery.should(QueryBuilders.termQuery("vocabulary.name", voc));
						}
						query.must(vocabNameQuery);
					}		
				}

				// System.out.println("QUERY" + query);
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

						SearchHitField props = hit.field("properties.values.prefLabel.en");

						List<String> labels = new ArrayList<>();
						for (int i = 0; i < searchLangCodes.length; i++) {
							SearchHitField label = hit.field("prefLabel." + searchLangCodes[i]);
							if (label != null) {
								labels.add((String) label.getValues().get(0));
							}
						}

						suggestions.add(new SearchSuggestion(word, hit.getId(),
								(String) hit.field("prefLabel." + (language.equalsIgnoreCase("all") ? "en" : language.toLowerCase()))
								.getValues().get(0),
								labels.toArray(new String[labels.size()]),
								(String) hit.field("uri").getValues().get(0),
								(String) hit.field("vocabulary.name").getValues().get(0),
								categories != null ? categories.getValues().toArray(new String[] {}) : null,
								props != null ? props.getValues().toArray(new String[] {}) : null));

					}
					sr = Elastic.getTransportClient().prepareSearchScroll(sr.getScrollId())
							.setScroll(new TimeValue(60000)).execute().actionGet();

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

					element.put("label", ss.langLabel);
					element.put("matchedLabel", ss.getSelectedLabel());
					element.put("uri", ss.uri);
					element.put("vocabulary", ss.vocabulary);

					ArrayNode array = Json.newObject().arrayNode();
					if (ss.categories != null) {
						for (String c : ss.categories) {
							array.add(c);
						}
					}

					if (ss.properties != null) {
						for (String c : ss.properties) {
							array.add(c);
						}
					}

					if (array.size() > 0) {
						element.put("categories", array);
					}

					// element.put("exact", ss.getSelectedLabel().equals(word) && prefix != null &&
					// prefix.equals(ss.vocabulary));
					element.put("exact", ss.getSelectedLabel().equals(word));

					terms.add(element);
				}
			}
			List<String> namespaceArrayList = Arrays.asList(namespaceArray);
			ArrayNode allTerms = Json.newObject().arrayNode();
			if (namespaceArrayList.contains("wikidata")) {
				allTerms = getWikidataSuggestions(word);
				allTerms.addAll(terms);
			} else {
				allTerms = terms;
			}

			// TODO: To be removed from code. Campaign terms are obsolete.
			if (campaignId != null) {
				campaignResults.addAll(allTerms);
				response.put("results", campaignResults);
			} else {
				response.put("results", allTerms);
			}

			return ok(response);

		} catch (Exception e) {
			e.printStackTrace();
			return internalServerError(e.getMessage());
		}
	}

	private static class SearchSuggestion implements Comparable<SearchSuggestion> {
		public String id;
		public String langLabel;
		public String[] labels;
		public String uri;
		public String vocabulary;
		public String[] categories;
		public String[] properties;

		public double distance;
		private int selectedLabel;

		private static JaccardDistance jaccard = new JaccardDistance(IndoEuropeanTokenizerFactory.INSTANCE);

		public SearchSuggestion(String reference, String id, String langLabel, String[] labels, String uri,
				String vocabulary, String[] categories, String[] properties) {
			this.id = id;
			this.langLabel = langLabel;
			this.labels = labels;
			this.uri = uri;
			this.vocabulary = vocabulary;
			this.categories = categories;
			this.properties = properties;

			distance = Double.MAX_VALUE;
			selectedLabel = 0;
			for (int i = 0; i < labels.length; i++) {
				// double d = jaccardDistance(2, reference, labels[i]);
				double d = jaccard.distance(reference.toLowerCase(), labels[i].toLowerCase());
				// if (!reference.equals(labels[i])) {
				// d += 0.1;
				// }

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

		// public static double jaccardDistance(int n, String s, String t) {
		// if (s == null || t == null) {
		// return 1;
		// }
		//
		// int l1 = s.length() - n + 1;
		// int l2 = t.length() - n + 1;
		//
		// int found = 0;
		// for (int i = 0; i < l1 ; i++ ){
		// for (int j = 0; j < l2; j++) {
		// int k = 0;
		// for( ; ( k < n ) && ( Character.toLowerCase(s.charAt(i+k)) ==
		// Character.toLowerCase(t.charAt(j+k)) ); k++);
		// if (k == n) {
		// found++;
		// }
		// }
		// }
		//
		// double dist = 1-(2*((double)found)/((double)(l1+l2)));
		// if (!s.equals(t)) {
		// dist += 0.1;
		// }
		//
		// return dist;
		// }
	}

}
