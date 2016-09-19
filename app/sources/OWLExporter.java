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


package sources;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bson.types.ObjectId;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

import model.DescriptiveData.Quality;
import model.basicDataTypes.Language;
import model.basicDataTypes.MultiLiteral;
import model.basicDataTypes.MultiLiteralOrResource;
import model.basicDataTypes.ProvenanceInfo;
import model.resources.CulturalObject;
import model.resources.CulturalObject.CulturalObjectData;
import model.resources.RecordResource.RecordDescriptiveData;
import model.resources.WithResourceType;
import sources.core.Utils;
import utils.ListUtils;

public class OWLExporter {
	private static final String _SPACE = "_space_";


	private static final String _SLASH = "_slash_";

	
	private OWLOntologyManager manager;
	private OWLOntology ontology;
	private OWLDataFactory factory;
	private DefaultPrefixManager prefixManager;
	private StringWriter string;


	public OWLExporter(String name){
		initOntology();
	}
	
	public void initOntology() {
//		Create the manager that we will use to load ontologies.
		manager = OWLManager.createOWLOntologyManager();
//		Create an ontology and name it "http://www.co-ode.org/ontologies/testont.owl"
		String base = "http://with.com/owl/resources/";
		IRI ontologyIRI = IRI.create(base);
//		Now create the ontology -we use the ontology IRI (not the physical URI)
		try {
			ontology = manager.createOntology(ontologyIRI);
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// 
//		Specify that A is a subclass of B.  Add a subclass axiom.  
		factory = manager.getOWLDataFactory();
		prefixManager = new DefaultPrefixManager(base);
		// Get reference to the :Person class (the full IRI
		
//		Create the document IRI for our ontology
//		IRI documentIRI = IRI.create("file:/tmp/MyOnt.owl");
		// Set up a mapping, which maps the ontology to the document IRI
//		SimpleIRIMapper mapper = new SimpleIRIMapper(, documentIRI), documentIRI);
//		manager.add;
	}
	
	public void exportClassAssertion(String className, String instance){
		OWLClass owlclass = factory.getOWLClass(":"+className, prefixManager);
		OWLNamedIndividual owlinstance = getOwlInstance(instance);
		OWLClassAssertionAxiom classAssertion = factory.getOWLClassAssertionAxiom(owlclass, owlinstance);
		manager.addAxiom(ontology, classAssertion);
	}

	private OWLNamedIndividual getOwlInstance(String instance) {
		return factory.getOWLNamedIndividual(":"+cleanName(instance), prefixManager);
	}

	static String cleanName(String instance) {
		return instance.replace("/", _SLASH).replace(" ", _SPACE);
	}
	
	public void exportRoleAssertion(String instanceA, String roleName, String instanceB) {
		OWLObjectProperty role = factory.getOWLObjectProperty(":"+roleName,prefixManager);
		OWLObjectPropertyAssertionAxiom assertion =  
		factory.getOWLObjectPropertyAssertionAxiom(role, getOwlInstance(instanceA), getOwlInstance(instanceB));
		manager.addAxiom(ontology, assertion);
	}
	
	public void exportDataPropertyAssertion(String instance, String propertyName, Object data) {
		if (data!=null){
			OWLDataProperty role = factory.getOWLDataProperty(":"+propertyName,prefixManager);
			OWLDataPropertyAssertionAxiom assertion = null;
			switch (data.getClass().getName()) {
			case "int":
			case "java.lang.Integer":
				assertion = factory.getOWLDataPropertyAssertionAxiom(role, getOwlInstance(instance),(Integer.parseInt(data.toString())));	
				break;
			case "java.lang.Double":
			case "java.lang.Float":
			case "double":
			case "float":
				assertion = factory.getOWLDataPropertyAssertionAxiom(role, getOwlInstance(instance),(Double.parseDouble(data.toString())));	
				break;
			case "java.lang.String":
			default:
				assertion = factory.getOWLDataPropertyAssertionAxiom(role, getOwlInstance(instance),data.toString());	
				break;
			}
			manager.addAxiom(ontology, assertion);
		}
	}
	
	public void flush(){
		// TODO save the ontology
		string = new StringWriter();
		try {
			manager.saveOntology(ontology, new OutputStream()
			{
			   
			@Override
			    public void write(int b) throws IOException {
			        string.append((char) b );
			    }

			    //Netbeans IDE automatically overrides this toString()
			    public String toString(){
			        return string.toString();
			    }
			});
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static class CulturalItemOWLExporter extends OWLExporter{
		
		public CulturalItemOWLExporter(String name) {
			super(name);
			// TODO Auto-generated constructor stub
		}

		public void exportItem(CulturalObject item){
			ProvenanceInfo p = item.getProvenance().get(item.getProvenance().size()-1);
			String instance = p.getProvider()+cleanName(p.getResourceId());
			CulturalObjectData descriptiveData = (CulturalObjectData) item.getDescriptiveData();
			exportDataPropertyAssertion(instance, "hasLabel", toText(descriptiveData.getLabel()));
			exportDataPropertyAssertion(instance, "hasDescription", toText(descriptiveData.getDescription()));
			exportDataPropertyAssertion(instance, "hasKeywords", toText(descriptiveData.getKeywords()));
			exportDataPropertyAssertion(instance, "hasAltLabels", toText(descriptiveData.getAltLabels()));
			for (String c : toManyText(descriptiveData.getCountry())) {
				exportRoleAssertion(instance, "hasCountry", c);
			}
			for (String c : toManyText(descriptiveData.getCity())) {
				exportRoleAssertion(instance, "hasCity", c);
			}
			Quality metadataQuality = descriptiveData.getMetadataQuality();
			if (metadataQuality!=null)
				exportClassAssertion(metadataQuality.toString()+"_metadataQuality", instance);
			for (String c : toManyText(descriptiveData.getDccontributor())) {
				exportRoleAssertion(instance, "hasContributor", c);
			}
			MultiLiteralOrResource dccreator = descriptiveData.getDccreator();
			List<String> manyText = toManyText(dccreator);
			for (String c : manyText) {
				exportRoleAssertion(instance, "hasCreator", c);
			}
			for (Language c : ListUtils.transform(toManyText(descriptiveData.getDclanguage()), (x)->Language.getLanguage(x))) {
				exportRoleAssertion(instance, "hasLanguage", c.getDefaultCode());
			}
			
			WithResourceType type = item.getResourceType();
			if (type!=null){
				exportClassAssertion(type+"_type", instance);
			}
			for (ObjectId e : item.getCollectedIn()) {
				exportClassAssertion(e+"_Collection", instance);
			}
		}

		private String toText(MultiLiteral literal) {
			if (Utils.hasInfo(literal)){
				List<String> list = literal.get(Language.DEFAULT);
				if (list==null){
					Iterator<List<String>> it = literal.values().iterator();
					if (it.hasNext())
						return it.next().toString();
				}
					
				return list.toString();
			} else
				return null;
		}
		
		private List<String> toManyText(MultiLiteral literal) {
			if (Utils.hasInfo(literal)){
				List<String> list = literal.fillDEF().get(Language.DEFAULT);
				if (list==null){
					Iterator<List<String>> it = literal.values().iterator();
					if (it.hasNext())
						return it.next();
				}
				return list;
			} else
				return new ArrayList<>();
		}
	}
	
	public String export(){
		flush();
		return string.toString();
	}

}
