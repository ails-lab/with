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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import oauth.signpost.http.HttpParameters;
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

	private HttpResponse getContentResponse(HttpUriRequest method) throws IOException, ClientProtocolException {
		URI url = method.getURI();
		try {
			Logger.debug("calling: " + url);
			long time = System.currentTimeMillis();
			HttpClient client = HttpClientBuilder.create().build();
			HttpResponse response = client.execute(method);
			long ftime = (System.currentTimeMillis() - time) / 1000;
			Logger.debug("waited " + ftime + " sec for: " + url);
			return response;
		} catch (Exception e) {
			Logger.error("calling: " + url);
			Logger.error("msg: " + e.getMessage());
			throw e;
		}
	}

	private JsonNode getJsonContentResponse(HttpUriRequest method) throws IOException, ClientProtocolException {
		HttpResponse response = getContentResponse(method);
		BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(rd, JsonNode.class);
	}

	public <T> T getContent(String url) throws Exception {
		JsonNode response = getJsonContentResponse(buildGet(url));
		return (T) response;
	}

	private HttpGet buildGet(String url) {
		return new HttpGet(Utils.replaceQuotes(url));
	}

//	public File getContentAsFile(String url) throws Exception {
//		HttpResponse response = getContentResponse(buildGet(url));
//		File tmp = new File("temp");
//		response.getEntity().writeTo(new FileOutputStream(tmp));
//		return tmp;
//	}
//
//	public <T> T postFileContent(String url, File file, String paramName, String paramValue) throws Exception {
//		String url1 = Utils.replaceQuotes(url);
//		HttpClient client = HttpClientBuilder.create().build();
//		HttpPost method = new HttpPost(url1);
//		method.setHeader(paramName, paramValue);
//
//		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
//		builder.addTextBody(paramName, paramValue);
//		builder.addBinaryBody("file", file, ContentType.APPLICATION_OCTET_STREAM, "file.ext");
//
//		method.setEntity(builder.build());
//		JsonNode res = getJsonContentResponse(method);
//		return (T) res;
//	}

}
