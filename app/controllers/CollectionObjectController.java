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


package controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import controllers.parameterTypes.MyPlayList;
import controllers.parameterTypes.StringTuple;
import db.DB;
import model.annotations.ContextData;
import model.annotations.ContextData.ContextDataBody;
import model.basicDataTypes.Language;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import model.basicDataTypes.WithAccess.AccessEntry;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import model.resources.RecordResource;
import model.resources.WithResource;
import model.resources.WithResource.WithResourceType;
import model.resources.collection.CollectionObject;
import model.resources.collection.CollectionObject.CollectionAdmin;
import model.resources.collection.SimpleCollection;
import model.usersAndGroups.Organization;
import model.usersAndGroups.Page;
import model.usersAndGroups.Project;
import model.usersAndGroups.User;
import model.usersAndGroups.UserGroup;
import model.usersAndGroups.UserOrGroup;
import play.Logger;
import play.Logger.ALogger;
import play.data.validation.Validation;
import play.libs.F;
import play.libs.F.Function0;
import play.libs.F.Option;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Result;
import sources.EuropeanaCollectionSpaceSource;
import sources.EuropeanaSpaceSource;
import sources.OWLExporter.CulturalItemOWLExporter;
import sources.core.CommonQuery;
import sources.core.SourceResponse;
import sources.core.Utils;
import utils.Locks;
import utils.Tuple;


@SuppressWarnings("rawtypes")
public class CollectionObjectController extends WithResourceController {

	public static final ALogger log = Logger
			.of(CollectionObjectController.class);

	public static Promise<Result> importSearch() {
		JsonNode json = request().body().asJson();
		if (json == null) {
			return Promise.pure((Result) badRequest("Expecting Json query"));
		} else {
			// Parse the query.
			try {
				ObjectNode resultInfo = Json.newObject();
				ObjectId creatorDbId = new ObjectId(loggedInUser());
				final CommonQuery q = Utils.parseJson(json.get("query"));
				final String cname = json.get("collectionName").toString();
				final int limit = (json.has("limit")) ? json.get("limit")
						.asInt() : -1;
				CollectionObject ccid = null;
				if (!isCollectionCreated(creatorDbId, cname)) {
					CollectionObject collection = new SimpleCollection();
					collection.getDescriptiveData().setLabel(
							new MultiLiteral(cname).fillDEF());
					boolean success = internalAddCollection(collection,
							WithResourceType.SimpleCollection, creatorDbId,
							resultInfo);
					if (!success)
						return Promise
								.pure((Result) badRequest("Expecting Json query"));
					ccid = collection;
				} else {
					List<CollectionObject> col = DB.getCollectionObjectDAO()
							.getByLabel(Language.DEFAULT, cname);
					ccid = col.get(0);
				}

				EuropeanaSpaceSource src = new EuropeanaSpaceSource();
				src.setUsingCursor(true);

				return internalImport(src, ccid, q, limit, resultInfo, true,
						true);

			} catch (Exception e) {
				log.error( "", e );
				return Promise.pure((Result) badRequest(e.getMessage()));
			}
		}
	}

	public static Result importIDs(String cname, String source, String ids) {
		ObjectNode resultInfo = Json.newObject();
		ObjectId creatorDbId = new ObjectId(loggedInUser());
		CollectionObject ccid = null;
		if (!isCollectionCreated(creatorDbId, cname)) {
			CollectionObject collection = new SimpleCollection();
			collection.getDescriptiveData().setLabel(
					new MultiLiteral(cname).fillDEF());
			boolean success = internalAddCollection(collection,
					WithResourceType.SimpleCollection, creatorDbId, resultInfo);
			if (!success)
				return badRequest(resultInfo);
			ccid = collection;
		} else {
			List<CollectionObject> col = DB.getCollectionObjectDAO()
					.getByLabel(Language.DEFAULT, cname);
			ccid = col.get(0);
		}
		for (String oid : ids.split("[,\\s]+")) {
			CulturalObject record = new CulturalObject();
			CulturalObjectData descriptiveData = new CulturalObjectData();
			descriptiveData.setLabel(new MultiLiteral(oid).fillDEF());
			record.setDescriptiveData(descriptiveData);
			record.addToProvenance(new ProvenanceInfo(source, null, oid));
			internalAddRecordToCollection(ccid.getDbId().toString(), record,
					F.Option.None(), resultInfo);
		}
		return ok(resultInfo);
	}

