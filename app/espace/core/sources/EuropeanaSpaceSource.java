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
import java.util.List;
import java.util.function.Function;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import utils.ListUtils;

import com.fasterxml.jackson.databind.JsonNode;

import espace.core.AutocompleteResponse;
import espace.core.AutocompleteResponse.DataJSON;
import espace.core.AutocompleteResponse.Suggestion;
import espace.core.CommonFilterLogic;
import espace.core.CommonFilters;
import espace.core.CommonQuery;
import espace.core.EuropeanaQuery;
import espace.core.FacetsModes;
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

public class EuropeanaSpaceSource extends ISpaceSource {

	public static final String LABEL = "Europeana";
	private String europeanaKey = "SECRET_KEY";

	public EuropeanaSpaceSource() {
		super();
		
		addDefaultWriter(CommonFilters.PROVIDER_ID, qfwriter("PROVIDER"));
		addDefaultWriter(CommonFilters.DATAPROVIDER_ID, qfwriter("DATA_PROVIDER"));
		addDefaultWriter(CommonFilters.COUNTRY_ID, qfwriter("COUNTRY"));

		addDefaultWriter(CommonFilters.YEAR_ID, qfwriterYEAR());

		addDefaultWriter(CommonFilters.CREATOR_ID, qfwriter("proxy_dc_creator"));
		
//		addDefaultWriter(CommonFilters.CONTRIBUTOR_ID, qfwriter("proxy_dc_contributor"));

		addDefaultWriter(CommonFilters.RIGHTS_ID, qfwriter("RIGHTS"));

		addDefaultWriter(CommonFilters.TYPE_ID, qfwriter("TYPE"));

		
		addMapping(CommonFilters.TYPE_ID, TypeValues.IMAGE, "IMAGE");
		addMapping(CommonFilters.TYPE_ID, TypeValues.VIDEO, "VIDEO");
		addMapping(CommonFilters.TYPE_ID, TypeValues.SOUND, "SOUND");
		addMapping(CommonFilters.TYPE_ID, TypeValues.TEXT, "TEXT");
		
		/**
		 * TODO check this 
		 */
		

		//addMapping(CommonFilters.RIGHTS_ID, RightsValues.Public, ".*(http://creativecommons.org/publicdomain/mark/1.0/ | http://creativecommons.org/publicdomain/zero/1.0/ | http://creativecommons.org/licenses/by/ | http://creativecommons.org/licenses/by-sa/).*");
		//use for commercial purposes,modify, adapt, or build upon 
		addMapping(CommonFilters.RIGHTS_ID, RightsValues.Commercial_and_Modify, ".*(creative)(?!.*nc)(?!.*nd).*");
		
		// public  = commercial and modify
		
		
		//addMapping(CommonFilters.RIGHTS_ID, RightsValues.Restricted, ".*(http://creativecommons.org/licenses/by-nc/ | http://creativecommons.org/licenses/by-nc-sa/ | http://creativecommons.org/licenses/by-nc-nd/ | http://creativecommons.org/licenses/by-nd/ | http://www.europeana.eu/rights/out-of-copyright-non-commercial/).*");
		//use for commercial purposes,
		addMapping(CommonFilters.RIGHTS_ID, RightsValues.Commercial, ".*(creative)(?!.*nc).*");
		
		// commercial is use for commercial but not modify
		//use for modify, adapt, or build upon 		
		//addMapping(CommonFilters.RIGHTS_ID, RightsValues.Permission, ".*!(http://creativecommons.org/licenses/by-nc/ | http://creativecommons.org/licenses/by-nc-sa/ | http://creativecommons.org/licenses/by-nc-nd/ | http://creativecommons.org/licenses/by-nd/ | http://creativecommons.org/publicdomain/mark/1.0/ | http://creativecommons.org/publicdomain/zero/1.0/ | http://creativecommons.org/licenses/by/ | http://creativecommons.org/licenses/by-sa/| http://www.europeana.eu/rights/out-of-copyright-non-commercial/).*");
		addMapping(CommonFilters.RIGHTS_ID, RightsValues.Modify, ".*(creative)(?!.*nd).*");
		
		addMapping(CommonFilters.RIGHTS_ID, RightsValues.Permission, "^(?!.*(screative)).*$");
		
		/*	addMapping(CommonFilters.RIGHTS_ID, RightsValues.Public, "http://creativecommons.org/publicdomain/mark/1.0/");
		addMapping(CommonFilters.RIGHTS_ID, RightsValues.Public, "http://creativecommons.org/publicdomain/zero/1.0/");
		addMapping(CommonFilters.RIGHTS_ID, RightsValues.Public, "http://creativecommons.org/licenses/by/");
		addMapping(CommonFilters.RIGHTS_ID, RightsValues.Public, "http://creativecommons.org/licenses/by-sa/");*/
		
	
		/*addMapping(CommonFilters.RIGHTS_ID, RightsValues.Restricted, "http://creativecommons.org/licenses/by-nc/");
		addMapping(CommonFilters.RIGHTS_ID, RightsValues.Restricted, "http://creativecommons.org/licenses/by-nc-sa/");
		addMapping(CommonFilters.RIGHTS_ID, RightsValues.Restricted, "http://creativecommons.org/licenses/by-nc-nd/");
		addMapping(CommonFilters.RIGHTS_ID, RightsValues.Restricted, "http://creativecommons.org/licenses/by-nd/");		
		addMapping(CommonFilters.RIGHTS_ID, RightsValues.Restricted, "http://www.europeana.eu/rights/out-of-copyright-non-commercial/");
*/
		//pemission is not *creative*  
	}

