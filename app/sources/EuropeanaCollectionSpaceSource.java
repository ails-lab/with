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

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import sources.core.ApacheHttpConnector;
import sources.core.CommonFilter;
import sources.core.CommonQuery;
import sources.core.HttpConnector;
import sources.core.SourceResponse;
import sources.core.Utils.Pair;

public class EuropeanaCollectionSpaceSource extends EuropeanaSpaceSource{

	private String collectionName;
	private String nextCursor;
	
	public EuropeanaCollectionSpaceSource(String collectionName) {
		super();
		setUsingCursor(true);
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
				return new Pair<String>("qf", parameter + ":" + function.apply(t.get(0)));
			}
		};
	}
	
	@Override
	public SourceResponse getResults(CommonQuery q) {
		q.filters = Arrays.asList(new CommonFilter("europeana_collectionName",getCollectionName()));
		return super.getResults(q);
	}
	
//	public SourceResponse getMyResults(CommonQuery q) {
//		SourceResponse res = new SourceResponse();
//		res.source = getSourceName();
//		String httpQuery = getHttpQuery(q);
//		res.query = httpQuery;
//		JsonNode response;
//		if (checkFilters(q)) {
//			try {
//				response = getHttpConnector().getURLContent(httpQuery);
//				res.totalCount = Utils.readIntAttr(response, "totalResults", true);
//				res.count = Utils.readIntAttr(response, "itemsCount", true);
//				res.items.setCulturalCHO(getItems(response));
//				nextCursor = Utils.readAttr(response, "nextCursor", true);
//				// res.facets = response.path("facets");
//				res.filtersLogic = createFilters(response);
//
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				 e.printStackTrace();
//			}
//		}
//		return res;
//	}

	public String getCollectionName() {
		return collectionName;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}
	
	

}
