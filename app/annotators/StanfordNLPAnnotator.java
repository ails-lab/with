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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import model.basicDataTypes.Language;


import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.bson.types.ObjectId;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;

import com.fasterxml.jackson.databind.JsonNode;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.tokensregex.TokenSequenceMatcher;
import edu.stanford.nlp.ling.tokensregex.TokenSequencePattern;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
//import edu.stanford.nlp.pipeline.CoreNLPProtos.IndexedWord;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.IntPair;
import elastic.Elastic;
import elastic.ElasticSearcher;
import elastic.ElasticSearcher.SearchOptions;
import play.libs.Json;

public class StanfordNLPAnnotator extends Annotator {

	public String getName() {
		return "StanfordNLP";
	}
	
	public String getService() {
		return "";
	}
	
    private SimpleAnnotationValue[] NERFinderTag;

    protected static Map<Language, StanfordNLPAnnotator> annotators = new HashMap<>();

    public static StanfordNLPAnnotator getAnnotator(Language lang) {
    	StanfordNLPAnnotator ta = annotators.get(lang);
    	if (ta == null) {
    		ta  = new StanfordNLPAnnotator(lang);
    		annotators.put(lang, ta);
    	}
    	
    	return ta;
    }    	
    	
    private StanfordCoreNLP pipeline;
    
    private StanfordNLPAnnotator(Language language) {
    	Properties props = new Properties();
//    	props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
//    	props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse");
    	props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
//    	props.put("annotators", "tokenize, ssplit, pos, lemma");
//    	props.put("annotators", "tokenize, ssplit, pos");
//    	props.put("annotators", "tokenize, ssplit");
    	
    	pipeline = new StanfordCoreNLP(props);
    }

	@Override
	public List<annotators.Annotation> annotate(String text, Map<String, Object> properties) throws Exception {
		text = strip(text);
		
		List<annotators.Annotation> res = new ArrayList<>();
		
		AnnotationIndex ai = analyze(text);
		
//		System.out.println(ai.getClassValueMap());
//		for (AnnotationValue av : ai.getValuesForClass("NE")) {
//			System.out.println(av);
		ArrayList<Span> locations = ai.getLocations("NE", new SimpleAnnotationValue("LOCATION"));
		if (locations != null) {
			for (Span span : locations) {
				res.add(new annotators.Annotation(this.getClass(), span.start, span.end, -1.0f, "http://nerd.eurecom.fr/ontology#Location", "Location", Annotator.Vocabulary.NERD));
			}
		}
		
		ArrayList<Span> persons = ai.getLocations("NE", new SimpleAnnotationValue("PERSON"));
		if (persons != null) {
			for (Span span : persons) {
				res.add(new annotators.Annotation(this.getClass(), span.start, span.end, -1.0f, "http://nerd.eurecom.fr/ontology#Person", "Person", Annotator.Vocabulary.NERD));
			}
		}

		ArrayList<Span> orgs = ai.getLocations("NE", new SimpleAnnotationValue("ORGANIZATION"));
		if (orgs != null) {
			for (Span span : orgs ) {
				res.add(new annotators.Annotation(this.getClass(), span.start, span.end, -1.0f, "http://nerd.eurecom.fr/ontology#Organization", "Organization", Annotator.Vocabulary.NERD));
			}
		}
		
		return res;
	}
	
