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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import model.ExhibitionRecord;
import model.Rights.Access;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class Deserializer {


	public static class ExhibitionRecordDeserializer extends JsonDeserializer<ExhibitionRecord> {

		@Override
		public ExhibitionRecord deserialize(JsonParser annot,
				DeserializationContext arg1) throws IOException,
				JsonProcessingException {
			ExhibitionRecord exhRec = new ExhibitionRecord();
			exhRec.setAnnotation(annot.getValueAsString());
			return exhRec;
		}
	}

	public static class CustomMapDeserializer extends JsonDeserializer<Map<ObjectId, Access>> {

		@Override
		public Map<ObjectId, Access> deserialize(JsonParser rights_string, DeserializationContext arg1)
				throws IOException, JsonProcessingException {
			Map<String, Integer> rights_map = rights_string.readValueAs(new TypeReference<Map<String, Integer>>() {
			});
			Map<ObjectId, Access> r = new HashMap<ObjectId,	Access>();
			for(Entry<String, Integer> e : rights_map.entrySet()) {
				r.put(new ObjectId(e.getKey()), Access.values()[e.getValue()]);
			}

			return r;
		}

	}

	public static class DateDeserializer extends JsonDeserializer<Object> {

		@Override
		public Object deserialize(JsonParser date, DeserializationContext arg1)
				throws IOException, JsonProcessingException {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
			try {
				return sdf.parse(date.getValueAsString());
			} catch (ParseException e) {
				e.printStackTrace();
				return null;
			}
		}
	}
}
