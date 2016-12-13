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
import model.annotations.Annotation.MotivationType;
import model.resources.collection.CollectionObject;
import model.usersAndGroups.UserGroup;
import vocabularies.Vocabulary;


public class Campaign {

	public static enum BadgeType {
		None, Bronze, Silver, Gold
	}

	
	
	/**
	 * The badge that each user is awarded, depending on his earned points (based on the number/type of his annotations).
	 * Badge types are: [None, Bronze, Silver, Gold].
	 */
	private BadgeType badge;
	
	/**
	 * A flag that shows if the campaign is currently active.
	 */
	private Boolean active;
	
	/**
	 * The date the campaign begins.
	 */
	private Date startDate;
	
	/**
	 * The date the campaign finishes.
	 */
	private Date endDate;
	
	/**
	 * A short description of the campaign's aim.
	 */
	private String description;
	
	/**
	 * The user group which launches the campaign.
	 */
	private UserGroup space;
	
	/**
	 * The purpose of campaign's annotations [Tagging, Linking, Commenting, Editing].
	 */
	private MotivationType campaignMotivation;
	
	/**
	 * The goal (number of annotations) of the campaign.
	 */
	private long annotationTarget;
	
	/**
	 * The list of supported thesauri for the annotations.
	 */
	private List<Vocabulary> vocabularies;
	
	/**
	 * The list of item collections to be annotated in this campaign.
	 */
	private List<CollectionObject> targetCollections;
	
	/**
	 * The list of item collections that appear in the front page.
	 */
	private List<CollectionObject> featuredCollections;
	
	
	
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
	
	public UserGroup getSpace() {
		return space;
	}

	public void setSpace(UserGroup space) {
		this.space = space;
	}
	
	public MotivationType getCampaignMotivation() {
		return campaignMotivation;
	}

	public void setCampaignMotivation(MotivationType campaignMotivation) {
		this.campaignMotivation = campaignMotivation;
	}

	public long getAnnotationTarget() {
		return annotationTarget;
	}

	public void setAnnotationTarget(long annotationTarget) {
		this.annotationTarget = annotationTarget;
	}
	
	public List<Vocabulary> getVocabularies() {
		return vocabularies;
	}

	public void setVocabularies(List<Vocabulary> vocabularies) {
		this.vocabularies = vocabularies;
	}

	public List<CollectionObject> getTargetCollections() {
		return targetCollections;
	}

	public void setTargetCollections(List<CollectionObject> targetCollections) {
		this.targetCollections = targetCollections;
	}

	public List<CollectionObject> getFeaturedCollections() {
		return featuredCollections;
	}

	public void setFeaturedCollections(List<CollectionObject> featuredCollections) {
		this.featuredCollections = featuredCollections;
	}

}
