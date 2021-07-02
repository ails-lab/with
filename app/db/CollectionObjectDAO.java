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
import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.CriteriaContainer;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryResults;
import org.mongodb.morphia.query.UpdateOperations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBObject;

import controllers.MediaController;
import db.DAO.QueryOperator;
import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.WithMediaType;
import model.MediaObject;
import model.annotations.ContextData;
import model.annotations.ContextData.ContextDataBody;
import model.basicDataTypes.WithAccess.Access;
import model.resources.RecordResource;
import model.resources.WithResourceType;
import model.resources.collection.CollectionObject;
import play.libs.Json;
import utils.Tuple;

@SuppressWarnings({ "rawtypes", "unchecked", "serial" })
public class CollectionObjectDAO extends WithResourceDAO<CollectionObject> {

	/*
	 * The constructor is optional because the explicit type is passed through
	 * generics.
	 */
	public CollectionObjectDAO() {
		super(CollectionObject.class);
	}

	public boolean isFavorites(ObjectId dbId) {
		Query<CollectionObject> q = this.createQuery().field("_id").equal(dbId);
		q.field("descriptiveData.label.def").equal("favorites");
		return (this.find(q).asList().size() == 0 ? false : true);
	}

	/**
	 * Increment entryCount (number of entries collected) in a CollectionObject
	 *
	 * @param dbId
	 */
	public void incEntryCount(ObjectId dbId) {
		Query<CollectionObject> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<CollectionObject> updateOps = this
				.createUpdateOperations().disableValidation();
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
				.createUpdateOperations().disableValidation();
		updateOps.set("administrative.lastModified", new Date());
		updateOps.dec("administrative.entryCount");
		this.update(q, updateOps);
	}


	public CollectionObject getSliceOfCollectedResources(ObjectId id, int startIdx, int slice) {
		BasicDBObject query = new BasicDBObject("_id", id);
		BasicDBObject projections = new BasicDBObject();
		projections.put("collectedResources", 1);
		projections.put("descriptiveData", 0);
		projections.put("media", 0);
		projections.put("usage", 0);
		projections.put("collectedResources", new BasicDBObject("$slice", new int[] { startIdx, slice }));
		return DB.getMorphia().fromDBObject(CollectionObject.class,
				this.getCollection().findOne(query, projections));
	}

	public void editCollection(ObjectId dbId, JsonNode json) {
		Query<CollectionObject> q = this.createQuery().field("_id").equal(dbId);
		UpdateOperations<CollectionObject> updateOps = this
				.createUpdateOperations();
		updateFields("", json, updateOps);
		updateOps.set("administrative.lastModified", new Date());
		this.update(q, updateOps);
	}

	public List<CollectionObject> getBySpecificAccess(
			List<ObjectId> effectiveIds, Access access, Boolean isExhibition,
			int offset, int count) {
		WithResourceType collectionType = isExhibition ? WithResourceType.Exhibition
				: WithResourceType.SimpleCollection;
		Query<CollectionObject> q = this.createQuery()
				.field("resourceType").equal(collectionType)
				.order("-administrative.lastModified").offset(offset)
				.limit(count);
		Criteria[] criteria = new Criteria[effectiveIds.size()];
		for (int i = 0; i < effectiveIds.size(); i++) {
			criteria[i] = formAccessLevelQuery(
					new Tuple(effectiveIds.get(i), Access.READ),
					QueryOperator.EQ);
		}
		q.or(criteria);
		return this.find(q).asList();
	}

	public Tuple<Integer, Integer> getHits(Query<CollectionObject> q,
			Optional<WithResourceType> collectionType) {
		Tuple<Integer, Integer> hits = new Tuple<Integer, Integer>(0, 0);
		if (!collectionType.isPresent()) {
			Query<CollectionObject> q2 = q.cloneQuery();
			q2.field("resourceType").equal("Exhibition");
			q.field("resourceType").equal("SimpleCollection");
			hits.x = (int) this.find(q).countAll();
			hits.y = (int) this.find(q2).countAll();
		} else {

			WithResourceType collectionTypeValue = collectionType.get();
			q.field("resourceType").equal(
					collectionTypeValue.toString());
			if (collectionTypeValue.equals(WithResourceType.Exhibition))
				hits.y = (int) this.find(q).countAll();
			else
				hits.x = (int) this.find(q).countAll();
		}
		return hits;
	}

