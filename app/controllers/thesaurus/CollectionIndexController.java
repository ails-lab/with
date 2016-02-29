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


package controllers.thesaurus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import model.basicDataTypes.Language;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Result;
import utils.AccessManager.Action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import controllers.CollectionObjectController;
import controllers.WithResourceController;
import elastic.ElasticSearcher;
import elastic.ElasticSearcher.SearchOptions;

public class CollectionIndexController extends WithResourceController	{

	public static final ALogger log = Logger.of(CollectionObjectController.class);

	public static Result getCollectionIndex(String id) {
		ObjectNode result = Json.newObject();
		
		JsonNode json = request().body().asJson();
//		JsonNode json = null;
		
		try {
			ElasticSearcher es = new ElasticSearcher();
			
//			MatchQueryBuilder query = QueryBuilders.matchQuery("collectedIn.collectionId", id);
			QueryBuilder query = CollectionObjectController.getIndexCollectionQuery(new ObjectId(id), json);

			SearchOptions so = new SearchOptions(0, Integer.MAX_VALUE);

			String[] fields = new String[] { "_id", "keywords.uri", "dctype.uri" };
			
			SearchResponse res = es.execute(query, so, fields);
			
			SearchHits sh = res.getHits();

			List<String[]> list = new ArrayList<>();

			Set<Object> all = new HashSet<>();
			
			for (Iterator<SearchHit> iter = sh.iterator(); iter.hasNext();) {
				SearchHit hit = iter.next();

				SearchHitField keywords = hit.field("keywords.uri");
				SearchHitField dctypes = hit.field("dctype.uri");

				List<Object> olist = new ArrayList<>();
				
				if (keywords != null) {
					olist.addAll(keywords.getValues());
					
					all.addAll(keywords.getValues());
				}				
				
				if (dctypes != null) {
					olist.addAll(dctypes.getValues());
					
					all.addAll(dctypes.getValues());
				}			
				
				if (olist.size() > 0 ) {
					list.add(olist.toArray(new String[] {}));
				}
			}
			
			ThesaurusFacet tf = new ThesaurusFacet();
			tf.create(list);
			
			ObjectId collectionDbId = new ObjectId(id);
			Result response = errorIfNoAccessToCollection(Action.READ, collectionDbId);
			
			if (!response.toString().equals(ok().toString()))
				return response;
			else {
				return ok(tf.toJSON(Language.EN));
			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}
	
}
