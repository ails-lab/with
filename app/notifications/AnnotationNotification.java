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

public class AnnotationNotification extends Notification {

	// The resource related with the action
	@JsonSerialize(using = Serializer.ObjectIdSerializer.class)
	private ObjectId resource;

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
		WithResource withResource = DB.getCollectionObjectDAO().getById(resource, new ArrayList<String>() {{ add("descriptiveData.label"); }});
		if (withResource == null)
			withResource = DB.getRecordResourceDAO().getById(resource, new ArrayList<String>() {{ add("descriptiveData.label"); }});
		if (withResource != null)
			return withResource.getDescriptiveData().getLabel().get(Language.DEFAULT).get(0);
		else
			return "";
	}

}
