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
import java.util.List;

import model.CollectionRecord;
import model.ExternalBasicRecord;
import model.Provider;
import model.resources.WithResource;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import utils.ListUtils;

import com.fasterxml.jackson.databind.JsonNode;

import elastic.ElasticUtils;

public class SourceResponse {

	public String query;
	public int totalCount;
	public int startIndex;
	public int count;
	public List<WithResource<?>> items;
	public String source;
	public JsonNode facets;
	public List<CommonFilterResponse> filters;
	public List<CommonFilterLogic> filtersLogic;

	public SourceResponse() {
		items = new ArrayList<>();
		filters = new ArrayList<>();
	}

	public SourceResponse(SearchResponse resp, int offset) {
		List<WithResource> elasticrecords = new ArrayList<WithResource>();
		this.totalCount = (int) resp.getHits().getTotalHits();
		for (SearchHit hit : resp.getHits().hits()) {
			elasticrecords.add(ElasticUtils.hitToRecord(hit));
		}
		//source refers to the external APIs and the WITH db
		//comesFrom in each record in the WITH db indicates where it was imported from, i.e. external APIs, Mint or UploadedByUser
		this.source = "WITHin";
		this.count = elasticrecords.size();
		this.startIndex = offset;
		List<WithResource<?>> items = new ArrayList<>();
		
//		TODO make it using the new model...
//		for (CollectionRecord r : elasticrecords) {
//			ExternalBasicRecord item = new ExternalBasicRecord();
//			Provider provider = new Provider(source);
//			provider.providerName = source;
//			item.addProvider(provider);
//			item.setTitle(r.getTitle());
//			item.setDescription(r.getDescription());
//			item.setThumbnailUrl(r.getThumbnailUrl().toString());
//			items.add(item);
//		}
		this.items = items;
	}

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

	public void addItem(WithResource<?> record) {
		if (record!=null)
			items.add(record);
	}
}
