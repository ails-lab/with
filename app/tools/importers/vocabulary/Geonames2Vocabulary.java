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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.basicDataTypes.Language;
import model.basicDataTypes.Literal;
import model.basicDataTypes.MultiLiteral;
import net.minidev.json.JSONObject;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.commons.compress.compressors.CompressorException;

import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class Geonames2Vocabulary {

//	private String ontologyFile = VocabularyImportConfiguration.srcdir + File.separator + "dbo";
	private String inPath = VocabularyImportConfiguration.srcdir + File.separator + "gnr";

	private File tmpFolder1;
	private File tmpFolder2;
	
	private IndexWriter writer;
	
	private static Pattern labelPattern = Pattern.compile("^(.*?)@(.*)$");
		
	private static SKOSImportConfiguration gno = new SKOSImportConfiguration("gno", 
	        "Geonames Ontology", 
	        "gno", 
	        "3.1", 
	        "",
            null, 
            null,
			null);
	
	private static OWLImportConfiguration gnr = new OWLImportConfiguration("gnr", 
	        "Geonames Resources", 
	        "gnr", 
	        "2016-12-15", 
	        "http://www.w3.org/2000/01/rdf-schema#label",
	        "",
	        null,
	        null,
	        null);	
	
	
	public static void doImport() {
		
		Geonames2Vocabulary importer = new Geonames2Vocabulary();
		try {
			importer.readOntology(gno);

			importer.prepareIndex();
			importer.readInstances();
			
			importer.createThesaurus(gnr);
			
			importer.erase();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public Geonames2Vocabulary() {

		tmpFolder1 = VocabularyImportConfiguration.getTempFolder();
		tmpFolder2 = VocabularyImportConfiguration.getTempFolder();

	}
	
	SKOSImportConfiguration conf;
	
	Map<String, ObjectNode> classMap;
	
	private void readOntology(SKOSImportConfiguration confx) throws FileNotFoundException, IOException {
		this.conf = confx;

		File tmpFolder = VocabularyImportConfiguration.getTempFolder();
		
		Model model = conf.readModel(tmpFolder, "ontology_v3.1.rdf");

		classMap = new HashMap<>();
		
		File outFile = new File(tmpFolder + File.separator + conf.folder + ".txt");

		ObjectNode vocabulary = Json.newObject();
		vocabulary.put("name", conf.prefix);
		vocabulary.put("version", conf.version);
		
		System.out.println("Creating vocabulary in " + outFile);
		try (FileWriter fr = new FileWriter(outFile); 
		      BufferedWriter br = new BufferedWriter(fr)) {
	
			String queryString;
			org.apache.jena.query.Query query;
			
			Map<String, ArrayNode> schemes = new HashMap<>();
			Map<String, Set<String>> schemeTopConcepts = new HashMap<>();

			Set<String> manualSchemes = conf.getManualSchemes();
			if (manualSchemes == null) {
				queryString = "PREFIX gn: <http://www.geonames.org/ontology#> SELECT ?subject  WHERE {?subject a gn:Class}" ;
				query = QueryFactory.create(queryString) ; 
				
				try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
					for (ResultSet results = qexec.execSelect(); results.hasNext() ; ) {
						QuerySolution qs = results.next();
						
						String uri = qs.get("subject").asResource().getURI();
						String turi = "<" + uri + ">";
						
						Set<String> keep = new HashSet<>();

						queryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?uri WHERE {?uri skos:inScheme " + turi + "}" ;
						query = QueryFactory.create(queryString) ; 
						try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
							for (ResultSet res = exec.execSelect(); res.hasNext() ; ) {
								QuerySolution q = res.next();
								
								String curi = q.get("uri").asResource().getURI();
								keep.add(curi);
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
			}
			
			queryString = "PREFIX gn: <http://www.geonames.org/ontology#> SELECT ?subject WHERE {" +
					   "{SELECT ?subject WHERE {?subject a gn:Class}} UNION " +
					   "{SELECT ?subject WHERE {?subject a gn:Code}}}";
			query = QueryFactory.create(queryString) ; 
	
			try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
				for (ResultSet results = qexec.execSelect(); results.hasNext() ; ) {
					QuerySolution qs = results.next();
					
					String xuri = qs.get("subject").asResource().getURI();
//					System.out.println(xuri);
					
					ObjectNode main = makeMainStructure(xuri, model, schemes.containsKey(xuri) ? "http://www.w3.org/2004/02/skos/core#ConceptScheme" : "http://www.w3.org/2004/02/skos/core#Concept");
					if (!schemes.containsKey(xuri)) {
						classMap.put(xuri, makeMainStructure(xuri, model, "http://www.w3.org/2004/02/skos/core#Concept"));
					}

					String uri = "<" + xuri + ">";
					
					Set<String> sc = null;
							
					JsonNode scopeNote = makeLiteralNode("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?literal WHERE {" + uri + " skos:definition ?literal}", model, "literal");
					if (scopeNote != null) {
						main.put("scopeNote", scopeNote);
					}
					
					if (schemes.containsKey(xuri)) {
						ArrayNode array = schemes.get(xuri);
						
						if (array.size() > 0) {
							main.put("topConcepts", schemes.get(xuri));
						}
						
					} else {
						Set<String> inSchemes = getNodesArray("PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT ?uri WHERE {" + uri + " skos:inScheme ?uri}", model, "uri");
						if (inSchemes != null) {
							sc = new HashSet<>();
							sc.addAll(inSchemes);
						}
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

//					System.out.println(main);

				}
			}
				
			ObjectNode main = makeMainStructure("http://www.geonames.org/ontology#parentCountry", model, "http://www.w3.org/2002/07/owl#ObjectProperty");
			main.put("vocabulary", vocabulary);

			ObjectNode json = Json.newObject();
			json.put("semantic", main);
			
			br.write(json.toString());
			br.write(VocabularyImportConfiguration.newLine);

		}
		
		conf.cleanUp(tmpFolder);
	}
		
	protected JsonNode makeLiteralNode(String queryString, Model model, String var) {
		Literal literal = new Literal();
		org.apache.jena.query.Query query = QueryFactory.create(queryString) ;
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
		
		org.apache.jena.query.Query query = QueryFactory.create(queryString) ;
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
		
		org.apache.jena.query.Query query = QueryFactory.create(queryString) ;
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
		org.apache.jena.query.Query query = QueryFactory.create(queryString) ;
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
		
		String queryString;
		org.apache.jena.query.Query query;
		
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
		
		if (prefLabel.size() == 0) {
			queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> SELECT ?literal WHERE {" + uri + " rdfs:comment ?literal}";
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

	
	private void prepareIndex() throws IOException {
		System.out.println("Creating index");
		
		OpenMode om = OpenMode.CREATE;
	  
		Directory dir = FSDirectory.open(tmpFolder2.toPath());
		IndexWriterConfig iwc = new IndexWriterConfig(new KeywordAnalyzer());
		iwc.setOpenMode(om);
		
		writer = new IndexWriter(dir, iwc);

	}
	
	private void erase() throws IOException {
		System.out.println("Clearing folder " + tmpFolder1);
		for (File f : tmpFolder1.listFiles()) {
			f.delete();
		}
		System.out.println("Clearing folder " + tmpFolder2);
		for (File f : tmpFolder2.listFiles()) {
			f.delete();
		}
		
		tmpFolder1.delete();
		tmpFolder2.delete();
	}

	private Map<String, String> countryMap;
	private Map<String, ObjectNode> countryJsonMap;
	
	
	private void readInstances() throws IOException, CompressorException {
		System.out.println("Adding instances");
		
		countryMap = new HashMap<>();
		countryJsonMap = new HashMap<>();
		
		for (File f : new File(inPath).listFiles()) {
			if (f.getName().endsWith(".zip")) {
				VocabularyImportConfiguration.uncompress(tmpFolder1, f);
			} else {
				File tf = new File(tmpFolder1 + File.separator + f.getName());
				System.out.println("Copying " + f + " " + tf);
				Files.copy(f.toPath(), tf.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		}
		
		Set<String> keepLanguages = new HashSet<>();
		for (Language l : Language.EuropeanLanguages) {
			keepLanguages.add(l.getDefaultCode());
		}
					
		try {
					
			for (File f : tmpFolder1.listFiles()) {
				if (f.getName().endsWith("txt") && !f.getName().startsWith("alternateNames") && !f.getName().startsWith("readme") && !f.getName().startsWith("iso-languagecodes") && !f.getName().startsWith("countryInfo")) {
					System.out.println("Importing file " + f);
					
					int[] ind = {1,6,7,8};
					String[] types = {"name", "featureclass", "featurecode", "countrycode"}; 

					try (
			         BufferedReader br = new BufferedReader(new FileReader(f))) {
						String line;
				
						int count = 0;
						while ((line = br.readLine()) != null)   {
							String[] tokens = line.split("\t");
							
							String geonameid = tokens[0];
							
							for (int i = 0; i < ind.length; i++) {
								String id = "ID_" + System.currentTimeMillis() + "_" + Math.floor(100000*Math.random());
							
								Document doc = new Document();
								
								doc.add(new StringField("id", id, Field.Store.YES));
								doc.add(new StringField("geonameid", geonameid, Field.Store.YES));
								doc.add(new StringField("type", types[i], Field.Store.YES));
								doc.add(new StringField("value", tokens[ind[i]], Field.Store.YES));

//								System.out.println(id + " " + geonameid + " " + types[i] + " " + tokens[ind[i]]);
								writer.addDocument(doc);
								count++;
							}
				
						}
						System.out.println(count + " elements added from " + f.getName());
					}
				} else if (f.getName().startsWith("alternateNames")) {
					System.out.println("Importing file " + f);
					try (
						BufferedReader br = new BufferedReader(new FileReader(f))) {
						String line;
				
						int count = 0;
						while ((line = br.readLine()) != null)   {
							String[] tokens = line.split("\t");
							
							String id = "ID_" + System.currentTimeMillis() + "_" + Math.floor(100000*Math.random());
							String geonameid = tokens[1];
							String lang = tokens[2];
							
							if (!keepLanguages.contains(lang)) {
								continue;
							}
							
							String name = tokens[3];
							String prefname = "";
							if (tokens.length > 4) {
								prefname = tokens[4];
							}
							
							Document doc = new Document();
									
							doc.add(new StringField("id", id, Field.Store.YES));
							doc.add(new StringField("geonameid", geonameid, Field.Store.YES));
							String type = "altName";
							if (prefname.equals("1")) {
								type = "officialName";
							} 
							doc.add(new StringField("type", type, Field.Store.YES));
							doc.add(new StringField("value", name + "@" + lang, Field.Store.YES));
							
							writer.addDocument(doc);
							count++;
						}
						System.out.println(count + " elements added from " + f.getName());
					}
				} else if (f.getName().startsWith("countryInfo")) {
					System.out.println("Importing file " + f);
					try (BufferedReader br = new BufferedReader(new FileReader(f))) {
						String line;
				
						while ((line = br.readLine()) != null) {
							if (line.startsWith("#")) {
								continue;
							}
							
							String[] tokens = line.split("\t");
								
							String countryCode = tokens[0];
							String geonameId = tokens[16];
								
							countryMap.put(countryCode, geonameId);
						}
					}
				}
			}
		} finally {		
			writer.close();
		}
		
	}
	
	private void createThesaurus(OWLImportConfiguration conf) throws IOException, CompressorException {
		ObjectNode vocabulary = Json.newObject();
		vocabulary.put("name", conf.prefix);
		vocabulary.put("version", conf.version);

		File gnrFile = new File(tmpFolder1 + File.separator + conf.folder + ".txt");
		
		try (Directory dir = FSDirectory.open(tmpFolder2.toPath());
			  DirectoryReader reader = DirectoryReader.open(dir);
				BufferedWriter obr = new BufferedWriter(new FileWriter(gnrFile))) {
		
			IndexSearcher searcher = new IndexSearcher(reader);
			
			for (Map.Entry<String, String> cc : countryMap.entrySet()) {
				Query query = new TermQuery(new Term("geonameid", cc.getValue()));
				TopDocs results = searcher.search(query, 1000);
				
				ScoreDoc[] hits = results.scoreDocs;
					
				List<String> prefLabels = new ArrayList<>();
//				List<String> altLabels = new ArrayList<>();
					
				for (ScoreDoc hit : hits) {
					Document doc = searcher.doc(hit.doc);
						
					String t = doc.getValues("type")[0];
					String v = doc.getValues("value")[0];

					if (t.equals("name")) {
						prefLabels.add(v + "@en");
					} else if (t.equals("officialName")) {
						prefLabels.add(v);
					} else if (t.equals("altName")) {
//						altLabels.add(v);
					} 
				}
				
//				altLabels.removeAll(prefLabels);
				
				ObjectNode semantic = Json.newObject();
				
				semantic.put("uri", "http://sws.geonames.org/" + cc.getValue());
				semantic.put("type", "http://with.image.ntua.gr/model/Instance");
				
				Literal prefLabel = new Literal();
				for (String label : prefLabels) {
					Matcher lm = labelPattern.matcher(label);
					if (lm.find()) {
						Language lang = Language.getLanguageByCode(lm.group(2));
						if (lang != null) {
							prefLabel.addLiteral(lang, JSONObject.escape(lm.group(1)));
						}
					}
				}
				
				if (prefLabel.size() > 0) {
					semantic.put("prefLabel", Json.toJson(prefLabel));
				}
				
//				MultiLiteral altLabel = new MultiLiteral();
//				for (String label : altLabels) {
//					Matcher lm = labelPattern.matcher(label);
//					if (lm.find()) {
//						Language lang = Language.getLanguageByCode(lm.group(2));
//						if (lang != null) {
//							altLabel.addLiteral(lang, JSONObject.escape(lm.group(1)));
//						}
//					}
//				}
//				
//				if (altLabel.size() > 0) {
//					semantic.put("altLabel", Json.toJson(altLabel));
//				}
				
				countryJsonMap.put(cc.getKey(), semantic);
			}
		
			System.out.println("Creating thesaurus files");
			for (File f : tmpFolder1.listFiles()) {
				if (f.getName().endsWith("txt") && !f.getName().startsWith("alternateNames") && !f.getName().startsWith("readme") && !f.getName().startsWith("iso-languagecodes") && !f.getName().startsWith("countryInfo") && !f.getName().startsWith(conf.folder)) {
					System.out.println("Reading file " + f);
				
					try (BufferedReader br = new BufferedReader(new FileReader(f))) {

						String line;
						while ((line = br.readLine()) != null)   {
							String geonameid = line.split("\t")[0];
							
							Query query = new TermQuery(new Term("geonameid", geonameid));
							TopDocs results = searcher.search(query, 1000);
								
							ScoreDoc[] hits = results.scoreDocs;
								

							List<String> prefLabels = new ArrayList<>();
							List<String> altLabels = new ArrayList<>();
							ObjectNode fc = null;
							String featureclass = null;
							String featurecode = null;
							String countrycode = null;
								
							for (ScoreDoc hit : hits) {
								Document doc = searcher.doc(hit.doc);
									
								String t = doc.getValues("type")[0];
								String v = doc.getValues("value")[0];
								
								if (t.equals("name")) {
									prefLabels.add(v + "@en");
								} else if (t.equals("officialName")) {
									prefLabels.add(v);
								} else if (t.equals("altName")) {
									altLabels.add(v);
								} else if (t.equals("countrycode")) {
									countrycode = v;
								} else if (t.equals("featurecode")) {
									featurecode = v;
								} else if (t.equals("featureclass")) {
									featureclass = v;
								} 
							}
							
							if (featureclass != null && featurecode != null) {
								fc = classMap.get("http://www.geonames.org/ontology#" + featureclass + "." + featurecode);
							}
							
							altLabels.removeAll(prefLabels);
							
							ObjectNode json = Json.newObject();
							ObjectNode semantic = Json.newObject();
							json.put("semantic", semantic);
							
							semantic.put("uri", "http://sws.geonames.org/" + geonameid);
							semantic.put("type", "http://with.image.ntua.gr/model/Instance");
							
							Literal prefLabel = new Literal();
							for (String label : prefLabels) {
								Matcher lm = labelPattern.matcher(label);
								if (lm.find()) {
									Language lang = Language.getLanguageByCode(lm.group(2));
									if (lang != null) {
										prefLabel.addLiteral(lang, JSONObject.escape(lm.group(1)));
									}
								}
							}
							
							if (prefLabel.size() > 0) {
								semantic.put("prefLabel", Json.toJson(prefLabel));
							}
							
							MultiLiteral altLabel = new MultiLiteral();
							for (String label : altLabels) {
								Matcher lm = labelPattern.matcher(label);
								if (lm.find()) {
									Language lang = Language.getLanguageByCode(lm.group(2));
									if (lang != null) {
										altLabel.addLiteral(lang, JSONObject.escape(lm.group(1)));
									}
								}
							}
							
							if (altLabel.size() > 0) {
								semantic.put("altLabel", Json.toJson(altLabel));
							}

							if (fc != null) {
								ArrayNode arr = Json.newObject().arrayNode();
								arr.add(fc);
								semantic.put("broader", arr);
								semantic.put("broaderTransitive", arr);
							}
							
							if (countrycode != null) {
								ObjectNode country = countryJsonMap.get(countrycode);
								if (country != null) {
									ArrayNode array = Json.newObject().arrayNode();
									array.add(country);
									ArrayNode props = Json.newObject().arrayNode();
									ObjectNode prop = Json.newObject();
									prop.put("name", "http://www.geonames.org/ontology#parentCountry");
									prop.put("values", array);
									props.add(prop);
								
									semantic.put("properties", props);
								}
							}
							
							semantic.put("vocabulary", vocabulary);
	
							obr.write(json.toString());
							obr.write(VocabularyImportConfiguration.newLine);
						}
					}
				}
			}
		}
		
		System.out.println("Compressing " + gnrFile);
		File cf = VocabularyImportConfiguration.compress(tmpFolder1, gnrFile.getName().substring(0, gnrFile.getName().lastIndexOf(".")));
		File tf = new File(VocabularyImportConfiguration.outdir + File.separator + cf.getName());
		System.out.println("Copying " + cf + " " + tf);
		Files.copy(cf.toPath(), tf.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}
}
