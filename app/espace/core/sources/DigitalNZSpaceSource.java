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
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;

import org.w3c.dom.Document;

import utils.Serializer;

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
import espace.core.SourceResponse.MyURL;
import espace.core.Utils;
import espace.core.Utils.Pair;
import espace.core.Utils.LongPair;

public class DigitalNZSpaceSource extends ISpaceSource {

	/**
	 * National Library of New Zealand
	 */
	private String Key = "SECRET_KEY";

	public DigitalNZSpaceSource() {
		super();
		
		addDefaultWriter(CommonFilters.CREATOR_ID, new Function<String, Pair<String>>() {
			@Override
			public Pair<String> apply(String t) {
				return new Pair<String>("and[creator][]",t);
			}
		});
		
		addDefaultWriter(CommonFilters.RIGHTS_ID, new Function<String, Pair<String>>() {
			@Override
			public Pair<String> apply(String t) {
				return new Pair<String>("and[rights][]",t);
			}
		});
		
		addMapping(CommonFilters.TYPE_ID, TypeValues.IMAGE, "Images",
				new Pair<String>("and[category][]","Images"));
		// addMapping(CommonFilters.TYPE_ID, TypeValues.VIDEO, "Other");
		addMapping(CommonFilters.TYPE_ID, TypeValues.SOUND, "Audio",
				new Pair<String>("and[category][]","Audio"));
		addMapping(CommonFilters.TYPE_ID, TypeValues.TEXT, "Books",
				new Pair<String>("and[category][]","Books"));
	}

	public String getHttpQuery(CommonQuery q) {
		QueryBuilder builder = new QueryBuilder("http://api.digitalnz.org/v3/records.json");
		builder.addSearchParam("api_key", Key);
		builder.addSearchParam("text", q.searchTerm);
		builder.addSearchParam("page",q.page);
		builder.addSearchParam("per_page",q.pageSize);
		builder.addSearchParam("facets","creator,category,rights");
		return addfilters(q, builder).getHttp();
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

	@Override
	public SourceResponse getResults(CommonQuery q) {
		SourceResponse res = new SourceResponse();
		res.source = getSourceName();
		String httpQuery = getHttpQuery(q);
		res.query = httpQuery;
		JsonNode response;
		CommonFilterLogic type = CommonFilterLogic.typeFilter();
		CommonFilterLogic creator = CommonFilterLogic.creatorFilter();
		CommonFilterLogic rights = CommonFilterLogic.rightsFilter();

		try {
			response = HttpConnector.getURLContent(httpQuery);

			ArrayList<ItemsResponse> a = new ArrayList<ItemsResponse>();

			JsonNode o = response.path("search");
			// System.out.print(o.path("name").asText() + " ");

			res.totalCount = Utils.readIntAttr(o, "result_count", true);
			res.startIndex = (Utils.readIntAttr(o, "page", true) - 1)
					* res.count;

			JsonNode aa = o.path("results");

			for (JsonNode item : aa) {
				// System.out.println(item.toString());

				List<String> v = Utils.readArrayAttr(item, "category", false);
				// System.out.println("add " + v);
				for (String string : v) {
					countValue(type, string);
				}
				ItemsResponse it = new ItemsResponse();
				it.id = Utils.readAttr(item, "id", true);
				it.title = Utils.readLangAttr(item, "title", false);
				it.creator = Utils.readLangAttr(item, "creator", false);
				it.description = Utils.readLangAttr(item, "description", false);

				it.thumb = Utils.readArrayAttr(item, "thumbnail_url", false);
				// TODO not present
				it.fullresolution = Utils.readArrayAttr(item,
						"large_thumbnail_url", false);
				// TODO read date and take year?
				it.year = null; // Utils.readArrayAttr(item, "issued", true);
				// TODO use author?
				it.dataProvider = null;// Utils.readLangAttr(item,
										// "contributor", false);
				it.url = new MyURL();
				it.url.original = Utils.readArrayAttr(item, "landing_url",
						false);
				it.url.fromSourceAPI = "http://www.digitalnz.org/records/"
						+ it.id;
				a.add(it);

			}
			res.count = a.size();

			res.items = a;
			

			readList(o.path("facets").path("category"), type);
			readList(o.path("facets").path("rights"), rights);

			readList(o.path("facets").path("creator"), creator);

			res.filters = new ArrayList<>();
			res.filters.add(type);
			res.filters.add(creator);
			res.filters.add(rights);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return res;
	}

	private void readList(JsonNode json, CommonFilterLogic filter) {
//		 System.out.println("************************\n"+json.toString());
		 for (Iterator<Entry<String, JsonNode>> iterator = json.fields(); iterator.hasNext();) {
			Entry<String, JsonNode> v = iterator.next();
			String label = v.getKey();
			int count = v.getValue().asInt();
			countValue(filter, label, v.getValue().asInt());
//			System.out.println(label+"   "+count);
		}
	}
	
	
	public ArrayList<RecordJSONMetadata> getRecordFromSource(String recordId) {
		ArrayList<RecordJSONMetadata> jsonMetadata = new ArrayList<RecordJSONMetadata>();
		JsonNode response;
		try {
			response = HttpConnector
					.getURLContent("http://api.digitalnz.org/v3/records/"
							+ recordId + ".json?api_key=" + Key);
			JsonNode record = response;
			jsonMetadata.add(new RecordJSONMetadata(Format.JSON_DNZ, record
					.toString()));
			Document xmlResponse = HttpConnector
					.getURLContentAsXML("http://api.digitalnz.org/v3/records/"
							+ recordId + ".xml?api_key=" + Key);
			jsonMetadata.add(new RecordJSONMetadata(Format.XML_DNZ, Serializer
					.serializeXML(xmlResponse)));
			return jsonMetadata;
		} catch (Exception e) {
			return jsonMetadata;
		}
	}

}
