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


package annotators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import play.libs.Json;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import utils.annotators.AnnotatedObject;
import utils.annotators.AnnotationIndex;
import utils.annotators.AnnotationValue;
import utils.annotators.Span;
import vocabularies.Vocabulary;
import model.annotations.Annotation;
import model.annotations.Annotation.AnnotationAdmin;
import model.annotations.Annotation.MotivationType;
import model.annotations.bodies.AnnotationBodyTagging;
import model.annotations.selectors.PropertyTextFragmentSelector;
import model.annotations.targets.AnnotationTarget;
import model.basicDataTypes.Language;
import model.basicDataTypes.MultiLiteral;
import model.resources.ThesaurusObject;
import model.resources.ThesaurusObject.SKOSSemantic;
import actors.annotation.NLPAnnotatorActor;
import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import akka.util.Timeout;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.dict.DictionaryEntry;
import com.aliasi.dict.ExactDictionaryChunker;
import com.aliasi.dict.MapDictionary;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.DB;

public class LookupAnnotator implements TextAnnotator {
	
	protected Language lang;

	public static String VOCABULARIES = "VOCABULARIES";
	
	protected static Map<Language, LookupAnnotator> annotators = new HashMap<>();

	private ExactDictionaryChunker tt;
	
	public static String getName() {
		return "WITH Dictionary Annotator";
	}

    public static synchronized LookupAnnotator getAnnotator(Language lang, boolean cs) {

		LookupAnnotator ta = annotators.get(lang);

    	if (ta == null) {
   			ta = new LookupAnnotator(lang, cs);
   			annotators.put(lang, ta);
   		} 
    	
    	return ta;
    } 

	private LookupAnnotator(Language lang, boolean caseSensitive) {
		this.lang = lang;
		
		String langField = "semantic.prefLabel." + lang.getDefaultCode();
		String altLangField = "semantic.altLabel." + lang.getDefaultCode();
		String enField = "semantic.prefLabel.en";
		String uriField = "semantic.uri";
		String thesaurusField = "semantic.vocabulary.name";
		
		Map<String, ArrayList<ObjectNode>> map = new HashMap<>();
		
		List<ThesaurusObject> res = DB.getThesaurusDAO().getByFieldAndValue("semantic.type", "http://www.w3.org/2004/02/skos/core#Concept", Arrays.asList(new String[] {langField, altLangField, enField, uriField, thesaurusField}));
		
		for (ThesaurusObject to : res) {
			SKOSSemantic semantic = to.getSemantic();
			
			String label = null;
			String enLabel = null;
			if (semantic.getPrefLabel() != null) {
				label = semantic.getPrefLabel().getLiteral(lang);
				enLabel = semantic.getPrefLabel().getLiteral(Language.EN);
			}

			List<String> altLabels = null;
			if (semantic.getAltLabel() != null) {
				altLabels = semantic.getAltLabel().get(lang);
			}
			
			String uri = semantic.getUri();
			String thesaurus = semantic.getVocabulary().getName();

			

			if (label != null && uri != null) {
				ObjectNode cc = Json.newObject();
				cc.put("uri", uri);
				cc.put("label", label);
				cc.put("vocabulary", thesaurus);

				if (enLabel != null) {
					cc.put("label-en", enLabel);
				}
					
				ArrayList<ObjectNode> list = map.get(label);
				if (list == null) {
					list = new ArrayList<>();
					map.put(label, list);
				}
				list.add(cc);
			}
				
			if (altLabels != null  && uri != null) {
				for (String altLabel : altLabels) {
					ObjectNode cc = Json.newObject();
					cc.put("uri", uri);
					cc.put("label", altLabel);
					cc.put("vocabulary", thesaurus);

					if (enLabel != null) {
						cc.put("label-en", enLabel);
					}
						
					ArrayList<ObjectNode> list = map.get(altLabel);
					if (list == null) {
						list = new ArrayList<>();
						map.put(altLabel, list);
					}
					list.add(cc);
				}
			}
		}

		MapDictionary<String> dict = new MapDictionary<>();
		
		for (Map.Entry<String, ArrayList<ObjectNode>> entry : map.entrySet()) {
			ArrayNode cc = Json.newObject().arrayNode();
			for (ObjectNode node : entry.getValue()) {
				cc.add(node);
			}
			dict.addEntry(new DictionaryEntry<String>(entry.getKey(), Json.stringify(cc)));
		}
			
		tt = new ExactDictionaryChunker(dict, IndoEuropeanTokenizerFactory.INSTANCE, false, caseSensitive);

	}
	
	
	private static Timeout timeout = new Timeout(Duration.create(10000, "seconds"));