	public Tuple<List<CollectionObject>, Tuple<Integer, Integer>> getCollectionsWithCount(
			Query<CollectionObject> q, Boolean isExhibition) {
		Tuple<Integer, Integer> hits = new Tuple<Integer, Integer>(0, 0);
		QueryResults<CollectionObject> result;
		List<CollectionObject> collections = new ArrayList<CollectionObject>();
		if (isExhibition == null) {
			result = this.find(q);
			collections = result.asList();
			Query<CollectionObject> q2 = q.cloneQuery().disableValidation();
			q2.field("resourceType").equal(
					WithResourceType.Exhibition);
			q.disableValidation().field("resourceType")
					.equal(WithResourceType.SimpleCollection);
			hits.x = (int) this.find(q).countAll();
			hits.y = (int) this.find(q2).countAll();
		} else {
			WithResourceType collectionType = isExhibition ? WithResourceType.Exhibition
					: WithResourceType.SimpleCollection;
			q.disableValidation().field("resourceType")
					.equal(collectionType);
			result = this.find(q);
			collections = result.asList();
			if (isExhibition)
				hits.y = (int) result.countAll();
			else
				hits.x = (int) result.countAll();
		}
		return new Tuple<List<CollectionObject>, Tuple<Integer, Integer>>(
				collections, hits);
	}

	/**
	 * Return the total number of CollectionObject entities for a specific query
	 *
	 * @param q
	 * @param isExhibition
	 * @return
	 */
	public Tuple<Integer, Integer> getCollectionsCount(
			Query<CollectionObject> q, Boolean isExhibition) {
		Tuple<Integer, Integer> hits = new Tuple<Integer, Integer>(0, 0);
		if (isExhibition == null) {
			Query<CollectionObject> q2 = q.cloneQuery();
			q2.field("resourceType").equal(
					WithResourceType.Exhibition);
			q.field("resourceType").equal(
					WithResourceType.SimpleCollection);
			hits.x = (int) this.find(q).countAll();
			hits.y = (int) this.find(q2).countAll();
		} else {
			WithResourceType collectionType = isExhibition ? WithResourceType.Exhibition
					: WithResourceType.SimpleCollection;
			q.field("resourceType").equal(collectionType);
			if (isExhibition)
				hits.y = (int) this.find(q).countAll();
			else
				hits.x = (int) this.find(q).countAll();
		}
		return hits;
	}

	/**
	 * Return CollectionObjects (bounded by a limit) that satisfy the logged in
	 * user's access criteria and optionally some other user access criteria.
	 *
	 * @param loggeInEffIds
	 * @param accessedByUserOrGroup
	 * @param creator
	 * @param isExhibition
	 * @param totalHits
	 * @param offset
	 * @param count
	 * @return
	 */
	public Tuple<List<CollectionObject>, Tuple<Integer, Integer>> getByLoggedInUsersAndAcl(
			List<ObjectId> loggeInEffIds,
			List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup,
			ObjectId creator, Boolean isExhibition, boolean totalHits,
			int offset, int count, String sortBy) {
		List<Criteria> criteria = new ArrayList<Criteria>(
				Arrays.asList(loggedInUserWithAtLeastAccessQuery(loggeInEffIds,
						Access.READ)));
		return getByAcl(criteria, accessedByUserOrGroup, creator, isExhibition,
				totalHits, offset, count, sortBy);
	}

	/**
	 * Return public CollectionObjects (bounded by a limit) that also satisfy
	 * some user access criteria.
	 *
	 * @param accessedByUserOrGroup
	 * @param creator
	 * @param isExhibition
	 * @param totalHits
	 * @param offset
	 * @param count
	 * @return
	 */
	public Tuple<List<CollectionObject>, Tuple<Integer, Integer>> getPublicAndByAcl(
			List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup,
			ObjectId creator, Boolean isExhibition, boolean totalHits,
			int offset, int count) {
		List<Criteria> criteria = new ArrayList<Criteria>(Arrays.asList(
				this.createQuery().criteria("administrative.access.isPublic")
						.equal(true)));
		return getByAcl(criteria, accessedByUserOrGroup, creator, isExhibition,
				totalHits, offset, count, "Date");
	}

