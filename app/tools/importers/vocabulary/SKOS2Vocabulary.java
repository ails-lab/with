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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.basicDataTypes.MultiLiteral;
import net.minidev.json.JSONObject;

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

public class SKOS2Vocabulary extends Data2Vocabulary<SKOSImportConfiguration> {

	public static SKOSImportConfiguration fashion = new SKOSImportConfiguration("fashion", 
	       	"Fashion Thesaurus", 
	        "fashion", 
	        "2014-12-10", 
	        "thesaurus.europeanafashion.eu",
	        null, 
	        new String[] { "http://thesaurus.europeanafashion.eu/thesaurus/Techniques", 
	                       "http://thesaurus.europeanafashion.eu/thesaurus/Colours",
	                       "http://thesaurus.europeanafashion.eu/thesaurus/Type",
	        	           "http://thesaurus.europeanafashion.eu/thesaurus/Materials" },
	        null);
	
	public static SKOSImportConfiguration gemet = new SKOSImportConfiguration("gemet", 
	        "GEMET Thesaurus", 
	        "gemet", 
	        "3.1", 
	        "www.eionet.europa.eu",
            null, 
            new String[] {},
			null);
	
	public static SKOSImportConfiguration euscreenxl = new SKOSImportConfiguration("euscreenxl", 
	        "EUScreenXL Thesaurus", 
	        "euscreenxl", 
	        "v1", 
	        "thesaurus.euscreen.eu",
            null, 
            new String[] {},
            null);	
	
	public static SKOSImportConfiguration photo = new SKOSImportConfiguration("photo", 
	        "Photography Thesaurus", 
	        "photo", 
	        "0", 
	        "bib.arts.kuleuven.be",
            null, 
	        null,
	        null);	
	
	public static SKOSImportConfiguration partageplus = new SKOSImportConfiguration("partageplus", 
	        "Partage Plus Thesaurus", 
	        "partageplus", 
	        "0", 
	        "partage.vocnet.org",
            null, 
	        null,
	        null);
	
	public static SKOSImportConfiguration mimo = new SKOSImportConfiguration("mimo", 
	        "MIMO Thesaurus", 
	        "mimo", 
	        "0", 
	        "www.mimo-db.eu",
            null, 
	        null,
	        null);	

	public static SKOSImportConfiguration[] confs = new SKOSImportConfiguration[] { fashion, gemet, euscreenxl, photo, partageplus, mimo };
	
