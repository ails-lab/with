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

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import play.libs.Json;
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

public class ImageAnnotator extends Annotator {
	
	protected static Map<Language, ImageAnnotator> annotators = new HashMap<>();

	private static String service = "";
	
	public static AnnotatorType getType() {
		return AnnotatorType.IMAGE;
	}

	public static String getName() {
		return "Image Analysis Annotator";
	}

    public static ImageAnnotator getAnnotator(Language lang, boolean cs) {
    	ImageAnnotator ta = null;
//    	ImageAnnotator ta = annotators.get(lang);
//    	
//    	if (ta == null) {
//    		synchronized (ImageAnnotator.class) {
//	    		if (ta == null) {
//		   			ta = new ImageAnnotator(lang, cs);
//		   			annotators.put(lang, ta);
//	    		}
//    		}
//   		} 
    	
    	return ta;
    } 
    
	private ImageAnnotator(Language lang, boolean caseSensitive) {
	}
	

	@Override
	public String getService() {
		return "";
	}
	
	@Override
	public List<Annotation> annotate(AnnotationTarget target, Map<String, Object> props) throws Exception {
		List<Annotation> res = new ArrayList<>();
		
		HttpClient client = HttpClientBuilder.create().build();

		HttpPost request = new HttpPost(service);
		request.setHeader("content-type", "application/json");
//		request.setHeader("accept", "application/json");

		ObjectNode json = Json.newObject();
//		json.put("id", arg1);
//		json.put("imageURL", ser);
//		json.put("annotationURL", arg1);
//		json.put("recordId", arg1);

		request.setEntity(new StringEntity(json.toString()));
		
		HttpResponse response = client.execute(request);		
		

//		  {  // single request
//			     "id" : "xxxxx" , // the id of the request, in case we later want to support status requests about it
//			     "imageURL" : "http://some.url.for.image/params?one=sdjhf&two=sjhfb", // the URL where the image can be downloaded. It might contain information to allow one-time access to images that are not publicly accessible. 
//			     "annotationURL": "http://some.with.endpoint/params?one=sjhdb&two=sjkdhfg", // url where the result should be posted in the agreed json format. This is a standard With URL again with the addition of a one-time-token to allow access as a certain user. 
//			 "recordId" :"o87403840w8e9ty0389ytpq", // the id of the record that will be annotated
//			// there may be more fields necessary
//			  },
		return res;
	}

}
