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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.JsonNode;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.multipart.FilePart;
import com.ning.http.multipart.MultipartRequestEntity;
import com.ning.http.multipart.Part;

import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;


public class HttpConnector {
	public static final ALogger log = Logger.of( HttpConnector.class );
	private static HttpConnector wsHttpConnector;


	public static HttpConnector getWSHttpConnector(){
		if (wsHttpConnector==null){
			wsHttpConnector = new HttpConnector();
		}
		return wsHttpConnector;
	}

	final int TIMEOUT_CONNECTION = 60000;
	
	
	public <T> T getContent(String url) throws Exception {
		try {
			log.debug("calling: " + url);
			long time = System.currentTimeMillis();
			String url1 = Utils.replaceQuotes(url);
			
			Promise<T> jsonPromise = WS.url(url1).get().map(new Function<WSResponse, T>() {
				public T apply(WSResponse response) {
//					System.out.println(response.getBody());
					T json = (T) response.asJson();
					long ftime = (System.currentTimeMillis() - time)/1000;
					log.debug("waited "+ftime+" sec for: " + url);
					return json;
				}
			});
			return (T) jsonPromise.get(TIMEOUT_CONNECTION);
		} catch (Exception e) {
			log.error("calling: " + url);
			log.error("msg: " + e.getMessage());

			throw e;
		}
	}
	
	public  File getContentAsFile(String url) throws Exception {
		try {
			log.debug("calling: " + url);
			long time = System.currentTimeMillis();
			String url1 = Utils.replaceQuotes(url);
			
			Promise<File> filePromise = WS.url(url1).get().map(new Function<WSResponse, File>() {
				public File apply(WSResponse response) throws IOException {
//					System.out.println(response.getBody());
					File tmp = new File("temp");
					FileUtils.writeByteArrayToFile(tmp, response.asByteArray());
					//T file = (T) response.asByteArray();
					long ftime = (System.currentTimeMillis() - time)/1000;
					log.debug("waited "+ftime+" sec for: " + url);
					return tmp;
				}
			});
			return filePromise.get(TIMEOUT_CONNECTION);
		} catch (Exception e) {
			log.error("calling: " + url);
			log.error("msg: " + e.getMessage());

			throw e;
		}
	}

	
	public  <T> T postFileContent(String url, File file, String paramName, String paramValue) throws Exception {
		try {
			log.debug("calling: " + url);
			long time = System.currentTimeMillis();
			String url1 = Utils.replaceQuotes(url);
			
			Promise<T> jsonPromise = WS.url(url1).setQueryParameter(paramName, paramValue).post(file).map(new Function<WSResponse, T>() {
				public T apply(WSResponse response) {
//					System.out.println(response.getBody());
					T json = (T) response.asJson();
					long ftime = (System.currentTimeMillis() - time)/1000;
					log.debug("waited "+ftime+" sec for: " + url);
					return json;
				}
			});
			return (T) jsonPromise.get(TIMEOUT_CONNECTION);
		} catch (Exception e) {
			log.error("calling: " + url);
			log.error("msg: " + e.getMessage());

			throw e;
		}
	}
	
/*	public  <T> T postMultiPartFormDataContent(String url, FilePart file, 
			String paramName, String paramValue) throws Exception {
		
		
		try {

			Map<String, FilePart> data = new HashMap<String, FilePart>();
			
			data.put(paramName, file);
			
			String boundary = "--XYZ123--";

			String body = "";
			for (String key : data.keySet()) {
			  body += "--" + boundary + "\r\n"
			       + "Content-Disposition: form-data; name=\""
			       + key + "\"\r\n\r\n"
			       + data.get(key) + "\r\n";
			}
			body += "--" + boundary + "--";


			Logger.debug("calling: " + url);
			long time = System.currentTimeMillis();
			String url1 = Utils.replaceQuotes(url);
			
			Promise<T> jsonPromise = WS.url(url1).setTimeout(10000)
					.setHeader("Content-Type", "multipart/form-data; boundary=" + boundary)
					.setHeader("Content-length", String.valueOf(body.length()))
					.post(body).map(new Function<WSResponse, T>() {
				public T apply(WSResponse response) {
//					System.out.println(response.getBody());
					T json = (T) response.asJson();
					long ftime = (System.currentTimeMillis() - time)/1000;
					Logger.debug("waited "+ftime+" sec for: " + url);
					return json;
				}
			});
			return (T) jsonPromise.get(TIMEOUT_CONNECTION);
		} catch (Exception e) {
			Logger.error("calling: " + url);
			Logger.error("msg: " + e.getMessage());

			throw e;
		}
	}
*/	
	
	
	public  <T> T postMultiPartFormDataContent(String url, File file, 
			String paramName, String paramValue) throws Exception {
		
		
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			
			// Build up the Multiparts
			List<Part> parts = new ArrayList<>();
			parts.add(new FilePart("file", file));
			Part[] partsA = parts.toArray(new Part[parts.size()]);
			
			FluentCaseInsensitiveStringsMap params = new FluentCaseInsensitiveStringsMap();
			
			params.add(paramName, "file"+paramValue);
			
			// Add it to the MultipartRequestEntity
			MultipartRequestEntity reqE = new MultipartRequestEntity(partsA,
					params);
			reqE.writeRequest(bos);
			InputStream reqIS = new ByteArrayInputStream(bos.toByteArray());
			//WSRequestHolder req = WS.url(InterchangeConfig.conflateUrl+"dataset")
			//    .setContentType(reqE.getContentType());
			
			
			log.debug("calling: " + url);
			long time = System.currentTimeMillis();
			String url1 = Utils.replaceQuotes(url);
			
			Promise<T> jsonPromise = WS.url(url1).setTimeout(10000)
					.post(reqIS).map(new Function<WSResponse, T>() {
				public T apply(WSResponse response) {
//					System.out.println(response.getBody());
					T json = (T) response.asJson();
					long ftime = (System.currentTimeMillis() - time)/1000;
					log.debug("waited "+ftime+" sec for: " + url);
					return json;
				}
			});
			log.info("request: " + WS.url(url1).setQueryParameter(paramName, paramValue)
					.post(reqIS));
			return (T) jsonPromise.get(TIMEOUT_CONNECTION);
		} catch (Exception e) {
			log.error("calling: " + url);
			log.error("msg: " + e.getMessage());

			throw e;
		}
	}

	
	public  <T> T postJsonContent(String url, JsonNode node) throws Exception {
		try {
			log.debug("calling: " + url);
			long time = System.currentTimeMillis();
			String url1 = Utils.replaceQuotes(url);
			
			Promise<T> jsonPromise = WS.url(url1).post(node).map(new Function<WSResponse, T>() {
				public T apply(WSResponse response) {
//					System.out.println(response.getBody());
					T json = (T) response.asJson();
					long ftime = (System.currentTimeMillis() - time)/1000;
					log.debug("waited "+ftime+" sec for: " + url);
					return json;
				}
			});
			return (T) jsonPromise.get(TIMEOUT_CONNECTION);
		} catch (Exception e) {
			log.error("calling: " + url);
			log.error("msg: " + e.getMessage());

			throw e;
		}
	}

