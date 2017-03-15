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
import model.Campaign;


public class CampaignDAO extends DAO<Campaign> {

	public CampaignDAO() {
		super(Campaign.class);
	}
	
	public List<Campaign> getCampaigns(ObjectId groupId, boolean active) {
		
		Query<Campaign> q = this.createQuery();
		
		if (groupId != null) {
			q = q.field("space").equal(groupId);
		}
		
		List<Campaign> campaigns = new ArrayList<Campaign>();
		campaigns = this.find(q).asList();
		
		Date today = new Date();
		List<Campaign> campaignList = new ArrayList<Campaign>();
		
		if (active) {
			for (Campaign campaign : campaigns) {
				if ( (campaign.getStartDate().before(today)) && (campaign.getEndDate().after(today)) ) {
					campaignList.add(campaign);
				}
			}
		}
		else {
			for (Campaign campaign : campaigns) {
				if ( (campaign.getStartDate().after(today)) || (campaign.getEndDate().before(today)) ) {
					campaignList.add(campaign);
				}
			}
		}
		
		return campaignList;
	}
	
	public Campaign getCampaign(ObjectId campaignId) {
		
		if (campaignId == null) {
			return null;
		}
		
		Query<Campaign> q = this.createQuery().field("_id").equal(campaignId);
		
		return this.findOne(q);
	}
	
	public long getCampaignsCount(ObjectId groupId) {
		
		Query<Campaign> q = this.createQuery();
		
		if (groupId != null) {
			q = q.field("space").equal(groupId);
		}
		
		return q.countAll();
	}
	
}
