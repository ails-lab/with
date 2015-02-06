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

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import sun.security.pkcs.ParsingException;
import views.html.index;

import com.fasterxml.jackson.databind.JsonNode;

import espace.core.CommonQuery;
import espace.core.ESpaceSources;
import espace.core.SourceResponse;
import espace.core.Utils;

public class SearchController extends Controller {

	public static Result index() {
		return ok(index.render("Your new application is ready."));
	}

	public static Result search() {
		System.out.println(request().body());
		JsonNode json = request().body().asJson();
		CommonQuery q = null;
		if (json == null) {
			return badRequest("Expecting Json query");
		} else {
			// Parse the query.
			try {
				q = Utils.parseJson(json);
			} catch (ParsingException e) {
				return badRequest(e.getMessage());
			}
		}
		return ok(Json.toJson(search(q)));
	}

	public static Object search(CommonQuery q) {
		Object result = ESpaceSources.fillResults(q);
		return result;
	}

	public static Result testsearch() {
		JsonNode json = request().body().asJson();
		CommonQuery q = null;
		if (json == null) {
			return badRequest("Expecting Json query");
		} else {
			// Parse the query.
			try {
				q = Utils.parseJson(json);
			} catch (ParsingException e) {
				return badRequest(e.getMessage());
			}
		}
		SourceResponse res = (SourceResponse) search(q);
		return ok(testsearch.render(q, res));
	}

}
