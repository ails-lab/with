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
	private  RecordType type;

	private ItemRights itemRights;

	private List<String> contributors;
	
	private List<Year> year;
	
	private List<Provider> provenanceChain = new ArrayList<Provider>();
	
	private String subject;
	
	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}
	
	public String getThumbnailUrl() {
		return this.thumbnailUrl;

	}

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public String getIsShownAt() {
		return isShownAt;
	}

	public void setIsShownAt(String isShownAt) {
		this.isShownAt = isShownAt;
	}

	public String getIsShownBy() {
		return isShownBy;
	}

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

	public List<String> getContributors() {
		return contributors;
	}

	public void setContributors(List<String> contributors) {
		this.contributors = contributors;
	}

	public List<Year> getYear() {
		return year;
	}

	public void setYear(List<Year> year) {
		this.year = year;
	}
	
	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	public ItemRights getItemRights() {
		return itemRights;
	}

	public void setItemRights(ItemRights itemRights) {
		this.itemRights = itemRights;
	}

}
