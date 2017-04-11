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
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import db.DB;
import model.Campaign;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Result;


public class CampaignController extends WithController {
	
	public static final ALogger log = Logger.of(AnnotationController.class);
	
	public static Result getCampaignCount() {
		
		long count = DB.getCampaignDAO().campaignCount();
		
		return ok(Json.toJson(count));
	}
	
	public static Result getCampaign(String campaignId) {

		ObjectId campaignDbId = new ObjectId(campaignId);
		Campaign campaign = DB.getCampaignDAO().getCampaign(campaignDbId);
		
		return ok(Json.toJson(campaign));
	}
	
	public static Result getActiveCampaigns(String groupId, int offset, int count) {
		ObjectNode result = Json.newObject();
		
		ObjectId groupDbId = null;
		if (StringUtils.isNotEmpty(groupId)) {
			groupDbId = new ObjectId(groupId);
		}
		
		List<Campaign> campaigns = new ArrayList<Campaign>();
		campaigns = DB.getCampaignDAO().getCampaigns(groupDbId, true, offset, count);
		
		if (campaigns == null) {
			result.put("error", "There are not any active campaigns for this UserGroup.");
			return internalServerError(result);
		}
		
		return ok(Json.toJson(campaigns));
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
	
	/*
	private static ObjectNode validateCampaign(JsonNode json) {
		
		ObjectNode result = Json.newObject();
		ObjectNode error = Json.newObject();
		
		String startDate = null;
		if (!json.has("startDate")) {
			error.put("startDate", "Starting date is empty.");
		}
		
		String endDate = null;
		if (!json.has("endDate")) {
			error.put("endDate", "Ending date is empty.");
		}
		
		String description = null;
		if (!json.has("description")) {
			error.put("description", "Description is empty.");
		}
		
		String space = null;
		if (!json.has("space")) {
			error.put("space", "UserGroup is empty.");
		}
		
		String campaignMotivation = null;
		if (!json.has("campaignMotivation")) {
			error.put("campaignMotivation", "Campaign motivation is empty.");
		}
		
		String annotationTarget = null;
		if (!json.has("annotationTarget")) {
			error.put("annotationTarget", "Annotation target is empty.");
		}
		
		String vocabularies = null;
		if (!json.has("vocabularies")) {
			error.put("vocabularies", "Vocabulary list is empty.");
		}
		
		String targetCollections = null;
		if (!json.has("targetCollections")) {
			error.put("targetCollections", "List of target collection is empty.");
		}
		
		String featuredCollections = null;
		if (!json.has("featuredCollections")) {
			error.put("featuredCollections", "List of featured collections is empty.");
		}
		
		if (error.size() != 0) {
			result.put("error", error);
		}
		
		return result;
	}
	
	public static Result createCampaign() {
		
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		
	}
	*/
}
