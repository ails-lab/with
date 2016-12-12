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


package model;

import java.util.Date;

import org.bson.types.ObjectId;

public class Campaign {

	public static enum BadgeType {
		Bronze, Silver, Gold
	}
	
	
	private Boolean active; 
	private Date startDate;
	private Date endDate;
	private String description;
	private long target;
	private BadgeType badge;
	
	
	public BadgeType getBadge(long points) {
		if (points < 50)
			badge = BadgeType.Bronze;
		else if (points < 100)
			badge = BadgeType.Silver;
		else
			badge = BadgeType.Gold;
		
		return badge;		
	}
	
	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public long getTarget() {
		return target;
	}

	public void setTarget(long target) {
		this.target = target;
	}
	
}
