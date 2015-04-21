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

import model.Rights;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;

import play.Logger;


public class RightsDAO extends DAO<Rights> {
	static private final Logger.ALogger log = Logger.of(Rights.class);


	public RightsDAO() {
		super(Rights.class);
	}

	public List<Rights> getByOwner(ObjectId ownerId) {
		Query<Rights> q = this.createQuery()
				.field("ownerId").equal(ownerId);
		return this.find(q).asList();
	}

}
