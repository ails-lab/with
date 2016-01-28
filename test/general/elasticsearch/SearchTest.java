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


package general.elasticsearch;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.junit.Test;

import utils.Tuple;
import elastic.Elastic;
import elastic.ElasticSearcher;
import elastic.ElasticSearcher.SearchOptions;

public class SearchTest {

	@Test
	public void testFederatedSearch() {
		ElasticSearcher searcher = new ElasticSearcher();
		searcher.addType(Elastic.typeResource);

		SearchOptions options = new SearchOptions(0, 10);
		options.setScroll(false);
		options.setFilterType("and");
		options.addFilter("dataProvider", "");
		options.addFilter("dataProvider", "");
		options.addFilter("provider", "");

		//Access Rights
		List<Tuple<ObjectId, Access>> userAccess = new ArrayList<Tuple<ObjectId, Access>>();
		userAccess.add(new Tuple<ObjectId, WithAccess.Access>(new ObjectId("55b637a7e4b0cbaeed931c95"), Access.READ));
		/*userAccess.add(new Tuple<ObjectId, WithAccess.Access>(new ObjectId(""), Access.READ));
		userAccess.add(new Tuple<ObjectId, WithAccess.Access>(new ObjectId(""), Access.READ));
		userAccess.add(new Tuple<ObjectId, WithAccess.Access>(new ObjectId(""), Access.READ));*/

		List<List<Tuple<ObjectId, Access>>> accessCriteria = new ArrayList<List<Tuple<ObjectId,Access>>>();
		accessCriteria.add(userAccess);
		options.accessList = accessCriteria;


		SearchResponse resp = searcher.search("dance", options);
		System.out.println(resp.getHits().getTotalHits());
	}

}
