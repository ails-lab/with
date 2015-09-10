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

public class Organization extends UserGroup {

	private String address;
	private Point location;
	private String url;

	private ObjectId bgImg;

	private List<ObjectId> featuredColsl;



	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Point getLocation() {
		return location;
	}

	public void setLocation(Point location) {
		this.location = location;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public ObjectId getBgImg() {
		return bgImg;
	}

	public void setBgImg(ObjectId bgImg) {
		this.bgImg = bgImg;
	}

	public List<ObjectId> getFeaturedColsl() {
		return featuredColsl;
	}

	public void setFeaturedColsl(List<ObjectId> featuredColsl) {
		this.featuredColsl = featuredColsl;
	}

}
