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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import model.annotations.Annotation.MotivationType;
import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.basicDataTypes.LiteralOrResource;
import utils.Deserializer;
import utils.Deserializer.LiteralDesiarilizer;
import utils.Serializer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Entity("Campaign")
public class Campaign {

	public Campaign() {
		this.contributorsPoints = new Hashtable<ObjectId, AnnotationCount>();
	}

	/**
	 * The badge that each user is awarded, depending on his earned points (based on the number/type of his annotations).
	 */
	public static enum BadgeType {
		None, Bronze, Silver, Gold
	}
	
	public static enum CampaignPurpose {
		Annotate, Validate
	}
	
	public static class CampaignTerm {
		public List<CampaignTerm> children;
		public LiteralOrResource labelAndUri = new LiteralOrResource();
		public boolean selectable;
		
		public void addChild(CampaignTerm child) {
			if (this.children == null)
				this.children = new ArrayList<CampaignTerm>();
			this.children.add(child);
		}
	}
	
	public static class CampaignTermWithInfo extends CampaignTerm {
		public String imageUrl;
		public Literal description = new Literal();
	}
	
	public static class AnnotationCount {
		private long created = 0;
		private long approved = 0;
		private long rejected = 0;
		private long records = 0;
		private long karmaPoints = 0;

		public AnnotationCount() {
			this.created = 0;
			this.approved = 0;
			this.rejected = 0;
			this.records = 0;
			this.karmaPoints = 0;
		}
		
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
		public long getKarmaPoints() {
			return karmaPoints;
		}
		public void setKarmaPoints(long karmaPoints) {
			this.karmaPoints = karmaPoints;
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
	
	public static class BadgePrizes {
		@JsonDeserialize(using = Deserializer.LiteralEnglishDefaultDesiarilizer.class)
		private Literal bronze;
		@JsonDeserialize(using = Deserializer.LiteralEnglishDefaultDesiarilizer.class)
		private Literal silver;
		@JsonDeserialize(using = Deserializer.LiteralEnglishDefaultDesiarilizer.class)
		private Literal gold;
		@JsonDeserialize(using = Deserializer.LiteralEnglishDefaultDesiarilizer.class)
		private Literal rookie;
		
		public Literal getBronze() {
			return bronze;
		}
		public void setBronze(Literal bronze) {
			this.bronze = bronze;
		}
		public Literal getSilver() {
			return silver;
		}
		public void setSilver(Literal silver) {
			this.silver = silver;
		}
		public Literal getGold() {
			return gold;
		}
		public void setGold(Literal gold) {
			this.gold = gold;
		}
		public Map<String, String> getRookie() {
			return rookie;
		}
		public void setRookie(Literal rookie) {
			this.rookie = rookie;
		}
	}
	
	
	@Id
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId dbId;

	@JsonSerialize(using = Serializer.DateSerializer.class)
	@JsonDeserialize(using = Deserializer.DateExtendedDeserializer.class)
	private Date startDate;
	
	@JsonSerialize(using = Serializer.DateSerializer.class)
	@JsonDeserialize(using = Deserializer.DateExtendedDeserializer.class)
	private Date endDate;
	
	@JsonSerialize(using = Serializer.DateSerializer.class)
	@JsonDeserialize(using = Deserializer.DateExtendedDeserializer.class)
	private Date created;
	
	private List<CampaignTerm> campaignTerms;

	private String username;
	
	@JsonDeserialize(using = Deserializer.LiteralEnglishDefaultDesiarilizer.class)
	private Literal title;
	
	@JsonDeserialize(using = Deserializer.LiteralEnglishDefaultDesiarilizer.class)
	private Literal description;
	
	@JsonDeserialize(using = Deserializer.LiteralEnglishDefaultDesiarilizer.class)
	private Literal instructions;
	
	private String banner;
	
	private String logo;
	
	@JsonDeserialize(using = Deserializer.LiteralEnglishDefaultDesiarilizer.class)
	private Literal disclaimer;
	
	@JsonSerialize(using = Serializer.ObjectIdArraySerializer.class)
//	@JsonDeserialize(using = Deserializer.ObjectIdArraySerializer.class)
	private Set<ObjectId> creators = new HashSet<ObjectId>();
	
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	@JsonDeserialize(using = Deserializer.ObjectIdDeserializer.class)	
	private ObjectId space;
	
	private String spacename;
	
	/**
	 * The purpose of campaign's annotations [Tagging, GeoTagging, Linking, Commenting, Editing, Polling].
	 */
	private List<MotivationType> motivation;
	
	private BadgeRanges badges;
	
	@JsonDeserialize(using = Deserializer.BadgePrizesDeserializer.class)
	private BadgePrizes prizes;

	
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
	private List<ObjectId> targetCollections = new ArrayList<ObjectId>();
	
	/**
	 * Hashtable with the campaign's contributors and the points they've earned
	 */
	private Hashtable<ObjectId, AnnotationCount> contributorsPoints;
	
	/**
	 * Field that specifies under which Project the WithCrowd platform is deployed
	 */
	private String project;
	
	

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
	
	public Boolean getActive() {
		Date today = new Date();
		return ( (today.compareTo(this.startDate)>0) && (today.compareTo(this.endDate)<0) );
	}
	
	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
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

	public Literal getDescription() {
		return description;
	}
	public void setDescription(Literal description) {
		this.description = description;
	}
	
	public ObjectId getSpace() {
		return space;
	}
	public void setSpace(ObjectId space) {
		this.space = space;
	}
	
	public List<MotivationType> getMotivation() {
		return motivation;
	}
	public void setMotivation(List<MotivationType> motivation) {
		this.motivation = motivation;
	}

	public long getAnnotationTarget() {
		return annotationTarget;
	}
	public void setAnnotationTarget(long annotationTarget) {
		if (annotationTarget != 0)
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

	public Literal getTitle() {
		return title;
	}
	public String getEnglishTitle() {
		return title.getLiteral(Language.EN);
	}
	public void setTitle(Literal title) {
		this.title = title;
	}
	
	public Hashtable<ObjectId, AnnotationCount> getContributorsPoints() {
		return contributorsPoints;
	}
	public void setContributorsPoints(Hashtable<ObjectId, AnnotationCount> contributorsPoints) {
		this.contributorsPoints = contributorsPoints;
	}

	public String getBanner() {
		return banner;
	}
	public void setBanner(String banner) {
		this.banner = banner;
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

	public Set<ObjectId> getCreators() {
		return creators;
	}

	public void setCreators(Set<ObjectId> creators) {
		this.creators = creators;
	}

	public String getLogo() {
		return logo;
	}

	public void setLogo(String logo) {
		this.logo = logo;
	}
	
	public Literal getDisclaimer() {
		return disclaimer;
	}
	public void setDisclaimer(Literal disclaimer) {
		this.disclaimer = disclaimer;
	}

	public List<CampaignTerm> getCampaignTerms() {
		return campaignTerms;
	}

	public void setCampaignTerms(List<CampaignTerm> campaignTerms) {
		this.campaignTerms = campaignTerms;
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public BadgePrizes getPrizes() {
		return prizes;
	}

	public void setPrizes(BadgePrizes prizes) {
		this.prizes = prizes;
	}

	public Literal getInstructions() {
		return instructions;
	}
	public void setInstructions(Literal instructions) {
		this.instructions = instructions;
	}

}
