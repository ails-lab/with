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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import model.Campaign;
import model.resources.collection.CollectionObject;


public class CampaignDAO extends DAO<Campaign> {

	public CampaignDAO() {
		super(Campaign.class);
	}
	
	public long campaignCount(String groupName) {
				
		Query<Campaign> q = this.createQuery();
		
		if (!groupName.isEmpty()) {
			q = q.field("spacename").equal(groupName);
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
	
	public List<Campaign> getCampaigns(String groupName, boolean active, int offset, int count) {
		
		Query<Campaign> q = this.createQuery();
		
		if (!groupName.isEmpty()) {
			q = q.field("spacename").equal(groupName);
		}
		
		Date today = new Date();
		if (active) {
			q = q.field("startDate").lessThanOrEq(today).field("endDate").greaterThanOrEq(today);
		}
		else {
			q.or(
				q.criteria("startDate").greaterThanOrEq(today),
				q.criteria("endDate").lessThanOrEq(today)
			);
		}

		q = q.offset(offset).limit(count);
		
		List<Campaign> campaigns = new ArrayList<Campaign>();
		campaigns = this.find(q).asList();

		return campaigns;
	}
	
	public long getCampaignsCount(ObjectId groupId) {
		
		Query<Campaign> q = this.createQuery();
		
		if (groupId != null) {
			q = q.field("space").equal(groupId);
		}
		return q.countAll();
	}
		
	public void incUserPoints(ObjectId campaignId, String userid, String annotType) {
		Query<Campaign> q = this.createQuery().field("_id").equal(campaignId);
		
		UpdateOperations<Campaign> updateOps1 = this
				.createUpdateOperations().disableValidation();
		updateOps1.inc("contributorsPoints."+userid+"."+annotType);
		this.update(q, updateOps1);
		
		UpdateOperations<Campaign> updateOps2 = this
				.createUpdateOperations().disableValidation();
		updateOps2.inc("annotationCurrent."+annotType);
		this.update(q, updateOps2);
	}
	
	public void decUserPoints(ObjectId campaignId, String userid, String annotType) {
		Query<Campaign> q = this.createQuery().field("_id").equal(campaignId);
		
		UpdateOperations<Campaign> updateOps1 = this
				.createUpdateOperations().disableValidation();
		updateOps1.dec("contributorsPoints."+userid+"."+annotType);
		this.update(q, updateOps1);
		
		UpdateOperations<Campaign> updateOps2 = this
				.createUpdateOperations().disableValidation();
		updateOps2.dec("annotationCurrent."+annotType);
		this.update(q, updateOps2);
	}
	
}
