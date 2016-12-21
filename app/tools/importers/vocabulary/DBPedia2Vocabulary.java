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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.basicDataTypes.Language;
import net.minidev.json.JSONObject;

import org.apache.jena.atlas.lib.SetUtils;
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
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.lang3.StringEscapeUtils;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.model.OWLOntologyID;

import play.libs.Json;

import com.aliasi.util.Arrays;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;


public class DBPedia2Vocabulary {

	private String ontologyFile = VocabularyImportConfiguration.srcdir + File.separator + "dbo";
	private String inPath = VocabularyImportConfiguration.srcdir + File.separator + "dbr";

	private String labelsFileName = "labels_en.ttl.bz2";
	private File tmpFolder;
	
	private IndexWriter writer;
	private OWLReasoner reasoner; 
	
	private static Pattern triple = Pattern.compile("^<(.*?)> <(.*?)> <?(.*?)>? \\.$");
	private static Pattern labelPattern = Pattern.compile("^\"(.*?)\"@(.*)$");
		
	private Map<OWLClass, ObjectNode> classMap = new HashMap<>();
	
	private static OWLDataFactory df = new OWLManager().getOWLDataFactory();

	private Set<OWLClass>[] filter;
	private String[] filterName;
	
	private static OWLImportConfiguration dbo = new OWLImportConfiguration("dbo", 
	        "DBPedia Ontology", 
	        "dbo", 
	        "2016-04", 
	        "http://www.w3.org/2000/01/rdf-schema#label",
	        "dbpedia.org",
	        null,
	        null,
	        null);	
	
