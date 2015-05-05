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
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import espace.core.CommonFilter;
import espace.core.CommonFilterResponse;
import espace.core.CommonFilters;
import espace.core.CommonQuery;
import espace.core.HttpConnector;
import espace.core.ISpaceSource;
import espace.core.SourceResponse;
import espace.core.SourceResponse.ItemsResponse;
import espace.core.SourceResponse.MyURL;
import espace.core.Utils;

public class DNZSpaceSource extends ISpaceSource {

	/**
	 * user: espace password: with2015 email: gardero@gmail.com
	 */
	private String Key = "Qcv9eq67Ep32HDbYXmsx";

	public DNZSpaceSource() {
		super();
		addMapping(CommonFilters.TYPE_ID, TypeValues.IMAGE, "Images", "&or[category][]=Images");
		// addMapping(CommonFilters.TYPE_ID, TypeValues.VIDEO, "Other");
		addMapping(CommonFilters.TYPE_ID, TypeValues.SOUND, "Audio", "&or[category][]=Audio");
		addMapping(CommonFilters.TYPE_ID, TypeValues.TEXT, "Books", "&or[category][]=Books");
	}

	public String getHttpQuery(CommonQuery q) {
		String qstr = "http://api.digitalnz.org/v3/records.json?api_key=" + Key + "&text="
				+ Utils.spacesPlusFormatQuery(q.searchTerm) + "&per_page=" + q.pageSize + "&page=" + q.page;
		qstr = addfilters(q, qstr);
		return qstr;
	}

	// private String filters(CommonQuery q, String qstr) {
	// if (q.filters != null) {
	// for (CommonFilter filter : q.filters) {
	// if (filter.filterID.equals(CommonFilters.TYPE_ID)) {
	// // eq.addSearch(Utils.getFacetsAttr(filter.value, "TYPE"));
	// List<String> v = vmap.translateToSpecific(filter.filterID, filter.value);
	// for (String string : v) {
	// if (v != null)
	// qstr += ("&or[category][]=" + Utils.spacesPlusFormatQuery(string));
	// }
	// }
	// }
	// }
	// return qstr;
	// }

	public String getSourceName() {
		return "DigitalNZ";
	}

	public String getDPLAKey() {
		return Key;
	}

	public void setDPLAKey(String dPLAKey) {
		Key = dPLAKey;
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		SourceResponse res = new SourceResponse();
		res.source = getSourceName();
		String httpQuery = getHttpQuery(q);
		res.query = httpQuery;
		JsonNode response;
		CommonFilterResponse type = CommonFilterResponse.typeFilter();

		try {
			response = HttpConnector.getURLContent(httpQuery);

			ArrayList<ItemsResponse> a = new ArrayList<ItemsResponse>();

			JsonNode o = response.path("search");
			// System.out.print(o.path("name").asText() + " ");

			res.totalCount = Utils.readIntAttr(o, "result_count", true);
			res.startIndex = (Utils.readIntAttr(o, "page", true) - 1) * res.count;

			JsonNode aa = o.path("results");

			for (JsonNode item : aa) {
				// System.out.println(item.toString());

				List<String> v = Utils.readArrayAttr(item, "category", false);
				System.out.println("add " + v);
				String t = v.get(0);
				countValue(type, t);

				ItemsResponse it = new ItemsResponse();
				it.id = Utils.readAttr(item, "id", true);
				it.title = Utils.readLangAttr(item, "title", false);
				it.creator = Utils.readLangAttr(item, "creator", false);
				it.description = Utils.readLangAttr(item, "description", false);

				it.thumb = Utils.readArrayAttr(item, "thumbnail_url", false);
				// TODO not present
				it.fullresolution = Utils.readArrayAttr(item, "large_thumbnail_url", false);
				// TODO read date and take year?
				it.year = null; // Utils.readArrayAttr(item, "issued", true);
				// TODO use author?
				it.dataProvider = null;// Utils.readLangAttr(item,
										// "contributor", false);
				it.url = new MyURL();
				it.url.original = Utils.readArrayAttr(item, "landing_url", false);
				it.url.fromSourceAPI = "http://www.digitalnz.org/records/" + it.id;
				a.add(it);

			}
			res.count = a.size();

			res.items = a;

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return res;
	}

}
