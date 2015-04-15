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


package db;

import java.util.List;

import model.CollectionEntry;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;

import play.Logger;
import play.Logger.ALogger;

public class CollectionEntryDAO extends DAO<CollectionEntry> {
	public static final ALogger log = Logger.of(CollectionEntry.class);

	public CollectionEntryDAO() {
		super(CollectionEntry.class);
	}

	/**
	 * Retrieve all personalized records referred to
	 * this record link
	 * @param recLinkId
	 * @return
	 */
	public List<CollectionEntry> getByRecLinkId(ObjectId recLinkId) {
		Query<CollectionEntry> q = this.createQuery()
				.field("baseLinkData._id").equal(recLinkId);
		return this.find(q).asList();
	}

	/**
	 * Retrieve the specific personalized record referred to
	 * the record link and the specified collection
	 * @param recLinkId
	 * @param collectionId
	 * @return
	 */
	public CollectionEntry getByPersonalizedRecord(ObjectId recLinkId, ObjectId collectionId) {
		Query<CollectionEntry> q = this.createQuery()
				.field("baseLinkData._id").equal(recLinkId)
				.field("collection").equal(collectionId);
		return this.findOne(q);
	}

	public int deleteByCollectionRecLinkId(ObjectId recLinkId, ObjectId colId) {
		Query<CollectionEntry> q = this.createQuery()
				.field("baseLinkData._id").equal(recLinkId)
				.field("collection").equal(colId);
		return this.deleteByQuery(q).getN();
	}

	public List<CollectionEntry> getByCollection(ObjectId colId) {
		return getByCollectionOffsetCount(colId, 0, -1);
	}

	public List<CollectionEntry> getByCollectionOffsetCount(ObjectId colId, int offset, int count) {
		Query<CollectionEntry> q = this.createQuery()
				.field("collectionId").equal(colId)
				.offset(offset)
				.limit(count);
		return this.find(q).asList();
	}

	public List<CollectionEntry> getBySource(String source,String sourceId) {
		Query<CollectionEntry> q = this.createQuery()
				.field("baseLinkData.source").equal(source)
				.field("baseLinkData.sourceId").equal(sourceId);
		return this.find(q).asList();
	}
}
