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


package model.annotations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import model.basicDataTypes.Literal;
import model.annotations.ContextData.ContextDataBody;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExhibitionData extends ContextData<ExhibitionData.ExhibitionAnnotationBody> {
	
	public static enum MediaType {VIDEO, AUDIO};
	public static enum TextPosition {LEFT, RIGHT};

	public ExhibitionData() {
		super();
		this.setTarget(new ContextDataTarget());
		this.body = new ExhibitionAnnotationBody();
		this.contextDataType = ContextDataType.valueOf(this.getClass().getSimpleName());
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ExhibitionAnnotationBody extends ContextDataBody {
		Literal text = new Literal();
		String mediaUrl="";
		String mediaDescription="";
		TextPosition textPosition = TextPosition.RIGHT;
		
		MediaType mediaType = null;

		public Literal getText() {
			return text;
		}
		public void setText(Literal text) {
			this.text = text;
		}
		
		public void setTextPosition(TextPosition textPosition) {
			this.textPosition = textPosition;
		}
		
		public TextPosition getTextPosition() {
			return textPosition;
		}
		
		public String getMediaUrl() {
			return mediaUrl;
		}
		public void setMediaUrl(String audioUrl) {
			this.mediaUrl = audioUrl;
		}

		public String getMediaDescription() {
			return mediaDescription;
		}
		
		public void setMediaDescription(String mediaDescription) {
			this.mediaDescription = mediaDescription;
		}
		
		public void setMediaType(MediaType mediaType) {
			this.mediaType = mediaType;
		}
		
		public MediaType getMediaType() {
			return mediaType;
		}
		@Override
		public String toString() {
			return "ExhibitionAnnotationBody [text=" + text + ", mediaUrl=" + mediaUrl + ", mediaDescription="
					+ mediaDescription + ", textPosition=" + textPosition + ", mediaType=" + mediaType + "]";
		}
		
		
	}

}
