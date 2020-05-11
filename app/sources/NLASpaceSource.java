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
import java.util.List;
import java.util.function.Function;

import org.apache.http.conn.ConnectTimeoutException;
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.JsonNode;

import model.resources.RecordResource;
import model.resources.WithResource;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import search.FiltersFields;
import search.Sources;
import search.Response.Failure;
import sources.core.AdditionalQueryModifier;
import sources.core.CommonFilterLogic;
import sources.core.CommonQuery;
import sources.core.ISpaceSource;
import sources.core.QueryBuilder;
import sources.core.QueryModifier;
import sources.core.RecordJSONMetadata;
import sources.core.RecordJSONMetadata.Format;
import sources.core.SourceResponse;
import sources.core.Utils;
import sources.core.Utils.Pair;
import sources.formatreaders.NLARecordFormatter;
import utils.Serializer;

public class NLASpaceSource extends ISpaceSource {
	public static final ALogger log = Logger.of( NLASpaceSource.class);
	
	public NLASpaceSource() {
		super(Sources.NLA);
		addDefaultQueryModifier(FiltersFields.TYPE.getFilterId(), qfwriter("format"));
		addDefaultQueryModifier(FiltersFields.YEAR.getFilterId(), qfwriterYEAR());

		formatreader = new NLARecordFormatter();

	}

	private Function<List<String>, QueryModifier> qfwriterYEAR() {
		return new Function<List<String>, QueryModifier>() {
			@Override
			public AdditionalQueryModifier apply(List<String> t) {
				String a = t.get(0), b = a;

				if (t.size() > 1) {
					b = t.get(1);
				}
				return new AdditionalQueryModifier(" date:[" + a + " TO " + b + "]");
			}
		};
	}

	private Function<List<String>, QueryModifier> qfwriter(String parameter) {
		return new Function<List<String>, QueryModifier>() {
			@Override
			public AdditionalQueryModifier apply(List<String> t) {
				return new AdditionalQueryModifier(
						" " + parameter + ":(" + Utils.getORList(t, false) + ")");
			}
		};
	}

	private Function<List<String>, Pair<String>> fwriter(String parameter) {
		return new Function<List<String>, Pair<String>>() {
			@Override
			public Pair<String> apply(List<String> t) {
				return new Pair<String>(parameter, Utils.getORList(t, false));
			}
		};
	}

	public String getHttpQuery(CommonQuery q) {
		// String spacesFormatQuery = Utils.spacesFormatQuery(q.searchTerm,
		// "%20");
		// spacesFormatQuery = addfilters(q, spacesFormatQuery);
		QueryBuilder builder = new QueryBuilder("http://api.trove.nla.gov.au/result");
		builder.addSearchParam("key", apiKey);
		builder.addSearchParam("zone", "picture,book,music,article");
		builder.setQuery("q", q.searchTerm);
		// TODO term to exclude?
		builder.addSearchParam("n", q.pageSize);
		builder.addSearchParam("s", "" + ((Integer.parseInt(q.page) - 1) * Integer.parseInt(q.pageSize)));
		builder.addSearchParam("encoding", "json");
		builder.addSearchParam("reclevel", "full");
		builder.addSearchParam("facet", "format,year");
		return addfilters(q, builder).getHttp();
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		if (!Utils.hasInfo(q.searchTerm) || q.searchTerm.equals("*"))
			q.searchTerm = " ";
		SourceResponse res = new SourceResponse();
		res.source = getSourceName().toString();
		String httpQuery = getHttpQuery(q);
		res.query = httpQuery;
		JsonNode response;
		CommonFilterLogic type = new CommonFilterLogic(FiltersFields.TYPE);
		CommonFilterLogic year = new CommonFilterLogic(FiltersFields.YEAR);

		if (checkFilters(q)) {
			try {
				response = getHttpConnector().getURLContent(httpQuery);
				JsonNode pa = response.path("response").path("zone");
				ArrayList<WithResource<?, ?>> a = new ArrayList<>();

				for (int i = 0; i < pa.size(); i++) {
					JsonNode o = pa.get(i);
					if (!o.path("name").asText().equals("people")) {
						log.debug(o.path("name").asText() );
						res.totalCount += Utils.readIntAttr(o.path("records"), "totalCount", true);
						res.count += Utils.readIntAttr(o.path("records"), "n", true);
						res.startIndex = Utils.readIntAttr(o.path("records"), "s", true);

						JsonNode aa = o.path("records").path("work");
						for (JsonNode item : aa) {

							List<String> v = Utils.readArrayAttr(item, "type", false);
							// type.addValue(vmap.translateToCommon(type.filterID,
							// ));
							// System.out.println("add " + v);
							// for (String string : v) {
							// countValue(type, string);
							// }
							res.addItem(formatreader.readObjectFrom(item));

						}
						for (JsonNode facet : o.path("facets").path("facet")) {
							if (!o.path("name").asText().equals("people")) {
								for (JsonNode jsonNode : facet.path("term")) {
									String label = jsonNode.path("search").asText();
									int count = jsonNode.path("count").asInt();
									JsonNode path = facet.path("name");
									switch (path.asText()) {
									case "format":
										countValue(type, label, count);
										break;
									case "year":
										countValue(year, label, count);
										break;
									default:
										break;
									}
								}
							}
						}

					}
				}

				res.filtersLogic = new ArrayList<>();
				 res.filtersLogic.add(type);
				 res.filtersLogic.add(year);

				// System.out.println(type);
			} catch (ConnectTimeoutException ce) {
				res.error = Failure.TIMEOUT;
				// TODO Auto-generated catch block
				log.error( "", ce );
			} catch (Exception e ) {
				log.error( "", e );				
			}
		}

		return res;
	}

	@Override
	public ArrayList<RecordJSONMetadata> getRecordFromSource(String recordId, RecordResource fullRecord) {
		ArrayList<RecordJSONMetadata> jsonMetadata = new ArrayList<RecordJSONMetadata>();
		JsonNode response;
		try {
			response = getHttpConnector().getURLContent(
					"http://api.trove.nla.gov.au/work/" + recordId + "?key=" + apiKey + "&encoding=json&reclevel=full");
			JsonNode record = response;
			if (record != null) {
				jsonMetadata.add(new RecordJSONMetadata(Format.JSON_NLA, record.toString()));
				String json = Json.toJson(formatreader.overwriteObjectFrom(fullRecord,record)).toString();
				jsonMetadata.add(new RecordJSONMetadata(Format.JSON_WITH, json));
			}
			Document xmlResponse = getHttpConnector().getURLContentAsXML(
					"http://api.trove.nla.gov.au/work/" + recordId + "?key=" + apiKey + "&encoding=xml&reclevel=full");
			jsonMetadata.add(new RecordJSONMetadata(Format.XML_NLA, Serializer.serializeXML(xmlResponse)));
			return jsonMetadata;
		} catch (Exception e) {
			return jsonMetadata;
		}
	}

	@Override
	public String apiConsole() {
		return "http://api.trove.nla.gov.au/";
	}

}