	@Override
	public List<Annotation> annotate(String text, ObjectId user, AnnotationTarget target, Map<String, Object> props) throws Exception {
		text = strip(text);
		List<Annotation> res = new ArrayList<>();
		
		Chunking chunking = tt.chunk(text);

		AnnotationIndex ai = null;

		ActorSelection nlpAnnotator = ((NLPAnnotatorActor.Descriptor)NLPAnnotatorActor.descriptor).getAnnotator(lang, false);
		if (nlpAnnotator != null) {
			Future<Object> future2 = Patterns.ask(nlpAnnotator, new NLPAnnotatorActor.ComputeAnnotationIndex(text), timeout);
			ai = ((NLPAnnotatorActor.AnnotationIndexResult)Await.result(future2, timeout.duration())).ai;
		}
		
		Set<Vocabulary> vocs = (Set<Vocabulary>)props.get(VOCABULARIES);
		
		String generator = getName();
		
	    for (Chunk chunk : chunking.chunkSet()) {
	    	JsonNode array = Json.parse(chunk.type());

	    	int start = chunk.start();
	    	int end = chunk.end();
	    	
//	    	Reject annotations not containing a noun
	    	if (ai != null) {
		    	ArrayList<AnnotatedObject> aos = ai.getAnnotationsInSpan(new Span(start, end));
		    	boolean accept = false; 
		    	loop:
		    	for (AnnotatedObject ao : aos) {
		    		Set<AnnotationValue> av = ao.get("POS");
		    		if (av != null) {
			    		for (AnnotationValue value : av) {
			    			if (value.getValue().toString().startsWith("NN")) {
			    				accept = true;
			    				break loop;
			    			}
			    		}
		    		}
		    	}
		    	
		    	if (!accept) {
		    		continue;
		    	}
	    	}
	    	
	    	for (Iterator<JsonNode> iter = array.elements(); iter.hasNext();) {
	    		JsonNode node = iter.next();
	    		
	    		Vocabulary voc = Vocabulary.getVocabulary(node.get("vocabulary").asText());
	    		if (vocs != null && !vocs.contains(voc)) {
	    			continue;
	    		}

	    		Annotation<AnnotationBodyTagging> ann = new Annotation<>();

	    		AnnotationBodyTagging annBody = new AnnotationBodyTagging();
	    		annBody.setUri(node.get("uri").asText());
	    		annBody.setUriVocabulary(voc.getName());
	    		
	    		MultiLiteral ml;
	    		JsonNode enLabel = node.get("label-en");
	    		if (enLabel != null) {
	    			ml = new MultiLiteral(Language.EN, enLabel.asText());
	    		} else {
	    			ml = new MultiLiteral(lang, node.get("label").asText());
	    		}
	    		ml.fillDEF();
	    		annBody.setLabel(ml);

	    		AnnotationTarget annTarget = (AnnotationTarget) target.clone();
	    		
	    		PropertyTextFragmentSelector selector = (PropertyTextFragmentSelector)annTarget.getSelector();
	    		selector.setStart(start);
	    		selector.setEnd(end);

	    		annTarget.setSelector(selector);

	    		ArrayList<AnnotationAdmin> admins  = new ArrayList<>();
	    		AnnotationAdmin admin = new Annotation.AnnotationAdmin();
	    		admin.setGenerator(generator);
	    		admin.setGenerated(new Date());
	    		admin.setConfidence(-1.0f);

	    		admins.add(admin);
	    		
	    		ann.setBody(annBody);
	    		ann.setTarget(annTarget);
	    		ann.setAnnotators(admins);
	    		ann.setMotivation(MotivationType.Tagging);

	    		res.add(ann);
	    	}
	    }

		return res;
	}

}
