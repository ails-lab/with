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
import search.Response.ValueCount;

import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;

import com.mongodb.DBObject;

public class ValueCountConverter extends TypeConverter{

		public ValueCountConverter() {
			super(ProvenanceInfo.class);
		}

		@Override
		public Object decode(Class<?> arg0, Object fromDbObject, MappedField arg2) {
			DBObject dbObj = (DBObject) fromDbObject;
			String value = "";
			int count = 0;
		    if (dbObj.containsField("value"))
		    	value = (String) dbObj.get("value");
		    if (dbObj.containsField("count"))
		    	count = Integer.parseInt((String) dbObj.get("count"));
			ValueCount p = new ValueCount();
			if (!value.isEmpty()) {
				p.value = value;
				p.count = count;
			}
			return p;
		}
}
