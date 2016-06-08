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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.FileInputStream;

import model.basicDataTypes.Language;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.cmdline.postag.POSModelLoader;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.util.Span;



import com.fasterxml.jackson.databind.JsonNode;

import play.libs.Json;

public class OpenNLPThesaurusAnnotator extends Annotator {

	public String getName() {
		return "OpenNLP";
	}

	public String getService() {
		return "";
	}
	
	private SentenceDetector sd;
    private POSTagger posTagger;
    private ChunkerME chunker;
    private NameFinderME[] NERFinder; 
    private SimpleAnnotationValue[] NERFinderTag;

    protected static Map<Language, OpenNLPThesaurusAnnotator> annotators = new HashMap<>();

    public static OpenNLPThesaurusAnnotator getThesaurusAnnotator(Language lang) throws Exception {
    	OpenNLPThesaurusAnnotator ta = annotators.get(lang);
    	if (ta == null) {
    		ta  = new OpenNLPThesaurusAnnotator(lang);
    		annotators.put(lang, ta);
    	}
    	
    	return ta;
    }    	
    	
    private OpenNLPThesaurusAnnotator(Language language) throws Exception {
    	String lang = language.getDefaultCode();
    	
   		File sentenceFile = new File("resources/opennlp/" + lang + "-sent.bin");
   		if (sentenceFile.exists()) {
   			sd = new SentenceDetectorME(new SentenceModel(new FileInputStream(sentenceFile)));
   		} else {
   			sd = null;
   		}

		File posFile = new File("resources/opennlp/" + lang + "-pos-maxent.bin");
		if (posFile.exists()) {
			posTagger = new POSTaggerME(new POSModelLoader().load(posFile));
		} else {
			posTagger = null;
		}

		File chunkerFile = new File("resources/opennlp/" + lang + "-chunker.bin");
		if (chunkerFile.exists()) {
			chunker = new ChunkerME(new ChunkerModel(new FileInputStream(chunkerFile)));
		} else {
			chunker = null;
		}
		
		ArrayList<File> nerFiles = new ArrayList<>();
		
		File path = new File("resources/opennlp/");
		for (File f : path.listFiles()) {
			if (f.getName().startsWith(lang + "-ner-")) {
				nerFiles.add(f);
			}
		}
		
		NERFinder = new NameFinderME[nerFiles.size()];
		NERFinderTag = new SimpleAnnotationValue[nerFiles.size()];
		
		for (int i = 0; i < NERFinder.length; i++) {
			try {
				NERFinder[i] = new NameFinderME(new TokenNameFinderModel(new FileInputStream(nerFiles.get(i))));
				String fn = nerFiles.get(i).getName();
				NERFinderTag[i] = new SimpleAnnotationValue(fn.substring(7, fn.length() - 4).toUpperCase());
			} catch (Exception e) {
				NERFinder[i] = null;
			}
		}
	}
    