	public AnnotationIndex analyze(String text) throws Exception {
		AnnotationIndex ta = new AnnotationIndex(text);
		
		Annotation document = new Annotation(text);

		pipeline.annotate(document);

       	for(CoreMap sentence: document.get(SentencesAnnotation.class)) {
       		
//       		List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
////       	(Adjective | Noun)* (Noun Preposition)? (Adjective | Noun)* Noun 
//
//       		TokenSequencePattern pattern = TokenSequencePattern.compile("[tag:/JJ.*|NN.*/]* ([tag:/NN.*/] [tag:\"IN\"])? [tag:/JJ.*|NN.*/]* [tag:/NN.*/] ");
//       		TokenSequenceMatcher matcher = pattern.getMatcher(tokens);
//       	   
//       		while (matcher.find()) {
//       			String matchedString = matcher.group();
//       			List<CoreMap> matchedTokens = matcher.groupNodes();
//       			
//       			String token1 = matchedString;
//       			String token2 = "";
//       			for (int i = 0; i < matchedTokens.size(); i++) {
//       				if (i > 0) {
//       					token2 += " ";
//       				}
//       				CoreMap token = matchedTokens.get(i);
//       				if (i == matchedTokens.size() - 1) {
//       					token2 += token.get(LemmaAnnotation.class);
//       				} else {
//       					token2 += token.get(TextAnnotation.class);
//       				}
//       				
//       				
//       			}
//       			System.out.println("<<<<<<<<<<<<<<<<< " + token1);
//       			System.out.println("<<<<<<<<<<<<<<<   " + token2);
//       		}
       	   
       		int ss = Integer.MAX_VALUE;
       		int se = Integer.MIN_VALUE;
    	   
       		int nerStart = -1;
       		int nerEnd= -1;
       		String nerType = "";

       		for (CoreLabel token: sentence.get(TokensAnnotation.class)) {

       			int s = token.beginPosition();
       			int e = token.endPosition();
        	   
       			if (s < ss) {
        			ss = s;
       			}
       			if (e > ss) {
       				se = e;
       			}
        	   
       			ta.add("TOKEN", null, s, e);
        	   
       			ta.add("POS", new SimpleAnnotationValue(token.get(PartOfSpeechAnnotation.class)), s, e);
       			ta.add("LEMMA", new SimpleAnnotationValue(token.get(LemmaAnnotation.class)), s, e);

       			String n = token.get(NamedEntityTagAnnotation.class);
       			
       			if (n.equals("O") || !nerType.equals(n)) {
       				if (nerStart != -1) {
       					ta.add("NE", new SimpleAnnotationValue(nerType), nerStart, nerEnd);
       					nerStart = -1;
       					nerEnd = -1;
       				}
        		   
       				if (!n.equals("O")) {
       					if (nerStart == -1) {
       						nerStart = s;
       					}
       					nerEnd = e;
       					nerType = n;
       				}
       			} else {
       				if (nerStart == -1) {
   						nerStart = s;
   					}
   					nerEnd = e;
   					nerType = n;
       			}
       		}
           
       		if (nerStart != -1) {
       			ta.add("NE", new SimpleAnnotationValue(nerType), nerStart, nerEnd);
       		}
           
       		ta.add("SENTENCE", null, ss, se);
       	}
       
       
//		LexicalizedParser lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");

//		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
//	    GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();

		Comparator<Span> ssc = new Span.StartSpanComparator();
		Comparator<Span> esc = new Span.EndSpanComparator();
		
		ArrayList<Span> sortedWordSpans = ta.getLocations("TOKEN", null);
			
		for (Span sentenceSpan : ta.getLocations("SENTENCE", null)) {
			ArrayList<String> tokens = new ArrayList<>();
			ArrayList<Span> annTokens = new ArrayList<>();
				
			int pos1 = Collections.binarySearch(sortedWordSpans, sentenceSpan, ssc);
			if (pos1 < 0) {
				pos1 = -pos1 - 1;
			}
			int pos2 = Collections.binarySearch(sortedWordSpans, sentenceSpan, esc);
			if (pos2 < 0) {
				pos2 = -pos2 - 1;
			}
				
			ArrayList<Span> spanList = new ArrayList<>();
			
			for (int i = pos1; i <= pos2; i++) {
				Span span = sortedWordSpans.get(i);
				spanList.add(span);
				AnnotatedObject anns = ta.getAnnotations(span); 
				Set<AnnotationValue> val = anns.get("TOKEN");
				if (val != null) {
					String word = text.substring(span.start, span.end);
			
					tokens.add(word);
					annTokens.add(span);
				}
			}
				
//			System.out.println("** " + tokens + "** " + pos1 + " " + pos2);
			
//			List<CoreLabel> rawWords = Sentence.toCoreLabelList(tokens.toArray(new String[] {}));
//		    Tree parse = lp.apply(rawWords);
//		    
//		    GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
//		    
//		    for (TypedDependency  tdl : gs.typedDependenciesCCprocessed()) {
//		    	IndexedWord tgn = tdl.dep();
//		    	int i = tgn.index();
//		    	Span s = annTokens.get(i - 1);
//		    	
//		    	if (tdl.gov().index() > 0) {
//		    		Span ss = spanList.get(tdl.gov().index() - 1);
//		    		ta.add("REF_ID", new ComplexAnnotationValue(tdl.reln().toString(), ta.getAnnotations(ss)), s.start, s.end);
//
//		    		AnnotatedObject ano = ta.getAnnotations(ss);
//					ta.add("IREF_ID", new ComplexAnnotationValue(tdl.reln().toString(), ta.getAnnotations(new Span(s.start, s.end))), ano.getSpan().start, ano.getSpan().end);
//		    	
////					System.out.println("** " + new ComplexAnnotationValue(tdl.reln().toString(), ta.getAnnotations(ss)) + "** " + text.substring(s.start, s.end) + " -- " + ta.getAnnotations(new Span(s.start, s.end)));
//					
//		    	} else {
//	    			ta.add("REF_ID", new ComplexAnnotationValue(tdl.reln().toString(), ""), s.start, s.end);
//	    			
////	    			System.out.println("** " + new ComplexAnnotationValue(tdl.reln().toString(), "") + "** " + text.substring(s.start, s.end) + " -- " + ta.getAnnotations(new Span(s.start, s.end)) );
//		    	}
//		    }
		}
		
		
//	
       return ta;
	}

