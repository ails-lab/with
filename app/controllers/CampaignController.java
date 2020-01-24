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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;
import model.Campaign;
import model.Campaign.AnnotationCount;
import model.Campaign.CampaignTerm;
import model.basicDataTypes.Language;
import model.resources.ThesaurusObject;
import model.usersAndGroups.UserGroup;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Result;

public class CampaignController extends WithController {

	public static final ALogger log = Logger.of(CampaignController.class);

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

	public static Result editCampaign(String id) throws ClassNotFoundException, JsonProcessingException, IOException {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		ObjectId campaignDbId = new ObjectId(id);
		Campaign campaign = DB.getCampaignDAO().get(campaignDbId);
		if (json == null) {
			result.put("error", "Invalid JSON");
			return badRequest(result);
		}
		// Class<?> clazz = Class.forName("model.Campaign");
		ObjectMapper mapper = new ObjectMapper();
		mapper.readerForUpdating(campaign).readValue(json);

		// Parse correctly the given dates
		if (json.has("startDate")) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			try {
				campaign.setStartDate(sdf.parse(json.get("startDate").asText()));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		}
		if (json.has("endDate")) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			try {
				campaign.setEndDate(sdf.parse(json.get("endDate").asText()));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		}

		// Campaign campaignChanges = (Campaign) Json.fromJson(json, clazz);
		DB.getCampaignDAO().makePermanent(campaign);
		return ok(Json.toJson(DB.getCampaignDAO().get(campaignDbId)));
	}

	public static Result getCampaigns(String group, String project, String state, String sortBy, int offset,
			int count) {
		ObjectNode result = Json.newObject();
		List<Campaign> campaigns = new ArrayList<Campaign>();
		campaigns = DB.getCampaignDAO().getCampaigns(group, project, state, sortBy, offset, count);
		if (campaigns == null) {
			result.put("error", "There are not any campaigns for this UserGroup.");
			return internalServerError(result);
		}
		return ok(Json.toJson(campaigns));
	}

