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
import java.util.List;
import java.util.function.Function;

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
import sources.core.SourceResponse;
import sources.core.Utils;
import sources.core.Utils.Pair;
import sources.formatreaders.BritishLibraryRecordFormatter;
import utils.ListUtils;

public class BritishLibrarySpaceSource extends ISpaceSource {
	private HashMap<String, String> licences;
	private HashMap<String, String> licencesId;

	public BritishLibrarySpaceSource() {
		super();
		LABEL = Sources.BritishLibrary.toString();
		apiKey = "8bddf33bef4c14c98d469bfb1f8e78c7";
		addDefaultWriter(CommonFilters.TYPE.name(), fwriter("media"));
		addDefaultWriter(CommonFilters.RIGHTS.name(), frwriter());
		addDefaultComplexWriter(CommonFilters.YEAR.name(), qfwriterYEAR());
		// addDefaultWriter(CommonFilters.COUNTRY.name(),
		// fwriter("sourceResource.spatial.country"));

		setLicences();

		addMapping(CommonFilters.TYPE.name(), RecordType.IMAGE.toString(), "photo");
		addMapping(CommonFilters.TYPE.name(), RecordType.VIDEO.toString(), "video");

		addMapping(CommonFilters.RIGHTS.name(), ItemRights.RR.toString(), getLicence("0"));
		addMapping(CommonFilters.RIGHTS.name(), ItemRights.Creative_Not_Commercial.toString(), getLicence("3"),
				getLicence("2"), getLicence("1"));
		addMapping(CommonFilters.RIGHTS.name(), ItemRights.Modify.toString(), getLicence("6"));
		addMapping(CommonFilters.RIGHTS.name(), ItemRights.Creative.toString(), getLicence("1"), getLicence("2"),
				getLicence("3"), getLicence("4"), getLicence("5"), getLicence("6"));
		addMapping(CommonFilters.RIGHTS.name(), ItemRights.UNKNOWN.toString(), getLicence("7"));
		addMapping(CommonFilters.RIGHTS.name(), ItemRights.Public.toString(), getLicence("9"), getLicence("10"));
		formatreader = new BritishLibraryRecordFormatter(vmap);

	}

	private void setLicences() {
		String url = "https://api.flickr.com/services/rest/?method=flickr.photos.licenses.getInfo&api_key=" + apiKey
				+ "&format=json&nojsoncallback=1";
		licences = new HashMap<String, String>();
		licencesId = new HashMap<String, String>();
		JsonNode response;

		try {
			response = HttpConnector.getURLContent(url);
			for (JsonNode item : response.path("licenses").path("license")) {
				String id = Utils.readAttr(item, "id", true);
				String name = Utils.readAttr(item, "name", true);
				licences.put(id, name);
				System.out.println(id + "-->" + name);
				licencesId.put(name, id);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getLicence(String id) {
		return licences.get(id);
	}

	public String getLicenceId(String name) {
		return licencesId.get(name);
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

	private Function<List<String>, Pair<String>> frwriter() {
		Function<String, String> function = (String s) -> {
			return licencesId.get(s);
		};
		return new Function<List<String>, Pair<String>>() {
			@Override
			public Pair<String> apply(List<String> t) {
				return new Pair<String>("license", Utils.getStringList(ListUtils.transform(t, function), ","));
			}
		};
	}

	private Function<List<String>, List<Pair<String>>> qfwriterYEAR() {
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

				return Arrays.asList(new Pair<String>("min_taken_date", start),
						new Pair<String>("max_taken_date", end));

			}

			private String next(String string) {
				return "" + (Integer.parseInt(string) + 1);
			}
		};
	}

	public String getHttpQuery(CommonQuery q) {
		QueryBuilder builder = new QueryBuilder("" + "https://api.flickr.com/services/rest/");
		builder.addSearchParam("method", "flickr.photos.search");
		builder.addSearchParam("api_key", apiKey);
		builder.addSearchParam("format", "json");
		builder.addSearchParam("user_id", "12403504%40N02");
		builder.addSearchParam("extras",
				"description,%20license,%20date_upload,%20date_taken,%20owner_name,%20icon_server,%20original_format,%20last_update,%20geo,%20tags,%20machine_tags,%20o_dims,%20views,%20media,%20path_alias,%20url_sq,%20url_t,%20url_s,%20url_q,%20url_m,%20url_n,%20url_z,%20url_c,%20url_l,%20url_o");
		builder.addQuery("text", q.searchTerm);
		builder.addSearchParam("page", q.page);

		builder.addSearchParam("per_page", "" + q.pageSize);
		builder.addSearchParam("nojsoncallback", "1");
		return addfilters(q, builder).getHttp();
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		SourceResponse res = new SourceResponse();
		res.source = getSourceName();
		String httpQuery = getHttpQuery(q);
		res.query = httpQuery;
		JsonNode response;
		// CommonFilterLogic rights = CommonFilterLogic.rightsFilter();
		if (checkFilters(q)) {
			try {
				response = HttpConnector.getURLContent(httpQuery);
				res.totalCount = Utils.readIntAttr(response.path("photos"), "total", true);
				res.count = Utils.readIntAttr(response.path("photos"), "perpage", true);
				for (JsonNode item : response.path("photos").path("photo")) {
					// countValue(type, t);
					// countValue(rights, it.rights, 1);
					res.addItem(formatreader.readObjectFrom(item));
				}
				res.count = res.items.getCulturalCHO().size();

				res.facets = response.path("facets");
				res.filtersLogic = new ArrayList<>();

				// for (String ir : licencesId.keySet()) {
				// countValue(rights, ir, 1);
				// }
				// res.filtersLogic.add(rights);

				// CommonFilterLogic type = CommonFilterLogic.typeFilter();
				// countValue(type, "video", 1);
				// countValue(type, "photo", 1);
				// res.filtersLogic.add(type);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return res;
	}

}
