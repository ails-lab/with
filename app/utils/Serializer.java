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


package utils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.ArrayList;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.mongodb.morphia.geo.Point;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import controllers.WithController;
import model.EmbeddedMediaObject;
import model.basicDataTypes.Language;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import model.basicDataTypes.WithAccess.AccessEntry;
import model.usersAndGroups.User;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import sources.utils.JsonContextRecord;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.net.MediaType;

import db.DB;

public class Serializer {
	public static final ALogger log = Logger.of(Serializer.class);

	public static class ObjectIdSerializer extends JsonSerializer<Object> {
		@Override
		public void serialize(Object oid, JsonGenerator jsonGen,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
			jsonGen.writeString(oid.toString());
		}

	}
	
	public static class JsonContextRecordSerializer extends JsonSerializer<Object> {
		@Override
		public void serialize(Object obj, JsonGenerator jsonGen,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
			jsonGen.writeObject(Json.toJson(((JsonContextRecord)obj).getRootInformation()));
		}

	}


	public static class ObjectIdArraySerializer extends JsonSerializer<Object> {
		@Override
		public void serialize(Object objectIds, JsonGenerator jsonGen,
				SerializerProvider provider) {
			try {
				HashSet<String> ids = new HashSet<String>();
				for (ObjectId id : ((Set<ObjectId>) objectIds)) {
					ids.add(id.toString());
				}
				jsonGen.writeObject(Json.toJson(ids));
			} catch (Exception e) {
				List<String> ids = new ArrayList<String>();
				for (ObjectId id : ((List<ObjectId>) objectIds)) {
					ids.add(id.toString());
				}
				try {
				jsonGen.writeObject(Json.toJson(ids));
				} catch (Exception e1) {
				}
			}
		}

	}

	public static class DateSerializer extends JsonSerializer<Object> {
		@Override
		public void serialize(Object date, JsonGenerator jsonGen,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
			jsonGen.writeString(sdf.format((Date) date));
		}

	}

	public static class PointSerializer extends JsonSerializer<Object> {
		@Override
		public void serialize(Object point, JsonGenerator jsonGen,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode json = mapper.createObjectNode();
			json.put("latitude", ((Point) point).getLatitude());
			json.put("longitude", ((Point) point).getLongitude());
			jsonGen.writeObject(json);
		}

	}

	public static class MimeTypeSerializer extends JsonSerializer<Object> {

		@Override
		public void serialize(Object mimeType, JsonGenerator jsonGen,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
			try {
				//System.out.println(mimeType.toString());
				if (mimeType == null)
					jsonGen.writeString(MediaType.ANY_TYPE.toString());
				else
					jsonGen.writeString(((MediaType) mimeType).toString());
			} catch (Exception e) {
				jsonGen.writeString(MediaType.ANY_TYPE.toString());
			}
		}
	}

	public static class WithAccessSerializer extends JsonSerializer<Object> {
		@Override
		public void serialize(Object rights, JsonGenerator jsonGen,
				SerializerProvider arg2) throws IOException,
				JsonProcessingException {
			boolean isPublic = ((WithAccess) rights).getIsPublic();
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode json = mapper.createObjectNode();
			json.put("isPublic", isPublic);
			json.put("acl", Json.toJson(((WithAccess) rights).getAcl()));
			jsonGen.writeObject(json);
		}
	}

	public static class AccessEntrySerializer extends JsonSerializer<Object> {
		@Override
		public void serialize(Object ae, JsonGenerator jsonGen,
				SerializerProvider arg2) throws IOException,
				JsonProcessingException {
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode json = mapper.createObjectNode();
			json.put("level", ((AccessEntry) ae).getLevel().ordinal());
			json.put("user", ((AccessEntry) ae).getUser().toString());
			jsonGen.writeObject(json);
		}
	}

	public static class AccessMapSerializer extends JsonSerializer<Object> {
		@Override
		public void serialize(Object map, JsonGenerator jsonGen,
				SerializerProvider arg2) throws IOException,
				JsonProcessingException {
			Map<String, String> rights = new HashMap<String, String>();
			for (Entry<ObjectId, Access> e : ((Map<ObjectId, Access>) map)
					.entrySet()) {
				rights.put(e.getKey().toString(), e.getValue().toString());
			}
			jsonGen.writeObject(Json.toJson(rights));
		}

	}

