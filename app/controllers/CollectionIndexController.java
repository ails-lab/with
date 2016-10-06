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


package controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import model.DescriptiveData;
import model.annotations.Annotation;
import model.annotations.ContextData;
import model.annotations.ContextData.ContextDataBody;
import model.annotations.bodies.AnnotationBodyTagging;
import model.basicDataTypes.Language;
import model.basicDataTypes.LiteralOrResource;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.MultiLiteralOrResource;
import model.resources.CulturalObject.CulturalObjectData;
import model.resources.PlaceObject.PlaceData;
import model.resources.AgentObject.AgentData;
import model.resources.RecordResource;
import model.resources.ThesaurusObject.SKOSTerm;

import org.bson.types.ObjectId;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Result;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import utils.Counter;
import utils.facets.ThesaurusFacet;
import db.DB;
import db.ThesaurusObjectDAO;

public class CollectionIndexController extends WithResourceController	{

	public static final ALogger log = Logger.of(CollectionObjectController.class);

	public static String[] lookupFields = new String[] {
		"descriptiveData.keywords.uri",
		"descriptiveData.dctype.uri",
		"descriptiveData.dcformat.uri",
		"descriptiveData.dctermsmedium.uri"
	};
	
	public static List<List<String>> termsRestrictionFromJSON(JsonNode json) {
		List<List<String>> uris = null;
		
		if (json != null) {
			JsonNode terms = json.get("terms");
			
			if (terms != null) {
				uris = new ArrayList<>();

				for (Iterator<JsonNode> iter = terms.elements(); iter.hasNext();) {
					JsonNode inode = iter.next();
					List<String> list = new ArrayList<>();
					for (Iterator<JsonNode> iter2 = inode.get("sub").elements(); iter2.hasNext();) {
						list.add(iter2.next().asText());
					}
					
					uris.add(list);
				}
				
			}
		}
		return uris;
	}
	
	public static Result getCollectionFacets(String id) {
		ObjectNode result = Json.newObject();
		
		try {
//			Result response = errorIfNoAccessToCollection(Action.READ, new ObjectId(id));
//			
//			if (!response.toString().equals(ok().toString())) {
//				return response;
//			}
			
			JsonNode json = request().body().asJson();
			
			List<List<String>> uris = termsRestrictionFromJSON(json);
			
			List<String[]> list = new ArrayList<>();
			
			List<String> fields = Arrays.asList(lookupFields);

			for (RecordResource rr : DB.getRecordResourceDAO().getByCollectionWithTerms(new ObjectId(id), uris, fields, fields, -1)) {
				List<Object> olist = new ArrayList<>();

				DescriptiveData dd = rr.getDescriptiveData();
				
				if (dd.getKeywords() != null) {
					List<String> k = dd.getKeywords().getURI();
			
					if (k != null) {
						olist.addAll(k);
					}
				}
				
				if (dd instanceof CulturalObjectData) {
					CulturalObjectData cdd = (CulturalObjectData)dd;
					
					if (cdd.getDctermsmedium() != null) {
						List<String> k = cdd.getDctermsmedium().getURI();
						
						if (k != null) {
							olist.addAll(k);
						}
					}
					
					if (cdd.getDcformat() != null) {
						List<String> k = cdd.getDcformat().getURI();
						
						if (k != null) {
							olist.addAll(k);
						}
					}

					if (cdd.getDctype() != null) {
						List<String> k = cdd.getDctype().getURI();
						
						if (k != null) {
							olist.addAll(k);
						}
					}
				}

				
				if (olist.size() > 0 ) {
					list.add(olist.toArray(new String[olist.size()]));
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
			
//			ObjectId collectionDbId = new ObjectId(id);
//			Result response = errorIfNoAccessToCollection(Action.READ, collectionDbId);
//			
//			if (!response.toString().equals(ok().toString())) {
//				return response;
//			} else {
				return ok(tf.toJSON(Language.EN));
//			}
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}


	public static Result getCollectionAnnotations(String cid) {
		ObjectNode result = Json.newObject();
		
		try {
			Result response = errorIfNoAccessToCollection(Action.READ, new ObjectId(cid));
			
			if (!response.toString().equals(ok().toString())) {
				return response;
			}
			
			List<ContextData<ContextDataBody>> rr = DB.getCollectionObjectDAO().getById(new ObjectId(cid)).getCollectedResources();
			
			Map<BodyClass, Counter> annMap = new HashMap<>();
			
			for (ContextData<ContextDataBody> cd : rr) {
				ObjectId rid = cd.getTarget().getRecordId();

				RecordResource<RecordResource.RecordDescriptiveData> rec = DB.getRecordResourceDAO().getById(rid);
				rec.fillAnnotations();
					
				Set<BodyClass> uris = new HashSet<>();
					
				for (Annotation ann : rec.getAnnotations()) {
					AnnotationBodyTagging body = (AnnotationBodyTagging)ann.getBody();
					if (body.getUri() != null) {
						uris.add(new BodyClass(body.getUri(), body.getLabel()));
					}
				}
				
				for (BodyClass bc : uris) {
					Counter cc = annMap.get(bc);
					if (cc == null) {
						cc = new Counter(0);
						annMap.put(bc, cc);
					}
					
					cc.increase();
				}
			}
			
			Set<SortClass> sorted = new TreeSet<>();
			
			for (Map.Entry<BodyClass, Counter> entry : annMap.entrySet()) {
				sorted.add(new SortClass(entry.getKey().uri, entry.getKey().label, entry.getValue().getValue()));
			}
			
			ArrayNode array = Json.newObject().arrayNode();

			for (SortClass sc : sorted) {
				ObjectNode entry = Json.newObject();
				entry.put("uri", sc.uri);
				entry.put("label", Json.toJson(sc.label));
				entry.put("count", sc.count);
				
				array.add(entry);
			}

			result.put("annotations", array);
			
			return ok(result);
			
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);
		}
	}
	
	public static QueryBuilder getSimilarItemsIndexCollectionQuery(ObjectId colId, DescriptiveData dd) {
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		query.must(QueryBuilders.termQuery("collectedIn", colId));

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
			addMultiLiteralOrResource(((CulturalObjectData)dd).getDctermsmedium(), "dctermsmedium", thesaurusDAO, query);
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
	
	private static class SortClass implements Comparable<SortClass> {
		public String uri;
		public MultiLiteral label;
		public int count;
		
		public SortClass(String uri, MultiLiteral label, int count) {
			this.uri = uri;
			this.label = label;
			this.count = count;
		}

		@Override
		public int compareTo(SortClass so) {
			if (count < so.count) {
				return 1;
			} else if (count > so.count) {
				return -1;
			} else {
				return 0;
			}
		}
	}
	
	private static class BodyClass {
		public String uri;
		public MultiLiteral label;
		
		public BodyClass(String uri, MultiLiteral label) {
			this.uri = uri;
			this.label = label;
		}

		public int hashCode() {
			return uri.hashCode();
		}
		
		public boolean equals(Object obj) {
			if (!(obj instanceof BodyClass)) {
				return false;
			}
			
			return uri.equals(((BodyClass)obj).uri);
		}
	}

}
