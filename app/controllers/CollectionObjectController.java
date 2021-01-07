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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.validation.ConstraintViolation;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.mongodb.morphia.geo.Point;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import actors.annotation.AnnotationControlActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import annotators.AnnotatorConfig;
import controllers.parameterTypes.MyPlayList;
import controllers.parameterTypes.StringTuple;
import db.DB;
import elastic.ElasticCoordinator;
import elastic.ElasticSearcher.SearchOptions;
import model.annotations.Annotation;
import model.annotations.Annotation.AnnotationAdmin;
import model.annotations.Annotation.AnnotationScore;
import model.annotations.ContextData;
import model.annotations.ContextData.ContextDataBody;
import model.annotations.bodies.AnnotationBodyGeoTagging;
import model.annotations.bodies.AnnotationBodyTagging;
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
import model.resources.WithResourceType;
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
import play.libs.Akka;
import play.libs.F;
import play.libs.F.Function0;
import play.libs.F.Option;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import search.Query;
import search.Response;
import search.Response.SingleResponse;
import search.Sources;
import sources.EuropeanaCollectionSpaceSource;
import sources.EuropeanaSpaceSource;
import sources.OWLExporter.CulturalItemOWLExporter;
import sources.core.CommonQuery;
import sources.core.JsonContextRecordFormatReader;
import sources.core.ParallelAPICall;
import sources.core.ParallelAPICall.Priority;
import sources.core.ResourcesListImporter;
import sources.core.SourceResponse;
import sources.core.Utils;
import sources.formatreaders.DanceExhibitionReader;
import sources.formatreaders.ExhibitionReader;
import sources.formatreaders.MuseumofModernArtRecordFormatter;
import sources.formatreaders.OmekaExhibitionReader;
import sources.utils.JsonContextRecord;
import utils.Deserializer.PointDeserializer;
import utils.ListUtils;
import utils.Locks;
import utils.MetricsUtils;
import utils.Serializer.PointSerializer;
import utils.Tuple;
import vocabularies.Vocabulary;

@SuppressWarnings("rawtypes")
public class CollectionObjectController extends WithResourceController {

	public static final ALogger log = Logger.of(CollectionObjectController.class);

	public  CompletionStage<Result> importSearch() {
		JsonNode json = request().body().asJson();
		if (json == null) {
			return CompletableFuture.completedFuture((Result) badRequest("Expecting Json query"));
		} else {
			// Parse the query.
			try {
				ObjectNode resultInfo = Json.newObject();
				ObjectId creatorDbId = new ObjectId(loggedInUser());
				final CommonQuery q = Utils.parseJson(json.get("query"));
				final String cname = json.get("collectionName").toString();
				final int limit = (json.has("limit")) ? json.get("limit").asInt() : -1;
				CollectionObject ccid = null;
				if (!isCollectionCreated(creatorDbId, cname)) {
					CollectionObject collection = new SimpleCollection();
					collection.getDescriptiveData().setLabel(new MultiLiteral(cname).fillDEF());
					boolean success = internalAddCollection(collection, WithResourceType.SimpleCollection, creatorDbId,
							resultInfo);
					if (!success)
						return CompletableFuture.completedFuture((Result) badRequest("Expecting Json query"));
					ccid = collection;
				} else {
					List<CollectionObject> col = DB.getCollectionObjectDAO().getByLabel(Language.DEFAULT, cname);
					ccid = col.get(0);
				}

				EuropeanaSpaceSource src = new EuropeanaSpaceSource();
				src.setUsingCursor(true);

				return internalImport(src, ccid, q, limit, resultInfo, true, false);

			} catch (Exception e) {
				log.error("", e);
				return CompletableFuture.completedFuture((Result) badRequest(e.getMessage()));
			}
		}
	}

	public Result importOmeka(String colid) {
		OmekaExhibitionReader r = new OmekaExhibitionReader();
		ObjectId creatorDbId = new ObjectId(loggedInUser());
		r.importOmeka(creatorDbId, colid);
		return ok("Collections from Omeka imported");
	}

	public Result importExhibition(String source) {
		Object res = importExhibitionInternal(source);
		return ok(Json.toJson(res));
	}

	public Object importExhibitionInternal(String source) {
		ObjectId creatorDbId = new ObjectId(loggedInUser());
		JsonNode json = request().body().asJson();
		Object res;
		ExhibitionReader r = null;
		switch (source.toLowerCase()) {
		case "omeka":
			r = new OmekaExhibitionReader();
		case "dance":
			r = new DanceExhibitionReader();
			break;
		default:
			break;
		}
		res = r.importExhibitionObjectFrom(new JsonContextRecord(json), creatorDbId);
		return res;
	}

	public Result importIDs(String cname, String source, String ids) {
		System.out.println("----------------check collection");
		ObjectNode resultInfo = Json.newObject();
		System.out.println("----------------check collection");
		CollectionObject ccid = getOrCreateCollection(cname, resultInfo);
		if (ccid == null)
			return internalServerError(resultInfo);
		System.out.println("----------------collection ok");
		for (String oid : ids.split("[,\\s]+")) {
			CulturalObject record = new CulturalObject();
			CulturalObjectData descriptiveData = new CulturalObjectData();
			descriptiveData.setLabel(new MultiLiteral(oid).fillDEF());
			record.setDescriptiveData(descriptiveData);
			record.addToProvenance(new ProvenanceInfo(source, null, oid));
			internalAddRecordToCollection(ccid.getDbId().toString(), record, Optional.empty(), resultInfo);
		}
		return ok(resultInfo);
	}

	private static CollectionObject getOrCreateCollection(String collectionName, ObjectNode resultInfo) {
		ObjectId creatorDbId = new ObjectId(loggedInUser());
		CollectionObject ccid = null;
		if (!isCollectionCreated(creatorDbId, collectionName)) {
			CollectionObject collection = new SimpleCollection();
			collection.getDescriptiveData().setLabel(new MultiLiteral(collectionName).fillDEF());
			boolean success = internalAddCollection(collection, WithResourceType.SimpleCollection, creatorDbId,
					resultInfo);
			if (!success) {
				resultInfo.put("error", "Failed creating the collection");
				return null;
			}
			ccid = collection;
		} else {
			List<CollectionObject> col = DB.getCollectionObjectDAO().getByLabel(Language.DEFAULT, collectionName);
			ccid = col.get(0);
		}
		return ccid;
	}

	/**
	 * creates a new collection corresponding to a collection in Europeana and
	 * collects all its items.
	 *
	 * @param id
	 * @return
	 */
	public CompletionStage<Result> createAndFillEuropeanaCollection(String id, int limit) {
		CollectionObject collection = new SimpleCollection();
		collection.getDescriptiveData().setLabel(new MultiLiteral(id).fillDEF());
		ObjectNode resultInfo = Json.newObject();
		ObjectId creatorDbId = new ObjectId(loggedInUser());
		boolean success = internalAddCollection(collection, WithResourceType.SimpleCollection, creatorDbId, resultInfo);
		if (!success)
			return CompletableFuture.completedFuture((Result) badRequest(resultInfo));
		CommonQuery q = new CommonQuery();

		EuropeanaCollectionSpaceSource src = new EuropeanaCollectionSpaceSource(id);
		return internalImport(src, collection, q, limit, resultInfo, false, false);
	}

	private CompletionStage<Result> internalImport(EuropeanaSpaceSource src, CollectionObject collection, CommonQuery q,
			int limit, ObjectNode resultInfo, boolean dontDuplicate, boolean waitToFinish) {
		q.page = 1 + "";
		q.pageSize = "20";
		SourceResponse result = src.getResults(q);
		int total = result.totalCount;
		final int mylimit = (limit == -1) ? total : Math.min(limit, total);

		int firstPageCount1 = addResultToCollection(result, collection.getDbId().toString(), mylimit, resultInfo,
				dontDuplicate);

		Supplier<Result> supplier = () -> {
				SourceResponse innerResult;
				int page = 1;
				int itemsCount = firstPageCount1;
				while (itemsCount < mylimit) {
					page++;
					q.page = page + "";
					result = src.getResults(q);
					if (result.error == null) {
						int c = addResultToCollection(innerResult, collection.getDbId().toString(), mylimit - itemsCount,
								resultInfo, dontDuplicate);
						itemsCount = itemsCount + c;
					} else {
						break;
					}
				}
				return ok(Json.toJson(
						collectionWithMyAccessData(collection, effectiveUserIds(), "BASIC", Optional.of("DEFAULT"))));
		};
		CompletableFuture<Result> promiseOfInt = CompletableFuture.supplyAsync(supplier);
		// Promise<Result> promiseOfInt =
		// ParallelAPICall.createPromise(function0, Priority.MINE);
		if (resultInfo.has("error"))
			return CompletableFuture.completedFuture((Result) badRequest(resultInfo));
		if (waitToFinish)
			return promiseOfInt;
		else
			return CompletableFuture.completedFuture(ok(Json.toJson(
					collectionWithMyAccessData(collection, effectiveUserIds(), "BASIC", Optional.of("DEFAULT")))));
	}

