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

import model.BasicDataTypes.LiteralOrResource;

import org.bson.types.ObjectId;

import com.google.common.net.MediaType;

public class EmbeddedMediaObject {
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
	private MediaType mimeType;
	
	public static enum Quality {
		UNKNOWN, IMAGE_SMALL, IMAGE_500k, IMAGE_1, IMAGE_4, VIDEO_SD, VIDEO_HD,
		AUDIO_8k, AUDIO_32k, AUDIO_256k, TEXT_IMAGE, TEXT_TEXT			
	}		

}
