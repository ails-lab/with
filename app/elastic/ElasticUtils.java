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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.net.MediaType;

import db.DB;
import model.DescriptiveData;
import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.MultiLiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import model.basicDataTypes.WithAccess.AccessEntry;
import model.resources.WithAdmin;
import model.resources.WithResource;
import model.resources.WithResourceType;
import play.Logger;
import play.libs.Json;
import utils.Serializer;

public class ElasticUtils {
	static private final Logger.ALogger log = Logger.of(ElasticUtils.class);

	/*
	 * Define the type of that instance
	 */
	public static <E> String defineInstanceOf(E doc) {

		String instanceName = doc.getClass().getSimpleName();
		List<String> enumNames = new ArrayList<String>();
		Arrays.asList(WithResourceType.values()).forEach( (t) -> {enumNames.add(t.toString()); return;} );
		if(enumNames.contains(instanceName)) {
			if(!instanceName.equalsIgnoreCase(WithResourceType.WithResource.toString()))
				return instanceName.toLowerCase();
			else
				return WithResourceType.RecordResource.toString().toLowerCase();
		} else
			return null;

	}

	/*
	 * Retrieve from DB the resources that where returned
	 * from an elastic query.
	 * Returns a Map of List of Resources per Type.
	 */

	public static Map<String, List<?>> getResourcesPerType(SearchResponse resp) {

		Map<String, List<ObjectId>> idsOfEachType = new HashMap<String, List<ObjectId>>();
		resp.getHits().forEach( (h) -> {
			if(!idsOfEachType.containsKey(h.getType())) {
				idsOfEachType.put(h.getType(), new ArrayList<ObjectId>() {{ add(new ObjectId(h.getId())); }});
			} else {
				idsOfEachType.get(h.getType()).add(new ObjectId(h.getId()));
			}
		});

		Map<String, List<?>> resourcesPerType = new HashMap<String, List<?>>();

		for(Entry<String, List<ObjectId>> e: idsOfEachType.entrySet()) {
			resourcesPerType.put(e.getKey() , DB.getRecordResourceDAO().getByIds(e.getValue()));

		}

		return resourcesPerType;
	}


	/*
	 * Method that creates elastic json with the same fields
	 */
	public static <T extends DescriptiveData, A extends WithAdmin> Map<String, Object> toIndex(WithResource<T, A> rr) {


		/*
		 *
		 */
		JsonNode m = mediaMapConverter(rr.getMedia());
		JsonNode jn = withAccessConverter(rr.getAdministrative());


		/*
		 *
		 */

		ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addSerializer(MultiLiteral.class, new Serializer.MUltiliteralSerializerForElastic());
		module.addSerializer(MultiLiteralOrResource.class, new Serializer.MUltiliteralSerializerForElastic());
		module.addSerializer(WithAccess.class, new Serializer.WithAccessSerializerForElastic());
		mapper.registerModule(module);
		mapper.setSerializationInclusion(Include.NON_NULL);

		// json with multiliteral _all and languages fields
		JsonNode json = mapper.valueToTree(rr);
		((ObjectNode)json).set("administrative", jn);
		((ObjectNode)json).set("media", m);

		if(json.get("provenance")!=null)
			((ArrayNode)json.get("provenance")).add(getDataProvAndSource(rr.getProvenance()));
		((ObjectNode)json).remove("content");
		((ObjectNode)json).remove("collectedResources");
		((ObjectNode)json).remove("dbId");


		Map<String, Object> elasticDoc = mapper.convertValue(json, Map.class);

		return elasticDoc;
	}



	private static JsonNode getDataProvAndSource(List<ProvenanceInfo> pr) {
		ObjectNode jn = Json.newObject();
		if(pr.size() > 0) {
			jn.put("dataProvider", pr.get(0).getProvider());
			jn.put("source", pr.get(pr.size()-1).getProvider());
		}

		return jn;

	}


	private static JsonNode mediaMapConverter(List<HashMap<MediaVersion, EmbeddedMediaObject>> m) {
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addSerializer(MediaType.class, new Serializer.MimeTypeSerializer());
		module.addSerializer(EmbeddedMediaObject.class, new Serializer.EmbeddeMediaSerializer());
		mapper.registerModule(module);
		mapper.setSerializationInclusion(Include.NON_NULL);

		List<EmbeddedMediaObject> ms = new ArrayList<EmbeddedMediaObject>();
		for(Map<MediaVersion, EmbeddedMediaObject> i: m)
			for(Entry<?,EmbeddedMediaObject> e: i.entrySet())
				ms.add(e.getValue());

		return mapper.valueToTree(ms);
	}

	private static JsonNode withAccessConverter(WithAdmin wa) {
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addSerializer(ObjectId.class, new Serializer.ObjectIdArraySerializer());
		module.addSerializer(WithAccess.class, new Serializer.WithAccessSerializerForElastic());
		mapper.registerModule(module);
		mapper.setSerializationInclusion(Include.NON_NULL);


		ObjectNode jn =  (ObjectNode) mapper.valueToTree(wa.getAccess());
		ObjectNode collBY = mapper.createObjectNode();
		collBY.put("READ", Json.toJson(wa.getCollectedBy().stream().filter( ae -> ae.getLevel()==Access.READ)
				.map(AccessEntry::getUser).map(ObjectId::toString).collect(Collectors.toList())));
		collBY.put("WRITE", Json.toJson(wa.getCollectedBy().stream().filter( ae -> ae.getLevel()==Access.WRITE)
				.map(AccessEntry::getUser).map(ObjectId::toString).collect(Collectors.toList())));
		collBY.put("OWN", Json.toJson(wa.getCollectedBy().stream().filter( ae -> ae.getLevel()==Access.OWN)
				.map(AccessEntry::getUser).map(ObjectId::toString).collect(Collectors.toList())));
		jn.put("collectedBy", collBY);
		return jn;
	}

	
	/**
	 * Remove documents from elastic that are not in mongo any more
	 */
	public static void purgeIndex() {
		try {
			// make a mongo record id set and check all existing elastic ids against it
			// make a list of the ones that are not in mongo
			Set<String> mongoIds = DB.getRecordResourceDAO().listIds()
					.collect( Collectors.toCollection(()->new HashSet<String>() ));

			ElasticDAO e = ElasticDAO.instance();
			Set<String> elasticIds = new HashSet<String>(e.findAllRecordIds());

			elasticIds.removeAll(mongoIds);
			log.info( "Found " + elasticIds.size() + " orphaned elastic culturalobject, removing ...");
			e.removeById(elasticIds, "culturalobject" );

			mongoIds = DB.getCollectionObjectDAO().listIds()
					.collect( Collectors.toCollection(()->new HashSet<String>() ));

			elasticIds = new HashSet<String>(e.findAllCollectionIds());
			elasticIds.addAll( e.findAllIdsByType("exhibition"));

			elasticIds.removeAll(mongoIds);
			log.info( "Found " + elasticIds.size() + " orphaned elastic simplecollection, removing ...");
			e.removeById(elasticIds, "simplecollection", "exhibition" );

			log.info( "Finished purging elastic");
		} catch( Exception e ) {
			log.error( "Purging elastic failed", e );
		}
	}
}
