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
import java.util.HashSet;
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

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;


public class AAT2Vocabulary extends SKOS2Vocabulary{

	private static SKOSImportConfiguration aat = new SKOSImportConfiguration("aat", 
	        "The Art & Architecture Thesaurus ", 
	        "aat", 
	        "2016-10-7", 
	        "vocab.getty.edu",
            null, 
	        null);	

	public static SKOSImportConfiguration[] confs = new SKOSImportConfiguration[] { aat };
	
	public static void main(String[] args) {
		doImport(confs);
	}
	
	public static void doImport() {
		for (SKOSImportConfiguration c : confs) {
			try {
				doImport(c);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void doImport(SKOSImportConfiguration conf) throws OWLOntologyCreationException, IOException {

		Set<String> ks = null;
		if (conf.existingSchemesToKeep != null) {
			ks = new HashSet<>();
			for (String s : conf.existingSchemesToKeep) {
				ks.add(s);
			}
		}
		
		File tmpFolder = VocabularyImportConfiguration.getTempFolder();
		
		Model model = ModelFactory.createDefaultModel();
		for (File f : conf.getInputFolder().listFiles()) {
			System.out.println("Reading: " + f);
			if (f.getName().endsWith(".zip")) {
				VocabularyImportConfiguration.uncompress(tmpFolder, f);
			} else {
				System.out.println("Importing to Fuseki: " + f);
				model.read(f.getAbsolutePath());
			}
		}
		
		for (File f : tmpFolder.listFiles()) {
			System.out.println("Importing to Fuseki: " + f);
			model.read(f.getAbsolutePath());
		}

		File outFile = new File(tmpFolder + File.separator + conf.folder + ".txt");

		ObjectNode vocabulary = Json.newObject();
		vocabulary.put("name", conf.prefix);
		vocabulary.put("version", conf.version);
		
		System.out.println("Creating vocabulary in " + outFile);
		try (FileWriter fr = new FileWriter(outFile);
				BufferedWriter br = new BufferedWriter(fr)) {
	
			String queryString;
			Query query;
			
			queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?subject  WHERE {?subject a <http://www.w3.org/2004/02/skos/core#ConceptScheme>}" ;
			query = QueryFactory.create(queryString) ; 
			
	
			try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
				for (ResultSet results = qexec.execSelect(); results.hasNext() ; ) {
					QuerySolution qs = results.next();
					
					String uri = qs.get("subject").asResource().getURI();
					if (conf.existingSchemesToKeep != null && !ks.contains(uri)) {
						continue;
					}
					
					ObjectNode main = makeMainStructure(uri, model);
					
					uri = "<" + uri + ">";
					
					Set<String> withBroader = new HashSet<>();
					
					queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> PREFIX gvp: <http://vocab.getty.edu/ontology#> SELECT ?z ?q WHERE {?z skos:inScheme " + uri + " .?z gvp:broaderGeneric|gvp:broaderPartitive ?q .?q skos:inScheme " + uri + " .}" ;
					query = QueryFactory.create(queryString) ; 
					try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
						for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
							QuerySolution q = res.next();
							withBroader.add(q.get("z").asResource().getURI());
						}
					}
					
					Set<String> keep = new HashSet<>();
					
					queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?uri WHERE {?uri skos:inScheme " + uri + "}" ;
					query = QueryFactory.create(queryString) ; 
					try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
						for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
							QuerySolution q = res.next();
							
							String curi = q.get("uri").asResource().getURI();
							if (!withBroader.contains(curi)) {
								keep.add(curi);
							}
						}
					}

					queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?uri WHERE {?uri skos:topConceptOf " + uri + "}" ;
					query = QueryFactory.create(queryString) ; 
					try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
						for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
							QuerySolution q = res.next();
							
							keep.add(q.get("uri").asResource().getURI());
						}
					}
					
					ArrayNode array = Json.newObject().arrayNode();
					
					for (String muri : keep) {
						array.add(makeMainStructure(muri, model));
					}
					
					if (array.size() > 0) {
						main.put("topConcepts", array);
					}

					
					main.put("vocabulary", vocabulary);

					ObjectNode json = Json.newObject();
					json.put("semantic", main);

					br.write(json.toString());
					br.write(VocabularyImportConfiguration.newLine);
				}
			}
	
			queryString = "PREFIX gvp: <http://vocab.getty.edu/ontology#> SELECT ?subject  WHERE {?subject a gvp:Concept }";
			query = QueryFactory.create(queryString) ; 
	
			try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
				for (ResultSet results = qexec.execSelect(); results.hasNext() ; ) {
					QuerySolution qs = results.next();
					
					String uri = qs.get("subject").asResource().getURI();
					
					ObjectNode main = makeMainStructure(uri, model);
					
					uri = "<" + uri + ">";
							
					JsonNode scopeNote = makeLiteralNode("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> PREFIX gvp: <http://vocab.getty.edu/ontology#> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT ?literal WHERE {" + uri + " skos:scopeNote ?q . ?q rdf:value ?literal}", model, "literal");
					if (scopeNote != null) {
						main.put("scopeNote", scopeNote);
					}
					
					ArrayNode broader = makeNodesArray("PREFIX gvp: <http://vocab.getty.edu/ontology#> SELECT DISTINCT ?q WHERE {{SELECT ?q WHERE {" + uri + " gvp:broaderGeneric ?q }} UNION {SELECT ?q WHERE {" + uri + " gvp:broaderPartitive ?q }}}", model, "q");
					if (broader != null) {
						main.put("broader", broader);
					}
					
					ArrayNode narrower = makeNodesArray("PREFIX gvp: <http://vocab.getty.edu/ontology#> SELECT DISTINCT ?q WHERE {{SELECT ?q WHERE {?q gvp:broaderGeneric " + uri + "}} UNION {SELECT ?q WHERE {?q gvp:broaderPartitive " + uri + "}}}", model, "q");
					if (narrower != null) {
						main.put("narrower", narrower);
					}
					
					ArrayNode broaderTransitive = makeNodesArray("PREFIX gvp: <http://vocab.getty.edu/ontology#> SELECT DISTINCT ?q WHERE {{SELECT ?q WHERE {" + uri + " gvp:broaderGeneric+ ?q }} UNION {SELECT ?q WHERE {" + uri + " gvp:broaderPartitive+ ?q }}}", model, "q");
					if (broaderTransitive != null) {
						main.put("broaderTransitive", broaderTransitive);
					}
					
					ArrayNode inCollections = makeURIArrayNode("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?uri WHERE {?uri skos:member " + uri + "}", model, "uri");
					if (inCollections != null) {
						main.put("inCollections", inCollections);
					}
	
					ArrayNode related = makeNodesArray("PREFIX gvp: <http://vocab.getty.edu/ontology#> SELECT DISTINCT ?q WHERE {{SELECT ?q WHERE {" + uri + " gvp:aat2000_related_to ?q}} UNION {SELECT ?q WHERE {?q gvp:aat2000_related_to " + uri + "}}}", model, "q");
					if (related != null) {
						main.put("related", related);
					}
					
					ArrayNode exactMatch = makeURIArrayNode("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?uri WHERE {" + uri + " skos:exactMatch ?uri}", model, "uri");
					if (exactMatch != null) {
						main.put("exactMatch", exactMatch);
					}
					
					ArrayNode closeMatch = makeURIArrayNode("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?uri WHERE {" + uri + " skos:closeMatch ?uri}", model, "uri");
					if (closeMatch != null) {
						main.put("closeMatch", closeMatch);
					}
					
					ArrayNode inSchemes = makeFilteredURIArrayNode("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?uri WHERE {" + uri + " skos:inScheme  ?uri}", model, "uri", ks);
					if (inSchemes != null) {
						main.put("inSchemes", inSchemes);
					} else if (conf.mainScheme != null){ 
						ArrayNode schemes = Json.newObject().arrayNode();
						schemes.add(conf.mainScheme);
						main.put("inSchemes", schemes);
					}
					
//					ArrayNode topConceptOf = makeURIArrayNode("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?uri WHERE {" + uri + " skos:topConceptOf ?uri}", model, "uri");
//					if (topConceptOf != null) {
//						main.put("topConceptOf", topConceptOf);
//					}
	
					main.put("vocabulary", vocabulary);

					ObjectNode json = Json.newObject();
					json.put("semantic", main);

					br.write(json.toString());
					br.write(VocabularyImportConfiguration.newLine);
				}
			}

			String[] cols = new String[] {"<http://www.w3.org/2004/02/skos/core#OrderedCollection>", 
					                     "<http://vocab.getty.edu/ontology#Facet>",
					                     "<http://vocab.getty.edu/ontology#GuideTerm>",
					                     "<http://vocab.getty.edu/ontology#Hierarchy>" };
			
			Set<String> coluris = new HashSet<>();
			
			for (String col : cols) {
				queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT DISTINCT ?subject  WHERE {?subject a " + col + "}" ;
				query = QueryFactory.create(queryString) ; 

				try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
					for (ResultSet results = qexec.execSelect(); results.hasNext() ; ) {
						QuerySolution qs = results.next();
						
						String uri = qs.get("subject").asResource().getURI();
						
						coluris.add(uri);
					}
				}
			}
			
			for (String uri : coluris) {
				ObjectNode main = makeMainStructure(uri, model);
				
				uri = "<" + uri + ">";
						
				ArrayNode members = makeNodesArray("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> PREFIX gvp: <http://vocab.getty.edu/ontology#> SELECT ?uri WHERE {" + uri + " skos:member|gvp:broaderGeneric+|gvp:broaderPartitive+ " + uri + "}", model, "uri");
				if (members != null) {
					main.put("members", members);
				}

				main.put("vocabulary", vocabulary);
				
				ObjectNode json = Json.newObject();
				json.put("semantic", main);

				br.write(json.toString());
				br.write(VocabularyImportConfiguration.newLine);
			}
		}
		
		System.out.println("Compressing " + tmpFolder + File.separator + conf.folder + ".txt");
		File cf = VocabularyImportConfiguration.compress(tmpFolder, conf.folder);
		File tf = new File(VocabularyImportConfiguration.outPath + File.separator + cf.getName());
		System.out.println("Copying file " + cf + " to " + tf);
		Files.copy(cf.toPath(), tf.toPath(), StandardCopyOption.REPLACE_EXISTING);

		System.out.println("Clearing " + tmpFolder);
		for (File f : tmpFolder.listFiles()) {
			f.delete();
		}
		tmpFolder.delete();
	}

	
	protected static ObjectNode makeMainStructure(String urit, Model model) {
		Literal prefLabel = new Literal();
		MultiLiteral altLabel = new MultiLiteral();
		
		String uri = "<" + urit + ">";
		
//		System.out.println(">> " + urit);
		
		String queryString;
		Query query;
		
		queryString = "PREFIX xl:<http://www.w3.org/2008/05/skos-xl#> SELECT ?literal WHERE {" + uri + " xl:prefLabel ?label . ?label xl:literalForm ?literal}";
		query = QueryFactory.create(queryString);
		
		try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
			for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
				QuerySolution s = res.next();
				org.apache.jena.rdf.model.Literal lit = s.get("literal").asLiteral();
				String lang = lit.getLanguage();
				Language ll = null;
				if (lang != null) {
					ll = Language.getLanguage(lang);
					if (ll == null) {
						ll = Language.UNKNOWN;
					}
				} else {
					ll = Language.UNKNOWN;
				}
				
				prefLabel.addLiteral(ll, JSONObject.escape(lit.getString()));
			}
		}
		
		queryString = "PREFIX xl:<http://www.w3.org/2008/05/skos-xl#> SELECT ?literal WHERE {" + uri + " xl:altLabel ?label . ?label xl:literalForm ?literal}";
		query = QueryFactory.create(queryString) ;
		
		try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
			for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
				QuerySolution s = res.next();
				org.apache.jena.rdf.model.Literal lit = s.get("literal").asLiteral();
				String lang = lit.getLanguage();
				Language ll = null;
				if (lang != null) {
					ll = Language.getLanguage(lang);
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
		
		queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> SELECT ?literal WHERE {" + uri + " rdfs:label ?literal}";
		query = QueryFactory.create(queryString) ;
		
		try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
			for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
				QuerySolution s = res.next();
				org.apache.jena.rdf.model.Literal lit = s.get("literal").asLiteral();
				String lang = lit.getLanguage();
				Language ll = null;
				if (lang != null) {
					ll = Language.getLanguage(lang);
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

		String type = null;

		queryString = "SELECT ?type WHERE {" + uri + " a ?type}";
		
		query = QueryFactory.create(queryString) ;
		
		try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
			for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
				QuerySolution s = res.next();
				String t = s.get("type").asResource().getURI();
				if (t.equals("http://vocab.getty.edu/ontology#Concept")) {
					type = "http://www.w3.org/2004/02/skos/core#Concept";
					break;
				} else if (t.equals("http://www.w3.org/2004/02/skos/core#OrderedCollection") || 
						   t.equals("http://vocab.getty.edu/ontology#Facet") ||
						   t.equals("http://vocab.getty.edu/ontology#GuideTerm") || 
						   t.equals("http://vocab.getty.edu/ontology#Hierarchy")) {
					type = "http://www.w3.org/2004/02/skos/core#Collection";
					break;
				}
			}
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
}
