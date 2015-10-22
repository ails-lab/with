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


package espace.core;

public enum CommonFilters {
		TYPE("Type"), PROVIDER("Provider"), CREATOR("Creator"), RIGHTS("Content Usage"),
		COUNTRY("Country"), YEAR("Year"), CONTRIBUTOR("Contributor"), DATA_PROVIDER("Data Provider");
		
		private final String text;
		private final String id;

	    private CommonFilters(final String text) {
	        this.text = text;
	        this.id = this.name();
	    }

	    private CommonFilters(String text, String id) {
			this.text = text;
			this.id = id;
		}
	    
		public String getID() {
			return id;
		}

		@Override
	    public String toString() {
	        return text;
	    };
	
	/*
	//TODO: remove duplication
	public static final String TYPE_NAME = "Type";
	public static final String PROVIDER_ID = "provider";
	public static final String PROVIDER_NAME = "Provider";
	public static final String CREATOR_ID = "creator";
	public static final String CREATOR_NAME = "Creator";
	public static final String RIGHTS_ID = "rights";

	public static final String RIGHTS_NAME = "Content Usage";
	public static final String COUNTRY_ID = "country";
	public static final String COUNTRY_NAME = "Country";
	public static final String YEAR_NAME = "Year";
	public static final String YEAR_ID = "year";
	public static final String CONTRIBUTOR_ID = "contributor";
	public static final String CONTRIBUTOR_NAME = "Contributor";
	public static final String DATAPROVIDER_ID = "data_provider";
	public static final String DATAPROVIDER_NAME = "Data Provider";

	public static final String AVAILABILITY_ID = "availability";
	public static final String AVAILABILITY_NAME = "Availability";

	public static final String REUSABILITY_ID = "reusability";
	public static final String REUSABILITY_NAME = "Reusability";*/
}
