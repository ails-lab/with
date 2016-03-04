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


package model.resources;

import java.util.ArrayList;
import java.util.List;

import model.annotations.ContextData;
import model.annotations.ContextData.ContextDataBody;
import model.annotations.ContextData.ContextDataType;

import org.bson.types.ObjectId;

import db.DB;

public class RecordResourceExtended extends
		RecordResource<RecordResource.RecordDescriptiveData> {

	public List<ObjectId> collectedIn;
	public List<ContextData> contextData;

	public void addExtendedInformation() {
		this.collectedIn = new ArrayList();
		this.contextData = new ArrayList();
		List<CollectionObject> collections = DB.getCollectionObjectDAO()
				.getByCollectedResource(getDbId(), null);
		for (CollectionObject c : collections) {
			this.collectedIn.add(c.getDbId());
			for (ContextData<ContextDataBody> contextData : c
					.getCollectedResources()) {
				if (contextData.getResourceId().equals(this.getDbId())
						&& !contextData.getContextDataType().equals(
								ContextDataType.None))
					this.contextData.add(new ContextData(c.getDbId(),
							contextData.getContextDataType()));
			}
		}

	}

}
