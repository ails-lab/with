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


package notifications;

import model.usersAndGroups.UserGroup;

import org.bson.types.ObjectId;

import utils.Serializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import db.DB;

public class GroupNotification extends Notification {
	
	// The group that is involved with the action (if group related)
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId group;
	
	public ObjectId getGroup() {
		return group;
	}

	public void setGroup(ObjectId group) {
		this.group = group;
	}

	public String getGroupName() {
		if (this.group == null) {
			return null;
		}
		UserGroup gr = DB.getUserGroupDAO().get(this.group);
		if (gr != null) {
			return gr.getFriendlyName();
		}
		return "DELETED";
	}

}
