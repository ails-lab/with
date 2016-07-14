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


package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.mvc.Result;
import sources.core.ApacheHttpConnector;
import sources.core.QueryBuilder;

public class DBPediaController extends WithResourceController {

	public static Result dbpediaLookup(String type, String query, int start, int count) {

		QueryBuilder builder = new QueryBuilder("http://zenon.image.ece.ntua.gr/fres/service/dbpedia-with");
		builder.addSearchParam("type", type);
		builder.addSearchParam("start", "" + start);
		builder.addSearchParam("rows", "" + count);
		builder.addQuery("query", query);

		ObjectNode result = Json.newObject();
		
		try {
			JsonNode response = ApacheHttpConnector.getApacheHttpConnector().getURLContent(builder.getHttp());
			
//			result.put("entryCount", totalHits);
//			result.put("result", response.textValue());
			return ok(response);
			
		} catch (Exception e) {
			
			result.put("error", e.getMessage());
			return internalServerError(result);			
		}
	}
	
}