	private Function<List<String>, Pair<String>> qfwriter(String parameter) {
		Function<String, String> function =
				(String s)->{return "%22"+Utils.spacesFormatQuery(s, "%20")+"%22";};
		return new Function<List<String>, Pair<String>>() {
			@Override
			public Pair<String> apply(List<String> t) {
				return new Pair<String>("qf", parameter+"%3A" + 
						Utils.getORList(ListUtils.transform(t, 
								function)));
			}
		};
	}
	
	private Function<List<String>, Pair<String>> qfwriterYEAR() {
		return new Function<List<String>, Pair<String>>() {
			@Override
			public Pair<String> apply(List<String> t) {
				String val = "%22"+t.get(0)+"%22";
				if (t.size()>1){
					val = "%5B"+val + "%20TO%20%22"+t.get(1)+"%22%5D";
				}
				return new Pair<String>("qf", "YEAR%3A" +val);
			}
		};
	}


	public String getHttpQuery(CommonQuery q) {
		QueryBuilder builder = new QueryBuilder("http://europeana.eu/api/v2/search.json");
		builder.addSearchParam("wskey", europeanaKey);
		builder.addQuery("query", q.searchTerm);
		builder.addSearchParam("start", "" + ((Integer.parseInt(q.page) - 1) * Integer.parseInt(q.pageSize) + 1));

		builder.addSearchParam("rows", "" + q.pageSize);
		builder.addSearchParam("profile", "rich%20facets");
		String facets = "DEFAULT";
		if (q.facetsMode!=null){
			switch (q.facetsMode) {
			case FacetsModes.SOME:
				facets = "proxy_dc_creator,"+facets;
				break;
			case FacetsModes.ALL:
				facets = "proxy_dc_creator,proxy_dc_contributor,"+facets;
				break;
			default:
				break;
			}
		}
		builder.addSearchParam("facet", facets);
		return addfilters(q, builder).getHttp();
	}