	public static void doImport(SKOSImportConfiguration... confs) {
		SKOS2Vocabulary s2v = new SKOS2Vocabulary();
		for (SKOSImportConfiguration c : confs) {
			try {
				s2v.doImport(c);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	protected SKOSImportConfiguration conf;

	
	protected void doImport(SKOSImportConfiguration confx) throws Exception {
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
			
			Map<String, ArrayNode> schemes = new HashMap<>();
			Map<String, Set<String>> schemeTopConcepts = new HashMap<>();

			Set<String> manualSchemes = conf.getManualSchemes();
			if (manualSchemes == null) {
				queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?subject  WHERE {?subject a skos:ConceptScheme}" ;
				query = QueryFactory.create(queryString) ; 
				
				try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
					for (ResultSet results = qexec.execSelect(); results.hasNext() ; ) {
						QuerySolution qs = results.next();
						
						String uri = qs.get("subject").asResource().getURI();
						String turi = "<" + uri + ">";
						
						Set<String> withBroader = new HashSet<>();
						
						queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?z WHERE {?z skos:inScheme " + turi + " . ?z skos:broader ?q . ?q skos:inScheme " + turi + " .}" ;
						query = QueryFactory.create(queryString) ; 
						try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
							for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
								QuerySolution q = res.next();
								withBroader.add(q.get("z").asResource().getURI());
							}
						}
						
						Set<String> keep = new HashSet<>();
						
						queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?uri WHERE {?uri skos:inScheme " + turi + "}" ;
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

						queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?uri WHERE {?uri skos:topConceptOf " + turi + "}" ;
						query = QueryFactory.create(queryString) ; 
						try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
							for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
								QuerySolution q = res.next();
								
								keep.add(q.get("uri").asResource().getURI());
							}
						}
						
						ArrayNode array = Json.newObject().arrayNode();
						
						Set<String> concepts = new HashSet<>();
						for (String muri : keep) {
							array.add(makeMainStructure(muri, model));
							concepts.add(muri);
						}
						
						if (array.size() > 0) {
							schemes.put(uri, array);
							schemeTopConcepts.put(uri, concepts);
						}

					}
				}
			} else {
				for (String scheme : manualSchemes) {
					String uri = scheme;
					String turi = "<" + uri + ">";
					
					Set<String> withBroader = new HashSet<>();
					
					queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?z WHERE {?z skos:inScheme " + turi + " . ?z skos:broader ?q . ?q skos:inScheme " + turi + " .}" ;
					query = QueryFactory.create(queryString) ; 
					try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
						for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
							QuerySolution q = res.next();
							withBroader.add(q.get("z").asResource().getURI());
						}
					}
					
					Set<String> keep = new HashSet<>();
					
					queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?uri WHERE {?uri skos:inScheme " + turi + "}" ;
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

					queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?uri WHERE {?uri skos:topConceptOf " + turi + "}" ;
					query = QueryFactory.create(queryString) ; 
					try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
						for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
							QuerySolution q = res.next();
							
							keep.add(q.get("uri").asResource().getURI());
						}
					}
					
					ArrayNode array = Json.newObject().arrayNode();
					
					Set<String> concepts = new HashSet<>();
					for (String muri : keep) {
						array.add(makeMainStructure(muri, model));
						concepts.add(muri);
					}
					
					if (array.size() > 0) {
						schemes.put(uri, array);
						schemeTopConcepts.put(uri, concepts);
					}
				}
			}
			
			queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?subject WHERE {" +
					   "{SELECT ?subject WHERE {?subject a skos:Concept }} UNION " +
					   "{SELECT ?subject WHERE {?subject a skos:ConceptScheme }}}";
			query = QueryFactory.create(queryString) ; 
	
			try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
				for (ResultSet results = qexec.execSelect(); results.hasNext() ; ) {
					QuerySolution qs = results.next();
					
					String xuri = qs.get("subject").asResource().getURI();
					
					ObjectNode main = makeMainStructure(xuri, model, schemes.containsKey(xuri) ? "http://www.w3.org/2004/02/skos/core#ConceptScheme" : null);
					
					if (manualSchemes != null && main.get("type").asText().equals("http://www.w3.org/2004/02/skos/core#ConceptScheme") && !manualSchemes.contains(xuri)) {
						continue;
					}
					
					String uri = "<" + xuri + ">";
					
					Set<String> sc = null;
							
					JsonNode scopeNote = makeLiteralNode("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?literal WHERE {" + uri + " skos:scopeNote ?literal}", model, "literal");
					if (scopeNote != null) {
						main.put("scopeNote", scopeNote);
					}
					
					if (schemes.containsKey(xuri)) {
						ArrayNode array = schemes.get(xuri);
						
						if (array.size() > 0) {
							main.put("topConcepts", schemes.get(xuri));
						}
						
					} else {
						Set<String> b = getNodesArray("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?q WHERE {" + uri + " skos:broader ?q }", model, "q");
						b.removeAll(schemes.keySet());
							
						ArrayNode broader = makeNodesArray(b, model);
						if (broader != null) {
							main.put("broader", broader);
						}
					
						Set<String> bt = getNodesArray("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?q WHERE {" + uri + " skos:broader+ ?q }", model, "q");
							
//						sc = SetUtils.intersection(schemes.keySet(), bt);
						for (Map.Entry<String, Set<String>> entry : schemeTopConcepts.entrySet()) {
							if (SetUtils.intersection(entry.getValue(), bt).size() > 0 || entry.getValue().contains(xuri)) {
								if (sc == null) {
									sc = new HashSet<>();
								}
								
								sc.add(entry.getKey());
							}
						}
							
						bt.removeAll(schemes.keySet());
						
						ArrayNode broaderTransitive = makeNodesArray(bt, model);
						if (broaderTransitive != null) {
							main.put("broaderTransitive", broaderTransitive);
						}
					
						ArrayNode narrower = makeNodesArray("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?q WHERE {?q skos:broader " + uri + "}", model, "q");
						if (narrower != null) {
							main.put("narrower", narrower);
						}
					}
					
//					ArrayNode inCollections = makeURIArrayNode("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?uri WHERE {?uri skos:member " + uri + "}", model, "uri");
//					if (inCollections != null) {
//						main.put("inCollections", inCollections);
//					}
	
					ArrayNode related = makeNodesArray("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT DISTINCT ?q WHERE {{SELECT ?q WHERE {" + uri + " skos:related ?q}} UNION {SELECT ?q WHERE {?q skos:related " + uri + "}}}", model, "q");
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
					
					if (sc != null && sc.size() > 0) {
						ArrayNode array = Json.newObject().arrayNode();
						for (String ss : sc) {
							array.add(ss);
						}
						
						main.put("inSchemes", array);
					}
					
					main.put("vocabulary", vocabulary);

					ObjectNode json = Json.newObject();
					json.put("semantic", main);

					br.write(json.toString());
					br.write(VocabularyImportConfiguration.newLine);

				}
			}
			
