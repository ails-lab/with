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
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;

import sources.EuropeanaSpaceSource.EuroQB;
import sources.core.ApacheHttpConnector;
import sources.core.CommonFilter;
import sources.core.CommonFilters;
import sources.core.CommonQuery;
import sources.core.FacetsModes;
import sources.core.HttpConnector;
import sources.core.QueryBuilder;
import sources.core.SourceResponse;
import sources.core.Utils;
import sources.core.Utils.Pair;
import utils.ListUtils;

public class EuropeanaCollectionSpaceSource extends EuropeanaSpaceSource{

	private String collectionName;
	private String nextCursor;
	
	public EuropeanaCollectionSpaceSource(String collectionName) {
		super();
		addDefaultWriter("europeana_collectionName", qfwriter("europeana_collectionName"));
		this.collectionName = collectionName;
	}
	
	@Override
	public HttpConnector getHttpConnector() {
		return ApacheHttpConnector.getApacheHttpConnector();
	}
	
	private Function<List<String>, Pair<String>> qfwriter(String parameter) {
		Function<String, String> function = (String s) -> {
			return "\"" + s + "\"";
		};
		return new Function<List<String>, Pair<String>>() {
			@Override
			public Pair<String> apply(List<String> t) {
				return new Pair<String>("qf", parameter + ":" + t.get(0));
			}
		};
	}
	
	public String getHttpQuery(CommonQuery q) {
		QueryBuilder builder = new EuroQB("http://europeana.eu/api/v2/search.json");
		builder.addSearchParam("wskey", apiKey);

		builder.addQuery("query", q.searchTerm);
		System.out.println(nextCursor);
		if (q.page.equals("1"))
			builder.addSearchParam("cursor", "*");
		else
			builder.addSearchParam("cursor", nextCursor);
		builder.addSearchParam("rows", "" + q.pageSize);
		builder.addSearchParam("profile", "rich+facets");
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
	
	@Override
	public SourceResponse getResults(CommonQuery q) {
		q.addFilter(new CommonFilter("europeana_collectionName",getCollectionName()));
		return getMyResults(q);
	}
	
	public SourceResponse getMyResults(CommonQuery q) {
		SourceResponse res = new SourceResponse();
		res.source = getSourceName();
		String httpQuery = getHttpQuery(q);
		res.query = httpQuery;
		JsonNode response;
		if (checkFilters(q)) {
			try {
				response = getHttpConnector().getURLContent(httpQuery);
				res.totalCount = Utils.readIntAttr(response, "totalResults", true);
				res.count = Utils.readIntAttr(response, "itemsCount", true);
				res.items.setCulturalCHO(getItems(response));
				nextCursor = Utils.readAttr(response, "nextCursor", true);
				// res.facets = response.path("facets");
				res.filtersLogic = createFilters(response);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				 e.printStackTrace();
			}
		}
		return res;
	}
	public SourceResponse getAllResults(CommonQuery q) {
		q.filters = Arrays.asList(new CommonFilter("europeana_collectionName",getCollectionName()));
		return getMyResults(q);
	}

	public String getCollectionName() {
		return collectionName;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}
	
	

}
