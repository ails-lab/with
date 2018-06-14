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
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;

import model.resources.RecordResource;
import model.resources.WithResource;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import search.FiltersFields;
import search.Sources;
import sources.core.AdditionalQueryModifier;
import sources.core.AutocompleteResponse;
import sources.core.AutocompleteResponse.DataJSON;
import sources.core.AutocompleteResponse.Suggestion;
import sources.core.CommonFilterLogic;
import sources.core.CommonQuery;
import sources.core.FacetsModes;
import sources.core.ISpaceSource;
import sources.core.QueryBuilder;
import sources.core.QueryModifier;
import sources.core.RecordJSONMetadata;
import sources.core.RecordJSONMetadata.Format;
import sources.core.SourceResponse;
import sources.core.Utils;
import sources.core.Utils.Pair;
import sources.formatreaders.EuropeanaItemRecordFormatter;
import sources.formatreaders.EuropeanaRecordFormatter;
import sources.utils.FunctionsUtils;
import utils.ListUtils;

public class EuropeanaSpaceSource extends ISpaceSource {
	
	public static final ALogger log = Logger.of( EuropeanaSpaceSource.class);
	
	private boolean usingCursor = false;
	private String nextCursor;
	private String profile;
	
	public String getProfile() {
		return profile;
	}

	public void setProfile(String profile) {
		this.profile = profile;
	}

	public EuropeanaSpaceSource() {
		super(Sources.Europeana);
		profile = "rich facets";
//		profile = "rich";
		apiKey = "SECRET_KEY";
		
		addDefaultWriter(FiltersFields.MIME_TYPE.getFilterId(), qfwriter("MIME_TYPE"));
		addDefaultWriter(FiltersFields.IMAGE_SIZE.getFilterId(), qfwriter("IMAGE_SIZE"));
		addDefaultWriter(FiltersFields.IMAGE_COLOUR.getFilterId(), qfwriter("IMAGE_COLOUR"));
		addDefaultWriter(FiltersFields.COLOURPALETE.getFilterId(), qfwriter("COLOURPALETE"));
		
		
		addDefaultQueryModifier(FiltersFields.PROVIDER.getFilterId(), qwriter("PROVIDER"));
		addDefaultQueryModifier(FiltersFields.DATA_PROVIDER.getFilterId(), qwriter("DATA_PROVIDER"));
		addDefaultQueryModifier(FiltersFields.COUNTRY.getFilterId(), qwriter("COUNTRY"));

		addDefaultQueryModifier(FiltersFields.YEAR.getFilterId(), qDatewriter());

		addDefaultQueryModifier(FiltersFields.CREATOR.getFilterId(), qwriter("CREATOR"));

		// addDefaultWriter(CommonFilters.CONTRIBUTOR_ID,
		// qfwriter("proxy_dc_contributor"));

		addDefaultQueryModifier(FiltersFields.RIGHTS.getFilterId(), qrightwriter());

		addDefaultQueryModifier(FiltersFields.TYPE.getFilterId(), qwriter("TYPE"));

		
		formatreader = new EuropeanaRecordFormatter();

	}

	private void addIdentityWriter(String filterId) {
		addDefaultWriter(filterId, qfwriter(filterId));
	}

	private Function<List<String>, QueryModifier> qrightwriter() {
		Function<String, String> function = (String s) -> {
			s = s.replace("(?!.*nc)", "* NOT *nc");
			s = s.replace("(?!.*nd)", "* NOT *nd");
			return "RIGHTS:(" + s.replace(".*", "*").replace(":", "\\:") + ")";
		};
		return new Function<List<String>, QueryModifier>() {
			@Override
			public AdditionalQueryModifier apply(List<String> t) {
				return new AdditionalQueryModifier(" " + Utils.getORList(ListUtils.transform(t, function), false));
			}
		};
	}
	
	private Function<List<String>, QueryModifier> qwriter(String parameter) {
		Function<String, String> function = (String s) -> {
			return parameter+":" + FunctionsUtils.smartquote().apply(s) + "";
		};
		return new Function<List<String>, QueryModifier>() {
			@Override
			public AdditionalQueryModifier apply(List<String> t) {
				return new AdditionalQueryModifier(" " + Utils.getORList(ListUtils.transform(t, function), false));
			}
		};
		
		
	}
	
