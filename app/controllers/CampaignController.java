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



import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.function.BiConsumer;
import java.util.function.Function;

import db.DB;
import edu.stanford.nlp.io.IOUtils;
import model.Campaign;
import model.Campaign.AnnotationCount;
import model.Campaign.CampaignBaseAnnotationsObject;
import model.Campaign.CampaignTerm;
import model.Campaign.CampaignTermWithInfo;
import model.annotations.Annotation;
import model.annotations.selectors.PropertyTextFragmentSelector;
import model.annotations.Annotation.AnnotationAdmin;
import model.annotations.Annotation.CreatorType;
import model.annotations.Annotation.MotivationType;
import model.annotations.bodies.AnnotationBody;
import model.annotations.bodies.AnnotationBodyTagging;
import model.annotations.bodies.AnnotationBodyCommenting;
import model.annotations.bodies.AnnotationBodyImageTagging;
import model.annotations.bodies.AnnotationBodySubTagging;
import model.annotations.bodies.AnnotationBodyColorTagging;
import model.annotations.targets.AnnotationTarget;

import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.basicDataTypes.MultiLiteral;
import model.resources.ThesaurusObject;
import model.resources.CulturalObject.CulturalObjectData;
import model.resources.RecordResource;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Result;
import sources.core.ParallelAPICall;


public class CampaignController extends WithController {

	public static final ALogger log = Logger.of(CampaignController.class);
	public static final String vocabularyPath = "vocabularies/";
	public static final Map<String, String> termsFile = Stream
			.of(new String[][] { { "sports", "Sport_Vocabulary.csv" },
					{ "cities-landscapes", "Cities_Landscapes_Means_of_Transport_Vocabulary.csv" },
					{ "instruments", "MIMO-Thesaurus for-campaign.csv" }, { "opera", "Opera_entities.csv" }, { "china", "China-terms.csv" }, { "test", "test.csv" } })
			.collect(Collectors.toMap(data -> data[0], data -> data[1]));

	public static Result getCampaignCount(String group, String project, String state) {
		long count = DB.getCampaignDAO().campaignCount(group, project, state);
		return ok(Json.toJson(count));
	}

	public static Result getCampaign(String campaignId) {
		ObjectId campaignDbId = new ObjectId(campaignId);
		Campaign campaign = DB.getCampaignDAO().getCampaign(campaignDbId);
		return ok(Json.toJson(campaign));
	}

	public static Result getCampaignByName(String cname) {
		Campaign campaign = DB.getCampaignDAO().getCampaignByName(cname);
		return ok(Json.toJson(campaign));
	}

	public static Result deleteCampaign(String campaignId) {
		ObjectId campaignDbId = new ObjectId(campaignId);
		DB.getCampaignDAO().deleteById(campaignDbId);
		return ok();
	}

	
	public static void updateLiteralField(Campaign c1, Campaign c2, Function<Campaign, Literal> f, BiConsumer<Campaign, Literal> bc) {
		if (f.apply(c1) == null && f.apply(c2) != null) {
			bc.accept(c1, f.apply(c2));
		} else {
			Literal newVal = f.apply(c2);
			if (newVal == null) {
				return;
			}
			Set<String> langs = newVal.keySet();
			for (String lang : langs) {
				String newLang = newVal.getLiteral(Language.getLanguageByCode(lang));
				if (newLang != null && (newLang.isEmpty() == false)) {
					f.apply(c1).addLiteral(Language.getLanguageByCode(lang), newLang);
				}
			}
		}
	}

	public static <T> void updateListField(Campaign c1, Campaign c2, Function<Campaign, List<T>> f, BiConsumer<Campaign, List<T>> bc) {
		if (f.apply(c1) == null && f.apply(c2) != null) {
			bc.accept(c1, f.apply(c2));
		} else {
			if (f.apply(c2) == null)
				return;
			f.apply(c1).clear();
			List<T> newList = f.apply(c2).stream().distinct().collect(Collectors.toList());
			f.apply(c1).addAll(newList);
		}
	}

	public static Result editCampaign(String id) throws ClassNotFoundException, JsonProcessingException, IOException, ParseException {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		ObjectId user = effectiveUserDbId();
		if (json == null) {
			result.put("error", "Invalid JSON");
			return badRequest(result);
		}
		ObjectId campaignDbId = new ObjectId(id);
		Campaign campaign = DB.getCampaignDAO().get(campaignDbId);
		if (user == null || !campaign.getCreators().contains(user)) {
			result.put("error", "No rights for campaign edit");
			return forbidden(result);
		}
		Class<?> clazz = Class.forName("model.Campaign");
		Campaign newCampaign = (Campaign) Json.fromJson(json, clazz);

		updateLiteralField(campaign, newCampaign, Campaign::getTitle, Campaign::setTitle);
		updateLiteralField(campaign, newCampaign, Campaign::getDescription, Campaign::setDescription);
		updateLiteralField(campaign, newCampaign, Campaign::getInstructions, Campaign::setInstructions);
		updateLiteralField(campaign, newCampaign, Campaign::getDisclaimer, Campaign::setDisclaimer);

		if (newCampaign.getPrizes() != null) {
				campaign.setPrizes(newCampaign.getPrizes());
		}
		if (newCampaign.getStartDate() != null)
			campaign.setStartDate(newCampaign.getStartDate());
		if (newCampaign.getEndDate() != null)
			campaign.setEndDate(newCampaign.getEndDate());
		if (newCampaign.getBanner() != null)
			campaign.setBanner(newCampaign.getBanner());
		if (newCampaign.getLogo() != null)
			campaign.setLogo(newCampaign.getLogo());
		if (newCampaign.getOrientation() != null) {
			campaign.setOrientation(newCampaign.getOrientation());
		}
		if (json.has("isPublic")) {
			campaign.setIsPublic(json.get("isPublic").asBoolean());
		}

		if( json.has( "hideRating" )) {
			campaign.setHideRating( json.get( "hideRating").asBoolean());
		}
		if( json.has( "hideComments" )) {
			campaign.setHideComments( json.get( "hideComments").asBoolean());
		}
		if( json.has( "hasPublicResults" )) {
			campaign.setHasPublicResults( json.get( "hasPublicResults").asBoolean());
		}



		if (json.has("vocabularyMapping")) {
			campaign.setVocabularyMapping(newCampaign.getVocabularyMapping());
		}
		if (newCampaign.getPurpose() != null) {
			campaign.setPurpose(newCampaign.getPurpose());
		}
		if (newCampaign.getValidationErrorType() != null) {
			campaign.setValidationErrorType(newCampaign.getValidationErrorType());
		}

		if (newCampaign.getColorTaggingColorsTerminology() != null) {
			campaign.setColorTaggingColorsTerminology(newCampaign.getColorTaggingColorsTerminology());
		}
		
		updateListField(campaign, newCampaign, Campaign::getTargetCollections, Campaign::setTargetCollections);
		updateListField(campaign, newCampaign, Campaign::getUserGroupIds, Campaign::setUserGroupIds);
		if (newCampaign.getCreators().size() != 0) {
			campaign.setCreators(newCampaign.getCreators());
			/**
			 * TODO: Add - remove access from extra campaign creators
			 */
		}
		if (newCampaign.getAnnotationTarget() != 0L) {
			campaign.setAnnotationTarget(newCampaign.getAnnotationTarget());
		}
		if (newCampaign.getContributorFeedbackMethod() != null)
			campaign.setContributorFeedbackMethod(newCampaign.getContributorFeedbackMethod());
		updateListField(campaign, newCampaign, Campaign::getVocabularies, Campaign::setVocabularies);
		updateListField(campaign, newCampaign, Campaign::getMotivation, Campaign::setMotivation);
		DB.getCampaignDAO().makePermanent(campaign);
		return ok(Json.toJson(DB.getCampaignDAO().get(campaignDbId)));
	}

