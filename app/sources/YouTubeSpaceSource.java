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

import model.DescriptiveData;
import model.EmbeddedMediaObject;
import model.EmbeddedMediaObject.MediaVersion;
import model.EmbeddedMediaObject.WithMediaType;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.basicDataTypes.ProvenanceInfo.Sources;
import model.resources.CulturalObject;
import model.resources.RecordResource;
import model.resources.RecordResource.RecordDescriptiveData;
import model.resources.WithResource.WithResourceType;
import play.libs.Json;

import org.json.JSONArray;
import org.json.JSONException;

import sources.core.AutocompleteResponse;
import sources.core.CommonQuery;
import sources.core.HttpConnector;
import sources.core.ISpaceSource;
import sources.core.RecordJSONMetadata;
import sources.core.SourceResponse;
import sources.core.Utils;
import sources.utils.JsonContextRecord;
import sources.core.AutocompleteResponse.DataJSON;
import sources.core.AutocompleteResponse.Suggestion;
import sources.core.CommonFilters;
import sources.core.RecordJSONMetadata.Format;
import sources.formatreaders.EuropeanaItemRecordFormatter;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.MediaType;

public class YouTubeSpaceSource extends ISpaceSource {

	// TODO keep track of the pages links and go to the requested page.

	private HashMap<String, String> roots;
	private int autoCompleteLimit = 0;

