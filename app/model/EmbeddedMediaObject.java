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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import model.basicDataTypes.LiteralOrResource;
import utils.MediaTypeConverter;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.net.MediaType;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Converters(MediaTypeConverter.class)
public class EmbeddedMediaObject {


	public static enum WithMediaType {
		VIDEO, IMAGE, TEXT, AUDIO
	}

	// this needs work
	public static enum WithMediaRights {
		Public("Attribution Alone"), 
		Restricted("Restricted"),
		Permission("Permission"), 
		Modify("Allow re-use and modifications"),
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

	    private WithMediaRights(final String text) {
	        this.text = text;
	    }

	    @Override
	    public String toString() {
	        return text;
	    }
	    
	}
	
	private int width, height;
	
	private int thumbWidth, thumbHeight;
	
	private WithMediaType type;
	
	private Set<WithMediaRights> withRights;
	
	// if the thumbnail is externally provided
	private String thumbnailUrl;
	// the media objects URL
	private String url;
    
	/*These do not have to be saved in the db
	 just returned in the json, i.e. the json has 
	 a field withThumbnailUrl computed based on 
	 whether there exists a MediaObject in the db* *
	// with urls for embedded or cached objects
	private String withUrl;
	private String withThumbnailUrl;*/
	
	
	private LiteralOrResource originalRights;
	/*
	 *  file name type values specified here:
	 *  http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/net/MediaType.html
	 */

	
	private MediaType mimeType;
	
	public static enum Quality {
		UNKNOWN, IMAGE_SMALL, IMAGE_500k, IMAGE_1, IMAGE_4, VIDEO_SD, VIDEO_HD,
		AUDIO_8k, AUDIO_32k, AUDIO_256k, TEXT_IMAGE, TEXT_TEXT
	}
	
	//in KB
	private long size;
	
	private Quality quality;
	
	
	
	
	
	
	
	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}
	
	public Quality getQuality() {
		return quality;
	}

	public void setQuality(Quality quality) {
		this.quality = quality;
	}

	
	public WithMediaType getType() {
		return type;
	}

	public void setType(WithMediaType type) {
		this.type = type;
	}

	public Set<WithMediaRights> getWithRights() {
		return withRights;
	}

	public void setWithRights(Set<WithMediaRights> withRights) {
		this.withRights = withRights;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	/*
	public String getWithUrl() {
		return withUrl;
	}

	public void setWithUrl(String withUrl) {
		this.withUrl = withUrl;
	}

	public String getWithThumbnailUrl() {
		return withThumbnailUrl;
	}

	public void setWithThumbnailUrl(String withThumbnailUrl) {
		this.withThumbnailUrl = withThumbnailUrl;
	}*/

	public LiteralOrResource getOriginalRights() {
		return originalRights;
	}

	public void setOriginalRights(LiteralOrResource originalRights) {
		this.originalRights = originalRights;
	}
	
	public MediaType getMimeType() {
		return mimeType;
	}

	public void setMimeType(MediaType mimeType) {
		this.mimeType = mimeType;
	}
	
	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	public int getHeight() {
		return height;
	}
	public void setHeight(int height) {
		this.height = height;
	}
	

	public int getThumbWidth() {
		return thumbWidth;
	}

	public void setThumbWidth(int thumbWidth) {
		this.thumbWidth = thumbWidth;
	}

	public int getThumbHeight() {
		return thumbHeight;
	}

	public void setThumbHeight(int thumbHeight) {
		this.thumbHeight = thumbHeight;
	}

}
