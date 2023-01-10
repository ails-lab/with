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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Collection;

import model.annotations.Annotation;
import model.annotations.Annotation.AnnotationAdmin;
import model.annotations.Annotation.AnnotationScore;
import model.annotations.Annotation.MotivationType;
import model.annotations.bodies.*;
import model.annotations.selectors.SelectorType;
import model.annotations.targets.AnnotationTarget;
import model.basicDataTypes.Language;
import model.resources.RecordResource;
import model.resources.WithResourceType;
import model.resources.collection.CollectionObject;
import play.Logger;
import play.libs.Json;

import org.apache.commons.beanutils.BeanToPropertyValueTransformer;
import org.apache.commons.collections.CollectionUtils;
import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;

import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHits;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.WriteResult;

import elastic.ElasticSearcher;
import elastic.ElasticSearcher.SearchOptions;

@SuppressWarnings("rawtypes")
public class AnnotationDAO extends DAO<Annotation> {

	public AnnotationDAO() {
		super(Annotation.class);
	}

	public List<Annotation> getByIds(Collection<ObjectId> annotationIds) {
		if (annotationIds == null || annotationIds.isEmpty())
			return new ArrayList<Annotation>();
		try {
			Query<Annotation> q = this.createQuery().field("_id").in(annotationIds);
			return this.find(q).asList();
		} catch (Exception e) {
			return new ArrayList<Annotation>();
		}
	}

