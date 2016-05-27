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

import java.util.Date;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import com.fasterxml.jackson.databind.JsonNode;

import model.resources.ThesaurusObject;

public class ThesaurusObjectDAO extends DAO<ThesaurusObject> {

	public ThesaurusObjectDAO() {
		super(ThesaurusObject.class);
	}
	
	public ThesaurusObject getByUri(String uri) {
		return this.findOne("semantic.uri", uri);
	}

	public void editRecord(String root, ObjectId dbId, JsonNode json) {
		Query<ThesaurusObject> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<ThesaurusObject> updateOps = this.createUpdateOperations();
		
		updateFields(root, json, updateOps);
		updateOps.set("administrative.lastModified", new Date());
		this.update(q, updateOps);
	}

}
