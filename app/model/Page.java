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

import java.util.Set;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import java.util.List;

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
	private Point coordinates;
	private String city;
	private String country;
	private String url;

	private ObjectId coverImage;

	private Set<ObjectId> featuredCollections;
	private Set<ObjectId> featuredExhibitions;
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

	public ObjectId getCoverImage() {
		return coverImage;
	}

	public void setCoverImage(ObjectId bgImg) {
		this.coverImage = bgImg;
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

	public Set<ObjectId> getFeaturedCollections() {
		return featuredCollections;
	}

	public void setFeaturedCollections(Set<ObjectId> featuredCollections) {
		this.featuredCollections = featuredCollections;
	}

	public Set<ObjectId> getFeaturedExhibitions() {
		return featuredExhibitions;
	}

	public void setFeaturedExhibitions(Set<ObjectId> featuredExhibitions) {
		this.featuredExhibitions = featuredExhibitions;
	}

}
