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


package espace.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import espace.core.SourceResponse.ItemsResponse;
import espace.core.SourceResponse.MyURL;

public class ESpaceSource implements ISpaceSource {

	public String getHttpQuery(CommonQuery q) {
		EuropeanaQuery eq = new EuropeanaQuery();
		eq.addSearch(Utils.spacesFormatQuery(q.searchTerm)
				+ ((q.termToExclude != null) ? "+NOT+(" + Utils.spacesFormatQuery(q.termToExclude) + ")" : ""));
		eq.addSearchParam("start", "" + ((q.page - 1) * q.pageSize + 1));
		eq.addSearchParam("rows", "" + q.pageSize);
		eq.addSearchParam("profile", "rich+facets");
		euroAPI(q, eq);
		return eq.getHttp();
	}

	private String euroAPI(CommonQuery q, EuropeanaQuery eq) {
		if (q.europeanaAPI != null) {
			String res = "";
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
			return res;
		}
		return "";
	}

	public String getSourceName() {
		return "Europeana";
	}

	public List<CommonItem> getPreview(CommonQuery q) {
		ArrayList<CommonItem> res = new ArrayList<CommonItem>();
		try {
			String httpQuery = getHttpQuery(q);
			// System.out.println(httpQuery);
			JsonNode node = HttpConnector.getURLContent(httpQuery);
			JsonNode a = node.path("items");
			for (int i = 0; i < a.size(); i++) {
				JsonNode o = a.get(i);
				// System.out.println(o);
				CommonItem item = new CommonItem();
				JsonNode path = o.path("title");
				// System.out.println(path);
				item.setTitle(path.get(0).asText());
				item.seteSource(this.getSourceName());
				JsonNode path2 = o.path("edmPreview");
				// System.out.println(path2);
				if (path2 != null && path2.get(0) != null)
					item.setPreview(path2.get(0).asText());
				res.add(item);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return res;
	}

	@Override
	public Object getResults(CommonQuery q) {
		SourceResponse res = new SourceResponse();
		res.source = getSourceName();
		String httpQuery = getHttpQuery(q);
		res.query = httpQuery;
		JsonNode response;
		try {
			response = HttpConnector.getURLContent(httpQuery);
			res.totalCount = Utils.readIntAttr(response, "totalResults", true);
			res.count = Utils.readIntAttr(response, "itemsCount", true);
			ArrayList a = new ArrayList<Object>();
			if (response.path("success").asBoolean()) {
				for (JsonNode item : response.path("items")) {
					ItemsResponse it = new ItemsResponse();
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
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return res;
	}

}
