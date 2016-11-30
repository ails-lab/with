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


package model.annotations.selectors;

import model.annotations.Annotation;

import org.mongodb.morphia.query.Query;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class ImageSVGSelector extends SelectorType {
	
	private String imageWithURL;
	
	private String text;
	private String format;
	
	
	@Override
    public Object clone() throws CloneNotSupportedException {
		ImageSVGSelector c = (ImageSVGSelector)super.clone();
		c.imageWithURL = imageWithURL;
		c.text = text;
		c.format = format;

		return c;
    }
	
	@Override
	public void cleanUp() {
		int p = imageWithURL.indexOf("/media/byUrl?url=");
		if (p >= 0) {
			imageWithURL = imageWithURL.substring(p);
		}
		imageWithURL = imageWithURL.replaceFirst("\\?token=[0-9]+$", "");
	}
	
	@Override
	public void addToQuery(Query<Annotation> q) {
		q.field("target.selector.imageWithURL").equal(imageWithURL);
		
		if (text != null) {
			q.field("target.selector.text").equal(text);
		}
		
		if (format != null) {
			q.field("target.selector.format").equal(format);
		}
	}


	public String getImageWithURL() {
		return imageWithURL;
	}


	public void setImageWithURL(String imageWithURL) {
		this.imageWithURL = imageWithURL;
	}


	public String getText() {
		return text;
	}


	public void setText(String text) {
		this.text = text;
	}


	public String getFormat() {
		return format;
	}


	public void setFormat(String format) {
		this.format = format;
	}


}
