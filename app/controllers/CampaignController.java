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

import db.DB;
import model.Campaign;
import play.Logger;
import play.Logger.ALogger;


public class CampaignController extends WithController {
	
	public static final ALogger log = Logger.of(AnnotationController.class);
	
	public static Result getActiveCampaigns(String group) {
		
		ObjectId groupId = null;
		if (StringUtils.isNotEmpty(group)) {
			groupId = new ObjectId(group);
		}
		
		List<Campaign> campaigns = new ArrayList<Campaign>();
		campaigns = DB.getCampaignDAO().getActiveCampaigns(groupId);
		
		for (Campaign campaign : campaigns) {
			
		}
	}
}
