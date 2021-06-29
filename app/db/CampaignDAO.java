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


package db;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import controllers.CampaignController;
import model.Campaign;
import model.Campaign.AnnotationCount;
import model.Campaign.PublishCriteria;
import model.usersAndGroups.User;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;

public class CampaignDAO extends DAO<Campaign> {
	
	public static final ALogger log = Logger.of(CampaignController.class);

	public CampaignDAO() {
		super(Campaign.class);
	}

	public long campaignCount(String groupName, String project, String state) {

		Query<Campaign> q = this.createQuery();

		if (!groupName.isEmpty()) {
			q = q.field("spacename").equal(groupName);
		}
		if (project == "") {
			q = q.field("project").equal("WITHcrowd");
		}
		else {
			q = q.field("project").equal(project);
		}

		Date today = new Date();
		if (state.equals("active")) {
			q = q.field("startDate").lessThanOrEq(today).field("endDate").greaterThanOrEq(today);
		}
		else if (state.equals("inactive")) {
			q = q.field("startDate").lessThanOrEq(today).field("endDate").lessThanOrEq(today);
		}
		else if (state.equals("upcoming")) {
			q = q.field("startDate").greaterThanOrEq(today).field("endDate").greaterThanOrEq(today);
		}
		else if (state.equals("all")) {
			q = q.field("startDate").exists().field("endDate").exists();
		}

		return q.countAll();
	}

	public Campaign getCampaign(ObjectId campaignId) {

		Query<Campaign> q = this.createQuery().field("_id").equal(campaignId);
		return this.findOne(q);
	}

	public Campaign getCampaignByName(String cname) {

		Query<Campaign> q = this.createQuery().field("username").equal(cname);
		return this.findOne(q);
	}

	public List<Campaign> getCampaigns(String groupName, String project, String state, String sortBy, int offset, int count) {
		Query<Campaign> q = this.createQuery();
		
		if (!groupName.isEmpty()) {
			q = q.field("spacename").equal(groupName);
		}
		if (!project.isEmpty()) {
			q = q.field("project").equal(project);
		}
		
		Date today = new Date();
		if (state.equals("active")) {
			q = q.field("startDate").lessThanOrEq(today).field("endDate").greaterThanOrEq(today);
		}
		else if (state.equals("inactive")) {
			q = q.field("startDate").lessThanOrEq(today).field("endDate").lessThanOrEq(today);
		}
		else if (state.equals("upcoming")) {
			q = q.field("startDate").greaterThanOrEq(today).field("endDate").greaterThanOrEq(today);
		}
		else if (state.equals("all")) {
			q = q.field("startDate").exists().field("endDate").exists();
		}
				
		if (sortBy.equals("Date_asc")) {
			q = q.order("startDate").offset(offset).limit(count);
		} else if (sortBy.equals("Date_desc")) {
			q = q.order("-startDate").offset(offset).limit(count);
		} else if (sortBy.equals("Alphabetical")) {
			q = q.order("title").offset(offset).limit(count);
		} else {
			q = q.offset(offset).limit(count);
		}
		List<Campaign> campaigns = new ArrayList<Campaign>();
		campaigns = this.find(q).asList();
		return campaigns;
	}

	public List<Campaign> getUserCampaigns(ObjectId userId, int offset, int count) {
		Query<Campaign> q = this.createQuery();
		q.field("creators").equal(userId);
		q = q.offset(offset).limit(count);
		List<Campaign> campaigns = new ArrayList<Campaign>();
		campaigns = this.find(q).asList();
		return campaigns;
	}

	public long countUserCampaigns(ObjectId userId) {
		Query<Campaign> q = this.createQuery();
		q.field("creators").equal(userId);
		long count = this.count(q);
		return count;
	}

