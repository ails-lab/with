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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.annotations.Annotation;
import model.annotations.Annotation.AnnotationAdmin;
import model.annotations.Annotation.MotivationType;
import model.annotations.bodies.AnnotationBodyTagging;
import model.basicDataTypes.Language;

import org.apache.commons.beanutils.BeanToPropertyValueTransformer;
import org.apache.commons.collections.CollectionUtils;
import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import com.fasterxml.jackson.databind.JsonNode;

@SuppressWarnings("rawtypes")
public class AnnotationDAO extends DAO<Annotation> {

	public AnnotationDAO() {
		super(Annotation.class);
	}

	public List<Annotation> getByIds(Set<ObjectId> annotationIds) {
		if (annotationIds == null || annotationIds.isEmpty())
			return new ArrayList<Annotation>();
		try {
			Query<Annotation> q = this.createQuery().field("_id")
					.in(annotationIds);
			return this.find(q).asList();
		} catch (Exception e) {
			return new ArrayList<Annotation>();
		}
	}

	public Annotation getExistingAnnotation(Annotation annotation) {
		if (annotation.getDbId() != null)
			return this.getById(annotation.getDbId());
		Query<Annotation> q = this.createQuery().disableValidation()
				.field("target.recordId")
				.equal(annotation.getTarget().getRecordId());
		if (annotation.getMotivation().equals(MotivationType.Tagging)) {
			AnnotationBodyTagging body = (AnnotationBodyTagging) annotation
					.getBody();
			if (body.getUri() != null)
				q.field("body.uri").equal(body.getUri());
			if (body.getLabel() != null)
				q.field("body.label.default").equal(
						body.getLabel().get(Language.DEFAULT));
		} else {
			return null;
		}
		return this.findOne(q);
	}

	public List<Annotation> getUserAnnotations(ObjectId userId, int offset,
			int count) {
		Query<Annotation> q = this.createQuery()
				.field("annotators.withCreator").equal(userId).offset(offset)
				.limit(count);
		return this.find(q).asList();
	}

	public List<Annotation> getUserAnnotations(ObjectId userId,
			List<String> retrievedFields) {
		Query<Annotation> q = this
				.createQuery()
				.field("annotators.withCreator")
				.equal(userId)
				.retrievedFields(
						true,
						retrievedFields.toArray(new String[retrievedFields
								.size()]));
		return this.find(q).asList();
	}

	public long countUserAnnotations(ObjectId userId) {
		long count = this.createQuery().field("annotators.withCreator")
				.equal(userId).countAll();
		return count;
	}

	// TODO: Mongo distinct count
	@SuppressWarnings("unchecked")
	public long countUserAnnotatedRecords(ObjectId userId) {
		Query<Annotation> q = this.createQuery()
				.field("annotators.withCreator").equal(userId)
				.retrievedFields(true, new String[] { "target.recordId" });
		List<Annotation> annotations = this.find(q).asList();
		List<ObjectId> recordIds = (List<ObjectId>) CollectionUtils.collect(
				annotations, new BeanToPropertyValueTransformer(
						"target.recordId"));
		Set<ObjectId> hs = new HashSet<>();
		hs.addAll(recordIds);
		return hs.size();

	}

	public void addAnnotators(ObjectId id, List<AnnotationAdmin> annotators) {
		Query<Annotation> q = this.createQuery().field("_id").equal(id);
		UpdateOperations<Annotation> updateOps = this.createUpdateOperations();
		updateOps.addAll("annotators", annotators, false);
		this.update(q, updateOps);
	}
	
	public void removeAnnotators(ObjectId id, List<AnnotationAdmin> annotators) {
		Query<Annotation> q = this.createQuery().field("_id").equal(id);
		UpdateOperations<Annotation> updateOps = this.createUpdateOperations();
		updateOps.removeAll("annotators", annotators);
		this.update(q, updateOps);
	}

	public void editAnnotationBody(ObjectId dbId, JsonNode json) {
		Query<Annotation> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<Annotation> updateOps = this.createUpdateOperations();
		updateFields("body", json, updateOps);
		updateOps.set("lastModified", new Date());
		this.update(q, updateOps);
	}

	public void deleteAnnotation(ObjectId annotationId) {
		Annotation annotation = this.get(annotationId);
		ObjectId recordId = annotation.getTarget().getRecordId();
		DB.getRecordResourceDAO().removeAnnotation(recordId, annotationId);
		this.deleteById(annotationId);
	}
}