	// private String getSearchTerm(CommonQuery q) {
	// if (Utils.hasAny(q.searchTerm))
	// return Utils.spacesPlusFormatQuery(q.searchTerm)
	// + (Utils.hasAny(q.termToExclude) ? "+NOT+("
	// + Utils.spacesPlusFormatQuery(q.termToExclude)
	// + ")" : "");
	// return null;
	// }

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
		return LABEL;
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
		CommonFilterLogic dataprovider = CommonFilterLogic.dataproviderFilter();
		CommonFilterLogic creator = CommonFilterLogic.creatorFilter();
		CommonFilterLogic rights = CommonFilterLogic.rightsFilter();
		CommonFilterLogic country = CommonFilterLogic.countryFilter();
		CommonFilterLogic year = CommonFilterLogic.yearFilter();
//		CommonFilterLogic contributor = CommonFilterLogic.contributorFilter();
		if (checkFilters(q)){
		try {
			response = HttpConnector.getURLContent(httpQuery);
			res.totalCount = Utils.readIntAttr(response, "totalResults", true);
			res.count = Utils.readIntAttr(response, "itemsCount", true);
			ArrayList<ItemsResponse> a = new ArrayList<ItemsResponse>();
			if (response.path("success").asBoolean()) {
				for (JsonNode item : response.path("items")) {
					ItemsResponse it = new ItemsResponse();
					String t = Utils.readAttr(item, "type", false);
					// countValue(type, t);
					it.id = Utils.readAttr(item, "id", true);
					it.thumb = Utils.readArrayAttr(item, "edmPreview", false);
					it.fullresolution = Utils.readArrayAttr(item, "edmIsShownBy", false);
					it.title = Utils.readLangAttr(item, "title", false);
					it.description = Utils.readLangAttr(item, "dcDescription", false);
					it.creator = Utils.readLangAttr(item, "dcCreator", false);
					it.year = Utils.readArrayAttr(item, "year", false);
					it.dataProvider = Utils.readLangAttr(item, "edmDataProvider",
							false);
					it.url = new MyURL();
					it.url.original = Utils.readArrayAttr(item, "edmIsShownAt",
							false);
					it.url.fromSourceAPI = Utils.readAttr(item, "guid", false);
					it.rights = Utils.readLangAttr(item, "rights", false);
					it.externalId = it.fullresolution.get(0);
					if (it.externalId == null || it.externalId == "")
						it.externalId = it.url.original.get(0);
					it.externalId = DigestUtils.md5Hex(it.externalId);
					a.add(it);
				}
			}
			res.items = a;
			res.facets = response.path("facets");

			for (JsonNode facet : response.path("facets")) {
				for (JsonNode jsonNode : facet.path("fields")) {
					String label = jsonNode.path("label").asText();
					int count = jsonNode.path("count").asInt();
					switch (facet.path("name").asText()) {
					case "TYPE":
						countValue(type, label, count);
						break;

					case "DATA_PROVIDER":
						countValue(dataprovider, label, false, count);
						break;
						
					case "PROVIDER":
						countValue(provider, label, false, count);
						break;

					case "RIGHTS":
						countValue(rights, label, false, count);
						break;

					case "proxy_dc_creator":
						countValue(creator, label, false, count);
						break;
//					case "proxy_dc_contributor":
//						countValue(contributor, label, false, count);
//						break;
					case "COUNTRY":
						countValue(country, label, false, count);
						break;

					case "YEAR":
						countValue(year, label, false, count);
						break;

					default:
						break;
					}
				}
			}

			res.filtersLogic = new ArrayList<>();
			res.filtersLogic.add(type);
			res.filtersLogic.add(provider);
			res.filtersLogic.add(dataprovider);
			res.filtersLogic.add(creator);
//			res.filtersLogic.add(contributor);
			res.filtersLogic.add(rights);
			res.filtersLogic.add(country);
			res.filtersLogic.add(year);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}}
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
			if (jsonResp == null || !jsonResp.getBoolean("success") || jsonResp.getJSONArray("items") == null)
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
			if (response != null)
				jsonMetadata.add(new RecordJSONMetadata(Format.JSON_EDM, record.toString()));
			response = HttpConnector.getURLContent("http://www.europeana.eu/api/v2/record/" + recordId
					+ ".jsonld?wskey=" + key);
			if (response != null) {
				record = response;
				jsonMetadata.add(new RecordJSONMetadata(Format.JSONLD_EDM, record.toString()));
			}
			return jsonMetadata;
		} catch (Exception e) {
			return jsonMetadata;
		}
	}

}
