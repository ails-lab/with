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

import java.io.IOException;

import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

import com.fasterxml.jackson.databind.JsonNode;

public class HttpConnector {

	private static final int CONNECTION_RETRIES = 3;
	private static final int TIMEOUT_CONNECTION = 40000;
	private static final int STATUS_OK_START = 200;
	private static final int STATUS_OK_END = 299;
	private static final String ENCODING = "UTF-8";

	public static JsonNode getURLContent(String url) throws IOException {
		System.out.println("calling: " + url);
		Promise<JsonNode> jsonPromise = WS.url(url).get().map(new Function<WSResponse, JsonNode>() {
			public JsonNode apply(WSResponse response) {
				JsonNode json = response.asJson();
				return json;
			}
		});
		return jsonPromise.get(TIMEOUT_CONNECTION);
	}

}
