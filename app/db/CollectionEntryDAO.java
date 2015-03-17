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

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;

import model.CollectionEntry;
import play.Logger;
import play.Logger.ALogger;

public class CollectionEntryDAO extends DAO<CollectionEntry> {
	public static final ALogger log = Logger.of(CollectionEntry.class);

	public CollectionEntryDAO() {
		super(CollectionEntry.class);
	}

	public CollectionEntry getByRecLinkId(ObjectId recLinkId) {
		Query<CollectionEntry> q = this.createQuery()
				.field("recordLink").equal(recLinkId);
		return this.findOne(q);
	}
	
	public int deleteByRecLinkId(ObjectId recLinkId) {
		Query<CollectionEntry> q = this.createQuery()
				.field("recordLink._id").equal(recLinkId);
		return this.deleteByQuery(q).getN();
	}
}
