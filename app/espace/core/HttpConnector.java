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


package espace.core;

import org.w3c.dom.Document;

import espace.core.Utils;
import play.Logger;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

import com.fasterxml.jackson.databind.JsonNode;

public class HttpConnector {

	private static final int TIMEOUT_CONNECTION = 40000;
	
	
	public static <T> T getContent(String url) throws Exception {
		try {
			Logger.debug("calling: " + url);
			long time = System.currentTimeMillis();
			String url1 = Utils.replaceQuotes(url);
			
			Promise<T> jsonPromise = WS.url(url1).get().map(new Function<WSResponse, T>() {
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
	
	public static JsonNode getURLContent(String url) throws Exception {
		return HttpConnector.<JsonNode>getContent(url);
	}
	
	public static String getURLStringContent(String url) throws Exception {
		return HttpConnector.<String>getContent(url);
	}

	public static Document getURLContentAsXML(String url) throws Exception {
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
