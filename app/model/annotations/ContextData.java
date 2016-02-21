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


import javax.validation.constraints.NotNull;

import model.resources.WithResource.WithResourceType;

import org.bson.types.ObjectId;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Embedded;

import utils.Deserializer;
import utils.Serializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class ContextData<T1 extends ContextData.ContextDataBody> {
	
	public static enum ContextDataType {
		ExhibitionData
	}
	
	public static class ContextDataBody {
	}
	
	public ContextData() {
		this.target = new ContextDataTarget();
		//default
		this.contextDataType = ContextDataType.ExhibitionData;
	}
	
	public ContextData(ObjectId colId, int position) {
		this.target = new ContextDataTarget();
		this.target.collectionId = colId;
		this.target.position = position;
		this.contextDataType = ContextDataType.ExhibitionData;
		//this.body = (T1) new ContextDataBody();
	}
	
	public static class ContextDataTarget {		
		@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
		ObjectId collectionId;
		int position;
		
		public ObjectId getCollectionId() {
			return collectionId;
		}
		public void setCollectionId(ObjectId collectionId) {
			this.collectionId = collectionId;
		}
		public int getPosition() {
			return position;
		}
		public void setPosition(int position) {
			this.position = position;
		}
	}
	
	T1 body;
	ContextDataTarget target;
	
	ContextDataType contextDataType;

	public T1 getBody() {
		return body;
	}

	public void setBody(T1 body) {
		this.body = body;
	}

	public ContextDataTarget getTarget() {
		return target;
	}

	public void setTarget(ContextDataTarget target) {
		this.target = target;
	}

	public ContextDataType getContextDataType() {
		return contextDataType;
	}

	public void setContextDataType(ContextDataType contextDataType) {
		this.contextDataType = contextDataType;
	}

}
