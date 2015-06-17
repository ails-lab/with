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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import model.CollectionRecord;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;

public class SourceResponse {

	public SourceResponse() {
	}

	public SourceResponse(SearchResponse resp, String source, int offset) {
		List<CollectionRecord> elasticrecords = new ArrayList<CollectionRecord>();
		this.totalCount = (int) resp.getHits().getTotalHits();
		this.source = source;
		for (SearchHit hit : resp.getHits().hits()) {
			elasticrecords.add(hitToRecord(hit));
		}
		this.count = elasticrecords.size();
		this.startIndex = offset;
		List<ItemsResponse> items = new ArrayList<ItemsResponse>();
		for (CollectionRecord r : elasticrecords) {
			ItemsResponse it = new ItemsResponse();
			it.title = Arrays.asList(new Lang(null, r.getTitle()));
			it.description = Arrays.asList(new Lang(null, r.getDescription()));
			it.id = r.getSourceId();
			it.thumb = Arrays.asList(r.getThumbnailUrl().toString());
			it.url = new MyURL();
			it.url.fromSourceAPI = r.getSourceUrl();
			items.add(it);
		}
		this.items = items;
	}

	private CollectionRecord hitToRecord(SearchHit hit) {
		JsonNode json = Json.parse(hit.getSourceAsString());
		CollectionRecord record = Json.fromJson(json, CollectionRecord.class);
		return record;
	}

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
		public List<Lang> rights;
		public String externalId;
	}

	public String query;
	public int totalCount;
	public int startIndex;
	public int count;
	public List<ItemsResponse> items;
	public String source;
	public JsonNode facets;
	public List<CommonFilterResponse> filters;
	public List<CommonFilterLogic> filtersLogic;
}
