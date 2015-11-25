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


package model.basicDataTypes;

import java.util.HashMap;

import model.basicDataTypes.WithAccess.Access;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Converters;

import db.RightsConverter;

/**
 * 
 * @author Arne Stabenau
 *
 * So that Model objects can have a proper type for rights embedded
 */
@Converters( RightsConverter.class )
public class WithAccess extends HashMap<ObjectId, Access> {
	
	public static enum Access {
		NONE, READ, WRITE, OWN
	}
	private boolean isPublic;

	public boolean isPublic() {
		return isPublic;
	}

	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}
}
