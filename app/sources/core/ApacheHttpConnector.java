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


package sources.core;


import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import play.Logger;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

public class ApacheHttpConnector extends HttpConnector {
	
	private static ApacheHttpConnector instance;
	
	public static HttpConnector getApacheHttpConnector(){
		if (instance==null){
			instance = new ApacheHttpConnector();
		}
		return instance;
	}
	
	public <T> T getContent(String url) throws Exception {
		try {
			Logger.debug("calling: " + url);
			long time = System.currentTimeMillis();
			String url1 = Utils.replaceQuotes(url);
			
			
			HttpClient client = HttpClientBuilder.create().build();
			HttpGet method = new HttpGet(url1);
			HttpResponse response = client.execute(method);
			BufferedReader rd = new BufferedReader(
	                new InputStreamReader(response.getEntity().getContent()));
			ObjectMapper mapper = new ObjectMapper();
			return (T)mapper.readValue(rd,JsonNode.class);
		} catch (Exception e) {
			Logger.error("calling: " + url);
			Logger.error("msg: " + e.getMessage());

			throw e;
		}
	}

}
