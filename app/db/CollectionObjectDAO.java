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

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import model.basicDataTypes.WithAccess.Access;
import model.resources.CollectionObject;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.CriteriaContainer;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CollectionObjectDAO extends CommonResourceDAO<CollectionObject> {

	/*
	 * The constructor is optional becuse the explicit type is passed through
	 * generics.
	 */
	public CollectionObjectDAO() {
		super(CollectionObject.class);
	}

	/**
	 * Increment entryCount (number of entries collected) in a CollectionObject
	 * 
	 * @param dbId
	 */
	public void incEntryCount(ObjectId dbId) {
		Query<CollectionObject> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<CollectionObject> updateOps = this
				.createUpdateOperations();
		updateOps.set("administrative.lastModified", new Date());
		updateOps.inc("administrative.entryCount");
		this.update(q, updateOps);
	}

	/**
	 * Decrement entryCount (number of entries collected) in a CollectionObject
	 * 
	 * @param dbId
	 */
	public void decEntryCount(ObjectId dbId) {
		Query<CollectionObject> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<CollectionObject> updateOps = this
				.createUpdateOperations();
		updateOps.set("administrative.lastModified", new Date());
		updateOps.dec("administrative.entryCount");
		this.update(q, updateOps);
	}

	public void editCollection(ObjectId dbId, JsonNode json) {
		Query<CollectionObject> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<CollectionObject> updateOps = this
				.createUpdateOperations();
		updateFields("", json, updateOps);
		this.update(q, updateOps);
	}

	public void updateFields(String parentField, JsonNode node,
			UpdateOperations<CollectionObject> updateOps) {
		Iterator<String> fieldNames = node.fieldNames();
		  while (fieldNames.hasNext()) {
	         String fieldName = fieldNames.next();
	         JsonNode fieldValue = node.get(fieldName);
        	 String newFieldName = parentField.isEmpty() ? fieldName : parentField + "." + fieldName;
	         if (fieldValue.isObject()) {
	        	 updateFields(newFieldName, fieldValue, updateOps);
	         }
	         else {//value
				try {
					ObjectMapper mapper = new ObjectMapper();
					Object value = mapper.treeToValue(fieldValue, newFieldName.getClass());
					updateOps.disableValidation().set(newFieldName, value);
				} catch (IOException e) {
					e.printStackTrace();
				}	 
	         }
	     }
	}

	/**
	 * Gets the union of the collections/exhibitions for a list of users and
	 * groups for a specific right
	 * 
	 * @param effectiveIds
	 * @param access
	 * @param isExhibition
	 * @param offset
	 * @param count
	 * @return the list of these collections
	 */
	public List<CollectionObject> getBySpecificAccess(
			List<ObjectId> effectiveIds, Access access, Boolean isExhibition,
			int offset, int count) {
		Query<CollectionObject> q = this.createQuery()
				.field("administrative.isExhibition").equal(isExhibition)
				.order("-administrative.lastModified").offset(offset)
				.limit(count);
		CriteriaContainer[] criteria = new CriteriaContainer[effectiveIds
				.size()];
		for (int i = 0; i < effectiveIds.size(); i++) {
			criteria[i] = this.createQuery()
					.criteria("administrative.access." + effectiveIds.get(i))
					.equal(access);
		}
		q.or(criteria);
		return this.find(q).asList();
	}

	public List<CollectionObject> getByMaxAccess(List<ObjectId> effectiveIds,
			Access access, Boolean isExhibition, int offset, int count) {
		Query<CollectionObject> q = this.createQuery()
				.order("-administrative.lastModified").offset(offset)
				.limit(count);
		CriteriaContainer[] criteria = new CriteriaContainer[effectiveIds
				.size()];
		for (int i = 0; i < effectiveIds.size(); i++) {
			criteria[i] = this.createQuery()
					.criteria("administrative.access." + effectiveIds.get(i))
					.greaterThanOrEq(access.ordinal());
		}
		q.field("administrative.isExhibition").equal(isExhibition);
		q.or(criteria);
		return this.find(q).asList();
	}

	public List<CollectionObject> getPublic(Boolean isExhibition, int offset,
			int count) {
		Query<CollectionObject> q = this.createQuery()
				.field("administrative.access.isPublic").equal(true)
				.order("-administrative.lastModified").offset(offset)
				.limit(count);
		q.field("administrative.isExhibition").equal(isExhibition);
		return this.find(q).asList();
	}

	public List<CollectionObject> getBySpecificAccessWithRestrictions(
			List<ObjectId> effectiveIds, Access access,
			Map<ObjectId, Access> restrictions, Boolean isExhibition,
			int offset, int count) {
		Query<CollectionObject> q = this.createQuery()
				.field("administrative.isExhibition").equal(isExhibition)
				.order("-administrative.lastModified").offset(offset)
				.limit(count);
		CriteriaContainer[] criteriaOr = new CriteriaContainer[effectiveIds
				.size()];
		for (int i = 0; i < effectiveIds.size(); i++) {
			criteriaOr[i] = this.createQuery()
					.criteria("administrative.access." + effectiveIds.get(i))
					.equal(access);
		}
		CriteriaContainer[] criteriaAnd = new CriteriaContainer[restrictions
				.size()];
		int i = 0;
		for (ObjectId id : restrictions.keySet()) {
			criteriaOr[i++] = this.createQuery()
					.criteria("administrative.access." + id)
					.equal(restrictions.get(id));
		}
		q.or(criteriaOr);
		q.and(criteriaAnd);
		return this.find(q).asList();
	}
	
	public List<CollectionObject> getByMaxAccessWithRestrictions(
			List<ObjectId> effectiveIds, Access access,
			Map<ObjectId, Access> restrictions, Boolean isExhibition,
			int offset, int count) {
		Query<CollectionObject> q = this.createQuery()
				.field("administrative.isExhibition").equal(isExhibition)
				.order("-administrative.lastModified").offset(offset)
				.limit(count);
		CriteriaContainer[] criteriaOr = new CriteriaContainer[effectiveIds
				.size()];
		for (int i = 0; i < effectiveIds.size(); i++) {
			criteriaOr[i] = this.createQuery()
					.criteria("administrative.access." + effectiveIds.get(i))
					.equal(access);
		}
		CriteriaContainer[] criteriaAnd = new CriteriaContainer[restrictions
				.size()];
		int i = 0;
		for (ObjectId id : restrictions.keySet()) {
			criteriaOr[i++] = this.createQuery()
					.criteria("administrative.access." + id)
					.equal(restrictions.get(id));
		}
		q.or(criteriaOr);
		q.and(criteriaAnd);
		return this.find(q).asList();
	}
	
}
