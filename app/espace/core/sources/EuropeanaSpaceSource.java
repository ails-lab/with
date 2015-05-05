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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;

import espace.core.AutocompleteResponse;
import espace.core.AutocompleteResponse.DataJSON;
import espace.core.AutocompleteResponse.Suggestion;
import espace.core.CommonFilterResponse;
import espace.core.CommonFilters;
import espace.core.CommonQuery;
import espace.core.EuropeanaQuery;
import espace.core.HttpConnector;
import espace.core.ISpaceSource;
import espace.core.RecordJSONMetadata;
import espace.core.RecordJSONMetadata.Format;
import espace.core.SourceResponse;
import espace.core.SourceResponse.ItemsResponse;
import espace.core.SourceResponse.MyURL;
import espace.core.Utils;

public class EuropeanaSpaceSource extends ISpaceSource {

	public EuropeanaSpaceSource() {
		super();
		addMapping(CommonFilters.TYPE_ID, TypeValues.IMAGE, "IMAGE", "&qf=TYPE:IMAGE");
		addMapping(CommonFilters.TYPE_ID, TypeValues.VIDEO, "VIDEO", "&qf=TYPE:VIDEO");
		addMapping(CommonFilters.TYPE_ID, TypeValues.SOUND, "SOUND", "&qf=TYPE:SOUND");
		addMapping(CommonFilters.TYPE_ID, TypeValues.TEXT, "TEXT", "&qf=TYPE:TEXT");
	}

	public String getHttpQuery(CommonQuery q) {
		EuropeanaQuery eq = new EuropeanaQuery();
		eq.addSearch(getSearchTerm(q));
		eq.addSearchParam("start", "" + ((Integer.parseInt(q.page) - 1) * Integer.parseInt(q.pageSize) + 1));
		eq.addSearchParam("rows", "" + q.pageSize);
		eq.addSearchParam("profile", "rich+facets");
		// filters(q, eq);
		euroAPI(q, eq);
		String http = eq.getHttp();
		http = addfilters(q, http);
		return http;
	}

	private String getSearchTerm(CommonQuery q) {
		if (Utils.hasAny(q.searchTerm))
			return Utils.spacesPlusFormatQuery(q.searchTerm)
					+ (Utils.hasAny(q.termToExclude) ? "+NOT+(" + Utils.spacesPlusFormatQuery(q.termToExclude) + ")"
							: "");
		return null;
	}

	private void euroAPI(CommonQuery q, EuropeanaQuery eq) {
		if (q.europeanaAPI != null) {
			eq.addSearch(Utils.getAttr(q.europeanaAPI.who, "who"));
			eq.addSearch(Utils.getAttr(q.europeanaAPI.where, "where"));
			if (q.europeanaAPI.facets != null) {
				eq.addSearch(Utils.getFacetsAttr(q.europeanaAPI.facets.TYPE, "TYPE"));
				eq.addSearch(Utils.getFacetsAttr(q.europeanaAPI.facets.LANGUAGE, "LANGUAGE"));
				eq.addSearch(Utils.getFacetsAttr(q.europeanaAPI.facets.YEAR, "YEAR"));
				eq.addSearch(Utils.getFacetsAttr(q.europeanaAPI.facets.COUNTRY, "COUNTRY"));
				eq.addSearch(Utils.getFacetsAttr(q.europeanaAPI.facets.RIGHTS, "RIGHTS"));
				eq.addSearch(Utils.getFacetsAttr(q.europeanaAPI.facets.PROVIDER, "PROVIDER"));
				eq.addSearch(Utils.getFacetsAttr(q.europeanaAPI.facets.UGC, "UGC"));
			}
			if (q.europeanaAPI.refinement != null) {
				if (q.europeanaAPI.refinement.refinementTerms != null) {
					for (String t : q.europeanaAPI.refinement.refinementTerms) {
						eq.addSearch(t);
					}
				}
				if (q.europeanaAPI.refinement.spatialParams != null) {

					if (q.europeanaAPI.refinement.spatialParams.latitude != null) {
						eq.addSearch(new Utils.Pair<String>("pl_wgs84_pos_lat", "["
								+ q.europeanaAPI.refinement.spatialParams.latitude.startPoint + "+TO+"
								+ q.europeanaAPI.refinement.spatialParams.latitude.endPoint + "]"));
					}
					if (q.europeanaAPI.refinement.spatialParams.longitude != null) {
						eq.addSearch(new Utils.Pair<String>("pl_wgs84_pos_long", "["
								+ q.europeanaAPI.refinement.spatialParams.longitude.startPoint + "+TO+"
								+ q.europeanaAPI.refinement.spatialParams.longitude.endPoint + "]"));
					}
				}
			}

			if (q.europeanaAPI.reusability != null) {
				eq.addSearchParam("reusability", Utils.getORList(q.europeanaAPI.reusability));
			}
		}
	}

