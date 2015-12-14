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
import java.util.Iterator;
import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import com.fasterxml.jackson.databind.JsonNode;

import model.resources.CollectionObject;

public class CollectionObjectDAO extends CommonResourceDAO<CollectionObject> {

	/*
	 * The constructor is optional becuse the explicit
	 * type is passed through generics.
	 */
	public CollectionObjectDAO() {
		super(CollectionObject.class);
	}

	/**
	 * Increment entryCount (number of entries collected) in a CollectionObject
	 * @param dbId
	 */
	public void incEntryCount(ObjectId dbId) {
		Query<CollectionObject> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<CollectionObject> updateOps = this.createUpdateOperations();
		updateOps.set("administrative.lastModified", new Date());
		updateOps.inc("administrative.entryCount");
		this.update(q, updateOps);
	}

	/**
	 * Decrement entryCount (number of entries collected) in a CollectionObject
	 * @param dbId
	 */
	public void decEntryCount(ObjectId dbId) {
		Query<CollectionObject> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<CollectionObject> updateOps = this.createUpdateOperations();
		updateOps.set("administrative.lastModified", new Date());
		updateOps.dec("administrative.entryCount");
		this.update(q, updateOps);
	}

	public void editCollection(ObjectId dbId, JsonNode json) {
		Query<CollectionObject> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<CollectionObject> updateOps = this.createUpdateOperations();
		updateFields("", json, updateOps);
		this.update(q,  updateOps);
	}

	public void updateFields(String parentField, JsonNode node, UpdateOperations<CollectionObject> updateOps) {
		Iterator<String> fieldNames = node.fieldNames();
	     while (fieldNames.hasNext()) {
	         String fieldName = fieldNames.next();
	         JsonNode fieldValue = node.get(fieldName);
        	 String newFieldName = parentField.isEmpty() ? fieldName : parentField + "." + fieldName;
	         if (fieldValue.isObject()) {
	        	 updateFields(newFieldName, fieldValue, updateOps);
	         }
	         else {//value
	        	 updateOps.set(newFieldName, fieldValue);
	         }
	     }
	}
}
