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
import java.util.Set;

import model.CollectionRecord;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import utils.ListUtils;

import com.fasterxml.jackson.databind.JsonNode;

import db.DB;
import elastic.ElasticUtils;

public class SourceResponse {

	public SourceResponse() {
		items = new ArrayList<>();
		filters = new ArrayList<>();
	}

	public SourceResponse(SearchResponse resp, int offset) {
		List<CollectionRecord> elasticrecords = new ArrayList<CollectionRecord>();
		this.totalCount = (int) resp.getHits().getTotalHits();
		for (SearchHit hit : resp.getHits().hits()) {
			elasticrecords.add(ElasticUtils.hitToRecord(hit));
		}
		//source refers to the external APIs and the WITH db
		//comesFrom in each record in the WITH db indicates where it was imported from, i.e. external APIs, Mint or UploadedByUser
		this.source = "WITHin";
		this.count = elasticrecords.size();
		this.startIndex = offset;
		List<ItemsResponse> items = new ArrayList<ItemsResponse>();
		for (CollectionRecord er : elasticrecords) {
			ItemsResponse it = new ItemsResponse();
			List<CollectionRecord> rs = DB.getCollectionRecordDAO().getByExternalId(er.getExternalId());
			if (rs != null && !rs.isEmpty()) {
				CollectionRecord r = rs.get(0);
				it.comesFrom = r.getSource();
				it.title = r.getTitle();
				it.description = r.getDescription();
				it.id = r.getSourceId();
				if(r.getThumbnailUrl() != null)
					it.thumb = Arrays.asList(r.getThumbnailUrl().toString());
				if (r.getIsShownBy() != null)
					it.fullresolution = Arrays.asList(r.getIsShownBy().toString());
				it.url = new MyURL();
				it.url.fromSourceAPI = r.getSourceUrl();
				it.provider = r.getProvider();
				it.externalId = r.getExternalId();
				it.type = r.getType();
				it.rights = r.getItemRights();
				it.dataProvider = r.getDataProvider();
				it.creator = r.getCreator();
				it.year = new ArrayList<>(Arrays.asList(r.getYear()));
			    it.tags = er.getTags();
				items.add(it);
			}
		}
		this.items = items;
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
		public String type;
		public List<String> thumb;
		public String title;
		public String description;
		public String creator;
		public List<String> year;
		public String dataProvider;
		public String provider;
		public MyURL url;
		public List<String> fullresolution;
		public String comesFrom;
		public String rights;
		public String externalId;
		public Set<String> tags;
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

	public SourceResponse merge(SourceResponse r2) {
		SourceResponse res = new SourceResponse();
		res.source = r2.source;
		res.query = query;
		res.count = count + r2.count;
		res.items = new ArrayList<>();
		if (items != null)
			res.items.addAll(items);
		if (r2.items != null)
			res.items.addAll(r2.items);
		if ((filtersLogic != null) && (r2.filtersLogic != null)) {
			res.filtersLogic = filtersLogic;
			FiltersHelper.merge(res.filtersLogic, r2.filtersLogic);
			res.filters = ListUtils.transform(res.filtersLogic, (CommonFilterLogic x) -> {
				return x.export();
			});

		}
		return res;
	}
}
