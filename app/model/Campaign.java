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
	
	public static enum CampaignPurpose {
		Annotate, Validate
	}

	public static class AnnotationCount {
		private long created;
		private long approved;
		private long rejected;
		private long records;
		
		public long getCreated() {
			return created;
		}
		public void setCreated(long created) {
			this.created = created;
		}
		public long getApproved() {
			return approved;
		}
		public void setApproved(long approved) {
			this.approved = approved;
		}
		public long getRejected() {
			return rejected;
		}
		public void setRejected(long rejected) {
			this.rejected = rejected;
		}
		public long getRecords() {
			return records;
		}
		public void setRecords(long records) {
			this.records = records;
		}
	}
	
	public static class BadgeRanges {
		private int bronze;
		private int silver;
		private int gold;
		
		public int getBronze() {
			return bronze;
		}
		public void setBronze(int bronze) {
			this.bronze = bronze;
		}
		public int getSilver() {
			return silver;
		}
		public void setSilver(int silver) {
			this.silver = silver;
		}
		public int getGold() {
			return gold;
		}
		public void setGold(int gold) {
			this.gold = gold;
		}
	}
	
	
	@Id
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId dbId;

	@JsonSerialize(using = Serializer.DateSerializer.class)
	private Date startDate;
	
	@JsonSerialize(using = Serializer.DateSerializer.class)
	private Date endDate;
	

	private String username;
	
	private String title;
	
	private String description;
	
	private String banner;
	
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId space;
	
	private String spacename;
	
	/**
	 * The purpose of campaign's annotations [Tagging, GeoTagging, Linking, Commenting, Editing].
	 */
	private List<MotivationType> campaignMotivation;
	
	private BadgeRanges badges;
	
	private CampaignPurpose purpose;
	
	/**
	 * The goal (number of annotations) of the campaign.
	 */
	private long annotationTarget;
	
	private AnnotationCount annotationCurrent;
	
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
	private Hashtable<ObjectId, AnnotationCount> contributorsPoints;



	public BadgeType getBadge(int points) {
		if (points >= badges.gold)
			return  BadgeType.Gold;
		else if (points >= badges.silver)
			return  BadgeType.Silver;
		else if (points >= badges.bronze)
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
		return title;
	}
	public void setCampaignTitle(String campaignTitle) {
		this.title = campaignTitle;
	}

	public Hashtable<ObjectId, AnnotationCount> getContributorsPoints() {
		return contributorsPoints;
	}
	public void setContributorsPoints(Hashtable<ObjectId, AnnotationCount> contributorsPoints) {
		this.contributorsPoints = contributorsPoints;
	}

	public String getCampaignBanner() {
		return banner;
	}
	public void setCampaignBanner(String campaignBanner) {
		this.banner = campaignBanner;
	}

	
	public AnnotationCount getAnnotationCurrent() {
		return annotationCurrent;
	}
	public void setAnnotationCurrent(AnnotationCount annotationCurrent) {
		this.annotationCurrent = annotationCurrent;
	}
	

	public BadgeRanges getBadges() {
		return badges;
	}
	public void setBadges(BadgeRanges badges) {
		this.badges = badges;
	}

	public CampaignPurpose getPurpose() {
		return purpose;
	}
	public void setPurpose(CampaignPurpose purpose) {
		this.purpose = purpose;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getSpacename() {
		return spacename;
	}

	public void setSpacename(String spacename) {
		this.spacename = spacename;
	}

}
