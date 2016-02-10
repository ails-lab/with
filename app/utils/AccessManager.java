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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import model.usersAndGroups.User;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Converters;

import db.DB;
import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;
import play.Logger;
import play.Logger.ALogger;

public class AccessManager {
	public static final ALogger log = Logger.of(AccessManager.class);

	public static enum Action {
		READ, EDIT, DELETE
	};
	
	public static boolean isSuperUser(String userId) {
		 return (DB.getUserDAO().isSuperUser(new ObjectId(userId)));
	}

	public static boolean hasAccessToRecordResource(String userIds, Action action, ObjectId resourceId) {
		return DB.getRecordResourceDAO().hasAccess(effectiveUserDbIds(userIds), action, resourceId);
	}
	
	public static boolean hasAccessToCollectionResource(String userIds, Action action, ObjectId resourceId) {
		return DB.getCollectionObjectDAO().hasAccess(effectiveUserDbIds(userIds), action, resourceId);
	}
	
	public static List<ObjectId> toObjectIds(List<String> userIds) {
		List<ObjectId> objectIds = new ArrayList<ObjectId>();
		for (String userId: userIds) {
			objectIds.add(new ObjectId(userId));
		}
		return objectIds;
	}

	public static boolean checkAccessRecursively(Map<ObjectId, Access> rights,
			ObjectId groupId) {
		return false;
	}

	public static Access getMaxAccess(WithAccess rights,
			List<String> userIds) {
		Access maxAccess = Access.NONE;
		for (String id : userIds) {
			User user = DB.getUserDAO().getById(new ObjectId(id),
					new ArrayList<String>(Arrays.asList("superUser")));
			if (user != null) {
			  if (user.isSuperUser())
				  return Access.OWN;
			  else if (rights.containsUser(new ObjectId(id))) {
					Access access = rights.getAcl(new ObjectId(id));
					if (access.ordinal() > maxAccess.ordinal())
						maxAccess = access;
			  }
			}
		}
		return maxAccess;
	}

	/*public static boolean increasedAccess(Access before, Access after) {
		if (before == null) {
			if (after == null) {
				return false;
			} else {
				return true;
			}
		}
		return (after.ordinal() > before.ordinal());
	}*/

	/**
	 * This methods supposes we have all user ids and all userGroup ids
	 * (recursively obtained) for the user, in a comma-separated list. It then
	 * transforms the comma-separated in java.util.List
	 * 
	 * @param effectiveUserIds
	 * @return
	 */
	public static List<String> effectiveUserIds(String effectiveUserIds) {
		if (effectiveUserIds == null)
			effectiveUserIds = "";
		List<String> userIds = new ArrayList<String>();
		for (String ui : effectiveUserIds.split(",")) {
			if (ui.trim().length() > 0)
				userIds.add(ui);
		}
		return userIds;
	}

	public static List<ObjectId> effectiveUserDbIds(String effectiveUserIds) {
		if (effectiveUserIds == null)
			effectiveUserIds = "";
		List<ObjectId> userIds = new ArrayList<ObjectId>();
		for (String ui : effectiveUserIds.split(",")) {
			if (ui.trim().length() > 0)
				userIds.add(new ObjectId(ui.trim()));
		}
		return userIds;
	}

	public static String effectiveUserId(String effectiveUserIds) {
		if (effectiveUserIds == null)
			effectiveUserIds = "";
		String[] ids = effectiveUserIds.split(",");
		return ids[0];
	}

}
