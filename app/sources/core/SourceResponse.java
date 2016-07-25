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


package sources.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.elasticsearch.action.search.SearchResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import model.resources.CulturalObject;
import model.resources.RecordResource;
import model.resources.WithResource;
import search.Response;
import search.Response.SingleResponse;
import search.Response.ValueCounts;
import search.Sources;
import utils.ListUtils;

@JsonIgnoreProperties({"filtersLogic","resourcesPerType", "facets"})
public class SourceResponse {

	public String query;
	public int totalCount;
	public int startIndex;
	public int count;
	public ItemsGrouping items;
	public Map<String, List<?>> resourcesPerType;
	public String source;
	public JsonNode facets;
	private List<CommonFilterResponse> filters;
	public List<CommonFilterLogic> filtersLogic;

	public SourceResponse() {
		items = new ItemsGrouping();
		filters = new ArrayList<>();
	}

	public SourceResponse(int totalHits, int offset, int count) {
		items = new ItemsGrouping();
		filters = new ArrayList<>();
		this.totalCount = totalHits;
		this.startIndex = offset;
		this.count = count;
	}
	//source refers to the external APIs and the WITH db
	//comesFrom in each record in the WITH db indicates where it was imported from, i.e. external APIs, Mint or UploadedByUser
	public SourceResponse(SearchResponse resp, int offset) {
		this.totalCount = (int) resp.getHits().getTotalHits();
		this.source = "WITHin";
		this.startIndex = offset;
		List<WithResource<?, ?>> items = new ArrayList<WithResource<?, ?>>();

		this.items.setCulturalCHO(items);
	}

	public Map<String, List<?>> getResourcesPerType() {
		return resourcesPerType;
	}
	public void setResourcesPerType(Map<String, List<?>> resourcesPerType) {
		this.resourcesPerType = resourcesPerType;
	}
	
	public void setSource(){
		for (CommonFilterResponse f : filters) {
			f.addSource(Sources.getSourceByID(this.source));
		}
	}

	public List<CommonFilterResponse> getFilters() {
		return filters;
	}

	public void setFilters(List<CommonFilterResponse> filters) {
		this.filters = filters;
		setSource();
	}

	public SourceResponse merge(SourceResponse r2) {
		SourceResponse res = new SourceResponse();
		res.source = r2.source;
		res.query = query;
		res.count = count + r2.count;
		res.items = new ItemsGrouping();
		if (items != null)
			res.items.addAll(items);
		if (r2.items != null)
			res.items.addAll(r2.items);
		if ((filtersLogic != null) && (r2.filtersLogic != null)) {
			res.filtersLogic = filtersLogic;
			FiltersHelper.merge(res.filtersLogic, r2.filtersLogic);
			res.setFilters(ListUtils.transform(res.filtersLogic, (CommonFilterLogic x) -> {
				return x.export();
			}));
		}
		return res;
	}

	public void addItem(WithResource<?, ?> record) {
		if (record != null) {
			if (record instanceof CulturalObject)
				items.getCulturalCHO().add(record);
			else if (record instanceof RecordResource)
				items.getRecordResource().add(record);
		}
	}

	@Override
	public String toString() {
		return "SourceResponse [source=" + source + ", query=" + query + ", totalCount=" + totalCount + "]";
	}

	public void transformResourcesToItems() {
		List<WithResource<?, ?>> resources = new ArrayList<WithResource<?, ?>>();
		for (Entry<String, List<?>> e: resourcesPerType.entrySet())
			if (!e.getKey().equals("collectionobject"))
				resources.addAll((List<WithResource<?, ?>>) e.getValue());
		items.setCulturalCHO(resources);
	}

	public SingleResponse exportToSingleSource() {
		SingleResponse r = new SingleResponse();
		r.count = this.count;
		r.totalCount = this.totalCount;
		r.items = this.items.getAll();
		r.facets =new ValueCounts();
		r.source = Sources.valueOf(source);
		for (CommonFilterLogic f : filtersLogic) {
			CommonFilterResponse ff = f.export();
			r.facets.put( ff.filterID, ListUtils.transform(ff.suggestedValues, (ValueCount x)-> 
			new Response.ValueCount(x.value, x.count),0,SingleResponse.FACETS_LIMIT));
		}
		return r;
	}

}

