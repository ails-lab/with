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

import utils.Deserializer;
import utils.MediaTypeConverter;
import utils.Serializer;

import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.net.MediaType;

import model.basicDataTypes.LiteralOrResource;
import sources.core.Utils;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Converters(MediaTypeConverter.class)
public class EmbeddedMediaObject {

	public static enum MediaVersion {
		Original, Medium, Thumbnail, Square, Tiny;

		public static boolean contains(String version) {
			for (MediaVersion v : MediaVersion.values()) {
				if (v.name().equals(version)) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * the text used to represent the value is defined by the getName() property
	 * in order to give support to values like "3D" which is not a valid id for an
	 * Enum value.
	 * @author Enrique Matos Alfonso (gardero@gmail.com)
	 *
	 */
	public static enum WithMediaType {
		VIDEO, IMAGE, TEXT, AUDIO, THREED{
			@Override
			public String getName() {
				return "3D";
			}
		}, OTHER{

			@Override
			public boolean isKnown() {
				return false;
			}


		};

		public static WithMediaType getType(String string) {
			for (WithMediaType v : WithMediaType.values()) {
				if (v.toString().equals(string) || v.getName().equals(string))
					return v;
			}
			return OTHER;
		}

		public boolean isKnown() {
			return true;
		}

		public String getName() {
			return this.name();
		}

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
		public static WithMediaRights getRighs(String code){
			if (Utils.hasInfo(code)){
				for (WithMediaRights v : WithMediaRights.values()) {
					if (v.toString().equals(code))
						return v;
				}
			}
			return UNKNOWN;
		}
	}

	private int width, height;

	private WithMediaType type;

	private WithMediaRights withRights;

	// the media object URL
	private String url= "empty";

	private MediaVersion mediaVersion;

	public MediaVersion getMediaVersion() {
		return mediaVersion;
	}

	public void setMediaVersion(MediaVersion mediaVersion) {
		this.mediaVersion = mediaVersion;
	}

	private LiteralOrResource originalRights;
	/*
	 * file name type values specified here:
	 * http://docs.guava-libraries.googlecode
	 * .com/git/javadoc/com/google/common/net/MediaType.html
	 */
	@JsonSerialize(using = Serializer.MimeTypeSerializer.class)
	@JsonDeserialize(using = Deserializer.MimeTypeDeserializer.class)
	private MediaType mimeType = MediaType.ANY_IMAGE_TYPE;

	public static enum Quality {
		UNKNOWN, IMAGE_SMALL, IMAGE_500k, IMAGE_1, IMAGE_4, VIDEO_SD, VIDEO_HD, AUDIO_8k, AUDIO_32k, AUDIO_256k, TEXT_IMAGE, TEXT_TEXT
	}

	// in KB
	private long size;

	private Quality quality;

	/*
	 * Getters/Setters
	 */
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

	public WithMediaRights getWithRights() {
		return withRights;
	}

	public void setWithRights(WithMediaRights withRights) {
		this.withRights = withRights;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getWithUrl() {
		if ((url == null) || url.isEmpty() || url.startsWith("/media") || (mediaVersion == null)) {
			return url;
		}
		else
			return "/media/byUrl?url=" + url + "&version=" + mediaVersion.toString();
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

	public EmbeddedMediaObject(int width, int height, WithMediaType type,
			WithMediaRights withRights, String url, MediaVersion mediaVersion,
			LiteralOrResource originalRights, MediaType mimeType, long size,
			Quality quality) {
		this.width = width;
		this.height = height;
		this.type = type;
		this.withRights = withRights;
		this.url = url;
		this.mediaVersion = mediaVersion;
		this.originalRights = originalRights;
		this.mimeType = mimeType;
		this.size = size;
		this.quality = quality;
	}

	/**
	 * Copy constructor.
	 */
	public EmbeddedMediaObject(EmbeddedMediaObject media) {
		this(media.getWidth(), media.getHeight(), media.getType(), media
				.getWithRights(), media.getUrl(), media.getMediaVersion(),
				media.getOriginalRights(), media.getMimeType(),
				media.getSize(), media.getQuality());
	}

	public EmbeddedMediaObject() {
	}
}