	private class Struct implements Comparable<Struct> {
		public int id;
		public String lemma;
		public String surface;
		
		public Struct(int id, String lemma, String surface) {
			this.id = id;
			this.lemma = lemma;
			this.surface = surface;
		}

		@Override
		public int compareTo(Struct o) {
			if (id < o.id) {
				return -1;
			} else if (id > o.id) {
				return 1;
			} else {
				return 0;
			}
		}
		
		public String toString() {
			return surface; 
		}
		
	}
	
	public ArrayList<ArrayList<Struct>> chunk(AnnotationIndex ai) {
//		System.out.println(ai.getValuesForClass("POS"));
		
		ArrayList<ArrayList<Struct>> res = new ArrayList<>();
		
		for (Span span :ai.getLocations("POS", new SimpleAnnotationValue("NN"))) {
			AnnotatedObject ao = ai.getAnnotations(span);
			
			String lemma = (String)ao.get("LEMMA").iterator().next().getValue();
			
			Set<String> check = new HashSet<>();
			check.add("compound");
			check.add("amod");
			
			ArrayList<Struct> phrase = new ArrayList<>();
			phrase.add(new Struct(ao.getID(), lemma, ao.getText()));
			
			System.out.println("----------------------- SEARCHING " + lemma);
			if (ao.get("IREF_ID") != null) {
				for (AnnotationValue x : ao.get("IREF_ID")) {
					ComplexAnnotationValue cav = (ComplexAnnotationValue)x;
					Object[] value = (Object[])cav.getValue();
					
					String type = (String)value[0];
					AnnotatedObject ref = (AnnotatedObject)value[1];
					
					if (check.contains(type)) {
						phrase.add(new Struct(ref.getID(), (String)ref.get("LEMMA").iterator().next().getValue(), ref.getText()));
					}
//					System.out.println(cav + " " + type + " " + ref.getID());
				}
				
				Collections.sort(phrase);
			}	
			
			res.add(phrase);
		}
		
		return res;
	}
	
