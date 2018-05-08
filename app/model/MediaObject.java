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

import org.apache.commons.codec.binary.Hex;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.net.MediaType;

import model.EmbeddedMediaObject.Quality;
import model.basicDataTypes.LiteralOrResource;

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

	public static enum Orientation {
		PORTRAIT, LANDSCAPE
	}

	@Id
	@JsonIgnore
	private ObjectId dbId;

	// which resource is this Media part of, this is the access rights
	// restriction
	// if there is none, the media object is publicly available
	private ArrayList<ObjectId> resources;

	@JsonIgnore
	private byte[] mediaBytes;

	// extended model fields

	// is an Enum but i leave it here till we decide upon the libraries to use
	private String codec;

	private double durationSeconds;

	// for pdfs only TODO: add this in the controller
	private int spatialResolution;

	private int bitRate;

	private int frameRate;

	// also an Enum!
	private String colorSpace;

	// use different Hex?
	private Hex componentColor;

	private Orientation orientation;

	private int audioChannelNumber;

	// audio only
	private int sampleSize;

	private int sampleRate;

	// If this is a thumbnail, the parentID field refers to
	// the parent media object, else it should be the id of the media object
	// itself
	private ObjectId parentId;

	// Setters/Getters
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

	public double getDurationSeconds() {
		return durationSeconds;
	}

	public void setDurationSeconds(double durationSeconds) {
		this.durationSeconds = durationSeconds;
	}

	public byte[] getMediaBytes() {
		return mediaBytes;
	}

	public void setMediaBytes(byte[] mediaBytes) {
		this.mediaBytes = mediaBytes;
	}

	public String getCodec() {
		return codec;
	}

	public void setCodec(String codec) {
		this.codec = codec;
	}

	public int getSpatialResolution() {
		return spatialResolution;
	}

	public void setSpatialResolution(int spatialResolution) {
		this.spatialResolution = spatialResolution;
	}

	public int getSampleSize() {
		return sampleSize;
	}

	public void setSampleSize(int sampleSize) {
		this.sampleSize = sampleSize;
	}

	public int getSampleRate() {
		return sampleRate;
	}

	public void setSampleRate(int sampleRate) {
		this.sampleRate = sampleRate;
	}

	public int getBitRate() {
		return bitRate;
	}

	public void setBitRate(int bitRate) {
		this.bitRate = bitRate;
	}

	public int getFrameRate() {
		return frameRate;
	}

	public void setFrameRate(int frameRate) {
		this.frameRate = frameRate;
	}

	public String getColorSpace() {
		return colorSpace;
	}

	public void setColorSpace(String colorSpace) {
		this.colorSpace = colorSpace;
	}

	public Hex getComponentColor() {
		return componentColor;
	}

	public void setComponentColor(Hex componentColor) {
		this.componentColor = componentColor;
	}

	public Orientation getOrientation() {
		return orientation;
	}

	public void setOrientation(Orientation orientation) {
		this.orientation = orientation;
	}

	public void setOrientation() {
		if (this.getWidth() >= this.getHeight()) {
			this.orientation = Orientation.LANDSCAPE;
		} else {
			this.orientation = Orientation.PORTRAIT;
		}
	}

	public int getAudioChannelNumber() {
		return audioChannelNumber;
	}

	public void setAudioChannelNumber(int audioChannelNumber) {
		this.audioChannelNumber = audioChannelNumber;
	}

	public ObjectId getParentId() {
		return parentId;
	}

	public void setParentId(ObjectId parentId) {
		this.parentId = parentId;
	}

	public MediaObject(int width, int height, WithMediaType type,
			WithMediaRights withRights, String url, MediaVersion mediaVersion,
			LiteralOrResource originalRights, MediaType mimeType, long size,
			Quality quality, ArrayList<ObjectId> resources, byte[] mediaBytes,
			String codec, double durationSeconds, int spatialResolution,
			int bitRate, int frameRate, String colorSpace, Hex componentColor,
			Orientation orientation, int audioChannelNumber, int sampleSize,
			int sampleRate, ObjectId parentId) {
		super(width, height, type, withRights, url, mediaVersion,
				originalRights, mimeType, size, quality);
		this.resources = resources;
		this.mediaBytes = mediaBytes;
		this.codec = codec;
		this.durationSeconds = durationSeconds;
		this.spatialResolution = spatialResolution;
		this.bitRate = bitRate;
		this.frameRate = frameRate;
		this.colorSpace = colorSpace;
		this.componentColor = componentColor;
		this.orientation = orientation;
		this.audioChannelNumber = audioChannelNumber;
		this.sampleSize = sampleSize;
		this.sampleRate = sampleRate;
		this.parentId = parentId;
	}

	public MediaObject(MediaObject mediaObject) {
		this(mediaObject.getWidth(), mediaObject.getHeight(),
				mediaObject.getType(), mediaObject.getWithRights(),
				mediaObject.getUrl(), mediaObject.getMediaVersion(),
				mediaObject.getOriginalRights(), mediaObject.getMimeType(),
				mediaObject.getSize(), mediaObject.getQuality(),
				mediaObject.getResources(), mediaObject.getMediaBytes(),
				mediaObject.getCodec(), mediaObject.getDurationSeconds(),
				mediaObject.getSpatialResolution(), mediaObject.getBitRate(),
				mediaObject.getFrameRate(), mediaObject.getColorSpace(),
				mediaObject.getComponentColor(), mediaObject.getOrientation(),
				mediaObject.getAudioChannelNumber(),
				mediaObject.getSampleSize(), mediaObject.getSampleRate(),
				mediaObject.getParentId());
	}

	public MediaObject() {
	}

	
}
