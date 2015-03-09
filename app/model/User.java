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
import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import play.Logger;
import play.Logger.ALogger;
import db.MediaDAO;

@Entity
public class User {

	public static final ALogger log = Logger.of(User.class);


	private static final int EMBEDDED_CAP = 20;
	
	@Id
	private ObjectId dbID;

	private String email;
	private String firstName;
	private String lastName;

	private String md5Password;
	private String facebookId;


	// we should experiment here with an array of fixed-size
	// We keep a complete search history, but have the first
	// k entries in here as a copy
	@Embedded
	private List<Search> searchHistory = new ArrayList<Search>();
	@Embedded
	private List<CollectionMetadata> collections = new ArrayList<CollectionMetadata>();

	// convenience methods
	
	/**
	 * The search should already be stored in the database separately
	 * @param search
	 */
	public void addToHistory( Search search ) {
		if( search.getDbID() == null ) {
			log.error( "Search is  not saved!" );
			return;
		}
		
		searchHistory.add( search );
		if( searchHistory.size() > EMBEDDED_CAP ) {
			searchHistory.remove(0);
		}
	}
	
	/**
	 * The Collection should already be stored in the database separately 
	 * @param col
	 */
	public void addToCollections( Collection col ) {
		if( col.getDbId() == null ) {
			log.error( "Collection is not saved!");
			return;
		}
		collections.add( col.getMetadata() );
		if( collections.size() > EMBEDDED_CAP) {
			collections.remove(0);
		}
		
	}
	
	
	// getter setter
	
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getMd5Password() {
		return md5Password;
	}

	public void setMd5Password(String md5Password) {
		this.md5Password = md5Password;
	}


	public List<Search> getSearchHistory() {
		return searchHistory;
	}

	public void setSearchHistory(List<Search> searcHistory) {
		this.searchHistory = searcHistory;
	}

	public List<CollectionMetadata> getCollectionMetadata() {
		return collections;
	}

	public void setCollectionMetadata(List<CollectionMetadata> collections) {
		this.collections = collections;
	}

	public ObjectId getDbID() {
		return dbID;
	}

	public void setDbID(ObjectId dbID) {
		this.dbID = dbID;
	}

	public String getFacebookId() {
		return facebookId;
	}

	public void setFacebookId(String facebookId) {
		this.facebookId = facebookId;
	}
	
}