	public List<CollectionObject> getAccessibleByGroupAndPublic(ObjectId groupId) {
		Query<CollectionObject> q = this.createQuery().disableValidation()
				.retrievedFields(true, "_id", "administrative.entryCount")
				.field("descriptiveData.label.default.0")
				.notEqual("_favorites").field("administrative.access.isPublic").equal(true);
		q.field("resourceType").equal(WithResourceType.SimpleCollection);
		q.and(formAccessLevelQuery(new Tuple(groupId, Access.READ), QueryOperator.GTE));
		return this.find(q).asList();
	}
	
	public List<CollectionObject> getAccessibleByGroup(ObjectId groupId) {
		Query<CollectionObject> q = this.createQuery().disableValidation()
				.retrievedFields(true, "_id", "administrative.entryCount")
				.field("descriptiveData.label.default.0")
				.notEqual("_favorites");
		q.field("resourceType").equal(WithResourceType.SimpleCollection);
		q.and(formAccessLevelQuery(new Tuple(groupId, Access.READ), QueryOperator.GTE));
		return this.find(q).asList();
	}

	public Tuple<List<CollectionObject>, Tuple<Integer, Integer>> getSharedAndByAcl(
			List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup,
			ObjectId creator, Boolean isExhibition, boolean totalHits,
			int offset, int count, String sortBy) {
		List<Criteria> criteria = new ArrayList<Criteria>(Arrays.asList(
				this.createQuery().criteria("administrative.withCreator")
						.notEqual(creator)));
		return getByAcl(criteria, accessedByUserOrGroup, creator, isExhibition,
				totalHits, offset, count, sortBy);
	}

	public Tuple<List<CollectionObject>, Tuple<Integer, Integer>> getByAcl(
			List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup,
			ObjectId creator, Boolean isExhibition, boolean totalHits,
			int offset, int count, String sortBy) {
		return getByAcl(new ArrayList<Criteria>(), accessedByUserOrGroup,
				creator, isExhibition, totalHits, offset, count, sortBy);
	}

	public Tuple<List<CollectionObject>, Tuple<Integer, Integer>> getByAcl(
			List<Criteria> andCriteria,
			List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup,
			ObjectId creator, Boolean isExhibition, boolean totalHits,
			int offset, int count, String sortBy) {
		Query<CollectionObject> q = this.createQuery().disableValidation()
				.retrievedFields(false, "collectedResources")
				.field("descriptiveData.label.default.0")
				.notEqual("_favorites");
		String orderBy =  sortBy.equals("Date") ? "-administrative.lastModified" : "descriptiveData.label.default";
		q.order(orderBy).offset(offset).limit(count);
		if (creator != null)
			q.field("administrative.withCreator").equal(creator);
		for (List<Tuple<ObjectId, Access>> orAccessed : accessedByUserOrGroup) {
			andCriteria.add(atLeastAccessCriteria(orAccessed));
		}
		if (andCriteria.size() > 0)
			q.and(andCriteria.toArray(new Criteria[andCriteria.size()]));
		if (totalHits) {
			return getCollectionsWithCount(q, isExhibition);
		} else {
			if (isExhibition != null) {
				WithResourceType collectionType = isExhibition ? WithResourceType.Exhibition
						: WithResourceType.SimpleCollection;
				q.field("resourceType").equal(collectionType);
			}
			return new Tuple<List<CollectionObject>, Tuple<Integer, Integer>>(
					this.find(q).asList(), null);
		}
	}

