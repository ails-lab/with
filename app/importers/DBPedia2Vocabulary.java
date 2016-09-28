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


package importers;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;


public class DBPedia2Vocabulary {

	private String ontologyFile = System.getProperty("user.dir") + DB.getConf().getString("vocabulary.srcpath") + File.separator + "dbpedia_2015-10.owl";
	private String labelsFileName = "labels_en.ttl.bz2";
	private String inPath = System.getProperty("user.dir") + DB.getConf().getString("vocabulary.srcpath");
	
	private File tmpIndex;
	
	private String dboFile = System.getProperty("user.dir") + DB.getConf().getString("vocabulary.path") + File.separator + "dbo.txt";
	private String dbrFile = System.getProperty("user.dir") + DB.getConf().getString("vocabulary.path") + File.separator + "dbx";
	
	private IndexWriter writer;
	private OWLReasoner reasoner; 
	
	private Pattern triple = Pattern.compile("^<(.*?)> <(.*?)> <?(.*?)>? \\.$");
	private Pattern labelPattern = Pattern.compile("^\"(.*?)\"@(.*)$");
	private String newLine = System.getProperty("line.separator");
	
	private Map<OWLClass, ObjectNode> labelMap = new HashMap<>();
	
	private static OWLDataFactory df = new OWLManager().getOWLDataFactory();

	private Set<OWLClass> filter;
	
	public static void main(String[] args) {
		new DBPedia2Vocabulary();
	}
	
