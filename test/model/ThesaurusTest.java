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


package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;

import model.annotations.Annotation;
import model.annotations.bodies.AnnotationBodyTagging;
import model.basicDataTypes.Language;
import model.resources.CulturalObject.CulturalObjectData;
import model.resources.RecordResource;
import play.libs.Json;
import utils.facets.ThesaurusFacet;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.CollectionIndexController;
import db.DB;
import elastic.Elastic;
import elastic.ElasticSearcher;
import elastic.ElasticSearcher.SearchOptions;

public class ThesaurusTest {

	public static void main(String[] args) {
		
		try {
//			Result response = errorIfNoAccessToCollection(Action.READ, new ObjectId(id));
//			
//			if (!response.toString().equals(ok().toString())) {
//				return response;
//			}
			
			String id = "57daa4d775fe24417dccbc51";
//			String id = "57ed16c71813044ae00de553";
//			String id = "58188182d4a0c1377410bb79";
			
//			JsonNode json = Json.parse("{\"terms\":[\"http://thesaurus.europeanafashion.eu/thesaurus/10412\"]}");
			JsonNode json = Json.parse("{\"terms\":[]}");
			
			QueryBuilder query = CollectionIndexController.getIndexCollectionQuery(new ObjectId(id),json);
			
			System.out.println(query);
			
			SearchOptions so = new SearchOptions(0, 10000);
			so.isPublic = false;
			
			SearchResponse scrollResp = new ElasticSearcher().getSearchRequestBuilder(query, so)
			        .setScroll(new TimeValue(60000))
			        .setQuery(query)
			        .addFields(CollectionIndexController.retrievedFields)

			        .setSize(10000).execute().actionGet(); 
			
			List<String[]> list = new ArrayList<>();

			while (true) {
			    for (SearchHit hit : scrollResp.getHits().getHits()) {
			    	List<Object> olist = new ArrayList<>();
			    	
			    	for (String field : CollectionIndexController.retrievedFields) {
			    		SearchHitField shf = hit.field(field);
			    		if (shf != null) {
			    			List<Object> values = shf.getValues();
			    			if (values != null) {
					    		olist.addAll(values);
					    	}
			    		}
			    	}
			    	
					if (olist.size() > 0 ) {
						list.add(olist.toArray(new String[olist.size()]));
					}
			    }
			    
			    scrollResp = Elastic.getTransportClient().prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(60000)).execute().actionGet();
			    if (scrollResp.getHits().getHits().length == 0) {
			        break;
			    }
			}
	
			Set<String> selected = new HashSet<>();

			if (json != null) {
				for (Iterator<JsonNode> iter = json.get("terms").elements(); iter.hasNext();) {
					selected.add(iter.next().asText());
				}
			}
			
			ThesaurusFacet tf = new ThesaurusFacet(Language.EN);
			tf.create(list, selected);

			
			System.out.println(tf.toJSON());
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