	public ObjectNode countMyAndSharedCollections(
			List<ObjectId> loggedInEffIds) {
		ObjectNode result = Json.newObject().objectNode();
		Query<CollectionObject> qMy = this.createQuery().disableValidation()
				.field("descriptiveData.label.default.0")
				.notEqual("_favorites");

		// count my collections-exhibitions
		qMy.field("administrative.withCreator").equal(loggedInEffIds.get(0));
		ObjectNode result1 = countPerCollectionType(qMy);
		result.put("my", result1);
		// count collections-exhibitions shared with me
		Query<CollectionObject> qShared = this.createQuery().disableValidation()
				.field("administrative.withCreator")
				.notEqual(loggedInEffIds.get(0));
		List<Criteria> criteria = new ArrayList<Criteria>(
				Arrays.asList(atLeastAccessCriteria(
						Arrays.asList(new Tuple(loggedInEffIds.get(0), Access.READ)))));
		qShared.and(criteria.toArray(new Criteria[criteria.size()]));
		ObjectNode result2 = countPerCollectionType(qShared);
		result.put("sharedWithMe", result2);
		return result;
	}

	public ObjectNode countPerCollectionType(Query<CollectionObject> q) {
		ObjectNode result = Json.newObject().objectNode();
		Query<CollectionObject> q1 = q.cloneQuery();
		q1.field("resourceType").equal(WithResourceType.SimpleCollection);
		long count = this.find(q1).countAll();
		result.put(WithResourceType.SimpleCollection.toString(), count);
		Query<CollectionObject> q2 = q.cloneQuery();
		q2.field("resourceType").equal(WithResourceType.Exhibition);
		count = this.find(q2).countAll();
		result.put(WithResourceType.Exhibition.toString(), count);
		return result;
	}

	public List<CollectionObject> getAtLeastCollections(
			List<ObjectId> loggeInEffIds, Access access, int offset,
			int count) {
		CriteriaContainer criteria = loggedInUserWithAtLeastAccessQuery(
				loggeInEffIds, access);
		Query<CollectionObject> q = this.createQuery().offset(offset)
				.limit(count + 1).retrievedFields(true, "_id");
		q.and(criteria);
		return this.find(q).asList();
	}

	public CollectionObject addToCollection(ObjectId collectionId,
			ObjectId recordId, int position, boolean last) {

		Query<CollectionObject> q = DB.getCollectionObjectDAO().createQuery()
				.field("_id").equal(collectionId);
		UpdateOperations<CollectionObject> collectionUpdate = DB
				.getCollectionObjectDAO().createUpdateOperations()
				.disableValidation();
		if (last) {
			collectionUpdate.add("collectedResources",
					new ContextData(recordId), true);
		} else {
			List<ContextData<ContextDataBody>> collectedResources = this
					.getById(collectionId, Arrays.asList("collectedResources"))
					.getCollectedResources();
			collectedResources.add(position, new ContextData(recordId));
			collectionUpdate.set("collectedResources", collectedResources);
		}
		collectionUpdate.inc("administrative.entryCount");
		collectionUpdate.set("administrative.lastModified", new Date());
		// true returns the oldVersion (contrary to documentation!!!)
		return DB.getDs().findAndModify(q, collectionUpdate, true);
	}

	public CollectionObject removeFromCollection(ObjectId collectionId, ObjectId recordId,
			int position, boolean first, boolean all) throws Exception {
		CollectionObject collection = this.getById(collectionId,
				Arrays.asList("collectedResources", "administrative.access"));
		int i = 0;
		List<ContextData> newCollectedResources = new ArrayList<ContextData>(
				collection.getCollectedResources());
		int resourcesRemoved = 0;
		ArrayList<Integer> positions = new ArrayList<Integer>();
		if (!first && !all) {
			ContextData resource = newCollectedResources.remove(position);
			resourcesRemoved = 1;
			if (!resource.getTarget().getRecordId().equals(recordId))
				throw new Exception("Invalid position");
		} else {
			for (ContextData data : (List<ContextData>) collection.getCollectedResources()) {
				if (data.getTarget().getRecordId().equals(recordId)) {
					if (first) {
						newCollectedResources.remove(i);
						resourcesRemoved = 1;
						break;
					}
					if (all) {
						newCollectedResources.remove(i - resourcesRemoved);
						positions.add(i);
						resourcesRemoved++;
					}
				}
				i++;
			}
		}
		if (resourcesRemoved == 0)
			throw new Exception("Record not in collection");
		Query<CollectionObject> q = DB.getCollectionObjectDAO().createQuery()
				.field("_id").equal(collectionId);
		UpdateOperations<CollectionObject> collectionUpdate = DB
				.getCollectionObjectDAO().createUpdateOperations()
				.disableValidation();
		collectionUpdate.set("collectedResources", newCollectedResources);
		collectionUpdate.inc("administrative.entryCount", 0 - resourcesRemoved);
		collectionUpdate.set("administrative.lastModified", new Date());
		this.update(q, collectionUpdate);
		if (!all) {
			removeCollectionMedia(collectionId, i);
		} else {
			do {
				removeCollectionMedia(collectionId,
						positions.get(--resourcesRemoved));
			} while (resourcesRemoved > 0);
		}
		return collection;
	}