	public static class LightUserSerializer extends JsonSerializer<Object> {

		@Override
		public void serialize(Object user, JsonGenerator jsonGen,
				SerializerProvider arg2) throws IOException,
				JsonProcessingException {
			User u = (User)user;
			ObjectNode json = DB.getCollectionObjectDAO().countMyAndSharedCollections(
					new ArrayList<ObjectId>() {{ add(u.getDbId()); addAll(u.getUserGroupsIds()); }});
			json.put("firstName", u.getFirstName());
			json.put("lastName", u.getLastName());
			if(u.getAvatar()!=null)
				json.put("avatar", Json.toJson(u.getAvatar()));
			if(u.getNotifications()!=null) {
				long pendingNots = u.getNotifications().stream().filter(n -> n.isPendingResponse()).count();
				json.put("pendingNotifications", pendingNots);
			}

			jsonGen.writeObject(json);
		}

	}

	public static class MUltiliteralSerializerForElastic extends JsonSerializer<Object> {

		@Override
		public void serialize(Object arg0, JsonGenerator arg1,
				SerializerProvider arg2) throws IOException,
				JsonProcessingException {

			MultiLiteral multi = (MultiLiteral)arg0;
			ObjectNode multiJson = Json.newObject();
			List<String> all_literals = new ArrayList<String>();

			for(Entry<String, List<String>> e: multi.entrySet()) {

				// accumulate to _all field all values except uri & default
				if(!e.getKey().equals(LiteralOrResource.URI) &&
						!e.getKey().equals(Language.DEFAULT.getDefaultCode()))
					all_literals.addAll(e.getValue());

				// add to json the languages except unknown
				if(!e.getKey().equals(Language.UNKNOWN.getDefaultCode()))
					multiJson.put(e.getKey(), Json.toJson(e.getValue()));
			}
			multiJson.put("_all", Json.toJson(all_literals));
			arg1.writeObject(multiJson);
		}

	}

	public static class WithAccessSerializerForElastic extends JsonSerializer<WithAccess> {

		@Override
		public void serialize(WithAccess arg0, JsonGenerator arg1,
				SerializerProvider arg2) throws IOException,
				JsonProcessingException {
			WithAccess wa = arg0;
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode json = mapper.createObjectNode();
			json.put("isPublic", wa.getIsPublic());
			ObjectNode access = mapper.createObjectNode();
			access.put("READ", Json.toJson(wa.getAcl().stream().filter( ae -> ae.getLevel()==Access.READ)
					.map(AccessEntry::getUser).map(ObjectId::toString).collect(Collectors.toList())));
			access.put("WRITE", Json.toJson(wa.getAcl().stream().filter( ae -> ae.getLevel()==Access.WRITE)
					.map(AccessEntry::getUser).map(ObjectId::toString).collect(Collectors.toList())));
			access.put("OWN", Json.toJson(wa.getAcl().stream().filter( ae -> ae.getLevel()==Access.OWN)
					.map(AccessEntry::getUser).map(ObjectId::toString).collect(Collectors.toList())));
			json.put("access", access);

			arg1.writeObject(json);
		}

	}

	public static class EmbeddeMediaSerializer extends JsonSerializer<EmbeddedMediaObject> {

		@Override
		public void serialize(EmbeddedMediaObject arg0, JsonGenerator arg1,
				SerializerProvider arg2) throws IOException,
				JsonProcessingException {
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode json = mapper.createObjectNode();
			SimpleModule module = new SimpleModule();
			module.addSerializer(MediaType.class, new Serializer.MimeTypeSerializer());
			mapper.registerModule(module);

			if(arg0.getType()!=null)
				json.put("type", Json.toJson(arg0.getType()));
			if(arg0.getWithRights()!=null)
				json.put("withRights", Json.toJson(arg0.getWithRights()));
			if(arg0.getQuality()!=null)
				json.put("quality", Json.toJson(arg0.getQuality()));
				json.put("mimeType", mapper.valueToTree(arg0.getMimeType()));

			arg1.writeObject(json);
		}

	}

	public static String serializeXML(Document doc) {
		DOMImplementationLS domImplementation = (DOMImplementationLS) doc
				.getImplementation();
		LSSerializer lsSerializer = domImplementation.createLSSerializer();
		return lsSerializer.writeToString(doc);
	}
}
