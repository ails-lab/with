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

import java.net.URLEncoder;

import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.net.MediaType;

import model.basicDataTypes.LiteralOrResource;
import play.Logger;
import play.Logger.ALogger;
import sources.core.Utils;
import utils.Deserializer;
import utils.MediaTypeConverter;
import utils.Serializer;

/**
 * @author Enrique Matos Alfonso (gardero@gmail.com)
 *
 */
@Entity
@JsonIgnoreProperties(value={"empty"},ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Converters(MediaTypeConverter.class)
public class EmbeddedMediaObject {
	public static final ALogger log = Logger.of( EmbeddedMediaObject.class );

	private static final int MEGA_PIXEL = 1000000;
	
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


		},
		// WEBPAGE indicates a media that is only playable in given URL
		// it could be because of restricted access or players not 
		// generally available. The recommended way is an iframe ... 
		WEBPAGE;

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
		PublicCC0("CC0"),
		Restricted("Restricted"),
		OUT_OF_COPYRIGHT("Out of Copyright"),
		
		// From CC
		
		Creative_BY("Use by attribution"),
		Creative_BY_SA("Share alike"),
		Creative_BY_NC("NOT Comercial"),
		Creative_BY_ND("NOT Modify"),
		Creative_BY_NC_SA("Share alike, not for commercial purposes"),
		Creative_BY_NC_ND("not modify, adapt, or build upon, not for commercial purposes"),
		
		RR("Rights Reserved"),
		RRPA("Rights Reserved - Paid Access"),
		RRRA("Rights Reserved - Restricted Access"),
		RRFA("Rights Reserved - Free Access"),
		PROVIDER_SPECIFIC("Provider specific rights statement"),
		UNKNOWN("Unknown"),

		// database seems to contain other WithMediaRights, temporarily we adding them here to
		// make it work... We need to find out why this happens
		Modify("You can do stuff to it??"),
		Creative_SA("Should probably be BY_SA"),
		Permission("Permission granted"),
		
		Creative("Use as you wish");
		
		
		private final String text;

		private WithMediaRights(final String text) {
			this.text = text;
		}

		@Override
		public String toString() {
			return text;
		}
		public static WithMediaRights getRights(String code){
			if (Utils.hasInfo(code)){
				for (WithMediaRights v : WithMediaRights.values()) {
					if (v.toString().equals(code) || v.name().equals(code))
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
	private String url= "";

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
		/**
		 * Unknown media quality.
		 */
		UNKNOWN,
		/**
		 * Poor media quality. </br>
		 * Images: small images, 1 MP or less. 
		 * Videos: 360p or less.
		 * Audio: 8 kHz or less.
		 */
		POOR,
		/**
		 * Medium media quality. </br>
		 * Images: less than 4 MP.
		 * Videos: 480p.
		 * Audio: 44.1 kHz or less.
		 */
		MEDIUM,
		/**High media quality. </br>
		 * Images: 4 MP or more.
		 * Videos: 720p or 1080p.
		 * Audio: More than 44.1 kHz.
		 */
		HIGH,
		
		IMAGE_SMALL, IMAGE_500k, IMAGE_1, IMAGE_4, VIDEO_SD, VIDEO_HD, AUDIO_8k, AUDIO_32k, AUDIO_256k, TEXT_IMAGE, TEXT_TEXT
	}
	
	public void computeQuality() {
		switch (type) {
		case IMAGE:
			int mp = getWidth()*getHeight();
			if (mp < MEGA_PIXEL)
				setQuality(Quality.POOR);
			else  if (mp < 4*MEGA_PIXEL){
				setQuality(Quality.MEDIUM);
			} else
				setQuality(Quality.HIGH);
			break;

		default:
			setQuality(Quality.UNKNOWN);
			break;
		}
				
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
		else {
			try {
			return "/media/byUrl?url=" + URLEncoder.encode(url,"UTF-8") + "&version=" + mediaVersion.toString();
			} catch( Exception e ) {
				log.error( "UTF-8 not known ...");
				return "Invalid";
			}
		}
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
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EmbeddedMediaObject other = (EmbeddedMediaObject) obj;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}

	public boolean isEmpty(){
		return Utils.hasInfo(url);
	}

	@Override
	public String toString() {
		return "EmbeddedMediaObject [" + mediaVersion + " " + url + "]";
	}
	
      
}
