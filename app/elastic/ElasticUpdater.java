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


package elastic;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.basicDataTypes.CollectionInfo;
import org.bson.types.ObjectId;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.script.ScriptService.ScriptType;
import org.elasticsearch.search.fetch.source.FetchSourceContext;

import com.fasterxml.jackson.databind.node.ArrayNode;
import play.Logger;
import play.libs.Json;

public class ElasticUpdater {
	static private final Logger.ALogger log = Logger.of(ElasticUpdater.class);


	/*
	 * Update one document with the structure provided.
	 */
	public static boolean updateOne(String type, ObjectId id, Map<String, Object> doc) {
		try{
			Elastic.getTransportClient().prepareUpdate(
					Elastic.index,
					type,
					id.toString())
					.setDoc(doc)
					.get();
		} catch(Exception e) {
			log.error("Cannot update (reindex) resource", e );
			return false;
		}
		return true;
	}


	/*
	 * Update one document with the structure provided
	 * retrieving dynamically the type.
	 */
	public static boolean updateOneNoType(ObjectId id, Map<String, Object> doc) {
		try{
			GetResponse resp = Elastic.getTransportClient().get(new GetRequest(Elastic.index, "_all", id.toString())
			.fetchSourceContext(FetchSourceContext.DO_NOT_FETCH_SOURCE))
			.actionGet();

			Elastic.getTransportClient().prepareUpdate(
					Elastic.index,
					resp.getType(),
					id.toString())
					.setDoc(doc)
					.get();
		} catch(Exception e) {
			log.error("Cannot update (reindex) resource", e );
			return false;
		}
		return true;
	}


	/*
	 * Bulk updates. Updates all documents provided with the structure
	 * provided.
	 */
	public static boolean updateMany(List<String> types, List<ObjectId> ids, List<Map<String, Object>> docs) throws Exception {

		if((ids.size() != docs.size()) ||
				(ids.size() != types.size()) ||
				(docs.size() != types.size())) {
			throw new Exception("Error: ids list does not have the same size with upDocs list");
		}

		if( ids.size() == 0 ) {
			log.debug("No resources to update!");
			return true;
		} else if( ids.size() == 1 ) {
					Elastic.getTransportClient().prepareUpdate(
							Elastic.index,
							types.get(0),
							ids.get(0).toString())
							.setDoc(docs.get(0))
							.get();
					return true;
		} else {

				int i = 0;
				for(Map<String, Object> doc: docs) {
					Elastic.getBulkProcessor().add(new UpdateRequest(
							Elastic.index,
							types.get(i),
							ids.get(i).toString())
					.doc(doc));
					i++;
				}
				Elastic.getBulkProcessor().close();
				return true;
		}

	}

	/*
	 * Bulk updates. Updates all documents provided with the structure
	 * provided.
	 */
	public static boolean updateManyNoType(List<ObjectId> ids, List<Map<String, Object>> docs) throws Exception {

		if((ids.size() != docs.size())) {
			throw new Exception("Error: ids list does not have the same size with upDocs list");
		}

		if( ids.size() == 0 ) {
			log.debug("No resources to update!");
			return true;
		} else if( ids.size() == 1 ) {
					GetResponse resp = Elastic.getTransportClient().get(new GetRequest(Elastic.index, "_all", ids.get(0).toString())
					.fetchSourceContext(FetchSourceContext.DO_NOT_FETCH_SOURCE))
					.actionGet();

					Elastic.getTransportClient().prepareUpdate(
							Elastic.index,
							resp.getType(),
							ids.get(0).toString())
							.setDoc(docs.get(0))
							.get();
					return true;
		} else {
				List<String> types = new ArrayList<String>();
				for(ObjectId id: ids) {
					GetResponse resp = Elastic.getTransportClient().get(new GetRequest(Elastic.index, "_all", id.toString())
					.fetchSourceContext(FetchSourceContext.DO_NOT_FETCH_SOURCE))
					.actionGet();
					types.add(resp.getType());
				}

				int i = 0;
				for(Map<String, Object> doc: docs) {
					Elastic.getBulkProcessor().add(new UpdateRequest(
							Elastic.index,
							types.get(i),
							ids.get(i).toString())
					.doc(doc));
					i++;
				}
				Elastic.getBulkProcessor().close();
				return true;
		}

	}


