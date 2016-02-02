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
import java.util.Set;

import model.CollectionRecord;
import model.ExternalBasicRecord;
import model.Provider;
import model.resources.CulturalObject;
import model.resources.WithResource;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import utils.ListUtils;

import com.fasterxml.jackson.databind.JsonNode;

import db.DB;
import elastic.ElasticUtils;

public class SourceResponse {

	public String query;
	public int totalCount;
	public int startIndex;
	public int count;
	public ItemsGrouping items;
	public Map<String, List<?>> resourcesPerType;
	public String source;
	public JsonNode facets;
	public List<CommonFilterResponse> filters;
	public List<CommonFilterLogic> filtersLogic;

	public SourceResponse() {
		items = new ItemsGrouping();
		filters = new ArrayList<>();
	}
	//source refers to the external APIs and the WITH db
	//comesFrom in each record in the WITH db indicates where it was imported from, i.e. external APIs, Mint or UploadedByUser
	public SourceResponse(SearchResponse resp, int offset) {

		this.totalCount = (int) resp.getHits().getTotalHits();
		this.source = "WITHin";
		this.startIndex = offset;
		List<WithResource<?, ?>> items = new ArrayList<WithResource<?, ?>>();


//		TODO make it using the new model...
//for (CollectionRecord er : elasticrecords) {
//	ItemsResponse it = new ItemsResponse();
//	List<CollectionRecord> rs = DB.getCollectionRecordDAO().getByExternalId(er.getExternalId());
//	if (rs != null && !rs.isEmpty()) {
//		CollectionRecord r = rs.get(0);
//		it.comesFrom = r.getSource();
//		it.title = r.getTitle();
//		it.description = r.getDescription();
//		it.id = r.getDbId().toString();
//		if(r.getThumbnailUrl() != null)
//			it.thumb = Arrays.asList(r.getThumbnailUrl().toString());
//		if (r.getIsShownBy() != null)
//			it.fullresolution = Arrays.asList(r.getIsShownBy().toString());
//		it.url = new MyURL();
//		it.url.fromSourceAPI = r.getSourceUrl();
//		it.provider = r.getProvider();
//		it.externalId = r.getExternalId();
//		it.type = r.getType();
//		it.rights = r.getItemRights();
//		it.dataProvider = r.getDataProvider();
//		it.creator = r.getCreator();
//		it.year = new ArrayList<>(Arrays.asList(r.getYear()));
//	    it.tags = er.getTags();
//		items.add(it);
//	}
//}
		this.items.setCulturalCHO(items);
	}


	public Map<String, List<?>> getResourcesPerType() {
		return resourcesPerType;
	}
	public void setResourcesPerType(Map<String, List<?>> resourcesPerType) {
		this.resourcesPerType = resourcesPerType;
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
			res.filters = ListUtils.transform(res.filtersLogic, (CommonFilterLogic x) -> {
				return x.export();
			});

		}
		return res;
	}

	public void addItem(WithResource<?, ?> record) {
		if (record!=null){
		if (record instanceof CulturalObject)
			items.getCulturalCHO().add(record);
		}
	}

	@Override
	public String toString() {
		return "SourceResponse [source=" + source + ", query=" + query + ", totalCount=" + totalCount + "]";
	}

}