	public Result uploadCollection() {
		ObjectNode resultInfo = Json.newObject();
		MultipartFormData body = request().body().asMultipartFormData();
		FilePart picture = body.getFile("items");
		String source = body.asFormUrlEncoded().get("source")[0];
		CollectionObject ccid = getOrCreateCollection(source, resultInfo);
		if (ccid == null)
			return internalServerError(resultInfo);
		if (picture != null) {
			File file = picture.getFile();
			// TODO pick the correct reader
			JsonContextRecordFormatReader itemReader = new MuseumofModernArtRecordFormatter();
			ResourcesListImporter rec = new ResourcesListImporter(itemReader);
			addResultToCollection(rec.process(file), ccid.getDbId().toString(), -1, resultInfo, true);
			if (resultInfo.has("error")) {
				return internalServerError(resultInfo);
			}
			return ok("File uploaded");
		} else {
			flash("error", "Missing file");
			return badRequest();
		}
	}

	private static int addResultToCollection(SourceResponse result, String collectionID, int limit,
			ObjectNode resultInfo, boolean dontRepeat) {
		Collection<WithResource<?, ?>> culturalCHO = result.items.getCulturalCHO();
		return addResultToCollection(culturalCHO, collectionID, limit, resultInfo, dontRepeat);
	}

	private static int addResultToCollection(Collection<WithResource<?, ?>> items, String collectionID, int limit,
			ObjectNode resultInfo, boolean dontRepeat) {
		log.debug("adding " + items.size() + " items to collection " + collectionID);
		int itemsCount = 0;
		for (Iterator<WithResource<?, ?>> iterator = items.iterator(); iterator.hasNext()
				&& ((limit < 0) || (itemsCount < limit));) {
			WithResource<?, ?> item = iterator.next();
			WithResourceController.internalAddRecordToCollection(collectionID, (RecordResource) item, Optional.empty(),
					resultInfo, dontRepeat);
			itemsCount++;
		}
		return itemsCount;
	}