	public static Result getUserCampaigns(String userId, int offset, int count) {
		ObjectNode result = Json.newObject();
		List<Campaign> campaigns = new ArrayList<Campaign>();
		campaigns = DB.getCampaignDAO().getUserCampaigns(new ObjectId(userId), offset, count);
		long total = DB.getCampaignDAO().countUserCampaigns(new ObjectId(userId));
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

	// TODO get multilingual object from front-end (title and description)
	public static Result createCampaign() {
		Campaign newCampaign = null;
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
			if (creator == null) {
				error.put("error", "No rights for campaign creation");
				return forbidden(error);
			} else {
				newCampaign.setCreators(new HashSet<ObjectId>(Arrays.asList(creator)));
			}
			// Set Campaign.title
			if (!json.has("title")) {
				error.put("error", "Must specify title for the campaign");
				return badRequest(error);
			} else if (json.get("title").asText().length() < 3) {
				error.put("error", "Title of Campaign must contain at least 3 characters");
				return badRequest(error);
			}
			String title = json.get("title").asText();
			newCampaign.setTitle(title);
			// Set Campaign.username
			String username = title.replaceAll("\\s+", "-").toLowerCase();
			if (!uniqueCampaignName(username)) {
				error.put("error", "Campaign name already exists! Please specify another name");
				return badRequest(error);
			} else {
				newCampaign.setUsername(username);
			}
			// Set Campaign.description
			if (!json.has("description")) {
				newCampaign.setDescription("");
			} else {
				newCampaign.setDescription(json.get("description").asText());
			}
			// Set Campaign.space and Campaign.spacename
			if ((!json.has("space")) && (!json.has("spacename"))) {
				newCampaign.setSpace(null);
				newCampaign.setSpacename("");
			} else if ((!json.has("space")) && (json.has("spacename"))) {
				String sname = json.get("spacename").asText();
				UserGroup with = DB.getUserGroupDAO().findOne("username", sname);
				newCampaign.setSpace(with.getDbId());
				newCampaign.setSpacename(sname);
			} else if ((json.has("space")) && (!json.has("spacename"))) {
				UserGroup group = DB.getUserGroupDAO().get(new ObjectId(json.get("space").asText()));
				String sname = group.getUsername();
				newCampaign.setSpace(new ObjectId(json.get("space").asText()));
				newCampaign.setSpacename(sname);
			} else {
				newCampaign.setSpace(new ObjectId(json.get("space").asText()));
				newCampaign.setSpacename(json.get("spacename").asText());
			}
			// Set Campaign.banner
			if (!json.has("banner")) {
				newCampaign.setBanner("");
			} else {
				newCampaign.setBanner(json.get("banner").asText());
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
			if (json.has("startDate")) {
				SimpleDateFormat sdfs = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
				try {
					newCampaign.setStartDate(sdfs.parse(json.get("startDate").asText()));
				} catch (ParseException e) {
					error.put("error", "Start date's format is invalid");
					return badRequest(error);
				}
			}
			if (json.has("endDate")) {
				SimpleDateFormat sdfe = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
				try {
					newCampaign.setEndDate(sdfe.parse(json.get("endDate").asText()));
				} catch (ParseException e) {
					error.put("error", "End date's format is invalid");
					return badRequest(error);
				}
			}

			newCampaign.setCreated(new Date());
			try {
				DB.getCampaignDAO().makePermanent(newCampaign);
			} catch (Exception e) {
				log.error("Cannot save campaign to database!", e.getMessage());
				error.put("error", "Cannot save campaign to database!");
				return internalServerError(error);
			}
			// updatePage(newGroup.getDbId(), json);

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
		String[] langs = new String[] { "it", "nl", "fr", "de", "es", "pl" };
		if (term.labelAndUri.getURI().contains("wikidata")) {
			term.labelAndUri.addURI(term.labelAndUri.getURI().replace("/wiki/", "/entity/"));
			HttpClient client = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(term.labelAndUri.getURI() + ".json");
			HttpResponse response = client.execute(request);
			InputStream jsonStream = response.getEntity().getContent();
			JsonNode json = Json.parse(jsonStream);
			for (String lang : langs) {
				JsonNode langNode = json.get("entities").fields().next().getValue().get("labels").get(lang);
				if (langNode != null) {
					String langTerm = langNode.get("value").asText();
					term.labelAndUri.addLiteral(Language.getLanguage(lang), langTerm);
				}
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
								&& Arrays.asList(langs).contains(node.get("Object").get("xml:lang").asText())) {
							String langTerm = node.get("Object").get("value").asText();
							String lan = node.get("Object").get("xml:lang").asText();
							term.labelAndUri.addLiteral(Language.getLanguage(lan), langTerm);
						}
					}
				}
			} catch (MalformedURLException e) {
				System.out.println(e);
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
					altTerm.labelAndUri.addLiteral(Language.EN, thes.getSemantic().getPrefLabel().getLiteral(Language.EN));
					altTerm.selectable = true;
					terms.add(altTerm);
				}
			}
		}
		return terms;
		
	}
	
	public static void readMIMO() throws Exception {
		Reader in = new FileReader("vocabularies/MIMO-Thesaurus for-campaign.csv");
		Iterable<CSVRecord> records = CSVFormat.DEFAULT.withDelimiter(',').withHeader().parse(in);
		ArrayList<CampaignTerm> terms = new ArrayList<CampaignTerm>();
		CampaignTerm lastOfLevel[];
		lastOfLevel = new CampaignTerm[3];
//		for (CSVRecord record : records) {
//			System.out.println(record.get("URI"));
//			ThesaurusObject thes = DB.getThesaurusDAO().getByUri(record.get("URI"));
//			if (thes.equals(null))
//				System.out.println(record.get("URI"));
//			else
//				System.out.println(thes.getSemantic().getPrefLabel());

//		}
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
					// addLangs(term);
					for (CampaignTerm createdTerm : createdTerms) {
						lastOfLevel[i - 2].addChild(createdTerm);
						lastOfLevel[i - 1] = createdTerm;
					}
				}
			}
		}

		System.out.println(terms);

	}
	
	public static Result getPopularAnnotations(String campaignName, String term, int offset, int count) {
		Map<String, Integer> res = DB.getAnnotationDAO().getByCampaign(campaignName, term, offset, count);
		return ok(Json.toJson(res)) ;
	}

	@SuppressWarnings("unused")
	public static Result readCampaignTerms() throws Exception {
		if (true) {
			readMIMO();
			return ok();
		}
		Reader in = new FileReader(
				"vocabularies/Cities_Landscapes_Means_of_Transport_Vocabulary.csv");
		Iterable<CSVRecord> records = CSVFormat.DEFAULT.withDelimiter(',').parse(in);
		ArrayList<CampaignTerm> terms = new ArrayList<CampaignTerm>();
		CampaignTerm lastOfLevel[];
		lastOfLevel = new CampaignTerm[3];
		int j = 0;
		for (CSVRecord record : records) {
			// if (j++ == 60)
			// break;
			if (lastOfLevel[0] == null || !record.get(0).equals(lastOfLevel[0].labelAndUri.getLiteral(Language.EN))) {
				CampaignTerm term = new CampaignTerm();
				term.labelAndUri.addLiteral(Language.EN, record.get(0));
				term.selectable = false;
				terms.add(term);
				lastOfLevel[0] = term;
			}
			for (int i = 1; i < (record.size() - 1); i++) {
				if (!record.get(i).equals("")) {
					CampaignTerm term = new CampaignTerm();
					term.labelAndUri.addLiteral(Language.EN, record.get(i));
					term.labelAndUri.addURI(record.get(3));
					term.selectable = true;
					addLangs(term);
					lastOfLevel[i - 1].addChild(term);
					lastOfLevel[i] = term;
				}
			}
		}
		Campaign campaign = DB.getCampaignDAO().getCampaignByName("cities-landscapes");
		campaign.setCampaignTerms(terms);
		DB.getCampaignDAO().makePermanent(campaign);
//		FileWriter fileWriter = new FileWriter("/tmp/output.json");
//		fileWriter.write(Json.toJson(terms).toString());
//		fileWriter.close();
		// System.out.println(Json.toJson(terms));
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
}