	/**
	 * Call to get all campaigns of a project (e.g. CrowdHeritage)
	 * WIll return only public campaigns.
	 * Does not get into account a specific user.
	 *
	 * @param group
	 * @param project
	 * @param state
	 * @param sortBy
	 * @param offset
	 * @param count
	 * @return
	 */
	public static Result getCampaigns(String group, String project, String state, String sortBy, int offset,
			int count) {
		ObjectNode result = Json.newObject();
		List<Campaign> campaigns = new ArrayList<Campaign>();
		campaigns = DB.getCampaignDAO().getCampaigns(group, project, state, sortBy, offset, count);

		if (campaigns == null) {
			result.put("error", "There are not any campaigns for this Project.");
			return internalServerError(result);
		}
		return ok(Json.toJson(campaigns));
	}

	/**
	 *
	 * Get the campaigns that a specific user is creator of. Supports pagination via offset, count
	 *
	 *
	 * @param offset
	 * @param count
	 * @return
	 */
	public static Result getUserCampaigns(int offset, int count) {
		ObjectNode result = Json.newObject();
		List<Campaign> campaigns = new ArrayList<Campaign>();
		ObjectId userId = effectiveUserDbId();
		if (userId == null) {
			return badRequest("User not logged in");
		}
		campaigns = DB.getCampaignDAO().getUserCampaigns(userId, offset, count);
		long total = DB.getCampaignDAO().countUserCampaigns(userId);
		if (campaigns == null) {
			result.put("error", "There are not any campaigns for this user");
			return internalServerError(result);
		}
		result.set("campaigns", Json.toJson(campaigns));
		result.put("count", total);
		return ok(result);
	}

	public static Result incUserPoints(String campaignId, String userId, String annotationType) {
		ObjectId campaignDbId = null;
		if (StringUtils.isNotEmpty(campaignId)) {
			campaignDbId = new ObjectId(campaignId);
		}
		DB.getCampaignDAO().incUserPoints(campaignDbId, userId, annotationType);

		return ok();
	}

	public static Result decUserPoints(String campaignId, String userId, String annotationType) {
		ObjectId campaignDbId = null;
		if (StringUtils.isNotEmpty(campaignId)) {
			campaignDbId = new ObjectId(campaignId);
		}
		DB.getCampaignDAO().decUserPoints(campaignDbId, userId, annotationType);
		return ok();
	}

	private static boolean uniqueCampaignName(String name) {
		return (DB.getCampaignDAO().getCampaignByName(name) == null);
	}

	public static Result resetCampaign(String campaignId) {
		DB.getCampaignDAO().resetCampaignPoints(new ObjectId(campaignId));
//		DB.getAnnotationDAO().deleteCampaignAnnotations(new ObjectId(campaignId));
//		DB.getAnnotationDAO().unscoreAutomaticAnnotations();
		return ok();
	}

	public static Result createEmptyCampaign(String campaignUserName) {
//		ArrayNode errors = Json.newObject().arrayNode();
		ObjectNode error = Json.newObject();
		if (campaignUserName == null) {
			error.put("error", "Please define a name for the campaign");
			return forbidden(error);
		}
		if  (!uniqueCampaignName(campaignUserName)) {
			error.put("error", "Campaign name already exists in DB. Use a different one");
			return forbidden(error);
		}

		ObjectId creator = effectiveUserDbId();
		if (creator == null || !DB.getUserDAO().getById(creator).getCampaignCreationAccess()) {
			error.put("error", "No rights for campaign creation");
			return forbidden(error);
		}

		Campaign newCampaign = new Campaign();
		newCampaign.setCreators(new HashSet<ObjectId>(Arrays.asList(creator)));
		newCampaign.setUsername(campaignUserName);
		newCampaign.setBanner("http://withculture.eu/assets/img/content/background-space.png");
		newCampaign.setAnnotationTarget(1000);
		newCampaign.setAnnotationCurrent(new Campaign.AnnotationCount());
		newCampaign.setContributorsPoints(new Hashtable<ObjectId, AnnotationCount>());
		newCampaign.setCreated(new Date());
		newCampaign.setStartDate(new Date());
		newCampaign.setEndDate(new Date());
		newCampaign.setIsPublic(false);

		newCampaign.setProject("CrowdHeritage");

		newCampaign.setTitle(new Literal("campaign title"));
		newCampaign.setDescription(new Literal("campaign description"));
		newCampaign.setInstructions(new Literal("campaign instructions"));
		newCampaign.setDisclaimer(new Literal("campaign disclaimer"));

		Campaign.BadgePrizes prizes = new Campaign.BadgePrizes();
		prizes.setGold(new Literal(Language.EN, "Congratulations!<br/>Now you've got the <strong>Gold</strong> badge!"));
		prizes.setSilver(new Literal(Language.EN, "Congratulations!<br/>Now you've got the <strong>Silver</strong> badge!"));
		prizes.setBronze(new Literal(Language.EN, "Congratulations!<br/>Now you've got the <strong>Bronze</strong> badge!"));
		prizes.setRookie(new Literal(Language.EN, "Keep annotating to win your badge!"));
		newCampaign.setPrizes(prizes);

		try {
			DB.getCampaignDAO().makePermanent(newCampaign);
		} catch (Exception e) {
			log.error("Cannot save campaign to database!", e.getMessage());
			error.put("error", "Cannot save campaign to database!");
			return internalServerError(error);
		}
		return ok(Json.toJson(newCampaign));

	}

