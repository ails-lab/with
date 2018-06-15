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


package search;

import org.apache.lucene.index.FilterLeafReader.FilterFields;

import model.basicDataTypes.Language;
import model.basicDataTypes.MultiLiteral;
import play.Logger;

public enum FiltersFields {
		/**
		 * also known as search term
		 */
		ANYWHERE(Fields.anywhere,"Anywhere"),
		TYPE(Fields.media_type,"Media Type"),
		PROVIDER(Fields.provenance_provider,"Provider"),
		CREATOR(Fields.descriptiveData_dccreator,"Creator"),
		RIGHTS(Fields.media_withRights,"Media Rights", true),
		COUNTRY(Fields.descriptiveData_country,"Spatial"),
		YEAR(Fields.descriptiveData_dcdate_year,"Dates"),
		CONTRIBUTOR("descriptiveData_dccontributor","Contributor"),
		DATA_PROVIDER(Fields.provenance_dataprovider,"Data Provider"),
		MIME_TYPE(Fields.media_mimeType,"Mime Type"),
		IMAGE_SIZE("IMAGE_SIZE","Image Size"),
		IMAGE_COLOUR("IMAGE_COLOUR","Image Color"),
		COLOURPALETE("COLOURPALETE","Color Palete")
		
		;

		private final MultiLiteral filterName;
		private final String filterId;
		private final boolean unique;
		
		
		private FiltersFields(Fields filterId, MultiLiteral filterName, boolean unique) {
			this(filterId.fieldId(),filterName, unique);
		}
		
		private FiltersFields(Fields filterId, MultiLiteral filterName) {
			this(filterId.fieldId(),filterName, false);
		}
		private FiltersFields(String filterId, MultiLiteral filterName, boolean unique) {
			this.filterId = filterId;
			this.filterName = filterName;
			this.unique = unique;
		}
		
		private FiltersFields(Fields filterId, String filterName, boolean unique) {
			this(filterId,new MultiLiteral(filterName).fillDEF(), unique);
		}
		
		private FiltersFields(Fields filterId, String filterName) {
			this(filterId,new MultiLiteral(filterName).fillDEF(), false);
		}
		
		private FiltersFields(String filterId, String filterName) {
			this(filterId,new MultiLiteral(filterName).fillDEF(), false);
		}
		public MultiLiteral getFilterName() {
			return filterName;
		}
		
		
		public String getFilterId() {
			return filterId;
		}
		
		public static FiltersFields getFilterFromID(String fieldID){
			for (FiltersFields f : FiltersFields.values()) {
				if (f.filterId.equals(fieldID))
					return f;
			}
			return null;
		}

		public boolean isUnique() {
			return unique;
		}

		
		

}
