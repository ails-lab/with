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
import java.util.Set;

import model.basicDataTypes.LiteralOrResource;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.net.MediaType;

public class EmbeddedMediaObject {
	@Id
	@JsonIgnore
	private ObjectId dbId;

	public static enum WithMediaType {
		VIDEO, IMAGE, TEXT, AUDIO
	}

	// this needs work
	public static enum WithMediaRights {
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

	    private WithMediaRights(final String text) {
	        this.text = text;
	    }

	    @Override
	    public String toString() {
	        return text;
	    }
	}

	private WithMediaType type;
	private Set<WithMediaRights> withRights;

	// if the thumbnail is externally provided
	private String thumbnailUrl;

	// the media objects URL
	private String url;

	// with urls for embedded or cached objects
	private String withUrl;
	private String withThumbnailUrl;


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


	/*
	 * Getters/Setters
	 */

	public ObjectId getDbId() {
		return dbId;
	}

	public void setDbId(ObjectId dbId) {
		this.dbId = dbId;
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
	}

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
}
