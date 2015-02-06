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
import espace.core.SourceResponse.MyURL;

public class DSpaceSource implements ISpaceSource {

	private String DPLAKey = "2edebbb32b1f42f86aaa56fd2edc1a28";

	public String getHttpQuery(CommonQuery q) {
		// q=zeus&api_key=2edebbb32b1f42f86aaa56fd2edc1a28&sourceResource.creator=Zeus
		return "http://api.dp.la/v2/items?api_key=" + DPLAKey + "&q="
				+ Utils.spacesFormatQuery(q.searchTerm == null ? "*" : q.searchTerm)
				+ ((q.termToExclude != null) ? "+NOT+(" + Utils.spacesFormatQuery(q.termToExclude) + ")" : "")
				+ "&page=" + q.page + "&page_size=" + q.pageSize;
	}

	public String getSourceName() {
		return "DPLA";
	}

	public String getDPLAKey() {
		return DPLAKey;
	}

	public void setDPLAKey(String dPLAKey) {
		DPLAKey = dPLAKey;
	}

	public List<CommonItem> getPreview(CommonQuery q) {
		ArrayList<CommonItem> res = new ArrayList<CommonItem>();
		try {
			String httpQuery = getHttpQuery(q);
			// System.out.println(httpQuery);
			JsonNode node = HttpConnector.getURLContent(httpQuery);
			JsonNode a = node.path("docs");
			for (int i = 0; i < a.size(); i++) {
				JsonNode o = a.get(i);
				CommonItem item = new CommonItem();
				JsonNode path = o.path("sourceResource").path("title");
				// System.out.println(path);
				item.setTitle(path.asText());
				item.seteSource(this.getSourceName());
				String description;
				JsonNode path2 = o.path("object");
				// System.out.println(path2);
				if (path2 != null)
					item.setPreview(path2.asText());
				res.add(item);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return res;
	}

	@Override
	public Object getResults(CommonQuery q) {
		SourceResponse res = new SourceResponse();
		res.source = getSourceName();
		String httpQuery = getHttpQuery(q);
		res.query = httpQuery;
		JsonNode response;
		try {
			response = HttpConnector.getURLContent(httpQuery);
			res.totalCount = Utils.readIntAttr(response, "limit", true);
			res.count = Utils.readIntAttr(response, "count", true);
			res.startIndex = Utils.readIntAttr(response, "start", true);
			ArrayList a = new ArrayList<Object>();

			for (JsonNode item : response.path("docs")) {
				ItemsResponse it = new ItemsResponse();
				it.id = Utils.readAttr(item, "id", true);
				it.thumb = Utils.readArrayAttr(item, "object", false);
				it.fullresolution = null;
				it.title = Utils.readLangAttr(item.path("sourceResource"), "title", false);
				it.description = Utils.readLangAttr(item.path("sourceResource"), "description", false);
				it.creator = Utils.readLangAttr(item.path("sourceResource"), "creator", false);
				it.year = null;
				it.dataProvider = Utils.readLangAttr(item.path("provider"), "name", false);
				it.url = new MyURL();
				it.url.original = Utils.readArrayAttr(item, "isShownAt", false);
				it.url.fromSourceAPI = Utils.readAttr(item, "guid", false);
				a.add(it);
			}
			res.items = a;
			res.facets = response.path("facets");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return res;
	}

}