	public static Result createCampaign() {
		Campaign newCampaign = null;
		ArrayNode errors = Json.newObject().arrayNode();
		ObjectNode error = Json.newObject();
		JsonNode json = request().body().asJson();
		try {
			if (json == null) {
				error.put("error", "Invalid JSON body");
				return badRequest(error);
			}
			Class<?> clazz = Class.forName("model.Campaign");
			newCampaign = (Campaign) Json.fromJson(json, clazz);
			// Set Campaign.creator
			ObjectId creator = effectiveUserDbId();
			if (creator == null || !DB.getUserDAO().getById(creator).getCampaignCreationAccess()) {
				error.put("error", "No rights for campaign creation");
				return forbidden(error);
			} else {
				newCampaign.setCreators(new HashSet<ObjectId>(Arrays.asList(creator)));
			}
			if (!json.has("title")) {
				errors.add("Must specify title for the campaign");
			} else if (json.get("title").asText().length() < 3) {
				errors.add("Title of Campaign must contain at least 3 characters");
			}
			if (!json.has("description")) {
				errors.add("Must specify description for the campaign");
			} else if (json.get("description").asText().length() < 3) {
				errors.add("Description of Campaign must contain at least 3 characters");
			}
			if (errors.size() > 0) {
				error.put("error", errors);
				return badRequest(error);
			}
			// Set Campaign.username
			String username = newCampaign.getEnglishTitle().replaceAll("\\s+", "-").toLowerCase();
			int numberOfTries = 2;
			while (!uniqueCampaignName(username) && numberOfTries < 21) {
				username = username + numberOfTries++;
			}
			if (!uniqueCampaignName(username)) {
				errors.add("Campaign name already exists! Please specify another name");
			} else {
				newCampaign.setUsername(username);
			}
			// Set Campaign.banner
			if (!json.has("banner")) {
				newCampaign.setBanner("http://withculture.eu/assets/img/content/background-space.png");
			}
			// Set Campaign.annotationTarget
			if (!json.has("annotationTarget")) {
				newCampaign.setAnnotationTarget(1000);
			} else {
				try {
					int tar = Integer.parseInt(json.get("annotationTarget").asText());
					newCampaign.setAnnotationTarget(tar);
				} catch (NumberFormatException e) {
					error.put("error", "Given annotation target is not a number");
					return badRequest(error);
				}
			}
			// Set Campaign.annotationCurrent
			newCampaign.setAnnotationCurrent(new Campaign.AnnotationCount());
			// Set Campaign.annotationCurrent
			newCampaign.setContributorsPoints(new Hashtable<ObjectId, AnnotationCount>());
			// Set Campaign.startDate and Campaign.endDate
//			if (json.has("startDate")) {
//				SimpleDateFormat sdfs = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
//				try {
//					newCampaign.setStartDate(sdfs.parse(json.get("startDate").asText()));
//				} catch (ParseException e) {
//					error.put("error", "Start date's format is invalid");
//					return badRequest(error);
//				}
//			}
//			if (json.has("endDate")) {
//				SimpleDateFormat sdfe = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
//				try {
//					newCampaign.setEndDate(sdfe.parse(json.get("endDate").asText()));
//				} catch (ParseException e) {
//					error.put("error", "End date's format is invalid");
//					return badRequest(error);
//				}
//			}
			if (errors.size() > 0) {
				error.put("error", errors);
				return badRequest(error);
			}
			newCampaign.setCreated(new Date());
			try {
				DB.getCampaignDAO().makePermanent(newCampaign);
			} catch (Exception e) {
				log.error("Cannot save campaign to database!", e.getMessage());
				error.put("error", "Cannot save campaign to database!");
				return internalServerError(error);
			}
			return ok(Json.toJson(newCampaign));
		} catch (Exception e) {
			error.put("error", e.getMessage());
			return internalServerError(error);
		}
	}

	// public CampaignTerm createCampaignTerm( String literal, String uri, boolean
	// selectable ) {
	// CampaignTerm term = new CampaignTerm();
	// term.labelAndUri.addLiteral(Language.EN, literal);
	// term.selectable = selectable;
	// if (uri == null || uri.equals(""))
	// return term;
	// term.labelAndUri.addURI(uri);
	// String requestUri = uri;
	// if (requestUri.contains("wikidata")) {
	// String[] split = requestUri.split("/");
	//
	// requestUri = Arrays.arrayToString(split);
	// }
	// requestUri = requestUri + ".json";
	// return term;
	//
	// }

	public static void addLangs(CampaignTerm term) throws ClientProtocolException, IOException {
		String[] langs = new String[] { "en", "it", "fr", "pl", "es" };
		if (term.labelAndUri.getURI().contains("wikidata")) {
			term.labelAndUri.addURI(term.labelAndUri.getURI().replace("/wiki/", "/entity/").replace("https", "http"));
			HttpClient client = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(term.labelAndUri.getURI() + ".json");
			HttpResponse response = client.execute(request);
			InputStream jsonStream = response.getEntity().getContent();
			try {
				JsonNode json = Json.parse(jsonStream);
				for (String lang : langs) {
					JsonNode langNode = json.get("entities").fields().next().getValue().get("labels").get(lang);
					if (langNode != null) {
						String langTerm = langNode.get("value").asText();
						term.labelAndUri.addLiteral(Language.getLanguage(lang), langTerm);
					} else {
						String englishTerm = term.labelAndUri.getLiteral(Language.EN);
						term.labelAndUri.addLiteral(Language.getLanguage(lang), englishTerm);
					}
					if (term instanceof CampaignTermWithInfo) {
						JsonNode descNode = json.get("entities").fields().next().getValue().get("descriptions").get(lang);
						if (descNode != null) {
							String desc = descNode.get("value").asText();
							((CampaignTermWithInfo) term).description.addLiteral(Language.getLanguage(lang), desc);
						}
					}
				}
			}
			catch (Exception e) {
				Logger.error(e.getMessage());
			}
		} else {
			try {
				URL u = new URL(term.labelAndUri.getURI() + ".json");
				InputStream jsonStream = u.openStream();
				JsonNode json = Json.parse(jsonStream);
				for (String lang : langs) {
					Iterator<JsonNode> it = json.get("results").get("bindings").iterator();
					while (it.hasNext()) {
						JsonNode node = it.next();
						if (node.get("Object").get("xml:lang") != null
								&& node.get("Predicate").get("value").textValue().contains("skos/core#prefLabel")
								&& Arrays.asList(langs).contains(node.get("Object").get("xml:lang").asText())) {
							String langTerm = node.get("Object").get("value").asText();
							String lan = node.get("Object").get("xml:lang").asText();
							term.labelAndUri.addLiteral(Language.getLanguage(lan), langTerm);
							break;
						}
					}
				}
			} catch (MalformedURLException e) {
				String englishTerm = term.labelAndUri.getLiteral(Language.EN);
				String[] s = englishTerm.split(",");
				if (s.length > 2) {
					Logger.info("Cannot create name");
				} else if (s.length == 2) {
					englishTerm = s[1].trim() + " " + s[0].trim();
				}
				for (String lang : langs) {
					term.labelAndUri.addLiteral(Language.getLanguage(lang), englishTerm);
				}
				Logger.error(e.getMessage());
				;
			}
		}

	}

