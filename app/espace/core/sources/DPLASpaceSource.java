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
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;

import espace.core.CommonFilterLogic;
import espace.core.CommonFilters;
import espace.core.CommonQuery;
import espace.core.HttpConnector;
import espace.core.ISpaceSource;
import espace.core.QueryBuilder;
import espace.core.RecordJSONMetadata;
import espace.core.RecordJSONMetadata.Format;
import espace.core.SourceResponse;
import espace.core.Utils;
import espace.core.Utils.Pair;
import espace.core.sources.formatreaders.DPLAExternalBasicRecordFormatter;
import model.ExternalBasicRecord;
import model.ExternalBasicRecord.ItemRights;
import model.ExternalBasicRecord.RecordType;
import utils.ListUtils;

public class DPLASpaceSource extends ISpaceSource {

	public static String LABEL = "DPLA";
	private String DPLAKey = "SECRET_KEY";
	private DPLAExternalBasicRecordFormatter formatreader;

	public String getHttpQuery(CommonQuery q) {
		// q=zeus&api_key=SECRET_KEY&sourceResource.creator=Zeus
		QueryBuilder builder = new QueryBuilder("http://api.dp.la/v2/items");
		builder.addSearchParam("api_key", DPLAKey);
		builder.addQuery("q", q.searchTerm);
		builder.addSearchParam("page", q.page);
		builder.addSearchParam("page_size", q.pageSize);
		builder.addSearchParam("facets",
				"provider.name,sourceResource.type,sourceResource.contributor,sourceResource.spatial.country,dataProvider");
		return addfilters(q, builder).getHttp();
	}

	public DPLASpaceSource() {
		super();
		formatreader = new DPLAExternalBasicRecordFormatter();
		addDefaultWriter(CommonFilters.TYPE.getID(), fwriter("sourceResource.type"));
		addDefaultWriter(CommonFilters.COUNTRY.getID(), fwriter("sourceResource.spatial.country"));
		addDefaultWriter(CommonFilters.CREATOR.getID(), fwriter("sourceResource.creator"));
		addDefaultWriter(CommonFilters.CONTRIBUTOR.getID(), fwriter("sourceResource.contributor"));
		addDefaultWriter(CommonFilters.PROVIDER.getID(), fwriter("provider.name"));
		addDefaultWriter(CommonFilters.TYPE.getID(), fwriter("sourceResource.type"));
		addDefaultComplexWriter(CommonFilters.YEAR.getID(), qfwriterYEAR());

		/**
		 * TODO check this
		 */

		addDefaultWriter(CommonFilters.RIGHTS.getID(), fwriter("sourceResource.rights"));
		addMapping(CommonFilters.RIGHTS.getID(), ItemRights.Commercial.toString(), ".*creative(?!.*nc).*");
		// ok RIGHTS:*creative* AND NOT RIGHTS:*nd*
		addMapping(CommonFilters.RIGHTS.getID(), ItemRights.Modify.toString(), ".*creative(?!.*nd).*");

		addMapping(CommonFilters.RIGHTS.getID(), ItemRights.Creative_Not_Commercial.toString(), ".*creative.*nc.*",
				".*non-commercial.*");

		addMapping(CommonFilters.RIGHTS.getID(), ItemRights.RRPA.toString(), ".*rr-p.*");
		addMapping(CommonFilters.RIGHTS.getID(), ItemRights.RRRA.toString(), ".*rr-r.*");
		addMapping(CommonFilters.RIGHTS.getID(), ItemRights.RRFA.toString(), ".*rr-f.*");

		addMapping(CommonFilters.RIGHTS.getID(), ItemRights.RRFA.toString(), ".*unknown.*");

		addMapping(CommonFilters.RIGHTS.getID(), ItemRights.Creative_Not_Modify.toString(), ".*creative.*nd.*");

		addMapping(CommonFilters.RIGHTS.getID(), ItemRights.Creative.toString(), ".*(creative).*");

		addMapping(CommonFilters.TYPE.getID(), RecordType.IMAGE.toString(), "image");
		addMapping(CommonFilters.TYPE.getID(), RecordType.VIDEO.toString(), "moving image");
		addMapping(CommonFilters.TYPE.getID(), RecordType.SOUND.toString(), "sound");
		addMapping(CommonFilters.TYPE.getID(), RecordType.TEXT.toString(), "text");

		// TODO: what to do with physical objects?
	}

