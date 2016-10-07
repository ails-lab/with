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
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;


public class OWL2Vocabulary {

	
	private static OWLDataFactory df = new OWLManager().getOWLDataFactory();

	private static OWLImportConfiguration nerd = new OWLImportConfiguration("nerd", 
	        "Named Entity Recognition and Disambiguation Ontology", 
	        "nerd", 
	        "0.5", 
	        null,
	        "nerd.eurecom.fr",
	        "http://nerd.eurecom.fr/ontology",
	        "http://nerd.eurecom.fr/ontology#Thing");	

	public static OWLImportConfiguration[] confs = new OWLImportConfiguration[] { nerd };
	
	public static void main(String[] args) {
		doImport(confs);
	}
	
	public static void doImport(OWLImportConfiguration[] confs) {
		for (OWLImportConfiguration c : confs) {
			try {
				doImport(c);
				c.compress();
				c.deleteTemp();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void doImport(OWLImportConfiguration conf) throws OWLOntologyCreationException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(IRI.create(conf.getInputFolder().listFiles()[0].toURI().toString()));
		
		Map<OWLClass, ObjectNode> labelMap = new HashMap<>();

		OWLReasoner reasoner = new Reasoner.ReasonerFactory().createReasoner(ontology);
		
		OWLAnnotationProperty ap = null;
		if (conf.labelProperty != null) {
			ap = df.getOWLAnnotationProperty(IRI.create(conf.labelProperty));
		}
		
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
			if (conf.isTop(cz)) {
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
			
			if (ap != null) {
				for (OWLAnnotation ann : cz.getAnnotations(ontology, ap)) {
					String label = ann.getValue().toString();
						
					Matcher lm = conf.labelMatcher(label);
					if (lm.find()) {
						Language lang = Language.getLanguage(lm.group(2));
						if (lang != null) {
							prefLabel.put(lang.getDefaultCode(), JSONObject.escape(lm.group(1)));
						}
					}
				}
			}
			
			semantic.put("prefLabel", prefLabel);

			Set<OWLClass> dsc = new HashSet<>();
			for ( OWLClass c :reasoner.getSuperClasses(cz, true).getFlattened()) {
				if (!conf.keep(c)) {
					continue;
				}
				if (conf.isTop(c)) {
					continue;
				}
				dsc.add(c);
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
			for (OWLClass c :reasoner.getSuperClasses(cz, false).getFlattened()) {
				if (!conf.keep(c)) {
					continue;
				}
				if (conf.isTop(c)) {
					continue;
				}

				
				asc.add(c);
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
				if (!conf.keep(c)) {
					continue;
				}
				if (conf.isTop(c)) {
					continue;
				}

				nar.add(c);
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

		
		try (FileWriter fr = new FileWriter(conf.getOutputFile());
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
			ObjectNode topc = Json.newObject();
			topc.put("uri", conf.mainScheme);
			topc.put("type", "http://www.w3.org/2004/02/skos/core#ConceptScheme");
			
			ObjectNode prefLabel = Json.newObject();
			prefLabel.put("en", conf.title);
			topc.put("prefLabel", prefLabel);
			
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
				if (pLabel.size() > 0) {
					term.put("prefLabel", pLabel);
				}
				
				arr.add(term);
			}
			
			topc.put("topConcepts", arr);
			topc.put("vocabulary", vocabulary);

			jtop.put("semantic", topc);
			
			br.write(jtop.toString());

        } catch (IOException e) {
			e.printStackTrace();
		}
	
	}
	
}
