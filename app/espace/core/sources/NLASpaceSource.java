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
import java.util.List;

import org.w3c.dom.Document;

import utils.Serializer;

import com.fasterxml.jackson.databind.JsonNode;

import espace.core.CommonFilterResponse;
import espace.core.CommonFilters;
import espace.core.CommonQuery;
import espace.core.HttpConnector;
import espace.core.ISpaceSource;
import espace.core.RecordJSONMetadata;
import espace.core.RecordJSONMetadata.Format;
import espace.core.SourceResponse;
import espace.core.SourceResponse.ItemsResponse;
import espace.core.SourceResponse.MyURL;
import espace.core.Utils;
import espace.core.Utils.Pair;

public class NLASpaceSource extends ISpaceSource {

	private String Key = "SECRET_KEY";

	public NLASpaceSource() {
		super();
		// addMapping(CommonFilters.TYPE_ID, TypeValues.IMAGE, "Images");
		// addMapping(CommonFilters.TYPE_ID, TypeValues.IMAGE, "Image",
		// "%20format%3AImage");
		addMapping(CommonFilters.TYPE_ID, TypeValues.IMAGE, "Image", "%20format%3APhotograph");
		addMapping(CommonFilters.TYPE_ID, TypeValues.VIDEO, "Video", "%20format%3AVideo");
		addMapping(CommonFilters.TYPE_ID, TypeValues.SOUND, "Sound", "%20format%3ASound");
		addMapping(CommonFilters.TYPE_ID, TypeValues.TEXT, "Books", "%20format%3ABooks");
		// addMapping(CommonFilters.TYPE_ID, TypeValues.TEXT, "Books");
	}

	public String getHttpQuery(CommonQuery q) {
		String spacesFormatQuery = Utils.spacesFormatQuery(q.searchTerm, "%20");
		spacesFormatQuery = addfilters(q, spacesFormatQuery);
		return "http://api.trove.nla.gov.au/result?key="
				+ Key
				+ "&zone=picture,book,music,article"
				+ "&q="
				+ spacesFormatQuery
				+ (Utils.hasAny(q.termToExclude) ? "%20NOT%20" + Utils.spacesFormatQuery(q.termToExclude, "%20")
						+ "%20" : "") + "&n=" + q.pageSize + "&s="
				+ ((Integer.parseInt(q.page) - 1) * Integer.parseInt(q.pageSize)) + "&encoding=json&reclevel=full";
	}

	public String getSourceName() {
		return "NLA";
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
		CommonFilterResponse type = CommonFilterResponse.typeFilter();

		try {
			response = HttpConnector.getURLContent(httpQuery);
			// System.out.println(response.toString());
			JsonNode pa = response.path("response").path("zone");
			ArrayList<ItemsResponse> a = new ArrayList<ItemsResponse>();

			for (int i = 0; i < pa.size(); i++) {
				JsonNode o = pa.get(i);
				if (!o.path("name").asText().equals("people")) {
					System.out.print(o.path("name").asText() + " ");
					res.totalCount += Utils.readIntAttr(o.path("records"), "totalCount", true);
					res.count += Utils.readIntAttr(o.path("records"), "n", true);
					res.startIndex = Utils.readIntAttr(o.path("records"), "s", true);

					JsonNode aa = o.path("records").path("work");

					// System.out.println(aa.size());

					for (JsonNode item : aa) {
						// System.out.println(item.toString());

						List<String> v = Utils.readArrayAttr(item, "type", false);
						// type.addValue(vmap.translateToCommon(type.filterID,
						// ));
						// System.out.println("add " + v);
						for (String string : v) {
							countValue(type, string);
						}

						ItemsResponse it = new ItemsResponse();
						it.id = Utils.readAttr(item, "id", true);
						it.thumb = Utils.readArrayAttr(Utils.findNode(item.path("identifier"), new Pair<String>("type",
								"url"), new Pair<String>("linktype", "thumbnail")), "value", false);
						// TODO not present
						it.fullresolution = null;
						it.title = Utils.readLangAttr(item, "title", false);
						it.description = Utils.readLangAttr(item, "abstract", false);
						it.year = Utils.readArrayAttr(item, "issued", true);

						// TODO are they the same?
						it.creator = Utils.readLangAttr(item, "contributor", false);
						it.dataProvider = Utils.readLangAttr(item, "contributor", false);

						it.url = new MyURL();
						it.url.original = Utils.readArrayAttr(Utils.findNode(item.path("identifier"), new Pair<String>(
								"type", "url"), new Pair<String>("linktype", "fulltext, restricted, unknown")),
								"value", false);
						it.url.fromSourceAPI = Utils.readAttr(item, "troveUrl", false);

						a.add(it);

					}
				}
			}

			res.items = a;
			res.filters = new ArrayList<>();
			res.filters.add(type);
			// System.out.println(type);
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
			response = HttpConnector.getURLContent("http://api.trove.nla.gov.au/work/" + recordId + "?key=" + Key
					+ "&encoding=json&reclevel=full");
			JsonNode record = response;
			jsonMetadata.add(new RecordJSONMetadata(Format.JSON, record.toString()));
			Document xmlResponse = HttpConnector.getURLContentAsXML("http://api.trove.nla.gov.au/work/" + recordId
					+ "?key=" + Key + "&encoding=xml&reclevel=full");
			jsonMetadata.add(new RecordJSONMetadata(Format.XML, Serializer.serializeXML(xmlResponse)));
			return jsonMetadata;
		} catch (IOException e) {
			e.printStackTrace();
			return jsonMetadata;
		}
	}

}
