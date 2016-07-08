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

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import play.Logger;
import play.libs.F.Promise;
import search.Query;
import search.Response.SingleResponse;
import search.Source;
import search.Sources;
import search.Filter;
import sources.core.ParallelAPICall;
import elastic.ElasticCoordinator;
import elastic.ElasticSearcher2.SearchOptions;

/*
 * This source is for internal search to WITH collections
 */
public class ElasticSource implements Source {
	public static final Logger.ALogger log = Logger.of(ElasticSource.class);

	@Override
	public Sources thisSource() {
		return Sources.WITHin;
	}

	@Override
	public Promise<SingleResponse> execute(Query query) {
		SearchOptions elasticoptions = new SearchOptions();


		ElasticCoordinator coord = new ElasticCoordinator(elasticoptions);
		Function<List<List<Filter>>, SingleResponse> recordSearch = ( (filters) -> ( coord.federatedSearch(filters)));
		return ParallelAPICall.createPromise(recordSearch, query.filters);
	}

	@Override
	public Promise<Object> completeRecord(Object incompleteRecord) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Promise<Object> getById(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Promise<String[]> autocomplete(String partialQueryString) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> supportedFieldIds() {
		// TODO Auto-generated method stub
		return null;
	}


}
