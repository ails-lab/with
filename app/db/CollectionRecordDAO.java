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

import model.CollectionRecord;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;

import play.Logger;
import play.Logger.ALogger;

public class CollectionRecordDAO extends DAO<CollectionRecord> {
	public static final ALogger log = Logger.of(CollectionRecord.class);

	public CollectionRecordDAO() {
		super(CollectionRecord.class);
	}

	public CollectionRecord getById(ObjectId id) {
		Query<CollectionRecord> q = this.createQuery()
				.field("_id").equal(id);
		return this.findOne(q);
	}


	/**
	 * Retrieve records from sepcific collection
	 * @param colId
	 * @return
	 */
	public List<CollectionRecord> getByCollection(ObjectId colId) {
		return getByCollectionOffsetCount(colId, 0, -1);
	}

	/**
	 * Retrieve records from sepcific collection by offset and count
	 * @param colId, offset, count
	 * @return
	 */
	public List<CollectionRecord> getByCollectionOffsetCount(ObjectId colId, int offset, int count) {
		Query<CollectionRecord> q = this.createQuery()
				.field("collectionId").equal(colId)
				.offset(offset)
				.limit(count);
		return this.find(q).asList();
	}

	/**
	 * Retrieve the total amount of records within a collection
	 */
	public long getItemCount(ObjectId colId) {
		Query<CollectionRecord> q = this.createQuery()
				.field("collectionId").equal(colId);
		return this.find(q).countAll();
	}


	public List<CollectionRecord> getBySource(String source,String sourceId) {
		Query<CollectionRecord> q = this.createQuery()
				.field("source").equal(source)
				.field("sourceId").equal(sourceId);
		return this.find(q).asList();
	}
}
