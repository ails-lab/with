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


package model.usersAndGroups;

import java.util.HashMap;
import java.util.List;

import model.EmbeddedMediaObject.MediaVersion;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import utils.Serializer;

@Indexes({
	@Index(fields = @Field(value = "coordinates", type = IndexType.GEO2DSPHERE), options = @IndexOptions(background=true))
	})
public class Page {

	private String address;

	@Embedded
	public static class Point {
		
		private double latitude;
		private double longitude;

		public double getLatitude() {
			return latitude;
		}

		public void setLatitude(double latitude) {
			this.latitude = latitude;
		}

		public double getLongitude() {
			return longitude;
		}

		public void setLongitude(double longitude) {
			this.longitude = longitude;
		}

	}

	//HAVE TO TEST
//	private org.mongodb.morphia.geo.Point coordinates;
	
	private Point coordinates;
	private String city;
	private String country;
	private String url;

	private HashMap<MediaVersion, String> cover;

	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private List<ObjectId> featuredCollections;
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private List<ObjectId> featuredExhibitions;
	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public HashMap<MediaVersion, String> getCover() {
		return cover;
	}

	public void setCover(HashMap<MediaVersion, String> cover) {
		this.cover = cover;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public Point getCoordinates() {
		return coordinates;
	}

	public void setCoordinates(Point coordinates) {
		this.coordinates = coordinates;
	}

	public List<ObjectId> getFeaturedCollections() {
		return featuredCollections;
	}

	public void setFeaturedCollections(List<ObjectId> featuredCollections) {
		this.featuredCollections = featuredCollections;
	}

	public void addFeaturedCollection(ObjectId featuredCollection) {
		this.featuredCollections.add(featuredCollection);
	}

	public List<ObjectId> getFeaturedExhibitions() {
		return featuredExhibitions;
	}

	public void setFeaturedExhibitions(List<ObjectId> featuredExhibitions) {
		this.featuredExhibitions = featuredExhibitions;
	}

	public void addFeaturedExhibition(ObjectId featuredExhibition) {
		this.featuredExhibitions.add(featuredExhibition);
	}

}
