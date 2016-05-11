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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;

import model.basicDataTypes.ProvenanceInfo.Sources;
import model.resources.CulturalObject;
import model.resources.RecordResource;
import model.resources.WithResource;
import play.Logger;
import play.libs.Json;
import sources.core.AdditionalQueryModifier;
import sources.core.AutocompleteResponse;
import sources.core.AutocompleteResponse.DataJSON;
import sources.core.AutocompleteResponse.Suggestion;
import sources.core.CommonFilterLogic;
import sources.core.CommonFilters;
import sources.core.CommonQuery;
import sources.core.FacetsModes;
import sources.core.HttpConnector;
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
import sources.formatreaders.HistorypinItemRecordFormatter;
import sources.formatreaders.HistorypinRecordFormatter;
import sources.utils.FunctionsUtils;
import utils.ListUtils;

public class HistorypinSpaceSource extends ISpaceSource {
	
	private boolean usingCursor = false;
	private String nextCursor;
	
	public HistorypinSpaceSource() {
		super(Sources.Historypin);
		vmap = FilterValuesMap.getHistorypinMap();
		addDefaultWriter(CommonFilters.TYPE.getId(), qfwriter("pin"));
//		addDefaultWriter(CommonFilters.MIME_TYPE.getId(), qfwriter("MIME_TYPE"));
//		addDefaultWriter(CommonFilters.IMAGE_SIZE.getId(), qfwriter("IMAGE_SIZE"));
//		addDefaultWriter(CommonFilters.IMAGE_COLOUR.getId(), qfwriter("IMAGE_COLOUR"));
//		addDefaultWriter(CommonFilters.COLOURPALETE.getId(), qfwriter("COLOURPALETE"));
//		
//		
//		addDefaultWriter(CommonFilters.PROVIDER.getId(), qfwriter("PROVIDER"));
//		addDefaultWriter(CommonFilters.DATA_PROVIDER.getId(), qfwriter("DATA_PROVIDER"));
//		addDefaultWriter(CommonFilters.COUNTRY.getId(), qfwriter("COUNTRY"));
//
//		addDefaultWriter(CommonFilters.YEAR.getId(), qfwriterYEAR());
//
//		addDefaultWriter(CommonFilters.CREATOR.getId(), qfwriter("CREATOR"));

		// addDefaultWriter(CommonFilters.CONTRIBUTOR_ID,
		// qfwriter("proxy_dc_contributor"));

//		addDefaultQueryModifier(CommonFilters.RIGHTS.getId(), qrightwriter());

//		addDefaultWriter(CommonFilters.TYPE.getId(), qfwriter("TYPE"));

		formatreader = new HistorypinRecordFormatter();

	}

	private void addIdentityWriter(String filterId) {
		addDefaultWriter(filterId, qfwriter(filterId));
	}

	private Function<List<String>, QueryModifier> qrightwriter() {
		Function<String, String> function = (String s) -> {
//			s = s.replace("(?!.*nc)", "*%20NOT%20*nc");
//			s = s.replace("(?!.*nd)", "*%20NOT%20*nd");
//			return "RIGHTS%3A%28" + s.replace(".", "") + "%29";
			s = s.replace("(?!.*nc)", "* NOT *nc");
			s = s.replace("(?!.*nd)", "* NOT *nd");
			return "RIGHTS:(" + s.replace(".", "") + ")";
		};
		return new Function<List<String>, QueryModifier>() {
			@Override
			public AdditionalQueryModifier apply(List<String> t) {
				return new AdditionalQueryModifier(" " + Utils.getORList(ListUtils.transform(t, function), false));
			}
		};
	}

	private Function<List<String>, Pair<String>> qfwriter(String parameter) {
		if (parameter.equals(CommonFilters.YEAR.name())) {
			return qfwriterYEAR();
		}
//		Function<String, String> function = (String s) -> {
//			return "\"" + s+ "\"";
//		};
//		return new Function<List<String>, Pair<String>>() {
//			@Override
//			public Pair<String> apply(List<String> t) {
//				return new Pair<String>("qf", parameter + ":" + Utils.getORList(ListUtils.transform(t, function)));
//			}
//		};
		return FunctionsUtils.toORList("qf", 
				(s)-> parameter + ":" + FunctionsUtils.smartquote().apply(s)
				);
	}

	private Function<List<String>, Pair<String>> qfwriterYEAR() {
		return new Function<List<String>, Pair<String>>() {
			@Override
			public Pair<String> apply(List<String> t) {
				String val = "\"" + t.get(0) + "\"";
				if (t.size() > 1) {
					val = "[" + val + " TO \"" + t.get(1) + "\"]";
				}
				return new Pair<String>("qf", "YEAR:" + val);
			}
		};
	}