	/*
	 * Update Context Data on many records
	 * Op stands for '+', '-', '/'
	 */
	public static boolean scriptAddOrRemoveToListField(List<ObjectId> ids, List<Map<String, Object>> docs, String field, String op) throws Exception {

		if((ids.size() != docs.size())) {
			throw new Exception("Error: ids list does not have the same size with upDocs list");
		}

		String script = "ctx._source." + field + " " + op + "= param";

		if( ids.size() == 0 ) {
			log.debug("No resources to update!");
			return true;
		} else if( ids.size() == 1 ) {
					GetResponse resp = Elastic.getTransportClient().get(new GetRequest(Elastic.index, "_all", ids.get(0).toString())
					.fetchSourceContext(FetchSourceContext.DO_NOT_FETCH_SOURCE))
					.actionGet();

					Elastic.getTransportClient().prepareUpdate(
							Elastic.index,
							resp.getType(),
							ids.get(0).toString())
							.addScriptParam("param", docs.get(0))
							.setScript(script, ScriptType.INLINE)
			.setRetryOnConflict(5)
			.execute().actionGet();
					return true;
		} else {
				List<String> types = new ArrayList<String>();
				for(ObjectId id: ids) {
					GetResponse resp = Elastic.getTransportClient().get(new GetRequest(Elastic.index, "_all", id.toString())
					.fetchSourceContext(FetchSourceContext.DO_NOT_FETCH_SOURCE))
					.actionGet();
					types.add(resp.getType());
				}

				int i = 0;
				for(Map<String, Object> doc: docs) {
					Elastic.getBulkProcessor().add(new UpdateRequest(
							Elastic.index,
							types.get(i),
							ids.get(i).toString())
					.addScriptParam("param", docs.get(0))
					.script(script, ScriptType.INLINE)
					.retryOnConflict(5));
					i++;
				}
				Elastic.getBulkProcessor().close();
				return true;
		}

	}

	/*
	 * Completes the whole update process of a merged document
	 */
	public static void addResourceToCollection(String id, List<CollectionInfo> newColIn) throws IOException {
		Map<String, List<Object>> doc =  new HashMap<String, List<Object>>();
		for(CollectionInfo ci: newColIn) {
			Map<String, Object> colInfo = new HashMap<String, Object>();
			colInfo.put("collectionId", ci.getCollectionId().toString());
			colInfo.put("position", ci.getPosition());

			if(doc.containsKey("collectedIn")) doc.get("collectedIn").add(colInfo);
			else doc.put("collectedIn", new ArrayList<Object>() {{ add(colInfo); }});
		}
		try {
			Elastic.getTransportClient().prepareUpdate()
				.setIndex(Elastic.index)
				.setType(Elastic.typeResource)
				.setId(id)
			.setDoc(doc)
			.setRetryOnConflict(5)
			.execute().actionGet();
		} catch (ElasticsearchException  e) {
			log.error("Cannot add entry to collectedIn and update document!", e);
		}
	}


	public static void removeResourceFromCollection(String id, ObjectId colId, int position) {
		try {
			Elastic.getTransportClient().prepareUpdate()
				.setIndex(Elastic.index)
				.setType(Elastic.typeResource)
				.setId(id)
			.addScriptParam("colId", colId.toString())
			.addScriptParam("pos", position)
			.setScript("info = null;"
					+ "for(el in ctx._source.collectedIn) {"
					+ " if(el.collectionId.equals(colId) &&"
					+ "    el.position == pos) { "
					+ "      info = el; "
					+ "  } "
					+ "};"
					+ "ctx._source.collectedIn.remove(info) ", ScriptType.INLINE)
			.setRetryOnConflict(5)
			.execute().actionGet();
		} catch (ElasticsearchException  e) {
			log.error("Cannot remove entry from collectedIn and update document!", e);
		}
	}


