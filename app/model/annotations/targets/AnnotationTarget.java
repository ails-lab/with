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


package model.annotations.targets;

import model.annotations.selectors.SelectorType;

import org.bson.types.ObjectId;

import utils.Deserializer;
import utils.Serializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class AnnotationTarget implements Cloneable {

	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId recordId;
	/**
	 * The withURI to which the annotation refers.
	 */
	private String withURI;

	/**
	 * The external id of the object to which the annotations refers.
	 */
	private String externalId;
	
	@JsonDeserialize(using = Deserializer.SelectorTypeDeserializer.class)
	private SelectorType selector;

	public ObjectId getRecordId() {
		return recordId;
	}

	public void setRecordId(ObjectId recordId) {
		this.recordId = recordId;
	}

	public String getWithURI() {
		return withURI;
	}

	public void setWithURI(String withURI) {
		this.withURI = withURI;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public SelectorType getSelector() {
		return selector;
	}

	@Override
	public String toString() {
		return "AnnotationTarget{" +
				"recordId=" + recordId +
				", withURI='" + withURI + '\'' +
				", externalId='" + externalId + '\'' +
				", selector=" + selector +
				'}';
	}

	public void setSelector(SelectorType selector) {
		this.selector = selector;
		
	}
	
	@Override
    public Object clone() {
		try {
			AnnotationTarget c = (AnnotationTarget)super.clone();
			c.externalId = this.externalId;
			c.recordId = this.recordId;
			c.withURI = this.withURI;
			
			c.selector = (SelectorType)selector.clone();
			
			return c;
		} catch (CloneNotSupportedException e) {
			return null;
		}
    }

}
