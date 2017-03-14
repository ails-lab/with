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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import model.EmbeddedMediaObject.MediaVersion;
import model.annotations.Annotation.MotivationType;
import utils.Deserializer;
import utils.Serializer;


@Entity("Campaign")
public class Campaign {

	/**
	 * The badge that each user is awarded, depending on his earned points (based on the number/type of his annotations).
	 */
	public static enum BadgeType {
		None, Bronze, Silver, Gold
	}

	public static class AnnotationCount {
		private long createdCount;
		private long approvedCount;
		private long rejectedCount;
		
		public long getCreatedCount() {
			return createdCount;
		}
		public void setCreatedCount(long createdCount) {
			this.createdCount = createdCount;
		}
		public long getApprovedCount() {
			return approvedCount;
		}
		public void setApprovedCount(long approvedCount) {
			this.approvedCount = approvedCount;
		}
		public long getRejectedCount() {
			return rejectedCount;
		}
		public void setRejectedCount(long rejectedCount) {
			this.rejectedCount = rejectedCount;
		}
	}
	
	
	@Id
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId dbId;

	@JsonSerialize(using = Serializer.DateSerializer.class)
	@JsonDeserialize(using = Deserializer.DateDeserializer.class)
	private Date startDate;
	
	@JsonSerialize(using = Serializer.DateSerializer.class)
	@JsonDeserialize(using = Deserializer.DateDeserializer.class)
	private Date endDate;
	

	private String campaignTitle;
	
	private String description;
	
	private HashMap<MediaVersion, EmbeddedMediaObject> campaignBanner;
	
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId space;
	
	/**
	 * The purpose of campaign's annotations [Tagging, GeoTagging, Linking, Commenting, Editing].
	 */
	private List<MotivationType> campaignMotivation;
	
	/**
	 * The goal (number of annotations) of the campaign.
	 */
	private long annotationTarget;
	
	/**
	 * The list of supported thesauri for the annotations.
	 */
	private List<String> vocabularies;
	
	/**
	 * The list of item collections to be annotated in this campaign.
	 */
	@JsonSerialize(using = Serializer.ObjectIdArraySerializer.class)
	private List<ObjectId> targetCollections;
	
	/**
	 * Hashtable with the campaign's contributors and the points they've earned
	 */
	private Hashtable<ObjectId, Integer> contributorsPoints;
		
		

	public BadgeType getBadge(int points) {
		if (points >= 150)
			return  BadgeType.Gold;
		else if (points >= 100)
			return  BadgeType.Silver;
		else if (points >= 50)
			return  BadgeType.Bronze;
		else
			return  BadgeType.None;		
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
	
	public ObjectId getSpace() {
		return space;
	}

	public void setSpace(ObjectId space) {
		this.space = space;
	}
	
	public List<MotivationType> getCampaignMotivation() {
		return campaignMotivation;
	}

	public void setCampaignMotivation(List<MotivationType> campaignMotivation) {
		this.campaignMotivation = campaignMotivation;
	}

	public long getAnnotationTarget() {
		return annotationTarget;
	}

	public void setAnnotationTarget(long annotationTarget) {
		this.annotationTarget = annotationTarget;
	}

	public List<String> getVocabularies() {
		return vocabularies;
	}

	public void setVocabularies(List<String> vocabularies) {
		this.vocabularies = vocabularies;
	}

	public List<ObjectId> getTargetCollections() {
		return targetCollections;
	}

	public void setTargetCollections(List<ObjectId> targetCollections) {
		this.targetCollections = targetCollections;
	}

	public String getCampaignTitle() {
		return campaignTitle;
	}

	public void setCampaignTitle(String campaignTitle) {
		this.campaignTitle = campaignTitle;
	}

	public Hashtable<ObjectId, Integer> getContributorsPoints() {
		return contributorsPoints;
	}

	public void setContributorsPoints(Hashtable<ObjectId, Integer> contributorsPoints) {
		this.contributorsPoints = contributorsPoints;
	}

	public HashMap<MediaVersion, EmbeddedMediaObject> getCampaignBanner() {
		return campaignBanner;
	}

	public void setCampaignBanner(HashMap<MediaVersion, EmbeddedMediaObject> campaignBanner) {
		this.campaignBanner = campaignBanner;
	}

}
