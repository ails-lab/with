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


package db.converters;

import model.basicDataTypes.ProvenanceInfo;

import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;

import com.mongodb.DBObject;

public class ProvenanceInfoConverter extends TypeConverter{

		public ProvenanceInfoConverter() {
			super(ProvenanceInfo.class);
		}

		@Override
		public Object decode(Class<?> arg0, Object fromDbObject, MappedField arg2) {
			DBObject dbObj = (DBObject) fromDbObject;
			String provider = (String) dbObj.get("provider");
			String uri = "";
			String recordId = "";
		    if (dbObj.containsField("uri"))
		    	uri = (String) dbObj.get("uri");
		    if (dbObj.containsField("recordId"))
		    	recordId = (String) dbObj.get("recordId");
			ProvenanceInfo p = new ProvenanceInfo(provider);
			if (!uri.isEmpty())
				p.setUri(uri);
			if (!recordId.isEmpty())
				p.setResourceId(recordId);
			return p;
		}
}