	private static OWLImportConfiguration dbr = new OWLImportConfiguration("dbr", 
	        "DBPedia Resources", 
	        "dbr", 
	        "2016-04", 
	        "http://www.w3.org/2000/01/rdf-schema#label",
	        "dbpedia.org",
	        null,
	        null,
	        null);	
	
	
	public static void doImport(List<String[]> filters) {
		
		DBPedia2Vocabulary importer = new DBPedia2Vocabulary(dbo, filters);
		try {
			importer.readOntology(dbo);

			importer.prepareIndex();
			importer.readInstances();
			
			importer.createThesaurus(dbr);
			
			importer.erase();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public DBPedia2Vocabulary(OWLImportConfiguration conf, List<String[]> restrictToType) {

		tmpFolder = VocabularyImportConfiguration.getTempFolder();

		if (restrictToType != null) {
			filter = new Set[restrictToType.size()];
			filterName = new String[filter.length];

			for (int i = 0; i < restrictToType.size(); i++) {
				filter[i] = new HashSet<>();
				filterName[i] = "";
				for (String s: restrictToType.get(i)) {
					filter[i].add(df.getOWLClass(IRI.create("http://dbpedia.org/ontology/" + s)));
					
					if (filterName[i].length() > 0) {
						filterName[i] += "-";
					}
					filterName[i] += s.toLowerCase();
				}
			}
		}
	}
	
	OWLOntology ontology;
	OWLAnnotationProperty ap;
	
	private Set<OWLClass> filter(Set<OWLClass> set, OWLImportConfiguration conf) {
		Set<OWLClass> res = new HashSet<>();
		
		for (OWLClass cz : set) {
			if (cz.equals(df.getOWLThing()) || cz.equals(df.getOWLNothing()) || !conf.keep(cz)) {
			} else {
				res.add(cz);
			}
		}
		
		return res;
	}
	
	private void readOntology(OWLImportConfiguration conf) throws OWLOntologyCreationException, FileNotFoundException, IOException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		ontology = manager.loadOntologyFromOntologyDocument(IRI.create(new File(ontologyFile).listFiles()[0].toURI().toString()));
		
		reasoner = new Reasoner.ReasonerFactory().createReasoner(ontology);
		
		ap = df.getOWLAnnotationProperty(IRI.create(conf.labelProperty));

		File outFile = new File(tmpFolder + File.separator + conf.folder + ".txt");
		
		try (FileWriter fr = new FileWriter(outFile);
            BufferedWriter br = new BufferedWriter(fr)) {

			ObjectNode vocabulary = Json.newObject();
			vocabulary.put("name", conf.prefix);
			vocabulary.put("version", conf.version);
			
			for (OWLClass cz : ontology.getClassesInSignature()) {
				if (!conf.keep(cz)) {
					continue;
				}
	
				classMap.put(cz, makeMainStructure(cz));
				
				ObjectNode main = makeMainStructure(cz);
				
				
				ArrayNode broader = makeNodesArray(filter(reasoner.getSuperClasses(cz, true).getFlattened(), conf));
				if (broader != null) {
					main.put("broader", broader);
				}
				
				ArrayNode broaderTransitive = makeNodesArray(filter(reasoner.getSuperClasses(cz, false).getFlattened(), conf));
				if (broaderTransitive != null) {
					main.put("broaderTransitive", broaderTransitive);
				}

				ArrayNode narrower = makeNodesArray(filter(reasoner.getSubClasses(cz, false).getFlattened(), conf));
				if (narrower != null) {
					main.put("narrower", narrower);
				}
				
				ArrayNode exactMatch = makeURIArrayNode(reasoner.getEquivalentClasses(cz).getEntities());
				if (exactMatch != null) {
					main.put("exactMatch", exactMatch);
				}
				
				main.put("vocabulary", vocabulary);
	
				ObjectNode json = Json.newObject();
				json.put("semantic", main);
	
				br.write(json.toString());
				br.write(VocabularyImportConfiguration.newLine);
			}
		}
		
		System.out.println("Compressing output");
		File cf = VocabularyImportConfiguration.compress(tmpFolder, conf.folder);
		File tf = new File(VocabularyImportConfiguration.outdir + File.separator + cf.getName());
		System.out.println("Copying output file");
		Files.copy(cf.toPath(), tf.toPath(), StandardCopyOption.REPLACE_EXISTING);

		System.out.println("Clearing tmp folder");
		for (File f : tmpFolder.listFiles()) {
			f.delete();
		}
		tmpFolder.delete();
	
	}
		
	protected ArrayNode makeNodesArray(Set<OWLClass> res) {
		if (res.size() > 0) {
			ArrayNode array = Json.newObject().arrayNode();
		
			for (OWLClass s : res) {
				array.add(makeMainStructure(s));
			}
			return array;
		} else {
			return null;
		}
	}
	
	protected ArrayNode makeURIArrayNode(Set<OWLClass> res) {
		if (res.size() > 0) {
			ArrayNode array = Json.newObject().arrayNode();
		
			for (OWLClass s : res) {
				array.add(s.getIRI().toString());
			}
			return array;
		} else {
			return null;
		}
	}
	
	protected ObjectNode makeMainStructure(OWLClass cz) {
		ObjectNode prefLabel = Json.newObject();
		for (OWLAnnotation ann : cz.getAnnotations(ontology, ap)) {
			String label = ann.getValue().toString();
			
			Matcher lm = labelPattern.matcher(label);
			if (lm.find()) {
				Language lang = Language.getLanguageByCode(lm.group(2));
				if (lang != null) {
					prefLabel.put(lang.getDefaultCode(), JSONObject.escape(lm.group(1)));
				}
			}
		}
		
		ObjectNode json = Json.newObject();
		json.put("uri", cz.getIRI().toString());
		
		String type = "http://www.w3.org/2004/02/skos/core#Concept";
		json.put("type", type);

		if (prefLabel.size() > 0) {
			json.put("prefLabel", Json.toJson(prefLabel));
		}
		
		return json;

	}
	
	private void prepareIndex() throws IOException {
		System.out.println("Creating index");
		
		OpenMode om = OpenMode.CREATE;
	  
		Directory dir = FSDirectory.open(tmpFolder.toPath());
		IndexWriterConfig iwc = new IndexWriterConfig(new KeywordAnalyzer());
		iwc.setOpenMode(om);
		
		writer = new IndexWriter(dir, iwc);

	}
	
	private void erase() throws IOException {
		System.out.println("Clearing folder " + tmpFolder);
		for (File f : tmpFolder.listFiles()) {
			f.delete();
		}
		tmpFolder.delete();
	}

	private void readInstances() throws IOException, CompressorException {
		System.out.println("Adding instances");
		
		try {
			for (File f : new File(inPath).listFiles()) {
				if (f.getName().endsWith("bz2")) {
					System.out.println("Importing file " + f);
					try (FileInputStream fin = new FileInputStream(f);
				         BufferedInputStream bis = new BufferedInputStream(fin);
				         CompressorInputStream input = new CompressorStreamFactory().createCompressorInputStream(bis);
				         BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
			
						String line;
				
						int count = 0;
						while ((line = br.readLine()) != null)   {
							Matcher m = triple.matcher(line);
							
							if (m.find()) {
								String id = "ID_" + System.currentTimeMillis() + "_" + Math.floor(100000*Math.random());
								String uri = m.group(1);
								String type = m.group(2);
								String value = StringEscapeUtils.unescapeJava(m.group(3));
								
								if (type.equals("http://www.w3.org/2002/07/owl#sameAs")) {
									if (!value.startsWith("http://dbpedia.org/")) {
										continue;
									} else {
										Document doc = new Document();
										
										doc.add(new StringField("id", id, Field.Store.YES));
										doc.add(new StringField("uri", value, Field.Store.YES));
										doc.add(new StringField("type", type, Field.Store.YES));
										doc.add(new StringField("value", uri, Field.Store.YES));
					
										writer.addDocument(doc);
										count++;
									}
								} else {
									Document doc = new Document();
									
									doc.add(new StringField("id", id, Field.Store.YES));
									doc.add(new StringField("uri", uri, Field.Store.YES));
									doc.add(new StringField("type", type, Field.Store.YES));
									doc.add(new StringField("value", value, Field.Store.YES));
				
									writer.addDocument(doc);
									count++;
								}
								
							}
						}
						System.out.println(count + " elements added from " + f.getName());
					}
				}
			}
		} finally {		
			writer.close();
		}
		
	}
	
	private File[] createThesaurus(OWLImportConfiguration conf) throws IOException, CompressorException {
		ObjectNode vocabulary = Json.newObject();
		vocabulary.put("name", conf.prefix);
		vocabulary.put("version", conf.version);

		File[] dbrFiles;
		
		try (Directory dir = FSDirectory.open(tmpFolder.toPath());
			  DirectoryReader reader = DirectoryReader.open(dir)) {
		
			IndexSearcher searcher = new IndexSearcher(reader);
		
			System.out.println("Creating thesaurus files");
			try (FileInputStream fin = new FileInputStream(inPath + File.separator + labelsFileName);
		         BufferedInputStream bis = new BufferedInputStream(fin);
		         CompressorInputStream input = new CompressorStreamFactory().createCompressorInputStream(bis);
		         BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
				
				if (filter == null) {
					dbrFiles = new File[] {new File(tmpFolder + File.separator + "dbr.txt")};
				} else {
					dbrFiles = new File[filterName.length];
					for (int i = 0; i < filterName.length; i++) {
						dbrFiles[i] = new File(tmpFolder + File.separator + "dbr-" + filterName[i] + ".txt");
						System.out.println(tmpFolder + File.separator + "dbr-" + filterName[i] + ".txt");
					}
				}
				
				BufferedWriter[] obr = new BufferedWriter[dbrFiles.length];
				for (int i = 0; i < obr.length; i++) {
					obr[i] = new BufferedWriter(new FileWriter(dbrFiles[i]));
				}

				try {
					String line;
					while ((line = br.readLine()) != null)   {
						Matcher m = triple.matcher(line);
						
						if (m.find()) {
							String uri = m.group(1);
		
							Query query = new TermQuery(new Term("uri", uri));
							TopDocs results = searcher.search(query, 1000);
							
							ScoreDoc[] hits = results.scoreDocs;
							
							String comment = null;
							List<String> sameAs = new ArrayList<>();
							List<String> labels = new ArrayList<>();
							Set<String> types = new HashSet<>();
							
							for (ScoreDoc hit : hits) {
								Document doc = searcher.doc(hit.doc);
								
								String t = doc.getValues("type")[0];
								String v = doc.getValues("value")[0];

								if (t.equals(dbr.labelProperty)) {
									labels.add(v);
								} else if (t.equals("http://www.w3.org/2000/01/rdf-schema#comment")) {
									comment = v;
								} else if (t.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") && v.contains("dbpedia")) {
									types.add(v);
								} else if (t.equals("http://www.w3.org/2002/07/owl#sameAs") && v.contains("dbpedia")) {
									sameAs.add(v);
								} 
							}
							
							ObjectNode json = Json.newObject();
							ObjectNode semantic = Json.newObject();
							json.put("semantic", semantic);
							
							semantic.put("uri", uri);
							semantic.put("type", "http://with.image.ntua.gr/model/Instance");
							
							ObjectNode prefLabel = Json.newObject();
							for (String label : labels) {
								Matcher lm = labelPattern.matcher(label);
								if (lm.find()) {
									Language lang = Language.getLanguageByCode(lm.group(2));
									if (lang != null) {
										prefLabel.put(lang.getDefaultCode(), JSONObject.escape(lm.group(1)));
									}
								}
							}
							
							if (prefLabel.size() > 0) {
								semantic.put("prefLabel", prefLabel);
							}
	
							Set<OWLClass> dsc = new HashSet<>();
							Set<OWLClass> asc = new HashSet<>();
							
							for (String type : types) {
								OWLClass cl = df.getOWLClass(IRI.create(type));
								
								for ( OWLClass c :reasoner.getSuperClasses(cl, true).getFlattened()) {
									if (c.getIRI().toString().contains("dbpedia")) {
										dsc.add(c);
									}
								}
								
								for ( OWLClass c :reasoner.getSuperClasses(cl, false).getFlattened()) {
									if (c.getIRI().toString().contains("dbpedia")) {
										asc.add(c);
									}
								}
							}
							
							if (dsc.size() > 0) {
								ArrayNode arr = Json.newObject().arrayNode();
							
								for (OWLClass sc : dsc) {
									arr.add(classMap.get(sc));
								}
								semantic.put("broader", arr);
							}
							
							Integer[] forFilter = new Integer[0];
	
							if (asc.size() > 0) {
								ArrayNode arr = Json.newObject().arrayNode();
							
								for (OWLClass sc : asc) {
									arr.add(classMap.get(sc));
									
									if (filter == null) {
										forFilter = new Integer[] {0};
									} else {
										List<Integer> tmp = new ArrayList<>();
										for (int i = 0; i < filter.length; i++) {
											if (filter[i].contains(sc)) {
												tmp.add(i);
											}
										}
										if (tmp.size() > 0) {
											forFilter = tmp.toArray(new Integer[] {});
										}
									}
								}
								semantic.put("broaderTransitive", arr);
							}
							
							ArrayNode same = Json.newObject().arrayNode();
							for (String sa : sameAs) {
								same.add(sa);
							}
							
							if (same.size() > 0) {
								semantic.put("exactMatch", same);
							}
							
							semantic.put("vocabulary", vocabulary);
	
							for (int f : forFilter) {
								obr[f].write(json.toString());
								obr[f].write(VocabularyImportConfiguration.newLine);
							}
						}
					}
				} finally {
					for (int i = 0; i < obr.length; i++) {
						obr[i].close();
					}
				}
			}
		}
		
		for (File s : dbrFiles) {
			System.out.println("Compressing " + s);
			File cf = VocabularyImportConfiguration.compress(tmpFolder, s.getName().substring(0, s.getName().lastIndexOf(".")));
			File tf = new File(VocabularyImportConfiguration.outdir + File.separator + cf.getName());
			System.out.println("Copying " + cf + " " + tf);
			Files.copy(cf.toPath(), tf.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}

		return dbrFiles;
	}
}
