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

import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.geo.Point;

public class Page {

	private String address;
	private Point coordinates;
	private String city;
	private String country;
	private String url;

	private ObjectId coverImage;

	private List<ObjectId> featuredCollections;
	private List<ObjectId> featuredExhibitions;



	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Point getLocation() {
		return coordinates;
	}

	public void setLocation(Point location) {
		this.coordinates = location;
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


	public List<ObjectId> getFeaturedColsl() {
		return featuredCollections;
	}

	public void setFeaturedColsl(List<ObjectId> featuredColsl) {
		this.featuredCollections = featuredColsl;
	}

	public List<ObjectId> getFeaturedExhibitions() {
		return featuredExhibitions;
	}

	public void setFeaturedExhibitions(List<ObjectId> featuredExhibitions) {
		this.featuredExhibitions = featuredExhibitions;
	}
}
