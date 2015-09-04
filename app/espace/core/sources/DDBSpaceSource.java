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


package espace.core.sources;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;

import espace.core.CommonFilterLogic;
import espace.core.CommonQuery;
import espace.core.HttpConnector;
import espace.core.ISpaceSource;
import espace.core.QueryBuilder;
import espace.core.SourceResponse;
import espace.core.Utils;
import espace.core.SourceResponse.ItemsResponse;
import espace.core.SourceResponse.MyURL;
import play.libs.Json;
import utils.ListUtils;

public class DDBSpaceSource extends ISpaceSource {

	public static final String LABEL = "DDB";
	private String key = "SECRET_KEY";

	@Override
	public String getSourceName() {
		return LABEL;
	}

	@Override
	public QueryBuilder getBuilder(CommonQuery q) {
		QueryBuilder builder = super.getBuilder(q);
		builder.setBaseUrl("http://api.deutsche-digitale-bibliothek.de/search?");
		builder.addSearchParam("oauth_consumer_key", key);
		builder.addQuery("query", q.searchTerm);
		builder.addSearchParam("rows", q.pageSize);
		builder.addSearchParam("offset", "" + ((Integer.parseInt(q.page) - 1) * Integer.parseInt(q.pageSize)));
		builder.addSearchParam("facet", "place_fct");
		builder.addSearchParam("facet", "type_fct");
		builder.addSearchParam("facet", "provider_fct");
		builder.addSearchParam("facet", "time_fct");
		builder.addSearchParam("facet", "license_group");

		FashionSearch fq = new FashionSearch();
		fq.setTerm(q.searchTerm);
		fq.setCount(Integer.parseInt(q.pageSize));
		fq.setOffset(((Integer.parseInt(q.page) - 1) * Integer.parseInt(q.pageSize)));
		builder.setData(fq);
		addfilters(q, builder);
		return builder;
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		SourceResponse res = new SourceResponse();
		res.source = getSourceName();
		// String httpQuery = getHttpQuery(q);
		QueryBuilder builder = getBuilder(q);
		res.query = builder.getHttp();
		JsonNode response;
		if (checkFilters(q)) {
			try {
				response = HttpConnector.getURLContent(res.query);
				JsonNode docs = response.path("results").get(0).path("docs");
				res.totalCount = Utils.readIntAttr(response, "numberOfResults", true);
				res.count = docs.size();
				// res.startIndex = Utils.readIntAttr(response, "offset", true);
				ArrayList<ItemsResponse> a = new ArrayList<ItemsResponse>();

				for (JsonNode item : docs) {
					ItemsResponse it = new ItemsResponse();
					it.id = Utils.readAttr(item, "id", true);
					List<String> readArrayAttr = Utils.readArrayAttr(item, "thumbnail", false);
					Function<String, String> f = (String x) -> {
						return "https://www.deutsche-digitale-bibliothek.de/" + x;
					};
					it.thumb = ListUtils.transform(readArrayAttr, f);
					it.fullresolution = null;
					it.title = Utils.readAttr(item, "title", false);
					// it.description = Utils.readLangAttr(item, "description",
					// false);
					// it.creator =
					// Utils.readLangAttr(item.path("sourceResource"),
					// "creator", false);
					// it.year = null;
					// it.dataProvider =
					// Utils.readLangAttr(item.path("provider"),
					// "name", false);
					it.url = new MyURL();
					// it.url.original = Utils.readArrayAttr(item, "isShownAt",
					// false);
					it.url.fromSourceAPI = "https://www.deutsche-digitale-bibliothek.de/item/" + it.id;
					a.add(it);
				}
				res.items = a;

				CommonFilterLogic dataProvider = CommonFilterLogic.dataproviderFilter();

				// JsonNode o = response.path("facets");
				// readList(o.path("dataProviders"), dataProvider);

				res.filtersLogic = new ArrayList<>();
				res.filtersLogic.add(dataProvider);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return res;
	}

}