	public String getSourceName() {
		return "Europeana";
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
			res.totalCount = Utils.readIntAttr(response, "totalResults", true);
			res.count = Utils.readIntAttr(response, "itemsCount", true);
			ArrayList<ItemsResponse> a = new ArrayList<ItemsResponse>();
			if (response.path("success").asBoolean()) {
				for (JsonNode item : response.path("items")) {
					ItemsResponse it = new ItemsResponse();
					String t = Utils.readAttr(item, "type", false);
					countValue(type, t);
					it.id = Utils.readAttr(item, "id", true);
					it.thumb = Utils.readArrayAttr(item, "edmPreview", false);
					it.fullresolution = Utils.readArrayAttr(item, "edmIsShownBy", false);
					it.title = Utils.readLangAttr(item, "title", false);
					it.description = Utils.readLangAttr(item, "dcDescription", false);
					it.creator = Utils.readLangAttr(item, "dcCreator", false);
					it.year = Utils.readArrayAttr(item, "year", false);
					it.dataProvider = Utils.readLangAttr(item, "dataProvider", false);
					it.url = new MyURL();
					it.url.original = Utils.readArrayAttr(item, "edmIsShownAt", false);
					it.url.fromSourceAPI = Utils.readAttr(item, "guid", false);
					a.add(it);
				}
			}
			res.items = a;
			res.facets = response.path("facets");
			res.filters = new ArrayList<>();
			res.filters.add(type);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// protected void countValue(CommonFilterResponse type, String t) {
		// type.addValue(vmap.translateToCommon(type.filterID, t));
		// }
		return res;
	}

	public String autocompleteQuery(String term, int limit) {
		return "http://www.europeana.eu/api/v2/suggestions.json?rows=" + limit + "&phrases=false&query=" + term;
	}

	public AutocompleteResponse autocompleteResponse(String response) {
		try {
			JSONObject jsonResp = new JSONObject(response);
			if (!jsonResp.getBoolean("success"))
				return new AutocompleteResponse();
			else {
				JSONArray items = jsonResp.getJSONArray("items");
				AutocompleteResponse ar = new AutocompleteResponse();
				ar.suggestions = new ArrayList<Suggestion>();
				for (int i = 0; i < items.length(); i++) {
					JSONObject item = items.getJSONObject(i);
					Suggestion s = new Suggestion();
					s.value = item.getString("term");
					DataJSON data = new DataJSON();
					data.category = "Europeana";
					data.frequencey = item.getInt("frequency");
					data.field = item.getString("field");
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

	public ArrayList<RecordJSONMetadata> getRecordFromSource(String recordId) {
		String key = "SECRET_KEY";
		ArrayList<RecordJSONMetadata> jsonMetadata = new ArrayList<RecordJSONMetadata>();
		JsonNode response;
		try {
			response = HttpConnector.getURLContent("http://www.europeana.eu/api/v2/record/" + recordId + ".json?wskey="
					+ key);
			JsonNode record = response.get("object");
			jsonMetadata.add(new RecordJSONMetadata(Format.JSON, record.toString()));
			response = HttpConnector.getURLContent("http://www.europeana.eu/api/v2/record/" + recordId
					+ ".jsonld?wskey=" + key);
			record = response;
			jsonMetadata.add(new RecordJSONMetadata(Format.JSONLD, record.toString()));
			return jsonMetadata;
		} catch (IOException e) {
			e.printStackTrace();
			return jsonMetadata;
		}
	}

}