	public YouTubeSpaceSource() {
		super(Sources.YouTube);
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


	@Override
	public SourceResponse getResults(CommonQuery q) {
		SourceResponse res = new SourceResponse();
		res.source = getSourceName().toString();
		String httpQuery = getHttpQuery(q);
		res.query = httpQuery;
		JsonNode response;
		
		if (q.filters==null || q.filters.size()==0 ||
				(q.filters.size()==1 && 
				q.filters.get(0).filterID.equals(CommonFilters.TYPE.getId()) &&
				q.filters.get(0).values.contains(WithMediaType.VIDEO)
				)){
			try {
				response = getHttpConnector().getURLContent(httpQuery);
				// System.out.println(httpQuery);
				// System.out.println(response.toString());
				JsonNode docs = response.path("items");
				res.totalCount = Utils.readIntAttr(response.path("pageInfo"),
						"totalResults", true);
				res.count = docs.size();
				res.startIndex = 0;

				for (JsonNode item : docs) {
					RecordResource it = parseRecord(new JsonContextRecord(item));
					res.addItem(it);
				}
				// res.facets = response.path("facets");

				savePageDetails(q.searchTerm, q.page, q.pageSize,
						response.path("nextPageToken").asText());

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} 

		return res;
	}

	private RecordResource parseRecord(JsonContextRecord item) throws Exception {
		RecordResource it = new RecordResource();
		String id = item.getStringValue("id.videoId");
		String isAt = "https://www.youtube.com/watch?v="+id;
		it.addToProvenance(new ProvenanceInfo(sourceLABEL.toString(), isAt, id));

		EmbeddedMediaObject th = new EmbeddedMediaObject();
		th.setType(WithMediaType.VIDEO);
		th.setUrl(item.getStringValue("snippet.thumbnails.default.url"));
		th.setWidth(item.getIntValue("snippet.thumbnails.default.width"));
		th.setHeight(item.getIntValue("snippet.thumbnails.default.height"));
		it.addMedia(MediaVersion.Thumbnail, th );
		
		th = new EmbeddedMediaObject();
		th.setType(WithMediaType.VIDEO);
		th.setUrl(isAt);
		it.addMedia(MediaVersion.Original, th);

		addOtherThumbs(item, it);
		
		RecordDescriptiveData model;
		it.setDescriptiveData(model=new RecordDescriptiveData());
		model.setIsShownAt(new LiteralOrResource(isAt).fillDEF());
		model.setLabel(item.getMultiLiteralValue("snippet.title"));
		model.setDescription(item.getMultiLiteralValue("snippet.title"));

		return it;
	}
	
	
	private RecordResource parseRecord2(JsonContextRecord item) throws Exception {
		RecordResource it = new RecordResource();
		String id = item.getStringValue("id");
		String isAt = "https://www.youtube.com/watch?v="+id;
		it.addToProvenance(new ProvenanceInfo(sourceLABEL.toString(), isAt, id));

		EmbeddedMediaObject th = new EmbeddedMediaObject();
		th.setType(WithMediaType.VIDEO);
		th.setUrl(item.getStringValue("snippet.thumbnails.default.url"));
		th.setWidth(item.getIntValue("snippet.thumbnails.default.width"));
		th.setHeight(item.getIntValue("snippet.thumbnails.default.height"));
		it.addMedia(MediaVersion.Thumbnail, th );
		
		th = new EmbeddedMediaObject();
		th.setType(WithMediaType.VIDEO);
		th.setUrl(isAt);
		it.addMedia(MediaVersion.Original, th);

		addOtherThumbs(item, it);
		RecordDescriptiveData model;
		it.setDescriptiveData(model=new RecordDescriptiveData());
		model.setIsShownAt(new LiteralOrResource(isAt).fillDEF());
		model.setLabel(item.getMultiLiteralValue("snippet.title"));
		model.setDescription(item.getMultiLiteralValue("snippet.title"));
		model.setKeywords(item.getMultiLiteralOrResourceValue("snippet.tags"));
		return it;
	}

	private void addOtherThumbs(JsonContextRecord item, RecordResource it) {
		EmbeddedMediaObject th;
		if (Utils.hasInfo(item.getValue("snippet.thumbnails.medium"))) {
			th = new EmbeddedMediaObject();
			th.setType(WithMediaType.VIDEO);
			th.setUrl(item.getStringValue("snippet.thumbnails.medium.url"));
			th.setWidth(item.getIntValue("snippet.thumbnails.medium.width"));
			th.setHeight(item.getIntValue("snippet.thumbnails.medium.height"));
			it.addMediaView(MediaVersion.Thumbnail, th);
		}
		if (Utils.hasInfo(item.getValue("snippet.thumbnails.high"))) {
			th = new EmbeddedMediaObject();
			th.setType(WithMediaType.VIDEO);
			th.setUrl(item.getStringValue("snippet.thumbnails.high.url"));
			th.setWidth(item.getIntValue("snippet.thumbnails.high.width"));
			th.setHeight(item.getIntValue("snippet.thumbnails.high.height"));
			it.addMediaView(MediaVersion.Thumbnail, th);
		}
		if (Utils.hasInfo(item.getValue("snippet.thumbnails.standard"))) {
			th = new EmbeddedMediaObject();
			th.setType(WithMediaType.VIDEO);
			th.setUrl(item.getStringValue("snippet.thumbnails.standard.url"));
			th.setWidth(item.getIntValue("snippet.thumbnails.standard.width"));
			th.setHeight(item.getIntValue("snippet.thumbnails.standard.height"));
			it.addMediaView(MediaVersion.Thumbnail, th);
		}
		if (Utils.hasInfo(item.getValue("snippet.thumbnails.maxres"))) {
			th = new EmbeddedMediaObject();
			th.setType(WithMediaType.VIDEO);
			th.setUrl(item.getStringValue("snippet.thumbnails.maxres.url"));
			th.setWidth(item.getIntValue("snippet.thumbnails.maxres.width"));
			th.setHeight(item.getIntValue("snippet.thumbnails.maxres.height"));
			it.addMediaView(MediaVersion.Thumbnail, th);
		}
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
			response = getHttpConnector()
					.getURLContent("https://www.googleapis.com/youtube/v3/videos?id="
							+ recordId + "&part=snippet&key=" + getKey());
			
			
			if (response != null) {
				JsonNode record = response.get("items").get(0);
				jsonMetadata.add(new RecordJSONMetadata(Format.JSON_YOUTUBE, record
						.toString()));
				Object item = parseRecord2(new JsonContextRecord(record));
				String json = Json.toJson(item).toString();
				jsonMetadata.add(new RecordJSONMetadata(Format.JSON_WITH, json));
			}
			
			
			return jsonMetadata;
		} catch (Exception e) {
			return jsonMetadata;
		}
	}
}