	public static List<CampaignTerm> createMIMOCampaignTerm(String uri, int level) {
		ArrayList<CampaignTerm> terms = new ArrayList<CampaignTerm>();
		ThesaurusObject thes = DB.getThesaurusDAO().getByUri(uri);
		CampaignTerm term = new CampaignTerm();
		term.labelAndUri.addURI(uri);
		term.labelAndUri.addLiteral(Language.EN, thes.getSemantic().getPrefLabel().getLiteral(Language.EN));
		term.labelAndUri.addLiteral(Language.IT, thes.getSemantic().getPrefLabel().getLiteral(Language.IT));
		term.labelAndUri.addLiteral(Language.FR, thes.getSemantic().getPrefLabel().getLiteral(Language.FR));
		term.selectable = true;
		terms.add(term);
		if (thes.getSemantic().getAltLabel() != null && level == 3) {
			List<String> alts = thes.getSemantic().getAltLabel().get(Language.DEFAULT);
			for (String alt : alts) {
				if (!alt.equalsIgnoreCase(thes.getSemantic().getPrefLabel().getLiteral(Language.EN))) {
					CampaignTerm altTerm = new CampaignTerm();
					altTerm.labelAndUri.addURI(uri);
					altTerm.labelAndUri.addLiteral(Language.EN,
							thes.getSemantic().getPrefLabel().getLiteral(Language.EN));
					altTerm.selectable = true;
					terms.add(altTerm);
				}
			}
		}
		return terms;

	}

	public static void readMIMO() throws Exception {
		Reader in = new FileReader(vocabularyPath + termsFile.get("instruments"));
		Iterable<CSVRecord> records = CSVFormat.DEFAULT.withDelimiter(',').withHeader().parse(in);
		ArrayList<CampaignTerm> terms = new ArrayList<CampaignTerm>();
		CampaignTerm lastOfLevel[];
		lastOfLevel = new CampaignTerm[3];
		for (CSVRecord record : records) {
			if (lastOfLevel[0] == null || !record.get("Level_1").equals("")
					&& !record.get("Level_1").equals(lastOfLevel[0].labelAndUri.getLiteral(Language.EN))) {
				List<CampaignTerm> createdTerms = createMIMOCampaignTerm(record.get("URI"), 0);
				terms.addAll(createdTerms);
				lastOfLevel[0] = createdTerms.get(0);
			}
			for (int i = 2; i <= 3; i++) {
				if (!record.get("Level_" + i).equals("")) {
					List<CampaignTerm> createdTerms = createMIMOCampaignTerm(record.get("URI"), i);
					for (CampaignTerm createdTerm : createdTerms) {
						lastOfLevel[i - 2].addChild(createdTerm);
						lastOfLevel[i - 1] = createdTerm;
					}
				}
			}
		}
	}

	public static Result getPopularAnnotations(String campaignName, String term, int offset, int count) {
		Map<String, Integer> res = DB.getAnnotationDAO().getByCampaign(campaignName, term, offset, count);
		return ok(Json.toJson(res));
	}

	public static Result readCampaignTerms(String cname) throws Exception {
		if (cname.equals("instruments")) {
			readMIMO();
			return ok();
		}
		Reader in = new FileReader(vocabularyPath + termsFile.get(cname));
		Iterable<CSVRecord> records = CSVFormat.DEFAULT.withDelimiter(',').parse(in);
		ArrayList<CampaignTerm> terms = new ArrayList<CampaignTerm>();
		CampaignTerm lastOfLevel[];
		lastOfLevel = new CampaignTerm[3];
		for (CSVRecord record : records) {
			System.out.println("Processing line of CSV: " + record.getRecordNumber());
			if (lastOfLevel[0] == null || !record.get(0).equals(lastOfLevel[0].labelAndUri.getLiteral(Language.EN))) {
				CampaignTerm term = new CampaignTermWithInfo();
				term.labelAndUri.addLiteral(Language.EN, record.get(0));
				if (!StringUtils.isEmpty(record.get(1))) {
					String uri = record.get(1).length() == 0 ? UUID.randomUUID().toString() : record.get(1);
					term.labelAndUri.addURI(uri);
					term.selectable = true;
					addLangs(term);
				}
				else {
					term.selectable = false;
				}
				terms.add(term);
				lastOfLevel[0] = term;
			}
			for (int i = 1; i < (record.size() - 1); i++) {
				if (!record.get(i).equals("")) {
					CampaignTermWithInfo term = new CampaignTermWithInfo();
					term.labelAndUri.addLiteral(Language.EN, record.get(i));
					if (!StringUtils.isEmpty(record.get(3))) {
						term.labelAndUri.addURI(record.get(3));
						term.selectable = true;
						addLangs(term);
					}
					else {
						term.selectable = false;
					}
					lastOfLevel[i - 1].addChild(term);
					lastOfLevel[i] = term;
				}
			}
		}
		Campaign campaign = DB.getCampaignDAO().getCampaignByName(cname);
		campaign.setCampaignTerms(terms);
		DB.getCampaignDAO().makePermanent(campaign);
		return ok();
	}

