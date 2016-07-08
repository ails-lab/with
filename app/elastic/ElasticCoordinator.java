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


package elastic;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;

import db.DB;
import search.Filter;
import search.Response.SingleResponse;
import elastic.ElasticSearcher2.SearchOptions;

public class ElasticCoordinator {

	private final SearchOptions options;

	public ElasticCoordinator(SearchOptions options) {
		this.options = options;
	}


	public SingleResponse federatedSearch(List<List<Filter>> filters) {
		ElasticSearcher2 searcher = new ElasticSearcher2();
		List<QueryBuilder> musts = new ArrayList<QueryBuilder>();
		for(List<Filter> ors: filters) {
			musts.add(searcher.boolShouldQuery(ors));
		}
		SearchResponse elasticresp =
				searcher.getBoolSearchRequestBuilder(musts, null, null, options)
				.execute().actionGet();


		SingleResponse sresp = new SingleResponse();
		List<ObjectId> ids = new ArrayList<ObjectId>();
		for(SearchHit h: elasticresp.getHits()) {
			ids.add(new ObjectId(h.getId()));
		}
		sresp.items = DB.getRecordResourceDAO().getByIds(ids);
		sresp.totalCount = (int) elasticresp.getHits().getTotalHits();

		return sresp;
	}
}
