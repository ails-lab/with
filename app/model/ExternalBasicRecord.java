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

import java.time.Year;
import java.util.ArrayList;
import java.util.List;

import model.usersAndGroups.Provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class ExternalBasicRecord {
	
	public static enum RecordType {
		IMAGE("Image"), TEXT("Text"), VIDEO("Video"), SOUND("Sound"), UNKNOWN("Unknown");

		private final String text;

	    private RecordType(final String text) {
	        this.text = text;
	    }

	    @Override
	    public String toString() {
	        return text;
	    }
	}
	
	public enum ItemRights {
		Public("Attribution Alone"), Restricted("Restricted"),
		Permission("Permission"), Modify("Allow re-use and modifications"),
		Commercial("Allow re-use for commercial"),
		Creative_Commercial_Modify("use for commercial purposes modify, adapt, or build upon"),
		Creative_Not_Commercial("NOT Comercial"),
		Creative_Not_Modify("NOT Modify"),
		Creative_Not_Commercial_Modify("not modify, adapt, or build upon, not for commercial purposes"),
		Creative_SA("share alike"),
		Creative_BY("use by attribution"),
		Creative("Allow re-use"),
		RR("Rights Reserved"),
		RRPA("Rights Reserved - Paid Access"),
		RRRA("Rights Reserved - Restricted Access"),
		RRFA("Rights Reserved - Free Access"),
		UNKNOWN("Unknown");

		private final String text;

	    private ItemRights(final String text) {
	        this.text = text;
	    }

	    @Override
	    public String toString() {
	        return text;
	    }

	}
	
	private String externalId;
	
	private String title;
	private String description;
	
	private String creator;
	
	// an optional URL for the thumbnail
	private String thumbnailUrl;

	// url to the provider web page for that record
	private String isShownAt;

	// url to the (full resoultion) content - external on in the WITH db
	private String isShownBy;

	//media type
	private  RecordType type = RecordType.UNKNOWN;

	private ItemRights itemRights;

	private List<String> contributors;
	
	private List<Year> years;
	
	private List<Provider> provenanceChain = new ArrayList<Provider>();
		
	private String subject;
	
	/**
	 * gets the hash value of the url that characterizes the original item the record refers to, 
	 * and is used to determine whether two records in the WITH database correspond to the same object. 
	 * For Europeana externalId=hashValue(isShowBy?null:isShownAt). 
	 * Extracted from the original item metadata.
	 * @return the ID.
	 */
	public String getExternalId() {
		return externalId;
	}

	/**
	 * sets the hash value of the url that characterizes the original item the record refers to, 
	 * and is used to determine whether two records in the WITH database correspond to the same object. 
	 * For Europeana externalId=hashValue(isShowBy?null:isShownAt). 
	 * Extracted from the original item metadata.
	 * @param externalId the new ID.
	 */
	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}
	
	public boolean externalIdNotNull() {
		return !((getIsShownBy() == null || getIsShownBy().isEmpty()) &&
		  (getIsShownAt() == null || getIsShownAt().isEmpty()));
	}
	
	/**
	 * gets a URL to the thumbnail of the object. Extracted from the original item metadata.
	 * @return URL
	 */
	public String getThumbnailUrl() {
		return this.thumbnailUrl;

	}

	/**
	 * sets a URL to the thumbnail of the object. Extracted from the original item metadata.
	 * @param thumbnailUrl URL
	 */
	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

	/**
	 * gets a name by which the item is known. Extracted from the original item metadata.
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * sets a name by which the item is known. Extracted from the original item metadata.
	 * @param title the new title.
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * gets the description of the digital item. Extracted from the original item metadata.
	 * @return the description text.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * sets the description of the digital item. Extracted from the original item metadata.
	 * @param description the new description.
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * gets the entity primarily responsible for making the resource. This may be a person, 
	 * organization or a service. Extracted from the original item metadata.
	 * @return the creator
	 */
	public String getCreator() {
		return creator;
	}

	/**
	 * sets the entity primarily responsible for making the resource. This may be a person, 
	 * organization or a service. Extracted from the original item metadata.
	 * @param creator the new creator
	 */
	public void setCreator(String creator) {
		this.creator = creator;
	}

	/**
	 * gets the URL to the provider web page for that record. Extracted from the original item metadata.
	 * @return
	 */
	public String getIsShownAt() {
		return isShownAt;
	}

	/**
	 * sets the URL to the provider web page for that record. Extracted from the original item metadata.
	 * @param isShownAt URL
	 */
	public void setIsShownAt(String isShownAt) {
		this.isShownAt = isShownAt;
	}

	/**
	 * gets the URL to the (full resolution) content. Extracted from the original item metadata.
	 * @return URL
	 */
	public String getIsShownBy() {
		return isShownBy;
	}

	/**
	 * sets URL to the (full resolution) content. Extracted from the original item metadata.
	 * @param isShownBy URL
	 */
	public void setIsShownBy(String isShownBy) {
		this.isShownBy = isShownBy;
	}

	public RecordType getType() {
		return type;
	}

	public void setType(RecordType type) {
		this.type = type;
	}

	public List<Provider> getProvenanceChain() {
		return provenanceChain;
	}
	
	public void setProvenanceChain() {
	}
	
	public void addProvider(Provider provider, int position) {
		provenanceChain.add(position, provider);
	}
	
	public void addProvider(Provider provider) {
		provenanceChain.add(provider);
	}
	
	public String getSource() {
		if (!provenanceChain.isEmpty())
			return provenanceChain.get(provenanceChain.size()-1).providerName;
		else 
			return "";
	}
	
	public String getRecordIdInSource() {
		if (!provenanceChain.isEmpty())
			return provenanceChain.get(provenanceChain.size()-1).recordId;
		else 
			return "";
	}
	
	public String getRecordUrlInSource() {
		if (!provenanceChain.isEmpty())
			return provenanceChain.get(provenanceChain.size()-1).recordUrl;
		else 
			return "";
	}

	/**
	 * gets the entities responsible for making contributions to the resource. 
	 * Extracted from the original item metadata.
	 * @return the list of contributors.
	 */
	public List<String> getContributors() {
		return contributors;
	}

	/**
	 * sets the entities responsible for making contributions to the resource. 
	 * Extracted from the original item metadata.
	 * @param contributors the new list of contributors
	 */
	public void setContributors(List<String> contributors) {
		this.contributors = contributors;
	}

	/**
	 * gets the list of years associated with important events in the life of the item. 
	 * Usually represents the year in which the original item was created. Extracted 
	 * from the original item metadata.
	 * @return the list of years.
	 */
	public List<Year> getYears() {
		return years;
	}

	/**
	 * sets the list of years associated with important events in the life of the item. 
	 * Usually represents the year in which the original item was created. Extracted 
	 * from the original item metadata.
	 * @param year the new list of years.
	 */
	public void setYears(List<Year> year) {
		this.years = years;
	}
	
	public void addYear(Year year) {
		this.years.add(year);
	}
	
	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	/**
	 * gets the rights statement that applies to the item. 
	 * @see ItemRights
	 * @return the rights value
	 */
	public ItemRights getItemRights() {
		return itemRights;
	}

	/**
	 * sets the rights statement that applies to the item. 
	 * @see ItemRights
	 * @param itemRights the new rights value
	 * 
	 */
	public void setItemRights(ItemRights itemRights) {
		this.itemRights = itemRights;
	}

}
