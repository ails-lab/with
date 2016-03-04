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

import model.basicDataTypes.WithAccess;

import org.bson.types.ObjectId;

import utils.Deserializer;
import utils.Serializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class ContextData<T1 extends ContextData.ContextDataBody> {

	public static enum ContextDataType {
		ExhibitionData, None
	}

	// The target resource Id
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId resourceId;
	private ContextDataType contextDataType;
	private T1 body;

	public static class ContextDataTarget {
		@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
		ObjectId collectionId;
		@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
		ObjectId recordId;
		@JsonSerialize(using = Serializer.WithAccessSerializer.class)
		@JsonDeserialize(using = Deserializer.WithAccessDeserializer.class)
		WithAccess access;
		

		public ObjectId getCollectionId() {
			return collectionId;
		}

		public void setCollectionId(ObjectId collectionId) {
			this.collectionId = collectionId;
		}

		public ObjectId getRecordId() {
			return recordId;
		}

		public void setRecordId(ObjectId recordId) {
			this.recordId = recordId;
		}

		public WithAccess getAccess() {
			return access;
		}

		public void setAccess(WithAccess access) {
			this.access = access;
		}
	}

	public static class ContextDataBody {
	}

	public ContextData() {
	}

	public ContextData(ObjectId resourceId) {
		this.resourceId = resourceId;
		this.contextDataType = ContextDataType.None;
	}

	public ContextData(ObjectId resourceId, ContextDataType contextDataType) {
		this.resourceId = resourceId;
		this.contextDataType = contextDataType;
	}

	public ContextData(ObjectId resourceId, ContextDataType contextDataType,
			T1 body) {
		this.resourceId = resourceId;
		this.contextDataType = contextDataType;
		this.body = body;
	}

	public ObjectId getResourceId() {
		return resourceId;
	}

	public void setResourceId(ObjectId resourceId) {
		this.resourceId = resourceId;
	}

	public T1 getBody() {
		return body;
	}

	public void setBody(T1 body) {
		this.body = body;
	}

	public ContextDataType getContextDataType() {
		return contextDataType;
	}

	public void setContextDataType(ContextDataType contextDataType) {
		this.contextDataType = contextDataType;
	}

}
