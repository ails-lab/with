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


package sources;

import java.util.ArrayList;
import java.util.HashMap;

import model.resources.RecordResource;

import org.json.JSONArray;
import org.json.JSONException;

import sources.core.AutocompleteResponse;
import sources.core.CommonQuery;
import sources.core.HttpConnector;
import sources.core.ISpaceSource;
import sources.core.RecordJSONMetadata;
import sources.core.SourceResponse;
import sources.core.Utils;
import sources.core.AutocompleteResponse.DataJSON;
import sources.core.AutocompleteResponse.Suggestion;
import sources.core.RecordJSONMetadata.Format;

import com.fasterxml.jackson.databind.JsonNode;

public class YouTubeSpaceSource extends ISpaceSource {

	// TODO keep track of the pages links and go to the requested page.

	private HashMap<String, String> roots;
	private int autoCompleteLimit = 0;

	public YouTubeSpaceSource() {
		super();
		roots = new HashMap<String, String>();
	}

	public String getHttpQuery(CommonQuery q) {
		String token = getPageInfo(q.searchTerm, q.page, q.pageSize);
		return getBaseURL()
				+ "search?part=snippet&q="
				+ Utils.spacesPlusFormatQuery(q.searchTerm == null ? "*"
						: q.searchTerm) + "&maxResults=" + q.pageSize
				+ (token == null ? "" : ("&pageToken=" + token))
				+ "&type=video&key=" + getKey();
	}

	private String getPageInfo(String q, String page, String pageSize) {
		if (page == null || page.equals("1")) {
			return null;
		}
		String string = roots.get(getPrevKey(q, page, pageSize));
		return string;
	}

	private static String getKey() {
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
//		res.source = getSourceName();
//		String httpQuery = getHttpQuery(q);
//		res.query = httpQuery;
//		JsonNode response;
//		
//		if (q.filters==null || q.filters.size()==0 ||
//				(q.filters.size()==1 && 
//				q.filters.get(0).filterID.equals(CommonFilters.TYPE_ID) &&
//				q.filters.get(0).values.contains(TypeValues.VIDEO)
//				)){
//			try {
//				response = HttpConnector.getURLContent(httpQuery);
//				// System.out.println(httpQuery);
//				// System.out.println(response.toString());
//				JsonNode docs = response.path("items");
//				res.totalCount = Utils.readIntAttr(response.path("pageInfo"),
//						"totalResults", true);
//				res.count = docs.size();
//				res.startIndex = 0;
//				ArrayList<ItemsResponse> a = new ArrayList<ItemsResponse>();
//
//				for (JsonNode item : docs) {
//					ItemsResponse it = new ItemsResponse();
//					it.id = Utils.readAttr(item.path("id"), "videoId", true);
//					it.thumb = Utils
//							.readArrayAttr(item.path("snippet").path("thumbnails")
//									.path("default"), "url", false);
//					it.fullresolution = Utils.readArrayAttr(item.path("snippet")
//							.path("thumbnails").path("high"), "url", false);
//					it.title = Utils.readAttr(item.path("snippet"), "title",
//							false);
//					it.description = Utils.readAttr(item.path("snippet"),
//							"description", false);
//					it.creator = null;// Utils.readLangAttr(item.path("sourceResource"),
//										// "creator", false);
//					it.year = null;
//					it.dataProvider = Utils.readAttr(item.path("snippet"),
//							"channelTitle", false);
//					it.url = new MyURL();
//					it.url.fromSourceAPI = "https://www.youtube.com/watch?v="
//							+ it.id;
//					it.url.original = new ArrayList<String>();
//					it.url.original.add(it.url.fromSourceAPI);
//					it.externalId = it.url.fromSourceAPI;
//					it.externalId = DigestUtils.md5Hex(it.externalId);
//					a.add(it);
//				}
//				res.items = a;
//				// res.facets = response.path("facets");
//
//				savePageDetails(q.searchTerm, q.page, q.pageSize,
//						response.path("nextPageToken").asText());
//
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		} 

		return res;
	}

	private void savePageDetails(String q, String page, String pageSize,
			String nextPageToken) {
		String key = getKey(q, page, pageSize);
		if (!roots.containsKey(key)) {
			roots.put(key, nextPageToken);
			// System.out.println("Saved [" + key + "]" + nextPageToken);
		}
	}

	private String getKey(String q, String page, String pageSize) {
		return q + "/" + page + "/" + pageSize;
	}

	private String getPrevKey(String q, String page, String pageSize) {
		return q + "/" + (Integer.parseInt(page) - 1) + "/" + pageSize;
	}

	public String autocompleteQuery(String term, int limit) {
		autoCompleteLimit = limit;
		return "http://suggestqueries.google.com/complete/search?hl=en&ds=yt&client=youtube&json=t"
				+ "&key=" + getKey() + "&q=" + term;
	}

	public AutocompleteResponse autocompleteResponse(String response) {
		try {
			JSONArray jsonResp = new JSONArray(response);
			JSONArray suggestionsArray = jsonResp.getJSONArray(1);
			if (suggestionsArray == null || suggestionsArray.length() == 0)
				return new AutocompleteResponse();
			else {
				AutocompleteResponse ar = new AutocompleteResponse();
				ar.suggestions = new ArrayList<Suggestion>();
				int suggestionsLength = autoCompleteLimit < suggestionsArray.length() ? autoCompleteLimit : suggestionsArray.length();
				for (int i = 0; i < suggestionsLength; i++) {
					String suggestion = (String) suggestionsArray.get(i);
					Suggestion s = new Suggestion();
					s.value = suggestion;
					DataJSON data = new DataJSON();
					data.category = "YouTube";
					s.data = data;
					ar.suggestions.add(s);
				}
				return ar;
			}
		} catch (JSONException e) {
			e.printStackTrace();
			return new AutocompleteResponse();
		}
	}

	@Override
	public ArrayList<RecordJSONMetadata> getRecordFromSource(String recordId, RecordResource fullRecord) {
		ArrayList<RecordJSONMetadata> jsonMetadata = new ArrayList<RecordJSONMetadata>();
		JsonNode response;
		try {
			response = HttpConnector
					.getURLContent("https://www.googleapis.com/youtube/v3/videos?id="
							+ recordId + "&part=snippet&key=" + getKey());
			JsonNode record = response.get("items").get(0);
			jsonMetadata.add(new RecordJSONMetadata(Format.JSON_YOUTUBE, record
					.toString()));
			return jsonMetadata;
		} catch (Exception e) {
			return jsonMetadata;
		}
	}
}
