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
import java.util.Map;
import java.util.Map.Entry;

import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import model.Rights.Access;
import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

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

	public static class DateSerializer extends JsonSerializer<Object> {
		@Override
		public void serialize(Object date, JsonGenerator jsonGen,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
			jsonGen.writeString(sdf.format((Date)date));
		}

	}

	public static class CustomMapSerializer extends JsonSerializer<Object> {
		@Override
		public void serialize(Object map, JsonGenerator jsonGen,
				SerializerProvider arg2) throws IOException,
				JsonProcessingException {
			Map<String, String> rights = new HashMap<String, String>();
			for(Entry<ObjectId, Access> e: ((Map<ObjectId, Access>)map).entrySet()) {
				rights.put(e.getKey().toString(), e.getValue().toString());
			}
			jsonGen.writeObject(Json.toJson(rights));
		}

	}
	
	public static String serializeXML(Document doc) {
		DOMImplementationLS domImplementation = (DOMImplementationLS) doc
				.getImplementation();
		LSSerializer lsSerializer = domImplementation.createLSSerializer();
		return lsSerializer.writeToString(doc);
	}
}