	public String getHttpQuery(CommonQuery q) {
		QueryBuilder builder = new QueryBuilder("http://www.historypin.org/en/api/pin/listing.json");
//		builder.addSearchParam("wskey", apiKey);

//		builder.addQuery("keyword", q.searchTerm);

		builder.addSearchParam("page", "" +q.page);
		builder.addSearchParam("limit", "" + q.pageSize);
		builder.add(new Pair<String>("keyword", q.searchTerm){
			public String getHttp(boolean encode) {
				String string = first + ":" + second.toString();
				if (encode){
					try {
						String encoded = URLEncoder.encode(second.toString(), "UTF-8");
						string = first + ":" + encoded;
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
				return string;
			}

		});
		
//		builder.addSearchParam("profile", "rich facets");
//		String facets = "DEFAULT";
//		if (q.facetsMode != null) {
//			switch (q.facetsMode) {
//			case FacetsModes.SOME:
//				facets = "proxy_dc_creator," + facets;
//				break;
//			case FacetsModes.ALL:
//				facets = "proxy_dc_creator,proxy_dc_contributor," + 
//			             "MIME_TYPE,IMAGE_SIZE,IMAGE_COLOUR,IMAGE_GREYSCALE,"+
//						facets;
//				break;
//			default:
//				break;
//			}
//		}
//		if (q.hasMedia)
//			builder.addSearchParam("media", "true");
////		builder.addSearchParam("facet", facets);
//		builder.setTail(q.tail);
		return addfilters(q, builder).getHttp();
	}

	public List<CommonFilterLogic> createFilters(JsonNode response) {
		
//		List<CommonFilterLogic> filters = new ArrayList<CommonFilterLogic>();
//		for (JsonNode facet : response.path("facets")) {
//			String filterType = facet.path("name").asText();
//			CommonFilters withFilter = CommonFilters.valueOf(filterType);//sourceToFiltersMappings.get(filterType);
//			if (withFilter != null) {
//				CommonFilterLogic filter = new CommonFilterLogic(withFilter);
//				for (JsonNode jsonNode : facet.path("fields")) {
//					String label = jsonNode.path("label").asText();
//					int count = jsonNode.path("count").asInt();
//					switch (filterType) {
//						case "TYPE": 
//						case "RIGHTS":
//							countValue(filter, label, count);
//							break;
//						case "DATA_PROVIDER": 
//						case "PROVIDER":
//						case "proxy_dc_creator":
//						case "COUNTRY":
//						case "YEAR":
//							countValue(filter, label, false, count);
//							break;
//						default:
//							break;
//					}
//					filters.add(filter);
//				}
//			}
//		}
//		return filters;

		CommonFilterLogic type = new CommonFilterLogic(CommonFilters.TYPE);
		CommonFilterLogic provider = new CommonFilterLogic(CommonFilters.PROVIDER);
		CommonFilterLogic dataprovider = new CommonFilterLogic(CommonFilters.DATA_PROVIDER);
		CommonFilterLogic creator = new CommonFilterLogic(CommonFilters.CREATOR);
		CommonFilterLogic rights = new CommonFilterLogic(CommonFilters.RIGHTS);
		CommonFilterLogic country = new CommonFilterLogic(CommonFilters.COUNTRY);
		CommonFilterLogic year = new CommonFilterLogic(CommonFilters.YEAR);
		
		CommonFilterLogic mtype = new CommonFilterLogic(CommonFilters.MIME_TYPE);
		CommonFilterLogic isize = new CommonFilterLogic(CommonFilters.IMAGE_SIZE);
		CommonFilterLogic icolor = new CommonFilterLogic(CommonFilters.IMAGE_COLOUR);
		CommonFilterLogic cpalete = new CommonFilterLogic(CommonFilters.COLOURPALETE);
				
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
		filters.add(type);
		filters.add(provider);
		filters.add(dataprovider);
		filters.add(creator);
		filters.add(rights);
		filters.add(country);
		filters.add(year);
		filters.add(mtype);
		filters.add(isize);
		filters.add(icolor);
		filters.add(cpalete);
		return filters;
	}

	public ArrayList<WithResource<?, ?>> getItems(JsonNode response) {
		ArrayList<WithResource<?, ?>> items = new ArrayList<>();
		try {
				for (JsonNode item : response.path("results")) {
					items.add(formatreader.readObjectFrom(item));
				}
		} catch (Exception e) {
			e.printStackTrace();
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
				res.totalCount = Utils.readIntAttr(response, "count", true);
				res.count = Utils.readIntAttr(response, "limit", true);
				res.items.setCulturalCHO(getItems(response));
				// res.facets = response.path("facets");
				//res.filtersLogic = createFilters(response);
				if (usingCursor) {
					nextCursor = Utils.readAttr(response, "nextCursor", true);
					if (!Utils.hasInfo(nextCursor))
						Logger.error("cursor error!!");
				}
				

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return res;
	}

	


	@Override
	public ArrayList<RecordJSONMetadata> getRecordFromSource(String recordId, RecordResource fullRecord) {
		String key = "ANnuDzRpW";
		ArrayList<RecordJSONMetadata> jsonMetadata = new ArrayList<RecordJSONMetadata>();
		JsonNode response;
		try {
			response = getHttpConnector()
					.getURLContent("http://www.historypin.org/en/api/pin/get.json?id=" + recordId);
			// todo read the other format;
			JsonNode record = response;
			if (response != null) {
				jsonMetadata.add(new RecordJSONMetadata(Format.JSON_Historypin, record.toString()));
				HistorypinItemRecordFormatter f = new HistorypinItemRecordFormatter();
				String json = Json.toJson(f.overwritedObjectFrom((CulturalObject)fullRecord,record)).toString();
				jsonMetadata.add(new RecordJSONMetadata(Format.JSON_WITH, json));
			}

			return jsonMetadata;
		} catch (Exception e) {
			return jsonMetadata;
		}
	}

	public boolean isUsingCursor() {
		return usingCursor;
	}

	public void setUsingCursor(boolean useCursor) {
		this.usingCursor = useCursor;
	}
	
}
