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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import play.Logger;
import play.Logger.ALogger;

public class ApacheHttpConnector extends HttpConnector {
	
	public static class FileAndType {
		public String mimeType;
		public File data;
	}
	
	public static final ALogger log = Logger.of( ApacheHttpConnector.class );

	private static final int TRIES = 3;
	
	private static ApacheHttpConnector instance;
	public static HttpClientBuilder httpClientBuilder =
			HttpClientBuilder.create()
			.setConnectionTimeToLive(1, TimeUnit.MINUTES)
			.setMaxConnPerRoute(5);

	public static HttpConnector getApacheHttpConnector(){
		if (instance==null){
			instance = new ApacheHttpConnector();			
		}
		return instance;
	}

	

	private JsonNode getJsonContentResponse(HttpUriRequest method) throws IOException, ClientProtocolException {
		URI url = method.getURI();
		CloseableHttpClient client = null;
		JsonNode readValue = null;
		CloseableHttpResponse response = null;
		BufferedReader rd = null;
		try {
			log.debug("calling: " + url);
			long time = System.currentTimeMillis();
			client = httpClientBuilder.build();
			response = client.execute(method);
			long ftime = (System.currentTimeMillis() - time) / 1000;
			log.debug("waited " + ftime + " sec for: " + url);
			rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
			ObjectMapper mapper = new ObjectMapper();
			readValue = mapper.readValue(rd, JsonNode.class);
		} catch (Exception e) {
			log.error("calling: " + url);
			log.error("msg: " + e.getMessage());
			throw e;
		} finally {
			if (client != null)
				client.close();
			if (response != null)
				response.close();
			if (rd != null)
				rd.close();
		}
		
		return readValue;
	}
	
	public FileAndType getContentAndType(String url) throws Exception {
		CloseableHttpClient client = null;
		CloseableHttpResponse response = null;
		try {
			FileAndType res = new FileAndType();
			log.debug("calling: " + url);
			long time = System.currentTimeMillis();
			res.data = File.createTempFile("dwnld", "");
			client = httpClientBuilder.build();
			response = client.execute(buildGet( url ));
			Header[] headers = response.getHeaders("Content-type");
			for( Header h: headers ) {
				if( StringUtils.isNotEmpty( h.getValue())) res.mimeType = h.getValue();
			}
			log.debug( "Content-type: " + StringUtils.join(headers, ","));
			FileUtils.copyInputStreamToFile(response.getEntity().getContent(), res.data );
			return res;
		} catch (Exception e) {
			log.error("calling: " + url);
			log.error("msg: " + e.getMessage());

			throw e;
		} finally{
			if (client != null)
				client.close();
			if (response != null)
				response.close();
		}
	}


	public <T> T getContent(String url) throws Exception {
		log.debug("1 calling: " + url);
		JsonNode response = getJsonContentResponse(buildGet(url));
		int tries=0;
		while (!response.isContainerNode() && tries<TRIES){
			response = getJsonContentResponse(buildGet(url));
			tries++;
		}
		return (T) response;
	}

	private HttpGet buildGet(String url) throws URISyntaxException {
		return new HttpGet(url);
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