	private Function<List<String>, QueryModifier> qDatewriter() {
		return new Function<List<String>, QueryModifier>() {
			@Override
			public AdditionalQueryModifier apply(List<String> t) {
				String val = dateRange(t);
				return new AdditionalQueryModifier(" YEAR:" + val);
			}
		};
		
		
	}
	

	private Function<List<String>, Pair<String>> qfwriter(String parameter) {
		if (parameter.equals(FiltersFields.YEAR.name())) {
			return qfwriterYEAR();
		}
		return FunctionsUtils.toORList("qf", 
				(s)-> parameter + ":" + FunctionsUtils.smartquote().apply(s)
				);
	}

	private Function<List<String>, Pair<String>> qfwriterYEAR() {
		return new Function<List<String>, Pair<String>>() {
			@Override
			public Pair<String> apply(List<String> t) {
				String val = dateRange(t);
				return new Pair<String>("qf", "YEAR:" + val);
			}
		};
	}

	class EuropeanaQueryBuilder extends QueryBuilder {

		public EuropeanaQueryBuilder() {
			super();
		}

		public EuropeanaQueryBuilder(String baseUrl) {
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
					query.second = next.second;
					res += (string + query.getHttp());
					added = true;
				} else {
					added = true;
					res += string + next.getHttp();
				}
			}
			if (Utils.hasInfo(tail)){
				res+=tail;
			}
			return res;
		}

	}

	public String getHttpQuery(CommonQuery q) {
		QueryBuilder builder = new EuropeanaQueryBuilder("http://www.europeana.eu/api/v2/search.json");
		builder.addSearchParam("wskey", apiKey);

		builder.setQuery("query", q.searchTerm!=null?q.searchTerm:"*");

		if (usingCursor){
			if (q.page.equals("1"))
				builder.addSearchParam("cursor", "*");
			else
				builder.addSearchParam("cursor", nextCursor);
		} else
		builder.addSearchParam("start", "" + (((Integer.parseInt(q.page) - 1) * Integer.parseInt(q.pageSize)) + 1));

		builder.addSearchParam("rows", "" + q.pageSize);
		builder.addSearchParam("profile", profile);
		String facets = "DEFAULT";
		if (q.facetsMode != null) {
			switch (q.facetsMode) {
			case FacetsModes.SOME:
				facets = "proxy_dc_creator," + facets;
				break;
			case FacetsModes.ALL:
				facets = "proxy_dc_creator,proxy_dc_contributor," + 
			             "MIME_TYPE,IMAGE_SIZE,IMAGE_COLOUR,IMAGE_GREYSCALE,"+
						facets;
				break;
			default:
				break;
			}
		}
		if (q.hasMedia)
			builder.addSearchParam("media", "true");
		builder.addSearchParam("facet", facets);
		builder.setTail(q.tail);
		return addfilters(q, builder).getHttp();
	}

	public List<CommonFilterLogic> createFilters(JsonNode response) {
		List<CommonFilterLogic> filters = new ArrayList<CommonFilterLogic>();
		CommonFilterLogic type = new CommonFilterLogic(FiltersFields.TYPE).addTo(filters);
		CommonFilterLogic provider = new CommonFilterLogic(FiltersFields.PROVIDER).addTo(filters);
		CommonFilterLogic dataprovider = new CommonFilterLogic(FiltersFields.DATA_PROVIDER).addTo(filters);
		CommonFilterLogic creator = new CommonFilterLogic(FiltersFields.CREATOR).addTo(filters);
		CommonFilterLogic rights = new CommonFilterLogic(FiltersFields.RIGHTS).addTo(filters);
		CommonFilterLogic country = new CommonFilterLogic(FiltersFields.COUNTRY).addTo(filters);
		CommonFilterLogic year = new CommonFilterLogic(FiltersFields.YEAR).addTo(filters);
		CommonFilterLogic mtype = new CommonFilterLogic(FiltersFields.MIME_TYPE).addTo(filters);
		CommonFilterLogic isize = new CommonFilterLogic(FiltersFields.IMAGE_SIZE).addTo(filters);
		CommonFilterLogic icolor = new CommonFilterLogic(FiltersFields.IMAGE_COLOUR).addTo(filters);
		CommonFilterLogic cpalete = new CommonFilterLogic(FiltersFields.COLOURPALETE).addTo(filters);
				
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
				case "MIME_TYPE":
					countValue(mtype, label, false, count);
					break;
				case "IMAGE_SIZE":
					countValue(isize, label, false, count);
					break;
				case "IMAGE_COLOUR":
					countValue(icolor, label, false, count);
					break;
				case "COLOURPALETE":
					countValue(cpalete, label, false, count);
					break;

				default:
					break;
				}

			}
		}
		return filters;
	}

	public ArrayList<WithResource<?, ?>> getItems(JsonNode response) {
		ArrayList<WithResource<?, ?>> items = new ArrayList<>();
		try {
			if (response.path("success").asBoolean()) {
				for (JsonNode item : response.path("items")) {
					items.add(formatreader.readObjectFrom(item));
				}
			}
		} catch (Exception e) {
			log.error("",e);
		}

		return items;
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		SourceResponse res = new SourceResponse();
		res.source = getSourceName().toString();
		String httpQuery = getHttpQuery(q);
		res.query = httpQuery;
		JsonNode response;
		if (checkFilters(q)) {
			try {
				response = getHttpConnector().getURLContent(httpQuery);
				res.totalCount = Utils.readIntAttr(response, "totalResults", true);
				res.count = Utils.readIntAttr(response, "itemsCount", true);
				res.items.setCulturalCHO(getItems(response));
				// res.facets = response.path("facets");
				res.filtersLogic = createFilters(response);
				if (usingCursor) {
					nextCursor = Utils.readAttr(response, "nextCursor", true);
					if (!Utils.hasInfo(nextCursor)){
						Logger.error("cursor error!!");
						Logger.error("response--->"+response.toString());
						Logger.error("[again]response--->"+getHttpConnector().getURLContent(httpQuery));
						res.error = true;
					}
				}
				

			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error( "", e );
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
			log.error("", e);
			return new AutocompleteResponse();
		}
	}

	@Override
	public ArrayList<RecordJSONMetadata> getRecordFromSource(String recordId, RecordResource fullRecord) {
		recordId = clean(recordId);
		String key = "SECRET_KEY";
		ArrayList<RecordJSONMetadata> jsonMetadata = new ArrayList<RecordJSONMetadata>();
		JsonNode response;
		try {
			response = getHttpConnector()
					.getURLContent("http://www.europeana.eu/api/v2/record/" + recordId + ".json?wskey=" + key);
			// todo read the other format;
			JsonNode record = response.get("object");
			if (response != null) {
//				jsonMetadata.add(new RecordJSONMetadata(Format.JSON_EDM, record.toString()));
				EuropeanaItemRecordFormatter f = new EuropeanaItemRecordFormatter();
				String json = Json.toJson(f.overwriteObjectFrom(fullRecord,record)).toString();
				jsonMetadata.add(new RecordJSONMetadata(Format.JSON_WITH, json));
//				RecordQuality q = new RecordQuality();
//				q.compute(new JsonContextRecord(json));
			}
			response = getHttpConnector()
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

	private String clean(String recordId) {
		if (recordId.contains("/aggregation/provider/"))
		return recordId.replace("/aggregation/provider/", "/");
		else return recordId;
	}

	public boolean isUsingCursor() {
		return usingCursor;
	}

	public void setUsingCursor(boolean useCursor) {
		this.usingCursor = useCursor;
	}

	private String dateRange(List<String> t) {
		String val = "\"" + t.get(0) + "\"";
		if (t.size() > 1) {
			val = "[" + val + " TO \"" + t.get(1) + "\"]";
		}
		return val;
	}

	@Override
	public String apiConsole() {
		return "http://labs.europeana.eu/api/console/?function=search&query=example";
	}
}
