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
import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;

import db.converters.AccessEnumConverter;
import utils.Serializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 *
 * @author Arne Stabenau
 *
 * So that Model objects can have a proper type for rights embedded
 */
public class WithAccess  {

	@Converters(AccessEnumConverter.class)
	public static class AccessEntry {

		public AccessEntry() {}

		public AccessEntry(ObjectId user, Access level) {
			this.user = user;
			this.level = level;
		}

		@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
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

	public boolean getIsPublic() {
		return isPublic;
	}

	public void setIsPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	public List<AccessEntry> getAcl() {
		if (acl==null)
			acl = new ArrayList<WithAccess.AccessEntry>();
		return acl;
	}

	public void addToAcl(AccessEntry accessEntry) {
		addToAcl(accessEntry.getUser(), accessEntry.getLevel());
	}

	public void addToAcl(ObjectId userId, Access access) {
		AccessEntry accessEntry = new AccessEntry(userId, access);
		for (AccessEntry entry: acl)
			if (entry.getUser().equals(userId)) {
				entry.setLevel(access);
				return;
			}
		this.acl.add(accessEntry);
	}

	public void removeFromAcl(ObjectId userId) {
		for (int i=0; i<this.acl.size(); i++) {
			if (acl.get(i).getUser().equals(userId)) {
				acl.remove(i);
				return;
			}
		}
	}
	
	/**
	 * Find maximum access level for given user.
	 * @param userId
	 * @return
	 */
	public Access accessByUser( ObjectId userId ) {
		Access res = Access.NONE;
		for(AccessEntry ae:acl ) {
			if( ae.user.equals(userId ))
				if( ae.level.ordinal() > res.ordinal())
					res = ae.level;	
		}
		return res;
	}
	
	/**
	 * Find maximum access level for given users (usually effectiveIds).
	 * @param userId
	 * @return
	 */
	public Access accessByUsers( ObjectId... users ) {
		Access res = Access.NONE;
		for(AccessEntry ae:acl ) {
			for( ObjectId id: users) 
				if( ae.user.equals(id ))
				if( ae.level.ordinal() > res.ordinal())
					res = ae.level;	
		}
		return res;		
	}
	
	public boolean canRead( ObjectId... users ) {
		return ( accessByUsers( users ) != Access.NONE ); 
	}
	
	public boolean canDelete( ObjectId... users ) {
		return ( accessByUsers( users ) == Access.OWN ); 
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

	public boolean containsUser(ObjectId userId) {
		for (AccessEntry entry: acl) {
			if (entry.getUser().equals(userId))
				return true;
		}
		return false;
	};

}
