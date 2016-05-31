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

import org.apache.http.impl.client.HttpClientBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;

import play.libs.Json;

public class DBPediaSpotlightAnnotator extends Annotator {

	public static String NAME = "DBPediaSpotlightAnnotator"; 
	
	private static String DPBEDIA_ENDPOINT = "http://zenon.image.ece.ntua.gr:8890/sparql";
	
	protected static Map<Language, DBPediaSpotlightAnnotator> annotators = new HashMap<>();
	
	public static Map<Language, String> serverMap = new HashMap<>();
	static {
		serverMap.put(Language.EN, "http://spotlight.sztaki.hu:2222/rest/annotate");
		serverMap.put(Language.FR, "http://spotlight.sztaki.hu:2225/rest/annotate");
		serverMap.put(Language.DE, "http://spotlight.sztaki.hu:2226/rest/annotate");
		serverMap.put(Language.RU, "http://spotlight.sztaki.hu:2227/rest/annotate");
		serverMap.put(Language.PT, "http://spotlight.sztaki.hu:2228/rest/annotate");
		serverMap.put(Language.HU, "http://spotlight.sztaki.hu:2229/rest/annotate");
		serverMap.put(Language.IT, "http://spotlight.sztaki.hu:2230/rest/annotate");
		serverMap.put(Language.ES, "http://spotlight.sztaki.hu:2231/rest/annotate");
		serverMap.put(Language.NL, "http://spotlight.sztaki.hu:2232/rest/annotate");
		serverMap.put(Language.TR, "http://spotlight.sztaki.hu:2235/rest/annotate");
	}
	
	private String service;
	
    public static DBPediaSpotlightAnnotator getAnnotator(Language lang) {
    	DBPediaSpotlightAnnotator ta = annotators.get(lang);
    	if (ta == null) {
    		if (serverMap.containsKey(lang)) {
    			ta = new DBPediaSpotlightAnnotator(lang);
    			annotators.put(lang, ta);
    		} else {
    			return null;
    		}
    	}
    	
    	return ta;
    }  
    
    private DBPediaSpotlightAnnotator(Language language) {
    	service = serverMap.get(language);
    }

	public String getName() {
		return "DBPediaSpotlight";
	}

	public String getService() {
		return service;
	}
	
	public List<Annotation> annotate(String text, Map<String, Object> props) throws Exception {
		List<Annotation> res = new ArrayList<>();
		
		HttpClient client = HttpClientBuilder.create().build();
		
		HttpPost request = new HttpPost(service);
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

	    		String query;
	    		if (URI.startsWith("http://dbpedia.org")) {
	    			query = "select ?label where {<" + URI + "> <http://www.w3.org/2000/01/rdf-schema#label> ?label}";
	    		} else {
	    			query = "select ?label where { ?x <http://www.w3.org/2000/01/rdf-schema#label> ?label . ?x <http://www.w3.org/2002/07/owl#sameAs> <" + URI + "> }";
	    		}
	    		
	    		System.out.println(query);

	    	    QueryExecution qe = QueryExecutionFactory.sparqlService(DPBEDIA_ENDPOINT, QueryFactory.create(query, Syntax.syntaxSPARQL));
	    		ResultSet rs = qe.execSelect();

	    		String label = null;
	    		if (rs.hasNext()) {
	    			QuerySolution sol = rs.next();
	    			
	    			List<String> vars = rs.getResultVars();
	    			
	    			RDFNode s = sol.get(vars.get(0));
	    			Literal literal = s.asLiteral();
//	    			String lang = literal.getLanguage();
	    			label = literal.getString();
	    		}
	    		
	    		res.add(new Annotation(this.getClass(), offset, offset + surfaceForm.length(), score, URI, label, "dbpedia"));
	    	}
	    }
	    
	    return res;
	    
	}
}
