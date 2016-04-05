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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import model.DescriptiveData;
import model.basicDataTypes.Language;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.MultiLiteralOrResource;
import model.resources.CulturalObject.CulturalObjectData;
import model.resources.RecordResource.RecordDescriptiveData;
import model.resources.PlaceObject.PlaceData;
import model.resources.AgentObject.AgentData;
import model.resources.ThesaurusObject.SKOSTerm;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
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
import akka.japi.Util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import controllers.CollectionObjectController;
import controllers.WithResourceController;
import db.DB;
import db.ThesaurusObjectDAO;
import elastic.ElasticSearcher;
import elastic.ElasticSearcher.SearchOptions;

public class CollectionIndexController extends WithResourceController	{

	public static final ALogger log = Logger.of(CollectionObjectController.class);

	public static String[] indexFacetFields = new String[] {
		"keywords.uri.all",
		"dctype.uri.all",
		"dcformat.uri.all"
	};

	
	public static Result getCollectionIndex(String id) {
		ObjectNode result = Json.newObject();
		
		try {
			JsonNode json = request().body().asJson();

//			System.out.println("QUERYING FOR TREE CONSTUCTION");
			ElasticSearcher es = new ElasticSearcher();
			
//			MatchQueryBuilder query = QueryBuilders.matchQuery("collectedIn.collectionId", id);
			QueryBuilder query = getIndexCollectionQuery(new ObjectId(id), json);

			SearchResponse res = es.execute(query, new SearchOptions(0, Integer.MAX_VALUE), indexFacetFields);
			SearchHits sh = res.getHits();

			List<String[]> list = new ArrayList<>();

			for (Iterator<SearchHit> iter = sh.iterator(); iter.hasNext();) {
				SearchHit hit = iter.next();

				List<Object> olist = new ArrayList<>();
				
				for (String field : indexFacetFields) {
					SearchHitField shf = hit.field(field);
				
					if (shf != null) {
						olist.addAll(shf.getValues());
					}				
				}				
				
				if (olist.size() > 0 ) {
					list.add(olist.toArray(new String[] {}));
				}
			}
			
			Set<String> selected = new HashSet<>();
			if (json != null) {
				for (Iterator<JsonNode> iter = json.get("terms").elements(); iter.hasNext();) {
					selected.add(iter.next().get("top").asText());
				}
			}
			
			ThesaurusFacet tf = new ThesaurusFacet();
			tf.create(list, selected);
			
			ObjectId collectionDbId = new ObjectId(id);
			Result response = errorIfNoAccessToCollection(Action.READ, collectionDbId);
			
			if (!response.toString().equals(ok().toString()))
				return response;
			else {
				return ok(tf.toJSON(Language.EN));
			}
		} catch (Exception e) {
//			e.printStackTrace();
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}
	
	public static QueryBuilder getIndexCollectionQuery(ObjectId colId, JsonNode json) {
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		query.must(QueryBuilders.termQuery("collectedIn.collectionId", colId));

		if (json != null) {
			for (Iterator<JsonNode> iter = json.get("terms").elements(); iter.hasNext();) {
				BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
	
				JsonNode inode = iter.next();
				for (Iterator<JsonNode> iter2 = inode.get("sub").elements(); iter2.hasNext();) {
					String s = iter2.next().asText();
	
					for (String f : indexFacetFields) {
						boolQuery = boolQuery.should(QueryBuilders.termQuery(f, s));
					}
				}
				
				query.must(boolQuery);
			}
		}

		return query;
	}

	public static QueryBuilder getSimilarItemsIndexCollectionQuery(ObjectId colId, DescriptiveData dd) {
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		query.must(QueryBuilders.termQuery("collectedIn.collectionId", colId));

		ThesaurusObjectDAO thesaurusDAO = DB.getThesaurusDAO();
		
		MultiLiteral label = dd.getLabel();
		if (label != null) {
			for (Map.Entry<String, List<String>> entry : label.entrySet()) {
				String lang = entry.getKey();
				if (lang.equals(Language.DEFAULT.getDefaultCode())) {
					continue;
				}
				lang = lang.toLowerCase();
				
				for (String v : entry.getValue()) {
					query.should(QueryBuilders.matchQuery("label." + lang, v));	
					query.should(QueryBuilders.matchQuery("altLabels." + lang, v));
				}
			}
		}
		
		MultiLiteral altlabel = dd.getAltLabels();
		if (altlabel != null) {
			for (Map.Entry<String, List<String>> entry : altlabel.entrySet()) {
				String lang = entry.getKey();
				if (lang.equals(Language.DEFAULT.getDefaultCode())) {
					continue;
				}
				lang = lang.toLowerCase();
				
				for (String v : entry.getValue()) {
					query.should(QueryBuilders.matchQuery("label." + lang, v));	
					query.should(QueryBuilders.matchQuery("altLabels." + lang, v));
				}
			}
		}
		
		MultiLiteral descr = dd.getDescription();
		if (descr != null) {
			for (Map.Entry<String, List<String>> entry : descr.entrySet()) {
				String lang = entry.getKey();
				if (lang.equals(Language.DEFAULT.getDefaultCode())) {
					continue;
				}
				lang = lang.toLowerCase();

				for (String v : entry.getValue()) {
					query.should(QueryBuilders.matchQuery("description." + lang, v));
				}
			}
		}
		
		addMultiLiteralOrResource(dd.getKeywords(), "keywords", thesaurusDAO, query);
		
		if (dd instanceof CulturalObjectData) {
			addMultiLiteralOrResource(((CulturalObjectData)dd).getDctype(), "dctype", thesaurusDAO, query);
			addMultiLiteralOrResource(((CulturalObjectData)dd).getDcformat(), "dcformat", thesaurusDAO, query);
		} else if (dd instanceof PlaceData) {
			addMultiLiteralOrResource(((PlaceData)dd).getNation(), "nation", thesaurusDAO, query);
			addMultiLiteralOrResource(((PlaceData)dd).getContinent(), "continent", thesaurusDAO, query);
			addMultiLiteralOrResource(((PlaceData)dd).getPartOfPlace(), "partofplace", thesaurusDAO, query);
		} else if (dd instanceof AgentData) {
			addMultiLiteralOrResource(((AgentData)dd).getBirthPlace(), "birthplace", thesaurusDAO, query);
		}
		
//		System.out.println(query);

		return query;
	}
	
	private static void addMultiLiteralOrResource(MultiLiteralOrResource source, String field, ThesaurusObjectDAO thesaurusDAO, BoolQueryBuilder query) {
		if (source != null) {
			List<String> uris = source.get(LiteralOrResource.URI);
			if (uris != null) {
				Set<String> broader = new HashSet<>();
				for (String uri : uris) {
					List<SKOSTerm> terms = thesaurusDAO.getByUri(uri).getSemantic().getBroaderTransitive();
					if (terms != null) {
						for (SKOSTerm t : terms) {
							broader.add(t.getUri());
						}
					}
				}
				
				for (String f : uris) {
					query.should(QueryBuilders.termQuery(field + ".uri.all", f).boost(4));
				}
				
				for (String f : broader) {
					query.should(QueryBuilders.termQuery(field + ".uri.all", f).boost(2));
				}
			}
			
			for (Map.Entry<String, List<String>> entry : source.entrySet()) {
				String lang = entry.getKey();
				if (lang.equals(LiteralOrResource.URI) || lang.equals(Language.DEFAULT.getDefaultCode())) {
					continue;
				}
				lang = lang.toLowerCase();
				
				for (String v : entry.getValue()) {
					query.should(QueryBuilders.matchQuery(field + "." + lang, v));
				}
			}
		}
	}

}