	private Function<List<String>, List<Pair<String>>> qfwriterYEAR() {
		Function<String, String> function = (String s) -> {
			return "%22" + Utils.spacesFormatQuery(s, "%20") + "%22";
		};
		return new Function<List<String>, List<Pair<String>>>() {
			@Override
			public List<Pair<String>> apply(List<String> t) {
				String start = "", end = "";
				if (t.size() == 1) {
					start = t.get(0) + "-01-01";
					end = next(t.get(0)) + "-01-01";
				} else if (t.size() > 1) {
					start = t.get(0) + "-01-01";
					end = next(t.get(1)) + "-01-01";
				}

				return Arrays.asList(new Pair<String>("sourceResource.date.after", start),
						new Pair<String>("sourceResource.date.before", end));

			}

			private String next(String string) {
				return "" + (Integer.parseInt(string) + 1);
			}
		};
	}

	private Function<List<String>, Pair<String>> fwriter(String parameter) {
		Function<String, String> function = (String s) -> {
			return "%22" + Utils.spacesFormatQuery(s, "%20") + "%22";
		};
		return new Function<List<String>, Pair<String>>() {
			@Override
			public Pair<String> apply(List<String> t) {
				return new Pair<String>(parameter, Utils.getORList(ListUtils.transform(t, function), false));
			}
		};
	}

	public String getSourceName() {
		return LABEL;
	}

	public String getDPLAKey() {
		return DPLAKey;
	}

	public void setDPLAKey(String dPLAKey) {
		DPLAKey = dPLAKey;
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		SourceResponse res = new SourceResponse();
		res.source = getSourceName();
		String httpQuery = getHttpQuery(q);
		res.query = httpQuery;
		JsonNode response;
//		CommonFilterLogic type = CommonFilterLogic.typeFilter();
//		CommonFilterLogic provider = CommonFilterLogic.providerFilter();
//		CommonFilterLogic dataProvider = CommonFilterLogic.dataproviderFilter();
//		CommonFilterLogic creator = CommonFilterLogic.creatorFilter();
//		CommonFilterLogic country = CommonFilterLogic.countryFilter();
//		CommonFilterLogic contributor = CommonFilterLogic.contributorFilter();
		if (checkFilters(q)) {
			try {
				response = HttpConnector.getURLContent(httpQuery);
				// System.out.println(response.toString());
				JsonNode docs = response.path("docs");
				res.totalCount = Utils.readIntAttr(response, "count", true);
				res.count = docs.size();
				res.startIndex = Utils.readIntAttr(response, "start", true);
				ArrayList<ExternalBasicRecord> a = new ArrayList<>();

				for (JsonNode item : docs) {

					// String t = Utils.readAttr(item.path("sourceResource"),
					// "type", false);
					// countValue(type, t);

					ExternalBasicRecord obj = formatreader.readObjectFrom(item);
					a.add(obj);
//					countValue(creator, obj.getCreator());
					
				}
				res.items = a;
				res.facets = response.path("facets");
				res.filtersLogic = new ArrayList<>();

//				readList(response.path("facets").path("provider.name"), provider);
//
//				readList(response.path("facets").path("dataProvider"), dataProvider);
//
//				readList(response.path("facets").path("sourceResource.type"), type);
//
//				readList(response.path("facets").path("sourceResource.contributor"), contributor);
//
//				readList(response.path("facets").path("sourceResource.spatial.country"), country);

				res.filtersLogic = new ArrayList<>();
//				res.filtersLogic.add(type);
//				res.filtersLogic.add(provider);
//				res.filtersLogic.add(dataProvider);
//				res.filtersLogic.add(creator);
//				res.filtersLogic.add(country);
//
//				res.filtersLogic.add(contributor);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return res;
	}

	private void readList(JsonNode json, CommonFilterLogic filter) {
		// System.out.println(json);
		for (JsonNode f : json.path("terms")) {
			String label = f.path("term").asText();
			int count = f.path("count").asInt();
			countValue(filter, label, count);
		}
	}

	public ArrayList<RecordJSONMetadata> getRecordFromSource(String recordId) {
		ArrayList<RecordJSONMetadata> jsonMetadata = new ArrayList<RecordJSONMetadata>();
		JsonNode response;
		try {
			response = HttpConnector.getURLContent("http://api.dp.la/v2/items?id=" + recordId + "&api_key=" + DPLAKey);
			JsonNode record = response.get("docs").get(0);
			if (record != null)
				jsonMetadata.add(new RecordJSONMetadata(Format.JSONLD_DPLA, record.toString()));
			return jsonMetadata;
		} catch (Exception e) {
			e.printStackTrace();
			return jsonMetadata;
		}
	}

}
