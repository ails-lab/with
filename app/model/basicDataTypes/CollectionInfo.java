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


package model.basicDataTypes;

import java.util.ArrayList;

import org.bson.types.ObjectId;

import utils.Serializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class CollectionInfo {
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId collectionId;
	private Integer position;
	
	public CollectionInfo() {
	}
	
	//position is Integer instead of int, so that we can do the null trick with morphia (see CommonResourcesDAO)
	public CollectionInfo(ObjectId collectionId, Integer position) {
		this.collectionId = collectionId;
		this.position = position;
	}
	
	public ObjectId getCollectionId() {
		return collectionId;
	}
	public void setCollectionId(ObjectId collectionId) {
		this.collectionId = collectionId;
	}
	public Integer getPosition() {
		return position;
	}
	public void setPosition(Integer position) {
		this.position = position;
	}
	
}
