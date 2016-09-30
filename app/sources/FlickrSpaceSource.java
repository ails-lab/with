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

import model.EmbeddedMediaObject.WithMediaType;
import play.Logger;
import play.Logger.ALogger;
import search.FiltersFields;
import search.Sources;
import sources.core.ApacheHttpConnector;
import sources.core.CommonFilterLogic;
import sources.core.CommonQuery;
import sources.core.ISpaceSource;
import sources.core.QueryBuilder;
import sources.core.SourceResponse;
import sources.core.Utils;
import sources.core.Utils.Pair;
import sources.formatreaders.FlickrRecordFormatter;
import sources.utils.JsonContextRecord;
import utils.ListUtils;

public abstract class FlickrSpaceSource extends ISpaceSource {

	public static final ALogger log = Logger.of( FlickrSpaceSource.class);
	
	private static HashMap<String, String> licences;
	private static HashMap<String, String> licencesId;
	protected String userID;

	public static String getLicence(String id) {
		setLicences();
		return licences.get(id);
	}

	protected static void setLicences() {
		if (licences==null){
			String url = "https://api.flickr.com/services/rest/?method=flickr.photos.licenses.getInfo&"
					+ "api_key=SECRET_KEY&format=json&nojsoncallback=1";
			licences = new HashMap<String, String>();
			licencesId = new HashMap<String, String>();
			JsonNode response;
	
			try {
				response = ApacheHttpConnector.getApacheHttpConnector().getURLContent(url);
				for (JsonNode item : response.path("licenses").path("license")) {
					String id = Utils.readAttr(item, "id", true);
					String name = Utils.readAttr(item, "name", true);
					licences.put(id, name);
					licencesId.put(name, id);
				}
			} catch (Exception e) {
				log.error("",e);
			}
		}
	}

	public static String getLicenceId(String name) {
		return licencesId.get(name);
	}

	public FlickrSpaceSource(Sources source, String userID) {
		super(source);
		apiKey = "SECRET_KEY";
		
		this.sourceLABEL = source;
		vmap = FilterValuesMap.getMap(source);
//		this.vmap = FilterValuesMap.getFlickrMap();
		this.userID = userID;
		addDefaultWriter(FiltersFields.TYPE.getFilterId(), fwriter("media"));
		addDefaultWriter(FiltersFields.RIGHTS.getFilterId(), frwriter());
		addDefaultComplexWriter(FiltersFields.YEAR.getFilterId(), qfwriterYEAR());
		// addDefaultWriter(CommonFilters.COUNTRY.name(),
		// fwriter("sourceResource.spatial.country"));


	}

	protected Function<List<String>, Pair<String>> fwriter(String parameter) {
		Function<String, String> function = (String s) -> {
			return s;
		};
		return new Function<List<String>, Pair<String>>() {
			@Override
			public Pair<String> apply(List<String> t) {
				return new Pair<String>(parameter, Utils.getORList(ListUtils.transform(t, function), false));
			}
		};
	}

	protected Function<List<String>, Pair<String>> frwriter() {
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

	protected Function<List<String>, List<Pair<String>>> qfwriterYEAR() {
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
		builder.addSearchParam("user_id", userID);
		builder.addSearchParam("extras",
				"description, license, date_upload, date_taken, owner_name, icon_server, original_format, last_update, geo,tags, machine_tags, o_dims, views, media, path_alias, url_sq, url_t, url_s, url_q, url_m, url_n, url_z, url_c, url_l, url_o");
		builder.setQuery("text", q.searchTerm);
		builder.addSearchParam("page", q.page);
	
		builder.addSearchParam("per_page", "" + q.pageSize);
		builder.addSearchParam("nojsoncallback", "1");
		return addfilters(q, builder).getHttp();
	}

	@Override
	public SourceResponse getResults(CommonQuery q) {
		SourceResponse res = new SourceResponse();
		res.source = getSourceName().toString();
		String httpQuery = getHttpQuery(q);
		res.query = httpQuery;
		JsonNode response;
		// CommonFilterLogic rights = CommonFilterLogic.rightsFilter();
		CommonFilterLogic rights = new CommonFilterLogic(FiltersFields.RIGHTS);
		if (checkFilters(q)) {
			try {
				response = getHttpConnector().getURLContent(httpQuery);
				res.totalCount = Utils.readIntAttr(response.path("photos"), "total", true);
				res.count = Utils.readIntAttr(response.path("photos"), "perpage", true);
				for (JsonNode item : response.path("photos").path("photo")) {
					// countValue(type, t);
					// countValue(rights, it.rights, 1);
					res.addItem(formatreader.readObjectFrom(item));
					String l = BritishLibrarySpaceSource.getLicence(new JsonContextRecord(item).getStringValue("license"));
					countValue(rights, l);
				}
				res.count = res.items.getCulturalCHO().size();
	
				res.facets = response.path("facets");
				res.filtersLogic = this.vmap.getRestrictionsAsFilters(q,res.totalCount);
	
				// for (String ir : licencesId.keySet()) {
				// countValue(rights, ir, 1);
				// }
				// res.filtersLogic.add(rights);
	
				// CommonFilterLogic type = CommonFilterLogic.typeFilter();
				// countValue(type, "video", 1);
				// countValue(type, "photo", 1);
			} catch (Exception e) {
				log.error("", e );
			}
		}
		return res;
	}
	
	public static class InternetArchiveSpaceSource extends FlickrSpaceSource{

		public InternetArchiveSpaceSource() {
			super(Sources.InternetArchive,"126377022@N07");
			formatreader = new FlickrRecordFormatter.InternetArchiveRecordFormatter();
		}
		
	}

}