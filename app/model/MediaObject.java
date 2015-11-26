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

import utils.MediaTypeConverter;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Converters;
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
@Converters(MediaTypeConverter.class)
public class MediaObject extends EmbeddedMediaObject {
	
	@Id
	@JsonIgnore
	private ObjectId dbId;
	
	// which resource is this Media part of, this is the access rights restriction
	// if there is none, the media object is publicly available
	private ArrayList<ObjectId> resources;

	private int width, height;

	private double durationSeconds;

	@JsonIgnore
	private byte[] thumbnailBytes;
	@JsonIgnore
	private byte[] mediaBytes;

	public ObjectId getDbId() {
		return dbId;
	}

	public void setDbId(ObjectId dbId) {
		this.dbId = dbId;
	}
	
	public ArrayList<ObjectId> getResources() {
		return resources;
	}
	public void setResources(ArrayList<ObjectId> resources) {
		this.resources = resources;
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
	public double getDurationSeconds() {
		return durationSeconds;
	}
	public void setDurationSeconds(double durationSeconds) {
		this.durationSeconds = durationSeconds;
	}
	public byte[] getThumbnailBytes() {
		return thumbnailBytes;
	}
	public void setThumbnailBytes(byte[] thumbnailBytes) {
		this.thumbnailBytes = thumbnailBytes;
	}
	public byte[] getMediaBytes() {
		return mediaBytes;
	}
	public void setMediaBytes(byte[] mediaBytes) {
		this.mediaBytes = mediaBytes;
	}



}
