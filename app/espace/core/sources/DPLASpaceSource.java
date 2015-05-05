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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import espace.core.CommonFilter;
import espace.core.CommonFilterResponse;
import espace.core.CommonFilters;
import espace.core.CommonQuery;
import espace.core.HttpConnector;
import espace.core.ISpaceSource;
import espace.core.RecordJSONMetadata;
import espace.core.SourceResponse;
import espace.core.RecordJSONMetadata.Format;
import espace.core.SourceResponse.ItemsResponse;
import espace.core.SourceResponse.MyURL;
import espace.core.Utils;

public class DPLASpaceSource extends ISpaceSource {

	private String DPLAKey = "SECRET_KEY";

	public String getHttpQuery(CommonQuery q) {
		// q=zeus&api_key=SECRET_KEY&sourceResource.creator=Zeus
		String qstr = "http://api.dp.la/v2/items?api_key=" + DPLAKey + "&q="
				+ Utils.spacesPlusFormatQuery(q.searchTerm == null ? "*" : q.searchTerm)
				+ (Utils.hasAny(q.termToExclude) ? "+NOT+(" + Utils.spacesPlusFormatQuery(q.termToExclude) + ")" : "")
				+ "&page=" + q.page + "&page_size=" + q.pageSize;
		qstr = addfilters(q, qstr);
		return qstr;
	}

	public DPLASpaceSource() {
		super();
		addMapping(CommonFilters.TYPE_ID, TypeValues.IMAGE, "image", "&sourceResource.type=image");
		addMapping(CommonFilters.TYPE_ID, TypeValues.VIDEO, "moving image", "&sourceResource.type=%22moving%20image%22");
		addMapping(CommonFilters.TYPE_ID, TypeValues.SOUND, "sound", "&sourceResource.type=sound");
		addMapping(CommonFilters.TYPE_ID, TypeValues.TEXT, "text", "&sourceResource.type=text");
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
		CommonFilterResponse type = CommonFilterResponse.typeFilter();

		try {
			response = HttpConnector.getURLContent(httpQuery);
			// System.out.println(response.toString());
			JsonNode docs = response.path("docs");
			res.totalCount = Utils.readIntAttr(response, "count", true);
			res.count = docs.size();
			res.startIndex = Utils.readIntAttr(response, "start", true);
			ArrayList<ItemsResponse> a = new ArrayList<ItemsResponse>();

			for (JsonNode item : docs) {

				String t = Utils.readAttr(item.path("sourceResource"), "type", false);
				countValue(type, t);

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
				it.url.fromSourceAPI = "http://dp.la/item/" + Utils.readAttr(item, "id", false);
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
			response = HttpConnector.getURLContent("http://api.dp.la/v2/items?id=" + recordId + "&api_key=" + DPLAKey);
			JsonNode record = response.get("docs").get(0);
			jsonMetadata.add(new RecordJSONMetadata(Format.JSONLD, record.toString()));
			return jsonMetadata;
		} catch (IOException e) {
			e.printStackTrace();
			return jsonMetadata;
		}
	}

}
