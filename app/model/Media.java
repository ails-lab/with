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

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A class to represent media, merge of all the interesting attributes and
 * access to the byte[] that is the data for it
 *
 * @author stabenau
 *
 */
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class Media {

	public enum BaseType {
		TEXT, IMAGE, AUDIO, VIDEO
	}
	
	@Id
	@JsonIgnore
	private ObjectId dbId;
	// examples, but there might be more

	// the owner
	private ObjectId ownerId;

	private int width, height;

	// IMAGE, VIDEO, AUDIO, TXT
	private BaseType type;

	// more explicit media type
	private String mimeType;

	// how long in seconds
	private float duration;

	// the actual data .. GridFS
	@JsonIgnore
	private byte[] data;

	private String filename;

	private Rights rights = new Rights();
	
	public ObjectId getDbId() {
		return dbId;
	}

	public void setDbId(ObjectId dbId) {
		this.dbId = dbId;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public boolean hasWidth() {
		return getWidth() != 0;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public boolean hasHeight() {
		return getHeight() != 0;
	}

	public BaseType getType() {
		return type;
	}

	public void setType(BaseType type) {
		this.type = type;
	}

	public boolean hasType() {
		return getType() != null;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public boolean hasMimeType() {
		return getMimeType() != null;
	}

	public float getDuration() {
		return duration;
	}

	public void setDuration(float duration) {
		this.duration = duration;
	}

	public boolean hasDuration() {
		return getDuration() != 0;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public boolean hasData() {
		return getData() != null;
	}

	public boolean hasOwner() {
		return getOwnerId() != null;
	}

	public ObjectId getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(ObjectId ownerId) {
		this.ownerId = ownerId;
	}

	public String getFilename() {
		return this.filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public Rights getRights() {
		return rights;
	}

	public void setRights(Rights rights) {
		this.rights = rights;
	}
}
