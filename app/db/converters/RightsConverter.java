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

import model.basicDataTypes.WithAccess;

import org.bson.types.ObjectId;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;


public class RightsConverter extends TypeConverter {//implements SimpleValueConverter {

	public RightsConverter() {
		super( WithAccess.class );
	}

	@Override
	public Object decode(Class<?> arg0, Object fromDbObject, MappedField arg2) {
		return arg2;
		/*WithAccess r = new WithAccess();
		DBObject dbObj = (DBObject) fromDbObject;
		for(String k: dbObj.keySet() ) {
			if( k.equals("isPublic"))
				r.setPublic((Boolean) dbObj.get("isPublic"));
			else
				r.put(new ObjectId(k),
					WithAccess.Access.values()[(int) dbObj.get(k)]);
		}
		return r;*/
	}

	@Override
    public Object encode(final Object value, final MappedField optionalExtraInfo) {
		return optionalExtraInfo;
    	/*WithAccess r = (WithAccess) value;
    	BasicDBObject dbObj = new BasicDBObject();
    	dbObj.put("isPublic", r.isPublic());
        return dbObj;*/
    }

}