	public List<Annotation> getByIdsWithRetrievedFields(Collection<ObjectId> annotationIds, List<String> retrievedFields) {
		if (annotationIds == null || annotationIds.isEmpty())
			return new ArrayList<Annotation>();
		try {
			Query<Annotation> q = this.createQuery().field("_id").in(annotationIds)
													.retrievedFields(true, retrievedFields.toArray(new String[retrievedFields.size()]));
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

	public List<Annotation> getByLabel(List<String> generators, String label) {
		if (generators.size() > 0) {
			Query<Annotation> q = this.createQuery().disableValidation();
			q.or(q.criteria("body.label.en").equal(label), q.criteria("body.label.default").equal(label));
			q.field("annotators.generator").in(generators).order("score.approvedBy");
			List<Annotation> anns = this.find(q).asList();
			return anns;
		} else {
			return new ArrayList<Annotation>();
		}
	}

	public Map<String, Integer> getByCampaign(String campaignName, String term, int offset, int count) {
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		BoolQueryBuilder langQuery = QueryBuilders.boolQuery();
		langQuery.should(QueryBuilders.regexpQuery("body.label.default", term + ".*"));
		langQuery.should(QueryBuilders.regexpQuery("body.label.en", term + ".*"));
		query.must(langQuery);
		query.must(QueryBuilders.regexpQuery("annotators.generator", campaignName.split("-")[0] + ".*"));
		SearchOptions so = new SearchOptions(offset, count);
		so.searchFields = new String[] { "annlabel" };

		ElasticSearcher searcher = new ElasticSearcher();
		searcher.setTypes(new ArrayList<String>() {
			{
				add("annotation");
			}
		});
		AggregationBuilder aggregation = AggregationBuilders.terms("agg").field("annlabel.raw").size(count);
		SearchRequestBuilder srb = searcher.getSearchRequestBuilder(query, so).addAggregation(aggregation);
		SearchResponse sr = srb.execute().actionGet();
		HashMap<String, Integer> result = new HashMap<String, Integer>();
		Terms agg = sr.getAggregations().get("agg");
		for (Terms.Bucket entry : agg.getBuckets()) {
			String key = (String) entry.getKey();
			long docCount = entry.getDocCount();
			result.put(key, Integer.valueOf((int) docCount));
		}
		return result;
	}

	public List<Annotation> getApprovedByRecordId(ObjectId recordId, List<Annotation.MotivationType> motivations,
			List<String> retrievedFields) {
		Query<Annotation> q = this.createQuery().disableValidation().field("target.recordId").equal(recordId)
				.field("motivation").hasAnyOf(motivations).field("score.approvedBy").exists().field("score.approvedBy")
				.notEqual(new String[0])
				.retrievedFields(true, retrievedFields.toArray(new String[retrievedFields.size()]));
		return this.find(q).asList();
	}

	public List<Annotation> getByRecordId(ObjectId recordId, List<Annotation.MotivationType> motivations, List<String> generators) {
		Query<Annotation> q = this.createQuery().disableValidation().field("target.recordId").equal(recordId)
				.field("motivation").hasAnyOf(motivations);
		if (generators != null && generators.size()>0) {
			q = q.field("annotators.generator").hasAnyOf(generators);
		}
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
		else if (annotation.getMotivation().equals(MotivationType.Commenting)) {
			AnnotationBodyCommenting body = (AnnotationBodyCommenting) annotation.getBody();

			q.field("body.label.default").equal(body.getLabel().get(Language.DEFAULT));
		}

		SelectorType selector = target.getSelector();
		if (selector != null) {
			selector.addToQuery(q);
		} else {
			q.field("target.selector").doesNotExist();
		}

		return this.findOne(q);
	}

	public List<Annotation> getUserAnnotations(ObjectId userId, String project, String campaign, int offset,
			int count) {
		Query<Annotation> q = this.createQuery().field("annotators.withCreator").equal(userId)
				.field("annotators.generator").equal(project + ' ' + campaign).offset(offset).limit(count);
		return this.find(q).asList();
	}

	public List<Annotation> getUserAnnotations(ObjectId userId, String project, String campaign,
			List<String> retrievedFields) {
		Query<Annotation> q = this.createQuery().field("annotators.generator").equal(project + " " + campaign);
		q.or(q.criteria("annotators.withCreator").equal(userId),
				q.criteria("score.approvedBy.withCreator").equal(userId),
				q.criteria("score.rejectedBy.withCreator").equal(userId));
		q.retrievedFields(true, retrievedFields.toArray(new String[retrievedFields.size()]));
		return this.find(q).asList();
	}

	public List<Annotation> getUserAnnotations(ObjectId userId, ObjectId recordId, List<String> retrievedFields) {
		Query<Annotation> q = this.createQuery().field("annotators.withCreator").equal(userId).field("target.recordId")
				.equal(recordId).retrievedFields(true, retrievedFields.toArray(new String[retrievedFields.size()]));
		return this.find(q).asList();
	}

	public long countUserCreatedAnnotations(ObjectId userId, String project, String campaign) {
		Query<Annotation> q = this.createQuery().field("annotators.generator").equal(project + ' ' + campaign);
		q.criteria("annotators.withCreator").equal(userId);
		long count = q.countAll();
		return count;
	}

	public long countUserUpvotedAnnotations(ObjectId userId, String project, String campaign) {
		Query<Annotation> q = this.createQuery().field("annotators.generator").equal(project + ' ' + campaign);
		q.criteria("score.approvedBy.withCreator").equal(userId);
		long count = q.countAll();
		return count;
	}

	public long countUserDownvotedAnnotations(ObjectId userId, String project, String campaign) {
		Query<Annotation> q = this.createQuery().field("annotators.generator").equal(project + ' ' + campaign);
		q.criteria("score.rejectedBy.withCreator").equal(userId);
		long count = q.countAll();
		return count;
	}

	public long countAnnotatedRecordsByLabel(String project, String campaign, String label) {
		Query<Annotation> q = this.createQuery().disableValidation()
				.field("annotators.generator").equal(project + ' ' + campaign);
		q.or(q.criteria("body.label.en").equal(label), q.criteria("body.label.default").equal(label));
		return this.find(q, "target").map(a -> a.getTarget().getRecordId()).distinct().count();
	}

	// TODO: Mongo distinct count
	@SuppressWarnings("unchecked")
	public long countUserAnnotatedRecords(ObjectId userId, String project, String campaign) {
		Query<Annotation> q = this.createQuery().field("annotators.generator").equal(project + ' ' + campaign);
		q.or(q.criteria("annotators.withCreator").equal(userId),
				q.criteria("score.approvedBy.withCreator").equal(userId),
				q.criteria("score.rejectedBy.withCreator").equal(userId));
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

	public void addRateObject(ObjectId id, ObjectId userId, AnnotationAdmin user) {
		Query<Annotation> q = this.createQuery().field("_id").equal(id);
		UpdateOperations<Annotation> updateOps = this.createUpdateOperations();
		updateOps.add("score.ratedBy", user, false);
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

	public void markAnnotationForPublish(ObjectId dbId) {
		Query<Annotation> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<Annotation> updateOps = this.createUpdateOperations();
		updateOps.set("publish", true);
		this.updateFirst(q, updateOps);
	}

	public void unmarkAnnotationForPublish(ObjectId dbId) {
		Query<Annotation> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<Annotation> updateOps = this.createUpdateOperations();
		updateOps.set("publish", false);
		this.updateFirst(q, updateOps);
	}
		
	public void initializeAnnotationsForPublish(ObjectId campaignId, Boolean allowRejected, int minScore) {
		String campaignName = DB.getCampaignDAO().getById(campaignId).getUsername();
		Query<Annotation> q = this.createQuery().field("annotators.generator").endsWith(campaignName);
		
		this.find(q).asList().stream()
			.forEach(ann -> {
				AnnotationScore score = ann.getScore();
				if (score == null) {
					this.unmarkAnnotationForPublish(ann.getDbId());
					return;
				}
				int app = (score == null || score.getApprovedBy() == null) ? 0 : score.getApprovedBy().size();
				int rej = (score == null || score.getRejectedBy() == null) ? 0 : score.getRejectedBy().size();
				if (app-rej >= minScore) {
					this.markAnnotationForPublish(ann.getDbId());
				} else {
					this.unmarkAnnotationForPublish(ann.getDbId());
				}
				if (!allowRejected && rej > 0) {
					this.unmarkAnnotationForPublish(ann.getDbId());
				}
			});
	}

	public void deleteCampaignAnnotations(ObjectId campaignId) {
		String campaignName = DB.getCampaignDAO().getById(campaignId).getUsername();
		Query<Annotation> q = this.createQuery().field("annotators.generator").endsWith(campaignName);
		this.find(q.retrievedFields(true, "_id", "target", "annotators")).asList().stream()
				.forEach(ann -> DB.getRecordResourceDAO().removeAnnotation(ann.getTarget().getRecordId(), ann.getDbId(),
						((AnnotationAdmin) ann.getAnnotators().get(0)).getWithCreator().toHexString()));
		this.deleteByQuery(q);
	}

	public List<Annotation> getCampaignAnnotations(ObjectId campaignId, Boolean filterPublish) {
		String campaignName = DB.getCampaignDAO().getById(campaignId).getUsername();
		Query<Annotation> q = this.createQuery().field("annotators.generator").endsWith(campaignName);
		return this.find(q).asList();
	}

	public List<Annotation> getCampaignAnnotations(String campaignName, Boolean filterPublish) {
		Query<Annotation> q = this.createQuery().field("annotators.generator").endsWith(campaignName);
		if (filterPublish) {
			q = q.field("publish").equal(true);
		}
		Iterator<Annotation> i = this.find(q).iterator();
		List<Annotation> annotations = new ArrayList<Annotation>();
		while (i.hasNext()) {
			try {
				annotations.add(i.next());
			} catch (Exception e) {
				Logger.error(e.getMessage());
			}
		}
		return annotations;
	}
	
	public ObjectNode getCampaignAnnotationsStatistics(String cname) {
		ObjectNode statistics = Json.newObject();
		ObjectMapper mapper = new ObjectMapper();
		
		Query<Annotation> queryAnnotationsMarkedForPublish = this.createQuery().field("annotators.generator").endsWith(cname).field("publish").equal(true);
		Query<Annotation> queryAnnotationsNotMarkedForPublish = this.createQuery().field("annotators.generator").endsWith(cname).field("publish").equal(false);
		Query<Annotation> queryTotalAnnotations = this.createQuery().field("annotators.generator").endsWith(cname);
		Query<Annotation> queryComputerGeneratedAnnotations = this.createQuery().field("annotators.generator").endsWith(cname).field("annotators.externalCreatorType").equal("Software");

		long publishAnns = queryAnnotationsMarkedForPublish.countAll();
		long unpublishAnns = queryAnnotationsNotMarkedForPublish.countAll();
		long totalAnns = queryTotalAnnotations.countAll();
		long softwareGeneratedAnnotations = queryComputerGeneratedAnnotations.countAll();
		long humanGeneratedAnnotations = totalAnns - softwareGeneratedAnnotations;

		Map<String, Integer> recordAnnCount = new HashMap<String, Integer>();
		Map<String, Integer> annDateCount = new HashMap<String, Integer>();
		List<Integer> votes = new ArrayList<Integer>();
		votes.add(0);
		votes.add(0);
		votes.add(0);
		this.find(queryTotalAnnotations).asList().stream()
			.forEach(ann -> {

				String recId = ann.getTarget().getRecordId().toString();
				Integer annCount = recordAnnCount.get(recId);
				recordAnnCount.put(recId, (annCount == null) ? 1 : annCount + 1);

				AnnotationAdmin createdAdmin = (AnnotationAdmin) ann.getAnnotators().get(0);
				String creationDate = createdAdmin.getCreated().toInstant().toString().substring(0, 10);
				Integer dateCount = annDateCount.get(creationDate);
				annDateCount.put(creationDate, (dateCount == null) ? 1 : dateCount + 1);
				
				AnnotationScore score = ann.getScore();
				int up = (score == null || score.getApprovedBy() == null) ? votes.get(0) : votes.get(0) + score.getApprovedBy().size();
				int down = (score == null || score.getRejectedBy() == null) ? votes.get(1) : votes.get(1) + score.getRejectedBy().size();
				int rate = (score == null || score.getRatedBy() == null) ? votes.get(2) : votes.get(2) + score.getRatedBy().size();
				votes.clear();
				votes.add(0, up);
				votes.add(1, down);
				votes.add(2, rate);
			});
		Map<Integer, Integer> annCountFreq = new HashMap<Integer, Integer>();
		for (Integer count : recordAnnCount.values()) {
			Integer countFreq = annCountFreq.get(count);
			annCountFreq.put(count, (countFreq == null) ? 1 : countFreq + 1);
		}
		TreeMap<String, Integer> sortedAnnDateCount = new TreeMap<>();
		sortedAnnDateCount.putAll(annDateCount);

		statistics.put("annotations-total", totalAnns);
		statistics.put("annotations-software", softwareGeneratedAnnotations);
		statistics.put("annotations-human", humanGeneratedAnnotations);
		statistics.put("annotations-accepted", publishAnns);
		statistics.put("annotations-rejected", unpublishAnns);
		statistics.put("items-annotated", recordAnnCount.size());
		statistics.put("annotation-count-frequency", mapper.valueToTree(annCountFreq));
		statistics.put("annotation-date-frequency", mapper.valueToTree(sortedAnnDateCount));
		statistics.put("upvotes", votes.get(0));
		statistics.put("downvotes", votes.get(1));
		statistics.put("rates", votes.get(2));
		
		return statistics;
	}

	public void unscoreAutomaticAnnotations() {
		Query<Annotation> q = this.createQuery().field("annotators.generator").equal("Image Analysis");
		UpdateOperations<Annotation> updateOps = this.createUpdateOperations();
		updateOps.unset("score");
		this.update(q, updateOps);
	}

	public void deleteAnnotation(ObjectId annotationId) {
		Annotation annotation = this.get(annotationId);
		ObjectId recordId = annotation.getTarget().getRecordId();
		DB.getRecordResourceDAO().removeAnnotation(recordId, annotationId,
				((AnnotationAdmin) annotation.getAnnotators().get(0)).getWithCreator().toHexString());
		this.deleteById(annotationId);
	}
}
