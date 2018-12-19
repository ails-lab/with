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

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import db.DB;
import model.Campaign;
import model.Campaign.AnnotationCount;
import model.usersAndGroups.User;
import model.usersAndGroups.UserGroup;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Result;

public class CampaignController extends WithController {

	public static final ALogger log = Logger.of(CampaignController.class);

	public static Result getCampaignCount(String group, boolean active) {
		long count = DB.getCampaignDAO().campaignCount(group, active);
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
		Class<?> clazz = Class.forName("model.Campaign");
		ObjectMapper mapper = new ObjectMapper();
		mapper.readerForUpdating(campaign).readValue(campaign);
//		Campaign campaignChanges = (Campaign) Json.fromJson(json, clazz);
		DB.getCampaignDAO().editCampaign(campaignDbId, json);
		return ok(Json.toJson(DB.getCampaignDAO().get(campaignDbId)));
	}
	
	public static Result getActiveCampaigns(String group, String sortBy, int offset, int count) {
		ObjectNode result = Json.newObject();
		List<Campaign> campaigns = new ArrayList<Campaign>();
		campaigns = DB.getCampaignDAO().getCampaigns(group, true, sortBy, offset, count);
		if (campaigns == null) {
			result.put("error", "There are not any active campaigns for this UserGroup.");
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
				newCampaign.setCreator(creator);
			}
			// Set Campaign.title
			if (!json.has("campaignTitle")) {
				error.put("error", "Must specify title for the campaign");
				return badRequest(error);
			} else if (json.get("campaignTitle").asText().length() < 3) {
				error.put("error", "Title of Campaign must contain at least 3 characters");
				return badRequest(error);
			}
			String title = json.get("campaignTitle").asText();
			newCampaign.setCampaignTitle(title);
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
			if (!json.has("campaignBanner")) {
				newCampaign.setCampaignBanner("");
			} else {
				newCampaign.setCampaignBanner(json.get("campaignBanner").asText());
			}
			// Set Campaign.annotationTarget
			if (!json.has("annotationTarget")) {
				newCampaign.setAnnotationTarget(0);
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
				SimpleDateFormat sdfs = new SimpleDateFormat("yyyy-MM-dd");
				try {
					newCampaign.setStartDate(sdfs.parse(json.get("startDate").asText()));
				} catch (ParseException e) {
					error.put("error", "Start date's format is invalid");
					return badRequest(error);
				}
			}
			if (json.has("endDate")) {
				SimpleDateFormat sdfe = new SimpleDateFormat("yyyy-MM-dd");
				try {
					newCampaign.setStartDate(sdfe.parse(json.get("startDate").asText()));
				} catch (ParseException e) {
					error.put("error", "End date's format is invalid");
					return badRequest(error);
				}
			}
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
