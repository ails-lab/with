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

import model.RecordLink;
import play.Logger;

public class RecordLinkDAO extends DAO<RecordLink> {
	static private final Logger.ALogger log = Logger.of(RecordLink.class);

	public RecordLinkDAO() {
		super( RecordLink.class );
	}

	public RecordLink getByDbId(ObjectId id) {
		Query<RecordLink> q =
				this.createQuery()
				.field("_id").equal(id);
		return this.findOne(q);
	}

}
