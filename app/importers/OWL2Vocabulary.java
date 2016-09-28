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

	private String inPath = System.getProperty("user.dir") + DB.getConf().getString("vocabulary.srcpath");
	private String outPath = System.getProperty("user.dir") + DB.getConf().getString("vocabulary.path");
	
	private OWLReasoner reasoner; 
	
	private Pattern labelPattern = Pattern.compile("^\"(.*?)\"@(.*)$");
	private String newLine = System.getProperty("line.separator");
	
	private Map<OWLClass, ObjectNode> labelMap = new HashMap<>();
	
	private static OWLDataFactory df = new OWLManager().getOWLDataFactory();

	public static void main(String[] args) {
		new OWL2Vocabulary("nerd/nerd-v0.5.n3", 
				           "Named Entity Recognition and Disambiguation Ontology", 
				           null, 
				           "nerd", 
				           "0.5", 
				           "http://nerd.eurecom.fr/ontology", 
				           "nerd.eurecom.fr", 
				           "http://nerd.eurecom.fr/ontology#Thing");
	}
	
	public OWL2Vocabulary(String fn, String title, String labelProperty, String name, String version, String scheme, String filter, String top) {

		try {
			readOntology(fn, title, labelProperty, name, version, scheme, filter, top);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void readOntology(String fn, String title, String labelProperty, String name, String version, String scheme, String filter, String top) throws OWLOntologyCreationException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(IRI.create(new File(inPath + File.separator + fn).toURI().toString()));
		
		reasoner = new Reasoner.ReasonerFactory().createReasoner(ontology);
		
		OWLAnnotationProperty ap = null;
		if (labelProperty != null) {
			ap = df.getOWLAnnotationProperty(IRI.create(labelProperty));
		}
		
		ObjectNode vocabulary = Json.newObject();
		vocabulary.put("name", name);
		vocabulary.put("version", version);
		
		ArrayNode schemes = Json.newObject().arrayNode();
		schemes.add(scheme);

		Set<OWLClass> topConcepts = new HashSet<>();
		
		ArrayList<ObjectNode> jsons = new ArrayList<>();
		for (OWLClass cz : ontology.getClassesInSignature()) {
			if (filter != null && !cz.getIRI().toString().contains(filter)) {
				continue;
			}
			if (top != null && cz.getIRI().toString().equals(top)) {
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
						
					Matcher lm = labelPattern.matcher(label);
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
				if (filter != null && !c.getIRI().toString().contains(filter)) {
					continue;
				}
				if (top != null && c.getIRI().toString().equals(top)) {
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
				if (filter != null && !c.getIRI().toString().contains(filter)) {
					continue;
				}
				if (top != null && c.getIRI().toString().equals(top)) {
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
				if (filter != null && !c.getIRI().toString().contains(filter)) {
					continue;
				}
				if (top != null && c.getIRI().toString().equals(top)) {
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

		
		int cutout = Math.max(fn.lastIndexOf("/"), fn.lastIndexOf("\\"));
		if (cutout > 0) {
			fn = fn.substring(cutout);
		}
		try (FileWriter fr = new FileWriter(new File(outPath + File.separator + fn + ".txt"));
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
			ObjectNode topc = Json.newObject();
			topc.put("uri", scheme);
			topc.put("type", "http://www.w3.org/2004/02/skos/core#ConceptScheme");
			
			ObjectNode prefLabel = Json.newObject();
			prefLabel.put("en", title);
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
				term.put("prefLabel", pLabel);
				
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
