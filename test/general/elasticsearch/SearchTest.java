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
import java.util.Map;

import model.basicDataTypes.WithAccess;
import model.basicDataTypes.WithAccess.Access;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.suggest.Suggest;
import org.junit.Test;

import com.sun.org.apache.xalan.internal.xsltc.compiler.sym;

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

	@Test
	public void testRelatedWithDisMax() {
		ElasticSearcher searcher = new ElasticSearcher();
		searcher.addType(Elastic.typeResource);

		SearchOptions options = new SearchOptions(0, 10);
		options.setScroll(false);
		options.setFilterType("and");
		/*options.addFilter("dataProvider", "");
		options.addFilter("dataProvider", "");
		options.addFilter("provider", "");*/

		SearchResponse resp = searcher.relatedWithDisMax("eirinirecord1", "mint", null, options);
		for(SearchHit h: resp.getHits().getHits()) {
			System.out.println(h.getSourceAsString());
		}

	}


	@Test
	public void testRelatedWithMLT() {
		ElasticSearcher searcher = new ElasticSearcher();
		searcher.addType(Elastic.typeResource);

		SearchOptions options = new SearchOptions(0, 10);
		options.setScroll(false);
		options.setFilterType("or");
		options.addFilter("isPublic", "false");

		/*options.addFilter("dataProvider", "");
		options.addFilter("dataProvider", "");
		options.addFilter("provider", "");*/

		List<String> fields = new ArrayList<String>() {{ add("label_all");add("description_all");add("provider"); }};
		SearchResponse resp = searcher.relatedWithMLT("title Mint", null, fields, options);
		for(SearchHit h: resp.getHits().getHits()) {
			System.out.println(h.getSourceAsString());
		}
	}

	@Test
	public void testRelatedWithShouldClauses() {

	}

	@Test
	public void testSearchAccessibleCollections() {

	}

	@Test
	public void testSearchSuggestions() {
		ElasticSearcher searcher = new ElasticSearcher();
		searcher.addType(Elastic.typeResource);

		SearchOptions options = new SearchOptions(0, 10);
		options.setScroll(false);
	  /*options.addFilter("dataProvider", "");
		options.addFilter("dataProvider", "");
		options.addFilter("provider", "");*/

		SuggestResponse resp = searcher.searchSuggestions("eirimnnirecord1", "label_all", options);
		resp.getSuggest().getSuggestion("eirimnnirecord1").forEach( (o) ->  (o.forEach( (s) -> (
								System.out.println(s.getText() + " with score " + s.getScore())) )) );
		System.out.println("done");
	}
}
