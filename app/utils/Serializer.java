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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bson.types.ObjectId;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import model.WithAccess;
import model.WithAccess.Access;


import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;



public class Serializer {
	public static final ALogger log = Logger.of(Serializer.class);

	public static class ObjectIdSerializer extends JsonSerializer<Object> {
		@Override
		public void serialize(Object oid, JsonGenerator jsonGen, SerializerProvider provider)
				throws IOException, JsonProcessingException {
			jsonGen.writeString(oid.toString());
		}

	}

	public static class ObjectIdArraySerializer extends JsonSerializer<Object> {
		@Override
		public void serialize(Object objectIds, JsonGenerator jsonGen, SerializerProvider provider)
				throws IOException, JsonProcessingException {
			HashSet<String> ids = new HashSet<String>();
			for (ObjectId e : ((Set<ObjectId>) objectIds)) {
				ids.add(e.toString());
			}
			jsonGen.writeObject(Json.toJson(ids));
		}

	}

	public static class DateSerializer extends JsonSerializer<Object> {
		@Override
		public void serialize(Object date, JsonGenerator jsonGen, SerializerProvider provider)
				throws IOException, JsonProcessingException {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
			jsonGen.writeString(sdf.format((Date) date));
		}

	}
	
	public static class RightsSerializer extends JsonSerializer<Object> {
		@Override
		public void serialize(Object rights, JsonGenerator jsonGen,
				SerializerProvider arg2) throws IOException,
				JsonProcessingException {
			boolean isPublic = ((WithAccess) rights).isPublic();
			Map<String, Integer> rightsMap = new HashMap<String, Integer>();
			for(Entry<ObjectId, Access> e: ((Map<ObjectId, Access>)rights).entrySet()) {
				rightsMap.put(e.getKey().toString(), e.getValue().ordinal());
			}
		    ObjectMapper mapper = new ObjectMapper();
			ObjectNode json = mapper.createObjectNode();
			json.put("isPublic", isPublic);
			json.put("rights", Json.toJson(rightsMap));	 
			jsonGen.writeObject(json);
		}
	}

	public static class CustomMapSerializer extends JsonSerializer<Object> {
		@Override
		public void serialize(Object map, JsonGenerator jsonGen, SerializerProvider arg2)
				throws IOException, JsonProcessingException {
			Map<String, Integer> rights = new HashMap<String, Integer>();
			for (Entry<ObjectId, Access> e : ((Map<ObjectId, Access>) map).entrySet()) {
				rights.put(e.getKey().toString(), e.getValue().ordinal());
			}
			jsonGen.writeObject(Json.toJson(rights));
		}

	}

	public static String serializeXML(Document doc) {
		DOMImplementationLS domImplementation = (DOMImplementationLS) doc.getImplementation();
		LSSerializer lsSerializer = domImplementation.createLSSerializer();
		return lsSerializer.writeToString(doc);
	}
}
