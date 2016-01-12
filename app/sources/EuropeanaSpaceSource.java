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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;

import model.EmbeddedMediaObject.WithMediaRights;
import model.EmbeddedMediaObject.WithMediaType;
import model.ExternalBasicRecord.ItemRights;
import model.ExternalBasicRecord.RecordType;
import model.Provider.Sources;
import model.resources.WithResource;
import sources.core.AdditionalQueryModifier;
import sources.core.AutocompleteResponse;
import sources.core.CommonFilterLogic;
import sources.core.CommonFilters;
import sources.core.CommonQuery;
import sources.core.FacetsModes;
import sources.core.HttpConnector;
import sources.core.ISpaceSource;
import sources.core.QueryBuilder;
import sources.core.QueryModifier;
import sources.core.RecordJSONMetadata;
import sources.core.SourceResponse;
import sources.core.Utils;
import sources.core.AutocompleteResponse.DataJSON;
import sources.core.AutocompleteResponse.Suggestion;
import sources.core.RecordJSONMetadata.Format;
import sources.core.Utils.Pair;
import sources.formatreaders.EuropeanaRecordFormatter;
import utils.ListUtils;

public class EuropeanaSpaceSource extends ISpaceSource {


	public EuropeanaSpaceSource() {
		super();
		LABEL = Sources.Europeana.toString();
		apiKey = "SECRET_KEY";
		formatreader = new EuropeanaRecordFormatter();
		
		
		addDefaultWriter(CommonFilters.PROVIDER.name(), qfwriter("PROVIDER"));
		addDefaultWriter(CommonFilters.DATA_PROVIDER.name(), qfwriter("DATA_PROVIDER"));
		addDefaultWriter(CommonFilters.COUNTRY.name(), qfwriter("COUNTRY"));

		addDefaultWriter(CommonFilters.YEAR.name(), qfwriterYEAR());

		addDefaultWriter(CommonFilters.CREATOR.name(), qfwriter("CREATOR"));

		// addDefaultWriter(CommonFilters.CONTRIBUTOR_ID,
		// qfwriter("proxy_dc_contributor"));

		addDefaultQueryModifier(CommonFilters.RIGHTS.name(), qrightwriter());

		addDefaultWriter(CommonFilters.TYPE.name(), qfwriter("TYPE"));

		addMapping(CommonFilters.TYPE.name(), WithMediaType.IMAGE, "IMAGE");
		addMapping(CommonFilters.TYPE.name(), WithMediaType.VIDEO, "VIDEO");
		addMapping(CommonFilters.TYPE.name(), WithMediaType.AUDIO, "SOUND");
		addMapping(CommonFilters.TYPE.name(), WithMediaType.TEXT, "TEXT");

//		addDefaultQueryModifier(CommonFilters.REUSABILITY_ID, qreusabilitywriter());

		
		
//	    filtersSupportedBySource = new ArrayList<CommonFilters>(
//	    		Arrays.asList(CommonFilters.PROVIDER, CommonFilters.COUNTRY, CommonFilters.CREATOR,
//	    				CommonFilters.DATA_PROVIDER, CommonFilters.PROVIDER, CommonFilters.RIGHTS,
//	    				CommonFilters.TYPE, CommonFilters.YEAR)
//	    		);
//	    sourceToFiltersMappings = new HashMap<String, CommonFilters>(){{
//	    		for (CommonFilters filter: filtersSupportedBySource) {
//	    			put(filter.name(), filter);
//	    		}
//	    	}};
//	    filtersToSourceMappings = new HashMap<CommonFilters, String>(){{
//	    		for (String key: sourceToFiltersMappings.keySet()) {
//	    			put(sourceToFiltersMappings.get(key), key);
//	    		}
//	    	}};
//		for (CommonFilters filterType: filtersSupportedBySource) {
//			addDefaultWriter(filterType.name(), qfwriter(filtersToSourceMappings.get(filterType)));
//		}
//		for (RecordType type: RecordType.values()) {
//			addMapping(CommonFilters.TYPE.name(), type.name(), type.name());
//		}

		addMapping(CommonFilters.RIGHTS.name(), WithMediaRights.Creative, ".*creative.*");
		addMapping(CommonFilters.RIGHTS.name(), WithMediaRights.Commercial, ".*creative(?!.*nc).*");
		addMapping(CommonFilters.RIGHTS.name(), WithMediaRights.Modify, ".*creative(?!.*nd).*");
		addMapping(CommonFilters.RIGHTS.name(), WithMediaRights.RR, ".*rr-.*");
		addMapping(CommonFilters.RIGHTS.name(), WithMediaRights.UNKNOWN, ".*unknown.*");
		formatreader.setMap(this.vmap);
	}
	
	private Function<List<String>, QueryModifier> qrightwriter() {
		Function<String, String> function = (String s) -> {
			s = s.replace("(?!.*nc)", "*%20NOT%20*nc");
			s = s.replace("(?!.*nd)", "*%20NOT%20*nd");
			return "RIGHTS%3A%28" + s.replace(".", "") + "%29";
		};
		return new Function<List<String>, QueryModifier>() {
			@Override
			public AdditionalQueryModifier apply(List<String> t) {
				return new AdditionalQueryModifier("%20" + Utils.getORList(ListUtils.transform(t, function), false));
			}
		};
	}
	
	private Function<List<String>, Pair<String>> qfwriter(String parameter) {
		if (parameter.equals(CommonFilters.YEAR.name())) {
			return qfwriterYEAR();
		}
		Function<String, String> function = (String s) -> {
			return "%22" + Utils.spacesFormatQuery(s, "%20") + "%22";
		};
		return new Function<List<String>, Pair<String>>() {
			@Override
			public Pair<String> apply(List<String> t) {
				return new Pair<String>("qf", parameter + "%3A" + Utils.getORList(ListUtils.transform(t, function)));
			}
		};
	}