	/**
	 * creates a new collection corresponding to a collection in Europeana and
	 * collects all its items.
	 *
	 * @param id
	 * @return
	 */
	public static Promise<Result> createAndFillEuropeanaCollection(String id,
			int limit) {
		CollectionObject collection = new SimpleCollection();
		collection.getDescriptiveData()
				.setLabel(new MultiLiteral(id).fillDEF());
		ObjectNode resultInfo = Json.newObject();
		ObjectId creatorDbId = new ObjectId(loggedInUser());
		boolean success = internalAddCollection(collection,
				WithResourceType.SimpleCollection, creatorDbId, resultInfo);
		if (!success)
			return Promise.pure((Result) badRequest(resultInfo));
		CommonQuery q = new CommonQuery();

		EuropeanaCollectionSpaceSource src = new EuropeanaCollectionSpaceSource(
				id);
		return internalImport(src, collection, q, limit, resultInfo, false,
				false);
	}

	private static Promise<Result> internalImport(EuropeanaSpaceSource src,
			CollectionObject collection, CommonQuery q, int limit,
			ObjectNode resultInfo, boolean dontDuplicate, boolean waitToFinish) {
		q.page = 1 + "";
		q.pageSize = "20";
		SourceResponse result = src.getResults(q);
		int total = result.totalCount;
		final int mylimit = (limit == -1) ? total : Math.min(limit, total);

		int firstPageCount1 = addResultToCollection(result, collection
				.getDbId().toString(), mylimit, resultInfo, dontDuplicate);

		Promise<Result> promiseOfInt = Promise.promise(new Function0<Result>() {
			public Result apply() {
				SourceResponse result;
				int page = 1;
				int itemsCount = firstPageCount1;
				while (itemsCount < mylimit) {
					page++;
					q.page = page + "";
					result = src.getResults(q);
					int c = addResultToCollection(result, collection.getDbId()
							.toString(), mylimit - itemsCount, resultInfo,
							dontDuplicate);
					itemsCount = itemsCount + c;
				}
				return ok(Json.toJson(collectionWithMyAccessData(
						collection,
						effectiveUserIds(), "BASIC", Option.Some("DEFAULT"))));
			}
		});
		if (resultInfo.has("error"))
			return Promise.pure((Result) badRequest(resultInfo));
		if (waitToFinish)
			return promiseOfInt;
		else
			return Promise.pure(ok(Json.toJson(collectionWithMyAccessData(
					collection,
					effectiveUserIds(), "BASIC", Option.Some("DEFAULT")))));
	}

	private static int addResultToCollection(SourceResponse result,
			String collectionID, int limit, ObjectNode resultInfo,
			boolean dontRepeat) {
		int itemsCount = 0;
		for (Iterator<WithResource<?, ?>> iterator = result.items
				.getCulturalCHO().iterator(); iterator.hasNext()
				&& (itemsCount < limit);) {
			WithResource<?, ?> item = iterator.next();
			WithResourceController.internalAddRecordToCollection(collectionID,
					(RecordResource) item, F.Option.None(), resultInfo,
					dontRepeat);
			itemsCount++;
		}
		return itemsCount;
	}

