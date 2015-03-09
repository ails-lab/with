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

import model.Collection;
import model.RecordLink;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;

import play.Logger;
import play.Logger.ALogger;

public class CollectionDAO extends DAO<Collection> {
	public static final ALogger log = Logger.of(CollectionDAO.class);

	public CollectionDAO() {
		super( Collection.class );
	}

	public List<Collection> getCollectionsByIds(List<String> ids) {
		Query<Collection> colQuery = this.createQuery()
				.field("_id").hasAnyOf(ids);
		return find(colQuery).asList();
	}

	public Collection getByTitle(String title) {
		return this.findOne("title", title);
	}

	public Collection getById(ObjectId id) {
		Query<Collection> q = this.createQuery()
				.field("_id").equal(id);
		return findOne(q);
	}

	public void deleteById(ObjectId id) {
		Query<Collection> q = this.createQuery()
				.field("_id").equal(id);
		this.deleteByQuery(q);
	}

	public List<RecordLink> getCollectionRecordLinks(String id) {
		Query<Collection> colQuery = this.createQuery()
				.field("_id").equal(id)
				.retrievedFields(true, "firstEntries");
		return find(colQuery).get().getFirstEntries();
	}
}
