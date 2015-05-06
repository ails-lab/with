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

import org.w3c.dom.Document;

import utils.Serializer;

import com.fasterxml.jackson.databind.JsonNode;

import espace.core.CommonQuery;
import espace.core.HttpConnector;
import espace.core.ISpaceSource;
import espace.core.RecordJSONMetadata;
import espace.core.SourceResponse;
import espace.core.RecordJSONMetadata.Format;
import espace.core.SourceResponse.ItemsResponse;
import espace.core.SourceResponse.MyURL;
import espace.core.Utils;

public class DigitalNZSpaceSource extends ISpaceSource {

	/**
	 * National Library of New Zealand
	 */
	private String Key = "SECRET_KEY";

	public String getHttpQuery(CommonQuery q) {
		return "http://api.digitalnz.org/v3/records.json?api_key=" + Key
				+ "&text=" + Utils.spacesPlusFormatQuery(q.searchTerm)
				+ "&per_page=" + q.pageSize + "&page=" + q.page;
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
		try {
			response = HttpConnector.getURLContent(httpQuery);

			ArrayList<ItemsResponse> a = new ArrayList<ItemsResponse>();

			JsonNode o = response.path("search");
			// System.out.print(o.path("name").asText() + " ");

			res.totalCount = Utils.readIntAttr(o, "result_count", true);
			res.count += Utils.readIntAttr(o, "per_page", true);
			res.startIndex = (Utils.readIntAttr(o, "page", true) - 1)
					* res.count;

			JsonNode aa = o.path("results");

			for (JsonNode item : aa) {
				// System.out.println(item.toString());
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

			res.items = a;

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
					.getURLContent("http://api.digitalnz.org/v3/records/"
							+ recordId + ".json?api_key=" + Key);
			JsonNode record = response;
			jsonMetadata.add(new RecordJSONMetadata(Format.JSON, record
					.toString()));
			Document xmlResponse = HttpConnector
					.getURLContentAsXML("http://api.digitalnz.org/v3/records/"
							+ recordId + ".xml?api_key=" + Key);
			jsonMetadata.add(new RecordJSONMetadata(Format.XML, Serializer
					.serializeXML(xmlResponse)));
			return jsonMetadata;
		} catch (Exception e) {
			return jsonMetadata;
		}
	}

}
