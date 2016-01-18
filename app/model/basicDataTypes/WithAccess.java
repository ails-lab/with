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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import model.basicDataTypes.WithAccess.Access;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;

import db.converters.RightsConverter;

/**
 *
 * @author Arne Stabenau
 *
 * So that Model objects can have a proper type for rights embedded
 */
@Converters( RightsConverter.class )
public class WithAccess extends HashMap<ObjectId, Access> {

	@Indexes({
		@Index(fields = @Field(value = "user", type = IndexType.ASC), options = @IndexOptions(disableValidation=true)),
		@Index(fields = @Field(value = "level", type = IndexType.ASC), options = @IndexOptions()),
		@Index(fields = {@Field(value = "user", type = IndexType.ASC), @Field(value = "level", type = IndexType.DESC) }, options = @IndexOptions())
	})
	public static class AccessEntry {

		public AccessEntry(ObjectId user, Access level) {
			this.user = user;
			this.level = level;
		}

		private ObjectId user;
		private Access level;


		public ObjectId getUser() {
			return user;
		}
		public void setUser(ObjectId user) {
			this.user = user;
		}

		public Access getLevel() {
			return level;
		}
		public void setLevel(Access level) {
			this.level = level;
		}
	}

	public static enum Access {
		NONE, READ, WRITE, OWN
	}


	/*
	 * WithAccess class declarations
	 */

	private boolean isPublic;
	private List<AccessEntry> acl = new ArrayList<AccessEntry>();

	public boolean isPublic() {
		return isPublic;
	}

	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	public List<AccessEntry> getAcl() {
		return acl;
	}

	public void addAccess(AccessEntry accessEntry) {
		this.acl.add(accessEntry);
	}
	
	public void addAccess(ObjectId userId, Access access) {
		AccessEntry accessEntry = new AccessEntry(userId, access);
		this.acl.add(accessEntry);
	}
	
	public void setAcl(List<AccessEntry> acl) {
		this.acl = acl;
	}
	
	public Access getAcl(ObjectId user) {
		for (AccessEntry ae: acl) {
			if (ae.user.equals(user))
				return ae.level;
		}
		return Access.NONE;
	}
}
