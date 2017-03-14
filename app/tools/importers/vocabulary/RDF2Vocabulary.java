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


package tools.importers.vocabulary;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.basicDataTypes.MultiLiteral;
import net.minidev.json.JSONObject;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.jena.atlas.lib.SetUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class RDF2Vocabulary extends OWL2Vocabulary {

	public static OWLImportConfiguration schema = new OWLImportConfiguration("schema", 
	       	"Schema.org", 
	        "schema", 
	        "2016-11-03", 
	        "schema.org",
	        null, 
			null,
			Language.EN,
			"http://schema.org/Thing");
	
	
	public static void doImport(OWLImportConfiguration... confs) {
		RDF2Vocabulary s2v = new RDF2Vocabulary();
		for (OWLImportConfiguration c : confs) {
			try {
				s2v.doImport(c);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private OWLImportConfiguration conf;
	protected void doImport(OWLImportConfiguration confx) throws OWLOntologyCreationException, IOException {
		this.conf = confx;

		File tmpFolder = VocabularyImportConfiguration.getTempFolder();
		
		Model model = conf.readModel(tmpFolder);

		File outFile = new File(tmpFolder + File.separator + conf.folder + ".txt");

		ObjectNode vocabulary = Json.newObject();
		vocabulary.put("name", conf.prefix);
		vocabulary.put("version", conf.version);
		
		System.out.println("Creating vocabulary in " + outFile);
		try (FileWriter fr = new FileWriter(outFile); 
		      BufferedWriter br = new BufferedWriter(fr)) {
	
			String queryString;
			Query query;
			
			queryString = "SELECT ?subject WHERE {?subject a <http://www.w3.org/2000/01/rdf-schema#Class>}";
			query = QueryFactory.create(queryString) ; 
	
			try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
				for (ResultSet results = qexec.execSelect(); results.hasNext() ; ) {
					QuerySolution qs = results.next();
					
					String xuri = qs.get("subject").asResource().getURI();
					
					if (xuri.equals(conf.top)) {
						continue;
					}
					
					ObjectNode main = makeMainStructure(xuri, model, "http://www.w3.org/2004/02/skos/core#Concept");
					
					String uri = "<" + xuri + ">";
							
					JsonNode scopeNote = makeLiteralNode("SELECT ?literal WHERE {" + uri + " <http://www.w3.org/2000/01/rdf-schema#comment> ?literal}", model, "literal");
					if (scopeNote != null) {
						main.put("scopeNote", scopeNote);
					}
					
					ArrayNode broader = makeNodesArray("SELECT ?q WHERE {" + uri + " <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?q}", model, "q");
					if (broader != null) {
						main.put("broader", broader);
					}
				
					ArrayNode broaderTransitive = makeNodesArray("SELECT ?q WHERE {" + uri + " <http://www.w3.org/2000/01/rdf-schema#subClassOf>+ ?q}", model, "q");
					if (broaderTransitive != null) {
						main.put("broaderTransitive", broaderTransitive);
					}
				
					ArrayNode narrower = makeNodesArray("SELECT ?q WHERE {?q <http://www.w3.org/2000/01/rdf-schema#subClassOf> " + uri + "}", model, "q");
					if (narrower != null) {
						main.put("narrower", narrower);
					}

//					ArrayNode related = makeNodesArray("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT DISTINCT ?q WHERE {{SELECT ?q WHERE {" + uri + " skos:related ?q}} UNION {SELECT ?q WHERE {?q skos:related " + uri + "}}}", model, "q");
//					if (related != null) {
//						main.put("related", related);
//					}
					
					ArrayNode exactMatch = makeURIArrayNode("SELECT ?uri WHERE {" + uri + " <http://www.w3.org/2002/07/owl#equivalentClass> ?uri}", model, "uri");
					if (exactMatch != null) {
						main.put("exactMatch", exactMatch);
					}
					
//					ArrayNode closeMatch = makeURIArrayNode("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?uri WHERE {" + uri + " skos:closeMatch ?uri}", model, "uri");
//					if (closeMatch != null) {
//						main.put("closeMatch", closeMatch);
//					}
					
					main.put("vocabulary", vocabulary);

					ObjectNode json = Json.newObject();
					json.put("semantic", main);

					br.write(json.toString());
					br.write(VocabularyImportConfiguration.newLine);

				}
			}
		}
		
		conf.cleanUp(tmpFolder);
		
	}
	
	protected JsonNode makeLiteralNode(String queryString, Model model, String var) {
		Literal literal = new Literal();
		Query query = QueryFactory.create(queryString) ;
		try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
			
			for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
				QuerySolution s = res.next();
				org.apache.jena.rdf.model.Literal lit = s.get(var).asLiteral();
				String lang = lit.getLanguage();
				Language ll = null;
				if (lang != null) {
					ll = Language.getLanguageByCode(lang);
				}
				
				if ((ll == null || ll == Language.UNKNOWN) && conf.defaultLanguage != null) {
					ll = conf.defaultLanguage;
				}
				
				literal.addLiteral(ll, lit.getString());
			}
		}
		
		if (literal.size() > 0) {
			return Json.toJson(literal);
		} else {
			return null;
		}
	}
	
	protected ObjectNode makeMainStructure(String urit, Model model, String manualType) {
		Literal prefLabel = new Literal();
		MultiLiteral altLabel = new MultiLiteral();
		
		String uri = "<" + urit + ">";
		
		String queryString;
		Query query;
		
		queryString = "SELECT ?literal WHERE {" + uri + " <http://www.w3.org/2000/01/rdf-schema#label> ?literal}";
		query = QueryFactory.create(queryString);
		
		try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
			for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
				QuerySolution s = res.next();
				org.apache.jena.rdf.model.Literal lit = s.get("literal").asLiteral();
				String lang = lit.getLanguage();
				Language ll = null;
				if (lang != null) {
					ll = Language.getLanguageByCode(lang);
				}
				
				if ((ll == null || ll == Language.UNKNOWN) && conf.defaultLanguage != null) {
					ll = conf.defaultLanguage;
				}
				
				prefLabel.addLiteral(ll, JSONObject.escape(lit.getString()));
			}
		}
		
		String type = null;
		if (manualType == null) {
	
			queryString = "SELECT ?type WHERE {" + uri + " a ?type}";
			query = QueryFactory.create(queryString) ;
			
			try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
				for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
					QuerySolution s = res.next();
					String t = s.get("type").asResource().getURI();
					if (t.contains("skos/core#")) {
						type = t;
						break;
					}
				}
			}
		} else {
			type = manualType;
		}
		
		ObjectNode json = Json.newObject();
		json.put("uri", urit);
		
		if (type != null) {
			json.put("type", type);
		}
		
		if (prefLabel.size() > 0) {
			json.put("prefLabel", Json.toJson(prefLabel));
			
		}
		if (altLabel.size() > 0) {
			json.put("altLabel", Json.toJson(altLabel));
		}

		return json;
	}
	
	protected Set<String> getNodesArray(String queryString, Model model, String var) {
		Set<String> ret = new HashSet<>();
		Query query = QueryFactory.create(queryString) ;
		try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
			
			for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
				QuerySolution s = res.next();
				if (s.get(var) != null) {
					String uri = s.get(var).asResource().getURI();
					if (!uri.equals(conf.top)) {
						ret.add(uri);
					}
				}
			}
		}
		
		return ret;
	}
	
	protected ArrayNode makeNodesArray(Set<String> res, Model model) {
		if (res.size() > 0) {
			ArrayNode array = Json.newObject().arrayNode();
		
			for (String s : res) {
				if (!s.equals(conf.top)) {
					array.add(makeMainStructure(s, model, "http://www.w3.org/2004/02/skos/core#Concept"));
				}
			}
			return array;
		} else {
			return null;
		}
	}
	
	protected ArrayNode makeURIArrayNode(String queryString, Model model, String var) {
		ArrayNode array = Json.newObject().arrayNode();
		
		Query query = QueryFactory.create(queryString) ;
		try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
			
			for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
				QuerySolution s = res.next();
				String uri = s.get(var).asResource().getURI();
				
				if (!uri.equals(conf.top)) {
					array.add(uri);
				}
			}
		}
		
		if (array.size() > 0) {
			return array;
		} else {
			return null;
		}
	}
	
	protected ArrayNode makeNodesArray(String queryString, Model model, String var) {
		return makeNodesArray(getNodesArray(queryString, model, var), model);
	}
}