	private Function<List<String>, Pair<String>> qfwriterYEAR() {
		return new Function<List<String>, Pair<String>>() {
			@Override
			public Pair<String> apply(List<String> t) {
				String val = "%22" + t.get(0) + "%22";
				if (t.size() > 1) {
					val = "%5B" + val + "%20TO%20%22" + t.get(1) + "%22%5D";
				}
				return new Pair<String>("qf", "YEAR%3A" + val);
			}
		};
	}

	class EuroQB extends QueryBuilder {

		public EuroQB() {
			super();
		}

		public EuroQB(String baseUrl) {
			super(baseUrl);
		}

		public String getHttp() {
			String res = getBaseUrl();
			Iterator<Pair<String>> it = parameters.iterator();
			boolean skipqf = false;
			boolean added = false;
			if (query.second != null) {
				res += ("?" + query.getHttp());
				added = true;
			} else {
				skipqf = true;
			}
			for (; it.hasNext();) {

				Pair<String> next = it.next();
				String string = added ? "&" : "?";
				if (next.first.equals("qf") && skipqf) {
					skipqf = false;
					res += (string + "query=" + next.second);
					added = true;
				} else {
					added = true;
					res += string + next.getHttp();
				}
			}
			return res;
		}

	}

	public String getHttpQuery(CommonQuery q) {
		QueryBuilder builder = new EuroQB("http://europeana.eu/api/v2/search.json");
		builder.addSearchParam("wskey", apiKey);

		builder.addQuery("query", q.searchTerm);

		builder.addSearchParam("start", "" + (((Integer.parseInt(q.page) - 1) * Integer.parseInt(q.pageSize)) + 1));

		builder.addSearchParam("rows", "" + q.pageSize);
		builder.addSearchParam("profile", "rich%20facets");
		String facets = "DEFAULT";
		if (q.facetsMode != null) {
			switch (q.facetsMode) {
			case FacetsModes.SOME:
				facets = "proxy_dc_creator," + facets;
				break;
			case FacetsModes.ALL:
				facets = "proxy_dc_creator,proxy_dc_contributor," + facets;
				break;
			default:
				break;
			}
		}
		builder.addSearchParam("facet", facets);
		return addfilters(q, builder).getHttp();
	}



	public List<CommonFilterLogic> createFilters(JsonNode response) {
		
		
		CommonFilterLogic type = new CommonFilterLogic(CommonFilters.TYPE);
		CommonFilterLogic provider = new CommonFilterLogic(CommonFilters.PROVIDER);
		CommonFilterLogic dataprovider = new CommonFilterLogic(CommonFilters.DATA_PROVIDER);
		CommonFilterLogic creator = new CommonFilterLogic(CommonFilters.CREATOR);
		CommonFilterLogic rights = new CommonFilterLogic(CommonFilters.RIGHTS);
		CommonFilterLogic country = new CommonFilterLogic(CommonFilters.COUNTRY);
		CommonFilterLogic year = new CommonFilterLogic(CommonFilters.YEAR);
		
		List<CommonFilterLogic> filters = new ArrayList<CommonFilterLogic>();
		for (JsonNode facet : response.path("facets")) {
			String filterType = facet.path("name").asText();
				for (JsonNode jsonNode : facet.path("fields")) {
					String label = jsonNode.path("label").asText();
					int count = jsonNode.path("count").asInt();
					switch (filterType) {
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
						countValue(rights, label, count);
						break;

					case "proxy_dc_creator":
						countValue(creator, label, false, count);
						break;
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
		filters.add(type);
		filters.add(provider);
		filters.add(dataprovider);
		filters.add(creator);
		filters.add(rights);
		filters.add(country);
		filters.add(year);
		return filters;
	}

	public ArrayList<WithResource<?>> getItems(JsonNode response) {
		ArrayList<WithResource<?>> items = new ArrayList<>();
		try{
		if (response.path("success").asBoolean()) {
			for (JsonNode item : response.path("items")) {
				items.add(formatreader.readObjectFrom(item));
			}
		}} catch(Exception e){
			e.printStackTrace();
		}
		
		return items;
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		SourceResponse res = new SourceResponse();
		res.source = getSourceName();
		String httpQuery = getHttpQuery(q);
		res.query = httpQuery;
		JsonNode response;
		if (checkFilters(q)) {
			try {
				response = HttpConnector.getURLContent(httpQuery);
				res.totalCount = Utils.readIntAttr(response, "totalResults", true);
				res.count = Utils.readIntAttr(response, "itemsCount", true);
				res.items.setCulturalHO(getItems(response));;
//				res.facets = response.path("facets");
				res.filtersLogic = createFilters(response);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
		}
		return res;
	}

	public String autocompleteQuery(String term, int limit) {
		return "http://www.europeana.eu/api/v2/suggestions.json?rows=" + limit + "&phrases=false&query=" + term;
	}

	public AutocompleteResponse autocompleteResponse(String response) {
		try {
			JSONObject jsonResp = new JSONObject(response);
			if ((jsonResp == null) || !jsonResp.getBoolean("success") || (jsonResp.getJSONArray("items") == null))
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
			response = HttpConnector
					.getURLContent("http://www.europeana.eu/api/v2/record/" + recordId + ".json?wskey=" + key);
			JsonNode record = response.get("object");
			if (response != null){
				jsonMetadata.add(new RecordJSONMetadata(Format.JSON_EDM, record.toString()));
			}
			response = HttpConnector
					.getURLContent("http://www.europeana.eu/api/v2/record/" + recordId + ".jsonld?wskey=" + key);
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
