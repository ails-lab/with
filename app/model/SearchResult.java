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
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import db.DB;

/**
 * In this class we store search results that were retrieved by a search and we want to
 * be able to share them with others or store them to compare with re-executed searches in the future.
 *
 * @author stabenau
 *
 */
@Entity
public class SearchResult {

	@Id
	private ObjectId dbID;

	// where in the Search was this result
	private int offset;

	// embed the search in here or reference
	private ObjectId search;

	// embedd the recordLink in here
	@Embedded
	private RecordLink recordLink;


	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public ObjectId getDbID() {
		return dbID;
	}

	public void setDbID(ObjectId dbID) {
		this.dbID = dbID;
	}

	public Search getSearch() {
		Search search =
				DB.getSearchDAO().getById(this.search);
		return search;
	}

	public void setSearch(ObjectId search) {
		this.search = search;
	}

	public RecordLink getRecordLink() {
		return recordLink;
	}

	public void setRecordLink(RecordLink recordLink) {
		this.recordLink = recordLink;
	}

}