	public DBPedia2Vocabulary() {

		filter = new HashSet<>();
		filter.add(df.getOWLClass(IRI.create("http://dbpedia.org/ontology/Person")));
		
		tmpIndex = new File(System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "lucene");
		try {
			prepare();
			readInstances();
			readOntology();
			createThesaurus();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void readOntology() throws OWLOntologyCreationException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(IRI.create(new File(ontologyFile).toURI().toString()));
		
		reasoner = new Reasoner.ReasonerFactory().createReasoner(ontology);
		
		OWLAnnotationProperty ap = df.getOWLAnnotationProperty(IRI.create("http://www.w3.org/2000/01/rdf-schema#label"));
		
		ObjectNode vocabulary = Json.newObject();
		vocabulary.put("name", "dbo");
		vocabulary.put("version", "2015-10");
		
		ArrayNode schemes = Json.newObject().arrayNode();
		schemes.add("http://dbpedia.org/ontology");

		Set<OWLClass> topConcepts = new HashSet<>();
		
		ArrayList<ObjectNode> jsons = new ArrayList<>();
		for (OWLClass cz : ontology.getClassesInSignature()) {
			if (cz.getIRI().toString().contains("dbpedia")) {

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
					if (c.getIRI().toString().contains("dbpedia")) {
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
					if (c.getIRI().toString().contains("dbpedia")) {
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
				for ( OWLClass c :reasoner.getSubClasses(cz, true).getFlattened()) {
					if (c.getIRI().toString().contains("dbpedia")) {
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
				br.write(newLine);
			}
			
			ObjectNode jtop = Json.newObject();
			ObjectNode top = Json.newObject();
			top.put("uri", "http://dbpedia.org/ontology");
			top.put("type", "http://www.w3.org/2004/02/skos/core#ConceptScheme");
			
			ObjectNode prefLabel = Json.newObject();
			prefLabel.put("en", "DBPedia ontology");
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
	
	private void prepare() throws IOException {
		System.out.println("Creating index");
		
		for (File f : tmpIndex.listFiles()) {
			f.delete();
		}

		OpenMode om = OpenMode.CREATE;
	  
		Directory dir = FSDirectory.open(tmpIndex.toPath());
		IndexWriterConfig iwc = new IndexWriterConfig(new KeywordAnalyzer());
		iwc.setOpenMode(om);
		
		writer = new IndexWriter(dir, iwc);

	}

	private void readInstances() throws IOException, CompressorException {
		System.out.println("Adding instances");
		
		for (File f : new File(inPath).listFiles()) {
			if (f.getName().endsWith("bz2")) {
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
		
		writer.close();
		
	}
	
	private void createThesaurus() throws IOException, CompressorException {
		ArrayNode schemes = Json.newObject().arrayNode();
		schemes.add("http://with.image.ntua.gr/schema/dbr");

		ObjectNode vocabulary = Json.newObject();
		vocabulary.put("name", "dbx");
		vocabulary.put("version", "2015-10");

		try (Directory dir = FSDirectory.open(tmpIndex.toPath());
			  DirectoryReader reader = DirectoryReader.open(dir)) {
		
			IndexSearcher searcher = new IndexSearcher(reader);
		
			if (filter != null) {
				dbrFile = dbrFile + "-restr";
			}
			
			dbrFile = dbrFile + ".txt";
			
			System.out.println("Creating thesaurus files");
			try (FileInputStream fin = new FileInputStream(inPath + File.separator + labelsFileName);
		         BufferedInputStream bis = new BufferedInputStream(fin);
		         CompressorInputStream input = new CompressorStreamFactory().createCompressorInputStream(bis);
		         BufferedReader br = new BufferedReader(new InputStreamReader(input));
					FileWriter fr = new FileWriter(new File(dbrFile));
		            BufferedWriter obr = new BufferedWriter(fr)) {
	
				String line;
		
				while ((line = br.readLine()) != null)   {
					Matcher m = triple.matcher(line);
					
					if (m.find()) {
						String uri = m.group(1);
	
						Query query = new TermQuery(new Term("uri", uri));
						TopDocs results = searcher.search(query, 100);
						
						ScoreDoc[] hits = results.scoreDocs;
						
						String label = null;
						Set<String> types = new HashSet<>();
						for (ScoreDoc hit : hits) {
							Document doc = searcher.doc(hit.doc);
							
							String t = doc.getValues("type")[0];
							String v = doc.getValues("value")[0];
							
							if (t.equals("http://www.w3.org/2000/01/rdf-schema#label")) {
								label = v;
							} else if (t.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") && v.contains("dbpedia")) {
								types.add(v);
							}
						}
						
						ObjectNode json = Json.newObject();
						ObjectNode semantic = Json.newObject();
						json.put("semantic", semantic);
						
						semantic.put("uri", uri);
						semantic.put("type", "http://with.image.ntua.gr/model/Instance");
						
						Matcher lm = labelPattern.matcher(label);
						if (lm.find()) {
							Language lang = Language.getLanguage(lm.group(2));
							if (lang != null) {
								ObjectNode prefLabel = Json.newObject();
								prefLabel.put(lang.getDefaultCode(), JSONObject.escape(lm.group(1)));
								semantic.put("prefLabel", prefLabel);
							}
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
								
//								ObjectNode pLabel = labelMap.get(sc);
//								if (pLabel == null) {
//									pLabel = Json.newObject();
//									labelMap.put(sc, pLabel);
//								}
//								term.put("prefLabel", pLabel);
								
								arr.add(term);
							}
							semantic.put("broader", arr);
						}
						
						boolean include = false;

						if (asc.size() > 0) {
							ArrayNode arr = Json.newObject().arrayNode();
//						
							for (OWLClass sc : asc) {
//								ObjectNode term = Json.newObject();
//								term.put("uri", sc.getIRI().toString());
//								term.put("type", "http://www.w3.org/2004/02/skos/core#Concept");
//								
//								ObjectNode pLabel = labelMap.get(sc);
//								if (pLabel == null) {
//									pLabel = Json.newObject();
//									labelMap.put(sc, pLabel);
//								}
//								term.put("prefLabel", pLabel);
//								
//								arr.add(term);
//								
								if (filter != null && filter.contains(sc)) {
									include = true;
								}
							}
//							semantic.put("broaderTransitive", arr);
						}
						
						semantic.put("inSchemes", schemes);
						semantic.put("vocabulary", vocabulary);

						if (include) {
							ObjectNode sem = (ObjectNode)json.get("semantic");
							if (sem.get("prefLabel").size() == 0) {
								sem.remove("prefLabel");
							}
							
//							ArrayNode bro = (ArrayNode)sem.get("broader");
//							if (bro != null) {
//								for (Iterator<JsonNode> iter = bro.elements(); iter.hasNext();) {
//									ObjectNode el = ((ObjectNode)iter.next());
//									if (el.get("prefLabel").size() == 0) {
//										el.remove("prefLabel");
//									}
//								}
//							}

//							ArrayNode brt = (ArrayNode)sem.get("broaderTransitive");
//							if (brt != null) {
//								for (Iterator<JsonNode> iter = brt.elements(); iter.hasNext();) {
//									ObjectNode el = ((ObjectNode)iter.next());
//									if (el.get("prefLabel").size() == 0) {
//										el.remove("prefLabel");
//									}
//								}
//							}

							obr.write(json.toString());
							obr.write(newLine);
						}
					}
				}
			}
		}
	}
}
