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

import org.apache.commons.codec.digest.DigestUtils;

import com.fasterxml.jackson.databind.JsonNode;

import espace.core.CommonFilterLogic;
import espace.core.CommonQuery;
import espace.core.FacetsModes;
import espace.core.HttpConnector;
import espace.core.ISpaceSource;
import espace.core.QueryBuilder;
import espace.core.SourceResponse;
import espace.core.Utils;
import espace.core.SourceResponse.ItemsResponse;
import espace.core.SourceResponse.MyURL;
import play.libs.Json;

public class RijksmuseumSpaceSource extends ISpaceSource {
	public static final String LABEL = "Rijksmuseum";
	private String apikey = "SECRET_KEY";

	@Override
	public String getSourceName() {
		return LABEL;
	}

	public String getHttpQuery(CommonQuery q) {
		QueryBuilder builder = new QueryBuilder("https://www.rijksmuseum.nl/api/en/collection");
		builder.addSearchParam("key", apikey);
		builder.addSearchParam("format", "json");
		builder.addQuery("q", q.searchTerm);
		builder.addSearchParam("p", "" + ((Integer.parseInt(q.page) - 1) * Integer.parseInt(q.pageSize) + 1));

		builder.addSearchParam("ps", "" + q.pageSize);

		return addfilters(q, builder).getHttp();
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		SourceResponse res = new SourceResponse();
		res.source = getSourceName();
		String httpQuery = getHttpQuery(q);
		res.query = httpQuery;
		JsonNode response;
		// CommonFilterLogic type = CommonFilterLogic.typeFilter();
		// CommonFilterLogic provider = CommonFilterLogic.providerFilter();
		// CommonFilterLogic dataprovider =
		// CommonFilterLogic.dataproviderFilter();
		// CommonFilterLogic creator = CommonFilterLogic.creatorFilter();
		// CommonFilterLogic rights = CommonFilterLogic.rightsFilter();
		// CommonFilterLogic country = CommonFilterLogic.countryFilter();
		// CommonFilterLogic year = CommonFilterLogic.yearFilter();
		// CommonFilterLogic contributor =
		// CommonFilterLogic.contributorFilter();
		if (checkFilters(q)) {
			try {
				response = HttpConnector.getURLContent(httpQuery);
				res.totalCount = Utils.readIntAttr(response, "count", true);
				// res.count = q.pageSize;
				ArrayList<ItemsResponse> a = new ArrayList<ItemsResponse>();
				for (JsonNode item : response.path("artObjects")) {
					ItemsResponse it = new ItemsResponse();
					// countValue(type, t);
					it.id = Utils.readAttr(item, "objectNumber", true);
					it.title = Utils.readLangAttr(item, "title", false);
					it.creator = Utils.readLangAttr(item, "principalOrFirstMaker", false);

					it.thumb = Utils.readArrayAttr(item.path("webImage"), "url", false);

					it.fullresolution = Utils.readArrayAttr(item.path("headerImage"), "url", false);
					// it.description = Utils.readLangAttr(item,
					// "dcDescription", false);
					// it.year = Utils.readArrayAttr(item, "year", false);
					// it.dataProvider = Utils.readLangAttr(item,
					// "edmDataProvider",
					// false);
					it.url = new MyURL();
					it.url.original = it.fullresolution;
					it.url.fromSourceAPI = "https://www.rijksmuseum.nl/en/search/objecten?q=dance&p=1&ps=12&ii=0#/"
							+ it.id + ",0";
					// it.rights = Utils.readLangAttr(item, "rights", false);
					it.externalId = it.fullresolution.get(0);
					it.externalId = DigestUtils.md5Hex(it.externalId);
					a.add(it);
				}
				res.items = a;
				res.count = a.size();

				res.facets = response.path("facets");

				// for (JsonNode facet : response.path("facets")) {
				// for (JsonNode jsonNode : facet.path("fields")) {
				// String label = jsonNode.path("label").asText();
				// int count = jsonNode.path("count").asInt();
				// switch (facet.path("name").asText()) {
				// case "TYPE":
				// countValue(type, label, count);
				// break;
				//
				// case "DATA_PROVIDER":
				// countValue(dataprovider, label, false, count);
				// break;
				//
				// case "PROVIDER":
				// countValue(provider, label, false, count);
				// break;
				//
				// case "RIGHTS":
				// countValue(rights, label, count);
				// break;
				//
				// case "proxy_dc_creator":
				// countValue(creator, label, false, count);
				// break;
				//// case "proxy_dc_contributor":
				//// countValue(contributor, label, false, count);
				//// break;
				// case "COUNTRY":
				// countValue(country, label, false, count);
				// break;
				//
				// case "YEAR":
				// countValue(year, label, false, count);
				// break;
				//
				// default:
				// break;
				// }
				// }
				// }

				res.filtersLogic = new ArrayList<>();
				// res.filtersLogic.add(type);
				// res.filtersLogic.add(provider);
				// res.filtersLogic.add(dataprovider);
				// res.filtersLogic.add(creator);
				//// res.filtersLogic.add(contributor);
				// res.filtersLogic.add(rights);
				// res.filtersLogic.add(country);
				// res.filtersLogic.add(year);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// protected void countValue(CommonFilterResponse type, String t) {
		// type.addValue(vmap.translateToCommon(type.filterID, t));
		// }
		return res;
	}

}
