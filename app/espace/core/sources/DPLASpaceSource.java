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

import org.apache.commons.codec.digest.DigestUtils;

import utils.ListUtils;

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
import espace.core.SourceResponse.ItemsResponse;
import espace.core.SourceResponse.Lang;
import espace.core.SourceResponse.MyURL;
import espace.core.Utils;
import espace.core.Utils.Pair;
import espace.core.Utils.LongPair;

public class DPLASpaceSource extends ISpaceSource {

	public static String LABEL = "DPLA";
	private String DPLAKey = "SECRET_KEY";

	public String getHttpQuery(CommonQuery q) {
		// q=zeus&api_key=SECRET_KEY&sourceResource.creator=Zeus
		QueryBuilder builder = new QueryBuilder("http://api.dp.la/v2/items");
		builder.addSearchParam("api_key", DPLAKey);
		builder.addSearchParam("q", q.searchTerm);
		builder.addSearchParam("page", q.page);
		builder.addSearchParam("page_size", q.pageSize);
		builder.addSearchParam("facets",
				"provider.name,sourceResource.type,sourceResource.contributor,sourceResource.spatial.country");
		return addfilters(q, builder).getHttp();
	}

	public DPLASpaceSource() {
		super();
		addDefaultWriter(CommonFilters.TYPE_ID, fwriter("sourceResource.type"));
		addDefaultWriter(CommonFilters.COUNTRY_ID, fwriter("sourceResource.spatial.country"));
		addDefaultWriter(CommonFilters.CREATOR_ID, fwriter("sourceResource.creator"));
		addDefaultWriter(CommonFilters.CONTRIBUTOR_ID, fwriter("sourceResource.contributor"));
		addDefaultWriter(CommonFilters.PROVIDER_ID, fwriter("provider.name"));
		addDefaultWriter(CommonFilters.TYPE_ID, fwriter("sourceResource.type"));
		addDefaultComplexWriter(CommonFilters.YEAR_ID, qfwriterYEAR());

		/**
		 * TODO check this 
		 */
		
		addDefaultWriter(CommonFilters.RIGHTS_ID, fwriter("sourceResource.rights"));

		addMapping(CommonFilters.RIGHTS_ID, RightsValues.Public, ".*(http://creativecommons.org/publicdomain/mark/1.0/ | http://creativecommons.org/publicdomain/zero/1.0/ | http://creativecommons.org/licenses/by/ | http://creativecommons.org/licenses/by-sa/).*");
		
		addMapping(CommonFilters.RIGHTS_ID, RightsValues.Restricted, ".*(http://creativecommons.org/licenses/by-nc/ | http://creativecommons.org/licenses/by-nc-sa/ | http://creativecommons.org/licenses/by-nc-nd/ | http://creativecommons.org/licenses/by-nd/ | http://www.europeana.eu/rights/out-of-copyright-non-commercial/).*");
		
		addMapping(CommonFilters.RIGHTS_ID, RightsValues.Permission, ".*!(http://creativecommons.org/licenses/by-nc/ | http://creativecommons.org/licenses/by-nc-sa/ | http://creativecommons.org/licenses/by-nc-nd/ | http://creativecommons.org/licenses/by-nd/ | http://creativecommons.org/publicdomain/mark/1.0/ | http://creativecommons.org/publicdomain/zero/1.0/ | http://creativecommons.org/licenses/by/ | http://creativecommons.org/licenses/by-sa/| http://www.europeana.eu/rights/out-of-copyright-non-commercial/).*");
		
		addMapping(CommonFilters.TYPE_ID, TypeValues.IMAGE, "image");
		addMapping(CommonFilters.TYPE_ID, TypeValues.VIDEO, "moving image");
		addMapping(CommonFilters.TYPE_ID, TypeValues.SOUND, "sound");
		addMapping(CommonFilters.TYPE_ID, TypeValues.TEXT, "text");

		// TODO: what to do with physical objects?
	}
	