	/*
	 * private static ObjectNode validateCampaign(JsonNode json) {
	 * 
	 * ObjectNode result = Json.newObject(); ObjectNode error = Json.newObject();
	 * 
	 * String startDate = null; if (!json.has("startDate")) { error.put("startDate",
	 * "Starting date is empty."); }
	 * 
	 * String endDate = null; if (!json.has("endDate")) { error.put("endDate",
	 * "Ending date is empty."); }
	 * 
	 * String description = null; if (!json.has("description")) {
	 * error.put("description", "Description is empty."); }
	 * 
	 * String space = null; if (!json.has("space")) { error.put("space",
	 * "UserGroup is empty."); }
	 * 
	 * String campaignMotivation = null; if (!json.has("campaignMotivation")) {
	 * error.put("campaignMotivation", "Campaign motivation is empty."); }
	 * 
	 * String annotationTarget = null; if (!json.has("annotationTarget")) {
	 * error.put("annotationTarget", "Annotation target is empty."); }
	 * 
	 * String vocabularies = null; if (!json.has("vocabularies")) {
	 * error.put("vocabularies", "Vocabulary list is empty."); }
	 * 
	 * String targetCollections = null; if (!json.has("targetCollections")) {
	 * error.put("targetCollections", "List of target collection is empty."); }
	 * 
	 * String featuredCollections = null; if (!json.has("featuredCollections")) {
	 * error.put("featuredCollections", "List of featured collections is empty."); }
	 * 
	 * if (error.size() != 0) { result.put("error", error); }
	 * 
	 * return result; }
	 */
	public static Result getUserPoints(String userId, String pointType) throws Exception {
		ObjectId withUser = null;
		String type;
		long points = 0;
		ObjectNode result = Json.newObject();
		if (userId != null)
			withUser = new ObjectId(userId);
		else
			withUser = WithController.effectiveUserDbId();

		if (pointType != null)
			type = pointType;
		else
			type = "karmaPoints";

		if (withUser == null)
			return badRequest(Json.parse("{ \"error\" : \"No user defined\" }"));
		List<Campaign> campaigns = new ArrayList<Campaign>();
		campaigns = DB.getCampaignDAO().getUserAnnotatedCampaigns(withUser, pointType);
		Campaign myCampaign;
		if (campaigns == null) {
			result.put("error", "DB error");
			return internalServerError(result);
		} else {
			for (int i = 0; i < campaigns.size(); i++) {
				myCampaign = campaigns.get(i);
				if (type.equals("karmaPoints"))
					points = points + ((myCampaign.getContributorsPoints()).get(withUser)).getKarmaPoints();
				else if (type.equals("created"))
					points = points + ((myCampaign.getContributorsPoints()).get(withUser)).getCreated();
				else if (type.equals("approved"))
					points = points + ((myCampaign.getContributorsPoints()).get(withUser)).getApproved();
				else if (type.equals("rejected"))
					points = points + ((myCampaign.getContributorsPoints()).get(withUser)).getRejected();
				else if (type.equals("rated"))
					points = points + ((myCampaign.getContributorsPoints()).get(withUser)).getRated();
				else
					points = points + ((myCampaign.getContributorsPoints()).get(withUser)).getKarmaPoints();
			}
		}
		result.set(type, Json.toJson(points));
		return ok(result);
	}

	public static Result updateKarma(String campaignId) throws Exception {
		ObjectId campaignDbId = new ObjectId(campaignId);
		ObjectId creatorId;
		Campaign campaign = DB.getCampaignDAO().getCampaign(campaignDbId);
		int i;
		// First we zero the karmaPoints of allUsers in order to recaclulate them
		Hashtable<ObjectId, AnnotationCount> contributors = campaign.getContributorsPoints();
		Set<ObjectId> keys = contributors.keySet();
		for (ObjectId key : keys) {
			contributors.get(key).setKarmaPoints(0);
			DB.getCampaignDAO().resetKarmaPoints(campaignDbId, key.toHexString());
		}
		// Then we search all the Annotations for the ones that belong to the specific
		// Campaign and we update annotation by annotation the karma points of their
		// creator
		List<Annotation> annotations;
		Annotation current;
		annotations = DB.getAnnotationDAO().getCampaignAnnotations(campaignDbId, false);
		for (i = 0; i < annotations.size(); i++) {
			current = annotations.get(i);
			if (current.getScore() != null) {
				if ((current.getScore().getRejectedBy() != null) && (current.getScore().getApprovedBy() != null)) {
					if (current.getScore().getRejectedBy().size() >= current.getScore().getApprovedBy().size()) {
						if (current.getAnnotators() != null) {
							if ((AnnotationAdmin) (current.getAnnotators()).get(0) != null) {
								creatorId = ((AnnotationAdmin) (current.getAnnotators()).get(0)).getWithCreator();
								if (creatorId != null) {
									DB.getCampaignDAO().incUserPoints(campaignDbId, creatorId.toString(),
											"karmaPoints");
								}
							}
						}
					}
				}
			}
		}
		return ok();
	}

	public static Result getContributors(String cname) {
		ObjectNode result = Json.newObject();
		ArrayList<ObjectNode> contributors = new ArrayList<ObjectNode>();
		contributors = DB.getCampaignDAO().getContributors(cname);
		if (contributors == null) {
			result.put("error", "The campaign does not have any contributors yet.");
			return internalServerError(result);
		}
		Comparator<ObjectNode> compareByPoints = new Comparator<ObjectNode>() {
			@Override
			public int compare(ObjectNode o1, ObjectNode o2) {
				Long p1 = o1.get("Total User Contributions").asLong();
				Long p2 = o2.get("Total User Contributions").asLong();
				return p2.compareTo(p1);
			}
		};
		contributors.sort(compareByPoints);
		return ok(Json.toJson(contributors));
	}
	
	public static Result initiateValidation(String campaignId, Boolean allowRejected, int minScore) {
		ObjectId campaignDbId = null;
		if (StringUtils.isNotEmpty(campaignId)) {
			campaignDbId = new ObjectId(campaignId);
		}
		DB.getCampaignDAO().initiateValidation(campaignDbId, allowRejected, minScore);
		DB.getAnnotationDAO().initializeAnnotationsForPublish(campaignDbId, allowRejected, minScore);

		return ok();
	}

	public static Result campaignStatistics(String cname) {
		ObjectNode statistics = DB.getCampaignDAO().campaignStatistics(cname);

		return ok(Json.toJson(statistics));
	}

