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
import search.Fields;
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
		VIDEO, IMAGE, TEXT, AUDIO, 
		THREED{
			@Override
			public String getName() {
				return "3D";
			}
		}, 
		OTHER{

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
		/**
		 * <h1>Public Domain Mark</h1>
		 * Our Public Domain Mark enables works that are no longer restricted by copyright to be marked as such in a standard and simple way, making them easily discoverable and available to others. Many cultural heritage institutions including museums, libraries and other curators are knowledgeable about the copyright status of paintings, books and manuscripts, photographs and other works in their collections, many of which are old and no longer under copyright.  The Public Domain Mark operates as a tag or a label, allowing institutions like those as well as others with such knowledge to communicate that a work is no longer restricted by copyright and can be freely used by others.  The mark can also be an important source of information, allowing others to verify a work’s copyright status and learn more about the work.
		 */
		Public("The Public Domain Mark (PDM)"),
		
		/**
		 * <h1>No Rights Reserved</h1>
		 * <h2>CC0</h2>
		 * CC0 enables scientists, educators, artists and other creators and owners of copyright- or database-protected content to waive those interests in their works and thereby place them as completely as possible in the public domain, so that others may freely build upon, enhance and reuse the works for any purposes without restriction under copyright or database law.
		 */
		PublicCC0("No Rights Reserved CC0"),
		
		
		// From CC
		
		
		/**
		 *  <h1>Attribution</h1>
		 *  <h2>CC BY</h2>
		 *  This license lets others distribute, remix, tweak, and build upon your work, even commercially, as long as they credit you for the original creation. This is the most accommodating of licenses offered. Recommended for maximum dissemination and use of licensed materials. 
		 */
		Creative_BY("Attribution"),
		
		/**
		 *  <h1>Attribution-ShareAlike</h1>
		 *  <h2>CC BY-SA</h2>
		 *  This license lets others remix, tweak, and build upon your work even for commercial purposes, as long as they credit you and license their new creations under the identical terms. This license is often compared to “copyleft” free and open source software licenses. All new works based on yours will carry the same license, so any derivatives will also allow commercial use. This is the license used by Wikipedia, and is recommended for materials that would benefit from incorporating content from Wikipedia and similarly licensed projects. 
		 */
		Creative_BY_SA("Attribution ShareAlike"),
		
		/**
		 *  <h1>Attribution-NonCommercial</h1>
		 *  <h2>CC BY-NC</h2>
		 *  This license lets others remix, tweak, and build upon your work non-commercially, and although their new works must also acknowledge you and be non-commercial, they don’t have to license their derivative works on the same terms. 
		 */
		Creative_BY_NC("Attribution NonCommercial"),
		
		/**
		 *  <h1>Attribution-NoDerivs</h1> <h2>CC BY-ND</h2>
		 *  This license allows for redistribution, commercial and non-commercial, as long as it is passed along unchanged and in whole, with credit to you. 
		 */
		Creative_BY_ND("Attribution NoDerivs"),
		
		/**
		 *  <h1>Attribution-NonCommercial-ShareAlike</h1>
		 *  <h2>CC BY-NC-SA</h2>
		 *  This license lets others remix, tweak, and build upon your work non-commercially, as long as they credit you and license their new creations under the identical terms. 
		 */
		Creative_BY_NC_SA("Attribution NonCommercial-ShareAlike"),
		
		/**
		 *  <h1>Attribution-NonCommercial-NoDerivs</h1>
		 *  <h2>CC BY-NC-ND</h2> This license is the most restrictive of our six main licenses, only allowing others to download your works and share them with others as long as they credit you, but they can’t change them in any way or use them commercially. 
		 */
		Creative_BY_NC_ND("Attribution NonCommercial-NoDerivs"),
		
		
		// InC
		
		
		/**
		 * <h1>In Copyright (InC)</h1>
		 * The InC statement is for use with in copyright Digital Objects which are freely available online and where re-use requires additional permission from the rights holder(s).
		 * What else do I need to know? This rights statement should be used for objects where any re-use is subject to additional permission from the rights holder(s), or you do want or you are not authorised to allow re-use of the Digital Object.
		 * When I use this statement, what information does the user see?  The user will be directed to http://rightsstatements.org/vocab/InC/1.0/
		 * TODO: check if this includes all the ones bellow 
		 */
		InC("In Copyright (InC)"),
		
		/**
		 * <h1>IN COPYRIGHT - EU ORPHAN WORK</h1>
		 * This Rights Statement is intended for use with Items for which the underlying Work has been identified as an Orphan Work in accordance with Directive 2012/28/EU of the European Parliament and of the Council of 25 October 2012 on certain permitted uses of Orphan Works. It can only be applied to Items derived from Works that are covered by the Directive: Works published in the form of books, journals, newspapers, magazines or other writings as well as cinematographic or audiovisual works and phonograms (note: this excludes photography and visual arts). It can only be applied by organizations that are beneficiaries of the Directive: publicly accessible libraries, educational establishments and museums, archives, film or audio heritage institutions and public-service broadcasting organizations, established in one of the EU member states. The beneficiary is also expected to have registered the work in the EU Orphan Works Database maintained by EUIPO.
		 * URI: http://rightsstatements.org/vocab/InC-OW-EU/1.0/
		 */
		InC_OW_EU("IN COPYRIGHT - EU ORPHAN WORK"),
		
		/**
		 * <h1>IN COPYRIGHT - EDUCATIONAL USE PERMITTED</h1>
		 * This Rights Statement can be used only for copyrighted Items for which the organization making the Item available is the rights-holder or has been explicitly authorized by the rights-holder(s) to allow third parties to use their Work(s) for educational purposes without first obtaining permission.
		 * URI: <a>http://rightsstatements.org/vocab/InC-EDU/1.0/</a>
		 */
		InC_EDU("IN COPYRIGHT - EDUCATIONAL USE PERMITTED"),
		
		/**
		 * <h1>IN COPYRIGHT - NON-COMMERCIAL USE PERMITTED</h1>
		 * This Rights Statement can be used only for copyrighted Items for which the organization making the Item available is the rights-holder or has been explicitly authorized by the rights-holder(s) to allow third parties to use their Work(s) for non-commercial purposes without obtaining permission first.
		 * <a>http://rightsstatements.org/vocab/InC-NC/1.0/
		 */
		InC_NC("IN COPYRIGHT - NON-COMMERCIAL USE PERMITTED"),
		
		/**
		 * <h1>IN COPYRIGHT - RIGHTS-HOLDER(S) UNLOCATABLE OR UNIDENTIFIABLE</h1>
		 * This Rights Statement is intended for use with an Item that has been identified as in copyright but for which no rights-holder(s) has been identified or located after some reasonable investigation. This Rights Statement should only be used if the organization that intends to make the Item available is reasonably sure that the underlying Work is in copyright. This Rights Statement is not intended for use by EU-based organizations who have identified works as Orphan Works in accordance with the EU Orphan Works Directive (they must use InC-OW-EU instead).
		 * <a>http://rightsstatements.org/vocab/InC-RUU/1.0/

		 */
		InC_RUU("IN COPYRIGHT - RIGHTS-HOLDER(S) UNLOCATABLE OR UNIDENTIFIABLE"),
		
		
				
		// NC
					
		/**
		 * <h1>NO COPYRIGHT - CONTRACTUAL RESTRICTIONS</h1>
		 * This Rights Statement can only be used for Items that are in the Public Domain but for which the organization that intends to make the Item available has entered into contractual agreement that requires it to take steps to restrict third party uses of the Item. In order for this Rights Statement to be conclusive, the organization that intends to make the Item available should provide a link to a page detailing the contractual restrictions that apply to the use of the Item.
		 * <a>http://rightsstatements.org/vocab/NoC-CR/1.0/
		 */
		NoC_CR("NO COPYRIGHT - CONTRACTUAL RESTRICTIONS"),
		
		/**
		 * <h1>NO COPYRIGHT - NON-COMMERCIAL USE ONLY</h1>
		 * This Rights Statement can only be used for Works that are in the Public Domain and have been digitized in a public-private partnership as part of which, the partners have agreed to limit commercial uses of this digital representation of the Work by third parties. It has been developed specifically to allow the inclusion of Works that have been digitized as part of the partnerships between European Libraries and Google, but can in theory be applied to Items that have been digitized in similar public-private partnerships.
		 * <a> http://rightsstatements.org/vocab/NoC-NC/1.0/
		 */
		NoC_NC("NO COPYRIGHT - NON-COMMERCIAL USE ONLY"),
		
		/**
		 * <h1>NO COPYRIGHT - OTHER KNOWN LEGAL RESTRICTIONS</h1>
		 * This Rights Statement should be used for Items that are in the Public Domain but that cannot be freely re-used as the consequence of known legal restrictions that prevent the organization that intends to make the Item available from allowing free re-use of the Item, such as cultural heritage or traditional cultural expression protections. In order for this Rights Statement to be conclusive, the organization that intends to make the Item available should provide a link to a page detailing the legal restrictions that limit re-use of the Item.
		 * <a>http://rightsstatements.org/vocab/NoC-OKLR/1.0/
		 */
		NoC_OKLR("NO COPYRIGHT - OTHER KNOWN LEGAL RESTRICTIONS"),
		
		
		/**
		 * <h1>NO COPYRIGHT - UNITED STATES</h1>
		 * This Rights Statement should be used for Items for which the organization that intends to make the Item available has determined are free of copyright under the laws of the United States. This Rights Statement should not be used for Orphan Works (which are assumed to be in-copyright) or for Items where the organization that intends to make the Item available has not undertaken an effort to ascertain the copyright status of the underlying Work.
		 * <a>http://rightsstatements.org/vocab/NoC-US/1.0/
		 */
		NoC_US("NO COPYRIGHT - UNITED STATES"),
		
		/**
		 * <h1>COPYRIGHT NOT EVALUATED</h1>
		 * This Rights Statement should be used for Items for which the copyright status is unknown and for which the organization that intends to make the Item available has not undertaken an effort to determine the copyright status of the underlying Work.
		 * <a>http://rightsstatements.org/vocab/CNE/1.0/
		 */
		CNE("COPYRIGHT NOT EVALUATED"),
		
		/**
		 * <h1>COPYRIGHT UNDETERMINED</h1>
		 * This Rights Statement should be used for Items for which the copyright status is unknown and for which the organization that has made the Item available has undertaken an (unsuccessful) effort to determine the copyright status of the underlying Work. Typically, this Rights Statement is used when the organization is missing key facts essential to making an accurate copyright status determination.
		 * <a>http://rightsstatements.org/vocab/UND/1.0/
		 */
		UND("COPYRIGHT UNDETERMINED"),
		
		/**
		 * <h1>NO KNOWN COPYRIGHT</h1>
		 * This Rights Statement should be used for Items for which the copyright status has not been determined conclusively, but for which the organization that intends to make the Item available has reasonable cause to believe that the underlying Work is not covered by copyright or related rights anymore. 
		 * This Rights Statement should not be used for Orphan Works (which are assumed to be in-copyright) or for Items where the organization that intends to make the Item available has not undertaken an effort to ascertain the copyright status of the underlying Work.
		 * <a>http://rightsstatements.org/vocab/NKC/1.0/
		 */
		UNKNOWN("NO KNOWN COPYRIGHT"),
//		UNKNOWN("Unknown"),
		/**
		 * general
		 */
		// TODO: public domain now...
		OUT_OF_COPYRIGHT("Out of Copyright"),
		
//		RRPA("Rights Reserved - Paid Access"),
//		RRRA("Rights Reserved - Restricted Access"),
//		RRFA("Rights Reserved - Free Access"),
		PROVIDER_SPECIFIC("Provider specific rights statement");
		

		// database seems to contain other WithMediaRights, temporarily we adding them here to
		// make it work... We need to find out why this happens
//		@Deprecated
//		Modify("Modify"),
//		@Deprecated
//		Restricted("Restricted"),
//		@Deprecated
//		Creative_SA("Creative SA"),
//		@Deprecated
//		Permission("Permission granted"),
//		@Deprecated
//		Creative_Not_Commercial_Modify("NOT comercial modify"),
//		@Deprecated
//		Creative("Creative"),
//		@Deprecated
//		Creative_Not_Modify("Not Modify"),
//		@Deprecated
//		Creative_Not_Commercial("Not Comercial");
//		
		
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

	/**
	 * media (image, video, audio, text) quality
	 * @author gardero
	 *
	 */
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
			url = url.startsWith("http:https:") ? url.replaceFirst("http:", "") : url;
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
