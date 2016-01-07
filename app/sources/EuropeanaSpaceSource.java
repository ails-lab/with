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
	    filtersSupportedBySource = new ArrayList<CommonFilters>(
	    		Arrays.asList(CommonFilters.PROVIDER, CommonFilters.COUNTRY, CommonFilters.CREATOR,
	    				CommonFilters.DATA_PROVIDER, CommonFilters.PROVIDER, CommonFilters.RIGHTS,
	    				CommonFilters.TYPE, CommonFilters.YEAR)
	    		);
	    sourceToFiltersMappings = new HashMap<String, CommonFilters>(){{
	    		for (CommonFilters filter: filtersSupportedBySource) {
	    			put(filter.name(), filter);
	    		}
	    	}};
	    filtersToSourceMappings = new HashMap<CommonFilters, String>(){{
	    		for (String key: sourceToFiltersMappings.keySet()) {
	    			put(sourceToFiltersMappings.get(key), key);
	    		}
	    	}};
		for (CommonFilters filterType: filtersSupportedBySource) {
			addDefaultWriter(filterType.name(), qfwriter(filtersToSourceMappings.get(filterType)));
		}
		for (RecordType type: RecordType.values()) {
			addMapping(CommonFilters.TYPE.name(), type.name(), type.name());
		}

		addMapping(CommonFilters.RIGHTS.name(), ItemRights.Creative.name(), ".*creative.*");
		addMapping(CommonFilters.RIGHTS.name(), ItemRights.Commercial.name(), ".*creative(?!.*nc).*");
		addMapping(CommonFilters.RIGHTS.name(), ItemRights.Modify.name(), ".*creative(?!.*nd).*");
		addMapping(CommonFilters.RIGHTS.name(), ItemRights.RR.name(), ".*rr-.*");
		addMapping(CommonFilters.RIGHTS.name(), ItemRights.UNKNOWN.name(), ".*unknown.*");

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
		List<CommonFilterLogic> filters = new ArrayList<CommonFilterLogic>();
		for (JsonNode facet : response.path("facets")) {
			String filterType = facet.path("name").asText();
			CommonFilters withFilter = sourceToFiltersMappings.get(filterType);
			if (withFilter != null) {
				CommonFilterLogic filter = new CommonFilterLogic(withFilter);
				for (JsonNode jsonNode : facet.path("fields")) {
					String label = jsonNode.path("label").asText();
					int count = jsonNode.path("count").asInt();
					switch (filterType) {
						case "TYPE":
						case "RIGHTS":
							countValue(filter, label, count);
							break;
						case "DATA_PROVIDER":
						case "PROVIDER":
						case "proxy_dc_creator":
						case "COUNTRY":
						case "YEAR":
							countValue(filter, label, false, count);
							break;
						default:
							break;
					}
					filters.add(filter);
				}
			}
		}
		return filters;
	}

	public ArrayList<WithResource<?>> getItems(JsonNode response) {
		ArrayList<WithResource<?>> items = new ArrayList<>();
		try{
		if (response.path("success").asBoolean()) {
			for (JsonNode item : response.path("items")) {
				items.add(formatreader.fillObjectFrom(item));
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
				res.items = getItems(response);
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
