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
import org.bson.types.ObjectId;

import play.libs.Akka;
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
import actors.TokenLoginActor;
import actors.TokenLoginActor.TokenCreateMessage;
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

public class ImageAnnotator extends Annotator {
	
	protected static Map<Language, ImageAnnotator> annotators = new HashMap<>();

	private static String service = "http://ironman.image.ece.ntua.gr:50000";
	
	public static AnnotatorType getType() {
		return AnnotatorType.IMAGE;
	}

	public static String getName() {
		return "Image Analysis Annotator";
	}

    public static ImageAnnotator getAnnotator(Language lang, boolean cs) {
    	ImageAnnotator ta = null;
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
		
	    TokenCreateMessage tokenCreateMsg = new TokenCreateMessage((ObjectId)props.get(Annotator.USERID));
	    final ActorSelection tokenLoginActor = Akka.system().actorSelection("/user/tokenLoginActor");

	    Timeout timeout = new Timeout(Duration.create(5, "seconds"));

	    Future<Object> future = Patterns.ask(tokenLoginActor, tokenCreateMsg, timeout);

	    JsonNode token = null;

	    try{
	        token = (JsonNode) Await.result(future, timeout.duration());
	    } catch( Exception e) {
	    	e.printStackTrace();
	        // TODO Auto-generated catch block
//	        log.error("", e);
	    } 
	    
		HttpClient client = HttpClientBuilder.create().build();

		HttpPost request = new HttpPost(service);
		request.setHeader("content-type", "application/json");
//		request.setHeader("accept", "application/json");

		ObjectNode json = Json.newObject();
//		json.put("id", arg1);
//		json.put("imageURL", ser);
//		json.put("annotationURL", arg1);
		json.put("recordId", target.getRecordId().toString());

//		request.setEntity(new StringEntity(json.toString()));
//		
//		HttpResponse response = client.execute(request);		

		return res;
	}

}
