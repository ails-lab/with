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

import model.Record;
import model.RecordLink;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;

import play.Logger;

public class RecordDAO extends DAO<Record> {
	static private final Logger.ALogger log = Logger.of(Record.class);

	public RecordDAO() {
		super( Record.class );
	}

	/**
	 * Get the embedded RecordLink from a Record
	 * @param dbId
	 * @return
	 */
	public RecordLink getRecordLink(String dbId) {
		Query<Record> q = this.createQuery()
				.field("dbId").equal(new ObjectId(dbId))
				.retrievedFields(true, "baseLinkData");
		return this.find(q).get().getBaseLinkData();
	}

	/**
	 * Retrieve the source from an embedded RecordLink
	 * @param dbId
	 * @return
	 */
	public String getRecordSource(String dbId) {
		Query<Record> q = this.createQuery()
				.field("_id").equal(new ObjectId(dbId))
				.retrievedFields(true, "baseLinkData.source");
		return this.find(q).get()
				.getBaseLinkData().getSource();
	}

}