//			queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?subject  WHERE {?subject a <http://www.w3.org/2004/02/skos/core#Collection>}" ;
//			query = QueryFactory.create(queryString) ; 
//			
//	
//			try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
//				for (ResultSet results = qexec.execSelect(); results.hasNext() ; ) {
//					QuerySolution qs = results.next();
//					
//					String uri = qs.get("subject").asResource().getURI();
//					
//					ObjectNode main = makeMainStructure(uri, model);
//					
//					uri = "<" + uri + ">";
//							
//					ArrayNode members = makeNodesArray("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?uri WHERE {" + uri + " skos:member ?uri}", model, "uri");
//					if (members != null) {
//						main.put("members", members);
//					}
//	
//					main.put("vocabulary", vocabulary);
//					
//					ObjectNode json = Json.newObject();
//					json.put("semantic", main);
//
//					br.write(json.toString());
//					br.write(VocabularyImportConfiguration.newLine);
//				}
//			}
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
	
	protected ArrayNode makeURIArrayNode(String queryString, Model model, String var) {
		ArrayNode array = Json.newObject().arrayNode();
		
		Query query = QueryFactory.create(queryString) ;
		try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
			
			for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
				QuerySolution s = res.next();
				String uri = s.get(var).asResource().getURI();
				array.add(uri);
			}
		}
		
		if (array.size() > 0) {
			return array;
		} else {
			return null;
		}
	}
	
	protected ArrayNode makeFilteredURIArrayNode(String queryString, Model model, String var, Set<String> keepOnly) {
		ArrayNode array = Json.newObject().arrayNode();
		
		Query query = QueryFactory.create(queryString) ;
		try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
			
			for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
				QuerySolution s = res.next();
				String uri = s.get(var).asResource().getURI();
				if (keepOnly == null || keepOnly.contains(uri)) {
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
	
	protected Set<String> getNodesArray(String queryString, Model model, String var) {
		Set<String> ret = new HashSet<>();
		Query query = QueryFactory.create(queryString) ;
		try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
			
			for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
				QuerySolution s = res.next();
				if (s.get(var) != null) {
					ret.add(s.get(var).asResource().getURI());
				}
			}
		}
		
		return ret;
	}
	
	protected ArrayNode makeNodesArray(Set<String> res, Model model) {
		if (res.size() > 0) {
			ArrayNode array = Json.newObject().arrayNode();
		
			for (String s : res) {
				array.add(makeMainStructure(s, model));
			}
			return array;
		} else {
			return null;
		}
	}
	
	protected ArrayNode makeNodesArray(String queryString, Model model, String var) {
		return makeNodesArray(getNodesArray(queryString, model, var), model);
	}

	protected ObjectNode makeMainStructure(String urit, Model model) {
		return makeMainStructure(urit, model, null);
	}
	
	protected ObjectNode makeMainStructure(String urit, Model model, String manualType) {
		Literal prefLabel = new Literal();
		MultiLiteral altLabel = new MultiLiteral();
		
		String uri = "<" + urit + ">";
		
//		System.out.println(">> " + urit);
		
		String queryString;
		Query query;
		
		queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?literal WHERE {" + uri + " skos:prefLabel ?literal}";
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
		
		queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?literal WHERE {" + uri + " skos:altLabel ?literal}";
		query = QueryFactory.create(queryString) ;
		
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
					ll = Language.getLanguageByCode(lang);
				}
				
				if ((ll == null || ll == Language.UNKNOWN) && conf.defaultLanguage != null) {
					ll = conf.defaultLanguage;
				}

				
				if (prefLabel.getLiteral(ll) == null) {
					prefLabel.addLiteral(ll, JSONObject.escape(lit.getString()));
				} else {
					altLabel.addLiteral(ll, JSONObject.escape(lit.getString()));
				}
				
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

}