	public static void main(String[] args) throws Exception {
//		String text = "The 'Hop Bine' was built in 1870 ready for later development in the eastern side of Biggleswade. Its description in 1898 was - \"A brick and slated house containing Tap Room, Parlour, Kitchen, Cellar and four upper rooms. Yard with boarded and tiled Shed, enclosed at either end as Stable and Coal-house; w.c.&c; also a piece of Garden Ground at side and back. The first owners were Frederick Archdale and Charles Robert Lindsell (namely Wells and Co). The first licensee was George Chesham from 1870 to 1877. Until recently it was a thriving little pub then Greene King changed to half pub half Indian restaurant. That killed it off! Now (February 2012) it is closed; the shutters are on and all signs have been removed; Greene King have applied for it to be changed to residential and to develop another two houses on the car park - pack 'em in tight!";
//		String text = "Have a look at these books and that book. I received a big nice mother of pearl. I felt especially proud to receive my commission as an officer in front of my grandmother, The Queen. She has been an incredible role model to me over the years, so it was very special to have her present for my graduation. I know it was also a memorable moment for my fellow officers to take part in the Parade in front of The Queen as Head of the Armed Forces."; 
//		String text = "Car salesman Tony Wilkins gives a helping hand to receptionist Linda Edser, aged 21. He carried her through the flood water in Cowbridge Road East, Cardiff.";
		String text = "The 'Hop Bine' was built in 1870 ready for later development in the eastern side of Biggleswade. Its description in 1898 was - A brick and slated house containing Tap Room, Parlour, Kitchen, Cellar and four upper rooms. Yard with boarded and tiled Shed, enclosed at either end as Stable and Coal-house; w.c.&c; also a piece of Garden Ground at side and back. The first owners were Frederick Archdale and Charles Robert Lindsell (namely Wells and Co). The first licensee was George Chesham from 1870 to 1877.  Until recently it was a thriving little pub then Greene King changed to half pub half Indian restaurant. That killed it off! Now (February 2012) it is closed; the shutters are on and all signs have been removed; Greene King have applied for it to be changed to residential and to develop another two houses on the car park - pack 'em in tight!";
		
		StanfordNLPAnnotator ta = StanfordNLPAnnotator.getAnnotator(Language.EN);

		ta.annotate(text, new HashMap<String, Object>());
//		AnnotationIndex ai = ta.analyze(text);
//		
//		System.out.println(ai.getValuesForClass("NE"));
		
//		System.out.println(ai.getValuesForClass("POS"));
//		
//		ArrayList<ArrayList<Struct>> list = ta.chunk(ai);
//		System.out.println(list);
		
//		for (Span span :ai.getLocations("POS", new SimpleAnnotationValue("NN"))) {
//			AnnotatedObject ao = ai.getAnnotations(span);
//			
//			String lemma = (String)ao.get("LEMMA").iterator().next().getValue();
//			
//			Set<String> check = new HashSet<>();
//			check.add("compound");
//			check.add("amod");
//			
//			ArrayList<Struct> phrase = new ArrayList<>();
//			phrase.add(new Struct(ao.getID(), lemma, ao.getText()));
//			
//			System.out.println("----------------------- SEARCHING " + lemma);
//			if (ao.get("IREF_ID") != null) {
//				for (AnnotationValue x : ao.get("IREF_ID")) {
//					ComplexAnnotationValue cav = (ComplexAnnotationValue)x;
//					Object[] value = (Object[])cav.getValue();
//					
//					String type = (String)value[0];
//					AnnotatedObject ref = (AnnotatedObject)value[1];
//					
//					if (check.contains(type)) {
//						phrase.add(new Struct(ref.getID(), (String)ref.get("LEMMA").iterator().next().getValue(), ref.getText()));
//					}
////					System.out.println(cav + " " + type + " " + ref.getID());
//				}
//				
//				Collections.sort(phrase);
//			}			
//			
//			System.out.println(phrase);
			
//			ElasticSearcher es = new ElasticSearcher();
//			
////			MatchQueryBuilder query = QueryBuilders.matchQuery("collectedIn.collectionId", id);
//			BoolQueryBuilder query = QueryBuilders.boolQuery();
//			query.must(QueryBuilders.termQuery("_type", Elastic.thesaurusResource));
//			query.must(QueryBuilders.matchQuery("prefLabel.en", lemma));
//
//			SearchResponse res = es.execute(query, new SearchOptions(0, Integer.MAX_VALUE), new String[] {"preLabel","prefLabel.en", "thesaurus", "uri"} );
//			SearchHits sh = res.getHits();
//
//			List<String[]> list = new ArrayList<>();
//
//			for (Iterator<SearchHit> iter = sh.iterator(); iter.hasNext();) {
//				SearchHit hit = iter.next();
//
//				List<Object> olist = new ArrayList<>();
//				
//				for (String field : new String[] {"prefLabel.en", "thesaurus", "uri"}) {
//					SearchHitField shf = hit.field(field);
//				
//					System.out.print(shf.getValues()  + " ");
//				}				
//				System.out.println();
//				
//			}
//		}
		
		
		
		
		
	}
	

}