	public Result sortCollectionObject(String collectionId) {
		ObjectNode result = Json.newObject();
		try {
			ObjectId collectionDbId = new ObjectId(collectionId);
			int entryCount = ((CollectionAdmin) DB.getCollectionObjectDAO()
					.getById(collectionDbId, Arrays.asList("administrative.entryCount")).getAdministrative())
							.getEntryCount();
			// Logger.info("Sorting collection "+collectionId);
			List<RecordResource> records = DB.getRecordResourceDAO().getByCollectionBetweenPositions(collectionDbId, 0,
					Math.min(entryCount, 1000));
			log.debug(ListUtils.transform(records, (x) -> x.getQualityMeasure()).toString());
			RecordResource<?>[] array = records.toArray(new RecordResource<?>[] {});
			Arrays.sort(array, Utils.compareQuality);
			for (int i = 0; i < array.length; i++) {
				DB.getCollectionObjectDAO().updateContextData(collectionDbId, new ContextData<>(array[i].getDbId()), i);
			}
			log.info("Items sorted based quality metric");

			records = DB.getRecordResourceDAO().getByCollectionBetweenPositions(collectionDbId, 0,
					Math.min(entryCount, 1000));
			log.debug(ListUtils.transform(records, (x) -> x.getQualityMeasure()).toString());

			return ok();
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	public static Result updateMediaQuality() {
		DB.getMediaObjectDAO().updateQualityForMediaObjects();
		return ok();
	}

	public static Result updateCollectionObject(String collectionId) {
		ObjectNode result = Json.newObject();
		try {
			ObjectId collectionDbId = new ObjectId(collectionId);
			int entryCount = ((CollectionAdmin) DB.getCollectionObjectDAO()
					.getById(collectionDbId, Arrays.asList("administrative.entryCount")).getAdministrative())
							.getEntryCount();
			Function<String, String> update = (String id) -> {
				int pos = 0;
				int pageSize = Math.min(entryCount, 100);
				while (pos < entryCount) {
					List<RecordResource> records = DB.getRecordResourceDAO()
							.getByCollectionBetweenPositions(collectionDbId, pos, pos + pageSize, "provenance");
					for (int i = 0; i < records.size(); i++) {
						RecordResource recordResource = records.get(i);
						@SuppressWarnings("unchecked")
						ProvenanceInfo provenance = (ProvenanceInfo) ListUtils.getLast(recordResource.getProvenance());
						WithResourceController.updateRecord(recordResource.getDbId(), provenance.getProvider(),
								provenance.getResourceId());
					}
					pos += pageSize;
				}
				return null;
			};
			ParallelAPICall.createPromise(update, collectionId, Priority.BACKEND);
			return ok();
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	public static Result exportCollectionObjectToOWL(String cname) {

		ObjectNode resultInfo = Json.newObject();
		// ObjectId creatorDbId = new ObjectId(loggedInUser());
		CollectionObject ccid = null;
		List<CollectionObject> col = DB.getCollectionObjectDAO().getByLabel(Language.DEFAULT, cname);
		if (!Utils.hasInfo(col)) {
			return badRequest(resultInfo);
		} else {
			ccid = col.get(0);
		}
		ObjectNode result = Json.newObject();
		CulturalItemOWLExporter exporter = new CulturalItemOWLExporter(ccid.getDbId().toString());
		try {
			ObjectId collectionDbId = ccid.getDbId();
			int entryCount = ((CollectionAdmin) DB.getCollectionObjectDAO()
					.getById(collectionDbId, Arrays.asList("administrative.entryCount")).getAdministrative())
							.getEntryCount();
			entryCount = Math.min(entryCount, 8000);
			int pageSize = Math.min(entryCount, 100);
			int pos = 0;
			while (pos < entryCount) {
				List<RecordResource> records = DB.getRecordResourceDAO().getByCollectionBetweenPositions(collectionDbId,
						pos, pos + pageSize);
				for (RecordResource recordResource : records) {
					if (recordResource instanceof CulturalObject) {
						CulturalObject new_record = (CulturalObject) recordResource;
						exporter.exportItem(new_record);
					}
				}
				pos += pageSize;
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
	 * @param collectionType the collection type that can take values of :
	 *                       {SimpleCollection, Exhibition }
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
		if ((colType == null) || (WithResourceType.valueOf(colType) == null))
			colType = WithResourceType.SimpleCollection.toString();
		try {
			ObjectId creatorDbId = effectiveUserDbId();
			if (creatorDbId == null) {
				error.put("error", "No rights for WITH resource creation");
				return forbidden(error);
			}
			Class<?> clazz = Class.forName("model.resources.collection." + colType);
			CollectionObject collection = (CollectionObject) Json.fromJson(json, clazz);
			boolean success = internalAddCollection(collection, WithResourceType.valueOf(colType), creatorDbId, error);
			if (!success)
				return badRequest(error);
			return ok(Json.toJson(
					collectionWithMyAccessData(collection, effectiveUserIds(), "BASIC", Option.Some("DEFAULT"))));
		} catch (Exception e) {
			error.put("error", e.getMessage());
			return internalServerError(error);
		}
	}

	private static boolean isCollectionCreated(ObjectId creatorDbId, String name) {
		return DB.getCollectionObjectDAO().existsForOwnerAndLabel(creatorDbId, null, Arrays.asList(name));
	}

	public static boolean internalAddCollection(CollectionObject collection, WithResourceType colType,
			ObjectId creatorDbId, ObjectNode error) {
		if ((collection.getDescriptiveData().getLabel() == null)
				|| collection.getDescriptiveData().getLabel().isEmpty()) {
			error.put("error", "Missing collection title");
			return false;
		}
		if (DB.getCollectionObjectDAO().existsForOwnerAndLabel(creatorDbId, null,
				collection.getDescriptiveData().getLabel().get(Language.DEFAULT))) {
			error.put("error", "Not unique collection title");
			return false;
		}
		Set<ConstraintViolation<CollectionObject>> violations = Validation.getValidator().validate(collection);
		if (!violations.isEmpty()) {
			ArrayNode properties = Json.newObject().arrayNode();
			for (ConstraintViolation<CollectionObject> cv : violations) {
				properties.add(Json.parse("{\"" + cv.getPropertyPath() + "\":\"" + cv.getMessage() + "\"}"));
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
		DB.getCollectionObjectDAO().updateWithURI(collection.getDbId(), "/collection/" + collection.getDbId());
		return true;
	}

	/**
	 * Retrieve a resource metadata. If the format is defined the specific
	 * serialization of the object is returned
	 *
	 * @param id the resource id
	 * @return the resource metadata
	 */
	public static Result getCollectionObject(String id, String profile, Option<String> locale) {
		ObjectNode result = Json.newObject();
		try {
			ObjectId collectionDbId = new ObjectId(id);
			Result response = errorIfNoAccessToCollection(Action.READ, collectionDbId);
			if (!response.toString().equals(ok().toString()))
				return response;
			else {
				CollectionObject collection = DB.getCollectionObjectDAO().getByIdAndExclude(new ObjectId(id),
						new ArrayList<String>() {
							{
								add("collectedResources");
							}
						});
				CollectionObject profiledCollection = collection.getCollectionProfile(profile);
				filterResourceByLocale(locale, profiledCollection);
				return ok(Json.toJson(profiledCollection));
			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	public static Result getMultipleCollectionObjects(List<String> id, String profile, Option<String> locale) {
		ArrayNode result = Json.newObject().arrayNode();
		for (String singleId : id) {
			try {
				ObjectId collectionDbId = new ObjectId(singleId);
				Result response = errorIfNoAccessToCollection(Action.READ, collectionDbId);

				if (response.toString().equals(ok().toString())) {
					CollectionObject collection = DB.getCollectionObjectDAO().getByIdAndExclude(new ObjectId(singleId),
							new ArrayList<String>() {
								{
									add("collectedResources");
								}
							});
					CollectionObject profiledCollection = collection.getCollectionProfile(profile);
					filterResourceByLocale(locale, profiledCollection);
					result.add(Json.toJson(profiledCollection));
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return ok(result);
	}

	/**
	 * Deletes all resource metadata
	 *
	 * @param id the resource id
	 * @return success message
	 */
	// TODO: cascaded delete (if needed)

	public static Result deleteCollectionObject(String id) {
		ObjectNode result = Json.newObject();
		Locks locks = null;
		try {
			locks = Locks.create().write("Collection #" + id).acquire();
			ObjectId collectionDbId = new ObjectId(id);
			Result response = errorIfNoAccessToCollection(Action.DELETE, collectionDbId);
			if (!response.toString().equals(ok().toString()))
				return response;
			else {
				CollectionObject collection = DB.getCollectionObjectDAO().get(collectionDbId);
				// TODO: have to test that this works
				DB.getRecordResourceDAO().removeAllRecordsFromCollection(collectionDbId);
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
	 * Edits the WITH resource according to the JSON body. For every field mentioned
	 * in the JSON body it either edits the existing one or it adds it (in case it
	 * doesn't exist)
	 *
	 * @param id
	 * @return the edited resource
	 */
	public static Result editCollectionObject(String id) {
		JsonNode json = request().body().asJson();
		ObjectNode result = Json.newObject();
		ObjectId collectionDbId = new ObjectId(id);
		try {
			Result response = errorIfNoAccessToCollection(Action.EDIT, collectionDbId);
			if (!response.toString().equals(ok().toString()))
				return response;
			if (json == null) {
				result.put("error", "Invalid JSON");
				return badRequest(result);
			}
			String colType = null;
			if (json.has("resourceType"))
				colType = json.get("resourceType").asText();
			if ((colType == null) || (WithResourceType.valueOf(colType) == null))
				colType = WithResourceType.SimpleCollection.toString();
			Class<?> clazz = Class.forName("model.resources.collection." + colType);
			CollectionObject collectionChanges = (CollectionObject) Json.fromJson(json, clazz);
			if ((collectionChanges.getDescriptiveData().getLabel() != null)
					&& DB.getCollectionObjectDAO().existsOtherForOwnerAndLabel(effectiveUserDbId(), null,
							collectionChanges.getDescriptiveData().getLabel().get(Language.DEFAULT), collectionDbId)) {
				ObjectNode error = Json.newObject();
				error.put("error", "Not unique collection title");
				return badRequest(error);
			}
			DB.getCollectionObjectDAO().editCollection(collectionDbId, json);
			return ok(Json.toJson(DB.getCollectionObjectDAO().get(collectionDbId)));
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	public static Result countMyAndShared() {
		ObjectNode result = Json.newObject().objectNode();
		List<String> effectiveUserIds = effectiveUserIds();
		if (effectiveUserIds.isEmpty()) {
			return badRequest("You should be signed in as a user.");
		} else {
			result = DB.getCollectionObjectDAO().countMyAndSharedCollections(toObjectIds(effectiveUserIds));
			return ok(result);
		}
	}

	public static Result list(Option<MyPlayList> directlyAccessedByUserOrGroup, Option<String> creator,
			Option<Boolean> isExhibition, Boolean collectionHits, int offset, int count, String profile,
			Option<String> locale, String sortBy) {
		if (directlyAccessedByUserOrGroup.isDefined() && (directlyAccessedByUserOrGroup.get() == null)) {
			return badRequest("Parameter 'directlyAccessedByUserOrGroup' was not well defined!");
		}

		if (WithController.isSuperUser()) {
			return list(Option.None(), Option.None(), Option.None(), Option.Some(true), isExhibition, collectionHits,
					offset, count, profile, locale, sortBy);
		}
		return list(directlyAccessedByUserOrGroup, Option.<MyPlayList>None(), creator, Option.Some(false), isExhibition,
				collectionHits, offset, count, profile, locale, sortBy);
	}

	public static Result listPublic(Option<MyPlayList> directlyAccessedByUserOrGroup, Option<String> creator,
			Option<Boolean> isExhibition, Boolean collectionHits, int offset, int count, String profile,
			Option<String> locale) {
		return list(directlyAccessedByUserOrGroup, Option.<MyPlayList>None(), creator, Option.Some(true), isExhibition,
				collectionHits, offset, count, profile, locale, "Date");
	}

	public static Result list(Option<MyPlayList> directlyAccessedByUserOrGroup,
			Option<MyPlayList> recursivelyAccessedByUserOrGroup, Option<String> creator, Option<Boolean> isPublic,
			Option<Boolean> isExhibition, Boolean collectionHits, int offset, int count, String profile,
			Option<String> locale, String sortBy) {
		ObjectNode result = Json.newObject().objectNode();
		ArrayNode collArray = Json.newObject().arrayNode();
		/*
		 * Metrics helper code
		 */
		final Histogram histogramResponseSize = MetricsUtils.registry.histogram(MetricsUtils.registry
				.name(CollectionObjectController.class, "listCollection", "histogram-response-size"));
		final Timer call_timer = MetricsUtils.registry
				.timer(MetricsUtils.registry.name(CollectionObjectController.class, "listCollection", "time"));
		final Timer.Context call_timeContext = call_timer.time();

		List<CollectionObject> userCollections;
		List<String> effectiveUserIds = effectiveUserIds();
		List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup = accessibleByUserOrGroup(
				directlyAccessedByUserOrGroup, recursivelyAccessedByUserOrGroup);
		Boolean isExhibitionBoolean = isExhibition.isDefined() ? isExhibition.get() : null;
		ObjectId creatorId = null;
		if (creator.isDefined() && creator.get().equals("undefined")) {
			result.put("collectionsOrExhibitions", Json.newObject().arrayNode());
			return ok(result);
		} else if (creator.isDefined() && !creator.get().equals("undefined")) {
			creatorId = DB.getUserDAO().getIdByUsername(creator.get());
			if (creatorId == null)
				return badRequest("User with username " + creator.get() + " does not exist.");
		}

		if (effectiveUserIds.isEmpty() || (isPublic.isDefined() && (isPublic.get() == true))) {
			// if not logged or ask for public collections, return all public
			// collections
			Tuple<List<CollectionObject>, Tuple<Integer, Integer>> info = DB.getCollectionObjectDAO().getPublicAndByAcl(
					accessedByUserOrGroup, creatorId, isExhibitionBoolean, collectionHits, offset, count);
			userCollections = info.x;
			if (info.y != null) {
				result.put("totalCollections", info.y.x);
				result.put("totalExhibitions", info.y.y);
			}
			for (CollectionObject collection : userCollections) {
				ObjectNode c;
				if (WithController.isSuperUser()) {
					c = collectionWithMyAccessData(collection, effectiveUserIds, profile, locale);
				} else {
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
			/*
			 * Metrics timer for collections DB retrieval
			 */
			final Timer dao_timer = MetricsUtils.registry.timer(MetricsUtils.registry
					.name(CollectionObjectController.class, "listCollection", "collectionsDBRetrival-time"));
			final Timer.Context dao_timeContext = dao_timer.time();

			Tuple<List<CollectionObject>, Tuple<Integer, Integer>> info;
			if (!isSuperUser())
				info = DB.getCollectionObjectDAO().getByLoggedInUsersAndAcl(toObjectIds(effectiveUserIds),
						accessedByUserOrGroup, creatorId, isExhibitionBoolean, collectionHits, offset, count, sortBy);
			else
				info = DB.getCollectionObjectDAO().getByAcl(accessedByUserOrGroup, creatorId, isExhibitionBoolean,
						collectionHits, offset, count, sortBy);
			if (info.y != null) {
				result.put("totalCollections", info.y.x);
				result.put("totalExhibitions", info.y.y);
			}
			List<ObjectNode> collections = collectionsWithMyAccessData(info.x, effectiveUserIds, profile, locale);
			for (ObjectNode c : collections) {
				c.remove("collectedResources");
				collArray.add(c);
			}
			result.put("collectionsOrExhibitions", collArray);

			try {
				return ok(result);
			} finally {
				/*
				 * Metrics helper code
				 */

				ObjectMapper objm = new ObjectMapper();

				byte[] yourBytes = new byte[0];
				try {
					yourBytes = objm.writeValueAsBytes(result);
				} catch (JsonProcessingException e) {
					log.debug("Cannot get bytes of result json.", e);
				}
				histogramResponseSize.update(yourBytes.length - histogramResponseSize.getCount());
				call_timeContext.stop();
				dao_timeContext.stop();
			}
		}
	}

	public static Result listShared(Boolean direct, Option<MyPlayList> directlyAccessedByUserOrGroup,
			Option<Boolean> isExhibition, Boolean collectionHits, int offset, int count, String profile,
			Option<String> locale, String sortBy) {
		return listShared(direct, directlyAccessedByUserOrGroup, Option.<MyPlayList>None(), isExhibition,
				collectionHits, offset, count, profile, locale, sortBy);
	}

	public static Result listShared(Boolean direct, Option<MyPlayList> directlyAccessedByUserOrGroup,
			Option<MyPlayList> recursivelyAccessedByUserOrGroup, Option<Boolean> isExhibition, boolean collectionHits,
			int offset, int count, String profile, Option<String> locale, String sortBy) {
		ObjectNode result = Json.newObject().objectNode();
		ArrayNode collArray = Json.newObject().arrayNode();
		List<String> effectiveUserIds = effectiveUserIds();
		Boolean isExhibitionBoolean = isExhibition.isDefined() ? isExhibition.get() : null;
		if (effectiveUserIds.isEmpty()) {
			return forbidden(Json.parse("\"error\", \"Must specify user for the collection\""));
		} else {
			ObjectId userId = new ObjectId(effectiveUserIds.get(0));
			List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup = new ArrayList<List<Tuple<ObjectId, Access>>>();
			accessedByUserOrGroup = accessibleByUserOrGroup(directlyAccessedByUserOrGroup,
					recursivelyAccessedByUserOrGroup);
			List<Tuple<ObjectId, Access>> accessedByLoggedInUser = new ArrayList<Tuple<ObjectId, Access>>();
			if (direct) {
				accessedByLoggedInUser.add(new Tuple<ObjectId, Access>(userId, Access.READ));
				accessedByUserOrGroup.add(accessedByLoggedInUser);
			} else {// indirectly: include collections for which user has access
				// via userGoup sharing
				for (String effectiveId : effectiveUserIds) {
					accessedByLoggedInUser.add(new Tuple<ObjectId, Access>(new ObjectId(effectiveId), Access.READ));
				}
				accessedByUserOrGroup.add(accessedByLoggedInUser);
			}
			Tuple<List<CollectionObject>, Tuple<Integer, Integer>> info = DB.getCollectionObjectDAO().getSharedAndByAcl(
					accessedByUserOrGroup, userId, isExhibitionBoolean, collectionHits, offset, count, sortBy);
			if (info.y != null) {
				result.put("totalCollections", info.y.x);
				result.put("totalExhibitions", info.y.y);
			}

			List<ObjectNode> collections = collectionsWithMyAccessData(info.x, effectiveUserIds, profile, locale);
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
			Option<MyPlayList> directlyAccessedByUserOrGroup, Option<MyPlayList> recursivelyAccessedByUserOrGroup) {
		List<List<Tuple<ObjectId, Access>>> accessedByUserOrGroup = new ArrayList<List<Tuple<ObjectId, Access>>>();
		if (directlyAccessedByUserOrGroup.isDefined()) {
			MyPlayList directlyUserNameList = directlyAccessedByUserOrGroup.get();
			for (StringTuple userAccess : directlyUserNameList.list) {
				List<Tuple<ObjectId, Access>> directlyAccessedByUser = new ArrayList<Tuple<ObjectId, Access>>();
				ObjectId userOrGroup = getUserOrGroup(userAccess.x);
				if (userOrGroup != null) {
					directlyAccessedByUser
							.add(new Tuple<ObjectId, Access>(userOrGroup, Access.valueOf(userAccess.y.toUpperCase())));
					accessedByUserOrGroup.add(directlyAccessedByUser);
				}
			}
		}
		// TODO: add support for userGroups in recursive case!!!!!
		if (recursivelyAccessedByUserOrGroup.isDefined()) {
			MyPlayList recursivelyUserNameList = recursivelyAccessedByUserOrGroup.get();
			for (StringTuple userAccess : recursivelyUserNameList.list) {
				List<Tuple<ObjectId, Access>> recursivelyAccessedByUser = new ArrayList<Tuple<ObjectId, Access>>();
				User user = DB.getUserDAO().getByUsername(userAccess.x);
				ObjectId userId = user.getDbId();
				Access access = Access.valueOf(userAccess.y.toUpperCase());
				recursivelyAccessedByUser.add(new Tuple<ObjectId, Access>(userId, access));
				Set<ObjectId> groupIds = user.getUserGroupsIds();
				for (ObjectId groupId : groupIds) {
					recursivelyAccessedByUser.add(new Tuple<ObjectId, Access>(groupId, access));
				}
				accessedByUserOrGroup.add(recursivelyAccessedByUser);
			}
		}
		return accessedByUserOrGroup;
	}

	private static ObjectId getUserOrGroup(String usernameOrDbId) {
		try {
			ObjectId dbId = new ObjectId(usernameOrDbId);
			if (DB.getUserDAO().existsEntity(dbId))
				return dbId;
			else if (DB.getUserGroupDAO().existsEntity(dbId))
				return dbId;
			else
				return null;
		} catch (IllegalArgumentException e) {
			ObjectId dbId = DB.getUserDAO().getIdByUsername(usernameOrDbId);
			if (dbId != null)
				return dbId;
			else
				return DB.getUserGroupDAO().getIdByName(usernameOrDbId);
		}
	}

	private static List<ObjectNode> collectionsWithMyAccessData(List<CollectionObject> userCollections,
			List<String> effectiveUserIds, String profile, Option<String> locale) {
		List<ObjectNode> collections = new ArrayList<ObjectNode>(userCollections.size());
		for (CollectionObject collection : userCollections) {
			collections.add(collectionWithMyAccessData(collection, effectiveUserIds, profile, locale));
		}
		return collections;
	}

	private static ObjectNode collectionWithMyAccessData(CollectionObject userCollection, List<String> effectiveUserIds,
			String profile, Option<String> locale) {
		CollectionObject profiledCollection = userCollection.getCollectionProfile(profile);
		filterResourceByLocale(locale, profiledCollection);
		ObjectNode c = (ObjectNode) Json.toJson(profiledCollection);
		Access maxAccess = getMaxAccess(profiledCollection.getAdministrative().getAccess(), effectiveUserIds);
		if (maxAccess.equals(Access.NONE))
			maxAccess = Access.READ;
		c.put("myAccess", maxAccess.toString());
		return c;
	}

	public static void addCollectionToList(int index, List<CollectionObject> collectionsOrExhibitions,
			List<ObjectId> colls, List<String> effectiveUserIds) {
		if (index < colls.size()) {
			ObjectId id = colls.get(index);
			CollectionObject c = DB.getCollectionObjectDAO().getById(id);
			if (effectiveUserIds.isEmpty()) {
				if (c.getAdministrative().getAccess().getIsPublic())
					collectionsOrExhibitions.add(c);
			} else {
				Access maxAccess = getMaxAccess(c.getAdministrative().getAccess(), effectiveUserIds);
				if (!maxAccess.equals(Access.NONE))
					collectionsOrExhibitions.add(c);
			}
		}
	}

	// If isExhibition is undefined, returns (max) countPerType collections and
	// countPerType exhibitions, i.e. (max) 2*countPerType
	// collectionsOrExhibitions
	public static Result getFeatured(String userOrGroupName, Option<Boolean> isExhibition, int offset, int countPerType,
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
				for (int i = start; (i < (start + countPerType)) && (i < collectionsSize); i++) {
					addCollectionToList(i, collectionsOrExhibitions, page.getFeaturedCollections(), effectiveUserIds);
					addCollectionToList(i, collectionsOrExhibitions, page.getFeaturedExhibitions(), effectiveUserIds);
				}
			} else {
				if (!isExhibition.get()) {
					for (int i = start; (i < (start + countPerType)) && (i < collectionsSize); i++)
						addCollectionToList(i, collectionsOrExhibitions, page.getFeaturedCollections(),
								effectiveUserIds);
				} else {
					for (int i = start; (i < (start + countPerType)) && (i < exhibitionsSize); i++)
						addCollectionToList(i, collectionsOrExhibitions, page.getFeaturedExhibitions(),
								effectiveUserIds);
				}
			}
			ArrayNode collArray = Json.newObject().arrayNode();
			List<ObjectNode> collections = collectionsWithMyAccessData(collectionsOrExhibitions, effectiveUserIds,
					profile, locale);
			for (ObjectNode c : collections)
				collArray.add(c);
			result.put("totalCollections", collectionsSize);
			result.put("totalExhibitions", exhibitionsSize);
			result.put("collectionsOrExhibitions", collArray);
			// TODO: put collection and exhibition hits in response
			return ok(result);
		} else
			return badRequest(
					"User or group with name " + userOrGroupName + " does not exist or has no specified page.");
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
		if ((favorite = DB.getCollectionObjectDAO().getByOwnerAndLabel(userId, null, "_favorites")) == null) {
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
		fav.getDescriptiveData().setLabel(new MultiLiteral(Language.DEFAULT, "_favorites"));
		DB.getCollectionObjectDAO().makePermanent(fav);
		return fav.getDbId();
	}

	/**
	 * List all Records from a Collection using a start item and a page size
	 */
	public static Result listRecordResources(String collectionId, String contentFormat, int start, int count,
			String profile, Option<String> locale, Option<String> sortingCriteria, boolean hideMine) {
		ObjectNode result = Json.newObject();
//		long notMine = DB.getRecordResourceDAO().countRecordsWithNoContributions(effectiveUserId(), new ObjectId(collectionId));
		ObjectId colId = new ObjectId(collectionId);
		Locks locks = null;
		try {
			locks = Locks.create().read("Collection #" + collectionId).acquire();
			Result response = errorIfNoAccessToCollection(Action.READ, colId);
			if (!response.toString().equals(ok().toString()))
				return response;
			else {
				List<RecordResource> records = null;
				if (hideMine) {
					records = DB.getRecordResourceDAO().getRecordsWithNoContributions(effectiveUserId(), colId, start,
							count);
				} else {
					records = (!sortingCriteria.isDefined())
							? DB.getRecordResourceDAO().getByCollectionBetweenPositions(colId, start, start + count)
							: DB.getRecordResourceDAO().getByCollectionBetweenPositionsAndSort(colId, start,
									start + count, sortingCriteria.get());
				}
				if (records == null) {
					result.put("message", "Cannot retrieve records from database!");
					return internalServerError(result);
				}
				ArrayNode recordsList = Json.newObject().arrayNode();
				for (RecordResource r : records) {
					// filter out records to which the user has no read access
					response = errorIfNoAccessToRecord(Action.READ, r.getDbId());
					if (!response.toString().equals(ok().toString())) {
						continue;
					} else if (contentFormat.equals("contentOnly")) {
						if (r.getContent() != null) {
							recordsList.add(Json.toJson(r.getContent()));
						}
						continue;
					} else if (contentFormat.equals("noContent") && (r.getContent() != null)) {
						r.getContent().clear();
						RecordResource profiledRecord = r.getRecordProfile(profile);
						filterResourceByLocale(locale, profiledRecord);
						recordsList.add(Json.toJson(profiledRecord));
						fillContextData(DB.getCollectionObjectDAO()
								.getSliceOfCollectedResources(colId, start, start + count).getCollectedResources(),
								recordsList);
						continue;
					} else if ((r.getContent() != null) && r.getContent().containsKey(contentFormat)) {
						HashMap<String, String> newContent = new HashMap<String, String>(1);
						newContent.put(contentFormat, (String) r.getContent().get(contentFormat));
						recordsList.add(Json.toJson(newContent));
						continue;
					} else {
						RecordResource profiledRecord = r.getRecordProfile(profile);
						filterResourceByLocale(locale, profiledRecord);
						recordsList.add(Json.toJson(profiledRecord));
						fillContextData(DB.getCollectionObjectDAO()
								.getSliceOfCollectedResources(colId, start, start + count).getCollectedResources(),
								recordsList);
					}
				}
				if (hideMine) {
					result.put("entryCount",
							DB.getRecordResourceDAO().countRecordsWithNoContributions(effectiveUserId(), colId));
				} else {
					result.put("entryCount",
							((CollectionAdmin) DB.getCollectionObjectDAO()
									.getById(colId, Arrays.asList("administrative.entryCount")).getAdministrative())
											.getEntryCount());
				}
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

	private static void fillContextData(List<ContextData<ContextDataBody>> contextData, ArrayNode recordsList) {
		for (JsonNode record : recordsList) {
			for (ContextData<ContextDataBody> data : contextData) {
				if (data.getTarget().getRecordId().toString().equals(record.get("dbId").asText())) {
					((ObjectNode) record).put("contextData", Json.toJson(data));
					contextData.remove(data);
					break;
				}
			}
		}

	}

	public static Result listUsersWithRights(String collectionId) {
		ArrayNode result = Json.newObject().arrayNode();
		List<String> retrievedFields = new ArrayList<String>(Arrays.asList("administrative.access"));
		CollectionObject collection = DB.getCollectionObjectDAO().getById(new ObjectId(collectionId), retrievedFields);
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
						log.error("User with id " + userId + " cannot be retrieved from db");
				}
			}
		}
		return ok(result);
	}

	/*
	 * Search for collections/exhibitions in myCollections page.
	 */
	public static Promise<Result> searchMyCollections(String term, boolean isShared, boolean isExhibition, int offset,
			int count) {
		return searchMyColOrEx(term, isShared,
				isExhibition ? WithResourceType.Exhibition.toString() : WithResourceType.SimpleCollection.toString(),
				offset, count);
	}

	public static Promise<Result> searchMyExhibitions(String term, boolean isShared, int offset, int count) {
		return searchMyColOrEx(term, isShared, WithResourceType.Exhibition.toString(), offset, count);
	}

	private static Promise<Result> searchMyColOrEx(String term, boolean isShared, String resourceType, int offset,
			int count) {
		if (effectiveUserIds().isEmpty()) {
			return Promise.pure(forbidden(Json.parse("\"error\", \"Must specify user for the collection\"")));
		}
		Query q = new Query();
		String userId = effectiveUserId();
		Query.Clause visible = Query.Clause.create();
		if (!isShared)
			visible.add("administrative.access.OWN", userId, true);
		else
			visible.add("administrative.access.READ", userId, true).add("administrative.access.WRITE", userId, true);

		Query.Clause searchTerm = Query.Clause.create().add("descriptiveData.label.default", term, false);
		Query.Clause type = Query.Clause.create().add("resourceType", resourceType, true);

		q.addClause(searchTerm.filters());
		q.addClause(type.filters());
		q.addClause(visible.filters());
		q.setStartCount(offset, count);

		Promise<SingleResponse> srp = Sources.WITHin.getDriver().execute(q);

		return srp.map(sr -> {
			Response r = new Response();
			r.query = q;
			r.addSingleResponse(sr);
			r.createAccumulated();

			return ok(Json.toJson(r));
		});

	}

	public static Promise<Result> searchPublicCollections(String term, boolean isExhibition, int offset, int count,
			Option<String> spaceId) {
		String resourceType = isExhibition ? WithResourceType.Exhibition.toString()
				: WithResourceType.SimpleCollection.toString();
		Query q = new Query();
		Query.Clause visible = Query.Clause.create();
		visible.add("administrative.isPublic", "true", true);
		Query.Clause searchTerm = Query.Clause.create().add("descriptiveData.label.default", term, false);
		Query.Clause type = Query.Clause.create().add("resourceType", resourceType, true);
		if (spaceId.isDefined()) {
			Query.Clause space = Query.Clause.create();
			space.add("administrative.access.WRITE", spaceId.get(), true);
			q.addClause(space.filters());
		}
		q.addClause(searchTerm.filters());
		q.addClause(type.filters());
		q.addClause(visible.filters());
		q.setStartCount(offset, count);
		Promise<SingleResponse> srp = Sources.WITHin.getDriver().execute(q);

		return srp.map(sr -> {
			Response r = new Response();
			r.query = q;
			r.addSingleResponse(sr);
			r.createAccumulated();

			return ok(Json.toJson(r));
		});
	}

	/*
	 * Search for records within a collection.
	 */
	public static Promise<Result> searchCollection(String term, String id, int offset, int count) {

		if (effectiveUserIds().isEmpty())
			if (!DB.getCollectionObjectDAO().isPublic(new ObjectId(id))) {
				return Promise.pure(forbidden(Json.parse("\"error\"  \"Must specify user for the collection\"")));
			}

		Query q = new Query();
		String userId = effectiveUserId();
		Query.Clause searchTerm = Query.Clause.create().add("descriptiveData.label.default", term, false);
		Query.Clause type = Query.Clause.create().add("collectedIn", id, true);

		q.addClause(searchTerm.filters());
		q.addClause(type.filters());
		q.setStartCount(offset, count);

		Promise<SingleResponse> srp = Sources.WITHin.getDriver().execute(q);

		return srp.map(sr -> {
			Response r = new Response();
			r.query = q;
			r.addSingleResponse(sr);
			r.createAccumulated();

			return ok(Json.toJson(r));
		});

	}

	public static Result getCollectionIndex(String id) {
		ObjectNode result = Json.newObject();
		try {
			ObjectId collectionDbId = new ObjectId(id);
			Result response = errorIfNoAccessToCollection(Action.READ, collectionDbId);
			if (!response.toString().equals(ok().toString()))
				return response;
			else {
				CollectionObject collection = DB.getCollectionObjectDAO().get(new ObjectId(id));
				/*
				 * List<RecordResource> firstEntries = DB.getCollectionObjectDAO()
				 * .getFirstEntries(collectionDbId, 3); result = (ObjectNode)
				 * Json.toJson(collection); result.put("firstEntries",
				 * Json.toJson(firstEntries));
				 */

				return ok(Json.toJson(collection));
			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	/**
	 * List all Records from a Collection that have certain thesaurus terms using a
	 * start item and a page size
	 */
	public static Result facetedListRecordResources(String collectionId, String contentFormat, int start, int count,
			String profile, Option<String> locale) {
		ObjectNode result = Json.newObject();
		ObjectId colId = new ObjectId(collectionId);
		Locks locks = null;

		JsonNode json = request().body().asJson();
		try {

			locks = Locks.create().read("Collection #" + collectionId).acquire();

			Result response = errorIfNoAccessToCollection(Action.READ, colId);

			if (!response.toString().equals(ok().toString())) {
				return response;
			} else {
				QueryBuilder query = CollectionIndexController.getIndexCollectionQuery(colId, json);

				SearchOptions so = new SearchOptions(start, count);
				so.isPublic = false;

				SearchResponse resp = new ElasticCoordinator().queryExcecution(query, so);

				List<String> ids = new ArrayList<>();
				SearchHits sh = resp.getHits();

				for (SearchHit hit : sh) {
					ids.add(hit.getId());
				}

				List<RecordResource> records = DB.getRecordResourceDAO().getByCollectionIds(colId, ids);

				if (records == null) {
					result.put("message", "Cannot retrieve records from database!");
					return internalServerError(result);
				}
				ArrayNode recordsList = Json.newObject().arrayNode();
				for (RecordResource r : records) {
					// filter out records to which the user has no read access
					response = errorIfNoAccessToRecord(Action.READ, r.getDbId());
					if (!response.toString().equals(ok().toString())) {
						continue;
					} else if (contentFormat.equals("contentOnly")) {
						if (r.getContent() != null) {
							recordsList.add(Json.toJson(r.getContent()));
						}
						continue;
					} else if (contentFormat.equals("noContent") && (r.getContent() != null)) {
						r.getContent().clear();
						RecordResource profiledRecord = r.getRecordProfile(profile);
						filterResourceByLocale(locale, profiledRecord);
						recordsList.add(Json.toJson(profiledRecord));
						fillContextData(DB.getCollectionObjectDAO().getById(colId, Arrays.asList("collectedResources"))
								.getCollectedResources(), recordsList);
						continue;
					} else if ((r.getContent() != null) && r.getContent().containsKey(contentFormat)) {
						HashMap<String, String> newContent = new HashMap<String, String>(1);
						newContent.put(contentFormat, (String) r.getContent().get(contentFormat));
						recordsList.add(Json.toJson(newContent));
						continue;
					} else {
						RecordResource profiledRecord = r.getRecordProfile(profile);
						filterResourceByLocale(locale, profiledRecord);
						recordsList.add(Json.toJson(profiledRecord));
						fillContextData(DB.getCollectionObjectDAO().getById(colId, Arrays.asList("collectedResources"))
								.getCollectedResources(), recordsList);
					}
				}

				result.put("entryCount", sh.getTotalHits());
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

	public static Result similarListRecordResources(String collectionId, String itemid, String contentFormat, int start,
			int count, String profile, Option<String> locale) {
		ObjectNode result = Json.newObject();
		ObjectId colId = new ObjectId(collectionId);
		Locks locks = null;

		try {

			locks = Locks.create().read("Collection #" + collectionId).acquire();

			Result response = errorIfNoAccessToCollection(Action.READ, colId);

			if (!response.toString().equals(ok().toString())) {
				return response;
			} else {

				ObjectId rid = new ObjectId(itemid);
				RecordResource rr = DB.getRecordResourceDAO().getById(rid);

				QueryBuilder query = CollectionIndexController.getSimilarItemsIndexCollectionQuery(colId,
						rr.getDescriptiveData());

				SearchOptions so = new SearchOptions(start, start + count);
				ElasticCoordinator es = new ElasticCoordinator();
				SearchResponse res = es.queryExcecution(query, so);
				SearchHits sh = res.getHits();

				List<String> ids = new ArrayList<>();
				for (Iterator<SearchHit> iter = sh.iterator(); iter.hasNext();) {
					SearchHit hit = iter.next();
					ids.add(hit.getId());
				}

				List<RecordResource> records = DB.getRecordResourceDAO().getByCollectionIds(colId, ids);
				Map<ObjectId, RecordResource> orderedMap = new HashMap<>();
				for (RecordResource r : records) {
					orderedMap.put(r.getDbId(), r);
				}

				records.clear();
				if (start == 0) {
					records.add(0, rr);
				}

				for (int i = 0; i < ids.size(); i++) {
					RecordResource r = orderedMap.get(new ObjectId(ids.get(i)));
					if (!r.getDbId().equals(rid)) {
						records.add(r);
					}
				}

				if (records == null) {
					result.put("message", "Cannot retrieve records from database!");
					return internalServerError(result);
				}
				ArrayNode recordsList = Json.newObject().arrayNode();
				for (RecordResource r : records) {
					// filter out records to which the user has no read access
					response = errorIfNoAccessToRecord(Action.READ, r.getDbId());
					if (!response.toString().equals(ok().toString())) {
						continue;
					} else if (contentFormat.equals("contentOnly")) {
						if (r.getContent() != null) {
							recordsList.add(Json.toJson(r.getContent()));
						}
						continue;
					} else if (contentFormat.equals("noContent") && (r.getContent() != null)) {
						r.getContent().clear();
						RecordResource profiledRecord = r.getRecordProfile(profile);
						filterResourceByLocale(locale, profiledRecord);
						recordsList.add(Json.toJson(profiledRecord));
						fillContextData(DB.getCollectionObjectDAO().getById(colId, Arrays.asList("collectedResources"))
								.getCollectedResources(), recordsList);
						continue;
					} else if ((r.getContent() != null) && r.getContent().containsKey(contentFormat)) {
						HashMap<String, String> newContent = new HashMap<String, String>(1);
						newContent.put(contentFormat, (String) r.getContent().get(contentFormat));
						recordsList.add(Json.toJson(newContent));
						continue;
					} else {
						RecordResource profiledRecord = r.getRecordProfile(profile);
						filterResourceByLocale(locale, profiledRecord);
						recordsList.add(Json.toJson(profiledRecord));
						fillContextData(DB.getCollectionObjectDAO().getById(colId, Arrays.asList("collectedResources"))
								.getCollectedResources(), recordsList);
					}
				}

				result.put("entryCount", sh.getTotalHits());
				result.put("records", recordsList);
				return ok(result);
			}
		} catch (Exception e1) {
//        	e1.printStackTrace();
			result.put("error", e1.getMessage());
			return internalServerError(result);
		} finally {
			if (locks != null)
				locks.release();
		}
	}

	private static ObjectNode userOrGroupJson(UserOrGroup user, Access accessRights) {
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

	private static Integer doTheImport(ObjectNode resultInfo, final CommonQuery q, final String cid,
			EuropeanaSpaceSource src, int total, int firstPageCount1) {
		SourceResponse result;
		int page = 1;
		int pageSize = 20;
		int itemsCount = firstPageCount1;
		while (itemsCount < total) {
			page++;
			q.page = page + "";
			result = src.getResults(q);
			for (WithResource<?, ?> item : result.items.getCulturalCHO()) {
				WithResourceController.internalAddRecordToCollection(cid, (RecordResource) item, F.Option.None(),
						resultInfo, true);
				itemsCount++;
			}
		}
		return 0;
	}

	public static Result addAnnotation(String id) {
		ObjectNode result = Json.newObject();
		JsonNode json = request().body().asJson();
		if (json == null) {
			result.put("error", "Invalid JSON");
			return badRequest();
		}
		if (WithController.effectiveUserDbId() == null) {
			result.put("error", "User not logged in");
			return badRequest();
		}
		try {
			ObjectId user = WithController.effectiveUserDbId();

			for (RecordResource record : DB.getRecordResourceDAO().getByCollection(new ObjectId(id),
					Arrays.asList(new String[] { "_id" }))) {
				Annotation annotation = AnnotationController.getAnnotationFromJson(json);

				annotation.getTarget().setRecordId(record.getDbId());
				annotation.getTarget().setWithURI("/record/" + record.getDbId());

				AnnotationController.addAnnotation(annotation, user);
			}

			return ok();
		} catch (Exception e1) {
			result.put("error", e1.getMessage());
			return internalServerError(result);
		}
	}

	public static Result annotateCollectionsByGroupId(String id) {
		ObjectNode result = Json.newObject();
		ObjectId gid = new ObjectId(id);

		try {
			JsonNode json = request().body().asJson();

			ObjectId user = WithController.effectiveUserDbId();
			List<AnnotatorConfig> annConfigs = AnnotatorConfig.createAnnotationConfigs(json);

			Random rand = new Random();
			String requestId = "AG" + (System.currentTimeMillis() + Math.abs(rand.nextLong())) + ""
					+ Math.abs(rand.nextLong());

			Akka.system().actorOf(Props.create(AnnotationControlActor.class, requestId, gid, user, false), requestId);
			ActorSelection ac = Akka.system().actorSelection("user/" + requestId);

			Set<String> annotated = new HashSet<>();
			for (CollectionObject co : DB.getCollectionObjectDAO().getAccessibleByGroup(gid)) {
				ObjectId cid = co.getDbId();

				Result response = errorIfNoAccessToCollection(Action.EDIT, cid);
				if (response.toString().equals(ok().toString())) {

					for (ContextData<ContextDataBody> cd : (List<ContextData<ContextDataBody>>) DB
							.getCollectionObjectDAO().getById(cid).getCollectedResources()) {
						String recordId = cd.getTarget().getRecordId().toHexString();
						if (!annotated.add(recordId)) {
							continue;
						}

						// if (DB.getRecordResourceDAO().hasAccess(uid, Action.EDIT, new
						// ObjectId(recordId)) || isSuperUser()) {
						try {
							RecordResourceController.annotateRecord(recordId, user, annConfigs, ac);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						// }
					}
				}
			}

			ac.tell(new AnnotationControlActor.AnnotateRequestsEnd(), ActorRef.noSender());

			return ok();

		} catch (Exception e1) {
			e1.printStackTrace();
			result.put("error", e1.getMessage());
			return internalServerError(result);
		}
	}

	public static Result annotateCollection(String id) {
		ObjectNode result = Json.newObject();
		ObjectId cid = new ObjectId(id);

		try {
			Result response = errorIfNoAccessToCollection(Action.EDIT, cid);
			if (!response.toString().equals(ok().toString())) {
				return response;
			} else {

				JsonNode json = request().body().asJson();
				System.out.println(">>>>>>>>> " + json);

				ObjectId user = WithController.effectiveUserDbId();
				List<AnnotatorConfig> annConfigs = AnnotatorConfig.createAnnotationConfigs(json);

				Random rand = new Random();
				String requestId = "AC" + (System.currentTimeMillis() + Math.abs(rand.nextLong())) + ""
						+ Math.abs(rand.nextLong());

				Akka.system().actorOf(Props.create(AnnotationControlActor.class, requestId, cid, user, false),
						requestId);
				ActorSelection ac = Akka.system().actorSelection("user/" + requestId);

				for (ContextData<ContextDataBody> cd : (List<ContextData<ContextDataBody>>) DB.getCollectionObjectDAO()
						.getById(cid).getCollectedResources()) {
					String recordId = cd.getTarget().getRecordId().toHexString();

//					if (DB.getRecordResourceDAO().hasAccess(uid, Action.EDIT, new ObjectId(recordId))  || isSuperUser()) {
					try {
						RecordResourceController.annotateRecord(recordId, user, annConfigs, ac);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
//					}
				}

				ac.tell(new AnnotationControlActor.AnnotateRequestsEnd(), ActorRef.noSender());

				return ok();
			}

		} catch (Exception e1) {
			e1.printStackTrace();
			result.put("error", e1.getMessage());
			return internalServerError(result);
		}
	}

	private static class ScoreInfo {
		public String id;
		public boolean approvedByUser;
		public boolean rejectedByUser;
		public int approved;
		public int rejected;

		public ScoreInfo(String id, int approved, int rejected, boolean approvedByUser, boolean rejectedByUser) {
			this.id = id;
			this.approved = approved;
			this.rejected = rejected;
			this.approvedByUser = approvedByUser;
			this.rejectedByUser = rejectedByUser;
		}
	}

	private static abstract class AnnotationInfo {
		public Map<String, List<ScoreInfo>> recordAnnotationMap;

		public AnnotationInfo() {
			this.recordAnnotationMap = new HashMap<String, List<ScoreInfo>>();
		}

		public void add(ObjectId recordId, Annotation ann, ObjectId userId) {
			AnnotationScore score = ann.getScore();
			int approved = 0;
			int rejected = 0;
			boolean approvedByUser = false;
			boolean rejectedByUser = false;

			if (score != null) {
				ArrayList<AnnotationAdmin> aby = score.getApprovedBy();
				ArrayList<AnnotationAdmin> rby = score.getRejectedBy();

				if (aby != null) {
					if (userId != null) {
						for (AnnotationAdmin id : aby) {
							if (id.getWithCreator().equals(userId)) {
								approvedByUser = true;
								break;
							}
						}
					}
					approved = aby.size();
				}
				if (rby != null) {
					if (userId != null) {
						for (AnnotationAdmin id : rby) {
							if (id.getWithCreator().equals(userId)) {
								rejectedByUser = true;
								break;
							}
						}
					}
					rejected = rby.size();
				}
			}

			List<ScoreInfo> annIds = recordAnnotationMap.get(recordId.toString());
			if (annIds == null) {
				annIds = new ArrayList<ScoreInfo>();
				recordAnnotationMap.put(recordId.toString(), annIds);
			}
			annIds.add(new ScoreInfo(ann.getDbId().toString(), approved, rejected, approvedByUser, rejectedByUser));
		}
	}

	private static class VocAnnotationInfo extends AnnotationInfo {
		public String uri;
		public String uriVocabulary;
		public MultiLiteral label;

		public String showLabel;

		public VocAnnotationInfo(String uri, String uriVocabulary, MultiLiteral label) {
			super();
			this.uri = uri;
			this.uriVocabulary = uriVocabulary;
			this.label = label;

			if (label != null) {
				List<String> def = label.get(Language.DEFAULT);
				if (def != null && def.size() > 0) {
					this.showLabel = def.get(0);
				}
			}

			if (showLabel == null) {
				showLabel = (uriVocabulary != null ? uriVocabulary : "unknown") + " : " + uri;
			} else {
				showLabel = (uriVocabulary != null ? uriVocabulary : "unknown") + " : " + showLabel;
			}
		}
	}

	private static class GeoAnnotationInfo extends AnnotationInfo {
		@JsonDeserialize(using = PointDeserializer.class)
		@JsonSerialize(using = PointSerializer.class)
		public Point coordinates;

		public GeoAnnotationInfo(Point coordinates) {
			super();
			this.coordinates = coordinates;
		}
	}

	public static Result getAnnotationSummary(String colId, String mode, boolean userOnly) {

		try {
			Result response = errorIfNoAccessToCollection(Action.READ, new ObjectId(colId));

			if (!response.toString().equals(ok().toString())) {
				return response;
			}

			int groupBy = 0;
			if (mode.equals("ANNOTATOR")) {
				groupBy = 1;
			} else if (mode.equals("VOCABULARY")) {
				groupBy = 2;
			}

			ObjectId userId = null;
			User user = effectiveUser();
			if (user != null) {
				userId = user.getDbId();
			}

			Map<String, Map<String, VocAnnotationInfo>> annMap = new TreeMap<>();
			Map<Point, GeoAnnotationInfo> geoGroup = new HashMap<>();

			for (Annotation ann : DB.getAnnotationDAO().getByCollection(new ObjectId(colId))) {
				if (userOnly) {
					boolean include = false;
					if (userId != null) {
						List<AnnotationAdmin> admins = ann.getAnnotators();

						for (AnnotationAdmin admin : admins) {
							if (admin.getWithCreator() != null && admin.getWithCreator().equals(userId)) {
								include = true;
								break;
							}
						}
					}

					if (!include) {
						continue;
					}
				}

				if (ann.getBody() instanceof AnnotationBodyTagging) {
					AnnotationBodyTagging body = ((AnnotationBodyTagging) ann.getBody());

					String uri = body.getUri();
					String uriVocabulary = body.getUriVocabulary().toLowerCase();
					MultiLiteral ml = ((AnnotationBodyTagging) ann.getBody()).getLabel();

					if (groupBy == 0) {
						Map<String, VocAnnotationInfo> annGroup = annMap.get("");
						if (annGroup == null) {
							annGroup = new HashMap<String, VocAnnotationInfo>();
							annMap.put("", annGroup);
						}

						VocAnnotationInfo info = annGroup.get(uri);
						if (info == null) {
							info = new VocAnnotationInfo(uri, uriVocabulary, ml);
							annGroup.put(uri, info);
						}

						info.add(ann.getTarget().getRecordId(), ann, userId);

					} else if (groupBy == 1) {
						for (AnnotationAdmin annAd : (ArrayList<AnnotationAdmin>) ann.getAnnotators()) {
							String generator = annAd.getGenerator();
							if (generator == null) {
								generator = "Unknown Annotator";
							}

							Map<String, VocAnnotationInfo> annGroup = annMap.get(generator);
							if (annGroup == null) {
								annGroup = new HashMap<String, VocAnnotationInfo>();
								annMap.put(generator, annGroup);
							}

							VocAnnotationInfo info = annGroup.get(uri);
							if (info == null) {
								info = new VocAnnotationInfo(uri, uriVocabulary, ml);
								annGroup.put(uri, info);
							}

							info.add(ann.getTarget().getRecordId(), ann, userId);
						}
					} else if (groupBy == 2) {
						Vocabulary voc = Vocabulary.getVocabulary(uriVocabulary);
						String name;
						if (voc != null) {
							name = voc.getLabel();
						} else {
							name = "Unknown";
						}

						Map<String, VocAnnotationInfo> annGroup = annMap.get(name);
						if (annGroup == null) {
							annGroup = new HashMap<String, VocAnnotationInfo>();
							annMap.put(name, annGroup);
						}

						VocAnnotationInfo info = annGroup.get(uri);
						if (info == null) {
							info = new VocAnnotationInfo(uri, uriVocabulary, ml);
							annGroup.put(uri, info);
						}

						info.add(ann.getTarget().getRecordId(), ann, userId);

					}
				} else if (ann.getBody() instanceof AnnotationBodyGeoTagging) {
					AnnotationBodyGeoTagging body = ((AnnotationBodyGeoTagging) ann.getBody());

					Point point = body.getCoordinates();

					GeoAnnotationInfo info = geoGroup.get(point);
					if (info == null) {
						info = new GeoAnnotationInfo(point);
						geoGroup.put(point, info);
					}

					info.add(ann.getTarget().getRecordId(), ann, userId);

				}
			}

			List<ObjectNode> groupList = new ArrayList<>();

			for (Map.Entry<String, Map<String, VocAnnotationInfo>> entry : annMap.entrySet()) {
				ObjectNode groupJson = Json.newObject();
				groupJson.put("name", entry.getKey());

				ArrayList<VocAnnotationInfo> list = new ArrayList<>(entry.getValue().values());
				Collections.sort(list, new Comparator<VocAnnotationInfo>() {

					@Override
					public int compare(VocAnnotationInfo arg0, VocAnnotationInfo arg1) {
						return arg0.showLabel.compareTo(arg1.showLabel);
					}
				});

				ArrayNode recArray = Json.newObject().arrayNode();

				for (VocAnnotationInfo info : list) {
					ObjectNode uriJson = Json.newObject();
					uriJson.put("uri", info.uri);
					uriJson.put("label", Json.toJson(info.label));
					uriJson.put("uriVocabulary", Json.toJson(info.uriVocabulary));

					ArrayNode instances = Json.newObject().arrayNode();
					for (Map.Entry<String, List<ScoreInfo>> raEntry : info.recordAnnotationMap.entrySet()) {
						ObjectNode instanceJson = Json.newObject();
						instanceJson.put("recordId", raEntry.getKey());

						ArrayNode ids = Json.newObject().arrayNode();
						for (ScoreInfo si : raEntry.getValue()) {
							ObjectNode scoreJson = Json.newObject();
							scoreJson.put("id", si.id);
							scoreJson.put("approved", si.approved);
							scoreJson.put("rejected", si.rejected);
							scoreJson.put("userapproved", si.approvedByUser);
							scoreJson.put("userrejected", si.rejectedByUser);

							ids.add(scoreJson);
						}

						instanceJson.put("annotations", ids);
						instances.add(instanceJson);
					}

					uriJson.put("instances", instances);

					recArray.add(uriJson);
				}

				groupJson.put("annotations", recArray);

				groupList.add(groupJson);
			}

			Collections.sort(groupList, new Comparator<ObjectNode>() {
				@Override
				public int compare(ObjectNode o1, ObjectNode o2) {
					return o1.get("name").asText().compareTo(o2.get("name").asText());
				}

			});

			ArrayNode groups = Json.newObject().arrayNode();

			for (ObjectNode on : groupList) {
				groups.add(on);
			}

			List<ObjectNode> geoGroupList = new ArrayList<>();

//			for (Map.Entry<String, Map<String, VocAnnotationInfo>> entry : annMap.entrySet()) {
			ObjectNode groupJson = Json.newObject();
			groupJson.put("name", "Coordinates");

			ArrayList<GeoAnnotationInfo> list = new ArrayList<>(geoGroup.values());
//				Collections.sort(list, new Comparator<GeoAnnotationInfo>() {
//
//					@Override
//					public int compare(GeoAnnotationInfo arg0, GeoAnnotationInfo arg1) {
//						return arg0.showLabel.compareTo(arg1.showLabel);
//					}
//				});

			ArrayNode recArray = Json.newObject().arrayNode();

			for (GeoAnnotationInfo info : list) {
				ObjectNode uriJson = Json.newObject();
				JsonNode coord = Json.toJson(info.coordinates);
				((ObjectNode) coord).remove("coordinates");
				uriJson.put("coordinates", coord);

				ArrayNode instances = Json.newObject().arrayNode();
				for (Map.Entry<String, List<ScoreInfo>> raEntry : info.recordAnnotationMap.entrySet()) {
					ObjectNode instanceJson = Json.newObject();
					instanceJson.put("recordId", raEntry.getKey());

					ArrayNode ids = Json.newObject().arrayNode();
					for (ScoreInfo si : raEntry.getValue()) {
						ObjectNode scoreJson = Json.newObject();
						scoreJson.put("id", si.id);
						scoreJson.put("approved", si.approved);
						scoreJson.put("rejected", si.rejected);
						scoreJson.put("userapproved", si.approvedByUser);
						scoreJson.put("userrejected", si.rejectedByUser);

						ids.add(scoreJson);
					}

					instanceJson.put("annotations", ids);
					instances.add(instanceJson);
				}

				uriJson.put("instances", instances);

				recArray.add(uriJson);
			}

			groupJson.put("annotations", recArray);

			geoGroupList.add(groupJson);
//			}

//			Collections.sort(groupList, new Comparator<ObjectNode>() {
//				@Override
//				public int compare(ObjectNode o1, ObjectNode o2) {
//					return o1.get("name").asText().compareTo(o2.get("name").asText());
//				}
//				
//			});

			ArrayNode geoGroups = Json.newObject().arrayNode();

			for (ObjectNode on : geoGroupList) {
				geoGroups.add(on);
			}

			ObjectNode res = Json.newObject();
			res.put("groups", groups);
			res.put("geo", geoGroups);

			return ok(res);

		} catch (Exception e) {
			e.printStackTrace();
			ObjectNode result = Json.newObject();
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

//	public static Result getAnnotations(String colId) {
//		ObjectNode result = Json.newObject();
//		
//		try {
//			Result response = errorIfNoAccessToCollection(Action.READ, new ObjectId(colId));
//			
//			if (!response.toString().equals(ok().toString())) {
//				return response;
//			}
//	
//			List<Annotation> anns = DB.getAnnotationDAO().getByCollection(new ObjectId(colId));
//			
//			return ok(Json.toJson(anns));
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//			result.put("error", e.getMessage());
//			return internalServerError(result);
//		}
//	}

	public static Result getAnnotations(String colId) {
		ObjectNode result = Json.newObject();

		try {
			Result response = errorIfNoAccessToCollection(Action.READ, new ObjectId(colId));

			if (!response.toString().equals(ok().toString())) {
				return response;
			}

			List<Annotation> anns = DB.getAnnotationDAO().getByCollection(new ObjectId(colId));

			return ok(Json.toJson(anns));

		} catch (Exception e) {
			e.printStackTrace();
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}

	public static Result nerdCollection(String colId) {
		List<RecordResource> records = DB.getRecordResourceDAO().getByCollection(new ObjectId(colId));	
		if (records != null){
			Function<String,Result> nerdRecord = (String recId) -> {
				return RecordResourceController.nerdRecord(recId);
			};
			for (RecordResource rec: records){
				ParallelAPICall.createPromise(nerdRecord, rec.getDbId().toString());
			}
			return ok();
		}
		else{
			return notFound();
		}
	}

	public static Result artStyleCollection(String colId, String myType, String model) {
		List<RecordResource> records = DB.getRecordResourceDAO().getByCollection(new ObjectId(colId));	
		if (records != null){
			Function<String,Result> artStyleRecord = (String recId) -> {
				return RecordResourceController.artStyleRecord(recId, myType, model);
			};
			for (RecordResource rec: records){
				ParallelAPICall.createPromise(artStyleRecord, rec.getDbId().toString());
			}
			return ok();
		}
		else{
			return notFound();
		}
	}
}