    public List<Annotation> annotate(String text, Map<String, Object> properties) throws Exception {
    	AnnotationIndex ta = new AnnotationIndex(text);
		
        for (Span s : sd.sentPosDetect(text)) {
        	int sentenceStart = s.getStart();
        	
        	String sentence = s.getCoveredText(text).toString();
        	System.out.println("SENTENCE " + sentence);

        	Span[] words = SimpleTokenizer.INSTANCE.tokenizePos(sentence);

            String[] simpleTokenizerLine = new String[words.length];
            for (int i = 0; i < words.length; i++) {
            	simpleTokenizerLine[i] = words[i].getCoveredText(sentence).toString();
            }
            
            String[] tags = posTagger.tag(simpleTokenizerLine);
            
            for (int c = 0; c < words.length; c++) {
            	Span cw = words[c];
            	int is = sentenceStart + cw.getStart();
            	int ie = sentenceStart + cw.getEnd();
            			
           		ta.add("TOKEN", null, is, ie);
           		ta.add("POS", new SimpleAnnotationValue(tags[c]), is, ie);
            }
            
        	String chunks[] = chunker.chunk(simpleTokenizerLine, tags);

        	if (chunks.length > 0) {

        		ArrayList<Integer> change = new ArrayList<>();
            	ArrayList<String> chunkt = new ArrayList<>();
            	
            	for (int c = 0; c < chunks.length; c++) {
            		if (!chunks[c].startsWith("I-")) {
            			chunkt.add(chunks[c]);
            			change.add(c);
            		}
            	}
            	change.add(chunks.length);
            
            	for (int c = 0; c < change.size() - 1; c++) {
            		int st = change.get(c);
            		int en = change.get(c + 1);
            		
            		Span scw = words[st];
            		Span ecw = words[en - 1];
            	
                	int is = sentenceStart + scw.getStart();
                	int ie = sentenceStart + ecw.getEnd();

               		String ss = chunkt.get(c).startsWith("B") ? chunkt.get(c).substring(2) : chunkt.get(c);
               		ta.add("CHUNK", new SimpleAnnotationValue(ss), is, ie);
               		System.out.println("CHUNK " + ss  + " " + text.substring(is, ie));

            	}
        	}


    		for (int i = 0; i < NERFinder.length; i++) {
    			if (NERFinder[i] != null) {
		            Span locationSpans[] = NERFinder[i].find(simpleTokenizerLine);
		            for (int c = 0; c <  locationSpans.length; c++) {
		            	Span cw = locationSpans[c];
		            	int is = sentenceStart + words[cw.getStart()].getStart();
		            	int ie = sentenceStart + words[cw.getEnd() - 1].getEnd();
		            			
	            		ta.add("NE", NERFinderTag[i], is, ie);
		            }
        		}
    		}
        }
             
		for (int i = 0; i < NERFinder.length; i++) {
			if (NERFinder[i] != null) {
				NERFinder[i].clearAdaptiveData();
			}
		}
        
		List<Annotation> res = new ArrayList<>();
		

	    
//	    JsonNode resources = root.get("Resources");
//	    if (resources != null) {
//	    	for (Iterator<JsonNode> iter = resources.elements(); iter.hasNext();) {
//	    		JsonNode resource = iter.next();
//	    		
//	    		String URI = resource.get("@URI").asText();
//	    		String types = resource.get("@types").asText();
//	    		String surfaceForm = resource.get("@surfaceForm").asText();
//	    		int offset = resource.get("@offset").asInt();
//	    		double score = resource.get("@similarityScore").asDouble();
//	    		
//	    		res.add(new Annotation(this.getClass(), text, offset, offset + surfaceForm.length(), URI, score));
//	    	}
//	    }
	    

	    return res;

	}
    

	public static void main(String[] args) throws Exception {
//		String text = "The 'Hop Bine' was built in 1870 ready for later development in the eastern side of Biggleswade. Its description in 1898 was - \"A brick and slated house containing Tap Room, Parlour, Kitchen, Cellar and four upper rooms. Yard with boarded and tiled Shed, enclosed at either end as Stable and Coal-house; w.c.&c; also a piece of Garden Ground at side and back. The first owners were Frederick Archdale and Charles Robert Lindsell (namely Wells and Co). The first licensee was George Chesham from 1870 to 1877. Until recently it was a thriving little pub then Greene King changed to half pub half Indian restaurant. That killed it off! Now (February 2012) it is closed; the shutters are on and all signs have been removed; Greene King have applied for it to be changed to residential and to develop another two houses on the car park - pack 'em in tight!";
		String text = "This is a red silk dress"; 
		
		OpenNLPThesaurusAnnotator ta = OpenNLPThesaurusAnnotator.getThesaurusAnnotator(Language.EN);
		ta.annotate(text, new HashMap<String, Object>());
	}
	
//	public static void main2(String[] args) throws Exception {
//		// http://sourceforge.net/apps/mediawiki/opennlp/index.php?title=Parser#Training_Tool
//		InputStream is = new FileInputStream("resources/opennlp/en-parser-chunking.bin");
//	 
//		ParserModel model = new ParserModel(is);
//	 
//		Parser parser = ParserFactory.create(model);
//	 
//		String sentence = "Programcreek is a very huge and useful website.";
//		opennlp.tools.parser.Parse topParses[] = ParserTool.parseLine(sentence, parser, 1);
//	 
//		for (opennlp.tools.parser.Parse p : topParses) {
//			System.out.println(p.getHead());
//			System.out.println(p.getHeadIndex());
//			for (opennlp.tools.parser.Parse cp : p.getChildren()) {
////				System.out.println("A " + cp.getHead() + " " + cp.);
//			}
//			p.show();
//		}
//		is.close();
//	 
//		/*
//		 * (TOP (S (NP (NN Programcreek) ) (VP (VBZ is) (NP (DT a) (ADJP (RB
//		 * very) (JJ huge) (CC and) (JJ useful) ) ) ) (. website.) ) )
//		 */
//	}

	
}