	public static AnnotationTarget parseAnnotationTarget(JsonNode annotation) {
		JsonNode target = annotation.get("target");
		String source = target.get("source").textValue();
		JsonNode selector = target.get("selector");

		AnnotationTarget annotationTarget = new AnnotationTarget();
		annotationTarget.setExternalId(source);

		if (selector != null) {
			String property = selector.get("property").textValue();
			JsonNode destination = selector.get("destination");
			String destinationValue = destination.get("value").textValue();
			String destinationLang = destination.get("language").textValue();

			PropertyTextFragmentSelector targetSelector = new PropertyTextFragmentSelector();
			targetSelector.setOrigValue(destinationValue);
			targetSelector.setOrigLang(Language.getLanguageByCode(destinationLang));

			if (selector.has("refinedBy")) {
				JsonNode refinedBy = selector.get("refinedBy");
				int start = refinedBy.get("start").asInt();
				int end = refinedBy.get("end").asInt();
				targetSelector.setStart(start);
				targetSelector.setEnd(end);
			}

			targetSelector.setProperty(property);
			annotationTarget.setSelector(targetSelector);
		}
		return annotationTarget;
	}

	public static AnnotationAdmin parseAnnotationAdmin(String campaignName, JsonNode annotation, String userId) {
		AnnotationAdmin administrative = new AnnotationAdmin();
		administrative.setWithCreator(new ObjectId(userId));
		if (annotation.has("created")) {
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
				Date created = sdf.parse(annotation.get("created").asText());
				administrative.setGenerated(created);
				administrative.setCreated(created);
				administrative.setLastModified(new Date());
			}
			catch (ParseException e) {
				administrative.setGenerated(new Date());
				administrative.setCreated(new Date());
				administrative.setLastModified(new Date());
			}
		}
		else {
			administrative.setGenerated(new Date());
			administrative.setCreated(new Date());
			administrative.setLastModified(new Date());
		}
		administrative.setGenerator("CrowdHeritage " + campaignName);

