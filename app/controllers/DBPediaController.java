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
import java.util.Iterator;
import java.util.List;

import model.basicDataTypes.Language;
import model.basicDataTypes.MultiLiteral;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.RDFNode;

import play.libs.Json;
import play.mvc.Result;
import sources.core.ApacheHttpConnector;
import sources.core.QueryBuilder;

public class DBPediaController extends WithResourceController {

	private static String DBPEDIA_INDEX = "http://zenon.image.ece.ntua.gr:8983/solr/dbpedia_with/select";
	private static String DBPEDIA_STORE = "http://zenon.image.ece.ntua.gr:8890/sparql";

	public static Result dbpediaLookup(String type, String query, int start, int count) {
		
		ObjectNode result = Json.newObject();
		
		try {
			result = doLookup(type, query, start, count);
			return ok();
			
		} catch (Exception e) {
			result.put("error", e.getMessage());
			return internalServerError(result);			
		}
	}

	
	public static ObjectNode doLookup(String type, String query, int start, int count) throws Exception {
	
		ObjectNode result = Json.newObject();
			
		String[] types = type.split(",");
			
		StringBuffer qs = new StringBuffer("+(");
		
		for (int i = 0; i < types.length; i++) {
			if (i > 0) {
				qs.append(" ");
			}
			qs.append("type: \"http://dbpedia.org/ontology/" + types[i] +"\"");
		}
		qs.append(")");
		
		qs.append(" ");
		
		query = query.replaceAll("[,.:'\"\\(\\)\\[\\]!\\-\\+\\?\\*\\^/]", " ");
		query = query.replaceAll("\b(AND|OR|NOT)\b", " ");
		
		String[] words = query.split("\\s+");
		
		qs.append("+(");
		for (int i = 0; i < words.length; i++) {
			if (i > 0) {
				qs.append(" ");
			}
			qs.append("+label.en:" + words[i]);
		}
		qs.append(")");
		
		QueryBuilder builder = new QueryBuilder(DBPEDIA_INDEX);
		builder.setQuery("q", qs.toString());
		builder.addSearchParam("fl", "uri,type");
		builder.addSearchParam("wt", "json");
		builder.addSearchParam("start", start + "");
		builder.addSearchParam("rows", count + "");
		
		JsonNode response = ApacheHttpConnector.getApacheHttpConnector().getURLContent(builder.getHttp());
		
		int matchesNo = response.get("response").get("numFound").asInt();
		result.put("totalcount", matchesNo);
		
		ArrayNode array = Json.newObject().arrayNode();
		
		JsonNode docs = response.get("response").get("docs");
		
		for (Iterator<JsonNode> iter = docs.elements(); iter.hasNext();) {
			JsonNode element = iter.next();
		
			String uri = element.get("uri").textValue();
		
			ObjectNode doc = Json.newObject();
		
			doc.put("uri", uri);
		
			JsonNode etypes = element.get("type");
			if (etypes != null) {
				ArrayNode doctypes = Json.newObject().arrayNode();
		
				for (Iterator<JsonNode> itert = etypes.elements(); itert.hasNext();) {
					doctypes.add(itert.next().asText());
				}
		
				doc.put("type", doctypes);
			}				
		
			String sparql = "select ?p ?v where  { <" + uri + "> ?p ?v }";
		
			QueryExecution qe = QueryExecutionFactory.sparqlService(DBPEDIA_STORE, QueryFactory.create(sparql, Syntax.syntaxSPARQL));
			ResultSet rst = qe.execSelect();
		
			List<RDFNode> label = new ArrayList<>();
			List<RDFNode> abstr = new ArrayList<>();
			RDFNode birthDate = null;
			RDFNode deathDate = null;
			List<RDFNode> birthPlace = new ArrayList<>();
			RDFNode depiction = null;
			RDFNode thumbnail = null;
			List<RDFNode> country = new ArrayList<>();
			List<RDFNode> subject = new ArrayList<>();
		
			while (rst.hasNext()) {
				QuerySolution sol = rst.next();
				String p = sol.get("?p").toString();
				RDFNode t = sol.get("?v");
		
				if (p.equals("http://www.w3.org/2000/01/rdf-schema#label")) {
					label.add(t);
				} else if (p.equals("http://dbpedia.org/ontology/abstract")) {
					abstr.add(t);
				} else if (p.equals("http://dbpedia.org/ontology/birthDate")) {
					if (birthDate == null) {
						birthDate = t;
					}
				} else if (p.equals("http://dbpedia.org/ontology/deathDate")) {
					if (deathDate == null) {
						deathDate = t;
					}
				} else if (p.equals("http://dbpedia.org/ontology/birthPlace")) {
					birthPlace.add(t);
				} else if (p.equals("http://xmlns.com/foaf/0.1/depiction")) {
					if (depiction == null) {
						depiction = t;
					}
				} else if (p.equals("http://dbpedia.org/ontology/thumbnail")) {
					if (thumbnail == null) {
						thumbnail = t;
					}
				} else if (p.equals("http://dbpedia.org/ontology/country")) {
					country.add(t);
				} else if (p.equals("http://purl.org/dc/terms/subject")) {
					subject.add(t);						
				}
			}
		
					
			doc.put("label", literal2Json(label));
			doc.put("abstract", literal2Json(abstr));
			if (birthDate != null) {
				doc.put("birthdate", birthDate.asLiteral().getString().toString());
			}
			if (deathDate != null) {
				doc.put("deathdate", deathDate.asLiteral().getString().toString());
			}
			doc.put("birthplace", uris2Array(birthPlace));
			if (depiction != null) {
				doc.put("depiction", depiction.toString());
			}
			if (thumbnail != null) {
				doc.put("thumbnail", thumbnail.toString());
			}
			doc.put("country", uris2Array(country));
			doc.put("subject", uris2Array(subject));
		
			array.add(doc);
		}
				
		result.put("results", array);
				
		return result;
	}
		
	private static ArrayNode uris2Array(List<RDFNode> list) {
		ArrayNode array = Json.newObject().arrayNode();
			
		for (RDFNode t : list) {
			array.add(t.toString());
		}
			
		return array;
	}
		
	private static JsonNode literal2Json(List<RDFNode> list) {
		MultiLiteral ml = new MultiLiteral();
				
		for (RDFNode t : list) {
			String s = t.asLiteral().getString();
			String l = t.asLiteral().getLanguage();
				
			if (l != null) {
				ml.addLiteral(Language.getLanguageByCode(l), s);
			} else {
				ml.addLiteral(Language.UNKNOWN, s);
			}
		}
		
		return Json.toJson(ml);
	}
}
