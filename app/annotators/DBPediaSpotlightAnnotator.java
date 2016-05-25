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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import model.basicDataTypes.Language;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;







import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;

public class DBPediaSpotlightAnnotator extends Annotator {

	public static String NAME = "DBPediaSpotlightAnnotator"; 
	
	private static String SPOTLIGHT_SERVER_URL = "http://spotlight.sztaki.hu:2222/rest/annotate";
	
	public static void main(String[] arg) throws Exception {
		new DBPediaSpotlightAnnotator().annotate("Bill Clinton was president of the United States of America.", null);
	}
	
	public String getName() {
		return "http://spotlight.sztaki.hu:2222/rest/annotate";
	}
	
	public List<Annotation> annotate(String text, Map<String, Object> props) throws Exception {
		List<Annotation> res = new ArrayList<>();
		
		Language lang = (Language)props.get(LANGUAGE);
		
		if (lang != Language.EN) {
			return res;
		}
		
		HttpClient client = new DefaultHttpClient();
		
		HttpPost request = new HttpPost(SPOTLIGHT_SERVER_URL);
		request.setHeader("content-type", "application/x-www-form-urlencoded");
		request.setHeader("accept", "application/json");
		
		text = URLEncoder.encode(text, "UTF-8");
		
		request.setEntity(new StringEntity("text=" + text, ContentType.create("application/x-www-form-urlencoded", Charset.forName("UTF-8"))));

		HttpResponse response = client.execute(request);
		
		int responseCode = response.getStatusLine().getStatusCode();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
    
		StringBuffer resx = new StringBuffer();
	    String line;
	       
	    while ((line = br.readLine()) != null) {
	    	resx.append(line + "\n");
	    }
	    br.close();
	    
	    JsonNode root = Json.parse(resx.toString());
	    
	    JsonNode resources = root.get("Resources");
	    if (resources != null) {
	    	for (Iterator<JsonNode> iter = resources.elements(); iter.hasNext();) {
	    		JsonNode resource = iter.next();
	    		
	    		String URI = resource.get("@URI").asText();
	    		String types = resource.get("@types").asText();
	    		String surfaceForm = resource.get("@surfaceForm").asText();
	    		int offset = resource.get("@offset").asInt();
	    		double score = resource.get("@similarityScore").asDouble();
	    		
	    		res.add(new Annotation(this.getClass(), text, offset, offset + surfaceForm.length(), URI, score));
	    	}
	    }
	    
	    return res;
	    
	}
}
