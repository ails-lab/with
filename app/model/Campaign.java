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
import java.util.List;
import model.resources.collection.CollectionObject;
import vocabularies.Vocabulary;

public class Campaign {

	public static enum BadgeType {
		None, Bronze, Silver, Gold
	}
	
	
	private Boolean active; 
	private Date startDate;
	private Date endDate;
	private String description;
	private long target;
	private BadgeType badge;
	private List<Vocabulary> vocabularies;
	private List<CollectionObject> collections;
	
	
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
	
	public BadgeType getBadge(int points) {
		if (points >= 150)
			badge = BadgeType.Gold;
		else if (points >= 100)
			badge = BadgeType.Silver;
		else if (points >= 50)
			badge = BadgeType.Bronze;
		else
			badge = BadgeType.None;
		
		return badge;		
	}

	public List<Vocabulary> getVocabularies() {
		return vocabularies;
	}

	public void setVocabularies(List<Vocabulary> vocabularies) {
		this.vocabularies = vocabularies;
	}

	public List<CollectionObject> getCollections() {
		return collections;
	}

	public void setCollections(List<CollectionObject> collections) {
		this.collections = collections;
	}
	
}
