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

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;

import model.DescriptiveData;
import model.basicDataTypes.MultiLiteralOrResource;

@Entity("CollectionObject")
public class CollectionObject extends WithResource<CollectionObject.CollectionDescriptiveData> {

	public CollectionObject() {
		super();
		this.administrative = new CollectionAdmin();
		this.resourceType = WithResourceType.valueOf(this.getClass()
				.getSimpleName());
	}

	public static class CollectionAdmin extends WithAdmin {

		private int entryCount = 0;
		private boolean isExhibition = false;

		public int getEntryCount() {
			return entryCount;
		}

		public void setEntryCount(int entryCount) {
			this.entryCount = entryCount;
		}

		public void incEntryCount() {
			this.entryCount++;
		}

		public boolean isExhibition() {
			return isExhibition;
		}

		public void setExhibition(boolean isExhibition) {
			this.isExhibition = isExhibition;
		}

	}

	@Embedded
	public static class CollectionDescriptiveData extends DescriptiveData {

		// start day or possible start days
		private MultiLiteralOrResource dccreator;
		// for whom the resource is intended or useful
		private MultiLiteralOrResource dctermsaudience;
		// additional views of the timespan
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

}
