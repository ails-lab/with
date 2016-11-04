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
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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


public class OWL2Vocabulary extends Data2Vocabulary<OWLImportConfiguration> {

	
	private static OWLDataFactory df = new OWLManager().getOWLDataFactory();

	public static OWLImportConfiguration nerd = new OWLImportConfiguration("nerd", 
	        "Named Entity Recognition and Disambiguation Ontology", 
	        "nerd", 
	        "0.5", 
	        null,
	        "nerd.eurecom.fr",
	        null,
	        null,
	        "http://nerd.eurecom.fr/ontology#Thing");	

	public static OWLImportConfiguration[] confs = new OWLImportConfiguration[] { nerd };
	
	public static void doImport(OWLImportConfiguration... confs) {
		OWL2Vocabulary o2v = new OWL2Vocabulary();
		for (OWLImportConfiguration c : confs) {
			try {
				o2v.doImport(c);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	OWLOntology ontology;
	OWLAnnotationProperty ap;
	
	protected void doImport(OWLImportConfiguration conf) throws Exception {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(IRI.create(conf.getInputFolder().listFiles()[0].toURI().toString()));
		
		OWLReasoner reasoner = new Reasoner.ReasonerFactory().createReasoner(ontology);
		
		if (conf.labelProperty != null) {
			ap = df.getOWLAnnotationProperty(IRI.create(conf.labelProperty));
		}
		
		File tmpFolder = VocabularyImportConfiguration.getTempFolder();
		File outFile = new File(tmpFolder + File.separator + conf.folder + ".txt");
		
		System.out.println("Creating vocabulary in " + outFile);
		try (FileWriter fr = new FileWriter(outFile);
				BufferedWriter br = new BufferedWriter(fr)) {

			ObjectNode vocabulary = Json.newObject();
			vocabulary.put("name", conf.prefix);
			vocabulary.put("version", conf.version);
		
		
			System.out.println("Processing ontology");
			for (OWLClass cz : ontology.getClassesInSignature()) {
				if (!conf.keep(cz) || conf.isTop(cz)) {
					continue;
				}
			
				ObjectNode main = makeMainStructure(cz, conf);
				
				ArrayNode broader = makeNodesArray(filter(reasoner.getSuperClasses(cz, true).getFlattened(), conf), conf);
				if (broader != null) {
					main.put("broader", broader);
				}
					
				ArrayNode broaderTransitive = makeNodesArray(filter(reasoner.getSuperClasses(cz, false).getFlattened(), conf), conf);
				if (broaderTransitive != null) {
					main.put("broaderTransitive", broaderTransitive);
				}

				ArrayNode narrower = makeNodesArray(filter(reasoner.getSubClasses(cz, false).getFlattened(), conf), conf);
				if (narrower != null) {
					main.put("narrower", narrower);
				}
	
				main.put("vocabulary", vocabulary);
	
				ObjectNode json = Json.newObject();
				json.put("semantic", main);
	
				br.write(json.toString());
				br.write(VocabularyImportConfiguration.newLine);
			}
		}
	
		conf.cleanUp(tmpFolder);
	}
	
	private Set<OWLClass> filter(Set<OWLClass> set, OWLImportConfiguration conf) {
		Set<OWLClass> res = new HashSet<>();
		
		for (OWLClass cz : set) {
			if (cz.equals(df.getOWLThing()) || cz.equals(df.getOWLNothing()) || !conf.keep(cz) || conf.isTop(cz)) {
			} else {
				res.add(cz);
			}
		}
		
		return res;
	}
	
	protected ArrayNode makeNodesArray(Set<OWLClass> res, OWLImportConfiguration conf) {
		if (res.size() > 0) {
			ArrayNode array = Json.newObject().arrayNode();
		
			for (OWLClass s : res) {
				array.add(makeMainStructure(s, conf));
			}
			return array;
		} else {
			return null;
		}
	}
	
	protected ObjectNode makeMainStructure(OWLClass cz, OWLImportConfiguration conf) {
		ObjectNode prefLabel = Json.newObject();
		
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

		
		ObjectNode json = Json.newObject();
		json.put("uri", cz.getIRI().toString());
		
		String type = "http://www.w3.org/2004/02/skos/core#Concept";
		json.put("type", type);

		if (prefLabel.size() > 0) {
			json.put("prefLabel", Json.toJson(prefLabel));
		}
		
		return json;

	}
}
