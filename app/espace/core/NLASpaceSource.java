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

public class NLASpaceSource implements ISpaceSource {

	private String Key = "SECRET_KEY";

	public String getHttpQuery(CommonQuery q) {
		// q=zeus&api_key=2edebbb32b1f42f86aaa56fd2edc1a28&sourceResource.creator=Zeus
		return "http://api.trove.nla.gov.au/result?key=" + Key + "&zone=picture,book,music,article" + "&q="
				+ Utils.spacesFormatQuery(q.searchTerm)
				+ (Utils.hasAny(q.termToExclude) ? "+NOT+(" + Utils.spacesFormatQuery(q.termToExclude) + ")+" : "")
				+ "&n=" + q.pageSize + "&s=" + ((Integer.parseInt(q.page) - 1) * Integer.parseInt(q.pageSize))
				+ "&encoding=json&reclevel=full";
		// return "http://api.dp.la/v2/items?api_key=" + Key + "&q="
		// + Utils.spacesFormatQuery(q.searchTerm == null ? "*" : q.searchTerm)
		// + ((q.termToExclude != null) ? "+NOT+(" +
		// Utils.spacesFormatQuery(q.termToExclude) + ")" : "")
		// + "&page=" + q.page + "&page_size=" + q.pageSize;
	}

	public String getSourceName() {
		return "NLA";
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

			JsonNode pa = response.path("response").path("zone");
			ArrayList a = new ArrayList<Object>();

			for (int i = 0; i < pa.size(); i++) {
				JsonNode o = pa.get(i);
				if (!o.path("name").asText().equals("people")) {
					System.out.print(o.path("name").asText() + " ");
					res.totalCount += Utils.readIntAttr(o.path("records"), "totalCount", true);
					res.count += Utils.readIntAttr(o.path("records"), "n", true);
					res.startIndex = Utils.readIntAttr(o.path("records"), "s", true);

					JsonNode aa = o.path("records").path("work");

					System.out.println(aa.size());

					for (JsonNode item : aa) {
						// System.out.println(item.toString());
						ItemsResponse it = new ItemsResponse();
						it.id = Utils.readAttr(item, "id", true);
						it.thumb = Utils.readArrayAttr(Utils.findNode(item.path("identifier"), new Pair<String>("type",
								"url"), new Pair<String>("linktype", "thumbnail")), "value", false);
						// TODO not present
						it.fullresolution = null;
						it.title = Utils.readLangAttr(item, "title", false);
						it.description = Utils.readLangAttr(item, "abstract", false);
						it.year = Utils.readArrayAttr(item, "issued", true);

						// TODO are they the same?
						it.creator = Utils.readLangAttr(item, "contributor", false);
						it.dataProvider = Utils.readLangAttr(item, "contributor", false);

						it.url = new MyURL();
						it.url.original = Utils.readArrayAttr(item, "troveUrl", false);

						// TODO What to use?
						// it.url.fromSourceAPI = Utils.readAttr(item, "guid",
						// false);
						a.add(it);

					}
				}
			}

			res.items = a;

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return res;
	}

}
