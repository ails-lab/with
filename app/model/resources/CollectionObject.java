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

import model.basicDataTypes.LiteralOrResource;
import model.DescriptiveData;

public class CollectionObject extends WithResource<CollectionObject.CollectionDescriptiveData> {

	public static class CollectionAdmin extends WithAdmin {

		private int entryCount;
		private boolean isExhibition;


		public int getEntryCount() {
			return entryCount;
		}
		public void setEntryCount(int entryCount) {
			this.entryCount = entryCount;
		}
		public boolean isExhibition() {
			return isExhibition;
		}
		public void setExhibition(boolean isExhibition) {
			this.isExhibition = isExhibition;
		}

	}

	public static class CollectionDescriptiveData extends DescriptiveData {

		//start day or possible start days
		private ArrayList<LiteralOrResource> dccreator;
		//for whom the resource is intended or useful
		private ArrayList<LiteralOrResource> dctermsaudience;
		//additional views of the timespan
		private ArrayList<LiteralOrResource> dclanguage;


		//TODO: add link to external collection
		public ArrayList<LiteralOrResource> getDccreator() {
			return dccreator;
		}
		public void setDccreator(ArrayList<LiteralOrResource> dccreator) {
			this.dccreator = dccreator;
		}
		public ArrayList<LiteralOrResource> getDctermsaudience() {
			return dctermsaudience;
		}
		public void setDctermsaudience(ArrayList<LiteralOrResource> dctermsaudience) {
			this.dctermsaudience = dctermsaudience;
		}
		public ArrayList<LiteralOrResource> getDclanguage() {
			return dclanguage;
		}
		public void setDclanguage(ArrayList<LiteralOrResource> dclanguage) {
			this.dclanguage = dclanguage;
		}

	}



}
