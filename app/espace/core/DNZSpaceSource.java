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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import espace.core.SourceResponse.ItemsResponse;
import espace.core.SourceResponse.Lang;
import espace.core.SourceResponse.MyURL;
import espace.core.Utils.Pair;

public class DNZSpaceSource implements ISpaceSource {

	/**
	 * user: espace password: with2015 email: gardero@gmail.com
	 */
	private String Key = "Qcv9eq67Ep32HDbYXmsx";

	public String getHttpQuery(CommonQuery q) {
		return "http://api.digitalnz.org/v3/records.json?api_key=" + Key + "&text="
				+ Utils.spacesFormatQuery(q.searchTerm) + "&per_page=" + q.pageSize + "&page=" + q.page;
	}

	public String getSourceName() {
		return "DigitalNZ";
	}

	public String getDPLAKey() {
		return Key;
	}

	public void setDPLAKey(String dPLAKey) {
		Key = dPLAKey;
	}

	public List<CommonItem> getPreview(CommonQuery q) {
		ArrayList<CommonItem> res = new ArrayList<CommonItem>();
		try {
			String httpQuery = getHttpQuery(q);
			// System.out.println(httpQuery);
			JsonNode node = HttpConnector.getURLContent(httpQuery);
			JsonNode a = node.path("response").path("zone");
			for (int i = 0; i < a.size(); i++) {
				JsonNode o = a.get(i);
				if (!o.path("name").asText().equals("people")) {
					JsonNode aa = node.path("records").path("zone");

					for (int k = 0; k < aa.size(); k++) {

						JsonNode oo = aa.get(k);
						CommonItem item = new CommonItem();
						JsonNode path = oo.path("sourceResource").path("title");
						// System.out.println(path);
						item.setTitle(path.asText());
						item.seteSource(this.getSourceName());
						String description;
						JsonNode path2 = oo.path("object");
						// System.out.println(path2);
						if (path2 != null)
							item.setPreview(path2.asText());
						res.add(item);
					}

				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return res;
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		SourceResponse res = new SourceResponse();
		res.source = getSourceName();
		String httpQuery = getHttpQuery(q);
		res.query = httpQuery;
		JsonNode response;
		try {
			response = HttpConnector.getURLContent(httpQuery);

			ArrayList a = new ArrayList<Object>();

			JsonNode o = response.path("search");
			// System.out.print(o.path("name").asText() + " ");

			res.totalCount = Utils.readIntAttr(o, "result_count", true);
			res.count += Utils.readIntAttr(o, "per_page", true);
			res.startIndex = (Utils.readIntAttr(o, "page", true) - 1) * res.count;

			JsonNode aa = o.path("results");

			for (JsonNode item : aa) {
				// System.out.println(item.toString());
				ItemsResponse it = new ItemsResponse();
				it.id = Utils.readAttr(item, "id", true);
				it.title = Utils.readLangAttr(item, "title", false);
				it.creator = Utils.readLangAttr(item, "creator", false);
				it.description = Utils.readLangAttr(item, "description", false);

				it.thumb = Utils.readArrayAttr(item, "thumbnail_url", false);
				// TODO not present
				it.fullresolution = null;
				// TODO read date and take year?
				it.year = null; // Utils.readArrayAttr(item, "issued", true);
				// TODO use author?
				it.dataProvider = null;// Utils.readLangAttr(item,
										// "contributor", false);
				it.url = new MyURL();
				it.url.original = Utils.readArrayAttr(item, "landing_url", false);
				it.url.fromSourceAPI = Utils.readAttr(item, "source_url", false);
				a.add(it);

			}

			res.items = a;

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return res;
	}

}