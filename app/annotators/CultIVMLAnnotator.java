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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import play.Play;
import play.libs.Json;
import sources.core.QueryBuilder;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CultIVMLAnnotator {
	
	public static String service = "http://ironman.image.ntua.gr:30000/WITH/";
	public static String reponseApi = "/annotation/annotateRequestResult";

	public static String ip = "";

	static {
		try {
			ip = "http://" + InetAddress.getLocalHost().getHostAddress() + ":" + Play.application().configuration().getString("http.port");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public static String getName() {
		return "CultIVML Annotator";
	}
	
	public static ObjectNode createDataEntry(String imageURL, String recordId, long token) {
		ObjectNode json = Json.newObject();
			
		QueryBuilder qb = new QueryBuilder(imageURL);
			
		if (qb.getHttp().startsWith("http://")) {
			json.put("imageURL", qb.getHttp());
		} else {
			qb.addSearchParam("token", token + "");
			json.put("imageURL", ip + qb.getHttp());
		}
		json.put("recordId", recordId);
		
		return json;
	}
		
	public static ObjectNode createMessage(String rid, List<ObjectNode> list, long token) {
		ArrayNode array = Json.newObject().arrayNode();
		for (ObjectNode obj : list) {
			array.add(obj);
		}

		ObjectNode json = Json.newObject();
		json.put("requestId", rid);
		json.put("annotationURL", ip + reponseApi + "?token=" + token);

		json.put("data", array);
		
		return json;
	}


}
