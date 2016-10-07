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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
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

	private String ontologyFile = VocabularyImportConfiguration.inPath + File.separator + "dbo";
	private String inPath = VocabularyImportConfiguration.inPath + File.separator + "dbr";

	private String dboFile = VocabularyImportConfiguration.outPath + File.separator + "dbo.txt";

	private String labelsFileName = "labels_en.ttl.bz2";
	private File tmpIndex;
	
	private IndexWriter writer;
	private OWLReasoner reasoner; 
	
	private static Pattern triple = Pattern.compile("^<(.*?)> <(.*?)> <?(.*?)>? \\.$");
	private static Pattern labelPattern = Pattern.compile("^\"(.*?)\"@(.*)$");
		
	private Map<OWLClass, ObjectNode> labelMap = new HashMap<>();
	
	private static OWLDataFactory df = new OWLManager().getOWLDataFactory();

	private Set<OWLClass>[] filter;
	private String[] filterName;
	
	private static OWLImportConfiguration dbo = new OWLImportConfiguration("dbo", 
	        "DBPedia Ontology", 
	        "dbo", 
	        "2015-10", 
	        "http://www.w3.org/2000/01/rdf-schema#label",
	        "dbpedia.org",
	        "http://dbpedia.org/ontology",
	        null);	
	
	public static void main(String[] args) {
		List<String[]> filters = new ArrayList<>();
		filters.add(new String[] {"Person"});
		filters.add(new String[] {"Place"});

		doImport(filters);
	}
	
	public static void doImport(List<String[]> filters) {
		
		DBPedia2Vocabulary importer = new DBPedia2Vocabulary(dbo, filters);
		try {
			importer.readOntology(dbo);
			dbo.compress();
			dbo.deleteTemp();

			importer.prepareIndex();
			importer.readInstances();
			File[] dbrFiles = importer.createThesaurus();
			
			for (File s : dbrFiles) {
				String name = s.getName();
				VocabularyImportConfiguration.compress(name.substring(0, name.lastIndexOf(".")));
				VocabularyImportConfiguration.deleteTemp(name.substring(0, name.lastIndexOf(".")));
			}
			importer.eraseIndex();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public DBPedia2Vocabulary(OWLImportConfiguration conf, List<String[]> restrictToType) {

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
	
	private void readOntology(OWLImportConfiguration conf) throws OWLOntologyCreationException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(IRI.create(new File(ontologyFile).listFiles()[0].toURI().toString()));
		
		reasoner = new Reasoner.ReasonerFactory().createReasoner(ontology);
		
		OWLAnnotationProperty ap = df.getOWLAnnotationProperty(IRI.create(conf.labelProperty));
		
		ObjectNode vocabulary = Json.newObject();
		vocabulary.put("name", conf.prefix);
		vocabulary.put("version", conf.version);
		
		ArrayNode schemes = Json.newObject().arrayNode();
		schemes.add(conf.mainScheme);

		Set<OWLClass> topConcepts = new HashSet<>();
		
		ArrayList<ObjectNode> jsons = new ArrayList<>();
		for (OWLClass cz : ontology.getClassesInSignature()) {
			if (!conf.keep(cz)) {
				continue;
			}

			ObjectNode json = Json.newObject();
			jsons.add(json);
			
			ObjectNode semantic = Json.newObject();
			json.put("semantic", semantic);
			semantic.put("uri", cz.getIRI().toString());
			semantic.put("type", "http://www.w3.org/2004/02/skos/core#Concept");
			
			ObjectNode prefLabel = labelMap.get(cz);
			if (prefLabel == null) {
				prefLabel = Json.newObject();
				labelMap.put(cz, prefLabel);
			}
			
			for (OWLAnnotation ann : cz.getAnnotations(ontology, ap)) {
				String label = ann.getValue().toString();
				
				Matcher lm = labelPattern.matcher(label);
				if (lm.find()) {
					Language lang = Language.getLanguage(lm.group(2));
					if (lang != null) {
						prefLabel.put(lang.getDefaultCode(), JSONObject.escape(lm.group(1)));
					}
				}
			}
			
			semantic.put("prefLabel", prefLabel);

			Set<OWLClass> dsc = new HashSet<>();
			for ( OWLClass c :reasoner.getSuperClasses(cz, true).getFlattened()) {
				if (conf.keep(c)) {
					dsc.add(c);
				}
			}
			
			if (dsc.size() > 0) {
				ArrayNode arr = Json.newObject().arrayNode();
			
				for (OWLClass sc : dsc) {
					ObjectNode term = Json.newObject();
					term.put("uri", sc.getIRI().toString());
					term.put("type", "http://www.w3.org/2004/02/skos/core#Concept");
					
					ObjectNode pLabel = labelMap.get(sc);
					if (pLabel == null) {
						pLabel = Json.newObject();
						labelMap.put(sc, pLabel);
					}
					term.put("prefLabel", pLabel);
					
					arr.add(term);
				}
				semantic.put("broader", arr);
			} else {
				topConcepts.add(cz);
			}
			
			Set<OWLClass> asc = new HashSet<>();
			for ( OWLClass c :reasoner.getSuperClasses(cz, false).getFlattened()) {
				if (conf.keep(c)) {
					asc.add(c);
				}
			}
			if (asc.size() > 0) {
				ArrayNode arr = Json.newObject().arrayNode();
			
				for (OWLClass sc : asc) {
					ObjectNode term = Json.newObject();
					term.put("uri", sc.getIRI().toString());
					term.put("type", "http://www.w3.org/2004/02/skos/core#Concept");
					
					ObjectNode pLabel = labelMap.get(sc);
					if (pLabel == null) {
						pLabel = Json.newObject();
						labelMap.put(sc, pLabel);
					}
					term.put("prefLabel", pLabel);
					
					arr.add(term);
				}
				semantic.put("broaderTransitive", arr);
			}

			Set<OWLClass> nar = new HashSet<>();
			for (OWLClass c :reasoner.getSubClasses(cz, true).getFlattened()) {
				if (conf.keep(c)) {
					nar.add(c);
				}
			}
			
			if (nar.size() > 0) {
				ArrayNode arr = Json.newObject().arrayNode();
			
				for (OWLClass sc : nar) {
					ObjectNode term = Json.newObject();
					term.put("uri", sc.getIRI().toString());
					term.put("type", "http://www.w3.org/2004/02/skos/core#Concept");
					
					ObjectNode pLabel = labelMap.get(sc);
					if (pLabel == null) {
						pLabel = Json.newObject();
						labelMap.put(sc, pLabel);
					}
					term.put("prefLabel", pLabel);
					
					arr.add(term);
				}
				semantic.put("narrower", arr);
			} 
			
			semantic.put("inSchemes", schemes);
			semantic.put("vocabulary", vocabulary);
		}

		
		try (FileWriter fr = new FileWriter(new File(dboFile));
            BufferedWriter br = new BufferedWriter(fr)) {
			
			for (ObjectNode on : jsons) {
				ObjectNode sem = (ObjectNode)on.get("semantic");
				if (sem.get("prefLabel").size() == 0) {
					sem.remove("prefLabel");
				}
				
				ArrayNode bro = (ArrayNode)sem.get("broader");
				if (bro != null) {
					for (Iterator<JsonNode> iter = bro.elements(); iter.hasNext();) {
						ObjectNode el = ((ObjectNode)iter.next());
						if (el.get("prefLabel").size() == 0) {
							el.remove("prefLabel");
						}
					}
				}

				ArrayNode brt = (ArrayNode)sem.get("broaderTransitive");
				if (brt != null) {
					for (Iterator<JsonNode> iter = brt.elements(); iter.hasNext();) {
						ObjectNode el = ((ObjectNode)iter.next());
						if (el.get("prefLabel").size() == 0) {
							el.remove("prefLabel");
						}
					}
				}
				
				ArrayNode nar = (ArrayNode)sem.get("narrower");
				if (nar != null) {
					for (Iterator<JsonNode> iter = nar.elements(); iter.hasNext();) {
						ObjectNode el = ((ObjectNode)iter.next());
						if (el.get("prefLabel").size() == 0) {
							el.remove("prefLabel");
						}
					}
				}
				

				br.write(on.toString());
				br.write(VocabularyImportConfiguration.newLine);
			}
			
			ObjectNode jtop = Json.newObject();
			ObjectNode top = Json.newObject();
			top.put("uri", conf.mainScheme);
			top.put("type", "http://www.w3.org/2004/02/skos/core#ConceptScheme");
			
			ObjectNode prefLabel = Json.newObject();
			prefLabel.put("en", conf.title);
			top.put("prefLabel", prefLabel);
			
			ArrayNode arr = Json.newObject().arrayNode();
			for (OWLClass sc : topConcepts) {
				ObjectNode term = Json.newObject();
				term.put("uri", sc.getIRI().toString());
				term.put("type", "http://www.w3.org/2004/02/skos/core#Concept");
				
				ObjectNode pLabel = labelMap.get(sc);
				if (pLabel == null) {
					pLabel = Json.newObject();
					labelMap.put(sc, pLabel);
				}
				term.put("prefLabel", pLabel);
				
				arr.add(term);
			}
			
			top.put("topConcepts", arr);
			top.put("vocabulary", vocabulary);

			jtop.put("semantic", top);
			
			br.write(jtop.toString());

        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}
	
	private void prepareIndex() throws IOException {
		System.out.println("Creating index");
		
		tmpIndex = new File(System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "lucene");
		if (!tmpIndex.exists()) {
			tmpIndex.mkdir();
		} else {
			for (File f : tmpIndex.listFiles()) {
				f.delete();
			}
		}

		OpenMode om = OpenMode.CREATE;
	  
		Directory dir = FSDirectory.open(tmpIndex.toPath());
		IndexWriterConfig iwc = new IndexWriterConfig(new KeywordAnalyzer());
		iwc.setOpenMode(om);
		
		writer = new IndexWriter(dir, iwc);

	}
	
	private void eraseIndex() throws IOException {
		if (tmpIndex.exists()) {
			for (File f : tmpIndex.listFiles()) {
				f.delete();
			}
		}
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
								
								Document doc = new Document();
						
								doc.add(new StringField("id", id, Field.Store.YES));
								doc.add(new StringField("uri", uri, Field.Store.YES));
								doc.add(new StringField("type", type, Field.Store.YES));
								doc.add(new StringField("value", value, Field.Store.YES));
			
								writer.addDocument(doc);
								count++;
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
	
	private File[] createThesaurus() throws IOException, CompressorException {
		ArrayNode schemes = Json.newObject().arrayNode();
		schemes.add("http://with.image.ntua.gr/schema/dbr");

		ObjectNode vocabulary = Json.newObject();
		vocabulary.put("name", "dbr");
		vocabulary.put("version", "2015-10");

		File[] dbrFiles;
		
		try (Directory dir = FSDirectory.open(tmpIndex.toPath());
			  DirectoryReader reader = DirectoryReader.open(dir)) {
		
			IndexSearcher searcher = new IndexSearcher(reader);
		
			System.out.println("Creating thesaurus files");
			try (FileInputStream fin = new FileInputStream(inPath + File.separator + labelsFileName);
		         BufferedInputStream bis = new BufferedInputStream(fin);
		         CompressorInputStream input = new CompressorStreamFactory().createCompressorInputStream(bis);
		         BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
				
				if (filter == null) {
					dbrFiles = new File[] {new File(VocabularyImportConfiguration.outPath + File.separator + "dbr.txt")};
				} else {
					dbrFiles = new File[filterName.length];
					for (int i = 0; i < filterName.length; i++) {
						dbrFiles[i] = new File(VocabularyImportConfiguration.outPath + File.separator + "dbr-" + filterName[i] + ".txt");
						System.out.println(VocabularyImportConfiguration.outPath + File.separator + "dbr-" + filterName[i] + ".txt");
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
							
							List<String> labels = new ArrayList<>();
							Set<String> types = new HashSet<>();
							for (ScoreDoc hit : hits) {
								Document doc = searcher.doc(hit.doc);
								
								String t = doc.getValues("type")[0];
								String v = doc.getValues("value")[0];
								
								if (t.equals("http://www.w3.org/2000/01/rdf-schema#label")) {
									labels.add(v);
								} else if (t.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") && v.contains("dbpedia")) {
									types.add(v);
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
									Language lang = Language.getLanguage(lm.group(2));
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
									ObjectNode term = Json.newObject();
									term.put("uri", sc.getIRI().toString());
									term.put("type", "http://www.w3.org/2004/02/skos/core#Concept");
									
									ObjectNode pLabel = labelMap.get(sc);
									if (pLabel != null) {
										term.put("prefLabel", pLabel);
									}
									
									arr.add(term);
								}
								semantic.put("broader", arr);
							}
							
							Integer[] forFilter = new Integer[0];
	
							if (asc.size() > 0) {
								ArrayNode arr = Json.newObject().arrayNode();
							
								for (OWLClass sc : asc) {
									ObjectNode term = Json.newObject();
									term.put("uri", sc.getIRI().toString());
									term.put("type", "http://www.w3.org/2004/02/skos/core#Concept");
									
									ObjectNode pLabel = labelMap.get(sc);
									if (pLabel != null) {
										term.put("prefLabel", pLabel);
									}
									
									arr.add(term);
									
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
							
							semantic.put("inSchemes", schemes);
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
		
		return dbrFiles;
	}
}
