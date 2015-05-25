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
import java.util.function.Function;


import utils.ListUtils;


import com.fasterxml.jackson.databind.JsonNode;


import espace.core.CommonFilterLogic;
import espace.core.CommonFilters;
import espace.core.CommonQuery;
import espace.core.HttpConnector;
import espace.core.ISpaceSource;
import espace.core.QueryBuilder;
import espace.core.RecordJSONMetadata;
import espace.core.RecordJSONMetadata.Format;
import espace.core.SourceResponse;
import espace.core.SourceResponse.ItemsResponse;
import espace.core.SourceResponse.Lang;
import espace.core.SourceResponse.MyURL;
import espace.core.Utils;
import espace.core.Utils.Pair;
import espace.core.Utils.LongPair;

public class DPLASpaceSource extends ISpaceSource {

	private String DPLAKey = "SECRET_KEY";

	public String getHttpQuery(CommonQuery q) {
		// q=zeus&api_key=SECRET_KEY&sourceResource.creator=Zeus
		QueryBuilder builder = new QueryBuilder("http://api.dp.la/v2/items");
		builder.addSearchParam("api_key", DPLAKey);
		builder.addSearchParam("q", q.searchTerm);
		builder.addSearchParam("page",q.page);
		builder.addSearchParam("page_size",q.pageSize);
		builder.addSearchParam("facets","provider.name,sourceResource.type");
		return addfilters(q, builder).getHttp();
	}

	public DPLASpaceSource() {
		super();
		addDefaultWriter(CommonFilters.TYPE_ID, new Function<String, Pair<String>>() {
			@Override
			public Pair<String> apply(String t) {
				return new LongPair<String>("sourceResource.type", t);
			}
		});
		addDefaultWriter(CommonFilters.CREATOR_ID, new Function<String, Pair<String>>() {
			@Override
			public Pair<String> apply(String t) {
				return new LongPair<String>("sourceResource.creator", t);
			}
		});
		addDefaultWriter(CommonFilters.PROVIDER_ID,new Function<String, Pair<String>>() {
			@Override
			public Pair<String> apply(String t) {
				return new LongPair<String>("provider.name", t);
			}
		});
		
		addMapping(CommonFilters.TYPE_ID, TypeValues.IMAGE, "image", new Pair<String>("sourceResource.type","image"));
		addMapping(CommonFilters.TYPE_ID, TypeValues.VIDEO, "moving image",
				new LongPair<String>("sourceResource.type","moving image"));
		addMapping(CommonFilters.TYPE_ID, TypeValues.SOUND, "sound", new Pair<String>("sourceResource.type","sound"));
		addMapping(CommonFilters.TYPE_ID, TypeValues.TEXT, "text", new Pair<String>("sourceResource.type","text"));

		// TODO: what to do with physical objects?
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

	@Override
	public SourceResponse getResults(CommonQuery q) {
		SourceResponse res = new SourceResponse();
		res.source = getSourceName();
		String httpQuery = getHttpQuery(q);
		res.query = httpQuery;
		JsonNode response;
		CommonFilterLogic type = CommonFilterLogic.typeFilter();
		CommonFilterLogic provider = CommonFilterLogic.providerFilter();
		CommonFilterLogic creator = CommonFilterLogic.creatorFilter();

		try {
			response = HttpConnector.getURLContent(httpQuery);
			// System.out.println(response.toString());
			JsonNode docs = response.path("docs");
			res.totalCount = Utils.readIntAttr(response, "count", true);
			res.count = docs.size();
			res.startIndex = Utils.readIntAttr(response, "start", true);
			ArrayList<ItemsResponse> a = new ArrayList<ItemsResponse>();

			for (JsonNode item : docs) {

				// String t = Utils.readAttr(item.path("sourceResource"),
				// "type", false);
				// countValue(type, t);

				ItemsResponse it = new ItemsResponse();
				it.id = Utils.readAttr(item, "id", true);
				it.thumb = Utils.readArrayAttr(item, "object", false);
				it.fullresolution = null;
				it.title = Utils.readLangAttr(item.path("sourceResource"),
						"title", false);
				it.description = Utils.readLangAttr(
						item.path("sourceResource"), "description", false);
				it.creator = Utils.readLangAttr(item.path("sourceResource"),
						"creator", false);
				countValue(creator,ListUtils.transform( it.creator, (Lang s)->{return s.value;}));
				it.year = null;
				it.dataProvider = Utils.readLangAttr(item.path("provider"),
						"name", false);
				it.url = new MyURL();
				it.url.original = Utils.readArrayAttr(item, "isShownAt", false);
				it.url.fromSourceAPI = "http://dp.la/item/"
						+ Utils.readAttr(item, "id", false);
				it.rights = Utils.readLangAttr(
						item.path("sourceResource"), "rights", false);

				a.add(it);
			}
			res.items = a;
			res.facets = response.path("facets");
			res.filters = new ArrayList<>();

			readList(response.path("facets").path("provider.name"), provider);

			readList(response.path("facets").path("sourceResource.type"), type);

			res.filters = new ArrayList<>();
			res.filters.add(type);
			res.filters.add(provider);
			res.filters.add(creator);
			System.out.println(provider.export());

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return res;
	}

	private void readList(JsonNode json, CommonFilterLogic filter) {
		// System.out.println(json);
		for (JsonNode f : json.path("terms")) {
			String label = f.path("term").asText();
			int count = f.path("count").asInt();
			countValue(filter, label, count);
		}
	}

	public ArrayList<RecordJSONMetadata> getRecordFromSource(String recordId) {
		ArrayList<RecordJSONMetadata> jsonMetadata = new ArrayList<RecordJSONMetadata>();
		JsonNode response;
		try {
			response = HttpConnector
					.getURLContent("http://api.dp.la/v2/items?id=" + recordId
							+ "&api_key=" + DPLAKey);
			JsonNode record = response.get("docs").get(0);
			jsonMetadata.add(new RecordJSONMetadata(Format.JSONLD_DPLA, record
					.toString()));
			return jsonMetadata;
		} catch (Exception e) {
			e.printStackTrace();
			return jsonMetadata;
		}
	}

}
