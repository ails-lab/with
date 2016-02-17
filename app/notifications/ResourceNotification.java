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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import model.basicDataTypes.Language;
import model.basicDataTypes.WithAccess.Access;
import model.resources.WithResource;
import model.usersAndGroups.User;
import model.usersAndGroups.UserGroup;

import org.bson.types.ObjectId;

import utils.Serializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import db.DB;

public class ResourceNotification extends Notification {
	
	// The resource related with the action
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId resource;

	public static class ShareInfo {
		//the userOrGroup the resource is shared with
		private ObjectId userOrGroup;
		private Access newAccess;
		//effectiveIds of the owner of the resource - the one who does the sharing
		private List<ObjectId> ownerEffectiveIds;
		
		public Access getNewAccess() {
			return newAccess;
		}
		public void setNewAccess(Access newAccess) {
			this.newAccess = newAccess;
		}
		public List<ObjectId> getOwnerEffectiveIds() {
			return ownerEffectiveIds;
		}
		public void setOwnerEffectiveIds(List<ObjectId> ownerEffectiveIds) {
			this.ownerEffectiveIds = ownerEffectiveIds;
		}
		public ObjectId getUserOrGroup() {
			return userOrGroup;
		}
		public void setUserOrGroup(ObjectId userOrGroup) {
			this.userOrGroup = userOrGroup;
		}
		
		public Boolean sharedWithGroup() {
			if (userOrGroup != null) {
				if (DB.getUserDAO().existsEntity(userOrGroup)) 
					return false;
				else if (DB.getUserGroupDAO().existsEntity(userOrGroup))
					return true;
			}
			return null;
		}
		
		public String getUserOrGroupName() {
			String username = "";
			User user = DB.getUserDAO().getById(userOrGroup, new ArrayList<String>(Arrays.asList("username")));
			if (user != null)
				username = user.getUsername();
			else {
				UserGroup userGroup =  DB.getUserGroupDAO().getById(userOrGroup, new ArrayList<String>(Arrays.asList("username")));
				username = userGroup.getUsername();
			}
			return username;
		}
	
	}
	
	private ShareInfo shareInfo;
	
	public ObjectId getResource() {
		return resource;
	}

	public void setResource(ObjectId resource) {
		this.resource = resource;
	}

	public String getResourceName() {
		if (this.resource == null) {
			return null;
		}
		WithResource withResource = DB.getCollectionObjectDAO().getById(resource);
		if (withResource == null)
			withResource = DB.getRecordResourceDAO().getById(resource);
		if (withResource != null)
			return withResource.getDescriptiveData().getLabel().get(Language.DEFAULT).get(0);
		else
			return "DELETED";
	}

	public ShareInfo getShareInfo() {
		return shareInfo;
	}

	public void setShareInfo(ShareInfo shareInfo) {
		this.shareInfo = shareInfo;
	}
	
	

}
