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

import org.mongodb.morphia.converters.SimpleValueConverter;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;

import com.google.common.net.MediaType;

public class MediaTypeConverter extends TypeConverter implements SimpleValueConverter {


		public MediaTypeConverter() {
			super(MediaType.class);
		}

		@Override
		public Object encode(Object value, MappedField optionalExtraInfo) {
			if(value==null) {
				return null;
			}

			return ((MediaType)value).toString();
		}

		@Override
		public Object decode(Class targetClass, Object fromDBObject,
				MappedField optionalExtraInfo) {
			if (fromDBObject == null) {
	            return null;
	        }
	        return MediaType.parse((String)fromDBObject);
		}
}
