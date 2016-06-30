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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;

import play.libs.Json;
import model.annotations.Annotation;
import model.annotations.Annotation.AnnotationAdmin;
import model.annotations.Annotation.MotivationType;
import model.annotations.bodies.AnnotationBodyTagging;
import model.annotations.selectors.PropertyTextFragmentSelector;
import model.annotations.targets.AnnotationTarget;
import model.basicDataTypes.Language;
import model.basicDataTypes.MultiLiteral;
import annotators.struct.AnnotatedObject;
import annotators.struct.AnnotationIndex;
import annotators.struct.AnnotationValue;
import annotators.struct.Span;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.dict.DictionaryEntry;
import com.aliasi.dict.ExactDictionaryChunker;
import com.aliasi.dict.MapDictionary;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import elastic.Elastic;
import elastic.ElasticSearcher;
import elastic.ElasticSearcher.SearchOptions;

public class DictionaryAnnotator extends Annotator {
	
	protected static Map<Language, DictionaryAnnotator> annotators = new HashMap<>();

	private ExactDictionaryChunker tt;
	
	public static String VOCABULARIES = "VOCABULARIES";
	
    public static DictionaryAnnotator getAnnotator(Language lang, boolean cs) {
    	DictionaryAnnotator ta = annotators.get(lang);
    	
    	if (ta == null) {
    		synchronized (DictionaryAnnotator.class) {
	    		if (ta == null) {
		   			ta = new DictionaryAnnotator(lang, cs);
		   			annotators.put(lang, ta);
	    		}
    		}
   		} 
    	
    	return ta;
    } 
    
	private DictionaryAnnotator(Language lang, boolean caseSensitive) {
		this.lang = lang;
		
		String langField = "prefLabel." + lang.getDefaultCode();
		String altLangField = "altLabel." + lang.getDefaultCode();
		String enField = "prefLabel.en";
		String uriField = "uri";
		String thesaurusField = "thesaurus";
		
		ElasticSearcher es = new ElasticSearcher();
		
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		query.must(QueryBuilders.termQuery("_type", Elastic.thesaurusResource));

		SearchOptions so = new SearchOptions(0, Integer.MAX_VALUE);
		so.isPublic = false;
		
		SearchResponse res = es.execute(query, so, new String[] {langField, altLangField, uriField, thesaurusField, enField} );
		SearchHits sh = res.getHits();

		Map<String, ArrayList<ObjectNode>> map = new HashMap<>();
		
		for (Iterator<SearchHit> iter = sh.iterator(); iter.hasNext();) {
			SearchHit hit = iter.next();
			
			SearchHitField label = hit.field(langField);
			SearchHitField altLabel = hit.field(altLangField);
			SearchHitField uri = hit.field(uriField);
			SearchHitField thesaurus = hit.field(thesaurusField);
			SearchHitField enLabel = hit.field(enField);
			
			if (label != null && uri != null) {
				String labelx  = label.getValue().toString();
				
				ObjectNode cc = Json.newObject();
				cc.put("uri", uri.getValue().toString());
				cc.put("label", labelx);
				cc.put("vocabulary", thesaurus.getValue().toString());

				if (enLabel != null) {
					cc.put("label-en", enLabel.getValue().toString());
//				} else {
//					cc.put("label-en", label.getValue().toString());
				}
				
				ArrayList<ObjectNode> list = map.get(labelx);
				if (list == null) {
					list = new ArrayList<>();
					map.put(labelx, list);
				}
				list.add(cc);
			}
			
			if (altLabel != null  && uri != null) {
				for (Object altValue : altLabel.getValues()) {
					String labelx  = altValue.toString();
					
					ObjectNode cc = Json.newObject();
					cc.put("uri", uri.getValue().toString());
					cc.put("label", labelx);
					cc.put("vocabulary", thesaurus.getValue().toString());

					if (enLabel != null) {
						cc.put("label-en", enLabel.getValue().toString());
//					} else {
//						cc.put("label-en", label.getValue().toString());
					}
					
					ArrayList<ObjectNode> list = map.get(labelx);
					if (list == null) {
						list = new ArrayList<>();
						map.put(labelx, list);
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
	
	@Override
	public String getName() {
		return "Dictionary Annotator";
	}

	@Override
	public String getService() {
		return "";
	}
	
	@Override
	public List<Annotation> annotate(String text, AnnotationTarget target, Map<String, Object> props) throws Exception {
		text = strip(text);
		
		List<Annotation> res = new ArrayList<>();
		
		Chunking chunking = tt.chunk(text);
		
		NERAnnotator sann = NERAnnotator.getAnnotator(lang);
		AnnotationIndex ai = null;
		if (sann != null) {
			ai = sann.analyze(text);
		}
		
		Set<AnnotationBodyTagging.Vocabulary> vocs = (Set<AnnotationBodyTagging.Vocabulary>)props.get(VOCABULARIES);
		
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
		    		for (AnnotationValue value : ao.get("POS")) {
		    			if (value.getValue().toString().startsWith("NN")) {
		    				accept = true;
		    				break loop;
		    			}
		    		}
		    	}
		    	
		    	if (!accept) {
		    		continue;
		    	}
	    	}
	    	
	    	for (Iterator<JsonNode> iter = array.elements(); iter.hasNext();) {
	    		JsonNode node = iter.next();
	    		
	    		AnnotationBodyTagging.Vocabulary voc = AnnotationBodyTagging.Vocabulary.getVocabulary(node.get("vocabulary").asText());
	    		if (vocs != null && !vocs.contains(voc)) {
	    			continue;
	    		}

	    		Annotation<AnnotationBodyTagging> ann = new Annotation<>();

	    		AnnotationBodyTagging annBody = new AnnotationBodyTagging();
	    		annBody.setUri(node.get("uri").asText());
	    		annBody.setUriVocabulary(voc);
	    		
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
	    		admin.setGenerator(getName());
	    		admin.setGenerated(new Date());
	    		admin.setConfidence(-1.0f);
//	    		admin.setWithCreator(withCreator);
//	    		admin.setCreated(new Date());

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
