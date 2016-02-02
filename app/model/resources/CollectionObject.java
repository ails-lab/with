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

import java.util.Map;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import model.DescriptiveData;
import model.basicDataTypes.MultiLiteralOrResource;

@Entity("CollectionObject")
public class CollectionObject extends WithResource<CollectionObject.CollectionDescriptiveData, CollectionObject.CollectionAdmin> {

	public CollectionObject() {
		super();
		this.administrative = new CollectionAdmin();
		this.resourceType = WithResourceType.valueOf(this.getClass()
				.getSimpleName());
	}


	@Embedded
	public static class CollectionAdmin extends WithResource.WithAdmin {

		public enum CollectionType {SimpleCollection, Exhibition};

		private int entryCount = 0;
		private CollectionType collectionType = CollectionType.SimpleCollection;

		public int getEntryCount() {
			return entryCount;
		}

		public void setEntryCount(int entryCount) {
			this.entryCount = entryCount;
		}

		public void incEntryCount() {
			this.entryCount++;
		}

		public CollectionType getCollectionType() {
			return collectionType;
		}

		public void setCollectionType(CollectionType collectionType) {
			this.collectionType = collectionType;
		}

	}

	@Embedded
	public static class CollectionDescriptiveData extends DescriptiveData {
		//TODO: change these to camelCase!
		// start day or possible start days
		private MultiLiteralOrResource dccreator;
		// for whom the resource is intended or useful
		private MultiLiteralOrResource dctermsaudience;
		// additional views of the timespan?
		private MultiLiteralOrResource dclanguage;

		// TODO: add link to external collection
		public MultiLiteralOrResource getDccreator() {
			return dccreator;
		}

		public void setDccreator(MultiLiteralOrResource dccreator) {
			this.dccreator = dccreator;
		}

		public MultiLiteralOrResource getDctermsaudience() {
			return dctermsaudience;
		}

		public void setDctermsaudience(
				MultiLiteralOrResource dctermsaudience) {
			this.dctermsaudience = dctermsaudience;
		}

		public MultiLiteralOrResource getDclanguage() {
			return dclanguage;
		}

		public void setDclanguage(MultiLiteralOrResource dclanguage) {
			this.dclanguage = dclanguage;
		}

	}


	/*
	 * Elastic transformations
	 */

	/*
	 * Currently we are indexing only Resources that represent
	 * collected records
	 */
	public Map<String, Object> transformCO() {
		Map<String, Object> idx_map =  this.transformWR();
		idx_map.put("collectionType", this.getAdministrative().getCollectionType());
		return idx_map;
	}

}