	private Function<List<String>, List<Pair<String>>> qfwriterYEAR() {
		Function<String, String> function =
				(String s)->{return "%22"+Utils.spacesFormatQuery(s, "%20")+"%22";};
		return new Function<List<String>, List<Pair<String>>>() {
			@Override
			public List<Pair<String>> apply(List<String> t) {
				String start="", end="";
				if (t.size()==1){
					start = t.get(0)+"-01-01";
					end = next(t.get(0))+"-01-01";
				} else
				if (t.size()>1){
					start = t.get(0)+"-01-01";
					end = next(t.get(1))+"-01-01";
				}
				
				return Arrays.asList(
						new Pair<String>("sourceResource.date.after", start),
						new Pair<String>("sourceResource.date.before", end)
						);
				
			}

			private String next(String string) {
				return ""+(Integer.parseInt(string)+1);
			}
		};
	}


	private Function<List<String>, Pair<String>> fwriter(String parameter) {
		Function<String, String> function =
				(String s)->{return "%22"+Utils.spacesFormatQuery(s, "%20")+"%22";};
		return new Function<List<String>, Pair<String>>() {
			@Override
			public Pair<String> apply(List<String> t) {
				return new Pair<String>(parameter, 
						Utils.getORList(ListUtils.transform(t, 
								function), false));
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
		CommonFilterLogic type = CommonFilterLogic.typeFilter();
		CommonFilterLogic provider = CommonFilterLogic.providerFilter();
		CommonFilterLogic creator = CommonFilterLogic.creatorFilter();
		CommonFilterLogic country = CommonFilterLogic.countryFilter();
		CommonFilterLogic contributor = CommonFilterLogic.contributorFilter();
		if (checkFilters(q)){
		try {
			response = HttpConnector.getURLContent(httpQuery);
			// System.out.println(response.toString());
			JsonNode docs = response.path("docs");
			res.totalCount = Utils.readIntAttr(response, "count", true);
			res.count = docs.size();
			res.startIndex = Utils.readIntAttr(response, "start", true);
			ArrayList<ItemsResponse> a = new ArrayList<ItemsResponse>();

			for (JsonNode item : docs) {

				// String t = Utils.readAttr(item.path("sourceResource"),
				// "type", false);
				// countValue(type, t);

				ItemsResponse it = new ItemsResponse();
				it.id = Utils.readAttr(item, "id", true);
				it.thumb = Utils.readArrayAttr(item, "object", false);
				it.fullresolution = null;
				it.title = Utils.readLangAttr(item.path("sourceResource"),
						"title", false);
				it.description = Utils.readLangAttr(
						item.path("sourceResource"), "description", false);
				it.creator = Utils.readLangAttr(item.path("sourceResource"),
						"creator", false);
				countValue(creator,
						ListUtils.transform(it.creator, (Lang s) -> {
							return s.value;
						}));
				it.year = null;
				it.dataProvider = Utils.readLangAttr(item.path("provider"),
						"name", false);
				it.url = new MyURL();
				it.url.original = Utils.readArrayAttr(item, "isShownAt", false);
				it.url.fromSourceAPI = "http://dp.la/item/"
						+ Utils.readAttr(item, "id", false);
				it.rights = Utils.readLangAttr(item.path("sourceResource"),
						"rights", false);
				it.externalId = it.url.original.get(0);

				it.externalId = DigestUtils.md5Hex(it.externalId);
				a.add(it);
			}
			res.items = a;
			res.facets = response.path("facets");
			res.filtersLogic = new ArrayList<>();

			readList(response.path("facets").path("provider.name"), provider);

			readList(response.path("facets").path("sourceResource.type"), type);

			readList(response.path("facets").path("sourceResource.contributor"), contributor);

			readList(response.path("facets").path("sourceResource.spatial.country"), country);

			res.filtersLogic = new ArrayList<>();
			res.filtersLogic.add(type);
			res.filtersLogic.add(provider);
			res.filtersLogic.add(creator);
			res.filtersLogic.add(country);

			res.filtersLogic.add(contributor);

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
			response = HttpConnector
					.getURLContent("http://api.dp.la/v2/items?id=" + recordId
							+ "&api_key=" + DPLAKey);
			JsonNode record = response.get("docs").get(0);
			if (record != null)
				jsonMetadata.add(new RecordJSONMetadata(Format.JSONLD_DPLA,
						record.toString()));
			return jsonMetadata;
		} catch (Exception e) {
			e.printStackTrace();
			return jsonMetadata;
		}
	}

}
