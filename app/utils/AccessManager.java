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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import model.User;
import model.User.Access;

import org.bson.types.ObjectId;

import play.Logger;
import play.Logger.ALogger;
import db.DB;

public class AccessManager {
	public static final ALogger log = Logger.of( AccessManager.class);

	public static enum Action {
		READ, EDIT, DELETE
	};

	public static final List<ObjectId> accessIds = new ArrayList<ObjectId>();

	public static void initialise(ObjectId userId) {
		accessIds.clear();
		addIds(userId);
	}

	public static void addIds(ObjectId id) {
		accessIds.add(id);
		User user = DB.getUserDAO().get(id);
		accessIds.addAll(user.getUserGroupsIds());
	}

	public static boolean checkAccess(Map<ObjectId, Access> rights, Action action) {
		for(ObjectId id: accessIds) {
			if(rights.containsKey(id) && (rights.get(id).ordinal() > action.ordinal()) ) {
				return true;
			}
		}
		return false;
	}

	public static void addRight(Map<ObjectId, Access> rights, Map<ObjectId, Access> rightsToGive) {
		rights.putAll(rightsToGive);
	}

	public static void removeRight(Map<ObjectId, Access> rights, Map<ObjectId, Access> rightToGo) {
		for(Entry<ObjectId, Access> e: rightToGo.entrySet())
			rights.remove(e.getKey(), e.getValue());
	}

}
