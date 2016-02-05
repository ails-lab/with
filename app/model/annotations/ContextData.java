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


import org.bson.types.ObjectId;

import utils.Serializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;


public class ContextData<T1 extends ContextData.ContextDataBody> {
	
	public static enum ContextDataType {
		ExhibitionAnnotation
	}
		
	public static class ContextDataBody {
	}
	
	public ContextData() {
		super();
		this.target = new ContextDataTarget();
	}
	
	public ContextData(ObjectId colId, int position) {
		super();
		this.target = new ContextDataTarget();
		this.target.collectionId = colId;
		this.target.position = position;
		this.body = (T1) new ContextDataBody();
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
