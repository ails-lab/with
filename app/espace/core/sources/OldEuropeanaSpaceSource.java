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
import java.util.function.Function;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import utils.ListUtils;

import com.fasterxml.jackson.databind.JsonNode;

import espace.core.AdditionalQueryModifier;
import espace.core.AutocompleteResponse;
import espace.core.AutocompleteResponse.DataJSON;
import espace.core.AutocompleteResponse.Suggestion;
import espace.core.CommonFilter;
import espace.core.CommonFilterLogic;
import espace.core.CommonFilters;
import espace.core.CommonQuery;
import espace.core.EuropeanaQuery;
import espace.core.FacetsModes;
import espace.core.HttpConnector;
import espace.core.ISpaceSource;
import espace.core.QueryBuilder;
import espace.core.QueryModifier;
import espace.core.RecordJSONMetadata;
import espace.core.RecordJSONMetadata.Format;
import espace.core.SourceResponse;
import espace.core.Utils;
import espace.core.Utils.Pair;
import espace.core.sources.formatreaders.EuropeanaExternalBasicRecordFormatter;
import model.ExternalBasicRecord;
import model.ExternalBasicRecord.ItemRights;
import model.ExternalBasicRecord.RecordType;

public class OldEuropeanaSpaceSource extends ISpaceSource {

	public static final String LABEL = "Europeana";
	private String europeanaKey = "ANnuDzRpW";
	private EuropeanaExternalBasicRecordFormatter formatreader;

	public OldEuropeanaSpaceSource() {
		super();
		formatreader = new EuropeanaExternalBasicRecordFormatter();
		addDefaultWriter(CommonFilters.PROVIDER.getID(), qfwriter("PROVIDER"));
		addDefaultWriter(CommonFilters.DATA_PROVIDER.getID(), qfwriter("DATA_PROVIDER"));
		addDefaultWriter(CommonFilters.COUNTRY.getID(), qfwriter("COUNTRY"));

		addDefaultWriter(CommonFilters.YEAR.getID(), qfwriterYEAR());

		addDefaultWriter(CommonFilters.CREATOR.getID(), qfwriter("CREATOR"));

		// addDefaultWriter(CommonFilters.CONTRIBUTOR.getID(),
		// qfwriter("proxy_dc_contributor"));

		addDefaultQueryModifier(CommonFilters.RIGHTS.getID(), qrightwriter());

		addDefaultWriter(CommonFilters.TYPE.getID(), qfwriter("TYPE"));

		addMapping(CommonFilters.TYPE.getID(), RecordType.IMAGE.toString(), "IMAGE");
		addMapping(CommonFilters.TYPE.getID(), RecordType.VIDEO.toString(), "VIDEO");
		addMapping(CommonFilters.TYPE.getID(), RecordType.SOUND.toString(), "SOUND");
		addMapping(CommonFilters.TYPE.getID(), RecordType.TEXT.toString(), "TEXT");

		addMapping(CommonFilters.RIGHTS.getID(), ItemRights.Creative.toString(), ".*creative.*");
		addMapping(CommonFilters.RIGHTS.getID(), ItemRights.Commercial.toString(), ".*creative(?!.*nc).*");
		addMapping(CommonFilters.RIGHTS.getID(), ItemRights.Modify.toString(), ".*creative(?!.*nd).*");
		addMapping(CommonFilters.RIGHTS.getID(), ItemRights.RR.toString(), ".*rr-.*");
		addMapping(CommonFilters.RIGHTS.getID(), ItemRights.UNKNOWN.toString(), ".*unknown.*");

	}

	private Function<List<String>, QueryModifier> qreusabilitywriter() {
		Function<String, String> function = (String s) -> {
			return "&REUSABILITY%3A" + s;
		};
		return new Function<List<String>, QueryModifier>() {
			@Override
			public AdditionalQueryModifier apply(List<String> t) {
				return new AdditionalQueryModifier("%20" + Utils.getORList(ListUtils.transform(t, function), false));
			}
		};
	}

	private Function<List<String>, Pair<String>> qfwriter(String parameter) {
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

	class euroQB extends QueryBuilder {

		public euroQB() {
			super();
		}

		public euroQB(String baseUrl) {
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

	public String getHttpQuery(CommonQuery q) {
		QueryBuilder builder = new euroQB("http://europeana.eu/api/v2/search.json");
		builder.addSearchParam("wskey", europeanaKey);

		builder.addQuery("query", q.searchTerm);

		builder.addSearchParam("start", "" + ((Integer.parseInt(q.page) - 1) * Integer.parseInt(q.pageSize) + 1));

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
		CommonFilterLogic type = new CommonFilterLogic(CommonFilters.TYPE);
		CommonFilterLogic provider = new CommonFilterLogic(CommonFilters.PROVIDER);
		CommonFilterLogic dataprovider = new CommonFilterLogic(CommonFilters.DATA_PROVIDER);
		CommonFilterLogic creator =new CommonFilterLogic(CommonFilters.CREATOR);
		CommonFilterLogic rights = new CommonFilterLogic(CommonFilters.RIGHTS);
		CommonFilterLogic country = new CommonFilterLogic(CommonFilters.COUNTRY);
		CommonFilterLogic year = new CommonFilterLogic(CommonFilters.YEAR);
		if (checkFilters(q)) {
			try {
				response = HttpConnector.getURLContent(httpQuery);
				res.totalCount = Utils.readIntAttr(response, "totalResults", true);
				System.out.println("europeana got " + res.totalCount);
				res.count = Utils.readIntAttr(response, "itemsCount", true);
				ArrayList<ExternalBasicRecord> a = new ArrayList<>();
				if (response.path("success").asBoolean()) {
					for (JsonNode item : response.path("items")) {
						a.add(formatreader.readObjectFrom(item));
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

				res.filtersLogic = new ArrayList<>();
				res.filtersLogic.add(type);
				res.filtersLogic.add(provider);
				res.filtersLogic.add(dataprovider);
				res.filtersLogic.add(creator);
				res.filtersLogic.add(rights);
				res.filtersLogic.add(country);
				res.filtersLogic.add(year);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}

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
		String key = "ANnuDzRpW";
		ArrayList<RecordJSONMetadata> jsonMetadata = new ArrayList<RecordJSONMetadata>();
		JsonNode response;
		try {
			response = HttpConnector
					.getURLContent("http://www.europeana.eu/api/v2/record/" + recordId + ".json?wskey=" + key);
			JsonNode record = response.get("object");
			if (response != null){
				jsonMetadata.add(new RecordJSONMetadata(Format.JSON_EDM, record.toString()));
			
				JsonNode item = record;
				
			
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