	public static Result sortCollectionObject(String collectionId) {
		ObjectNode result = Json.newObject();
		try {
			ObjectId collectionDbId = new ObjectId(collectionId);
			int entryCount = ((CollectionAdmin) DB
					.getCollectionObjectDAO()
					.getById(collectionDbId,
							Arrays.asList("administrative.entryCount"))
					.getAdministrative()).getEntryCount();
			//Logger.info("Sorting collection "+collectionId);
			List<RecordResource> records = DB.getRecordResourceDAO()
					.getByCollectionBetweenPositions(collectionDbId, 0,
							Math.min(entryCount, 1000));
			RecordResource<?>[] array = records
					.toArray(new RecordResource<?>[] {});
			Arrays.sort(array, Utils.compareThumbs);
			log.debug("Items sorted based on image quality");
			return ok();
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}
	
	public static Result exportCollectionObjectToOWL(String cname) {
		
		ObjectNode resultInfo = Json.newObject();
		ObjectId creatorDbId = new ObjectId(loggedInUser());
		CollectionObject ccid = null;
		if (!isCollectionCreated(creatorDbId, cname)) {
			return badRequest(resultInfo);
		} else {
			List<CollectionObject> col = DB.getCollectionObjectDAO()
					.getByLabel(Language.DEFAULT, cname);
			ccid = col.get(0);
		}
		ObjectNode result = Json.newObject();
		CulturalItemOWLExporter exporter = new CulturalItemOWLExporter(ccid.getDbId().toString());
		try {
			ObjectId collectionDbId = ccid.getDbId();
			int entryCount = ((CollectionAdmin) DB
					.getCollectionObjectDAO()
					.getById(collectionDbId,
							Arrays.asList("administrative.entryCount"))
					.getAdministrative()).getEntryCount();
			List<RecordResource> records = DB.getRecordResourceDAO()
					.getByCollectionBetweenPositions(collectionDbId, 0,
							Math.min(entryCount, 1000));
			
			for (RecordResource recordResource : records) {
				if (recordResource instanceof CulturalObject) {
					CulturalObject new_record = (CulturalObject) recordResource;
					exporter.exportItem(new_record);
				}
			}
			return ok(exporter.export());
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	/**
	 * Creates a new Collection from the JSON body
	 *
	 * @param collectionType
	 *            the collection type that can take values of :
	 *            {SimpleCollection, Exhibition }
	 * @return the newly created collection
	 */
	public static Result createCollectionObject() {
		ObjectNode error = Json.newObject();
		JsonNode json = request().body().asJson();
		if (json == null) {
			error.put("error", "Invalid JSON");
			return badRequest(error);
		}
		String colType = null;
		if (json.has("resourceType"))
			colType = json.get("resourceType").asText();
		if ((colType == null)
				|| (WithResourceType.valueOf(colType) == null))
			colType = WithResourceType.SimpleCollection.toString();
		try {
			ObjectId creatorDbId = effectiveUserDbId();
			if (creatorDbId == null) {
				error.put("error", "No rights for WITH resource creation");
				return forbidden(error);
			}
			Class<?> clazz = Class.forName("model.resources.collection."
					+ colType);
			CollectionObject collection = (CollectionObject) Json.fromJson(json,
					clazz);
			boolean success = internalAddCollection(collection, WithResourceType.valueOf(colType),
					creatorDbId, error);
			if (!success)
				return badRequest(error);
			return ok(Json.toJson(collectionWithMyAccessData(
					collection, effectiveUserIds(), "BASIC", Option.Some("DEFAULT"))));
		} catch (Exception e) {
			error.put("error", e.getMessage());
			return internalServerError(error);
		}
	}

	private static boolean isCollectionCreated(ObjectId creatorDbId, String name) {
		return DB.getCollectionObjectDAO().existsForOwnerAndLabel(creatorDbId,
				null, Arrays.asList(name));
	}

	private static boolean internalAddCollection(CollectionObject collection,
			WithResourceType colType, ObjectId creatorDbId, ObjectNode error) {
		if (collection.getDescriptiveData().getLabel() == null || collection.getDescriptiveData().getLabel().isEmpty()) {
			error.put("error", "Missing collection title");
			return false;
		}
		if (DB.getCollectionObjectDAO().existsForOwnerAndLabel(
				creatorDbId,
				null,
				collection.getDescriptiveData().getLabel()
						.get(Language.DEFAULT))) {
			error.put("error", "Not unique collection title");
			return false;
		}
		Set<ConstraintViolation<CollectionObject>> violations = Validation
				.getValidator().validate(collection);
		if (!violations.isEmpty()) {
			ArrayNode properties = Json.newObject().arrayNode();
			for (ConstraintViolation<CollectionObject> cv : violations) {
				properties.add(Json.parse("{\"" + cv.getPropertyPath()
						+ "\":\"" + cv.getMessage() + "\"}"));
			}
			error.put("error", properties);
			return false;
		}
		// Fill with all the administrative metadata
		collection.setResourceType(colType);
		collection.getAdministrative().setWithCreator(creatorDbId);
		collection.getAdministrative().setCreated(new Date());
		collection.getAdministrative().setLastModified(new Date());
		if (collection.getAdministrative() instanceof CollectionAdmin) {
			((CollectionAdmin) collection.getAdministrative()).setEntryCount(0);
		}
		DB.getCollectionObjectDAO().makePermanent(collection);
		DB.getCollectionObjectDAO().updateWithURI(collection.getDbId(),
				"/collection/" + collection.getDbId());
		return true;
	}

	/**
	 * Retrieve a resource metadata. If the format is defined the specific
	 * serialization of the object is returned
	 *
	 * @param id
	 *            the resource id
	 * @return the resource metadata
	 */
	public static Result getCollectionObject(String id, String profile, Option<String> locale) {
		ObjectNode result = Json.newObject();
		try {
			ObjectId collectionDbId = new ObjectId(id);
			Result response = errorIfNoAccessToCollection(Action.READ,
					collectionDbId);
			if (!response.toString().equals(ok().toString()))
				return response;
			else {
				CollectionObject collection = DB.getCollectionObjectDAO().get(
						new ObjectId(id));
				CollectionObject profiledCollection = collection.getCollectionProfile(profile);
				filterResourceByLocale(locale, profiledCollection);
				return ok(Json.toJson(profiledCollection));
			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	/**
	 * Deletes all resource metadata
	 *
	 * @param id
	 *            the resource id
	 * @return success message
	 */
	// TODO: cascaded delete (if needed)
	public static Result deleteCollectionObject(String id) {
		ObjectNode result = Json.newObject();
		Locks locks = null;
		try {
			locks = Locks.create().write("Collection #" + id).acquire();
			ObjectId collectionDbId = new ObjectId(id);
			Result response = errorIfNoAccessToCollection(Action.DELETE,
					collectionDbId);
			if (!response.toString().equals(ok().toString()))
				return response;
			else {
				CollectionObject collection = DB.getCollectionObjectDAO().get(
						collectionDbId);
				// TODO: have to test that this works
				DB.getRecordResourceDAO().removeAllRecordsFromCollection(
						collectionDbId);
				DB.getCollectionObjectDAO().makeTransient(collection);

				result.put("message", "Resource was deleted successfully");
				return ok(result);
			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		} finally {
			if (locks != null)
				locks.release();
		}
	}

	/**
	 * Edits the WITH resource according to the JSON body. For every field
	 * mentioned in the JSON body it either edits the existing one or it adds it
	 * (in case it doesn't exist)
	 *
	 * @param id
	 * @return the edited resource
	 */
	public static Result editCollectionObject(String id) {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		ObjectId collectionDbId = new ObjectId(id);
		try {
			Result response = errorIfNoAccessToCollection(Action.EDIT,
					collectionDbId);
			if (!response.toString().equals(ok().toString()))
				return response;
			if (json == null) {
				result.put("error", "Invalid JSON");
				return badRequest(result);
			}
			String colType = null;
			if (json.has("resourceType"))
				colType = json.get("resourceType").asText();
			if ((colType == null)
					|| (WithResourceType.valueOf(colType) == null))
				colType = WithResourceType.SimpleCollection.toString();
			Class<?> clazz = Class.forName("model.resources.collection."
						+ colType);
			CollectionObject collectionChanges = (CollectionObject) Json.fromJson(json,
					clazz);
			if ((collectionChanges.getDescriptiveData().getLabel() != null)
					&& DB.getCollectionObjectDAO().existsOtherForOwnerAndLabel(
							effectiveUserDbId(),
							null,
							collectionChanges.getDescriptiveData().getLabel()
									.get(Language.DEFAULT), collectionDbId)) {
				ObjectNode error = Json.newObject();
				error.put("error", "Not unique collection title");
				return badRequest(error);
			}
			DB.getCollectionObjectDAO().editCollection(collectionDbId, json);
			return ok(Json.toJson(DB.getCollectionObjectDAO().get(
					collectionDbId)));
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	public static Result countMyAndShared() {
		ObjectNode result = Json.newObject().objectNode();
		List<String> effectiveUserIds = 
				effectiveUserIds();
		if (effectiveUserIds.isEmpty()) {
			return badRequest("You should be signed in as a user.");
		} else {
			result = DB.getCollectionObjectDAO().countMyAndSharedCollections(
					toObjectIds(effectiveUserIds));
			return ok(result);
		}
	}
	

	public static Result list(Option<MyPlayList> directlyAccessedByUserOrGroup,
			Option<String> creator, Option<Boolean> isExhibition,
			 Boolean collectionHits, int offset, int count, String profile, Option<String> locale) {
		if (WithController.isSuperUser()) {
			return list(Option.None(), Option.None(),Option.None(), Option.Some(true), isExhibition, collectionHits, offset, count, profile, locale);
		}
		return list(directlyAccessedByUserOrGroup, Option.<MyPlayList>None(), creator,
				Option.Some(false), isExhibition, collectionHits, offset, count, profile, locale);
	}
	
	public static Result listPublic(Option<MyPlayList> directlyAccessedByUserOrGroup,
			Option<String> creator, Option<Boolean> isExhibition,
			 Boolean collectionHits, int offset, int count, String profile, Option<String> locale) {
		return list(directlyAccessedByUserOrGroup, Option.<MyPlayList>None(), creator, Option.Some(true),
				isExhibition, collectionHits, offset, count, profile, locale);
	}
	
	public static Result list(Option<MyPlayList> directlyAccessedByUserOrGroup,
			Option<MyPlayList> recursivelyAccessedByUserOrGroup,
			Option<String> creator, Option<Boolean> isPublic,
			Option<Boolean> isExhibition, Boolean collectionHits, int offset,
			int count, String profile, Option<String> locale) {
		ObjectNode result = Json.newObject().objectNode();
		ArrayNode collArray = Json.newObject().arrayNode();
		List<CollectionObject> userCollections;
		List<String> effectiveUserIds = effectiveUserIds();
		List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup = accessibleByUserOrGroup(
				directlyAccessedByUserOrGroup, recursivelyAccessedByUserOrGroup);
		Boolean isExhibitionBoolean = isExhibition.isDefined() ? isExhibition
				.get() : null;
		ObjectId creatorId = null;
		if (creator.isDefined() && creator.get().equals("undefined")) {
			result.put("collectionsOrExhibitions", Json.newObject().arrayNode());
			return ok(result);
		} else if (creator.isDefined() && !creator.get().equals("undefined")) {
			User creatorUser = DB.getUserDAO().getByUsername(creator.get());
			if (creatorUser != null)
				creatorId = creatorUser.getDbId();
			else 
				return badRequest("User with username " + creator.get() + " does not exist.");
		}
		if (effectiveUserIds.isEmpty()
				|| (isPublic.isDefined() && isPublic.get() == true)) {
			// if not logged or ask for public collections, return all public
			// collections
			Tuple<List<CollectionObject>, Tuple<Integer, Integer>> info = DB
					.getCollectionObjectDAO().getPublicAndByAcl(
							accessedByUserOrGroup, creatorId,
							isExhibitionBoolean, collectionHits, offset, count);
			userCollections = info.x;
			if (info.y != null) {
				result.put("totalCollections", info.y.x);
				result.put("totalExhibitions", info.y.y);
			}
			for (CollectionObject collection : userCollections) {
				ObjectNode c;
				if (WithController.isSuperUser()) {
					c = collectionWithMyAccessData(collection, effectiveUserIds, profile, locale);
				}
				else {
					CollectionObject profiledCollection = collection.getCollectionProfile(profile);
					filterResourceByLocale(locale, profiledCollection);
					c = (ObjectNode) Json.toJson(profiledCollection);
				}
				c.remove("collectedResources");
				if (effectiveUserIds.isEmpty())
					c.put("access", Access.READ.toString());
				collArray.add(c);
			}
			result.put("collectionsOrExhibitions", collArray);
			return ok(result);
		} else { // logged in, check if super user, if not, restrict query to
					// accessible by effectiveUserIds
			Tuple<List<CollectionObject>, Tuple<Integer, Integer>> info;
			if (!isSuperUser())
				info = DB.getCollectionObjectDAO().getByLoggedInUsersAndAcl(
						toObjectIds(effectiveUserIds),
						accessedByUserOrGroup, creatorId, isExhibitionBoolean,
						collectionHits, offset, count);
			else
				info = DB.getCollectionObjectDAO().getByAcl(
						accessedByUserOrGroup, creatorId, isExhibitionBoolean,
						collectionHits, offset, count);
			if (info.y != null) {
				result.put("totalCollections", info.y.x);
				result.put("totalExhibitions", info.y.y);
			}
			List<ObjectNode> collections = collectionsWithMyAccessData(info.x,
					effectiveUserIds, profile, locale);
			for (ObjectNode c : collections) {
				c.remove("collectedResources");
				collArray.add(c);
			}
			result.put("collectionsOrExhibitions", collArray);
			return ok(result);
		}
	}
	
	public static Result listShared(Boolean direct, Option<MyPlayList> directlyAccessedByUserOrGroup,
			Option<Boolean> isExhibition, Boolean collectionHits, int offset, int count, 
			String profile, Option<String> locale) {
		return listShared(direct, directlyAccessedByUserOrGroup, Option.<MyPlayList>None(),
				isExhibition, collectionHits, offset, count, profile, locale);
	}

	public static Result listShared(Boolean direct,
			Option<MyPlayList> directlyAccessedByUserOrGroup,
			Option<MyPlayList> recursivelyAccessedByUserOrGroup,
			Option<Boolean> isExhibition, boolean collectionHits, int offset,
			int count, String profile, Option<String> locale) {
		ObjectNode result = Json.newObject().objectNode();
		ArrayNode collArray = Json.newObject().arrayNode();
		List<String> effectiveUserIds = effectiveUserIds();
		Boolean isExhibitionBoolean = isExhibition.isDefined() ? isExhibition
				.get() : null;
		if (effectiveUserIds.isEmpty()) {
			return forbidden(Json
					.parse("\"error\", \"Must specify user for the collection\""));
		} else {
			ObjectId userId = new ObjectId(effectiveUserIds.get(0));
			List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup = new ArrayList<List<Tuple<ObjectId, Access>>>();
			accessedByUserOrGroup = accessibleByUserOrGroup(
					directlyAccessedByUserOrGroup,
					recursivelyAccessedByUserOrGroup);
			List<Tuple<ObjectId, Access>> accessedByLoggedInUser = new ArrayList<Tuple<ObjectId, Access>>();
			if (direct) {
				accessedByLoggedInUser.add(new Tuple<ObjectId, Access>(userId,
						Access.READ));
				accessedByUserOrGroup.add(accessedByLoggedInUser);
			} else {// indirectly: include collections for which user has access
					// via userGoup sharing
				for (String effectiveId : effectiveUserIds) {
					accessedByLoggedInUser.add(new Tuple<ObjectId, Access>(
							new ObjectId(effectiveId), Access.READ));
				}
				accessedByUserOrGroup.add(accessedByLoggedInUser);
			}
			Tuple<List<CollectionObject>, Tuple<Integer, Integer>> info = DB
					.getCollectionObjectDAO().getSharedAndByAcl(
							accessedByUserOrGroup, userId, isExhibitionBoolean,
							collectionHits, offset, count);
			if (info.y != null) {
				result.put("totalCollections", info.y.x);
				result.put("totalExhibitions", info.y.y);
			}

			List<ObjectNode> collections = collectionsWithMyAccessData(info.x,
					effectiveUserIds, profile, locale);
			for (ObjectNode c : collections) {
				c.remove("collectedResources");
				collArray.add(c);
			}
			result.put("collectionsOrExhibitions", collArray);
			return ok(result);
		}
	}

	// input parameter lists' (directlyAccessedByUserOrGroup etc) intended
	// meaning is AND of its entries
	// returned list of lists accessedByUserOrGroup represents AND of OR entries
	// i.e. each entry in directlyAccessedByUserName for example has to be
	// included in a separate list!
	private static List<List<Tuple<ObjectId, Access>>> accessibleByUserOrGroup(
			Option<MyPlayList> directlyAccessedByUserOrGroup,
			Option<MyPlayList> recursivelyAccessedByUserOrGroup) {
		List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup = new ArrayList<List<Tuple<ObjectId, Access>>>();
		if (directlyAccessedByUserOrGroup.isDefined()) {
			MyPlayList directlyUserNameList = directlyAccessedByUserOrGroup
					.get();
			for (StringTuple userAccess : directlyUserNameList.list) {
				List<Tuple<ObjectId, Access>> directlyAccessedByUser = new ArrayList<Tuple<ObjectId, Access>>();
				UserOrGroup userOrGroup = getUserOrGroup(userAccess.x);
				if (userOrGroup != null) {
					directlyAccessedByUser.add(new Tuple<ObjectId, Access>(
							userOrGroup.getDbId(), Access.valueOf(userAccess.y
									.toUpperCase())));
					accessedByUserOrGroup.add(directlyAccessedByUser);
				}
			}
		}
		// TODO: add support for userGroups in recursive case!!!!!
		if (recursivelyAccessedByUserOrGroup.isDefined()) {
			MyPlayList recursivelyUserNameList = recursivelyAccessedByUserOrGroup
					.get();
			for (StringTuple userAccess : recursivelyUserNameList.list) {
				List<Tuple<ObjectId, Access>> recursivelyAccessedByUser = new ArrayList<Tuple<ObjectId, Access>>();
				User user = DB.getUserDAO().getByUsername(userAccess.x);
				ObjectId userId = user.getDbId();
				Access access = Access.valueOf(userAccess.y.toUpperCase());
				recursivelyAccessedByUser.add(new Tuple<ObjectId, Access>(
						userId, access));
				Set<ObjectId> groupIds = user.getUserGroupsIds();
				for (ObjectId groupId : groupIds) {
					recursivelyAccessedByUser.add(new Tuple<ObjectId, Access>(
							groupId, access));
				}
				accessedByUserOrGroup.add(recursivelyAccessedByUser);
			}
		}
		return accessedByUserOrGroup;
	}

	private static UserOrGroup getUserOrGroup(String username) {
		User user = DB.getUserDAO().getByUsername(username);
		UserOrGroup userOrGroup = null;
		if (user != null) {
			userOrGroup = user;
		} else {
			UserGroup userGroup = DB.getUserGroupDAO().getByName(username);
			if (userGroup != null) {
				userOrGroup = userGroup;
			}
		}
		return userOrGroup;
	}

	private static List<ObjectNode> collectionsWithMyAccessData(
			List<CollectionObject> userCollections,
			List<String> effectiveUserIds, String profile, Option<String> locale) {
		List<ObjectNode> collections = new ArrayList<ObjectNode>(
				userCollections.size());
		for (CollectionObject collection : userCollections) {			
			collections.add(collectionWithMyAccessData(collection,
					effectiveUserIds, profile, locale));			
		}
		return collections;
	}

	private static ObjectNode collectionWithMyAccessData(
			CollectionObject userCollection, List<String> effectiveUserIds, 
			String profile, Option<String> locale) {
		CollectionObject profiledCollection = userCollection.getCollectionProfile(profile);
		filterResourceByLocale(locale, profiledCollection);
		ObjectNode c = (ObjectNode) Json.toJson(profiledCollection);
		Access maxAccess = getMaxAccess(profiledCollection
				.getAdministrative().getAccess(), effectiveUserIds);
		if (maxAccess.equals(Access.NONE))
			maxAccess = Access.READ;
		c.put("myAccess", maxAccess.toString());
		return c;
	}

	public static void addCollectionToList(int index,
			List<CollectionObject> collectionsOrExhibitions,
			List<ObjectId> colls, List<String> effectiveUserIds) {
		if (index < colls.size()) {
			ObjectId id = colls.get(index);
			CollectionObject c = DB.getCollectionObjectDAO().getById(id);
			if (effectiveUserIds.isEmpty()) {
				if (c.getAdministrative().getAccess().getIsPublic())
					collectionsOrExhibitions.add(c);
			} else {
				Access maxAccess = getMaxAccess(c
						.getAdministrative().getAccess(), effectiveUserIds);
				if (!maxAccess.equals(Access.NONE))
					collectionsOrExhibitions.add(c);
			}
		}
	}

	// If isExhibition is undefined, returns (max) countPerType collections and
	// countPerType exhibitions, i.e. (max) 2*countPerType
	// collectionsOrExhibitions
	public static Result getFeatured(String userOrGroupName,
			Option<Boolean> isExhibition, int offset, int countPerType, 
			String profile, Option<String> locale) {
		Page page = null;
		UserGroup userGroup = DB.getUserGroupDAO().getByName(userOrGroupName);
		if (userGroup != null) {
			if (userGroup instanceof Organization)
				page = ((Organization) userGroup).getPage();
			else if (userGroup instanceof Project)
				page = ((Project) userGroup).getPage();
		} else {
			User user = DB.getUserDAO().getByUsername(userOrGroupName);
			if (user != null) {
				page = user.getPage();
			}
		}
		if (page != null) {
			List<String> effectiveUserIds = effectiveUserIds();
			ObjectNode result = Json.newObject().objectNode();
			int start = offset * countPerType;
			int collectionsSize = page.getFeaturedCollections().size();
			int exhibitionsSize = page.getFeaturedExhibitions().size();
			List<CollectionObject> collectionsOrExhibitions = new ArrayList<CollectionObject>();
			if (!isExhibition.isDefined()) {
				for (int i = start; (i < (start + countPerType))
						&& (i < collectionsSize); i++) {
					addCollectionToList(i, collectionsOrExhibitions,
							page.getFeaturedCollections(), effectiveUserIds);
					addCollectionToList(i, collectionsOrExhibitions,
							page.getFeaturedExhibitions(), effectiveUserIds);
				}
			} else {
				if (!isExhibition.get()) {
					for (int i = start; (i < (start + countPerType))
							&& (i < collectionsSize); i++)
						addCollectionToList(i, collectionsOrExhibitions,
								page.getFeaturedCollections(), effectiveUserIds);
				} else {
					for (int i = start; (i < (start + countPerType))
							&& (i < exhibitionsSize); i++)
						addCollectionToList(i, collectionsOrExhibitions,
								page.getFeaturedExhibitions(), effectiveUserIds);
				}
			}
			ArrayNode collArray = Json.newObject().arrayNode();
			List<ObjectNode> collections = collectionsWithMyAccessData(
					collectionsOrExhibitions, effectiveUserIds, profile, locale);
			for (ObjectNode c : collections)
				collArray.add(c);
			result.put("totalCollections", collectionsSize);
			result.put("totalExhibitions", exhibitionsSize);
			result.put("collectionsOrExhibitions", collArray);
			// TODO: put collection and exhibition hits in response
			return ok(result);
		} else
			return badRequest("User or group with name " + userOrGroupName
					+ " does not exist or has no specified page.");

	}

	/**
	 * @return
	 */
	public static Result getFavoriteCollection(String profile, Option<String> locale) {
		ObjectId userId = new ObjectId(loggedInUser());
		if (userId == null) {
			return forbidden();
		}
		CollectionObject favorite;
		ObjectId favoritesId;
		if ((favorite = DB.getCollectionObjectDAO().getByOwnerAndLabel(userId,
				null, "_favorites")) == null) {
			favoritesId = createFavorites(userId);
		} else {
			favoritesId = favorite.getDbId();
		}
		return getCollectionObject(favoritesId.toString(), profile, locale);

	}

	public static ObjectId createFavorites(ObjectId userId) {
		CollectionObject fav = new SimpleCollection();
		fav.getAdministrative().setCreated(new Date());
		fav.getAdministrative().setWithCreator(userId);
		fav.getDescriptiveData().setLabel(
				new MultiLiteral(Language.DEFAULT, "_favorites"));
		DB.getCollectionObjectDAO().makePermanent(fav);
		return fav.getDbId();
	}

	/**
	 * List all Records from a Collection using a start item and a page size
	 */
	public static Result listRecordResources(String collectionId,
			String contentFormat, int start, int count, String profile, Option<String> locale) {
		ObjectNode result = Json.newObject();
		ObjectId colId = new ObjectId(collectionId);
		Locks locks = null;
		try {
			locks = Locks.create().read("Collection #" + collectionId)
					.acquire();
			Result response = errorIfNoAccessToCollection(Action.READ, colId);
			if (!response.toString().equals(ok().toString()))
				return response;
			else {
				List<RecordResource> records = DB.getRecordResourceDAO()
						.getByCollectionBetweenPositions(colId, start,
								start + count);
				if (records == null) {
					result.put("message",
							"Cannot retrieve records from database!");
					return internalServerError(result);
				}
				ArrayNode recordsList = Json.newObject().arrayNode();
				for (RecordResource r : records) {
					// filter out records to which the user has no read access
					response = errorIfNoAccessToRecord(Action.READ, r.getDbId());
					if (!response.toString().equals(ok().toString())) {
						continue;
					}
					if (contentFormat.equals("contentOnly")) {
						if (r.getContent() != null) {
							recordsList.add(Json.toJson(r.getContent()));
						}
						continue;
					}
					if (contentFormat.equals("noContent")
							&& r.getContent() != null) {
						r.getContent().clear();
						RecordResource profiledRecord = r.getRecordProfile(profile);
						filterResourceByLocale(locale, profiledRecord);
						recordsList.add(Json.toJson(profiledRecord));
						fillContextData(
								DB.getCollectionObjectDAO()
										.getById(
												colId,
												Arrays.asList("collectedResources"))
										.getCollectedResources(), recordsList);
						continue;
					}
					if (r.getContent() != null
							&& r.getContent().containsKey(contentFormat)) {
						HashMap<String, String> newContent = new HashMap<String, String>(
								1);
						newContent.put(contentFormat, (String) r.getContent()
								.get(contentFormat));
						recordsList.add(Json.toJson(newContent));
						continue;
					}
					RecordResource profiledRecord = r.getRecordProfile(profile);
					filterResourceByLocale(locale, profiledRecord);
					recordsList.add(Json.toJson(profiledRecord));
				}
				result.put(
						"entryCount",
						((CollectionAdmin) DB.getCollectionObjectDAO()
								.getById(
										colId,
										new ArrayList<String>(
												Arrays.asList("administrative.entryCount")))
								.getAdministrative()).getEntryCount());
				result.put("records", recordsList);
				return ok(result);
			}
		} catch (Exception e1) {
			result.put("error", e1.getMessage());
			return internalServerError(result);
		} finally {
			if (locks != null)
				locks.release();
		}
	}

	private static void fillContextData(
			List<ContextData<ContextDataBody>> contextData,
			ArrayNode recordsList) {
		for (JsonNode record : recordsList) {
			for (ContextData<ContextDataBody> data : contextData) {
				if (data.getTarget().getRecordId().toString()
						.equals(record.get("dbId").asText())) {
					((ObjectNode) record).put("contextData", Json.toJson(data));
					contextData.remove(data);
					break;
				}
			}
		}

	}

	public static Result listUsersWithRights(String collectionId) {
		ArrayNode result = Json.newObject().arrayNode();
		List<String> retrievedFields = new ArrayList<String>(
				Arrays.asList("administrative.access"));
		CollectionObject collection = DB.getCollectionObjectDAO().getById(
				new ObjectId(collectionId), retrievedFields);
		WithAccess access = collection.getAdministrative().getAccess();
		for (AccessEntry ae : access.getAcl()) {
			if (ae.getLevel().ordinal() > 0) {
				ObjectId userId = ae.getUser();
				User user = DB.getUserDAO().getById(userId, null);
				Access accessRights = ae.getLevel();
				if (user != null) {
					result.add(userOrGroupJson(user, accessRights));
				} else {
					UserGroup usergroup = DB.getUserGroupDAO().get(userId);
					if (usergroup != null)
						result.add(userOrGroupJson(usergroup, accessRights));
					else
						return internalServerError("User with id " + userId
								+ " cannot be retrieved from db");
				}
			}
		}
		return ok(result);
	}

	private static ObjectNode userOrGroupJson(UserOrGroup user,
			Access accessRights) {
		ObjectNode userJSON = Json.newObject();
		userJSON.put("userId", user.getDbId().toString());
		userJSON.put("username", user.getUsername());
		if (user instanceof User) {
			userJSON.put("category", "user");
			userJSON.put("firstName", ((User) user).getFirstName());
			userJSON.put("lastName", ((User) user).getLastName());
		} else
			userJSON.put("category", "group");
		String image = UserAndGroupManager.getImageBase64(user);
		userJSON.put("accessRights", accessRights.toString());
		if (image != null) {
			userJSON.put("image", image);
		}
		return userJSON;
	}

	private static Integer doTheImport(ObjectNode resultInfo,
			final CommonQuery q, final String cid, EuropeanaSpaceSource src,
			int total, int firstPageCount1) {
		SourceResponse result;
		int page = 1;
		int pageSize = 20;
		int itemsCount = firstPageCount1;
		while (itemsCount < total) {
			page++;
			q.page = page + "";
			result = src.getResults(q);
			for (WithResource<?, ?> item : result.items.getCulturalCHO()) {
				WithResourceController.internalAddRecordToCollection(cid,
						(RecordResource) item, F.Option.None(), resultInfo,
						true);
				itemsCount++;
			}
		}
		return 0;
	}
}
