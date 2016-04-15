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
import java.util.Map;

import org.bson.types.ObjectId;

import model.DescriptiveData;
import model.annotations.ContextData;


public class RecordResource<T extends RecordResource.RecordDescriptiveData>
	extends WithResource<T, RecordResource.RecordAdmin> {

	public RecordResource() {
		super();
		this.administrative = new RecordAdmin();
	}

	public RecordResource(ObjectId id) {
		this.administrative = new RecordAdmin();
		this.setDbId(id);
	}

	public static class RecordDescriptiveData extends DescriptiveData {

	}

	public static class RecordAdmin extends WithResource.WithAdmin {

		// if this resource / record is derived (modified) from a different Record.
		private ObjectId parentResourceId;

		public ObjectId getParentResourceId() {
			return parentResourceId;
		}

		public void setParentResourceId(ObjectId parentResourceId) {
			this.parentResourceId = parentResourceId;
		}

	}
	/*
	 * Elastic transformations
	 */

	/*
	 * Currently we are indexing only Resources that represent
	 * collected records
	 */
	public Map<String, Object> transform() {
		Map<String, Object> idx_map =  this.transformWR();

		List<ObjectId> colIn = new ArrayList<ObjectId>();
		this.getCollectedIn().forEach( (ci) -> {colIn.add(ci);} );

		idx_map.put("collectedId", colIn);

		return idx_map;
	}
}