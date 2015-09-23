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

import model.Rights.Access;
import model.UserGroup;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import db.DB;

public class AccessManager {
	public static final ALogger log = Logger.of(AccessManager.class);

	public static enum Action {
		READ, EDIT, DELETE
	};

	public static boolean checkAccess(Map<ObjectId, Access> rights,
			List<String> userIds, Action action) {
		for (String id : userIds) {
			if (DB.getUserDAO()
					.getById(new ObjectId(id),
							new ArrayList<String>(Arrays.asList("superUser")))
					.isSuperUser()) {
				return true;
			}
			if (rights.containsKey(new ObjectId(id))
					&& (rights.get(new ObjectId(id)).ordinal() > action
							.ordinal())) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean checkAccessRecursively(Map<ObjectId, Access> rights,
			ObjectId groupId) {
		return false;
	}

	public static Access getMaxAccess(Map<ObjectId, Access> rights,
			List<String> userIds) {
		Access maxAccess = Access.NONE;
		for (String id : userIds) {
			if (DB.getUserDAO()
					.getById(new ObjectId(id),
							new ArrayList<String>(Arrays.asList("superUser")))
					.isSuperUser()) {
				return Access.OWN;
			}
			if (rights.containsKey(new ObjectId(id))) {
				Access access = rights.get(new ObjectId(id));
				if (access.ordinal() > maxAccess.ordinal()) {
					maxAccess = access;
				}
			}
		}
		return maxAccess;
	}

	/**
	 * This methods supposes we have all user ids and all userGroup ids
	 * (recursively obtained) for the user, in a comma-separated list.
	 * It then transforms the comma-separated in java.util.List
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

	public static String effectiveUserId(String effectiveUserIds) {
		if (effectiveUserIds == null)
			effectiveUserIds = "";
		String[] ids = effectiveUserIds.split(",");
		return ids[0];
	}

}
