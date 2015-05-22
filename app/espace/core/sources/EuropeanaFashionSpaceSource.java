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

import com.fasterxml.jackson.databind.JsonNode;

import espace.core.CommonQuery;
import espace.core.HttpConnector;
import espace.core.ISpaceSource;
import espace.core.RecordJSONMetadata;
import espace.core.RecordJSONMetadata.Format;
import espace.core.SourceResponse;
import espace.core.SourceResponse.ItemsResponse;
import espace.core.SourceResponse.MyURL;
import espace.core.Utils;

public class EuropeanaFashionSpaceSource extends ISpaceSource {

	public String getHttpQuery(CommonQuery q) {
		return "http://www.europeanafashion.eu/api/search/"
				+ Utils.spacesPlusFormatQuery(q.searchTerm == null ? "*"
						: q.searchTerm)
				+ "/"
				+ q.pageSize
				+ "/"
				+ ""
				+ ((Integer.parseInt(q.page) - 1)
						* Integer.parseInt(q.pageSize) + 1);
	}

	public String getSourceName() {
		return "EFashion";
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
			// System.out.println(response.toString());
			JsonNode docs = response.path("results");
			res.totalCount = Utils.readIntAttr(response, "total", true);
			res.count = docs.size();
			res.startIndex = Utils.readIntAttr(response, "offset", true);
			ArrayList<ItemsResponse> a = new ArrayList<ItemsResponse>();

			for (JsonNode item : docs) {
				ItemsResponse it = new ItemsResponse();
				it.id = Utils.readAttr(item, "id", true);
				it.thumb = Utils.readArrayAttr(item, "thumbnail", false);
				it.fullresolution = null;
				it.title = Utils.readLangAttr(item, "title", false);
				it.description = Utils.readLangAttr(item, "description", false);
				// it.creator = Utils.readLangAttr(item.path("sourceResource"),
				// "creator", false);
				// it.year = null;
				// it.dataProvider = Utils.readLangAttr(item.path("provider"),
				// "name", false);
				it.url = new MyURL();
				// it.url.original = Utils.readArrayAttr(item, "isShownAt",
				// false);
				it.url.fromSourceAPI = "http://www.europeanafashion.eu/record/a/"
						+ it.id;
				a.add(it);
			}
			res.items = a;
			res.facets = response.path("facets");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return res;
	}

	public ArrayList<RecordJSONMetadata> getRecordFromSource(String recordId) {
		ArrayList<RecordJSONMetadata> jsonMetadata = new ArrayList<RecordJSONMetadata>();
		JsonNode response;
		try {
			response = HttpConnector
					.getURLContent("http://www.europeanafashion.eu/api/record/"
							+ recordId);
			JsonNode record = response;
			jsonMetadata.add(new RecordJSONMetadata(Format.JSON_EDM, record
					.toString()));
			return jsonMetadata;
		} catch (Exception e) {
			return jsonMetadata;
		}
	}

}
