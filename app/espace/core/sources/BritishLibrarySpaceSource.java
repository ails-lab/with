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
import java.util.Arrays;

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

public class BritishLibrarySpaceSource extends ISpaceSource {
	public static final String LABEL = "The British Library";
	private String apikey = "8bddf33bef4c14c98d469bfb1f8e78c7";

	@Override
	public String getSourceName() {
		return LABEL;
	}

	public String getHttpQuery(CommonQuery q) {
		QueryBuilder builder = new QueryBuilder(
				""
				+ "https://api.flickr.com/services/rest/");
		builder.addSearchParam("method","flickr.photos.search");
		builder.addSearchParam("api_key", apikey);
		builder.addSearchParam("format", "json");
		builder.addSearchParam("user_id","12403504%40N02");
		builder.addSearchParam("extras", "description,%20license,%20date_upload,%20date_taken,%20owner_name,%20icon_server,%20original_format,%20last_update,%20geo,%20tags,%20machine_tags,%20o_dims,%20views,%20media,%20path_alias,%20url_sq,%20url_t,%20url_s,%20url_q,%20url_m,%20url_n,%20url_z,%20url_c,%20url_l,%20url_o");
		builder.addQuery("text", q.searchTerm);
		builder.addSearchParam("page", "" + ((Integer.parseInt(q.page) - 1) * Integer.parseInt(q.pageSize) + 1));

		builder.addSearchParam("per_page", "" + q.pageSize);

		return addfilters(q, builder).getHttp();
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		SourceResponse res = new SourceResponse();
		res.source = getSourceName();
		String httpQuery = getHttpQuery(q);
		res.query = httpQuery;
		JsonNode response;

		if (checkFilters(q)) {
			try {
				String urlStringContent = HttpConnector.getURLStringContent(httpQuery);
				int s = urlStringContent.indexOf("({")+1;
				int e = urlStringContent.lastIndexOf("})")+1;
				String substring = urlStringContent.substring(s, e);
				response = Json.parse(substring);
				res.totalCount = Utils.readIntAttr(response.path("photos"), "total", true);
				res.count = Utils.readIntAttr(response.path("photos"), "perpage", true);
				ArrayList<ItemsResponse> a = new ArrayList<ItemsResponse>();
				for (JsonNode item : response.path("photos").path("photo")) {
					ItemsResponse it = new ItemsResponse();
					// countValue(type, t);
					it.id = Utils.readAttr(item, "id", true);
					it.title = Utils.readAttr(item, "title", false);
//					it.creator = Utils.readAttr(item, "principalOrFirstMaker", false);

					it.thumb = Utils.readArrayAttr(item, "url_s", false);
					it.fullresolution = Utils.readArrayAttr(item, "url_o", false);
					it.description = Utils.readAttr(item.path("description"),"_content", false);
					it.year = Arrays.asList(Utils.readAttr(item, "datetaken", false).substring(0, 4));
					System.out.println(it.year);
					it.dataProvider = LABEL;
					it.url = new MyURL();
					it.url.original = it.fullresolution;
					it.url.fromSourceAPI = "https://www.flickr.com/photos/britishlibrary/"
							+ it.id + "/";
					// it.rights = Utils.readLangAttr(item, "rights", false);
					it.externalId = it.fullresolution.get(0);
					it.externalId = DigestUtils.md5Hex(it.externalId);
					a.add(it);
				}
				res.items = a;
				res.count = a.size();

				res.facets = response.path("facets");


				res.filtersLogic = new ArrayList<>();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return res;
	}

}
