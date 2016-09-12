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

import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

import play.Logger;
import play.Logger.ALogger;

@Converters(JacksonConverter.class)
public class JacksonConverter extends TypeConverter  {
	
	static ObjectMapper om =new ObjectMapper();
	public static final ALogger log = Logger.of( JacksonConverter.class);

	public JacksonConverter() {
		super(ObjectNode.class);
	}
	
	public Object decode(Class targetClass, Object fromDBObject,
			MappedField optionalExtraInfo) {
		try {
			if (fromDBObject == null) 
				return null;
			else {
				return om.readTree( fromDBObject.toString());
			}
		} catch( Exception e ) {
			log.error( "Mongo Jackson conversion issue", e );
			return null;
		}
	}
	
	public Object encode( Object resource,  MappedField optionalExtraInfo) {
		try {
			DBObject res = (DBObject) JSON.parse( om.writeValueAsString(resource));
			return res;
		} catch( Exception e ) {
			log.error( "Jackson Mongo conversion issue", e );
			return null;
		}
	}
}
