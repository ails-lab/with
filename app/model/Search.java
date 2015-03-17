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

import java.util.Date;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import db.DB;

@Entity
public class Search {

	@Id
	private ObjectId dbID;

	// a ref to the user
	private ObjectId user;

	// when the search was done
	private Date searchDate;

	private String query;


	public ObjectId getDbID() {
		return dbID;
	}

	public void setDbID(ObjectId dbID) {
		this.dbID = dbID;
	}

	public Date getSearchDate() {
		return searchDate;
	}

	public void setSearchDate(Date searchDate) {
		this.searchDate = searchDate;
	}

	public User getUser() {
		User user =
				DB.getUserDAO().getById(this.user);
		return user;
	}

	public void setUser(User user) {
		this.user = user.getDbId();
	}
	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

}
