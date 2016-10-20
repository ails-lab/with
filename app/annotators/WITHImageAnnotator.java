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

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import play.libs.Json;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class WITHImageAnnotator extends RequestAnnotator {
	
	private static String service = "http://ironman.image.ece.ntua.gr:50000";
	private static String reponseApi = "/image/imageAnnotations";
	
	public static AnnotatorDescriptor descriptor = new Descriptor();
	
	public static class Descriptor implements RequestAnnotator.Descriptor {
	
		@Override
		public AnnotatorType getType() {
			return AnnotatorType.IMAGE;
		}
	
		@Override
		public String getName() {
			return "Image Analysis Annotator";
		}
		
		@Override
		public String getResponseApi() {
			return reponseApi;
		}
		
		@Override
		public String getService() {
			return service;
		}
		
		@Override
		public int getDataLimit() {
			return 100;
		}
	}
	

	public static HttpResponse sendRequest(String requestId, ArrayNode array) throws ClientProtocolException, IOException {
		ObjectNode json = Json.newObject();
		json.put("requestId", requestId);
		json.put("data", array);
		
		HttpClient client = HttpClientBuilder.create().build();

		HttpPost request = new HttpPost(service);
		request.setHeader("content-type", "application/json");
//		request.setHeader("accept", "application/json");

		request.setEntity(new StringEntity(json.toString()));
		
//		System.out.println(json.toString());
		return client.execute(request);

	}
}