	public List<Campaign> getUserAnnotatedCampaigns(ObjectId userId, String pointType) {
		Query<Campaign> q = this.createQuery();
		String userExists = "contributorsPoints." + userId.toString();
		q.criteria(userExists).exists();
		List<Campaign> campaigns = new ArrayList<Campaign>();
		campaigns = this.find(q).asList();

		return campaigns;
	}

	public void incUserPoints(ObjectId campaignId, String userId, String annotType) {
		Query<Campaign> q = this.createQuery().field("_id").equal(campaignId);
		
		UpdateOperations<Campaign> updateOps1 = this.createUpdateOperations().disableValidation();
		updateOps1.inc("contributorsPoints." + userId + "." + annotType);
		this.update(q, updateOps1);

		if (!annotType.equals("records")) {
			UpdateOperations<Campaign> updateOps2 = this.createUpdateOperations().disableValidation();
			updateOps2.inc("annotationCurrent." + annotType);
			this.update(q, updateOps2);
		}
	}
	
	public void resetCampaignPoints(ObjectId campaignId) {
		Query<Campaign> q = this.createQuery().field("_id").equal(campaignId);
		UpdateOperations<Campaign> updateOps = this.createUpdateOperations().disableValidation();
		updateOps.set("contributorsPoints", Json.newObject());
		updateOps.set("annotationCurrent", Json.newObject());
		this.update(q, updateOps);
	}

	public void resetKarmaPoints(ObjectId campaignId, String userId) {
		Query<Campaign> q = this.createQuery().field("_id").equal(campaignId);
		UpdateOperations<Campaign> updateOps = this.createUpdateOperations().disableValidation();
		updateOps.set("contributorsPoints." + userId + ".karmaPoints", 0);
		this.update(q, updateOps);
	}

	public void decUserPoints(ObjectId campaignId, String userId, String annotType) {
		Query<Campaign> q = this.createQuery().field("_id").equal(campaignId);

		UpdateOperations<Campaign> updateOps1 = this.createUpdateOperations().disableValidation();
		updateOps1.dec("contributorsPoints." + userId + "." + annotType);
		this.update(q, updateOps1);

		if (!annotType.equals("records")) {
			UpdateOperations<Campaign> updateOps2 = this.createUpdateOperations().disableValidation();
			updateOps2.dec("annotationCurrent." + annotType);
			this.update(q, updateOps2);
		}
	}

	public void editCampaign(ObjectId dbId, JsonNode json) {
		Query<Campaign> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<Campaign> updateOps = this.createUpdateOperations();
		updateFields("", json, updateOps);
		this.update(q, updateOps);
	}
	
	public ArrayList<ObjectNode> getContributors(String cname) {
		ArrayList<ObjectNode> contributors = new ArrayList<ObjectNode>();
		
		Query<Campaign> q = this.createQuery().field("username").equal(cname);
		Hashtable<ObjectId, AnnotationCount> points = this.findOne(q).getContributorsPoints();
        for (ObjectId key: points.keySet()) {		
        	Query<User> q1 = DB.getUserDAO().createQuery().field("_id").equal(key);
        	User u = DB.getUserDAO().findOne(q1);
        	ObjectNode entry = Json.newObject();
        	entry.put("First Name", u.getFirstName());
        	entry.put("Last Name", u.getLastName());
        	entry.put("E-Mail", u.getEmail());
        	entry.put("Username", u.getUsername());
        	entry.put("Points", points.get(key).getCreated() + points.get(key).getApproved() + points.get(key).getRejected());
        	contributors.add(entry);
        }
        
        return contributors;
	}
	
	public void initiateValidation(ObjectId campaignId, Boolean allowRejected, int minScore) {
		Query<Campaign> q = this.createQuery().field("_id").equal(campaignId);
		
		UpdateOperations<Campaign> updateOps1 = this.createUpdateOperations();
		PublishCriteria value = new Campaign.PublishCriteria();
		value.setAllowRejected(allowRejected);
		value.setMinScore(minScore);
		updateOps1.set("publishCriteria", value);
		this.update(q, updateOps1);
	}

}
