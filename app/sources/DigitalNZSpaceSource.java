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

import org.apache.commons.codec.digest.DigestUtils;
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.JsonNode;

import model.ExternalBasicRecord;
import model.ExternalBasicRecord.ItemRights;
import model.ExternalBasicRecord.RecordType;
import model.Provider.Sources;
import model.resources.WithResource;
import sources.core.CommonFilterLogic;
import sources.core.CommonFilters;
import sources.core.CommonQuery;
import sources.core.HttpConnector;
import sources.core.ISpaceSource;
import sources.core.QueryBuilder;
import sources.core.RecordJSONMetadata;
import sources.core.SourceResponse;
import sources.core.Utils;
import sources.core.RecordJSONMetadata.Format;
import sources.core.Utils.Pair;
import sources.formatreaders.DNZBasicRecordFormatter;
import utils.ListUtils;
import utils.Serializer;

public class DigitalNZSpaceSource extends ISpaceSource {

	/**
	 * National Library of New Zealand
	 */

	public DigitalNZSpaceSource() {
		super();
		LABEL = Sources.DigitalNZ.toString();
		apiKey = "SECRET_KEY";
		formatreader = new DNZBasicRecordFormatter();
		addDefaultWriter(CommonFilters.TYPE.name(), fwriter("and[category][]"));
		addDefaultWriter(CommonFilters.CREATOR.name(), fwriter("and[creator][]"));
		addDefaultWriter(CommonFilters.YEAR.name(), qfwriterYEAR());
		addDefaultWriter(CommonFilters.RIGHTS.name(), fwriter("and[usage][]"));

		// TODO: rights_url shows the license in the search

		addMapping(CommonFilters.TYPE.name(), RecordType.IMAGE.toString(), "Images");
		addMapping(CommonFilters.TYPE.name(), RecordType.SOUND.toString(), "Audio");
		addMapping(CommonFilters.TYPE.name(), RecordType.TEXT.toString(), "Books");

		// addMapping(CommonFilters.RIGHTS.name(), RightsValues.Creative_Commercial,
		// "");
		// ok RIGHTS:*creative* AND NOT RIGHTS:*nd*
		// addMapping(CommonFilters.RIGHTS.name(), RightsValues.Creative_Modify,
		// ".*creative(?!.*nd).*");

		// addMapping(CommonFilters.RIGHTS.name(),
		// RightsValues.Creative_Not_Commercial,
		// "http://creativecommons.org/licenses/by-nc/3.0/nz/",
		// "http://creativecommons.org/licenses/by-nc-sa/3.0/",
		// "This work is licensed under a Creative Commons
		// Attribution-Noncommercial 3.0 New Zealand License");
		//
		// addMapping(CommonFilters.RIGHTS.name(), RightsValues.UNKNOWN, "No known
		// copyright restrictions\nCopyright Expired",
		// "No known copyright restrictions");
		// addMapping(CommonFilters.RIGHTS.name(), RightsValues.RR, "All rights
		// reserved");

		addMapping(CommonFilters.RIGHTS.name(), ItemRights.Creative.toString(), "Share");
		addMapping(CommonFilters.RIGHTS.name(), ItemRights.Modify.toString(), "Modify");
		addMapping(CommonFilters.RIGHTS.name(), ItemRights.Commercial.toString(), "Use commercially");
		addMapping(CommonFilters.RIGHTS.name(), ItemRights.UNKNOWN.toString(), "Unknown");
		addMapping(CommonFilters.RIGHTS.name(), ItemRights.RR.toString(), "All rights reserved");
	}

	private Function<List<String>, Pair<String>> fwriter(String parameter) {

		Function<String, String> function = (String s) -> {
			return Utils.spacesFormatQuery(s, "%20");
		};
		return new Function<List<String>, Pair<String>>() {
			@Override
			public Pair<String> apply(List<String> t) {
				return new Pair<String>(parameter, Utils.getORList(ListUtils.transform(t, function), false));
			}
		};
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
		builder.addQuery("text", q.searchTerm);
		builder.addSearchParam("page", q.page);
		builder.addSearchParam("per_page", q.pageSize);
		builder.addSearchParam("facets", "year,creator,category,usage");
		return addfilters(q, builder).getHttp();
	}

	public List<CommonQuery> splitFilters(CommonQuery q) {
		return q.splitFilters(this);
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		SourceResponse res = new SourceResponse();
		res.source = getSourceName();
		String httpQuery = getHttpQuery(q);
		res.query = httpQuery;
		JsonNode response;
		CommonFilterLogic type = new CommonFilterLogic(CommonFilters.TYPE);
		CommonFilterLogic creator = new CommonFilterLogic(CommonFilters.CREATOR);
		CommonFilterLogic rights = new CommonFilterLogic(CommonFilters.RIGHTS);
		CommonFilterLogic year = new CommonFilterLogic(CommonFilters.YEAR);;

		if (checkFilters(q)) {
			try {
				response = HttpConnector.getURLContent(httpQuery);

				JsonNode o = response.path("search");
				// System.out.print(o.path("name").asText() + " ");

				res.totalCount = Utils.readIntAttr(o, "result_count", true);
				res.startIndex = (Utils.readIntAttr(o, "page", true) - 1) * res.count;

				JsonNode aa = o.path("results");

				for (JsonNode item : aa) {
					// System.out.println(item.toString());

//					List<String> v = Utils.readArrayAttr(item, "category", false);
//					// System.out.println("add " + v);
//					for (String string : v) {
//						countValue(type, string);
//					}
					res.addItem(formatreader.readObjectFrom(item));

				}
				res.count = res.items.getCulturalHO().size();


				readList(o.path("facets").path("category"), type);
				readList(o.path("facets").path("usage"), rights);
				readList(o.path("facets").path("year"), year);

				readList(o.path("facets").path("creator"), creator);

				res.filtersLogic = new ArrayList<>();
				res.filtersLogic.add(type);
				res.filtersLogic.add(creator);
				res.filtersLogic.add(rights);
				res.filtersLogic.add(year);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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

	public ArrayList<RecordJSONMetadata> getRecordFromSource(String recordId) {
		ArrayList<RecordJSONMetadata> jsonMetadata = new ArrayList<RecordJSONMetadata>();
		JsonNode response;
		try {
			response = HttpConnector
					.getURLContent("http://api.digitalnz.org/v3/records/" + recordId + ".json?api_key=" + apiKey);
			JsonNode record = response;
			jsonMetadata.add(new RecordJSONMetadata(Format.JSON_DNZ, record.toString()));
			Document xmlResponse = HttpConnector
					.getURLContentAsXML("http://api.digitalnz.org/v3/records/" + recordId + ".xml?api_key=" + apiKey);
			jsonMetadata.add(new RecordJSONMetadata(Format.XML_DNZ, Serializer.serializeXML(xmlResponse)));
			return jsonMetadata;
		} catch (Exception e) {
			return jsonMetadata;
		}
	}

}