public  <T> T postContent(String url, String parameter, String paramName) throws Exception {
		try {
			log.debug("calling: " + url);
			long time = System.currentTimeMillis();
			String url1 = Utils.replaceQuotes(url);
			
			//Promise<T> jsonPromise = WS.url(url1).post("content").map(new Function<WSResponse, T>() {
			Promise<T> jsonPromise = WS.url(url1)
					.setQueryParameter(paramName, parameter)
					.post("content").map(new Function<WSResponse, T>() {
				public T apply(WSResponse response) {
					T json = (T) response.asJson();
					long ftime = (System.currentTimeMillis() - time)/1000;
					log.debug("waited "+ftime+" sec for: " + url);
					return json;
				}
			});
			return (T) jsonPromise.get(TIMEOUT_CONNECTION);
		} catch (Exception e) {
			log.error("calling: " + url);
			log.error("msg: " + e.getMessage());

			throw e;
		}
	}


	public  JsonNode getURLContent(String url) throws Exception {
		return this.<JsonNode>getContent(url);
	}
	
	public  File getURLContentAsFile(String url) throws Exception {
		return this.<File>getContentAsFile(url);
	}

	
	public  JsonNode postURLContent(String url, String parameter, String paramName) throws Exception {
		return this.<JsonNode>postContent(url, parameter, paramName);
	}

	
	public  JsonNode postJson(String url, JsonNode node) throws Exception {
		return this.<JsonNode>postJsonContent(url, node);
	}

	
	public  JsonNode postFile(String url, File file, String paramName, String paramValue) throws Exception {
		return this.<JsonNode>postFileContent(url, file, paramName, paramValue);
	}
	
	public  JsonNode postMultiPartFormData(String url, File file, String paramName, String paramValue) throws Exception {
		return this.<JsonNode>postMultiPartFormDataContent(url, file, paramName, paramValue);
	}

	
	public  String getURLStringContent(String url) throws Exception {
		return this.<String>getContent(url);
	}

	public  Document getURLContentAsXML(String url) throws Exception {
		try {
			Promise<Document> xmlPromise = WS.url(url).get().map(new Function<WSResponse, Document>() {
				public Document apply(WSResponse response) {
					Document xml = response.asXml();
					return xml;
				}
			});
			return xmlPromise.get(TIMEOUT_CONNECTION);
		} catch (Exception e) {
			throw e;
		}

	}
}