		if (annotation.has("creator")) {
			JsonNode creator = annotation.get("creator");
			if (creator.has("id")) {
				administrative.setExternalCreatorId(creator.get("id").asText());
			}
			if (creator.has("type")) {
				administrative.setExternalCreatorType(CreatorType.valueOf(creator.get("type").asText()));
			}
			if (creator.has("name")) {
				administrative.setExternalCreatorName(creator.get("name").asText());
			}
		}
		if (annotation.has("confidence")) {
			administrative.setConfidence(annotation.get("confidence").asDouble());
		}
		return administrative;
	}

	public static void findCorrespondingRecordResource(AnnotationTarget target) {
		RecordResource r = DB.getRecordResourceDAO().getByAnnotationExternalId(target.getExternalId());
		target.setRecordId(r.getDbId());
		target.setWithURI("/record/" + r.getDbId());
	}

	public static void internalDebiasAnnotationsIngestion(String campaignName, JsonNode json, String userId, String baseAnnotationsUUID) {
		JsonNode annotations = json.get("items");
		Campaign campaign = DB.getCampaignDAO().getCampaignByName(campaignName);

		for (JsonNode annotation : annotations) {
			JsonNode body = annotation.get("body");
			AnnotationBodySubTagging bdy = new AnnotationBodySubTagging();
			bdy.setUri(body.asText());

			ThesaurusObject term = DB.getThesaurusDAO().getByUri(body.asText());
			bdy.setLabel(new MultiLiteral(term.getSemantic().getPrefLabel()));
			bdy.setDescription(new MultiLiteral(term.getSemantic().getDescription()));
			bdy.setUriVocabulary(term.getSemantic().getVocabulary().getName());

			JsonNode targets = annotation.get("target");
			for (JsonNode targetJson : targets) {
				try {

					
					Annotation newAnnotation = new Annotation();
					newAnnotation.setMotivation(MotivationType.SubTagging);
					newAnnotation.setBody(bdy);
					
					AnnotationAdmin administrative = new AnnotationAdmin();
					administrative.setWithCreator(new ObjectId(userId));
					administrative.setGenerated(new Date());
					administrative.setCreated(new Date());
					administrative.setLastModified(new Date());
					administrative.setGenerator("CrowdHeritage " + campaignName);
					administrative.setExternalCreatorType(CreatorType.Software);
					administrative.setExternalCreatorName("DE-BIAS TOOL");
					newAnnotation.setAnnotators(new ArrayList(Arrays.asList(administrative)));

					AnnotationTarget tgt = new AnnotationTarget();
					String externalId = targetJson.get("source").asText();

					// hugo did not send the / in the europeana id so we need to normalize
					RecordResource r = DB.getRecordResourceDAO().getByAnnotationExternalId(externalId);
					if (r == null) {
						externalId = "/" + externalId;
						r = DB.getRecordResourceDAO().getByAnnotationExternalId(externalId);
					}
					tgt.setExternalId(externalId);
					tgt.setRecordId(r.getDbId());
					tgt.setWithURI("/record/" + r.getDbId());

					PropertyTextFragmentSelector selector = new PropertyTextFragmentSelector();
					JsonNode selectorJson = targetJson.get("selector");
					JsonNode refinedBy = selectorJson.get("refinedBy");
					
					selector.setPrefix(refinedBy.get("prefix").asText());
					selector.setSuffix(refinedBy.get("suffix").asText());
					selector.setAnnotatedValue(refinedBy.get("exact").get("@value").asText());

					Language lang = Language.getLanguageByCode(refinedBy.get("exact").get("@language").asText());
					String property = selectorJson.get("hasPredicate").asText();
					selector.setOrigLang(lang);
					// newAnnotation.setScope(scope);
					selector.setProperty(property);
					// we could have more than one values for each language.
					// we check whether the combination of prefix, annotated value and suffix exists as a substring on each candidate value
					String originalValue = "";
					String valueToCheck = selector.getPrefix() + selector.getAnnotatedValue() + selector.getSuffix();
					if (property.equals("dc:title")) {
						for (String val : r.getDescriptiveData().getLabel().get(lang)) {
							if (val.contains(valueToCheck)) {
								originalValue = val;
								break;
							}
						}
					}
					else if (property.equals("dc:description")) {
						for (String val : r.getDescriptiveData().getDescription().get(lang)) {
							if (val.contains(valueToCheck)) {
								originalValue = val;
								break;
							}
						}				
					}
					else if (property.equals("dc:type")) {
						for (String val : ((CulturalObjectData) r.getDescriptiveData()).getDctype().get(lang)) {
							if (val.contains(valueToCheck)) {
								originalValue = val;
								break;
							}
						}                
					}
					else if (property.equals("dc:subject")) {
						for (String val : ((CulturalObjectData) r.getDescriptiveData()).getKeywords().get(lang)) {
							if (val.contains(valueToCheck)) {
								originalValue = val;
								break;
							}
						}  
					}

					selector.setOrigValue(originalValue);

					String prefix = selector.getPrefix();
					String annotatedValue = selector.getAnnotatedValue();
					int start = -1;
					int end = -1;
					if (prefix == null || prefix.isEmpty()) {
						start = 0;
					}
					else {
						start = originalValue.indexOf(prefix) + prefix.length();
					}
					end = start + annotatedValue.length();

					selector.setStart(start);
					selector.setEnd(end);
					tgt.setSelector(selector);
					newAnnotation.setTarget(tgt);
					DB.getAnnotationDAO().makePermanent(newAnnotation);
					newAnnotation.setAnnotationWithURI("/annotation/" + newAnnotation.getDbId());
					DB.getAnnotationDAO().makePermanent(newAnnotation); // is this needed for a second time?
					DB.getRecordResourceDAO().addAnnotation(newAnnotation.getTarget().getRecordId(), newAnnotation.getDbId(),
							userId);
				}
				catch (Exception e) {
                    e.printStackTrace();
                }
			}
		}
	}

	public static void internalNtuaModelAnnotationsImport(String campaignName, MotivationType motivationType, JsonNode json, String userId, String baseAnnotationsUUID) {
		JsonNode annotations = json.get("@graph");
		Campaign campaign = DB.getCampaignDAO().getCampaignByName(campaignName);
		
		List<Campaign.ColorInfo> colorTerminology = null;
		if (motivationType.equals(MotivationType.ColorTagging)) {
			colorTerminology = campaign.getColorTaggingColorsTerminology();
		}

		AtomicInteger successfulAnnotationImports = new AtomicInteger();
		AtomicInteger failedAnnotationImports = new AtomicInteger();
		for (JsonNode annotation : annotations) {
			try {
				JsonNode body = annotation.get("body");
				/*
					We need to parse the body which will either be text (e.g. "body": "http://wikidata.org/entity/Q23"
					or string containing a uri, or an Array (TODO: we will deal with that later).
				*/
				List<AnnotationBody> bodies = new ArrayList<>();

				Function<MotivationType, AnnotationBodyTagging> annotationBodyFactory = (requestedMotivation) -> {
					AnnotationBodyTagging annotationBody;
					if (requestedMotivation.equals(MotivationType.Tagging)) {
						annotationBody = new AnnotationBodyTagging();
					}
					else if (requestedMotivation.equals(MotivationType.ColorTagging)) {
						annotationBody = new AnnotationBodyColorTagging();
					}
					else if (requestedMotivation.equals(MotivationType.SubTagging)) {
						annotationBody = new AnnotationBodySubTagging();
					}
					else {
						annotationBody = new AnnotationBodyImageTagging();
					}
					return annotationBody;
				};

				/* First case: body is an object, so based on NTUA annotation model
				we have the free text annotation option, which translates to Commenting
				*/
				if (body.isObject()) {
					AnnotationBodyCommenting annotationBody = new AnnotationBodyCommenting();
					MultiLiteral label = new MultiLiteral(Language.getLanguageByCode(body.get("language").textValue()), body.get("value").textValue());
					label.fillDEF();
					annotationBody.setLabel(label);
					bodies.add(annotationBody);
				}
				/* Second case: body is an array, so it is some variation of tagging. In this approach,
				we check to see if it is either plain tagging or ColorTagging
				*/
				else if (body.isArray()) {
					for (JsonNode bdy : body) {
						AnnotationBodyTagging annotationBody = annotationBodyFactory.apply(motivationType);
						annotationBody.setUri(bdy.asText());
						if (motivationType.equals(MotivationType.ColorTagging) && colorTerminology != null) {
							String uri = bdy.asText();
							colorTerminology.stream()
								.filter(color -> uri.equals(color.getUri()))
								.findAny()
								.ifPresent(color -> annotationBody.setLabel(new MultiLiteral(color.getLabel()).fillDEF()));
						}
					
						if (motivationType.equals(MotivationType.SubTagging) || motivationType.equals(MotivationType.Tagging)) {
							ThesaurusObject term = DB.getThesaurusDAO().getByUri(bdy.asText());
							annotationBody.setLabel(new MultiLiteral(term.getSemantic().getPrefLabel()));
							annotationBody.setDescription(new MultiLiteral(term.getSemantic().getDescription()));
							annotationBody.setUriVocabulary(term.getSemantic().getVocabulary().getName());
						}
						bodies.add(annotationBody);
					}
				}
				/* Third case: body is a string, so it is some variation of tagging. In this approach,
				we check to see if it is either plain tagging or ColorTagging
				*/ 
				else {
					AnnotationBodyTagging annotationBody = annotationBodyFactory.apply(motivationType);				
					annotationBody.setUri(body.asText());
					if (motivationType.equals(MotivationType.ColorTagging) && colorTerminology != null) {
							String uri = body.asText();
							colorTerminology.stream()
								.filter(color -> uri.equals(color.getUri()))
								.findAny()
								.ifPresent(color -> annotationBody.setLabel(new MultiLiteral(color.getLabel()).fillDEF()));
					}
					if (motivationType.equals(MotivationType.SubTagging) || motivationType.equals(MotivationType.Tagging)) {
						ThesaurusObject term = DB.getThesaurusDAO().getByUri(body.asText());
						annotationBody.setLabel(new MultiLiteral(term.getSemantic().getPrefLabel()));
						annotationBody.setDescription(new MultiLiteral(term.getSemantic().getDescription()));
						annotationBody.setUriVocabulary(term.getSemantic().getVocabulary().getName());
					}
					bodies.add(annotationBody);
				}

				for (AnnotationBody annBody : bodies) {
					Annotation newAnnotation = new Annotation();
					newAnnotation.setMotivation(motivationType);
					newAnnotation.setBody(annBody);

					if (annotation.has("id")) {
						String annotationExternalId = annotation.get("id").asText();
						newAnnotation.setExternalId(annotationExternalId);
					}
					AnnotationAdmin annotationAdmin = parseAnnotationAdmin(campaignName, annotation, userId);
					newAnnotation.setAnnotators(new ArrayList(Arrays.asList(annotationAdmin)));
					
					AnnotationTarget annotationTarget = parseAnnotationTarget(annotation);
					findCorrespondingRecordResource(annotationTarget);

					newAnnotation.setTarget(annotationTarget);
					if (annotation.has("scope")) {
						String scope = annotation.get("scope").asText();
						newAnnotation.setScope(scope);
					}
					DB.getAnnotationDAO().makePermanent(newAnnotation);
					newAnnotation.setAnnotationWithURI("/annotation/" + newAnnotation.getDbId());
					DB.getAnnotationDAO().makePermanent(newAnnotation); // is this needed for a second time?
					DB.getRecordResourceDAO().addAnnotation(newAnnotation.getTarget().getRecordId(), newAnnotation.getDbId(),
							userId);
					successfulAnnotationImports.getAndIncrement();
				}
			}
			catch (Exception e) {
				failedAnnotationImports.getAndIncrement();
			}
			
		}
		final int successfulAnnotationImportsFinal = successfulAnnotationImports.get();
		final int failedAnnotationImportsFinal = failedAnnotationImports.get();

		campaign.getBaseAnnotations().stream()
			.filter(baseAnnotations -> baseAnnotations.uuid.equals(baseAnnotationsUUID))
			.findFirst()
			.ifPresent(baseAnnotations -> {
				
				baseAnnotations.status = successfulAnnotationImportsFinal == 0 
											? Campaign.BaseAnnotationsImportStatus.FAILED 
											: Campaign.BaseAnnotationsImportStatus.COMPLETED;
				baseAnnotations.uploadedAt = new Date();
				baseAnnotations.successCount = successfulAnnotationImportsFinal;
				baseAnnotations.failedCount = failedAnnotationImportsFinal;
				DB.getCampaignDAO().makePermanent(campaign);
			});
	}

	public static Result importDebiasAnnotations(String campaignName) throws ParseException {
		ObjectNode error = Json.newObject();
		JsonNode json = request().body().asJson();
		MotivationType motivationType = MotivationType.SubTagging;
		String userId = WithController.effectiveUserId();
		if (json == null) {
			error.put("error", "Invalid JSON");
			return badRequest(error);
		}

		if (userId == null) {
			error.put("error", "User not logged in");
			return badRequest(error);
		}

		CampaignBaseAnnotationsObject baseAnnotations = new CampaignBaseAnnotationsObject();
		baseAnnotations.source = Campaign.BaseAnnotationsSource.FILE;
		baseAnnotations.status = Campaign.BaseAnnotationsImportStatus.IMPORTING;
		baseAnnotations.startedAt = new Date();
		Campaign campaign = DB.getCampaignDAO().getCampaignByName(campaignName);
		List<CampaignBaseAnnotationsObject> campaignBaseAnnotationsList = campaign.getBaseAnnotations();
		campaignBaseAnnotationsList.add(baseAnnotations);
		campaign.setBaseAnnotations(campaignBaseAnnotationsList);
		DB.getCampaignDAO().makePermanent(campaign);
		
		ParallelAPICall.Priority.BACKEND.getExcecutionContext().execute(() -> 
			internalDebiasAnnotationsIngestion(campaignName, json, userId, baseAnnotations.uuid)
		);
		return ok();

	}

	public static Result importAnnotationsFromNtuaModel(String campaignName, String motivation) throws ParseException {
		ObjectNode error = Json.newObject();
		JsonNode json = request().body().asJson();
		MotivationType motivationType = Annotation.MotivationType.valueOf(motivation);
		String userId = WithController.effectiveUserId();
		if (json == null) {
			error.put("error", "Invalid JSON");
			return badRequest(error);
		}

		if (userId == null) {
			error.put("error", "User not logged in");
			return badRequest(error);
		}
		CampaignBaseAnnotationsObject baseAnnotations = new CampaignBaseAnnotationsObject();
		baseAnnotations.source = Campaign.BaseAnnotationsSource.FILE;
		baseAnnotations.status = Campaign.BaseAnnotationsImportStatus.IMPORTING;
		baseAnnotations.startedAt = new Date();
		Campaign campaign = DB.getCampaignDAO().getCampaignByName(campaignName);
		List<CampaignBaseAnnotationsObject> campaignBaseAnnotationsList = campaign.getBaseAnnotations();
		campaignBaseAnnotationsList.add(baseAnnotations);
		campaign.setBaseAnnotations(campaignBaseAnnotationsList);
		DB.getCampaignDAO().makePermanent(campaign);
		ParallelAPICall.Priority.BACKEND.getExcecutionContext().execute(() -> 
			internalNtuaModelAnnotationsImport(campaignName, motivationType, json, userId, baseAnnotations.uuid)
		);
		return ok();
	}

	public static Result importAnnotationsFromMint(String campaignName, String motivation, String mintUrl) {
		ObjectMapper om = new ObjectMapper();
		MotivationType motivationType = Annotation.MotivationType.valueOf(motivation);
		String userId = WithController.effectiveUserId();
		ObjectNode error = Json.newObject();

		if (userId == null) {
			error.put("error", "User not logged in");
			return badRequest(error);
		}

		// parse request parts, unzip, have a json
		try (GzipCompressorInputStream gzIn =  new GzipCompressorInputStream(IOUtils.getInputStreamFromURLOrClasspathOrFileSystem(mintUrl))){
			final JsonNode json = om.readTree(gzIn);

			CampaignBaseAnnotationsObject baseAnnotations = new CampaignBaseAnnotationsObject();
			baseAnnotations.source = Campaign.BaseAnnotationsSource.MINT;
			baseAnnotations.status = Campaign.BaseAnnotationsImportStatus.IMPORTING;
			baseAnnotations.startedAt = new Date();
			Campaign campaign = DB.getCampaignDAO().getCampaignByName(campaignName);
			List<CampaignBaseAnnotationsObject> campaignBaseAnnotationsList = campaign.getBaseAnnotations();
			campaignBaseAnnotationsList.add(baseAnnotations);
			campaign.setBaseAnnotations(campaignBaseAnnotationsList);
			DB.getCampaignDAO().makePermanent(campaign);
			ParallelAPICall.Priority.BACKEND.getExcecutionContext().execute(() -> 
						internalNtuaModelAnnotationsImport(campaignName, motivationType, json, userId, baseAnnotations.uuid)
			);
		}
		catch (JsonProcessingException e) {
			Logger.error("Failed to read mint json", e);
			return badRequest(e.getMessage());
		}
		catch (IOException e) {
			Logger.error("Failed to read mint json", e);
			return internalServerError(e.getMessage());
		}
		
		return ok();
	}

}