	public void moveInCollection(ObjectId collectionId, ObjectId recordId,
			int oldPosition, int newPosition) {
		CollectionObject collection = this.getById(collectionId,
				Arrays.asList("collectedResources"));
		List<ContextData<ContextDataBody>> collectedResources = collection
				.getCollectedResources();
		ObjectId collectedRecordId = collectedResources.get(oldPosition)
				.getTarget().getRecordId();
		if (!collectedRecordId.equals(recordId))
			return;
		ContextData<ContextDataBody> collectedRecord = collectedResources
				.remove(oldPosition);
		collectedResources.add(newPosition, collectedRecord);
		Query<CollectionObject> q = DB.getCollectionObjectDAO().createQuery()
				.field("_id").equal(collectionId);
		UpdateOperations<CollectionObject> collectionUpdate = DB
				.getCollectionObjectDAO().createUpdateOperations()
				.disableValidation();
		collectionUpdate.set("collectedResources", collectedResources);
		collectionUpdate.set("administrative.lastModified", new Date());
		this.update(q, collectionUpdate);
		removeCollectionMedia(collectionId, oldPosition);
		addCollectionMedia(collectionId, collectedRecordId, newPosition);
	}

	// it may happen that e.g. the thumbnail of the 4th instead of the 3d record
	// of the media appears in the collections's (3) media
	public void addCollectionMedia(ObjectId collectionId, ObjectId recordId,
			int position) {
		CollectionObject collection = this.getById(collectionId,
				Arrays.asList("media"));
		if (position > 4)
			return;
		List<HashMap<MediaVersion, EmbeddedMediaObject>> recordMedia = DB
				.getRecordResourceDAO()
				.getById(recordId,
						new ArrayList<String>(Arrays.asList("media")))
				.getMedia();
		EmbeddedMediaObject thumbnail;
		if (recordMedia != null) {
			HashMap<MediaVersion, EmbeddedMediaObject> media = recordMedia
					.get(0);
			if (media.containsKey(MediaVersion.Original)
					&& !media.containsKey(MediaVersion.Thumbnail)
					&& media.get(MediaVersion.Original).getType()
					.equals(WithMediaType.IMAGE)) {
				String originalUrl = media.get(MediaVersion.Original).getUrl();
				MediaObject original = MediaController
						.downloadMedia(originalUrl, MediaVersion.Original);
				thumbnail = new EmbeddedMediaObject(
						MediaController.makeThumbnail(original));
			} else {
				thumbnail = media.get(MediaVersion.Thumbnail);
			}
			if (thumbnail != null) {
				List<HashMap<MediaVersion, EmbeddedMediaObject>> collectionMedia = collection
						.getMedia();
				collectionMedia.add(position,
						new HashMap<MediaVersion, EmbeddedMediaObject>() {
							{
								put(MediaVersion.Thumbnail, thumbnail);
							}
						});
				UpdateOperations<CollectionObject> collectionUpdate = DB
						.getCollectionObjectDAO().createUpdateOperations()
						.disableValidation();
				Query<CollectionObject> cq = DB.getCollectionObjectDAO()
						.createQuery().field("_id").equal(collectionId);
				collectionUpdate.set("media", collectionMedia.size() < 5
						? collectionMedia : collectionMedia.subList(0, 5));
				this.update(cq, collectionUpdate);
			}
		}

	}

