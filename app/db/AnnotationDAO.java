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
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import model.annotations.Annotation;
import model.annotations.Annotation.AnnotationAdmin;
import model.annotations.Annotation.MotivationType;
import model.annotations.bodies.AnnotationBodyGeoTagging;
import model.annotations.bodies.AnnotationBodyTagging;
import model.annotations.bodies.AnnotationBodyColorTagging;
import model.annotations.bodies.AnnotationBodyPolling;
import model.annotations.selectors.SelectorType;
import model.annotations.targets.AnnotationTarget;
import model.basicDataTypes.Language;
import model.resources.collection.CollectionObject;

import org.apache.commons.beanutils.BeanToPropertyValueTransformer;
import org.apache.commons.collections.CollectionUtils;
import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.WriteResult;

@SuppressWarnings("rawtypes")
public class AnnotationDAO extends DAO<Annotation> {

	public AnnotationDAO() {
		super(Annotation.class);
	}

	public List<Annotation> getByIds(Set<ObjectId> annotationIds) {
		if (annotationIds == null || annotationIds.isEmpty())
			return new ArrayList<Annotation>();
		try {
			Query<Annotation> q = this.createQuery().field("_id").in(annotationIds);
			return this.find(q).asList();
		} catch (Exception e) {
			return new ArrayList<Annotation>();
		}
	}

	public List<Annotation> getByCollection(ObjectId collectionId) {
		CollectionObject collection = DB.getCollectionObjectDAO().getById(collectionId,
				new ArrayList<String>(Arrays.asList("collectedResources")));

		List<ObjectId> recIds = (List<ObjectId>) CollectionUtils.collect(collection.getCollectedResources(),
				new BeanToPropertyValueTransformer("target.recordId"));

		if (recIds.size() > 0) {
			Query<Annotation> q = this.createQuery().field("target.recordId").in(recIds);
			return this.find(q).asList();
		} else {
			return new ArrayList<Annotation>();
		}
	}

	public List<Annotation> getApprovedByRecordId(ObjectId recordId, List<Annotation.MotivationType> motivations,
			List<String> retrievedFields) {
		Query<Annotation> q = this.createQuery().disableValidation().field("target.recordId").equal(recordId)
				.field("motivation").hasAnyOf(motivations).field("score.approvedBy").exists().field("score.approvedBy")
				.notEqual(new String[0])
				.retrievedFields(true, retrievedFields.toArray(new String[retrievedFields.size()]));
		return this.find(q).asList();
	}

	public List<Annotation> getByRecordId(ObjectId recordId, List<Annotation.MotivationType> motivations) {
		Query<Annotation> q = this.createQuery().disableValidation().field("target.recordId").equal(recordId)
				.field("motivation").hasAnyOf(motivations);
		return this.find(q).asList();
	}

	public Annotation getExistingAnnotation(Annotation annotation) {
		if (annotation.getDbId() != null)
			return this.getById(annotation.getDbId());

		AnnotationTarget target = (AnnotationTarget) annotation.getTarget();

		Query<Annotation> q = this.createQuery().disableValidation().field("target.recordId")
				.equal(target.getRecordId());

		if (annotation.getMotivation().equals(MotivationType.Tagging)) {
			AnnotationBodyTagging body = (AnnotationBodyTagging) annotation.getBody();

			if (body.getUri() != null) {
				q.field("body.uri").equal(body.getUri());
			}
		} else if (annotation.getMotivation().equals(MotivationType.ColorTagging)) {
			AnnotationBodyColorTagging body = (AnnotationBodyColorTagging) annotation.getBody();

			if (body.getUri() != null) {
				q.field("body.uri").equal(body.getUri());
			}
		} else if (annotation.getMotivation().equals(MotivationType.Polling)) {
			AnnotationBodyPolling body = (AnnotationBodyPolling) annotation.getBody();

			if (body.getUri() != null) {
				q.field("body.uri").equal(body.getUri());
			}
		} else if (annotation.getMotivation().equals(MotivationType.GeoTagging)) {
			AnnotationBodyGeoTagging body = (AnnotationBodyGeoTagging) annotation.getBody();

			q.field("body.coordinates").equal(body.getCoordinates());
		}

		SelectorType selector = target.getSelector();
		if (selector != null) {
			selector.addToQuery(q);
		} else {
			q.field("target.selector").doesNotExist();
		}

		return this.findOne(q);
	}

	public List<Annotation> getUserAnnotations(ObjectId userId, int offset, int count) {
		Query<Annotation> q = this.createQuery().field("annotators.withCreator").equal(userId).offset(offset)
				.limit(count);
		return this.find(q).asList();
	}

	public List<Annotation> getUserAnnotations(ObjectId userId, List<String> retrievedFields) {
		Query<Annotation> q = this.createQuery().field("annotators.withCreator").equal(userId).retrievedFields(true,
				retrievedFields.toArray(new String[retrievedFields.size()]));
		return this.find(q).asList();
	}

