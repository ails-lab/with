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
import java.util.Map.Entry;
import java.util.function.Function;

import org.apache.http.conn.ConnectTimeoutException;
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.JsonNode;

import model.resources.RecordResource;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import search.FiltersFields;
import search.Sources;
import search.Response.Failure;
import sources.core.CommonFilterLogic;
import sources.core.CommonQuery;
import sources.core.ISpaceSource;
import sources.core.QueryBuilder;
import sources.core.RecordJSONMetadata;
import sources.core.RecordJSONMetadata.Format;
import sources.core.SourceResponse;
import sources.core.Utils;
import sources.core.Utils.Pair;
import sources.formatreaders.DNZBasicRecordFormatter;
import sources.utils.FunctionsUtils;
import utils.Serializer;

public class DigitalNZSpaceSource extends ISpaceSource {

	public static final ALogger log = Logger.of( DigitalNZSpaceSource.class);
	
	/**
	 * National Library of New Zealand
	 */

	public DigitalNZSpaceSource() {
		super(Sources.DigitalNZ);
		addDefaultWriter(FiltersFields.TYPE.getFilterId(), fwriter("and[category][]"));
		addDefaultWriter(FiltersFields.CREATOR.getFilterId(), fwriter("and[creator][]"));
		addDefaultWriter(FiltersFields.YEAR.getFilterId(), qfwriterYEAR());
		addDefaultWriter(FiltersFields.RIGHTS.getFilterId(), fwriter("and[usage][]"));

		// TODO: rights_url shows the license in the search

		formatreader = new DNZBasicRecordFormatter();

	}

	private Function<List<String>, Pair<String>> fwriter(String parameter) {
		return FunctionsUtils.toORList(parameter, false);
	}

	private Function<List<String>, Pair<String>> qfwriterYEAR() {
		return new Function<List<String>, Pair<String>>() {
			@Override
			public Pair<String> apply(List<String> t) {
				String val = t.get(0);
				if (t.size() > 1) {
					val = t.get(1);
				}
				return new Pair<String>("i[year][]", "[" + t.get(0) + " TO " + val + "]");
			}
		};
	}

	public String getHttpQuery(CommonQuery q) {
		QueryBuilder builder = new QueryBuilder("http://api.digitalnz.org/v3/records.json");
		builder.addSearchParam("api_key", apiKey);
		builder.setQuery("text", q.searchTerm);
		builder.addSearchParam("page", q.page);
		builder.addSearchParam("per_page", q.pageSize);
		builder.addSearchParam("facets", "year,creator,category,usage");
		builder.addSearchParam("facets_per_page", "20");
		return addfilters(q, builder).getHttp();
	}

	public List<CommonQuery> splitFilters(CommonQuery q) {
		return q.splitFilters(this);
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		if (!Utils.hasInfo(q.searchTerm) || q.searchTerm.equals("*"))
			q.searchTerm = "format:(picture OR book OR music OR article)";
		SourceResponse res = new SourceResponse();
		res.source = getSourceName().toString();
		String httpQuery = getHttpQuery(q);
		res.query = httpQuery;
		JsonNode response;
		CommonFilterLogic type = new CommonFilterLogic(FiltersFields.TYPE);
		CommonFilterLogic creator = new CommonFilterLogic(FiltersFields.CREATOR);
		CommonFilterLogic rights = new CommonFilterLogic(FiltersFields.RIGHTS);
		CommonFilterLogic year = new CommonFilterLogic(FiltersFields.YEAR);
		;

		if (checkFilters(q)) {
			try {
				response = getHttpConnector().getURLContent(httpQuery);

				JsonNode o = response.path("search");
				// System.out.print(o.path("name").asText() + " ");

				res.totalCount = Utils.readIntAttr(o, "result_count", true);
				res.startIndex = (Utils.readIntAttr(o, "page", true) - 1) * res.count;

				JsonNode aa = o.path("results");

				for (JsonNode item : aa) {
					// System.out.println(item.toString());

					// List<String> v = Utils.readArrayAttr(item, "category",
					// false);
					// // System.out.println("add " + v);
					// for (String string : v) {
					// countValue(type, string);
					// }
					res.addItem(formatreader.readObjectFrom(item));

				}
				res.count = res.items.getCulturalCHO().size();

				readList(o.path("facets").path("category"), type);
				readList(o.path("facets").path("usage"), rights);
				readList(o.path("facets").path("year"), year);

				readList(o.path("facets").path("creator"), creator);

				res.filtersLogic = new ArrayList<>();
				res.filtersLogic.add(type);
				res.filtersLogic.add(creator);
				res.filtersLogic.add(rights);
				res.filtersLogic.add(year);
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

	private void readList(JsonNode json, CommonFilterLogic filter) {
		// System.out.println("************************\n"+json.toString());
		for (Iterator<Entry<String, JsonNode>> iterator = json.fields(); iterator.hasNext();) {
			Entry<String, JsonNode> v = iterator.next();
			String label = v.getKey();
			int count = v.getValue().asInt();
			countValue(filter, label, v.getValue().asInt());
			// System.out.println(label+" "+count);
		}
	}

	@Override
	public ArrayList<RecordJSONMetadata> getRecordFromSource(String recordId, RecordResource fullRecord) {
		ArrayList<RecordJSONMetadata> jsonMetadata = new ArrayList<RecordJSONMetadata>();
		JsonNode response;
		try {
			response = getHttpConnector()
					.getURLContent("http://api.digitalnz.org/v3/records/" + recordId + ".json?api_key=" + apiKey);
			JsonNode record = response;
			if (record != null) {
				jsonMetadata.add(new RecordJSONMetadata(Format.JSON_DNZ, record.toString()));
				String json = Json.toJson(formatreader.overwriteObjectFrom(fullRecord,record.path("record"))).toString();
				jsonMetadata.add(new RecordJSONMetadata(Format.JSON_WITH, json));
			}
			Document xmlResponse = getHttpConnector()
					.getURLContentAsXML("http://api.digitalnz.org/v3/records/" + recordId + ".xml?api_key=" + apiKey);
			jsonMetadata.add(new RecordJSONMetadata(Format.XML_DNZ, Serializer.serializeXML(xmlResponse)));
			return jsonMetadata;
		} catch (Exception e) {
			return jsonMetadata;
		}
	}

	@Override
	public String apiConsole() {
		return "http://api.digitalnz.org/";
	}

}
