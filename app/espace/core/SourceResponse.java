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

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public class SourceResponse {
	public static class Lang {
		public Lang(String languageCode, String textValue) {
			this.lang = languageCode;
			this.value = textValue;
		}

		public Lang() {
			super();
		}

		public String lang;
		public String value;
	}

	public static class MyURL {
		public List<String> original;
		public String fromSourceAPI;
	}

	public static class ItemsResponse {
		public String id;
		public List<String> thumb;
		public List<Lang> title;
		public List<Lang> description;
		public List<Lang> creator;
		public List<String> year;
		public List<Lang> dataProvider;
		public MyURL url;
		public List<String> fullresolution;
	}

	public String query;
	public int totalCount;
	public int startIndex;
	public int count;
	public List<ItemsResponse> items;
	public String source;
	public JsonNode facets;
	public List<CommonFilterLogic> filters;
}