	public List<Annotation> getUserAnnotations(ObjectId userId, ObjectId recordId, List<String> retrievedFields) {
		Query<Annotation> q = this.createQuery().field("annotators.withCreator").equal(userId).field("target.recordId")
				.equal(recordId).retrievedFields(true, retrievedFields.toArray(new String[retrievedFields.size()]));
		return this.find(q).asList();
	}

	public long countUserCreatedAnnotations(ObjectId userId) {
		Query<Annotation> q = this.createQuery();
		q.criteria("annotators.withCreator").equal(userId);
		long count = q.countAll();
		return count;
	}

	public long countUserUpvotedAnnotations(ObjectId userId) {
		Query<Annotation> q = this.createQuery();
		q.criteria("score.approvedBy.withCreator").equal(userId);
		long count = q.countAll();
		return count;
	}

	public long countUserDownvotedAnnotations(ObjectId userId) {
		Query<Annotation> q = this.createQuery();
		q.criteria("score.rejectedBy.withCreator").equal(userId);
		long count = q.countAll();
		return count;
	}

	// TODO: Mongo distinct count
	@SuppressWarnings("unchecked")
	public long countUserAnnotatedRecords(ObjectId userId) {
		Query<Annotation> q = this.createQuery();
		q.or(q.criteria("annotators.withCreator").equal(userId), 
			 q.criteria("score.approvedBy.withCreator").equal(userId),
			 q.criteria("score.rejectedBy.withCreator").equal(userId)
			);
		q.retrievedFields(true, new String[] { "target.recordId" });
		
		List<Annotation> annotations = this.find(q).asList();
		List<ObjectId> recordIds = (List<ObjectId>) CollectionUtils.collect(annotations,
				new BeanToPropertyValueTransformer("target.recordId"));
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

	public void addApprove(ObjectId id, ObjectId userId) {
		Query<Annotation> q = this.createQuery().field("_id").equal(id);
		UpdateOperations<Annotation> updateOps = this.createUpdateOperations();
		updateOps.add("score.approvedBy", userId, false);
		updateOps.removeAll("score.rejectedBy", userId);
		this.update(q, updateOps);
	}

	public void addApproveObject(ObjectId id, ObjectId userId, AnnotationAdmin user) {
		Query<Annotation> q = this.createQuery().field("_id").equal(id);
		UpdateOperations<Annotation> updateOps = this.createUpdateOperations();
		updateOps.add("score.approvedBy", user, false);
		AnnotationAdmin u = new AnnotationAdmin();
		u.setWithCreator(userId);
		updateOps.removeAll("score.rejectedBy", u);
		this.update(q, updateOps);
	}

	public void removeScore(ObjectId id, ObjectId userId) {
		Query<Annotation> q = this.createQuery().field("_id").equal(id);
		UpdateOperations<Annotation> updateOps = this.createUpdateOperations();
		updateOps.removeAll("score.approvedBy", userId);
		updateOps.removeAll("score.rejectedBy", userId);
		this.update(q, updateOps);
	}

	public void removeScoreObject(ObjectId id, ObjectId userId) {
		Query<Annotation> q = this.createQuery().field("_id").equal(id);
		UpdateOperations<Annotation> updateOps = this.createUpdateOperations();
		AnnotationAdmin u = new AnnotationAdmin();
		u.setWithCreator(userId);
		updateOps.removeAll("score.approvedBy", u);
		updateOps.removeAll("score.rejectedBy", u);
		this.update(q, updateOps);
	}

	public void addReject(ObjectId id, ObjectId userId) {
		Query<Annotation> q = this.createQuery().field("_id").equal(id);
		UpdateOperations<Annotation> updateOps = this.createUpdateOperations();
		updateOps.add("score.rejectedBy", userId, false);
		updateOps.removeAll("score.approvedBy", userId);
		this.update(q, updateOps);
	}

	public void addRejectObject(ObjectId id, ObjectId userId, AnnotationAdmin user) {
		Query<Annotation> q = this.createQuery().field("_id").equal(id);
		UpdateOperations<Annotation> updateOps = this.createUpdateOperations();
		updateOps.add("score.rejectedBy", user, false);
		AnnotationAdmin u = new AnnotationAdmin();
		u.setWithCreator(userId);
		updateOps.removeAll("score.approvedBy", u);
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

	public void deleteCampaignAnnotations(ObjectId campaignId) {
		String campaignName = DB.getCampaignDAO().getById(campaignId).getUsername();
		Query<Annotation> q = this.createQuery().field("annotators.generator").endsWith(campaignName);
		this.find(q.retrievedFields(true, "_id", "target")).asList().stream()
				.forEach(ann -> DB.getRecordResourceDAO().removeAnnotation(ann.getTarget().getRecordId(), ann.getDbId(),
						((AnnotationAdmin) ann.getAnnotators().get(0)).getWithCreator().toHexString()));
		this.deleteByQuery(q);
	}

	public void deleteAnnotation(ObjectId annotationId) {
		Annotation annotation = this.get(annotationId);
		ObjectId recordId = annotation.getTarget().getRecordId();
		DB.getRecordResourceDAO().removeAnnotation(recordId, annotationId,
				((AnnotationAdmin) annotation.getAnnotators().get(0)).getWithCreator().toHexString());
		this.deleteById(annotationId);
	}
}
