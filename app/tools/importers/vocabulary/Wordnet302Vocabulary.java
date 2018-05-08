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

import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.basicDataTypes.MultiLiteral;
import net.minidev.json.JSONObject;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import play.libs.Json;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class Wordnet302Vocabulary extends Data2Vocabulary<SKOSImportConfiguration> {

	private static SKOSImportConfiguration wn30 = new SKOSImportConfiguration("wn30", 
	       	"Wordnet 3.0", 
	        "wn30", 
	        "3.0", 
	        null,
	        "http://wordnet-rdf.princeton.edu/wn30/", 
	        null,
	        null);

	public static SKOSImportConfiguration[] confs = new SKOSImportConfiguration[] { wn30 };
	
	public static void doImport(SKOSImportConfiguration[] confs) {
		Wordnet302Vocabulary s2v = new Wordnet302Vocabulary();
		for (SKOSImportConfiguration c : confs) {
			try {
				s2v.doImport(c);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private static String prefix = "http://www.w3.org/2006/03/wn/wn20/schema/";
	
	protected void doImport(SKOSImportConfiguration conf) throws OWLOntologyCreationException, IOException {
		
		File tmpFolder = VocabularyImportConfiguration.getTempFolder();
		
		Model model = conf.readModel(tmpFolder);

		File outFile = new File(tmpFolder + File.separator + conf.folder + ".txt");
//
		ObjectNode vocabulary = Json.newObject();
		vocabulary.put("name", conf.prefix);
		vocabulary.put("version", conf.version);
		
		System.out.println("Creating vocabulary in " + outFile);
		try (FileWriter fr = new FileWriter(outFile);
			BufferedWriter br = new BufferedWriter(fr)) {
	
//			Set<ObjectNode> top = new HashSet<>();
			
			for (String type : new String[] { "NounSynset", "VerbSynset", "AdjectiveSynset", "AdverbSynset", "AdjectiveSatelliteSynset" }) {
				
				ObjectNode cmain = Json.newObject();
				cmain.put("uri", prefix + type);
				cmain.put("type", "http://www.w3.org/2004/02/skos/core#ConceptScheme");
				
				ArrayNode array = Json.newObject().arrayNode();
				cmain.put("topConcepts", array);
				
				cmain.put("vocabulary", vocabulary);

				ObjectNode cjson = Json.newObject();
				cjson.put("semantic", cmain);
				
				String queryString;
				Query query;
				
				queryString = "PREFIX wn20schema: <http://www.w3.org/2006/03/wn/wn20/schema/> SELECT ?word WHERE {?word a wn20schema:" + type + "}" ;
				query = QueryFactory.create(queryString) ; 
				
				try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
					for (ResultSet results = qexec.execSelect(); results.hasNext() ; ) {
						QuerySolution qs = results.next();
						
						String uri = qs.get("word").asResource().getURI();
						
						ObjectNode main = makeMainStructure(uri, model);

						ArrayNode broader = makeNodesArray("PREFIX wn20schema: <http://www.w3.org/2006/03/wn/wn20/schema/> SELECT ?q WHERE {<" + uri + "> wn20schema:hyponymOf ?q }", model, "q");
						if (broader != null) {
							main.put("broader", broader);
						} else {
							array.add(main);
//							top.add(json);
						}
						
						ArrayNode narrower = makeNodesArray("PREFIX wn20schema: <http://www.w3.org/2006/03/wn/wn20/schema/> SELECT ?q WHERE {?q wn20schema:hyponymOf <" + uri + ">}", model, "q");
						if (narrower != null) {
							main.put("narrower", narrower);
						}
						
						ArrayNode broaderTransitive = makeNodesArray("PREFIX wn20schema: <http://www.w3.org/2006/03/wn/wn20/schema/> SELECT ?q WHERE {<" + uri + "> wn20schema:hyponymOf+ ?q }", model, "q");
						if (broaderTransitive != null) {
							main.put("broaderTransitive", broaderTransitive);
						}
						
						ArrayNode schemes = Json.newObject().arrayNode();
						schemes.add(prefix + type);
						
						main.put("inSchemes", schemes);
						main.put("vocabulary", vocabulary);
	
						ObjectNode json = Json.newObject();
						json.put("semantic", main);
	
						br.write(json.toString());
						br.write(VocabularyImportConfiguration.newLine);
						
						main.remove("vocabulary");
						main.remove("narrower");
						main.remove("broader");
						main.remove("broaderTransitive");
						main.remove("inSchemes");
	
					}
				}
				
				br.write(cjson.toString());
				br.write(VocabularyImportConfiguration.newLine);
			}
			
//			ObjectNode jtop = Json.newObject();
//			ObjectNode topc = Json.newObject();
//			topc.put("uri", conf.mainScheme);
//			topc.put("type", "http://www.w3.org/2004/02/skos/core#ConceptScheme");
//			
//			ObjectNode prefLabel = Json.newObject();
//			prefLabel.put("en", conf.title);
//			topc.put("prefLabel", prefLabel);
//				
//			ArrayNode arr = Json.newObject().arrayNode();
//			for (ObjectNode t : top) {
//				t.remove("narrower");
//				t.remove("broader");
//				t.remove("broaderTransitive");
//				
//				arr.add(t);
//			}
//				
//			topc.put("topConcepts", arr);
//			topc.put("vocabulary", vocabulary);
//
//			jtop.put("semantic", topc);
//				
//			br.write(jtop.toString());
		}
		
		conf.cleanUp(tmpFolder);
		
	}

	protected ArrayNode makeNodesArray(String queryString, Model model, String var) {
		ArrayNode array = Json.newObject().arrayNode();
		
		Query query = QueryFactory.create(queryString) ;
		try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
			
			for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
				QuerySolution s = res.next();
				if (s.get(var) != null) {
					array.add(makeMainStructure(s.get(var).asResource().getURI(), model));
				}
			}
		}
		
		if (array.size() > 0) {
			return array;
		} else {
			return null;
		}
	}
	protected ObjectNode makeMainStructure(String urit, Model model) {
		Literal prefLabel = new Literal();
		MultiLiteral altLabel = new MultiLiteral();
		
		String uri = "<" + urit + ">";
		
//		System.out.println(">> " + urit);
		
		String queryString;
		Query query;
		
		queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> SELECT ?literal WHERE {" + uri + " rdfs:label ?literal}";
		query = QueryFactory.create(queryString) ;
		
		try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
			for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
				QuerySolution s = res.next();
				org.apache.jena.rdf.model.Literal lit = s.get("literal").asLiteral();
				String lang = lit.getLanguage();
				Language ll = null;
				if (lang != null) {
					ll = Language.getLanguageByCode(lang);
					if (ll == null) {
						ll = Language.UNKNOWN;
					}
				} else {
					ll = Language.UNKNOWN;
				}
				
				if (prefLabel.getLiteral(ll) == null) {
					prefLabel.addLiteral(ll, JSONObject.escape(lit.getString()));
				} else {
					altLabel.addLiteral(ll, JSONObject.escape(lit.getString()));
				}
				
			}
		}

		ObjectNode json = Json.newObject();
		
		String type = null;

		queryString = "SELECT ?type WHERE {" + uri + " a ?type}";
		query = QueryFactory.create(queryString) ;
		
		try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
			for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
				QuerySolution s = res.next();
				type = s.get("type").asResource().getURI();
				break;
			}
		}

		String sid = null;

		queryString = "PREFIX wn20schema: <http://www.w3.org/2006/03/wn/wn20/schema/> SELECT ?sid WHERE {" + uri + " wn20schema:synsetId ?sid}";
		query = QueryFactory.create(queryString) ;
		
		try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
			for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
				QuerySolution s = res.next();
				sid = s.get("sid").asLiteral().getInt() + "";
				break;
			}
		}

		if (type.endsWith("NounSynset")) {
			sid = sid.substring(1) + "-n";
		} else if (type.endsWith("AdjectiveSynset")) {
			sid = sid.substring(1) + "-a";
		} else if (type.endsWith("AdjectiveSatelliteSynset")) {
			sid = sid.substring(1) + "-s";
		} else if (type.endsWith("VerbSynset")) {
			sid = sid.substring(1) + "-v";
		} else if (type.endsWith("AdverbSynset")) {
			sid = sid.substring(1) + "-r";
		}

		json.put("uri", "http://wordnet-rdf.princeton.edu/wn30/" + sid);
		
		json.put("type", "http://www.w3.org/2004/02/skos/core#Concept");
		
		if (prefLabel.size() > 0) {
			json.put("prefLabel", Json.toJson(prefLabel));
			
		}
		if (altLabel.size() > 0) {
			json.put("altLabel", Json.toJson(altLabel));
		}
		


		return json;
	}
}
