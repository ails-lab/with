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

public class ESpaceSource implements ISpaceSource {

	private String europeanaKey = "ANnuDzRpW";

	public String getHttpQuery(CommonQuery q) {
		return "http://europeana.eu/api/v2/search.json?wskey=" + europeanaKey + "&query="
				+ Utils.spacesFormatQuery(q.query) + "&start=" + ((q.page - 1) * q.pageSize + 1) + "&rows="
				+ q.pageSize + "&profile=rich+facets";
	}

	public String getSourceName() {
		return "europeana";
	}

	public String getEuropeanaKey() {
		return europeanaKey;
	}

	public void setEuropeanaKey(String europeanaKey) {
		this.europeanaKey = europeanaKey;
	}

	public List<CommonItem> getPreview(CommonQuery q) {
		ArrayList<CommonItem> res = new ArrayList<CommonItem>();
		try {
			String httpQuery = getHttpQuery(q);
			// System.out.println(httpQuery);
			JsonNode node = HttpConnector.getURLContent(httpQuery);
			JsonNode a = node.path("items");
			for (int i = 0; i < a.size(); i++) {
				JsonNode o = a.get(i);
				// System.out.println(o);
				CommonItem item = new CommonItem();
				JsonNode path = o.path("title");
				// System.out.println(path);
				item.setTitle(path.get(0).asText());
				item.seteSource(this.getSourceName());
				JsonNode path2 = o.path("edmPreview");
				// System.out.println(path2);
				if (path2 != null && path2.get(0) != null)
					item.setPreview(path2.get(0).asText());
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
			res.totalCount = Utils.readIntAttr(response, "totalResults", true);
			res.count = Utils.readIntAttr(response, "itemsCount", true);
			ArrayList a = new ArrayList<Object>();
			if (response.path("success").asBoolean()) {
				for (JsonNode item : response.path("items")) {
					ItemsResponse it = new ItemsResponse();
					it.id = Utils.readAttr(item, "id", true);
					it.thumb = Utils.readArrayAttr(item, "edmPreview", false);
					it.title = Utils.readArrayAttr(item, "title", false);
					it.description = Utils.readArrayAttr(item, "dcDescription", false);
					it.creator = Utils.readArrayAttr(item, "dcCreator", false);
					it.year = Utils.readArrayAttr(item, "year", false);
					it.dataProvider = Utils.readArrayAttr(item, "dataProvider", false);
					it.url = new MyURL();
					it.url.original = Utils.readArrayAttr(item, "edmIsShownAt", false);
					it.url.fromSourceAPI = Utils.readAttr(item, "guid", false);
					a.add(it);
				}
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