	public void updateContextData(ObjectId collectionId, ContextData contextData, int position)
			throws Exception {
		ObjectId recordId = contextData.getTarget().getRecordId();
		List<ContextData<ContextDataBody>> collectedResources = this
				.getById(collectionId, Arrays.asList("collectedResources"))
				.getCollectedResources();
		if (!collectedResources.get(position).getTarget().getRecordId()
				.equals(recordId))
			throw new Exception("Invalid record position");
		Query<CollectionObject> q = this.createQuery().field("_id")
				.equal(collectionId);
		UpdateOperations<CollectionObject> updateOps = this
				.createUpdateOperations();
		ObjectNode contextDataJson = ((ObjectNode) Json.toJson(contextData))
				.put("className", contextData.getClass().getName());
		updateFields("collectedResources." + position, contextDataJson,
				updateOps);
		updateOps.set("administrative.lastModified", new Date());
		this.update(q, updateOps);
	}

	/*
	 * public void addCollectionMediaAsync(ObjectId collectionId, ObjectId
	 * recordId) { BiFunction<ObjectId, ObjectId, Boolean> methodQuery =
	 * (ObjectId colId, ObjectId recId) -> { try { addCollectionMedia(colId,
	 * recId); return true; } catch (Exception e) { return false; }
	 *
	 * }; ParallelAPICall.createPromise(methodQuery, collectionId, recordId); }
	 */
	public void removeCollectionMedia(ObjectId collectionId, int position) {
		if (position > 4)
			return;
		List<HashMap<MediaVersion, EmbeddedMediaObject>> collectionMedia = this
				.getById(collectionId, Arrays.asList("media")).getMedia();
		collectionMedia.remove(position);
		for (int i = collectionMedia.size(); i < 5; i++) {
			RecordResource record = DB.getRecordResourceDAO()
					.getByCollectionAndPosition(collectionId, i);
			if (record == null)
				break;
			HashMap<MediaVersion, EmbeddedMediaObject> media = (HashMap<MediaVersion, EmbeddedMediaObject>) record
					.getMedia().get(0);
			EmbeddedMediaObject thumbnail = media.get(MediaVersion.Thumbnail);
			HashMap<MediaVersion, EmbeddedMediaObject> colMedia = new HashMap<MediaVersion, EmbeddedMediaObject>() {
				{
					put(MediaVersion.Thumbnail, thumbnail);
				}
			};
			collectionMedia.add(colMedia);
		}
		if (collectionMedia.isEmpty()) {
			HashMap<MediaVersion, EmbeddedMediaObject> emptyMedia = new HashMap<MediaVersion, EmbeddedMediaObject>() {
				{
					put(MediaVersion.Thumbnail, new EmbeddedMediaObject());
				}
			};
			collectionMedia.add(emptyMedia);
		}
		UpdateOperations<CollectionObject> colUpdate = DB
				.getCollectionObjectDAO().createUpdateOperations()
				.disableValidation();
		Query<CollectionObject> cq = DB.getCollectionObjectDAO().createQuery()
				.field("_id").equal(collectionId);
		colUpdate.set("media", collectionMedia);
		this.update(cq, colUpdate);

	}

	public void updateBackgroundImg(ObjectId exhId, HashMap<MediaVersion, EmbeddedMediaObject> media) {
		UpdateOperations<CollectionObject> colUpdate = DB
				.getCollectionObjectDAO().createUpdateOperations()
				.disableValidation();
		Query<CollectionObject> cq = DB.getCollectionObjectDAO()
				.createQuery().field("_id").equal(exhId);
		colUpdate.set("backgroundImg", media);
		this.update(cq, colUpdate);
	}
	
	public int countCollectionItems(ObjectId dbId) {
		Query<CollectionObject> q = this.createQuery().field("_id").equal(dbId);
		CollectionObject collection = this.findOne(q);
		return collection.getCollectedResources().size();
	}
}
