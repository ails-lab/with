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
import espace.core.SourceResponse;
import espace.core.SourceResponse.ItemsResponse;
import espace.core.SourceResponse.MyURL;
import espace.core.Utils;

public class YouTubeSpaceSource implements ISpaceSource {

	// TODO keep track of the pages links and go to the requested page.

	public String getHttpQuery(CommonQuery q) {
		return getBaseURL() + "search?part=snippet&q="
				+ Utils.spacesPlusFormatQuery(q.searchTerm == null ? "*" : q.searchTerm) + "&maxResults=" + q.pageSize
				+ "&type=video&key=" + getKey();
	}

	private String getKey() {
		return "SECRET_KEY";
	}

	private String getBaseURL() {
		return "https://www.googleapis.com/youtube/v3/";
	}

	public String getSourceName() {
		return "YouTube";
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
			JsonNode docs = response.path("items");
			res.totalCount = Utils.readIntAttr(response.path("pageInfo"), "totalResults", true);
			res.count = docs.size();
			res.startIndex = 0;
			ArrayList<ItemsResponse> a = new ArrayList<ItemsResponse>();

			for (JsonNode item : docs) {
				ItemsResponse it = new ItemsResponse();
				it.id = Utils.readAttr(item.path("id"), "videoId", true);
				it.thumb = Utils.readArrayAttr(item.path("snippet").path("thumbnails").path("default"), "url", false);
				it.fullresolution = Utils.readArrayAttr(item.path("snippet").path("thumbnails").path("high"), "url",
						false);
				it.title = Utils.readLangAttr(item.path("snippet"), "title", false);
				it.description = Utils.readLangAttr(item.path("snippet"), "description", false);
				it.creator = null;// Utils.readLangAttr(item.path("sourceResource"),
									// "creator", false);
				it.year = null;
				it.dataProvider = Utils.readLangAttr(item.path("snippet"), "channelTitle", false);
				it.url = new MyURL();
				it.url.fromSourceAPI = "https://www.youtube.com/watch?v=" + it.id;
				it.url.original = new ArrayList<String>();
				it.url.original.add(it.url.fromSourceAPI);
				a.add(it);
			}
			res.items = a;
			// res.facets = response.path("facets");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return res;
	}

}
