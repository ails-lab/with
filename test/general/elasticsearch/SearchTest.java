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
import model.resources.WithResource;
import model.resources.WithResource.WithResourceType;
import model.resources.collection.CollectionObject;
import model.usersAndGroups.User;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.suggest.Suggest;
import org.junit.Test;

import com.sun.org.apache.xalan.internal.xsltc.compiler.sym;

import db.DB;
import search.Filter;
import search.Query;
import search.Response.SingleResponse;
import utils.Tuple;
import elastic.Elastic;
import elastic.ElasticCoordinator;
import elastic.ElasticSearcher;
import elastic.ElasticSearcher.SearchOptions;

public class SearchTest {

	@Test
	public void testFederatedSearch() {

		ElasticSearcher searcher = new ElasticSearcher();
		searcher.addType(Elastic.typeResource);

		SearchOptions options = new SearchOptions(0, 10);
		options.setScroll(false);


		Query q = new Query();
		q.count = 20;
		q.start = 0;
		List<List<Filter>> filters = new ArrayList<List<Filter>>();
		List<Filter> ors = new ArrayList<Filter>();
		Filter f1 = new Filter("label", "dance");
		ors.add(f1);
		options.count = q.count;
		options.offset = q.start;


		ElasticCoordinator proxy = new ElasticCoordinator(options);
		SingleResponse sr = proxy.federatedSearch(filters);
		System.out.println(sr.items);
	}

	@Test
	public void testRelatedWithDisMax() {
		ElasticSearcher searcher = new ElasticSearcher();
		searcher.addType(Elastic.typeResource);

		SearchOptions options = new SearchOptions(0, 10);
		options.setScroll(false);
		/*options.addFilter("dataProvider", "");
		options.addFilter("dataProvider", "");
		options.addFilter("provider", "");*/

		/*SearchResponse resp = searcher.relatedWithDisMax("eirinirecord1", "mint", null, options);
		for(SearchHit h: resp.getHits().getHits()) {
			System.out.println(h.getSourceAsString());
		}*/

	}


	@Test
	public void testRelatedWithMLT() {
		ElasticSearcher searcher = new ElasticSearcher();
		searcher.addType(Elastic.typeResource);

		SearchOptions options = new SearchOptions(0, 10);
		options.setScroll(false);

		/*options.addFilter("dataProvider", "");
		options.addFilter("dataProvider", "");
		options.addFilter("provider", "");*/

		List<String> fields = new ArrayList<String>() {{ add("label_all");add("description_all");add("provider"); }};
		/*SearchResponse resp = searcher.relatedWithMLT("title Mint", null, fields, options);
		for(SearchHit h: resp.getHits().getHits()) {
			System.out.println(h.getSourceAsString());
		}*/
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

	@Test
	public void testListUserCollections() {

		User u = DB.getUserDAO().getByUsername("qwerty");
		if(u==null) return;

		ElasticSearcher searcher = new ElasticSearcher();
		searcher.addType(WithResourceType.CollectionObject.toString().toLowerCase());
		SearchOptions options = new SearchOptions();
		List<Tuple<ObjectId, Access>> user_acl = new ArrayList<Tuple<ObjectId,Access>>();
		user_acl.add(new Tuple<ObjectId, WithAccess.Access>(u.getDbId(), Access.OWN));


		/*SearchResponse resp = searcher.searchAccessibleCollections(options);


		Tuple<List<CollectionObject>, Tuple<Integer, Integer>> tree =
				DB.getCollectionObjectDAO().getByAcl(options.accessList, u.getDbId(), false, true, 0, 200);

		assertEquals((int)resp.getHits().getTotalHits(), (int)tree.y.x);
*/
	}

	@Test
	public void testSearchMycollections() {

	}

	@Test
	public void testSearchWithinCollection() {

	}
}