	public static void updatePositionInCollection(String id, ObjectId colId, int old_position, int new_position) {
		try {
			Elastic.getTransportClient().prepareUpdate()
				.setIndex(Elastic.index)
				.setType(Elastic.typeResource)
				.setId(id)
			.addScriptParam("colId", colId.toString())
			.addScriptParam("old_pos", old_position)
			.addScriptParam("new_pos", new_position)
			.setScript("for(el in ctx._source.collectedIn) {"
					+ "   if( el.collectionId.equals(colId) &&"
					+ "    el.position == old_pos) { "
					+ "      el.position = new_pos; "
					+ " } "
					+ "  };", ScriptType.INLINE)
			.setRetryOnConflict(5)
			.execute().actionGet();
		} catch (ElasticsearchException  e) {
			log.error("Cannot update merged record document!", e);
		}
	}


	/*
	 * Update rights on a collection
	 */
	public static void updateCollectionRights(String type, ObjectId id) {
		try {
			Elastic.getTransportClient().prepareUpdate(
						Elastic.index,
						type,
						id.toString())
				.setDoc(prepareUpdateOnRights(id))
				.execute().actionGet();		} catch (Exception e) {
			log.error("Cannot update collection rights!", e);
		}
	}

	public static XContentBuilder prepareUpdateOnRights(ObjectId id) {
		XContentBuilder doc = null;
		try {

			doc = jsonBuilder().startObject();
			ArrayNode array = Json.newObject().arrayNode();
			/*for(Entry<ObjectId, Access> e: collection.getRights().entrySet()) {
				ObjectNode right = Json.newObject();
				right.put("user", e.getKey().toString());
				switch (e.getValue().toString()) {
				case "OWN":
					right.put("access", 3);
					break;
				case "WRITE":
					right.put("access", 2);
					break;
				case "READ":
					right.put("access", 1);
					break;
				case "NONE":
					right.put("access", 0);
					break;
				default:
					break;
				}
				array.add(right);
			}*/
			doc.rawField("rights", array.toString().getBytes());
			doc.endObject();

		} catch(IOException io) {
			log.error("Cannot create document to update!", io);
			return null;
		}

		return doc;
	}


	/*
	 * Takes a list of ids and visibility values and updates
	 * the visibility on these documents
	 *
	 * For example when a Collection becomes public then we have to make all the nested douments public
	 * Or when a Collection becomes private the all the resources become private unless that resources that
	 * belong in public collections.
	 */
	public static void updateVisibility(String type, List<ObjectId> ids, List<Boolean> visibility) throws Exception {

		if(ids.size() != visibility.size()) {
			throw new Exception("Error: ids list does not have the same size with upDocs list");
		}


		XContentBuilder doc = null;
		if( ids.size() == 0 ) {
			log.debug("No resources to update!");
		} else if( ids.size() == 1 ) {

			try {
				doc = jsonBuilder().startObject();
				doc.field("isPublic", visibility.get(0));
				doc.endObject();
			} catch(IOException io) {
				log.error("Cannot create document to update!", io);
			}

			Elastic.getTransportClient().prepareUpdate(
					Elastic.index,
					type,
					ids.get(0).toString())
				.setDoc(doc)
				.get();
		} else {
				for(int i = 0; i<ids.size(); i++) {

					try {
						doc = jsonBuilder().startObject();
						doc.field("isPublic", visibility.get(i));
						doc.endObject();
					} catch(IOException io) {
						log.error("Cannot create document to update!", io);
					}
					Elastic.getBulkProcessor().add(new UpdateRequest(
							Elastic.index,
							type,
							ids.get(i).toString())
						.doc(doc));
				}
				Elastic.getBulkProcessor().flush();
		}
	}


	/*
	 * Increment likes on collection type
	 */

	public static void incLikes(String type, ObjectId id) {
		try {
			Elastic.getTransportClient().prepareUpdate(
						Elastic.index,
						type,
						id.toString())
				.setScript("ctx._source.usage.likes++;", ScriptType.INLINE)
				.execute().actionGet();
			} catch (Exception e) {
			log.error("Cannot update collection likes!", e);
			}
	}

	/*
	 * Decrement likes on collection type
	 */
	public static void decLikes(String type, ObjectId id) {
		try {
			Elastic.getTransportClient().prepareUpdate(
						Elastic.index,
						type,
						id.toString())
				.setScript("ctx._source.usage.likes--;", ScriptType.INLINE)
				.execute().actionGet();
			} catch (Exception e) {
			log.error("Cannot update collection likes!", e);
			}
	}

}
